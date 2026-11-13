-- Members are added by email only (not by username).
-- Replaces add_group_member() from 20261112120000_group_admins_members.sql.
-- create_group() calls it, so its p_members are emails now too.
-- Rollback: supabase/rollbacks/20261112120400_add_member_by_email_only.down.sql

CREATE OR REPLACE FUNCTION public.add_group_member(p_group_id uuid, p_identifier text)
  RETURNS uuid
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_email text := lower(trim(p_identifier));
  v_user_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated' using errcode = '28000';
  end if;
  if not public.is_group_admin(p_group_id) then
    raise exception 'Only group admins can add members';
  end if;
  if v_email is null or v_email !~ '^[^@\s]+@[^@\s]+$' then
    raise exception 'Enter the email address of the person you want to add';
  end if;

  select u.id into v_user_id from auth.users u where lower(u.email) = v_email;

  if v_user_id is null then
    raise exception 'No user found with the email %', v_email;
  end if;
  if public.is_group_member(p_group_id, v_user_id) then
    raise exception '% is already in this group', public.profile_display_name(v_user_id);
  end if;

  insert into public.group_members (group_id, user_id, role)
  values (p_group_id, v_user_id, 'member');

  return v_user_id;
end;
$function$;
