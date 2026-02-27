---
description: Add a new test or test class to the RefineJ project. Describe what you want to test and this skill will create a well-structured JUnit 5 test following project conventions.
argument-hint: <what to test>
---
Add a test for: **$ARGUMENTS**

## Test conventions in this project
- Framework: JUnit 5 (`@Test`, `@ParameterizedTest`, `@MethodSource`)
- Assertions: AssertJ (`assertThat`, `assertThatNoException`, `assertThatThrownBy`)
- No Mockito unless genuinely needed (prefer real object graphs)
- Test class name: `<ClassUnderTest>Test` or `<FeatureName>Test`
- Parameterised contract tests: follow `EngineContractTest` as the reference pattern

## Steps
1. Identify the right module and package for the test.
2. Check if a test class already exists for this area — read it before adding.
3. Write the test(s) following the conventions above.
4. Run the tests:

```bash
cd /Users/alex.mondshain/IdeaProjects/refinej && ./mvnw test -pl <module>
```

5. Confirm all tests pass (including pre-existing ones).

## Test fixture location
Shared Java fixtures go in `refinej-cli/src/test/resources/fixtures/`.
The `simple/` fixture has `Greeter.java` and `App.java` — reuse it for engine tests.
