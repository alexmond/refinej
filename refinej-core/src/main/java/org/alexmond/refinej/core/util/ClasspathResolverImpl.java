package org.alexmond.refinej.core.util;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.engine.api.BuildType;
import org.alexmond.refinej.core.engine.api.ClasspathResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves the compile classpath by invoking the project's build tool.
 *
 * <p>Phase 1: functional stub that invokes Maven's
 * {@code dependency:build-classpath} goal. Gradle support is stubbed for Phase 2.
 * Failure is non-fatal — engines fall back to no-classpath mode (reduced accuracy).
 */
@Slf4j
@Component
public class ClasspathResolverImpl implements ClasspathResolver {

    private static final String CP_OUTPUT_FILE = ".refinej/classpath.txt";

    @Override
    public List<Path> resolve(Path projectRoot, BuildType buildType) {
        return switch (buildType) {
            case MAVEN  -> resolveMaven(projectRoot);
            case GRADLE -> resolveGradle(projectRoot);
            default -> {
                log.warn("Unknown build type for {}; classpath resolution skipped", projectRoot);
                yield List.of();
            }
        };
    }

    private List<Path> resolveMaven(Path projectRoot) {
        Path outputFile = projectRoot.resolve(CP_OUTPUT_FILE);
        try {
            Files.createDirectories(outputFile.getParent());
            ProcessBuilder pb = new ProcessBuilder(
                    "mvn", "-q", "dependency:build-classpath",
                    "-Dmdep.outputFile=" + outputFile.toAbsolutePath())
                    .directory(projectRoot.toFile())
                    .redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                br.lines().forEach(line -> log.debug("mvn: {}", line));
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Maven classpath resolution exited with code {}; using no-classpath mode",
                        exitCode);
                return List.of();
            }

            if (!Files.exists(outputFile)) return List.of();

            String cpLine = Files.readString(outputFile).trim();
            return Arrays.stream(cpLine.split(":"))
                    .filter(s -> !s.isBlank())
                    .map(Path::of)
                    .filter(Files::exists)
                    .toList();

        } catch (Exception e) {
            log.warn("Maven classpath resolution failed: {}; using no-classpath mode",
                    e.getMessage());
            return List.of();
        }
    }

    private List<Path> resolveGradle(Path projectRoot) {
        // TODO RFJ-020: invoke `gradle dependencies --configuration compileClasspath`
        log.warn("Gradle classpath resolution not yet implemented; using no-classpath mode");
        return List.of();
    }
}
