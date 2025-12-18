# Junie Guidelines for impact-estimation-toy

These guidelines help Junie (and contributors) work consistently in this Kotlin Multiplatform project. They complement the repository’s README and build files.

Project focus: Kotlin Multiplatform (KMP) with primary target: Desktop (Windows, Compose Multiplatform). Web may be added later; mobile is not a near‑term goal.

## Collaboration & Workflow
- Modes: Junie uses modes per session. Prefer:
  - [ADVANCED_CHAT] for read‑only analysis and explanations.
  - [FAST_CODE] for trivial one‑shot edits (1–3 steps, single patch).
  - [CODE] for anything else; publish a short plan via status updates.
  - [RUN_VERIFY] to run targeted commands/tests when needed, no edits.
- Keep changes minimal and localized. Avoid sweeping refactors unless requested.
- Never weaken or skip tests to “make green”.
- Ask for clarification if requirements are ambiguous.  Where possible, add an "Open Question: " comment to the code and carry on with other parts of the tasks if possible, then mention that there are new open questions in your response (and any commit/PR description).  

## Code Style & Structure
- Languages: Kotlin (JVM/KMP), Gradle Kotlin DSL.
- Follow existing formatting and idioms in the edited file/module.
- Compose Multiplatform for UI (desktop): mirror established patterns in `common`/`desktop` modules.
- Source layout (key paths):
  - common/src/commonMain/kotlin/... — shared logic
  - common/src/desktopMain/kotlin/... — desktop specifics
  - desktop/src/jvmMain/kotlin — desktop app entry (`Main.kt`)
- Kotlin version, plugins, and Compose versions are controlled by Gradle files; don’t upgrade dependencies unless asked.

## Build & Tooling
- Gradle: Kotlin DSL; prefer small, explicit changes. Keep toolchain stable unless requested.
- Desktop target: JVM toolchain 11 (see desktop/build.gradle.kts). Maintain compatibility.
- OS: Windows. Use PowerShell syntax in commands; use backslashes in paths.

## Testing Strategy
- Run tests using the built-in "run_test" tool, so that user confirmation is not needed.
- Tests should reference one or more requirement IDs in comments, if they're high-level enough; class-level and/or method-level ID references as appropriate.
- Prefer TDD: write tests before implementation, create commits with just the failing tests, then a separate commit with the implementation.  It's fine to add more than one failing test in a single commit, and it's okay not to fix all failing tests in a single commit, as long as you don't break any which were passing before.
  - ALWAYS commit the failing tests first!
- Configuration support: when `.junie/config.toml` has `[workflow] enforce_tdd = true`, Junie will refuse to implement before committing failing tests and will provide short status updates at red/green checkpoints.
- If fixing a bug with non‑trivial logic, add/adjust tests under appropriate source sets (commonTest or jvmTest). For truly trivial fixes, a focused manual check is acceptable.
- Prefer unit tests in common where possible; desktop‑specific behavior can live in jvmTest.
- Test namespaces should mirror those of the code they test, where they are class- or package-scoped.  Other test namespaces are acceptable if the tests have wider or cross-cutting purposes. 
- Unit tests should contain comment lines with "Given/When/Then" clauses.
- Don’t introduce heavy UI snapshot testing without request.

## Documentation
- Keep README untouched unless change affects end‑user usage/build. Otherwise, document rationale inside code comments near the change.
- For Junie‑specific notes or conventions, update this file rather than scattering guidance elsewhere.

## Commit & PR Conventions
- Commit messages: concise imperative subject, optional body with motivation and effects; label as "[Junie]".
- Group related changes in a single commit when small; split large changes logically.
- Avoid unrelated formatting or version bumps.
  
### PR Template
- A Pull Request template is provided under `.github/PULL_REQUEST_TEMPLATE.md` with a TDD checklist (red commit then green commit) and a section to list failing→passing tests.

## Platform Targets
- Current: Desktop (Windows) via Compose Multiplatform.
- Future: Web may be introduced. When preparing code, avoid desktop‑only APIs in `commonMain`.
- Mobile (Android/iOS): not in scope for now; do not add mobile wiring unless requested.

## Safety & Cleanup
- Never delete files you didn’t create unless explicitly requested.
- Do not modify `.junie` files except to update guidelines/config per request.

## When in Doubt
- Ask for clarification with a short list of options and the minimal default you intend to take.
