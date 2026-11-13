-- Down for migrations/20261110120400_expenses.sql
-- WARNING: deletes all expenses.
BEGIN;

DROP TRIGGER IF EXISTS on_expense_added ON public.expenses;
DROP FUNCTION IF EXISTS public.notify_expense_added();
DROP TABLE IF EXISTS public.expenses;

COMMIT;
