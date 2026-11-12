# MyApplication

## Development

| Command                                                   | What it does                                  |
| --------------------------------------------------------- | --------------------------------------------- |
| `./gradlew ktlintCheck`                                   | Check code style (rules in `.editorconfig`)   |
| `./gradlew ktlintFormat`                                  | Auto-fix code style where possible            |
| `./gradlew testDebugUnitTest`                             | JVM unit tests (`app/src/test`)               |
| `./gradlew lintDebug`                                     | Android Lint (fails the build on errors)      |
| `./gradlew assembleDebug assembleDebugAndroidTest`        | Build the app and the instrumented tests      |
| `./gradlew connectedDebugAndroidTest`                     | Run instrumented/Compose UI tests on a device |

### Continuous integration

`.github/workflows/ci.yml` runs on every pull request to `master` and every push to
`master`: ktlint, unit tests, Android Lint, and a debug build of the app and the
instrumented tests. If a step fails, the reports in `app/build/reports/` are uploaded
as a workflow artifact. Run the same commands locally before opening a PR.

Instrumented tests need an emulator or device, so CI only builds them.

## Supabase Database

The app uses Supabase for authentication and the database. The schema is managed
with the [Supabase CLI](https://supabase.com/docs/guides/cli) and versioned as SQL
migrations, the same setup as in `mobile-app`.

```text
supabase/
├── config.toml     Supabase CLI config (local stack ports, auth settings)
├── migrations/     Schema changes ("up"), applied in timestamp order
└── rollbacks/      One "down" file per migration: <same name>.down.sql
```

### Up and down migrations

The Supabase CLI only knows "up" migrations and runs every `.sql` file in
`migrations/`, so the matching "down" files live in `rollbacks/`. Every new
migration should come with one:

```text
migrations/20261112120000_group_admins_members.sql        up
rollbacks/20261112120000_group_admins_members.down.sql    down
```

Down files undo their migration and run in one transaction. Several of them
delete data (marked `WARNING` at the top). To roll back, run them newest first
against the **local** database:

```bash
psql "postgresql://postgres:postgres@127.0.0.1:54322/postgres" \
  -f supabase/rollbacks/20261112120200_notifications_read_state.down.sql
```

Afterwards, delete or move the migration file before running `npx supabase db reset`. Otherwise the reset applies it again.

On the shared live database, never run a down file directly. That would leave the
migration history out of sync, and we don't run `migration repair` there. Instead,
create a new migration (`npx supabase migration new revert_<name>`), copy the
down file's contents into it, and push that.

### What each screen uses

| Screen                 | Tables / functions                                                                  |
| ---------------------- | ----------------------------------------------------------------------------------- |
| Sign up / Login        | `profiles`, `is_username_available()`                                               |
| Groups                 | `get_my_groups()`                                                                   |
| Create Group           | `groups` (`name`, `description`), `create_group()`, `add_group_member()` (by email) |
| Manage Members         | `group_members` (`role`), `get_group_members()`, `add_group_member()`, delete rows  |
| Add Expense            | `create_expense()` (equal split into `expense_splits`)                              |
| Expense Details / Edit | `get_group_expenses()`, `get_expense_details()`, `update_expense()`, `delete_expense()` (payer only) |
| Balances               | `get_my_balances()`, `send_payment_reminder()`                                      |
| Balances → details     | `get_shared_expenses()` (the expenses behind one balance)                           |
| Notifications          | `notifications` (`read_at`, `expense_id`), `mark_notifications_read()`              |
| Profile                | `profiles` (`full_name`, `phone`, `notifications_enabled`)                          |

### App configuration

The app reads two values at build time (exposed as `BuildConfig` fields):

| `local.properties` key    | Environment variable fallback |
| ------------------------- | ----------------------------- |
| `supabase.url`            | `SUPABASE_URL`                |
| `supabase.publishableKey` | `SUPABASE_PUBLISHABLE_KEY`    |

Copy the lines from `local.properties.example` into `local.properties` and fill in
your project's values. `local.properties` is git-ignored. Never add a Supabase
secret key to the app.

Use the shared client `SupabaseProvider.client`
(`app/src/main/java/com/example/myapplication/data/remote/SupabaseProvider.kt`)
instead of creating new clients.

### Working with migrations

```bash
npx supabase login
npx supabase link --project-ref jbnkvycqrijqbjmstner   # once per machine

npx supabase migration new <name>   # creates supabase/migrations/<timestamp>_<name>.sql
npx supabase migration list --remote   # compare local files with the live database
npx supabase db push --remote --dry-run # show what would be applied
npx supabase db push --remote          # applies new migrations to the linked project
```

Without `--remote`, these commands target the local database (`supabase start`), not the live one.

The live database is shared with `mobile-app`. Never change a migration that has already been pushed;
add a new one instead. Don't run `supabase migration repair` on it.

### Local database

Requires Docker.

```bash
npx supabase start      # local stack, applies all migrations
npx supabase db reset   # re-create the local database from the migrations
```

### Security

- Enable Row Level Security (RLS) on every new table and add policies for it.
- Never put a Supabase secret key in the app or commit credentials.
