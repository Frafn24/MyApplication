-- notifications: shown on the Notifications screen.
-- Rows are only written by database functions, never directly by the app.
CREATE TABLE public.notifications (
  id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid        NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
  group_id   uuid        REFERENCES public.groups (id) ON DELETE SET NULL,
  actor_id   uuid        REFERENCES public.profiles (id) ON DELETE SET NULL,
  type       text        NOT NULL CHECK (type IN ('expense_added', 'member_joined', 'payment_reminder')),
  title      text        NOT NULL,
  message    text        NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX notifications_user_id_created_at_idx ON public.notifications (user_id, created_at DESC);

ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Læs egne notifikationer" ON public.notifications
  FOR SELECT TO authenticated
  USING (user_id = (select auth.uid()));
CREATE POLICY "Slet egne notifikationer" ON public.notifications
  FOR DELETE TO authenticated
  USING (user_id = (select auth.uid()));

REVOKE ALL ON TABLE public.notifications FROM anon, authenticated;
GRANT SELECT, DELETE ON TABLE public.notifications TO authenticated;

-- "Group Updated": notify the other members when someone joins a group.
CREATE OR REPLACE FUNCTION public.notify_member_joined()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_group_name text;
begin
  select name into v_group_name from public.groups where id = new.group_id;

  insert into public.notifications (user_id, group_id, actor_id, type, title, message)
  select gm.user_id, new.group_id, new.user_id, 'member_joined', 'Group Updated',
         public.profile_display_name(new.user_id) || ' joined ' || v_group_name
  from public.group_members gm
  join public.profiles p on p.id = gm.user_id
  where gm.group_id = new.group_id
    and gm.user_id <> new.user_id
    and p.notifications_enabled;

  return new;
end;
$function$;
REVOKE ALL ON FUNCTION public.notify_member_joined() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER on_group_member_added
  AFTER INSERT ON public.group_members
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_member_joined();
