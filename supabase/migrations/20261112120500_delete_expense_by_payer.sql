-- Expense Details: "Delete Expense", only for the person who paid the expense.
--   * The delete policy on expenses now checks paid_by instead of created_by.
--   * delete_expense(): deletes and tells the app why if it isn't allowed.
-- Splits are deleted with the expense (ON DELETE CASCADE); notifications about it
-- keep their text and lose the link (expense_id ON DELETE SET NULL).
-- Rollback: supabase/rollbacks/20261112120500_delete_expense_by_payer.down.sql

DROP POLICY "Slet egen udgift" ON public.expenses;
CREATE POLICY "Betaler sletter udgift" ON public.expenses
  FOR DELETE TO authenticated
  USING (paid_by = (select auth.uid()));

-- SECURITY INVOKER: the policy above decides what may be deleted.
CREATE OR REPLACE FUNCTION public.delete_expense(p_expense_id uuid)
  RETURNS void
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;

  delete from public.expenses where id = p_expense_id;

  if not found then
    raise exception 'Only the person who paid this expense can delete it';
  end if;
end;
$function$;
REVOKE ALL ON FUNCTION public.delete_expense(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_expense(uuid) TO authenticated;
