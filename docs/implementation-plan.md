# Impact Estimation Tool — Design & Implementation Plan

This plan outlines staged implementation for the Impact Estimation Tool (IET) focusing on Desktop (Windows) via Compose Multiplatform. It follows the suggested order: models → storage → UI. No code is included here.

## Stage 0 — Foundations & Project Wiring
- [x] Confirm Kotlin Multiplatform targets: common + desktop (JVM). Remove the Android target completely for now.
  - Android app module excluded in settings.gradle.kts; common module has no Android target configured.
- [x] Confirm JVM toolchain (21) and Compose versions are consistent across modules.
  - jvmToolchain(21) set in :common and :desktop; Compose and Kotlin versions centralized via gradle.properties.
- [x] Establish module boundaries/namespaces for model and storage under `commonMain` where possible.
  - Created packages: `org.tameter.iet.model` and `org.tameter.iet.storage` with Stage 0 docs.
- [x] Decide DI/locator approach (simple service provider vs. manual wiring).
  - Decision: Simple `ServiceLocator` singleton in commonMain (manual registration); no external DI.
- [x] Define error-handling strategy (validation errors vs. domain invariants vs. IO errors).
  - Added `DomainError` sealed interface with `ValidationError`, `InvariantViolation`, and `IoError` in commonMain.
- [x] Define number format and locale handling for numeric inputs/outputs.
  - Added `NumberPolicy` (decimal separator abstraction, default 2 fraction digits for percentages). Platform-specific formatting deferred to later stages.
- [x] Define unit testing strategy and locations (commonTest, jvmTest) per guidelines.
  - Added `Stage0ScaffoldingTest` under commonTest validating locator and number policy; future model tests will use commonTest, desktop UI specifics under jvmTest.
- [x] Add lightweight sample data set for manual testing.
  - Added `docs/examples/sample-iet.json` with minimal plausible content.

## Stage 1 — Domain Model (IET/Model)

Goal: Implement core domain entities and computations suitable for UI binding.

- [x] Entities: `Requirement` (id/name, unit, type: Performance|Resource).
- [x] Requirement specs:
  - [x] Performance: `current`, `goal` (both numeric; any value allowed).
  - [x] Resource: `budget` (numeric; must be positive).
- [x] Entities: `DesignIdea` (id/name, description optional).
- [x] Entity: `Estimation` (estimatedValue, optional confidenceRange which, for now, is a symmetric absolute delta).
- [x] Table Aggregates: container that maps `QualityRequirement` × `DesignIdea` → `Estimation`.
- [x] Computation: Impact percentage for an estimation
  - [x] Performance: relative to the `current → goal` delta.
  - [x] Resource: relative to the `0 → budget` range.
- [x] Computation: Totals per group (Performance Totals, Resource Totals) across rows per column.
- [x] Computation: Performance-to-Cost Ratio per DesignIdea (TotalPerformance% / TotalResource%).
- [x] Validation: 
  - [x] Goal ≠ Current for Performance rows (to avoid divide-by-zero). If equal, flag validation error and exclude from totals, highlighting both the invalid cell and the column total.
  - [x] Where the TotalResource% is 0%, the Performance-to-Cost Ratio is undefined and should be displayed as N/A, and excluded from ordering.
  - [x] Resource values are minimized; minimum legal value is zero.
  - [x] Estimated resulting value may be below current or above goal for Performance; ≥ 0 for Resource.
  - [x] ConfidenceRange is non-negative and interpretable on same scale.
- [ ] Domain services: 
  - [x] Recalculation service that recomputes all dependent outputs on any change.
  - [ ] Simple change-logging hooks to support undo/redo later.
- [x] Data structures prepared for stable ordering (preserve insertion order for rows/columns).
- [x] Bridge-friendly API: observable or callback-based change notifications for UI recomposition.

## Stage 2 — Model–UI Bridge

Goal: Ensure the model is UI-ready. If pure common model lacks observability, provide a bridge in desktop layer.

- [x] Use Kotlin `SharedFlow`/`MutableSharedFlow` in common as the mechanism of observability.
- [x] Define events: row/column added, removed, reordered; cell edited; metadata changed; recompute complete.
  - Note: Event types are defined and `CellEdited` is emitted on edits; `RecomputeComplete` is emitted after cell edits. Emission after structural operations (add/remove/reorder) is not implemented yet (now covered by new failing tests).
- [x] Provide stable identifiers for rows and columns to correlate UI cells.
- [x] Provide operations to add/remove/reorder rows and columns.
- [x] Provide operation to modify cell contents.
- [x] Provide derived read models optimized for UI (e.g., flattened rows with totals and pinned footer rows).

## Stage 3 — Storage (IET/Model/Storage)

Goal: Persist and load user inputs to/from JSON with a human-readable, stable order and schema reference.

- [ ] Choose JSON library available in common (e.g., kotlinx.serialization JSON).
- [ ] Define DTOs mirroring domain with explicit order-preserving lists.
- [ ] Define on-disk schema versioning; embed `$schema`/`schemaVersion` field in root.
- [ ] Produce compiled-in JSON schema and exportable `.json` schema file under `docs/`.
- [ ] Implement serialization (save): domain → DTO → JSON; preserve ordering of rows/columns.
- [ ] Implement deserialization (load): JSON → DTO → domain; validate and normalize.
- [ ] Implement schema migration hooks for future versions (no-op for v1).
- [ ] Round-trip tests: domain → JSON → domain equality (ignoring computed outputs).
- [ ] Files saved to OS default location initially (Desktop? User home "Documents" folder?), with "last output folder"
remembered between sessions.

## Stage 4 — Desktop UI (Compose)

Goal: Implement an editable, scrollable table with fixed headers and pinned totals and ratio rows.

- [ ] App shell and window with menu or toolbar for File (New, Open, Save, Save As), Edit (Undo/Redo), and Help.
- [ ] Layout: scrollable table with separate panes for fixed row headers and column headers.
- [ ] Pinned rows: group totals (Performance, Resource) and final Performance-to-Cost Ratio row.
- [ ] Row re-ordering: individual Performance and Cost rows can be dragged to re-order.
- [ ] Column re-ordering: columns can be dragged to re-order.
- [ ] Auto-sizing: columns/rows size to content; allow manual resize later if needed.
- [ ] Editing behavior: commit on Enter or focus loss; validation feedback inline.
- [ ] Navigation: keyboard (arrows, Tab/Shift+Tab, Enter), mouse clicks; selection highlight.
- [ ] Recalc: trigger model recomputation on edit; UI recompose via bridge.
- [ ] Accessibility: basic focus order and labels.

## Stage 5 — Undo/Redo

Goal: Allow users to undo/redo edits across model changes.

- [ ] Command or memento-based change tracking around the model service.
- [ ] Scope: cell edits, row/column add/remove, rename, unit changes, and reorders.
- [ ] Integrate with UI: menu items and shortcuts (Ctrl+Z / Ctrl+Y).
- [ ] Undo stack depth limit (100 steps).
- [ ] Undo stack persists in memory across save, or re-load of same file, but reset when a new file is created, or a different file is loaded.

## Stage 6 — Validation, Formatting, and Units

Goal: Provide consistent user feedback and avoid invalid computations.

- [ ] Central validation rules shared between UI and storage.
- [ ] Numeric parsing/formatting policy, including decimal separator and precision.
- [ ] Display of confidence ranges and percentages with sensible rounding (e.g., 1 or 2 decimals).
- [ ] Error display inline and summarized (e.g., status area).

## Stage 7 — Sample Data and Documentation

- [ ] Provide a small example IET JSON file under `docs/examples/`.
- [ ] Document JSON schema under `docs/schema/iet.schema.json` and reference from plan/README.
- [ ] Update `docs/requirements.md` cross-links to schema and example.
- [ ] Add a brief user guide for the desktop UI basics and keyboard shortcuts.

## Stage 8 — Testing

- [ ] Unit tests for domain computations (cell impact, totals, ratio; edge cases).
- [ ] Unit tests for validation rules.
- [ ] Serialization tests (round-trip, ordering stability, schema version tag).
- [ ] UI-layer tests kept minimal; rely on manual verification for layout specifics.

Open Question: Any requirement for golden JSON fixtures for regression? If yes, add canonical fixtures.

## Stage 9 — Performance and Responsiveness

- [ ] Ensure recompute complexity is acceptable for typical table sizes (e.g., 100 rows × 10 columns).
- [ ] Avoid unnecessary recompositions; batch updates during edits when appropriate.
- [ ] Consider virtualization if very large tables become a requirement (future).

## Stage 10 — Future Considerations (Out of Scope for v1)

- [ ] Web target using Compose Multiplatform Web; reuse common model and storage.
- [ ] Import/export CSV.
- [ ] Templates for common requirement types.
- [ ] Multiple sheets per file.
- [ ] Collaboration or live sharing.
- [ ] User-editable, saved list of units, and custom rendering for some units such as currency.  

---

### Milestone Checklist Summary

- [x] Stage 0 Foundations
- [x] Stage 1 Domain Model
- [ ] Stage 2 Model–UI Bridge
- [ ] Stage 3 Storage
- [ ] Stage 4 Desktop UI
- [ ] Stage 5 Undo/Redo
- [ ] Stage 6 Validation & Formatting
- [ ] Stage 7 Samples & Docs
- [ ] Stage 8 Testing
- [ ] Stage 9 Performance
- [ ] Stage 10 Future (Optional)
