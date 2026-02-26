---
description: Implement a specific feature, engine method, or CLI command in the RefineJ project. Describe what you want to implement and this skill will guide you through the right files, patterns, and steps.
argument-hint: <feature description>
---
Implement the following in the RefineJ project: **$ARGUMENTS**

Before writing any code:
1. Read `CLAUDE.md` (repo root) to confirm module placement rules.
2. Read `docs/ROADMAP.adoc` to identify the relevant phase and ticket number(s).
3. Read `docs/DETAILED-DESIGN.adoc` for interface signatures and design constraints.
4. Identify which files need to change using Glob/Grep — never guess file locations.

## Module placement rules (from CLAUDE.md)
- Domain records, interfaces, enums → `refinej-core`
- Engine logic → the relevant `refinej-engine-*` module only
- CLI commands → `refinej-cli`
- Engine modules must **not** import from `refinej-cli` or other engine modules

## Code style
- Use Lombok `@Slf4j` for logging, `@RequiredArgsConstructor` for constructor injection
- Spring beans: `@Component`, `@Configuration`, `@Service`
- Tests use JUnit 5 + AssertJ; no Mockito unless explicitly needed
- Records are immutable; avoid adding setters

After implementing, run `/test-contract` to verify all engine contract tests still pass, then run `/build` to confirm compilation.
