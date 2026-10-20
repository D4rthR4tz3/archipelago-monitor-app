# Commit messages

Commits follow [Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) with a
leading [gitmoji](https://gitmoji.dev), as defined by the prompt
[conventional-commit-with-gitmoji-ai-prompt](https://github.com/theorib/git-commit-message-ai-prompt/blob/main/prompts/conventional-commit-with-gitmoji-ai-prompt.md).
That prompt is the source of truth; this page summarizes it. When an AI assistant writes a commit
message for this repository, it uses that prompt.

## Format

```text
<emoji> <type>(<scope>): <description>

- <what changed and why>
- <long bullet points wrap onto the next line
  without a new dash>

<footer>: <value>
```

## Subject line

- English, imperative mood, lowercase, no period at the end.
- Scope is optional, in English, and names one component (`protocol`, `app`, `ui`, `deps`).
  Omit it when the change spans unrelated areas.
- Maximum 100 characters, emoji included.

## Body

- Optional; omit it when the subject says everything.
- Bullet points with `-`, at most 100 characters per line.
- Factual and verifiable from the diff. No vague qualifiers like "for clarity" unless the diff
  is a pure formatting change.
- Dependency updates list every changed direct dependency as `name: old → new`.

## Footer

- `token: value` (or `token #value`), at most 100 characters per line.
- `BREAKING CHANGE: …` for incompatible changes (or `!` before the colon in the subject).
- Issue references: `Fixes #12`, `Closes #34`, `Refs #56`.
- No `Co-authored-by` footers, see [attribution](attribution.md).

## Types

| Emoji | Type     | Use for                                                    |
| ----- | -------- | ---------------------------------------------------------- |
| 🏗️    | build    | Build system or external dependencies                      |
| 🔧    | chore    | Other changes that do not touch source or test files       |
| 👷    | ci       | CI configuration and scripts                               |
| 📝    | docs     | Documentation only                                         |
| ✨    | feat     | A new feature                                              |
| 🐛    | fix      | A bug fix                                                  |
| ⚡️    | perf     | A performance improvement                                  |
| ♻️    | refactor | Code change that neither fixes a bug nor adds a feature    |
| ⏪️    | revert   | Reverting a previous commit                                |
| 💄    | style    | Formatting only, no change in meaning                      |
| ✅    | test     | Adding or correcting tests                                 |
| 🌐    | i18n     | Internationalization and translations                      |

## One commit, one concern

A commit covers one logical change. If a diff contains unrelated changes, split it into several
commits. (The prompt also allows several subject blocks in one message for unrelated changes;
prefer separate commits.)

## Examples

```text
✨ feat(protocol): add websocket client for the archipelago server

- connect via wss:// and fall back to ws:// when the secure handshake fails
- answer RoomInfo with GetDataPackage and Connect using the Tracker tag
```

```text
🔧 chore(deps): update okhttp to 5.5.0
```
