# AGENT.md — Auren Money OS

Operational rules for any AI agent (Claude, Copilot, Gemini, etc.) working on this Android Kotlin / Jetpack Compose project.

---

## 🛡️ 1. Security comes FIRST. Always.

Before correctness, before performance, before "clean code" — **security is priority #1**.

- **Never** hardcode API keys, secrets, tokens, or credentials. Use `.env`, `local.properties`, BuildConfig, or Android Keystore.
- **Never** commit `.env`, `debug.keystore`, `local.properties`, or `google-services.json` with real values.
- Validate **all** user input on every entry point (forms, deeplinks, intents, IPC).
- Use **parameterized queries** in Room DAOs. Never string-concatenate SQL.
- For Gemini / network calls: enforce HTTPS only, certificate pinning where viable, no PII in logs.
- Sensitive data (salary, balance, debts) must live in encrypted Room or EncryptedSharedPreferences — **never** plain `SharedPreferences`.
- Auth state (`firebase_auth_prefs`) should move to EncryptedSharedPreferences.
- Run static analysis (lint, detekt) and check for OWASP MASVS violations before any merge.
- Treat every external input as hostile until proven safe.

---

## ♻️ 2. Reuse before you create.

**Before writing any new function, class, composable, or extension:**

1. `grep -r` the **entire** `app/` module for similar names, behaviour, signatures.
2. Check `FinanceRepository.kt`, `FinanceViewModel.kt`, `AurenApp.kt`, `theme/` first — most utilities already live there.
3. If a similar function exists with 70%+ overlap → **extend/refactor it**, don't fork it.
4. If reuse needs a parameter — add the parameter; don't create a second copy.

Duplicate logic is a bug. Two implementations drift.

---

## ✅ 3. Test EVERY change.

No commit, no PR, no "quick fix" ships without tests being run.

- **Unit tests** (`app/src/test/`) — Robolectric for Android-dependent logic, plain JUnit for pure Kotlin.
- **Instrumented tests** (`app/src/androidTest/`) — for DB, integration, real-device behaviour.
- **Screenshot tests** (`GreetingScreenshotTest.kt` pattern) — for Composable changes; regenerate baselines only when a UI change is intentional.
- Run before every change: `./gradlew test` and `./gradlew connectedAndroidTest` (when possible).
- If a bug is fixed → write a regression test that fails on the old code and passes on the new.
- New feature → minimum: 1 unit test for ViewModel logic + 1 screenshot test for Composable.

---

## 🏗️ 4. Keep the structure intact.

The project is laid out as a standard Android Gradle module. Do **not** restructure:

```
Auren/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt
│   │   ├── data/        ← Room, Repository, Gemini
│   │   └── ui/          ← Composables, ViewModel, theme
│   ├── src/main/res/
│   ├── src/test/        ← Unit + Robolectric + screenshot
│   └── src/androidTest/ ← Instrumented
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

If a new layer is genuinely needed (e.g. `domain/`, `di/`) — propose it, don't just add it.

---

## 📐 5. Engineering standards

- **Order of priorities:** Security → Correctness → Tests → Readability → Performance → Style.
- All new public functions get KDoc.
- All ViewModels expose `StateFlow`, not `LiveData`.
- All side-effects in Composables go through `LaunchedEffect` / `rememberCoroutineScope`.
- No `!!` on nullable types in production code — use `?:`, `requireNotNull`, or a sealed result.
- No `runBlocking` on the main thread.
- Logging: never log PII, salary, or balances. Use Timber-style tags.
- Update `CHANGELOG.md` for every behaviour change.

---

## 🚦 6. Before you finish a task — checklist

- [ ] Did I search the codebase for existing utilities I could reuse?
- [ ] Did I run `./gradlew test` and confirm green?
- [ ] Did I add/update at least one test for what I changed?
- [ ] Did I review my diff for hardcoded secrets, logging of PII, or unencrypted storage?
- [ ] Did I keep the project structure unchanged?
- [ ] Did I update CHANGELOG.md / docs if behaviour changed?

If any box is unchecked → the task is not done.

---

_Security first. Reuse second. Tests always._
