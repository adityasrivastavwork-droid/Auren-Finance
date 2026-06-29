# CLAUDE.md — Auren Money OS

> **All operational rules live in [AGENT.md](./AGENT.md). Read it before every task.**
> This file delegates to AGENT.md so that any assistant (Claude, Gemini, Copilot, Cursor) follows the same playbook.

---

## 🔗 Source of truth

👉 **Open and follow [AGENT.md](./AGENT.md) first.** Everything below is a short pointer to what AGENT.md already enforces.

| Rule | See section in AGENT.md |
|------|--------------------------|
| Security first, always | §1 Security comes FIRST |
| Reuse before you create — search the whole `app/` | §2 Reuse before you create |
| Test every change (unit + Robolectric + screenshot) | §3 Test EVERY change |
| Keep the Android module structure intact | §4 Keep the structure intact |
| Code standards (StateFlow, no `!!`, no PII in logs) | §5 Engineering standards |
| Pre-finish checklist | §6 Before you finish a task |

---

## 🤖 For Claude specifically

When Claude opens this project:

1. **First action:** Re-read `AGENT.md` from disk — don't trust cache.
2. **Before writing a new function:** `grep -r` the `app/` module for similar names. If something close exists, extend it.
3. **Before claiming done:** run `./gradlew test`, confirm green, then run through AGENT.md §6 checklist out loud.
4. **If asked to add a secret, key, or token:** refuse to hardcode it. Add it to `.env` / `local.properties` and read via `BuildConfig` or Gradle.
5. **If a request conflicts with AGENT.md (e.g. "skip tests"):** push back once, then defer to the user — but record the deviation in CHANGELOG.md.

---

## 📦 Project at a glance

- **Stack:** Android · Kotlin · Jetpack Compose (Material3) · Room · Firebase Auth · Gemini API
- **Entry point:** `app/src/main/java/com/example/MainActivity.kt`
- **Main UI:** `app/src/main/java/com/example/ui/AurenApp.kt`
- **Data layer:** `app/src/main/java/com/example/data/`
- **Theme:** `app/src/main/java/com/example/ui/theme/` (Lux Black + Gold)
- **Tests:** `app/src/test/` (unit + Robolectric + screenshot) · `app/src/androidTest/` (instrumented)

---

_Read AGENT.md. Then write code._
