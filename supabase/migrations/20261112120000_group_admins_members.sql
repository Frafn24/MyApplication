-- Create Group + Manage Members screens.
--   * groups.description (Create Group "Description" field)
--   * group_members.role: 'admin' / 'member' ("Admin" label on Manage Members)
--   * Admins (not only the original creator) manage the group and its members
--   * add_group_member(): "+ Add Member" by email or username
--   * create_group(): create a group with description and members in one call
--   * get_group_members(): member list with names for Manage Members
-- Rollback: supabase/rollbacks/20261112120000_group_admins_members.down.sql

-- groups.description
ALTER TABLE public.groups
  ADD COLUMN description text
  CONSTRAINT groups_description_length CHECK (description IS NULL OR char_length(description) <= 500);

-- group_members.role
ALTER TABLE public.group_members
  ADD COLUMN role text NOT NULL DEFAULT 'member'
  CONSTRAINT group_members_role_check CHECK (role IN ('admin', 'member'));

-- Existing groups: the creator is admin.
UPDATE public.group_members gm
SET role = 'admin'
FROM public.groups g
WHERE g.id = gm.group_id AND g.created_by = gm.user_id;

-- Groups whose creator is gone: the longest-standing member becomes admin.
UPDATE public.group_members gm
SET role = 'admin'
FROM (
  SELECT DISTINCT ON (m.group_id) m.group_id, m.user_id
  FROM public.group_members m
  WHERE NOT EXISTS (
    SELECT 1 FROM public.group_members a WHERE a.group_id = m.group_id AND a.role = 'admin'
  )
  ORDER BY m.group_id, m.joined_at, m.user_id
) first_member
WHERE gm.group_id = first_member.group_id AND gm.user_id = first_member.user_id;

-- SECURITY DEFINER for the same reason as is_group_member: no policy recursion.
CREATE OR REPLACE FUNCTION public.is_group_admin(p_group_id uuid, p_user_id uuid DEFAULT auth.uid())
  RETURNS boolean
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
  select exists (
    select 1 from public.group_members
    where group_id = p_group_id and user_id = p_user_id and role = 'admin'
  );
$function$;
REVOKE ALL ON FUNCTION public.is_group_admin(uuid, uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.is_group_admin(uuid, uuid) TO authenticated;

-- groups: admins update/delete instead of only the creator.
DROP POLICY "Opdater egen gruppe" ON public.groups;
DROP POLICY "Slet egen gruppe" ON public.groups;
CREATE POLICY "Admin opdaterer gruppe" ON public.groups
  FOR UPDATE TO authenticated
  USING (public.is_group_admin(id))
  WITH CHECK (public.is_group_admin(id));
CREATE POLICY "Admin sletter gruppe" ON public.groups
  FOR DELETE TO authenticated
  USING (public.is_group_admin(id));

-- Only name and description can be edited (not created_by / created_at).
REVOKE UPDATE ON TABLE public.groups FROM authenticated;
GRANT UPDATE (name, description) ON TABLE public.groups TO authenticated;

-- group_members: admins add members; anyone can leave; admins remove non-admins.
DROP POLICY "Opretter tilføjer medlemmer" ON public.group_members;
DROP POLICY "Forlad gruppe eller fjern medlem" ON public.group_members;
CREATE POLICY "Admin tilføjer medlemmer" ON public.group_members
  FOR INSERT TO authenticated
  WITH CHECK (public.is_group_admin(group_id));
CREATE POLICY "Forlad gruppe eller admin fjerner medlem" ON public.group_members
  FOR DELETE TO authenticated
  USING (
    user_id = (select auth.uid())
    OR (role = 'member' AND public.is_group_admin(group_id))
  );

-- The creator of a group becomes its first member, as admin.
CREATE OR REPLACE FUNCTION public.handle_new_group()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
begin
  if new.created_by is not null then
    insert into public.group_members (group_id, user_id, role)
    values (new.id, new.created_by, 'admin')
    on conflict (group_id, user_id) do update set role = 'admin';
  end if;
  return new;
end;
$function$;

-- A group never ends up without an admin while it still has members
-- (last admin leaves, or their account is deleted).
CREATE OR REPLACE FUNCTION public.ensure_group_admin()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
begin
  if old.role = 'admin' and not exists (
    select 1 from public.group_members
    where group_id = old.group_id and role = 'admin'
  ) then
    update public.group_members
    set role = 'admin'
    where (group_id, user_id) = (
      select group_id, user_id from public.group_members
      where group_id = old.group_id
      order by joined_at, user_id
      limit 1
    );
  end if;
  return null;
end;
$function$;
REVOKE ALL ON FUNCTION public.ensure_group_admin() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER on_group_member_removed
  AFTER DELETE ON public.group_members
  FOR EACH ROW
  EXECUTE FUNCTION public.ensure_group_admin();

-- "+ Add Member": add a user by email or username. Admins only.
-- Returns the added user's id. Errors use P0001 so the app can show the message.
CREATE OR REPLACE FUNCTION public.add_group_member(p_group_id uuid, p_identifier text)
  RETURNS uuid
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_identifier text := lower(trim(p_identifier));
  v_user_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if not public.is_group_admin(p_group_id) then
    raise exception 'Only group admins can add members';
  end if;
  if v_identifier is null or v_identifier = '' then
    raise exception 'Enter an email or username';
  end if;

  if position('@' in v_identifier) > 1 then
    select u.id into v_user_id from auth.users u where lower(u.email) = v_identifier;
  else
    select p.id into v_user_id from public.profiles p where p.username = ltrim(v_identifier, '@');
  end if;

  if v_user_id is null then
    raise exception 'No user found with that email or username';
  end if;
  if public.is_group_member(p_group_id, v_user_id) then
    raise exception '% is already in this group', public.profile_display_name(v_user_id);
  end if;

  insert into public.group_members (group_id, user_id, role)
  values (p_group_id, v_user_id, 'member');

  return v_user_id;
end;
$function$;
REVOKE ALL ON FUNCTION public.add_group_member(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.add_group_member(uuid, text) TO authenticated;

-- "Create Group": the group, its description and the added members in one
-- transaction. p_members holds emails or usernames. Returns the new group's id.
CREATE OR REPLACE FUNCTION public.create_group(
  p_name text,
  p_description text DEFAULT NULL,
  p_members text[] DEFAULT '{}'
)
  RETURNS uuid
  LANGUAGE plpgsql
  SET search_path TO ''
  AS $function$
declare
  v_group_id uuid;
  v_member text;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if char_length(trim(coalesce(p_name, ''))) not between 1 and 100 then
    raise exception 'The group name must be between 1 and 100 characters';
  end if;

  insert into public.groups (name, description, created_by)
  values (trim(p_name), nullif(trim(p_description), ''), auth.uid())
  returning id into v_group_id;

  foreach v_member in array coalesce(p_members, '{}') loop
    if nullif(trim(v_member), '') is not null then
      perform public.add_group_member(v_group_id, v_member);
    end if;
  end loop;

  return v_group_id;
end;
$function$;
REVOKE ALL ON FUNCTION public.create_group(text, text, text[]) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_group(text, text, text[]) TO authenticated;

-- Manage Members: members of a group the caller belongs to, admins first.
CREATE OR REPLACE FUNCTION public.get_group_members(p_group_id uuid)
  RETURNS TABLE (user_id uuid, name text, username text, is_admin boolean, is_me boolean, joined_at timestamptz)
  LANGUAGE sql
  STABLE
  SET search_path TO ''
  AS $function$
  select gm.user_id,
         coalesce(nullif(p.full_name, ''), p.username, 'Unknown') as name,
         p.username,
         gm.role = 'admin' as is_admin,
         gm.user_id = (select auth.uid()) as is_me,
         gm.joined_at
  from public.group_members gm
  left join public.profiles p on p.id = gm.user_id
  where gm.group_id = p_group_id
    and public.is_group_member(p_group_id)
  order by gm.role = 'admin' desc, name;
$function$;
REVOKE ALL ON FUNCTION public.get_group_members(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_group_members(uuid) TO authenticated;
