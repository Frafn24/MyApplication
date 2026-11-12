-- Notifications screen: opening a notification.
--   * notifications.read_at: unread/read state
--   * notifications.expense_id: which expense a "New Expense" notification is about
--   * mark_notifications_read(): mark one, some or all as read
-- Rollback: supabase/rollbacks/20261112120200_notifications_read_state.down.sql

ALTER TABLE public.notifications
  ADD COLUMN read_at timestamptz,
  ADD COLUMN expense_id uuid REFERENCES public.expenses (id) ON DELETE SET NULL;

CREATE INDEX notifications_unread_idx ON public.notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX notifications_expense_id_idx ON public.notifications (expense_id);

-- Users may only change read_at, on their own notifications.
CREATE POLICY "Marker egne notifikationer som læst" ON public.notifications
  FOR UPDATE TO authenticated
  USING (user_id = (select auth.uid()))
  WITH CHECK (user_id = (select auth.uid()));
GRANT UPDATE (read_at) ON TABLE public.notifications TO authenticated;

-- p_ids NULL marks all of the caller's notifications as read.
-- Returns how many were marked.
CREATE OR REPLACE FUNCTION public.mark_notifications_read(p_ids uuid[] DEFAULT NULL)
  RETURNS integer
  LANGUAGE sql
  SET search_path TO ''
  AS $function$
  with marked as (
    update public.notifications
    set read_at = now()
    where user_id = (select auth.uid())
      and read_at is null
      and (p_ids is null or id = any (p_ids))
    returning 1
  )
  select count(*)::integer from marked;
$function$;
REVOKE ALL ON FUNCTION public.mark_notifications_read(uuid[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.mark_notifications_read(uuid[]) TO authenticated;

-- "New Expense" notifications now point to the expense.
CREATE OR REPLACE FUNCTION public.notify_expense_added()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_actor uuid := coalesce(new.created_by, new.paid_by);
begin
  insert into public.notifications (user_id, group_id, actor_id, expense_id, type, title, message)
  select gm.user_id, new.group_id, v_actor, new.id, 'expense_added', 'New Expense',
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
