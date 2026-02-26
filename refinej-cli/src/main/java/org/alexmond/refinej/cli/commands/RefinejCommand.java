package org.alexmond.refinej.cli.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * Root Picocli command. Prints help when called with no subcommand.
 */
@Component
@Command(
        name = "refinej",
        description = "Semantic refactoring CLI for Java projects.",
        mixinStandardHelpOptions = true,
        version = "0.1.0-SNAPSHOT",
        subcommands = {
                IndexCommand.class,
                ReindexCommand.class,
                QueryCommand.class,
                RefactorCommand.class,
                StatusCommand.class
        }
)
public class RefinejCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }
}
