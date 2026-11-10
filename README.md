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
└── migrations/     Schema changes, applied in timestamp order
```

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
