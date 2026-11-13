-- Balances -> "View details" -> Expense Details: the expenses behind one balance.
--   * get_shared_expenses(): every expense in a group between the caller and one
--     other member, and how much each one moves the balance between them.
-- Uses the same rule as get_my_balances(), so the effects add up to the balance.
-- Rollback: supabase/rollbacks/20261112120300_shared_expenses.down.sql

-- effect > 0: the other member owes the caller this much for the expense.
-- effect < 0: the caller owes the other member.
CREATE OR REPLACE FUNCTION public.get_shared_expenses(p_group_id uuid, p_other_user_id uuid)
  RETURNS TABLE (
    id uuid,
    description text,
    amount numeric,
    paid_by_me boolean,
    effect numeric,
    created_at timestamptz
  )
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  select e.id,
         e.description,
         e.amount,
         e.paid_by = (select auth.uid()) as paid_by_me,
         case when e.paid_by = (select auth.uid()) then s.amount else -s.amount end as effect,
         e.created_at
  from public.expenses e
  join public.expense_splits s on s.expense_id = e.id
  where e.group_id = p_group_id
    and p_other_user_id <> (select auth.uid())
    and (
      (e.paid_by = (select auth.uid()) and s.user_id = p_other_user_id)
      or (e.paid_by = p_other_user_id and s.user_id = (select auth.uid()))
    )
  order by e.created_at desc;
$function$;
REVOKE ALL ON FUNCTION public.get_shared_expenses(uuid, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_shared_expenses(uuid, uuid) TO authenticated;
