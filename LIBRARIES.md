# barebones-commander — Architecture & Libraries

Current inventory for `barebones-commander` after Phase 31.

## At A Glance

| | |
|---|---|
| Type | Desktop dual-pane file manager |
| Primary language | Java 25 |
| Other JVM languages | No first-party Kotlin; Kotlin stdlib is pinned only for transitive dependency hygiene |
| Build system | Gradle 9.5.x, Kotlin DSL, version catalog |
| Runtime model | Plain JVM application; no OSGi/Felix runtime |
| UI toolkit | Swing + FlatLaf |
| Supported OS | macOS and Linux |
| Logging | Internal `barebones-logging` facade backed by JDK logging APIs |
| License | GPLv3 |

See `NATIVE_DEPS_AUDIT.md` for the native/JNA/shell-out inventory and
candidate replacements.

## Modules

### Core

- `barebones-core`
- `barebones-core-preload`
- `barebones-logging`
- `barebones-commons-{collections,conf,file,io,runtime,util}`
- `barebones-preferences`
- `barebones-translator`
- `barebones-encoding`
- `barebones-process`
- `barebones-command`

### Protocols

- `barebones-protocol-api`
- `barebones-protocol-sftp`
- `barebones-protocol-s3`
- `barebones-protocol-nfs`
- `sun-net-www` (vendored support code required by Yanfs/NFS)

### Archive Formats

- `barebones-archiver`
- `barebones-format-{zip,tar,gzip,bzip2,xz}`

### Viewers

- `barebones-viewer-api`
- `barebones-viewer-text`

### OS Integration

- `barebones-os-api`
- `barebones-os-linux`
- `barebones-os-macos`

## Main Libraries

Versions are declared in `gradle/libs.versions.toml`.

### UI / Core

| Library | Version | Purpose |
|---|---:|---|
| FlatLaf | 3.7.1 | Swing look-and-feel |
| RSyntaxTextArea | 3.6.2 | Text viewer/editor component |
| ICU4J | 78.3 | Locale-aware collation |
| JCommander | 1.82 | CLI argument parsing |
| json-smart | 2.6.0 | JSON helper |
| unix4j-command | 0.6 | Small Unix-style command helpers |
| SnakeYAML | 2.6 | YAML parsing |
| dd-plist | 1.29 | Apple plist parsing |
| JetBrains annotations | 26.1.0 | Compile-only nullability annotations |

### Native Interop

| Library | Version | Purpose |
|---|---:|---|
| JNA / JNA Platform | 5.18.1 | macOS Keychain/Security.framework, libsecret, macOS OS integration |

### Protocols

| Library | Version | Used by |
|---|---:|---|
| `com.github.mwiede:jsch` | 2.28.2 | SFTP |
| AWS SDK v2 BOM | 2.44.4 | S3-compatible storage |
| Testcontainers BOM | 1.21.4 | LocalStack S3 integration tests |

### Archive / Utility

| Library | Version | Purpose |
|---|---:|---|
| commons-collections4 | 4.5.0 | Collections helpers |
| commons-compress | 1.28.0 | Tar and archive support |
| commons-lang3 | 3.20.0 | Utility helpers |
| XZ for Java | 1.12 | xz/lzma support |

### Tests / Analysis / Build Plugins

| Library / plugin | Version | Purpose |
|---|---:|---|
| JUnit BOM | 5.14.4 | JUnit modules |
| FindSecBugs plugin | 1.14.0 | SpotBugs security rules |
| Grgit Gradle plugin | 5.3.3 | Git metadata in build |
| CycloneDX Gradle plugin | 3.2.4 | SBOM generation |
| SpotBugs Gradle plugin | 6.5.4 | Static analysis |
| OWASP Dependency-Check Gradle plugin | 12.2.2 | Vulnerability scan |

## Phase 23 Pin Decisions

Phase 23 checked Maven Central and Gradle Plugin Portal metadata on 2026-05-10.
Most catalog entries were already current stable releases. Intentional pins:

| Catalog key | Current | Metadata latest | Reason |
|---|---:|---:|---|
| `testcontainers` | 1.21.4 | 2.0.5 | `org.testcontainers:localstack` metadata currently releases only through 1.21.4; 2.x needs a separate migration. |
| `junit-bom` | 5.14.4 | 6.1.0-RC1 | Latest metadata is a JUnit 6 release candidate; keep latest stable JUnit 5.x. |
| `kotlin-stdlib` | 2.3.21 | 2.4.0-Beta2 | Latest metadata is beta; keep latest stable 2.3.x. |
| `grgit` | 5.3.3 | marker says 5.0.0-rc.3 | Plugin marker metadata is stale/inconsistent; 5.3.3 is present in the version list and already works with the build. |
