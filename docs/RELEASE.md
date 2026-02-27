# Release Process

## Prerequisites

- All tests pass: `./scripts/mvnw.sh verify`
- CHANGELOG.md is up to date
- You are on the `main` branch with a clean working tree

## Steps

1. **Update version**:
   ```bash
   ./scripts/mvnw.sh versions:set -DnewVersion=0.1.0
   ./scripts/mvnw.sh versions:commit
   ```

2. **Update CHANGELOG.md**: Change `[Unreleased]` to `[0.1.0] - YYYY-MM-DD`

3. **Run full verification**:
   ```bash
   ./scripts/mvnw.sh clean verify
   ```

4. **Commit the release**:
   ```bash
   git add -A
   git commit -m "Release v0.1.0"
   ```

5. **Tag the release**:
   ```bash
   git tag v0.1.0
   git push origin main --tags
   ```

6. **Create GitHub Release**: Use `gh release create v0.1.0` or the GitHub UI. Attach `refinej-cli/target/refinej-cli-0.1.0.jar`.

7. **Bump to next snapshot**:
   ```bash
   ./scripts/mvnw.sh versions:set -DnewVersion=0.2.0-SNAPSHOT
   ./scripts/mvnw.sh versions:commit
   git add -A
   git commit -m "Bump version to 0.2.0-SNAPSHOT"
   git push origin main
   ```
