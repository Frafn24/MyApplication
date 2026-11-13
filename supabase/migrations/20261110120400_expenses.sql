-- expenses: an amount paid by one member of a group. Source of all balances.
CREATE TABLE public.expenses (
  id          uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id    uuid           NOT NULL REFERENCES public.groups (id) ON DELETE CASCADE,
  paid_by     uuid           NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
  description text           NOT NULL CHECK (char_length(trim(description)) BETWEEN 1 AND 200),
  amount      numeric(12, 2) NOT NULL CHECK (amount > 0),
  created_by  uuid           DEFAULT auth.uid() REFERENCES public.profiles (id) ON DELETE SET NULL,
  created_at  timestamptz    NOT NULL DEFAULT now()
);
CREATE INDEX expenses_group_id_idx ON public.expenses (group_id);
CREATE INDEX expenses_paid_by_idx ON public.expenses (paid_by);

ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Læs udgifter i egne grupper" ON public.expenses
  FOR SELECT TO authenticated
  USING (public.is_group_member(group_id));
CREATE POLICY "Opret udgift i egen gruppe" ON public.expenses
  FOR INSERT TO authenticated
  WITH CHECK (
    created_by = (select auth.uid())
    AND public.is_group_member(group_id)
    AND public.is_group_member(group_id, paid_by)
  );
CREATE POLICY "Opdater egen udgift" ON public.expenses
  FOR UPDATE TO authenticated
  USING (created_by = (select auth.uid()))
  WITH CHECK (
    created_by = (select auth.uid())
    AND public.is_group_member(group_id)
    AND public.is_group_member(group_id, paid_by)
  );
CREATE POLICY "Slet egen udgift" ON public.expenses
  FOR DELETE TO authenticated
  USING (created_by = (select auth.uid()));

REVOKE ALL ON TABLE public.expenses FROM anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.expenses TO authenticated;

-- "New Expense": notify the other members when an expense is added.
CREATE OR REPLACE FUNCTION public.notify_expense_added()
  RETURNS TRIGGER
  LANGUAGE plpgsql
  SECURITY DEFINER
  SET search_path TO ''
  AS $function$
declare
  v_actor uuid := coalesce(new.created_by, new.paid_by);
begin
  insert into public.notifications (user_id, group_id, actor_id, type, title, message)
  select gm.user_id, new.group_id, v_actor, 'expense_added', 'New Expense',
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
REVOKE ALL ON FUNCTION public.notify_expense_added() FROM PUBLIC, anon, authenticated;

CREATE TRIGGER on_expense_added
  AFTER INSERT ON public.expenses
  FOR EACH ROW
  EXECUTE FUNCTION public.notify_expense_added();
