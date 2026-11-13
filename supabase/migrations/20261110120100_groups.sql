-- groups: shown on the Groups screen.
-- Read access is added in the group_members migration (members only).
CREATE TABLE public.groups (
  id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  name       text        NOT NULL CHECK (char_length(trim(name)) BETWEEN 1 AND 100),
  created_by uuid        DEFAULT auth.uid() REFERENCES public.profiles (id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Opret gruppe" ON public.groups
  FOR INSERT TO authenticated
  WITH CHECK (created_by = (select auth.uid()));
CREATE POLICY "Opdater egen gruppe" ON public.groups
  FOR UPDATE TO authenticated
  USING (created_by = (select auth.uid()))
  WITH CHECK (created_by = (select auth.uid()));
CREATE POLICY "Slet egen gruppe" ON public.groups
  FOR DELETE TO authenticated
  USING (created_by = (select auth.uid()));

REVOKE ALL ON TABLE public.groups FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.groups TO authenticated;
