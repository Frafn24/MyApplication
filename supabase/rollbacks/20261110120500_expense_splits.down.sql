-- Down for migrations/20261110120500_expense_splits.sql
-- WARNING: deletes all expense splits.
BEGIN;

DROP FUNCTION IF EXISTS public.send_payment_reminder(uuid, uuid);
DROP FUNCTION IF EXISTS public.get_my_groups();
DROP FUNCTION IF EXISTS public.get_my_balances();
DROP TABLE IF EXISTS public.expense_splits;

COMMIT;
