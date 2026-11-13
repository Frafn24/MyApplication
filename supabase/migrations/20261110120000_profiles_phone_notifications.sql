-- profiles: fields shown on the Profile screen.
ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS phone text,
  ADD COLUMN IF NOT EXISTS notifications_enabled boolean NOT NULL DEFAULT true;

ALTER TABLE public.profiles
  ADD CONSTRAINT profiles_phone_length
  CHECK (phone IS NULL OR char_length(phone) <= 30);

-- Display name used in notifications and balances.
CREATE OR REPLACE FUNCTION public.profile_display_name(p_user_id uuid)
  RETURNS text
  LANGUAGE sql
  STABLE
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
  select coalesce(nullif(p.full_name, ''), p.username, 'Someone')
  from public.profiles p
  where p.id = p_user_id;
$function$;
REVOKE ALL ON FUNCTION public.profile_display_name(uuid) FROM PUBLIC, anon, authenticated;
