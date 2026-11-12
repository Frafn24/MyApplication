-- Down for migrations/20261026121540_remote_schema.sql
-- WARNING: deletes all profiles. New sign-ups no longer get a profile.
BEGIN;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
DROP FUNCTION IF EXISTS public.handle_new_user();
DROP TABLE IF EXISTS public.profiles;

COMMIT;
