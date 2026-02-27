package org.alexmond.refinej.core;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot configuration for JPA integration tests in the refinej-core module
 * (which has no {@code @SpringBootApplication}). Explicitly scans only the JPA entities,
 * repositories, and services so that engine-related beans (which require engine modules)
 * are never loaded.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("org.alexmond.refinej.core.domain")
@EnableJpaRepositories("org.alexmond.refinej.core.repository")
@ComponentScan("org.alexmond.refinej.core.service")
class TestConfiguration {

}
