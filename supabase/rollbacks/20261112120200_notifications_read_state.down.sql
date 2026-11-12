-- Down for migrations/20261112120200_notifications_read_state.sql
-- Deletes read state and the expense link on notifications.
BEGIN;

-- Previous version of notify_expense_added (without expense_id).
CREATE OR REPLACE FUNCTION public.notify_expense_added()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_actor uuid := coalesce(new.created_by, new.paid_by);
begin
  insert into public.notifications (user_id, group_id, actor_id, type, title, message)
  select gm.user_id, new.group_id, v_actor, 'expense_added', 'New Expense',
         public.profile_display_name(v_actor) || ' added ' || new.description
         || ' - ' || trim_scale(new.amount)::text || ' DKK'
  from public.group_members gm
  join public.profiles p on p.id = gm.user_id
  where gm.group_id = new.group_id
    and gm.user_id <> v_actor
    and p.notifications_enabled;

  return new;
end;
$function$;

DROP FUNCTION IF EXISTS public.mark_notifications_read(uuid[]);

REVOKE UPDATE (read_at) ON TABLE public.notifications FROM authenticated;
DROP POLICY IF EXISTS "Marker egne notifikationer som læst" ON public.notifications;

DROP INDEX IF EXISTS public.notifications_expense_id_idx;
DROP INDEX IF EXISTS public.notifications_unread_idx;

ALTER TABLE public.notifications
  DROP COLUMN IF EXISTS expense_id,
  DROP COLUMN IF EXISTS read_at;

COMMIT;
