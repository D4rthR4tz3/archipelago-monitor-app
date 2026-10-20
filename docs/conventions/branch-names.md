# Branch names

Branch names follow [Conventional Branch 1.1.0](https://conventionalbranch.org/v1.1.0/), with
one deviation: the AI agent prefixes (`ai/`, `copilot/`, `cursor/`, `claude/`, `codex/`) are
not used. A branch is named after the change it carries, never after the tool or person that
created it. This page summarizes the rules; where it differs from the specification, this page
wins.

## Format

```text
<type>/<description>
```

`main` is the trunk branch and has no prefix. There is no `master` or `develop` branch.

## Types

| Type       | Use for                                                     | Example                      |
| ---------- | ----------------------------------------------------------- | ---------------------------- |
| `feat/`    | A new feature or an improvement of an existing one          | `feat/hint-item-counters`    |
| `fix/`     | A bug fix                                                   | `fix/reconnect-loop`         |
| `hotfix/`  | An urgent fix for a released version                        | `hotfix/crash-on-start`      |
| `release/` | Preparing a release; the description is the version         | `release/v1.2.0`             |
| `chore/`   | Everything else: docs, build, CI, dependencies, tests, ...  | `chore/update-dependencies`  |

`chore/` also covers formatting, refactoring and translations.

The specification also allows `feature/` and `bugfix/` (and its skill prefers them); this
repository uses the short forms `feat/` and `fix/` only, so branch types match the commit types
in [commit messages](commit-messages.md).

When the type is unclear, use `feat/`.

The branch type describes the main change of the pull request. The commits on the branch keep
their own, more specific commit types (`docs`, `ci`, `refactor`, …).

## Description

- English, lowercase letters `a-z` and digits `0-9`, words separated by single hyphens.
- Dots only inside version numbers (`release/v1.2.0`, `chore/upgrade-agp-9.4.1`).
- No spaces, underscores, uppercase letters, or further slashes.
- No consecutive hyphens or dots, no hyphen next to a dot (`v1.-2`), and none at the start or
  end of the description.
- Short and descriptive: two to five words that name the feature or fix, about 50 characters
  for the whole branch name, so the branch is recognizable in a list.
- Specific: `fix/header-overflow`, not `fix/bug` or `feat/new-feature`. No random or generated
  names (`claude/project-thread-xyz`) and no personal names (`feat/john-working-on-stuff`).
- Do not repeat the type: `fix/header-bug`, not `fix/fix-header-bug`.
- When a GitHub issue exists, start with its number: `fix/issue-12-reconnect-loop`.

## Creating a branch

```sh
git checkout main
git pull origin main
git checkout -b <type>/<description>
git push -u origin <type>/<description>
```

Never create a branch named like the trunk (`main`, `master`, `develop`). When an assembled
name breaks a rule, fix it before creating the branch: lowercase everything, replace spaces and
underscores with hyphens, collapse repeated hyphens and strip leading or trailing ones.

## Validation

A branch name is valid if it matches this regular expression (the specification's regex
without the AI agent prefixes, `feature`, `bugfix`, `master` and `develop`):

```text
^(?:main|(?:feat|fix|hotfix|release|chore)/[a-z0-9]+(?:\.[a-z0-9]+)*(?:-[a-z0-9]+(?:\.[a-z0-9]+)*)*)$
```

The workflow [`branch-name.yml`](../../.github/workflows/branch-name.yml) checks every push
and pull request against this expression and fails for any other branch name.

## Lifecycle

- One branch per pull request, branched from the current `main`.
- Pull requests target `main`. Do not stack a pull request on another feature branch.
- The branch is deleted after its pull request is merged or closed.

## Examples

| Branch name                     | Valid | Notes                                  |
| ------------------------------- | ----- | -------------------------------------- |
| `feat/swipe-between-tabs`       | yes   | New feature                            |
| `fix/landscape-hints-tabs`      | yes   | Bug fix                                |
| `chore/branch-naming`           | yes   | Documentation                          |
| `release/v1.0.0`                | yes   | Release with version                   |
| `fix/issue-12-reconnect-loop`   | yes   | Fix with issue number                  |
| `claude/project-thread-kr0d56`  | no    | AI agent prefix and random description |
| `feature/add-login-page`        | no    | Use the short form `feat/`             |
| `docs/branch-naming`            | no    | Unknown type, use `chore/`             |
| `feat/Hint_Counters`            | no    | Uppercase letters and underscore       |
| `fix/reconnect--loop`           | no    | Consecutive hyphens                    |
