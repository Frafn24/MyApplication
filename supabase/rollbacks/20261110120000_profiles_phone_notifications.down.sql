-- Down for migrations/20261110120000_profiles_phone_notifications.sql
-- WARNING: deletes phone numbers and notification settings from profiles.
BEGIN;

DROP FUNCTION IF EXISTS public.profile_display_name(uuid);
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_phone_length;
ALTER TABLE public.profiles
  DROP COLUMN IF EXISTS notifications_enabled,
  DROP COLUMN IF EXISTS phone;

COMMIT;
