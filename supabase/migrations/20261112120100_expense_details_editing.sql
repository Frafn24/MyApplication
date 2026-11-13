-- Add Expense, Expense Details and Edit Expense screens.
--   * expenses.updated_at
--   * create_expense() / update_expense(): expense + equal split in one transaction
--   * get_group_expenses(): a group's expenses with the caller's share
--   * get_expense_details(): one expense with its split, for Expense Details
-- Rollback: supabase/rollbacks/20261112120100_expense_details_editing.down.sql

ALTER TABLE public.expenses
  ADD COLUMN updated_at timestamptz NOT NULL DEFAULT now();

CREATE TRIGGER expenses_set_updated_at
  BEFORE UPDATE ON public.expenses
  FOR EACH ROW
  EXECUTE FUNCTION public.set_updated_at();

-- Replaces the split of an expense with an equal split between p_user_ids.
-- Leftover cents go to the first users in the list, so the split always adds
-- up to the expense amount (100 / 3 = 33.34 + 33.33 + 33.33).
-- SECURITY INVOKER: the expense_splits policies still apply.
CREATE OR REPLACE FUNCTION public.set_equal_split(p_expense_id uuid, p_user_ids uuid[])
  RETURNS void
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
declare
  v_group_id uuid;
  v_amount numeric(12, 2);
  v_users uuid[];
  v_count integer;
  v_share numeric(12, 2);
  v_extra_cents integer;
begin
  select e.group_id, e.amount into v_group_id, v_amount
  from public.expenses e
  where e.id = p_expense_id;
  if v_group_id is null then
    raise exception 'Expense not found';
  end if;

  -- Remove duplicates and nulls, keep the order.
  select coalesce(array_agg(u.id order by u.ord), '{}')
  into v_users
  from (
    select t.id, min(t.ord) as ord
    from unnest(p_user_ids) with ordinality as t (id, ord)
    where t.id is not null
    group by t.id
  ) u;

  v_count := cardinality(v_users);
  if v_count = 0 then
    raise exception 'Choose at least one person to split with';
  end if;
  if exists (select 1 from unnest(v_users) as u (id) where not public.is_group_member(v_group_id, u.id)) then
    raise exception 'Everyone in the split must be a member of the group';
  end if;

  v_share := trunc(v_amount / v_count, 2);
  v_extra_cents := ((v_amount - v_share * v_count) * 100)::integer;

  delete from public.expense_splits where expense_id = p_expense_id;

  insert into public.expense_splits (expense_id, user_id, amount)
  select p_expense_id, u.id, v_share + case when u.ord <= v_extra_cents then 0.01 else 0 end
  from unnest(v_users) with ordinality as u (id, ord);
end;
$function$;
-- Callable by authenticated because create_expense/update_expense run as the
-- caller; RLS only lets it change splits of the caller's own expenses.
REVOKE ALL ON FUNCTION public.set_equal_split(uuid, uuid[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.set_equal_split(uuid, uuid[]) TO authenticated;

-- "Add Expense". Returns the new expense's id.
CREATE OR REPLACE FUNCTION public.create_expense(
  p_group_id uuid,
  p_description text,
  p_amount numeric,
  p_paid_by uuid,
  p_split_with uuid[]
)
  RETURNS uuid
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
declare
  v_expense_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id) then
    raise exception 'You are not a member of this group';
  end if;
  if not public.is_group_member(p_group_id, p_paid_by) then
    raise exception 'The payer must be a member of the group';
  end if;
  if char_length(trim(coalesce(p_description, ''))) not between 1 and 200 then
    raise exception 'The title must be between 1 and 200 characters';
  end if;
  if p_amount is null or round(p_amount, 2) <= 0 then
    raise exception 'The amount must be greater than 0';
  end if;

  insert into public.expenses (group_id, paid_by, description, amount, created_by)
  values (p_group_id, p_paid_by, trim(p_description), round(p_amount, 2), auth.uid())
  returning id into v_expense_id;

  perform public.set_equal_split(v_expense_id, p_split_with);

  return v_expense_id;
end;
$function$;
REVOKE ALL ON FUNCTION public.create_expense(uuid, text, numeric, uuid, uuid[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_expense(uuid, text, numeric, uuid, uuid[]) TO authenticated;

-- "Edit Expense". Only the person who added the expense can edit it.
CREATE OR REPLACE FUNCTION public.update_expense(
  p_expense_id uuid,
  p_description text,
  p_amount numeric,
  p_paid_by uuid,
  p_split_with uuid[]
)
  RETURNS void
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
declare
  v_group_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  select e.group_id into v_group_id
  from public.expenses e
  where e.id = p_expense_id and e.created_by = auth.uid();
  if v_group_id is null then
    raise exception 'Only the person who added this expense can edit it';
  end if;
  if not public.is_group_member(v_group_id, p_paid_by) then
    raise exception 'The payer must be a member of the group';
  end if;
  if char_length(trim(coalesce(p_description, ''))) not between 1 and 200 then
    raise exception 'The title must be between 1 and 200 characters';
  end if;
  if p_amount is null or round(p_amount, 2) <= 0 then
    raise exception 'The amount must be greater than 0';
  end if;

  update public.expenses
  set description = trim(p_description),
      amount = round(p_amount, 2),
      paid_by = p_paid_by
  where id = p_expense_id;

  perform public.set_equal_split(p_expense_id, p_split_with);
end;
$function$;
REVOKE ALL ON FUNCTION public.update_expense(uuid, text, numeric, uuid, uuid[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.update_expense(uuid, text, numeric, uuid, uuid[]) TO authenticated;

-- A group's expenses, newest first. my_share is what the caller owes of each.
CREATE OR REPLACE FUNCTION public.get_group_expenses(p_group_id uuid)
  RETURNS TABLE (
    id uuid,
    description text,
    amount numeric,
    paid_by uuid,
    paid_by_name text,
    my_share numeric,
    created_at timestamptz
  )
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  select e.id,
         e.description,
         e.amount,
         e.paid_by,
         coalesce(nullif(p.full_name, ''), p.username, 'Unknown') as paid_by_name,
         coalesce(s.amount, 0) as my_share,
         e.created_at
  from public.expenses e
  left join public.profiles p on p.id = e.paid_by
  left join public.expense_splits s on s.expense_id = e.id and s.user_id = (select auth.uid())
  where e.group_id = p_group_id
  order by e.created_at desc;
$function$;
REVOKE ALL ON FUNCTION public.get_group_expenses(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_group_expenses(uuid) TO authenticated;

-- Expense Details. splits: [{"user_id": "...", "name": "...", "amount": 150.00}, ...]
CREATE OR REPLACE FUNCTION public.get_expense_details(p_expense_id uuid)
  RETURNS TABLE (
    id uuid,
    group_id uuid,
    description text,
    amount numeric,
    paid_by uuid,
    paid_by_name text,
    can_edit boolean,
    created_at timestamptz,
    updated_at timestamptz,
    splits jsonb
  )
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  select e.id,
         e.group_id,
         e.description,
         e.amount,
         e.paid_by,
         coalesce(nullif(payer.full_name, ''), payer.username, 'Unknown') as paid_by_name,
         e.created_by = (select auth.uid()) as can_edit,
         e.created_at,
         e.updated_at,
         coalesce((
           select jsonb_agg(
                    jsonb_build_object(
                      'user_id', s.user_id,
                      'name', coalesce(nullif(p.full_name, ''), p.username, 'Unknown'),
                      'amount', s.amount
                    )
                    order by s.user_id = (select auth.uid()) desc, p.full_name, p.username
                  )
           from public.expense_splits s
           left join public.profiles p on p.id = s.user_id
           where s.expense_id = e.id
         ), '[]'::jsonb) as splits
  from public.expenses e
  left join public.profiles payer on payer.id = e.paid_by
  where e.id = p_expense_id;
$function$;
REVOKE ALL ON FUNCTION public.get_expense_details(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_expense_details(uuid) TO authenticated;
