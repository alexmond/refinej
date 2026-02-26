package org.alexmond.refinej.cli.commands;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import org.springframework.stereotype.Component;

/**
 * {@code refinej refactor} — parent command; delegates to {@code rename} and
 * {@code move}.
 */
@Component
@Command(name = "refactor", description = "Apply semantic refactoring operations.", mixinStandardHelpOptions = true,
		subcommands = { RefactorRenameCommand.class, RefactorMoveCommand.class })
public class RefactorCommand implements Callable<Integer> {

	@Spec
	CommandSpec spec;

	@Override
	public Integer call() {
		this.spec.commandLine().usage(System.out);
		return 0;
	}

}
