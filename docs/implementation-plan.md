# Impact Estimation Tool — Design & Implementation Plan

This plan outlines staged implementation for the Impact Estimation Tool (IET) focusing on Desktop (Windows) via Compose Multiplatform. It follows the suggested order: models → storage → UI. No code is included here.

## Stage 0 — Foundations & Project Wiring
- [ ] Confirm Kotlin Multiplatform targets: common + desktop (JVM). Android target exists but is out of scope for now.
- [ ] Confirm JVM toolchain (11) and Compose versions are consistent across modules.
- [ ] Establish module boundaries/namespaces for model and storage under `commonMain` where possible.
- [ ] Decide DI/locator approach (simple service provider vs. manual wiring).
- [ ] Define error-handling strategy (validation errors vs. domain invariants vs. IO errors).
- [ ] Define number format and locale handling for numeric inputs/outputs.
- [ ] Define unit testing strategy and locations (commonTest, jvmTest) per guidelines.
- [ ] Add lightweight sample data set for manual testing.

Open Question: Should Android target be completely disabled for now, or left as-is but ignored by desktop-focused work?

## Stage 1 — Domain Model (IET/Model)

Goal: Implement core domain entities and computations suitable for UI binding.

- [ ] Entities: `QualityRequirement` (id/name, current, goal, unit, type: Performance|Cost).
- [ ] Entities: `DesignIdea` (id/name, description optional).
- [ ] Entity: `EstimationCell` (estimatedValue, optional confidenceRange).
- [ ] Table Aggregates: container that maps `QualityRequirement` × `DesignIdea` → `EstimationCell`.
- [ ] Computation: Impact percentage for a cell relative to requirement current→goal delta.
- [ ] Computation: Totals per group (Performance Totals, Cost Totals) across rows per column.
- [ ] Computation: Performance-to-Cost Ratio per DesignIdea (TotalPerformance% / TotalCost%).
- [ ] Validation: 
  - [ ] Goal ≠ Current for performance rows (to avoid divide-by-zero). Define behavior when equal.
  - [ ] Costs are minimized; minimum legal value is zero.
  - [ ] Estimated value may be below current or above goal for performance; >= 0 for cost.
  - [ ] ConfidenceRange is non-negative and interpretable on same scale.
- [ ] Domain services: 
  - [ ] Recalculation service that recomputes all dependent outputs on any change.
  - [ ] Simple change-logging hooks to support undo/redo later.
- [ ] Data structures prepared for stable ordering (preserve insertion order for rows/columns).
- [ ] Bridge-friendly API: observable or callback-based change notifications for UI recomposition.

Open Question: Should we model confidence as symmetric absolute delta, percentage, or asymmetric bounds? Requirements mention "+/- range" — assume symmetric absolute value unless specified.

Open Question: When Goal == Current for a performance requirement, should impact be treated as 0, undefined, or require user correction? Proposal: flag validation error and exclude from totals.

Open Question: For Performance-to-Cost Ratio when cost total is 0% (or undefined), should ratio be Infinity, N/A, or capped? Proposal: display N/A and exclude from ordering.

## Stage 2 — Model–UI Bridge

Goal: Ensure the model is UI-ready. If pure common model lacks observability, provide a bridge in desktop layer.

- [ ] Decide observable mechanism:
  - Option A: Kotlin `StateFlow`/`MutableStateFlow` in common.
  - Option B: Immutable model + desktop adapter translating to Compose `mutableStateOf`.
- [ ] Define events: row/column added, removed, reordered; cell edited; metadata changed; recompute complete.
- [ ] Provide stable identifiers for rows and columns to correlate UI cells.
- [ ] Provide derived read models optimized for UI (e.g., flattened rows with totals and pinned footer rows).

Open Question: Preference for StateFlow in common vs. Compose state in desktop? Default: StateFlow in common to allow future Web target.

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

Open Question: Exact JSON schema shape preferences (naming, nesting of cells by row or by column)? Suggest: rows list, each row has list of cell inputs ordered by design idea order.

Open Question: Where to store files by default and recent-files list? Desktop-only for now; propose OS file dialog without app-managed directory.

## Stage 4 — Desktop UI (Compose)

Goal: Implement an editable, scrollable table with fixed headers and pinned totals and ratio rows.

- [ ] App shell and window with menu or toolbar for File (New, Open, Save, Save As), Edit (Undo/Redo), and Help.
- [ ] Layout: scrollable table with separate panes for fixed row headers and column headers.
- [ ] Pinned rows: group totals (Performance, Costs) and final Performance-to-Cost Ratio row.
- [ ] Auto-sizing: columns/rows size to content; allow manual resize later if needed.
- [ ] Editing behavior: commit on Enter or focus loss; validation feedback inline.
- [ ] Navigation: keyboard (arrows, Tab/Shift+Tab, Enter), mouse clicks; selection highlight.
- [ ] Recalc: trigger model recomputation on edit; UI recompose via bridge.
- [ ] Accessibility: basic focus order and labels.

Open Question: Is column reordering required by users? For v1, assume fixed order with add/remove at end and drag reorder as future enhancement.

## Stage 5 — Undo/Redo

Goal: Allow users to undo/redo edits across model changes.

- [ ] Command or memento-based change tracking around the model service.
- [ ] Scope: cell edits, row/column add/remove, rename, unit changes, and reorders.
- [ ] Integrate with UI: menu items and shortcuts (Ctrl+Z / Ctrl+Y).
- [ ] Persist undo stack? Likely no — stack resets on file load/new.

Open Question: Depth limit and memory policy for history? Default: 100 steps.

## Stage 6 — Validation, Formatting, and Units

Goal: Provide consistent user feedback and avoid invalid computations.

- [ ] Central validation rules shared between UI and storage.
- [ ] Numeric parsing/formatting policy, including decimal separator and precision.
- [ ] Display of confidence ranges and percentages with sensible rounding (e.g., 1 or 2 decimals).
- [ ] Error display inline and summarized (e.g., status area).

Open Question: Should units affect formatting (e.g., currency, time) with specialized renderers? For v1, treat units as free-text labels and keep numeric formatting generic.

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
- [ ] Column reorder and resize with persistence.
- [ ] Templates for common requirement types.
- [ ] Multiple sheets per file.
- [ ] Collaboration or live sharing.

---

### Milestone Checklist Summary

- [ ] Stage 0 Foundations
- [ ] Stage 1 Domain Model
- [ ] Stage 2 Model–UI Bridge
- [ ] Stage 3 Storage
- [ ] Stage 4 Desktop UI
- [ ] Stage 5 Undo/Redo
- [ ] Stage 6 Validation & Formatting
- [ ] Stage 7 Samples & Docs
- [ ] Stage 8 Testing
- [ ] Stage 9 Performance
- [ ] Stage 10 Future (Optional)
