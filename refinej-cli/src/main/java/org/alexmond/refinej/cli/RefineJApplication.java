package org.alexmond.refinej.cli;

import org.alexmond.refinej.cli.commands.RefinejCommand;
import org.alexmond.refinej.cli.commands.SpringPicocliFactory;
import picocli.CommandLine;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

/**
 * RefineJ CLI entry point.
 *
 * <p>
 * Spring Boot starts the application context (which wires all engine beans, resolvers,
 * etc.). Picocli then parses the command line and dispatches to the appropriate command,
 * using {@link SpringPicocliFactory} so that Spring-managed beans are injected into
 * commands.
 */
@SpringBootApplication(scanBasePackages = "org.alexmond.refinej")
public class RefineJApplication {

	public static void main(String[] args) {
		// Start Spring context without passing args (avoids conflicts with Picocli option
		// names)
		ApplicationContext ctx = new SpringApplicationBuilder(RefineJApplication.class).web(WebApplicationType.NONE)
			.bannerMode(Banner.Mode.OFF)
			.run();

		int exitCode = new CommandLine(ctx.getBean(RefinejCommand.class), ctx.getBean(SpringPicocliFactory.class))
			.setCaseInsensitiveEnumValuesAllowed(true)
			.execute(args);

		System.exit(exitCode);
	}

}
