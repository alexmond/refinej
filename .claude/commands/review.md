---
description: Review the current staged/unstaged changes in the RefineJ repo — check for correctness, test coverage, adherence to module dependency rules, code style, and potential issues before committing.
---
Review the current changes in the RefineJ repository.

1. Gather the diff:
```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && git diff HEAD
git status
```

2. For each changed file, read the full file to understand context.

3. Check for:

### Module dependency violations
- Engine modules must only import from `refinej-core` and their engine library (Spoon/OpenRewrite/JavaParser)
- `refinej-core` must NOT import engine-specific classes
- `refinej-cli` is the only module allowed to import from all others

### Code quality
- All new `@Component`/`@Service`/`@Configuration` beans use `@RequiredArgsConstructor` (not `@Autowired` on fields) unless the field is `@Autowired` in a Picocli command (where constructor injection is not available)
- New log statements use `@Slf4j` — no `System.out.println` except in CLI command output
- Records are truly immutable — no mutable fields
- Exception handling is specific — no bare `catch (Exception e) { return null; }`

### Test coverage
- Any new public method in `RefactoringEngine` has a contract test in `EngineContractTest`
- New CLI commands have at least a smoke test

### Consistency
- New engine methods are stubbed in all three engines
- New domain types are Java records (not classes)

4. Run the contract tests to verify:
```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./scripts/mvnw.sh test -pl refinej-cli -Dtest=EngineContractTest
```

5. Provide a structured review with: ✅ Good, ⚠️ Minor issue, ❌ Must fix.
