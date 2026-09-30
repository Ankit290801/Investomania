# LLM Agent Operating Guidelines

**Scope:** This document is project-agnostic. It defines *how* any task must be executed — the non-negotiable rules of engagement, the config/dead-code doctrine, the ledger protocol, and the autonomous execution loop. It contains no project-specific tasks. The actual TODO/backlog (what to build) will be supplied separately, in the format defined in Section 5. This document governs every task in that backlog, present or future, without needing to be restated.

---

## 1. Prime Directives (Non-Negotiable, Zero Tolerance)

These override convenience, speed, or "just this once" reasoning at every step, for every task, in every project this agent works on. If a directive and a task instruction ever conflict, the directive wins and the task must be re-planned before continuing.

| # | Directive | Severity |
|---|---|---|
| D1 | **No hardcoding.** No literal URLs, keys, model/provider names, prompts, category/label lists, magic numbers, user-facing copy, colors, timeouts, retry counts, feature toggles, or thresholds may appear inline in source code. Every one of these lives in a versioned config file and is read through a config accessor. A hardcoded value discovered at any point — including in code written earlier in the same session — is a **build-blocking defect**: stop, extract it to config, and re-verify before continuing. | **SEV-0 — blocking** |
| D2 | **No monolithic architecture.** Every feature/domain is a self-contained module with an explicit public interface. No module may reach into another module's internals. No single file, class, or function owns more than one responsibility. If a module starts absorbing unrelated logic mid-task, split it before proceeding — never "finish first, refactor later." | **SEV-0 — blocking** |
| D3 | **No dead code, ever.** Zero unused imports, functions, classes, files, config keys, feature flags, or commented-out blocks at the end of any task. When a change makes prior code obsolete, that code is deleted in the same task — not deprecated-in-place, not commented out "just in case." Version control is the safety net for "just in case"; the working tree is not. | **SEV-0 — blocking** |
| D4 | **Config-driven by default.** Before writing any value into code, the default assumption is "this belongs in config." Code contains *logic*; config contains *anything that could plausibly change without a logic change*. | Structural rule |
| D5 | **The TODO Ledger is the single source of truth for progress.** It is updated as part of completing a task, never as an afterthought, and never retroactively reconstructed. | Process rule |
| D6 | **Minimize human intervention.** Every task must be completable autonomously — using config defaults, sandboxed/mock values, and documented assumptions — except the narrow set of tasks defined in Section 8 that genuinely cannot be done without a human. When forced to assume something, the assumption is written into config (with a rationale comment/metadata field) and logged, not silently baked in. | Process rule |

---

## 2. Configuration Doctrine

1. **What belongs in config, never code:** connection endpoints, credentials/paths-to-secrets (never the secrets themselves), timeouts/retry/backoff policy, categorical data (taxonomies, labels, enums that reflect business content rather than program structure), prompt/template text, all user-facing copy tied to an event, design tokens, feature flags, rate limits, batching/sizing parameters, scheduling windows, and any bundled fallback/default data.
2. **Single point of access:** exactly one config-loading module per project reads raw config files. Every other module consumes typed config objects exposed through it. No module reads a config file directly.
3. **Schema-validated:** every config file has a matching schema. Config is validated at load time; malformed config fails loudly in development and falls back safely in production — the agent never proceeds on unvalidated config.
4. **Layered with safe defaults:** bundled/default config must be a complete, independently functional configuration on its own. Any remote/overriding config layer is optional — its absence or failure must never break the base functionality.
5. **New key ⇒ new schema entry, same task.** A config key introduced without a corresponding schema entry and a bundled default is itself a D1/D4 violation, not a follow-up.

---

## 3. Modularity Doctrine

1. Structure the codebase (of whatever project the backlog belongs to) as isolated modules, each with: its own domain logic, its own data access, its own presentation/interface layer, and a single explicit contract through which other modules may interact with it.
2. Cross-module communication happens **only** through declared contracts/interfaces — never by importing another module's internals directly.
3. Shared logic used by more than one module is extracted into a shared/common layer with its own contract — it is not duplicated, and it is not left inside whichever module happened to need it first.
4. Orchestration code (wiring modules together, sequencing calls) is kept separate from the modules' own logic — an orchestrator calls public interfaces; it does not contain business logic of its own.
5. Before closing any task, ask: *"Could this module be understood, tested, and replaced in isolation, without touching anything else?"* If the answer is no, the task is not done — restructure until it is.

---

## 4. Dead Code Doctrine (elaboration of D3)

- A refactor, a fix, or a feature addition that renders older code unreachable requires the **removal** of that code as part of the same unit of work — not a TODO comment to remove it later.
- "Might be needed later" is not a reason to keep unused code. If it's needed later, it will exist in version control history.
- Commented-out code is treated identically to dead code: remove it, don't park it.
- Unused config keys, unused feature flags, and unused test fixtures are dead code too, and are subject to the same removal rule.
- Every task's closing guardrail pass (Section 6) includes an explicit dead-code sweep — this is not optional or occasional, it runs every single time.

---

## 5. TODO Ledger Protocol

The backlog supplied separately must conform to (or be normalized into) this structure before execution begins.

### 5.1 Ledger schema
```json
{
  "tasks": [
    {
      "id": "string, stable and unique",
      "title": "short description",
      "status": "pending | in_progress | blocked | done",
      "acceptance_criteria": ["..."],
      "dependencies": ["ids of tasks that must be done first"],
      "config_touched": [],
      "files_touched": [],
      "dead_code_swept": false,
      "guardrail_checklist_passed": false,
      "assumptions_made": [],
      "blocked_reason": null,
      "last_updated": null
    }
  ]
}
```

### 5.2 Update protocol — mandatory, every task, no exceptions
1. **Before starting:** set `status = in_progress`, timestamp it.
2. **While working:** append each file touched and each config key added/changed as it happens, not reconstructed afterward from memory.
3. **Before closing:** run the full Guardrail Checklist (Section 6). Every item must pass before `guardrail_checklist_passed` is set to `true`.
4. **On completion:** set `status = done`, timestamp it, append one line to a running changelog.
5. **If genuinely blocked** (matches Section 8's escalation list): set `status = blocked`, fill `blocked_reason` with the exact, specific question for the human, and move on to the next unblocked, dependency-satisfied task — never stall the whole run over one blocked item.
6. A task is never marked `done` with unresolved guardrail failures, and never silently skipped — skipped is `blocked` with a stated reason, always.

---

## 6. Guardrail Checklist (run before closing out *every* task, no exceptions)

- [ ] **Hardcoding scan:** search for literal URLs, keys, category/label strings, prompt text, magic numbers outside config-loading code and tests. Any hit → extract to config, re-run the scan.
- [ ] **Secrets scan:** check source and config for anything resembling a live credential. Real secrets belong only in a secrets manager / environment, never in the repository.
- [ ] **Module boundary scan:** confirm no cross-module internal imports per Section 3.
- [ ] **Dead code scan:** static analysis for unused imports/functions/classes/files/config keys/flags; confirm anything made obsolete this task was deleted.
- [ ] **Config schema validation:** every config file touched still validates; every new key has a schema entry and a bundled default.
- [ ] **Test coverage:** new logic has at least one automated test — not just manual inspection.
- [ ] **Lint/format clean:** zero new warnings introduced.
- [ ] **Ledger updated:** per Section 5.2.

A task cannot be marked `done` while any box is unchecked. This checklist is identical regardless of project or task type — it does not get abbreviated for "small" tasks.

---

## 7. Autonomous Loop-Based Execution — Standing Permission

**The agent is granted standing permission to execute the entire backlog end-to-end in a continuous loop, without pausing for per-task confirmation, subject only to the stopping conditions in 7.3.** This permission is granted in advance so that ordinary task completion never requires a check-in; it does not need to be re-requested per task.

### 7.1 The loop
```
LOOP:
  1. Load the current ledger.
  2. Select the next task where status == pending AND all dependencies == done.
     - If multiple qualify, prefer the one that unblocks the most downstream tasks.
     - If none qualify but pending tasks remain (all blocked on dependencies or
       genuinely blocked per Section 8), go to 7.3 (stop/report).
  3. Set status = in_progress; execute the task per Sections 1-4.
  4. Run the full Guardrail Checklist (Section 6).
     - If it fails: fix within the same task iteration; do not close and move on
       with a failing checklist; do not ask the human to fix it unless the failure
       itself is a genuine Section 8 escalation.
  5. Update the ledger per Section 5.2; set status = done.
  6. Immediately return to step 1. Do not wait for a prompt to continue.
```

### 7.2 What this permission covers
- Moving from one task directly into the next without narrating intent to a human first or awaiting acknowledgment.
- Making the autonomous assumptions permitted under D6 in order to keep the loop moving, provided each is logged.
- Re-planning a task's internal approach (not its acceptance criteria) if the first approach violates a Prime Directive mid-execution.

### 7.3 Stopping conditions — the loop halts and reports when, and only when
- **All tasks are `done`.** Report completion with a summary of what was built and a link/pointer to the ledger and changelog.
- **All remaining pending tasks are blocked** (either on unmet dependencies that cannot be resolved, or on a genuine Section 8 escalation). Report exactly which tasks are blocked and why, with the specific question needed to unblock each.
- **A single task triggers a genuine Section 8 escalation.** Mark only that task `blocked`, log the specific question, and continue the loop on other unblocked tasks — a partial escalation is not a reason to halt the entire loop.
- **A Prime Directive cannot be satisfied for a task as specified** (e.g., the task as written cannot be done without hardcoding something with no sane config equivalent). Do not silently violate the directive and do not silently skip the task — mark it `blocked`, state the specific conflict, and continue the loop elsewhere.

The loop never halts merely because a task was "large," "took a while," or "involved several files" — those are not stopping conditions.

---

## 8. Escalation Boundary — the *only* acceptable manual-intervention points

Everything not on this list is completed autonomously, with assumptions documented per D6. This list exists because these genuinely cannot be resolved by the agent:

1. **Real credentials/secrets** — actual API keys, account tokens, signing keys. The agent may wire up *where* a secret is read from; a human must supply the value itself, into a secrets manager or environment, never into the repository.
2. **Ownership-gated accounts/platforms** — third-party account creation, billing, store/marketplace enrollment, and final submission steps that require a human's identity or payment method.
3. **Legally or contractually binding text** — privacy policy language, terms of service, compliance justifications submitted to a third party — content a human must own and approve.
4. **Purely subjective approval a stakeholder wants** (e.g., final brand sign-off) — the agent still produces a complete, working default rather than waiting; this is an optional confirmation point, not a blocker on completion.

If a task seems to require escalation but isn't clearly one of the above four, the agent defaults to **not escalating**: it proceeds with the most defensible assumption, documents it, and flags it for later human review rather than pausing the loop.

---

## 9. Definition of Done (applies to every task and to the backlog as a whole)

**Per task:**
- Ledger status is `done`, `guardrail_checklist_passed = true`.
- No Prime Directive was violated to get there.

**Per backlog run:**
- Every task is `done`, or explicitly `blocked` with a specific, resolvable question logged against it.
- A full scan across the codebase for hardcoded URLs/keys/category-data/prompts/copy returns zero hits outside config-loading code and tests.
- Static analysis and the dead-code sweep report zero unused symbols/files across the entire codebase touched by this backlog.
- Every config-eligible value (Section 2.1) is confirmed live-editable via its config file with no code change required.
- The assumptions log is either empty or every remaining entry is tied to a Section 8 escalation awaiting a human answer.

---

*This document is intentionally silent on what to build. When the backlog is supplied, apply Sections 1–9 to it exactly as written — no re-confirmation of these rules is required before starting the loop.*
