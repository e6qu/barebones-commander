# barebones-commander Architecture

`barebones-commander` is a plain JVM/Swing desktop file manager focused on
local files, SFTP/SSH, NFSv2/v3, S3-compatible object storage, basic archive
browsing, and a text viewer on macOS and Linux.

## Runtime Model

The app starts at `dev.barebones.commander.bootstrap.Main`, parses command-line
configuration, and calls `Bootstrap.start(...)`. There is no Felix/OSGi
container. Module wiring is explicit:

- archive formats are discovered with `ServiceLoader<ArchiveFormatProvider>`;
- remaining modules still expose small static `Activator.register(...)`
  entry points until their extension points are migrated;
- shutdown hooks call module shutdown paths for cached resources such as S3
  connections and secret stores.

The root Gradle application target builds a normal classpath app and a fat JAR.
Release packaging uses `jpackage` for Linux DEB/RPM and macOS DMG only.

## Modules

Core application modules:

- `barebones-core`, `barebones-core-preload`
- `barebones-command`, `barebones-preferences`, `barebones-translator`
- `barebones-commons-{collections,conf,file,io,runtime,util}`
- `barebones-logging`, `barebones-process`, `barebones-encoding`

Protocol modules:

- `barebones-protocol-api`
- `barebones-protocol-sftp`
- `barebones-protocol-s3`
- `barebones-protocol-nfs`
- `sun-net-www`, vendored support code required by the current Yanfs/NFS stack

Archive modules:

- `barebones-archiver`
- `barebones-format-{zip,tar,gzip,bzip2,xz}`

Viewer and OS modules:

- `barebones-viewer-api`, `barebones-viewer-text`
- `barebones-os-api`, `barebones-os-linux`, `barebones-os-macos`
- `barebones-secret-store`

Test support:

- `barebones-test-support`

## File And Protocol Layer

`barebones-commons-file` owns `AbstractFile`, `FileURL`, scheme parsing,
protocol registration, file pools, archive registration, and file icon
providers. Local and search protocols are registered statically for tests and
low-level usage. SFTP, NFS, and S3 are registered by their protocol modules at
bootstrap.

`FileURL` parses locations into scheme-specific file URLs, with protocol
modules providing the concrete filesystem implementations registered at
bootstrap.

## GUI Layer

The UI is Swing with FlatLaf. Main user workflows live under `barebones-core`
and are centered on:

- `MainFrame` and dual-pane folder tables;
- `Action` classes for commands and keyboard bindings;
- protocol connection panels from the protocol modules;
- dialogs for preferences, credentials, file operations, and progress;
- background jobs for copy/move/delete/archive/listing work.

Swing code must keep UI mutations on the EDT and long-running file/protocol
work off the EDT. Repo-local Codex skill
`.codex/skills/clean-java-gui-slop` documents the review checklist for Java GUI
changes where browser-style automated inspection is not available.

## Storage, Secrets, And Native Integration

Credentials are routed through `barebones-secret-store`. macOS uses Keychain via
JNA; Linux uses libsecret via JNA. The fallback encrypted file store is scoped
by the secret-store module and covered by focused tests.

Native and shell-out surfaces are intentionally small and documented in
`NATIVE_DEPS_AUDIT.md`. Current supported OS integration is macOS and Linux.

## Build And Verification

The build requires JDK 25. Java compilation uses `--release 25`,
`-Xlint:deprecation`, `-Xlint:unchecked`, and `-Werror`, so deprecation and
unchecked warnings are treated as build failures. Tests run on JUnit 5 through
the JUnit Platform. SpotBugs/FindSecBugs runs with `ignoreFailures=false`
against the configured baseline.

Typical local verification:

```sh
./gradlew check
```

CI runs unit tests on Ubuntu and macOS, package/SBOM smoke checks, a TLS-bypass
grep gate, and SpotBugs.
