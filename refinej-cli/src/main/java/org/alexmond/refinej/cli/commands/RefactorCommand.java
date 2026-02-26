package org.alexmond.refinej.cli.commands;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * {@code refinej refactor} — parent command; delegates to {@code rename} and {@code move}.
 */
@Component
@Command(
        name = "refactor",
        description = "Apply semantic refactoring operations.",
        mixinStandardHelpOptions = true,
        subcommands = {
                RefactorRenameCommand.class,
                RefactorMoveCommand.class
        }
)
public class RefactorCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 0;
    }
}
