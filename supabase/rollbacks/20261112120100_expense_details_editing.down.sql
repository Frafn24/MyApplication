-- Down for migrations/20261112120100_expense_details_editing.sql
-- Expenses and splits are kept; only the functions and updated_at are removed.
BEGIN;

DROP FUNCTION IF EXISTS public.get_expense_details(uuid);
DROP FUNCTION IF EXISTS public.get_group_expenses(uuid);
DROP FUNCTION IF EXISTS public.update_expense(uuid, text, numeric, uuid, uuid[]);
DROP FUNCTION IF EXISTS public.create_expense(uuid, text, numeric, uuid, uuid[]);
DROP FUNCTION IF EXISTS public.set_equal_split(uuid, uuid[]);

DROP TRIGGER IF EXISTS expenses_set_updated_at ON public.expenses;
ALTER TABLE public.expenses DROP COLUMN IF EXISTS updated_at;

COMMIT;
