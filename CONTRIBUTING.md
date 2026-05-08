# Contributing to `barebones-commander`

Thanks for considering a contribution. This fork prioritises **a small,
auditable codebase** over feature breadth — please read this whole file before
opening a PR.

## Scope reminder

Before adding code, check that the change fits the fork's narrow scope:

- **Kept**: local FS, SFTP/SSH, dual-pane Swing UI with mouse + drag-and-drop
  + keyboard bindings, basic archive formats (zip / tar / gz / bz2 / xz),
  text viewer, Linux + macOS.
- **Removed**: every other protocol, viewer, OS adapter, archive format, and
  the embedded terminal. See `PLAN.md` for the full keep / drop list.

PRs that add features outside this scope are unlikely to merge. Please open a
discussion first.

## Workflow

1. **Fork** this repo and create a branch off `master`. Branch name pattern:
   `phase-N/short-description`, `chore/...`, `fix/...`, or `docs/...`.
2. **Open a PR against `master`**. No direct pushes — branch protection on
   `master` will reject them.
3. **One logical change per PR.** "Refactor + add feature" is two PRs.
4. **Tests** (`./gradlew test`) must remain green. CI runs on PR open and on
   every push to the PR branch.
5. **Squash-merge** is the only merge style. The PR title becomes the squash
   commit subject — keep it short and imperative ("phase-1: remove RAR
   format module").
6. **No `--amend` of pushed commits.** Push a fresh commit on top instead.
7. **No `--no-verify`** for hooks. If a pre-commit hook fails, fix the
   underlying issue.

## Commit conventions

- Subject line ≤ 72 chars, imperative mood ("remove ...", not "removed ...").
- Body explains **why**, not what (the diff already shows what).
- Reference the relevant `PLAN.md` phase / sub-PR when applicable
  (e.g., "PR-1.4 of PLAN.md").
- All commits must be authored by a real person (no AI-generated author
  identities).
- **Commit signing** is required from Phase 8 onwards (`git commit -S`). See
  `PLAN.md` §8.

## Reviewer checklist

For maintainers reviewing a PR:

- [ ] Within scope (see "Scope reminder" above).
- [ ] No new Critical/High findings introduced (cross-check against
      `SECURITY_REVIEW.md`).
- [ ] No new Maven coordinate that brings in a `GPL-incompatible`,
      `AGPL`, `unrar-clause`, or `proprietary` license.
- [ ] No new direct call to `setDefaultSSLSocketFactory` or
      `setDefaultHostnameVerifier` (a CI grep gate also enforces this once
      Phase 5 lands).
- [ ] No new direct call to `Runtime.exec(String)` with a concatenated
      argument list (use `ProcessBuilder(List<String>)`).
- [ ] If the PR adds an XML reader: `FEATURE_SECURE_PROCESSING=true` and
      `disallow-doctype-decl=true` are set on the factory.
- [ ] If the PR persists user secrets: it goes through the OS keychain
      abstraction, not `XORCipher` or any plaintext file.
- [ ] Tests added for new behaviour where it is unit-testable.

## Where to find context

- `LIBRARIES.md` — architecture and library inventory.
- `SECURITY_REVIEW.md` — full Critical / High audit.
- `PLAN.md` — phased roadmap; every PR should map to a numbered phase step.
- `NOTICE` — third-party attributions and upstream credit.

## Upstream sync

This fork tracks upstream `mucommander/mucommander` as `git remote upstream`.
Cherry-picking specific bug fixes from upstream is welcome, but full merges
are not — the diverged scope and renamed packages would create churn. A
useful incantation:

```sh
git fetch upstream
git log upstream/master --since='2026-05-08' --oneline   # see new upstream commits
git cherry-pick <sha>                                     # pick a specific fix
# Resolve any path conflicts (com/mucommander → dev/barebones/commander, etc.)
```

If upstream renames a class we still ship, attribute the rename to upstream
in the commit body.

## License

By contributing, you agree your contribution is released under the project's
GPLv3 license, the same terms as the rest of the codebase.
