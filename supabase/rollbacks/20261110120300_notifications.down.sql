-- Down for migrations/20261110120300_notifications.sql
-- WARNING: deletes all notifications.
BEGIN;

DROP TRIGGER IF EXISTS on_group_member_added ON public.group_members;
DROP FUNCTION IF EXISTS public.notify_member_joined();
DROP TABLE IF EXISTS public.notifications;

COMMIT;
