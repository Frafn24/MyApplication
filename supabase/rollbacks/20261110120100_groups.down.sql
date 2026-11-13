-- Down for migrations/20261110120100_groups.sql
-- WARNING: deletes all groups.
BEGIN;

DROP TABLE IF EXISTS public.groups;

COMMIT;
