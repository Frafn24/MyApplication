-- expense_splits: how much of an expense each member owes.
-- For every split where user_id <> paid_by, that user owes the payer the amount.
CREATE TABLE public.expense_splits (
  expense_id uuid           NOT NULL REFERENCES public.expenses (id) ON DELETE CASCADE,
  user_id    uuid           NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
  amount     numeric(12, 2) NOT NULL CHECK (amount >= 0),
  PRIMARY KEY (expense_id, user_id)
);
CREATE INDEX expense_splits_user_id_idx ON public.expense_splits (user_id);

ALTER TABLE public.expense_splits ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Læs fordelinger i egne grupper" ON public.expense_splits
  FOR SELECT TO authenticated
  USING (
    exists (select 1 from public.expenses e where e.id = expense_id and public.is_group_member(e.group_id))
  );
CREATE POLICY "Opret fordeling på egen udgift" ON public.expense_splits
  FOR INSERT TO authenticated
  WITH CHECK (
    exists (
      select 1 from public.expenses e
      where e.id = expense_id
        and e.created_by = (select auth.uid())
        and public.is_group_member(e.group_id, user_id)
    )
  );
CREATE POLICY "Opdater fordeling på egen udgift" ON public.expense_splits
  FOR UPDATE TO authenticated
  USING (
    exists (select 1 from public.expenses e where e.id = expense_id and e.created_by = (select auth.uid()))
  )
  WITH CHECK (
    exists (
      select 1 from public.expenses e
      where e.id = expense_id
        and e.created_by = (select auth.uid())
        and public.is_group_member(e.group_id, user_id)
    )
  );
CREATE POLICY "Slet fordeling på egen udgift" ON public.expense_splits
  FOR DELETE TO authenticated
  USING (
    exists (select 1 from public.expenses e where e.id = expense_id and e.created_by = (select auth.uid()))
  );

REVOKE ALL ON TABLE public.expense_splits FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.expense_splits TO authenticated;

-- Balances screen: net amount between the caller and each other member, per group.
-- amount > 0: the other member owes the caller. amount < 0: the caller owes them.
CREATE OR REPLACE FUNCTION public.get_my_balances()
  RETURNS TABLE (group_id uuid, group_name text, other_user_id uuid, other_name text, amount numeric)
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  with debts as (
    select e.group_id, s.user_id as debtor, e.paid_by as creditor, s.amount
    from public.expenses e
    join public.expense_splits s on s.expense_id = e.id
    where s.user_id <> e.paid_by
  ),
  mine as (
    select d.group_id, d.debtor as other_user_id, d.amount
    from debts d where d.creditor = (select auth.uid())
    union all
    select d.group_id, d.creditor as other_user_id, -d.amount
    from debts d where d.debtor = (select auth.uid())
  )
  select m.group_id,
         g.name as group_name,
         m.other_user_id,
         coalesce(nullif(p.full_name, ''), p.username, 'Unknown') as other_name,
         sum(m.amount) as amount
  from mine m
  join public.groups g on g.id = m.group_id
  left join public.profiles p on p.id = m.other_user_id
  group by m.group_id, g.name, m.other_user_id, p.full_name, p.username
  having sum(m.amount) <> 0
  order by g.name, other_name;
$function$;

-- Groups screen: the caller's groups with member count and net balance.
CREATE OR REPLACE FUNCTION public.get_my_groups()
  RETURNS TABLE (id uuid, name text, member_count integer, my_balance numeric, created_at timestamptz)
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  select g.id,
         g.name,
         (select count(*)::integer from public.group_members gm where gm.group_id = g.id) as member_count,
         coalesce((select sum(b.amount) from public.get_my_balances() b where b.group_id = g.id), 0) as my_balance,
         g.created_at
  from public.groups g
  where public.is_group_member(g.id)
  order by g.created_at desc;
$function$;

REVOKE ALL ON FUNCTION public.get_my_balances() FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.get_my_groups() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_my_balances() TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_my_groups() TO authenticated;

-- "Remind" on the Balances screen: sends a "Payment Reminder" notification.
-- At most one per debtor, group and sender per hour.
CREATE OR REPLACE FUNCTION public.send_payment_reminder(p_group_id uuid, p_debtor_id uuid)
  RETURNS void
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_me uuid := auth.uid();
  v_owed numeric;
  v_group_name text;
begin
  if v_me is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if not public.is_group_member(p_group_id, v_me) or not public.is_group_member(p_group_id, p_debtor_id) then
    raise exception 'Not a member of this group' using errcode = '42501';
  end if;

  select coalesce(sum(case when e.paid_by = v_me then s.amount else -s.amount end), 0)
  into v_owed
  from public.expenses e
  join public.expense_splits s on s.expense_id = e.id
  where e.group_id = p_group_id
    and ((e.paid_by = v_me and s.user_id = p_debtor_id)
      or (e.paid_by = p_debtor_id and s.user_id = v_me));

  if v_owed <= 0 then
    raise exception 'This member does not owe you anything' using errcode = 'P0001';
  end if;

  if exists (
    select 1 from public.notifications n
    where n.user_id = p_debtor_id
      and n.group_id = p_group_id
      and n.type = 'payment_reminder'
      and n.actor_id = v_me
      and n.created_at > now() - interval '1 hour'
  ) then
    return;
  end if;

  select name into v_group_name from public.groups where id = p_group_id;

  insert into public.notifications (user_id, group_id, actor_id, type, title, message)
  select p_debtor_id, p_group_id, v_me, 'payment_reminder', 'Payment Reminder',
         public.profile_display_name(v_me) || ' reminds you that you owe '
         || trim_scale(v_owed)::text || ' DKK in ' || v_group_name
  from public.profiles p
  where p.id = p_debtor_id and p.notifications_enabled;
end;
$function$;
REVOKE ALL ON FUNCTION public.send_payment_reminder(uuid, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.send_payment_reminder(uuid, uuid) TO authenticated;
