-- group_members: who is in which group (member count on the Groups screen).
CREATE TABLE public.group_members (
  group_id  uuid        NOT NULL REFERENCES public.groups (id) ON DELETE CASCADE,
  user_id   uuid        NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
  joined_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (group_id, user_id)
);
CREATE INDEX group_members_user_id_idx ON public.group_members (user_id);

ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;

-- SECURITY DEFINER so policies can check membership without recursing into
-- group_members' own policies.
CREATE OR REPLACE FUNCTION public.is_group_member(p_group_id uuid, p_user_id uuid DEFAULT auth.uid())
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
  select exists (
    select 1 from public.group_members
    where group_id = p_group_id and user_id = p_user_id
  );
$function$;

CREATE OR REPLACE FUNCTION public.shares_group_with(p_user_id uuid)
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
  select exists (
    select 1
    from public.group_members mine
    join public.group_members theirs on theirs.group_id = mine.group_id
    where mine.user_id = (select auth.uid()) and theirs.user_id = p_user_id
  );
$function$;

REVOKE ALL ON FUNCTION public.is_group_member(uuid, uuid) FROM PUBLIC, anon;
REVOKE ALL ON FUNCTION public.shares_group_with(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_group_member(uuid, uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.shares_group_with(uuid) TO authenticated;

-- Members can see their groups (the creator also right after inserting it).
CREATE POLICY "Læs egne grupper" ON public.groups
  FOR SELECT TO authenticated
  USING (public.is_group_member(id) OR created_by = (select auth.uid()));

-- Members can see the names of people they share a group with (Balances screen).
CREATE POLICY "Læs profiler i fælles grupper" ON public.profiles
  FOR SELECT TO authenticated
  USING (public.shares_group_with(id));

CREATE POLICY "Læs medlemmer i egne grupper" ON public.group_members
  FOR SELECT TO authenticated
  USING (public.is_group_member(group_id));
CREATE POLICY "Opretter tilføjer medlemmer" ON public.group_members
  FOR INSERT TO authenticated
  WITH CHECK (
    exists (select 1 from public.groups g where g.id = group_id and g.created_by = (select auth.uid()))
  );
CREATE POLICY "Forlad gruppe eller fjern medlem" ON public.group_members
  FOR DELETE TO authenticated
  USING (
    user_id = (select auth.uid())
    OR exists (select 1 from public.groups g where g.id = group_id and g.created_by = (select auth.uid()))
  );

REVOKE ALL ON TABLE public.group_members FROM anon, authenticated;
GRANT SELECT, INSERT, DELETE ON TABLE public.group_members TO authenticated;

-- The creator of a group becomes its first member.
CREATE OR REPLACE FUNCTION public.handle_new_group()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
begin
  if new.created_by is not null then
    insert into public.group_members (group_id, user_id)
    values (new.id, new.created_by)
    on conflict do nothing;
  end if;
  return new;
end;
$function$;
REVOKE ALL ON FUNCTION public.handle_new_group() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER on_group_created
  AFTER INSERT ON public.groups
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_group();
