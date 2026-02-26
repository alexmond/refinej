---
description: Add a new operation to the RefactoringEngine interface and stub it out in all three engine implementations (SpoonEngine, OpenRewriteEngine, JavaParserEngine). Always use this skill when extending the engine contract to keep all three engines in sync.
argument-hint: <method signature and description>
---
Add the following new operation to the `RefactoringEngine` interface and stub it in all three engines: **$ARGUMENTS**

## Step-by-step process

### 1. Update the interface
File: `refinej-core/src/main/java/org/alexmond/refinej/core/engine/api/RefactoringEngine.java`
- Add the new method signature with a Javadoc comment referencing the relevant RFJ ticket
- Follow the existing method naming convention

### 2. Stub all three engines
Each must implement the new method. Stub pattern:
```java
@Override
public ReturnType newMethod(params) {
    // TODO RFJ-XXX: not yet implemented
    throw new UnsupportedOperationException("newMethod not implemented — see RFJ-XXX");
}
```
Files to update:
- `refinej-engine-spoon/src/main/java/org/alexmond/refinej/engine/spoon/SpoonEngine.java`
- `refinej-engine-rewrite/src/main/java/org/alexmond/refinej/engine/rewrite/OpenRewriteEngine.java`
- `refinej-engine-javaparser/src/main/java/org/alexmond/refinej/engine/javaparser/JavaParserEngine.java`

### 3. Add contract test assertions
File: `refinej-cli/src/test/java/org/alexmond/refinej/cli/EngineContractTest.java`
- Add a new `@ParameterizedTest` method for the new operation
- For stub-phase: assert the method throws `UnsupportedOperationException` OR returns a defined default

### 4. Build and test
Run `/test-contract` to verify all engines satisfy the updated contract.
