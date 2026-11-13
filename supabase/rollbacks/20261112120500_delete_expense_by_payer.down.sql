-- Down for migrations/20261112120500_delete_expense_by_payer.sql
-- Expenses already deleted are not restored.
BEGIN;

DROP FUNCTION IF EXISTS public.delete_expense(uuid);

DROP POLICY IF EXISTS "Betaler sletter udgift" ON public.expenses;
CREATE POLICY "Slet egen udgift" ON public.expenses
  FOR DELETE TO authenticated
  USING (created_by = (select auth.uid()));

COMMIT;
