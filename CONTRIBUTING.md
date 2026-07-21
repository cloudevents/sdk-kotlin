# Contributing to CloudEvents Kotlin SDK

Thank you for contributing to the CloudEvents Kotlin SDK! This document consolidates the
contribution workflow for this repository, drawn from the CloudEvents SDK maintainer and
PR guidelines adapted to a Kotlin/Gradle project.

---

## 1. Prerequisites

Before you begin, ensure your environment meets these requirements:

- **Git** — configure your identity consistently across all work on this repo:

  ```bash
  git config user.name "Your Name"
  git config user.email "you@example.com"
  ```

  The name and email here will appear in every commit's `Author` line. They must match
  the `Signed-off-by` trailer added by `--signoff` — a mismatch will cause the DCO check
  to reject your PR.

- **JDK 21** — the project targets JDK 21 (Temurin). Install from
  [Adoptium](https://adoptium.net/) or via your OS package manager. The Gradle toolchain
  block will auto-provision JDK 21 if Gradle can find a compatible provisioner; setting
  `JAVA_HOME` to a JDK 21 installation guarantees a clean build.

- **Gradle** — the repository ships a Gradle wrapper (`gradlew` / `gradlew.bat`) that
  downloads the pinned Gradle version automatically. Run `./gradlew` on Linux/macOS or
  `gradlew.bat` on Windows. If you have Gradle installed system-wide, you may also run
  `gradle` directly.

---

## 2. Fork and Clone

1. Fork the repository on GitHub to your personal account.
2. Clone your fork locally:

   ```bash
   git clone https://github.com/<your-github-user>/sdk-kotlin.git
   cd sdk-kotlin
   ```

3. Add the upstream repository as a remote so you can keep your fork in sync:

   ```bash
   git remote add upstream https://github.com/cloudevents/sdk-kotlin.git
   ```

---

## 3. Branching

Always branch from the current `main` HEAD. If your change relates to an existing GitHub
issue, include the issue number in the branch name:

```bash
git fetch upstream
git checkout -b 42-fix-validation-edge-case upstream/main
```

Branch naming convention: `<issue-number>-<short-description>` — e.g.
`42-fix-validation-edge-case` or `117-add-subject-validation`.

If there is no issue, use a descriptive slug: `fix-type-coercion-in-attribute-accessor`.

Keep branches focused. One branch, one logical change.

---

## 4. Build and Test

### Build all targets

```bash
gradle build
```

This compiles every Kotlin Multiplatform target (JVM, JS, wasmJs, Linux, macOS, iOS,
Windows) and runs all tests on the targets supported by your current OS.

### Run quality checks

```bash
gradle check
```

`check` runs the full suite: Spotless formatting, detekt static analysis, and all tests.
Your PR must pass `gradle check` locally before you push.

### Target-specific tasks

| Task | Description |
|------|-------------|
| `gradle :core:jvmTest` | Run tests on JVM only |
| `gradle :core:jsNodeTest` | Run tests on JS/Node |
| `gradle :core:wasmJsNodeTest` | Run tests on wasmJs/Node |
| `gradle :core:linuxX64Test` | Run tests on Linux x64 (Linux only) |
| `gradle :core:allTests` | Run tests on all targets supported by the current OS |
| `gradle spotlessApply` | Auto-fix formatting violations |
| `gradle spotlessCheck` | Check formatting without modifying files |

### Apple and Windows targets

Apple targets (`macosX64`, `macosArm64`, `iosArm64`, `iosX64`, `iosSimulatorArm64`)
require a macOS host to compile and test. `mingwX64` links and tests best on Windows.
On other hosts, these targets still compile but test execution requires the appropriate
runner.

---

## 5. Commit Messages

This project follows the [Conventional Commits specification](https://www.conventionalcommits.org/en/v1.0.0/).

**Format:**

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

**Rules:**
- The first line (`type(scope): description`) must be a single sentence, no trailing
  period, and fewer than 80 characters.
- `type` must be one of: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
  `build`, `ci`, `chore`.
- Do not start the description with a capital letter or write it in Sentence, Title,
  or ALL-CAPS case; proper nouns and type names (e.g. `CloudEvent`, `SpecVersion`) may
  keep their casing.
- Add a body or footer when the change needs further explanation or references an issue.

**Examples of good commit messages:**

```
feat(core): add SpecVersion enum with v1.0 and v0.3 entries
fix(validation): reject empty string in required attribute check
docs: update CONTRIBUTING with Apple target build notes
```

**Examples of bad commit messages (will be rejected):**

```
WIP
Fixed stuff.
update CONTRIBUTING
```

A local commit-msg hook validates the format before each commit — see
[§10 Local commit hook](#10-local-commit-hook).

---

## 6. DCO Sign-Off

Every commit **must** carry a Developer Certificate of Origin sign-off. Add it with the
`--signoff` flag:

```bash
git commit --signoff
```

This appends a `Signed-off-by` line to the commit message:

```
Signed-off-by: Your Name <you@example.com>
```

**The `Author` and `Signed-off-by` must match exactly** — same name, same email address.
The DCO GitHub App checks this automatically on every PR commit. A mismatch will block
the PR from merging.

To verify before pushing:

```bash
git log --format="%an <%ae>%n%b" -1
```

Both lines should show the same name and email.

**Web-UI commits are not allowed.** GitHub's web editor does not support `--signoff`, so
commits made through the GitHub web interface will fail the DCO check. Always commit
locally with `git commit --signoff`.

**Watch out for email drift:** If you have multiple git configurations (work, personal,
different machines), verify that `git config user.email` in this repository matches the
email you intend to sign off with. Run `git config --list | grep email` to check.

---

## 7. Staying Current with `main`

Keep your branch rebased on top of upstream `main` before you submit or update a PR:

```bash
git fetch upstream
git rebase upstream/main
```

Resolve any conflicts, then continue:

```bash
git rebase --continue
```

After rebasing, force-push your branch to your fork:

```bash
git push -f origin 42-fix-validation-edge-case
```

Do not merge `upstream/main` into your branch — always rebase. Merge commits add noise
to the history and complicate squashing.

---

## 8. Submitting a Pull Request

Before opening a PR:

1. Make sure `gradle check` passes locally with no failures or formatting violations.
2. Squash your branch down to **one commit** per logical change. The PR title becomes the
   squash-merge subject line, so it must itself be a valid Conventional Commit:

   ```
   feat(core): add SpecVersion enum with v1.0 and v0.3 entries
   ```

3. Open the pull request against `cloudevents/sdk-kotlin main` from your fork.

The PR title must be a Conventional Commit — the CI `commitlint` check validates it.
If the title is not a valid conventional commit message, the PR checks will fail and
the PR cannot be merged.

**Interactive squash (when you have multiple commits):**

```bash
git rebase -i upstream/main   # mark extra commits as fixup or squash
git push -f origin your-branch
```

---

## 9. Code Review

After submitting your PR:

- At least **one maintainer approval** is required before a PR can be merged.
- A PR author may approve their own PR but needs at least one additional maintainer
  approval.
- If a maintainer has submitted a PR and it has not received another maintainer's
  approval after **72 hours**, they may self-merge.
- All automated checks (DCO, commitlint, build, quality gates) must be green before
  merging.
- If a reviewer requests changes, address each point or reply explaining why it is not
  addressed, then push an updated commit.

Maintainers merge via "Squash and merge" — the PR title becomes the single commit message
in `main`.

---

## 10. Local Commit Hook

The repository provides an opt-in Gradle-managed `commit-msg` git hook that validates
your commit messages against the Conventional Commits format before each commit.

The hook is installed automatically the first time any Gradle task runs:

```bash
gradle tasks   # installing a hook is a side-effect of running any task
```

To verify the hook is installed:

```bash
ls .git/hooks/commit-msg
```

Once installed, an attempt to commit with a non-conventional message will be rejected
locally with an explanatory error — giving you fast feedback before the push hits CI.

The hook is opt-in in the sense that it only applies after you have run a Gradle task
in the repository. It does not require npm or any tooling beyond Gradle.

---

## 11. Spec References

This SDK conforms to **CloudEvents v1.0.2** — the latest released spec text. Verify all
model, type-system, naming, and validation work against the pinned permalinks below, and
cite the relevant section in your tests and pull requests. Do not track the in-progress
1.0.3-wip text.

- [CloudEvents spec v1.0.2](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md) —
  the normative source for context attributes, the type system, and naming rules
- [CloudEvents SDK requirements v1.0.2](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/SDK.md) —
  version-support policy and SDK conformance requirements
- [CloudEvents website](https://cloudevents.io) — overview, links, and community resources

See `docs/adr/0001-spec-authority-pinned-to-v1.0.2.md` for why the authority is pinned.

---

## 12. Supply-Chain Security

The build and CI pipeline are hardened to catch known CVEs early, lock the dependency graph
against tampering or resolution drift, and produce a machine-readable inventory of what the SDK
ships. This section documents the moving parts a contributor is most likely to touch.

### 12.1 Dependency verification metadata

`gradle/verification-metadata.xml` pins a SHA-256 checksum for every artifact Gradle resolves —
the plugin classpath, every Kotlin Multiplatform target's klibs, and the test dependencies. Gradle
enforces these checksums automatically on **every** invocation: if a downloaded artifact's checksum
does not match the committed value, the build fails.

**Regenerate the metadata** whenever a dependency, plugin, or the Kotlin/Gradle toolchain changes:

```bash
gradle --write-verification-metadata sha256 --no-parallel --refresh-dependencies \
  resolveAllDependencies build
```

- `resolveAllDependencies` (defined in the root build) resolves every resolvable configuration
  across all projects, so all target klibs are captured — not just the ones the current host can
  compile. `build` is added so task-execution-time tooling classpaths (detekt, Spotless) are
  captured too.
- `--refresh-dependencies` forces a clean re-download from the source repositories, so a stale or
  poisoned local cache cannot seed a bad checksum into the committed file.
- `--no-parallel` keeps resolution deterministic while the file is being written.

**Trust model.** Verification is checksum-only; PGP signature verification is deferred
(`verify-signatures=false`). Both binary artifacts and their Gradle module/POM metadata are
checksummed (`verify-metadata=true`), so metadata tampering is caught as well. Three host-specific
build toolchains are listed under `<trusted-artifacts>` rather than checksummed, because each is
published with a per-OS archive/classifier and so cannot be checksummed from a single host — a
Linux-generated file would otherwise fail the macOS and Windows build legs:

- the **Kotlin/Native compiler** (`org.jetbrains.kotlin:kotlin-native-prebuilt`), resolved via Maven
  with an OS/arch classifier (e.g. `-macos-aarch64.tar.gz`, `-windows-x86_64.zip`);
- the **Node.js runtime** and **Yarn** used by the JS/Wasm targets.

These are build toolchains, not shipped dependencies. npm/yarn *package* dependencies are likewise
not covered here — they are pinned by the committed `kotlin-js-store` lockfiles.

**Generation strategy.** Because our runtime dependency set is minimal and every dependency klib is
a host-independent Maven Central artifact, the complete file is generated on Linux and enforced on
all runners. The `Verify dependency metadata integrity` step in `build.yml` guards against drift:
on the Linux leg it re-resolves the whole graph from source into a throwaway
`gradle/verification-metadata.dryrun.xml` and diffs it against the committed file, so a checksum
that only matches a poisoned local cache is caught by a clean pull from Maven Central.

### 12.2 Renovate and the verification-metadata gate

Dependency updates are automated by the [Renovate](https://docs.renovatebot.com/) app
(`renovate.json`): version-catalog libraries, Gradle plugins, the Kotlin+KSP+TestBalloon toolchain
(kept in lockstep because TestBalloon is Kotlin-version-pinned), GitHub Actions digests, and the
Gradle wrapper are each grouped into their own PRs.

The Kotlin JS/Wasm yarn lockfiles under `kotlin-js-store/` are deliberately **not** Renovate-managed:
they have no `package.json`, so Renovate's npm manager has no manifest to drive, and their webpack/
yarn toolchain versions are pinned by the Kotlin Gradle plugin. They refresh when the Kotlin toolchain
is bumped (the `kotlin-toolchain` group), which is the only point at which they should change.

**Important friction to expect:** a Renovate bump PR changes dependency versions but the
Mend-hosted Renovate app **cannot** run the regeneration command (`postUpgradeTasks` is unavailable
on the cloud app), so every such PR will **fail** the verification-metadata gate. To land it, check
out the Renovate branch locally, run the regeneration command from §12.1, and force-push the
updated `gradle/verification-metadata.xml` onto the branch.

### 12.3 Software Bill of Materials (SBOM)

A CycloneDX SBOM is produced by the `org.cyclonedx.bom` plugin:

```bash
gradle :cloudevents-kotlin-core:cyclonedxDirectBom
# -> core/build/reports/cyclonedx/bom.cdx.json
```

The SBOM is scoped to the JVM target's runtime classpath (`jvmRuntimeClasspath`) — the conventional
scope for a KMP library, matching how coverage is measured on the JVM target. **Limitation:**
native/JS/Wasm klib dependencies are not represented; per-target SBOMs are future work. The release
workflow regenerates the SBOM against the release tag and attaches it to the GitHub Release for
downstream consumers and security tooling.

### 12.4 Vulnerability scanning

- **PR Dependency Review** (`dependency-review.yml`) is the **blocking** gate: it fails a PR that
  introduces a HIGH-or-above severity CVE.
- **OSV-Scanner** (`osv-scanner.yml`) runs weekly and on demand as two **advisory** (non-blocking)
  scans that upload SARIF to the Security tab: one over the runtime SBOM (what ships to consumers)
  and one over the verification metadata (build/plugin classpath). Advisories confined to the build
  classpath can be suppressed in `osv-scanner.toml` once triaged — prefer letting Renovate bump the
  affected plugin over adding a permanent suppression.

### 12.5 Pinned GitHub Actions

Every `uses:` reference in the workflows is pinned to a full commit SHA with a trailing version
comment (e.g. `actions/checkout@<sha> # v6.0.3`). Tags are mutable and can be repointed at malicious
commits; a SHA is immutable. Renovate keeps both the digest and the version comment up to date via
`helpers:pinGitHubActionDigests`, so pinning does not mean the actions go stale.
