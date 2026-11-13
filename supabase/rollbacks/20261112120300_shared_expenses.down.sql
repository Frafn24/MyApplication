-- Down for migrations/20261112120300_shared_expenses.sql
BEGIN;

DROP FUNCTION IF EXISTS public.get_shared_expenses(uuid, uuid);

COMMIT;
