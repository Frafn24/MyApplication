-- Down for migrations/20261102120000_fix_profiles_signup.sql
--
-- Not reverted on purpose:
--   * the least-privilege grants on profiles. The old grants gave anon
--     TRUNCATE on profiles (not covered by RLS), which must not come back.
--   * the backfilled profile rows (they cannot be told apart from others).
BEGIN;

ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_username_format;

DROP TRIGGER IF EXISTS profiles_set_updated_at ON public.profiles;
DROP FUNCTION IF EXISTS public.set_updated_at();
DROP FUNCTION IF EXISTS public.is_username_available(text);

-- Original handle_new_user (only full_name).
CREATE OR REPLACE FUNCTION public.handle_new_user()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
begin
  insert into public.profiles (id, full_name)
  values (new.id, new.raw_user_meta_data ->> 'full_name');
  return new;
end;
$function$;

COMMIT;
