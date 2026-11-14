# AGENTS.md — Kiraya Diary (for Codex)

**App:** Kiraya Diary: Rent Manager
**Package:** `com.kiraya.diary`
**What it does:** An offline rent manager for landlords. It tracks properties, tenants, monthly rent, bills, payments and receipts.
**Stack:** Kotlin, Jetpack Compose, Material 3, MVVM + Clean Architecture, Hilt, Room (KSP), Coroutines/Flow, Navigation Compose, DataStore, WorkManager.

## Rules
1. **Only write code. Do NOT run Gradle or build the app** (no `./gradlew`, no assemble, no tests run). Write the Gradle files (version catalog, wrapper config, build scripts), but do not run them. The user will build in Android Studio.
2. **Every screen and every feature gets its own new branch.** For each item in the list below:
   - `git checkout main && git pull`
   - `git checkout -b feature/<name>` (branch names are given below)
   - write the code → `git add . && git commit -m "feat(<name>): <short summary>"`
   - `git push -u origin feature/<name>`
   - merge into `main` and push `main`, so the next branch starts from the latest code.
3. **If anything is unclear, ask the user first.** Use short, simple English and give options. Always ask before adding a library that is not listed here, and if the git remote is missing.
4. **Fully offline.** Never add the INTERNET permission.
5. Money is stored as a `Long` in paisa. Dates are stored as epoch millis.
6. All text goes in `strings.xml`: English in `values/`, Urdu in `values-ur/`. Support RTL, light theme and dark theme.
7. `minSdk 24`, and use the latest stable versions for the rest. Never commit keystores or `local.properties`.

## Branches (do them in this order)

| # | Branch | What to build |
|---|--------|---------------|
| 1 | `feature/project-setup` | Gradle files, version catalog, Hilt app class, package structure (core/data/domain/ui/di) |
| 2 | `feature/theme-navigation` | M3 theme (light/dark), Navigation Compose routes, bottom bar (Home, Tenants, Reports, Settings) |
| 3 | `feature/database` | Room: Property, Unit (flat/shop/room), Tenant, Lease, RentEntry, Payment, Bill, Expense. DAOs, repositories, use cases |
| 4 | `feature/home-dashboard` | This month: total rent due, collected, pending, overdue tenants, and quick actions |
| 5 | `feature/properties` | Add/edit/delete properties and their units, with vacant/occupied status |
| 6 | `feature/tenants` | Tenant list + detail: name, phone, CNIC photo, unit, rent amount, advance/security deposit, start date |
| 7 | `feature/lease` | Lease start/end date, yearly rent increase %, and an agreement photo |
| 8 | `feature/monthly-rent` | Rent entries made automatically every month (WorkManager), with paid/partial/unpaid status |
| 9 | `feature/payments` | Record full or partial payments (cash/bank/JazzCash note), and a payment history per tenant |
| 10 | `feature/bills-split` | Bijli/gas/water bills: add a bill, split it between tenants or units, and add it to their dues |
| 11 | `feature/receipts` | Rent receipt PDF (PdfDocument), shared via WhatsApp/share sheet with FileProvider |
| 12 | `feature/reminders` | Rent due notifications, a WhatsApp reminder message (Urdu/English), and a lease-ending alert |
| 13 | `feature/expenses` | Property expenses (repair, maintenance) and a profit view |
| 14 | `feature/reports` | Monthly/yearly income, a per-property report, a pending dues list, and PDF/CSV export |
| 15 | `feature/move-out` | Tenant move-out: final settlement, deposit return/deduction, and the unit marked vacant |
| 16 | `feature/backup-restore` | Offline backup file (DB + photos zip) through SAF, and restore |
| 17 | `feature/app-lock` | PIN + biometric lock, and a hide-amounts toggle |
| 18 | `feature/settings-onboarding` | Language (EN/UR), theme, currency format, 3-screen onboarding |
| 19 | `feature/polish-release` | App icon, splash, empty states, R8 rules, signing config read from `keystore.properties` |

After each branch, tell the user in 2–3 lines what was done and the branch name.
