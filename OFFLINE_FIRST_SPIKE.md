# Offline-first architecture spike

> **Status: Experimental / WIP**

This branch explores a possible offline-first architecture for the Mobile
Expense Sharing App. It is intended as a research and implementation starting
point for the database/backend work.

## Purpose

This branch demonstrates how the app could later support:

- local data with Room
- repositories between ViewModels and data sources
- local-first expense creation
- `PENDING`, `SYNCED` and `FAILED` sync states
- background synchronization with WorkManager
- a future Supabase integration

## Important status

This branch is **not production-ready** and must not be treated as a finished
offline implementation.

Known limitations:

- Room is not active in `MainActivity`; the default app container still uses mock repositories.
- Supabase synchronization is not implemented.
- `SyncExpensesWorker` is only a skeleton.
- Repository interfaces may change as the database design is finalized.
- The Room schema and migrations are provisional.
- Conflict resolution is not implemented.
- Complete `FAILED` handling and manual retry behavior are not implemented.
- Reliability and WorkManager tests are not implemented.

## Intended use

The branch is meant to give the database/backend team something concrete to
review and build on. The code should be evaluated as an architectural spike,
not as a completed feature.

The stable MVVM implementation remains on the `ayla` branch. This branch is
kept separate so the group can experiment with Room and WorkManager without
making the MVVM branch depend on unfinished offline functionality.

## Main components

```text
Compose UI
    ↓
ViewModel
    ↓
Repository
    ↓
Room database
    ↓
WorkManager worker
    ↓
Supabase (planned)
```

The UI should eventually read from Room. New expenses should be stored locally
first with a pending status, and the worker should synchronize them when a
network connection is available.

## Next implementation steps

1. Confirm the Room schema and repository contracts with the database/backend team.
2. Connect the worker to the actual Supabase API.
3. Implement correct retry and permanent failure handling.
4. Decide how conflicts are resolved.
5. Add WorkManager and airplane-mode tests.
6. Only merge the branch after the behavior has been reviewed and tested.
