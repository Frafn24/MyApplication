-- Down for migrations/20261112120400_add_member_by_email_only.sql
-- Restores add_group_member() accepting an email or a username.
BEGIN;

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

COMMIT;
