# Native Dependencies Audit

Phase 24 inventory, generated on 2026-05-10. Scope: every checked-in JNI/JNA
binding, platform shell-out, and vendored native-adjacent implementation that
affects the supported macOS and Linux runtime.

## Summary

This phase removed one dead native surface: `barebones-commons-file` no longer
depends on JNA because its unused `libc`/`statvfs` wrapper was deleted.

The remaining native and shell-out surfaces are intentional platform integration
points. The best follow-up work is not a bulk rewrite; it is a small set of
targeted PRs:

1. Replace the credentials-file `chmod` shell-out with Java NIO POSIX
   permissions.
2. Migrate macOS Keychain from legacy `SecKeychain*` calls to the modern
   `SecItem*` API while keeping JNA.
3. Harden platform command execution with timeouts and consistent interrupt
   handling where the app currently waits on external commands.
4. Evaluate NFS replacement only if NFS becomes a maintenance hotspot; today
   the vendored NFS code is pure Java and already isolated.

## Inventory

| Area | Location | Mechanism | Purpose | Java-native replacement | Recommendation |
|---|---|---|---|---|---|
| macOS Keychain | `barebones-secret-store/.../macos/SecurityFramework.java` | JNA to `Security.framework` | Store, lookup, and delete generic-password items in the user keychain | No Java SE API. A modern native replacement is JNA bindings to `SecItem*` with CoreFoundation dictionary marshalling. | Keep JNA, but migrate away from legacy `SecKeychain*` APIs in a focused PR. |
| Linux Secret Service | `barebones-secret-store/.../linux/Libsecret.java` | JNA to `libsecret-1` and GLib/GIO cancellation symbols | Store, lookup, clear credentials via the user's Secret Service provider | Direct D-Bus via a Java D-Bus client is possible, but would replace a small stable C binding with a larger protocol implementation. | Keep libsecret JNA. The current wrapper is small and has cancellation timeouts. |
| macOS trash | `barebones-os-macos/.../OSXTrash.java` | JNA Platform `MacFileUtils`; AppleScript fallback for SMB/Finder cases | Move files to Trash, count/open/empty Finder Trash | `java.awt.Desktop.moveToTrash` can move files, but does not cover Finder count/empty/open behavior. | Keep. Consider `Desktop.moveToTrash` as a fallback simplification only after manual macOS testing. |
| macOS xattrs | `barebones-os-macos/.../XAttrUtils.java`, `OSXDesktopAdapter.java` | JNA Platform xattr binding | Preserve Finder tags and comments on local file copies | No Java SE API for macOS Finder metadata xattrs. | Keep. Scope is local-only and isolated to post-copy metadata preservation. |
| macOS AppleScript | `barebones-os-macos/.../AppleScript.java`, `OSXTrash.java`, `OSXDesktopAdapter.java` | `osascript` through `ProcessRunner` | Finder trash actions, Finder comment writes, URL opening workaround | No Java SE Apple Event or Finder API. | Keep. Already has streaming decoding, bounded output, and tests. |
| macOS discovery tools | `OSXDesktopAdapter.java` | `dscl`, `duti`, `mdfind`, shell `command -v duti` | User shell discovery and "Open With" app discovery | Partial APIs exist, but not for `duti`'s per-UTI default-app listing. | Keep, but centralize execution and ensure processes are destroyed after timeout. |
| Linux desktop settings | `GSettings.java`, `GConfTool.java`, `KdeConfig.java` | `gsettings`, `gconftool`, `kreadconfig` | Read multi-click interval / desktop config values | Direct D-Bus or dconf/KConfig libraries would add platform-specific complexity. | Keep shell-outs, but add timeouts and restore interrupt status consistently. |
| Linux openers/trash UI | `GnomeDesktopAdapter.java`, `KdeDesktopAdapter.java`, `XfceDesktopAdapter.java`, trash classes | `xdg-open`, `gvfs-open`, `gnome-open`, `kfmclient`, `kioclient`, `thunar`, `nautilus`, `ktrash` | Open files, URLs, terminals, and Trash in the user's desktop environment | `java.awt.Desktop` covers some open/browse cases but not every desktop-specific Trash action. | Keep for now. Consider a later Desktop API fallback pass, not a blind replacement. |
| Local process runner | `barebones-process/.../LocalProcess.java` and callers | `ProcessBuilder` | User commands, file openers, AppleScript, desktop helpers | This is the Java-native process API. | Keep. It is the correct abstraction for user-configured commands. |
| Credentials chmod | `Chmod.java`, `CredentialsManager.java` | `chmod` shell-out | Set credentials file to mode `0600` on Unix-like systems | `java.nio.file.Files.setPosixFilePermissions` covers the current internal use. | Replace in the next code PR; this is low risk and removes an avoidable shell-out. |
| FreeBSD mount list | `LocalFile.streamMountPoints()` | `/sbin/mount -p` for FreeBSD only | Enumerate local mount points | Linux path already uses `/proc/mounts`; FreeBSD is outside current supported OS targets. | Remove or guard more tightly in a cleanup PR. It is dead for macOS/Linux support. |
| SFTP | `barebones-protocol-sftp` | `com.github.mwiede:jsch` pure Java SSH/SFTP | SFTP backend | Apache MINA SSHD is a maintained pure Java alternative. | Defer. JSch fork is current and working; migrate only if a concrete capability or maintenance issue appears. |
| NFS | `barebones-protocol-nfs`, `sun-net-www` | Vendored pure Java Sun/Yanfs RPC/NFS code | In-process NFSv2/v3 backend | A maintained Java NFS client would be preferable, but credible drop-in options need proof-of-concept testing. | Keep isolated. Do not attempt a large replacement without an integration-test fixture. |

## Current Native Libraries

| Module | Native dependency after this phase | Notes |
|---|---|---|
| `barebones-secret-store` | JNA, macOS Security.framework, Linux libsecret | Required for OS keychain backends; AES-GCM file fallback remains available. |
| `barebones-os-macos` | JNA Platform, macOS xattr/trash helpers, AppleScript/osascript | Required for Finder-specific metadata and Trash integration. |
| `barebones-commons-file` | none | Its unused `libc`/`statvfs` binding was removed in this phase. |

## Follow-up Candidates

### Phase 25 Candidate: Remove `chmod` Shell-Out

`CredentialsManager` only needs to set the credentials file to `0600`.
`Files.setPosixFilePermissions` can do that directly on macOS and Linux. This
should be a narrow PR with tests for the mode-to-permission mapping and a manual
verification note for both OS families.

### Phase 26 Candidate: Modernize macOS Keychain Binding

The current keychain code uses legacy `SecKeychainAddGenericPassword`,
`SecKeychainFindGenericPassword`, and `SecKeychainItemDelete`. It works, but the
modern Apple API family is `SecItem*`. A replacement still needs JNA because
Java SE has no keychain API. The value is deprecation reduction, not native
dependency removal.

### Phase 27 Candidate: Platform Process Hardening

Several desktop helpers invoke short-lived platform tools. Most use argument
arrays, which avoids shell injection, but timeouts and interrupt handling are
inconsistent. A follow-up should provide a small shared helper for bounded
platform commands and migrate:

- `OSXDesktopAdapter.runCommand`
- `KdeConfig.getValue`
- `OSCommand.runCommand`
- Linux Trash `open` / `empty` helpers where they wait synchronously

User-configured commands should stay on `ProcessRunner`; that is application
functionality, not an accidental native dependency.

## References

- Java NIO POSIX permissions:
  https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#setPosixFilePermissions(java.nio.file.Path,java.util.Set)
- Java Desktop integration:
  https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/java/awt/Desktop.html
- Apple Keychain Services:
  https://developer.apple.com/documentation/security/keychain_services
- Apple guide for local UNIX manual pages (`man osascript`):
  https://developer.apple.com/documentation/os/reading-unix-manual-pages
- libsecret reference:
  https://gnome.pages.gitlab.gnome.org/libsecret/
- Secret Service API:
  https://specifications.freedesktop.org/secret-service-spec/latest/
- Apache MINA SSHD:
  https://mina.apache.org/sshd-project/
