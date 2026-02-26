package org.alexmond.refinej.cli.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * {@code refinej query} — parent command; delegates to {@code symbol} and {@code refs}.
 */
@Component
@Command(
        name = "query",
        description = "Query the project symbol index.",
        mixinStandardHelpOptions = true,
        subcommands = {
                QuerySymbolCommand.class,
                QueryRefsCommand.class
        }
)
public class QueryCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }
}
