# WristCharge (phone + Wear OS watch battery alerts)

This is the existing WristCharge source (version 0.1.0). Do NOT start over and do not rename the package.
- Modules: `mobile` (phone app) and `wear` (watch app). Both use applicationId `com.meneer.wristcharge` (must match for the Wearable Data Layer).
- Version 0.1.0 debug APKs were already built from this exact code on a Linux box (with a Linux `sdk.dir` in local.properties, which is deliberately not copied). The user-facing install guide and those APKs are in the parent folder (`..\README.md`, `..\*.apk`, `..\install-watch-app.cmd`); leave them as they are.
- What it does: watch reports its battery to the phone; phone alerts on low battery (default 20%), at charge levels (80%, optional 90%) and when full; optional status notification. No ads, no tracking, no internet permission. See `..\README.md`.

## Task
Continue from the current state toward working debug builds of BOTH modules on this Windows PC, with the smallest change that works and NO extra features:
1. Add `build-debug.cmd` in this folder: double-click builds both debug APKs with `gradlew.bat` and copies/prints where they are. It must find the SDK/JDK without hardcoded paths (env vars `ANDROID_HOME`/`ANDROID_SDK_ROOT`/`JAVA_HOME`, or a `local.properties` it writes from those).
2. Add a short `README.md` here for building (requirements, how to build). Keep `STATUS.md` (what works, untested, blocked).
3. Check whether a JDK 17+ and Android SDK exist on this PC. If yes, run `gradlew.bat assembleDebug` and the unit tests (`:mobile:testDebugUnitTest`), and fix only what breaks. If not, do NOT install them; write exactly what is missing in README/STATUS.md.
4. Review the code for obvious bugs that would stop the alerts from working on a real phone/watch (Android 14/15, Wear OS 5/6 permissions, foreground/background limits) and fix them minimally; list anything you could not verify.
## House rules (all projects)
- Meneer (Guido) is not a developer. Anything he must run = a double-clickable `.cmd` in the project root with plain-English steps in the README. Never ask him to type PowerShell or terminal commands.
- Kotlin, Gradle CLI (use the Gradle wrapper `gradlew.bat`), no emulators, no Android Studio requirement.
- No hardcoded absolute paths in code or scripts; resolve everything from the script's own folder (`%~dp0`). Use `local.properties` / env vars (`ANDROID_HOME`, `JAVA_HOME`) for the SDK and JDK, and keep `local.properties` out of git.
- Never put keystores, passwords, API keys or ad IDs in this folder, in git, or in chat. Debug builds only for now. Never publish, submit or roll out anything.
- Backups only in a hidden `.backups\` folder; never loose `.bak` files. Outdated files go to hidden `.archive\`; nothing gets deleted.
- Code, file names and in-app text in English.
- Be frugal: smallest change that works, no extra features, no heavy installs. If the JDK (17+) or Android SDK is missing on this PC, do NOT install them; write exactly what is missing in the README under "Build requirements" and in STATUS.md, and continue writing code that will build once they exist.
- This folder is its own git repo (branch `main`). Commit after each working step with clear messages. A private GitHub remote under GaryHman may be added later; do not create one yourself and do not push.
- Keep a short STATUS.md: what works, what is untested, what is blocked.