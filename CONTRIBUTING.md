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
- Use lower-case for the description.
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

- [CloudEvents specification](https://github.com/cloudevents/spec) — the normative
  source for event format, attributes, and protocol bindings
- [CloudEvents SDK requirements](https://github.com/cloudevents/spec/blob/main/cloudevents/SDK.md) —
  version-support policy and SDK conformance requirements
- [CloudEvents website](https://cloudevents.io) — overview, links, and community resources
