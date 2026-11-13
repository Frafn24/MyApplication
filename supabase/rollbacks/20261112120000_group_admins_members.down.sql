-- Down for migrations/20261112120000_group_admins_members.sql
-- Deletes group descriptions and member roles. Groups and members are kept.
BEGIN;

DROP FUNCTION IF EXISTS public.get_group_members(uuid);
DROP FUNCTION IF EXISTS public.create_group(text, text, text[]);
DROP FUNCTION IF EXISTS public.add_group_member(uuid, text);

DROP TRIGGER IF EXISTS on_group_member_removed ON public.group_members;
DROP FUNCTION IF EXISTS public.ensure_group_admin();

-- Previous version of handle_new_group (without role).
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

-- group_members: back to creator-based policies.
DROP POLICY IF EXISTS "Forlad gruppe eller admin fjerner medlem" ON public.group_members;
DROP POLICY IF EXISTS "Admin tilføjer medlemmer" ON public.group_members;
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

-- groups: back to creator-based policies and table-wide UPDATE.
REVOKE UPDATE (name, description) ON TABLE public.groups FROM authenticated;
GRANT UPDATE ON TABLE public.groups TO authenticated;
DROP POLICY IF EXISTS "Admin sletter gruppe" ON public.groups;
DROP POLICY IF EXISTS "Admin opdaterer gruppe" ON public.groups;
CREATE POLICY "Opdater egen gruppe" ON public.groups
  FOR UPDATE TO authenticated
  USING (created_by = (select auth.uid()))
  WITH CHECK (created_by = (select auth.uid()));
CREATE POLICY "Slet egen gruppe" ON public.groups
  FOR DELETE TO authenticated
  USING (created_by = (select auth.uid()));

DROP FUNCTION IF EXISTS public.is_group_admin(uuid, uuid);

ALTER TABLE public.group_members DROP COLUMN IF EXISTS role;
ALTER TABLE public.groups DROP COLUMN IF EXISTS description;

COMMIT;
