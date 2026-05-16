---
name: clean-java-gui-slop
description: Clean up AI-generated or fast-moving Java GUI/native desktop code before shipping. Use proactively when modifying Java Swing, AWT, JavaFX, platform integration, desktop packaging, native OS adapters, file-manager UI flows, or any GUI-adjacent Java code where browser-style tooling is unavailable or weak; also use during reviews of Java GUI PRs, UI bug fixes, refactors, tests, and warning/deprecation cleanup.
---

# Clean Java GUI Slop

Use this skill as a compact pre-edit and pre-ship checklist for Java desktop work. It adapts the `sockerless` vibe-coding discipline to Java and GUI apps: verify the real user-visible surface, avoid fake fallbacks, fit existing code, test actual UI/runtime behavior where possible, and prune code as part of every change.

## First Move

Before a substantial edit, state the 1-2 checks most relevant to the change. Do not recite the whole checklist.

- GUI behavior or layout change: verify the actual rendered state, event path, focus/keyboard behavior, and platform differences.
- Refactor: search for existing implementations and preserve hidden behavior such as threading, listeners, disposal, error normalization, and persisted settings.
- Test change: assert the user contract, not implementation metadata or current incidental behavior.
- Warning/deprecation cleanup: fix the root API usage; do not suppress warnings unless fixing would break required behavior and the reason is documented in the plan.
- Native/platform change: verify on the real platform path or clearly state which platform could not be exercised.

## Checklist

Stop after a "no" and resolve it before writing code.

### Codebase Fit

1. Search first with `rg`. Has this behavior, helper, model, renderer, action, preference, or adapter already been implemented?
2. Read nearby code before editing. Match local threading, listener, action, model, resource-loading, logging, and exception patterns.
3. Avoid decorative abstractions. Add factories/providers/managers only when there is real repeated complexity or an established local extension point.
4. Prune while changing. Remove obsolete helpers, branches, comments, imports, fixtures, resources, and docs made stale by the change.

### Java GUI Truth

5. Identify the real UI contract: visible state, enabled/disabled state, selection, focus traversal, keyboard shortcuts, drag/drop, accessibility name/description when present, persistence, and error display.
6. Respect the UI thread. Swing/AWT work belongs on the EDT; long-running I/O or filesystem work must not block it. Check `SwingWorker`, timers, listeners, and callbacks for thread handoff.
7. Do not replace event-driven behavior with sleeps, polling, arbitrary delays, or "works on my machine" timing unless the surrounding code already defines that contract.
8. Check lifecycle and ownership: listeners removed, dialogs disposed, timers stopped, native handles released, temporary files cleaned, background jobs cancellable.
9. Validate layout by rendering when feasible. Prefer a real run, screenshot, or focused GUI test over reasoning from code. If tooling cannot inspect the UI, say that explicitly and test the event/model layer plus manual run path.
10. Keep macOS/Linux behavior separate when needed. Do not reintroduce Windows support, Windows packaging, or platform shims in projects that have dropped it.

### Error Handling And State

11. Fail loud for real failures. Do not fabricate success, silently swallow exceptions, return placeholder values, or add fallback branches that hide missing state.
12. Fix root causes instead of stacking null/type guards. If many guards appear, identify which invariant should be established earlier.
13. Preserve user data and preferences carefully. Migration, defaults, and config parsing must be explicit and tested against real stored shapes where possible.
14. Treat logs and dialogs as contracts. Avoid leaking secrets; avoid generic "something went wrong" messages that lose actionable cause.

### Tests And Verification

15. Tests must derive from the contract: UI model behavior, public API behavior, file format, platform API, or documented workflow. Do not assert bug IDs, phase numbers, class names, timing artifacts, or incidental strings unless they are user-facing requirements.
16. Avoid mocks that replace the thing being tested. For desktop code, it is fine to isolate the filesystem or OS boundary, but at least one test or manual run should exercise the real action/listener/model path.
17. For Swing tests, assert state on the EDT and flush queued events. For async UI paths, wait on a real completion signal rather than sleeping.
18. After bulk generics/deprecation/warning cleanup, run the broadest practical checks: `./gradlew check`, module tests, SpotBugs/lints, and any packaging smoke relevant to touched modules.
19. If a GUI cannot be fully automated, leave a concise manual verification note: OS, command, workflow clicked/typed, observed result, and cleanup.

### Dependencies And APIs

20. Verify new dependencies against official registries or docs before adding them. Prefer JDK, existing project libraries, and mature GUI/platform libraries over plausible new packages.
21. Prefer current Java APIs. Fix deprecated APIs directly when practical; do not suppress deprecation or unchecked warnings just to make CI green.
22. Use typed Java shapes where possible. Avoid raw collections, `Map<String,Object>`, reflection, and stringly action parameters unless the surrounding API requires them.

### Review Traps

23. Re-read your own diff as if reviewing an unfamiliar PR. Look for additions without deletions, duplicate helpers, swallowed errors, fake fallbacks, sleeps, EDT violations, stale docs, and tests that merely snapshot current behavior.
24. After a commit, verify it actually landed with `git log --oneline -1` if hooks or auto-formatters ran.

## Output

When this skill fires, briefly name the checks selected and then proceed. Example:

```text
Using clean-java-gui-slop: this is a Swing refactor, so I’m checking existing action/listener patterns and EDT/lifecycle behavior before editing.
```

If verification is incomplete, state the exact gap instead of implying the UI is proven.
