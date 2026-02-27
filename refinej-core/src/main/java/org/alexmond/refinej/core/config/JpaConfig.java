package org.alexmond.refinej.core.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers the JPA entity and repository base packages for the refinej-core module.
 *
 * <p>
 * Without this, Spring Boot's auto-configuration would only scan the {@code refinej-cli}
 * application package and miss the entities and repositories that live in
 * {@code refinej-core}.
 */
@Configuration
@EntityScan("org.alexmond.refinej.core.domain")
@EnableJpaRepositories("org.alexmond.refinej.core.repository")
public class JpaConfig {

}
