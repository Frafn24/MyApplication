-- Down for migrations/20261110120200_group_members.sql
-- WARNING: deletes all group memberships.
BEGIN;

DROP TRIGGER IF EXISTS on_group_created ON public.groups;
DROP FUNCTION IF EXISTS public.handle_new_group();

-- Policies on other tables that use the helper functions.
DROP POLICY IF EXISTS "Læs profiler i fælles grupper" ON public.profiles;
DROP POLICY IF EXISTS "Læs egne grupper" ON public.groups;

DROP TABLE IF EXISTS public.group_members;

DROP FUNCTION IF EXISTS public.shares_group_with(uuid);
DROP FUNCTION IF EXISTS public.is_group_member(uuid, uuid);

COMMIT;
