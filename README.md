# barebones-commander

[![License](http://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

`barebones-commander` is a small, security-first dual-pane file manager focused on
local files plus **SFTP/SSH**, **NFS**, and **S3-compatible storage**, on
**Linux** and **macOS**.

It is a **fork of [muCommander](https://github.com/mucommander/mucommander)**, with most
upstream features removed in favor of a smaller, easier-to-audit codebase.

## Status

Early — v0.1.0 is in active development. See [`PLAN.md`](PLAN.md) for the
phased roadmap.

## Scope

| Current scope |
|---|
| Local file system |
| **SFTP / SSH**, **NFS**, **S3-compatible storage** |
| Linux + macOS OS adapters |
| Basic archive formats: zip, tar, gzip, bzip2, xz |
| Text viewer |
| Mouse-driven dual-pane GUI, drag & drop, keyboard bindings |

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the current runtime/module shape,
[`LIBRARIES.md`](LIBRARIES.md) for the library inventory, and
[`SECURITY_REVIEW.md`](SECURITY_REVIEW.md) for the audit that motivated this fork.

## Agent Skills

Repo-local Codex skills live under [`.codex/skills`](.codex/skills). The
current skill, `clean-java-gui-slop`, is a Java/Swing/native-GUI checklist for
avoiding AI-generated UI, threading, warning-suppression, and verification
slop in this desktop app.

## Build

Requires JDK 25+ (LTS). The application runs as a plain JVM app; the old
upstream OSGi runtime has been removed.

```sh
./gradlew run        # run from sources
./gradlew tgz        # produce a Linux tarball
./gradlew dmg        # produce a macOS DMG  (-PskipDmgSign for unsigned)
```

## License

`barebones-commander` is released under the **GNU General Public License v3** (GPLv3),
preserving the original muCommander license. See [`LICENSE`](LICENSE) and
[`NOTICE`](NOTICE).

## Trademark

`muCommander` is the brand of the upstream project (https://www.mucommander.com,
copyright the original authors). This fork does **not** claim affiliation with the
upstream muCommander project. The fork has been renamed and re-iconified to avoid
brand confusion. All upstream copyright headers in source files are preserved.
