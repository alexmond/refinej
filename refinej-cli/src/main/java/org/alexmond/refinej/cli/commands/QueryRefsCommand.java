package org.alexmond.refinej.cli.commands;

import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.alexmond.refinej.core.domain.Reference;
import org.alexmond.refinej.core.domain.UsageKind;
import org.alexmond.refinej.core.model.JsonDto;
import org.alexmond.refinej.core.service.QueryService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code refinej query refs} — find all usages of a symbol.
 */
@Component
@Command(name = "refs", description = "Find all usages of a symbol.", mixinStandardHelpOptions = true)
public class QueryRefsCommand implements Callable<Integer> {

	@Autowired
	private QueryService queryService;

	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	@Option(names = "--name", required = true, description = "Fully-qualified name of the symbol.")
	private String name;

	@Option(names = "--kind", description = "Filter by usage kind (e.g. METHOD_CALL, IMPORT).")
	private String kind;

	@Option(names = "--json", description = "Output as JSON.")
	private boolean json;

	@Override
	public Integer call() {
		List<Reference> refs;
		if (this.kind != null && !this.kind.isBlank()) {
			UsageKind usageKind = UsageKind.valueOf(this.kind.toUpperCase());
			refs = this.queryService.findReferences(this.name, usageKind);
		}
		else {
			refs = this.queryService.findReferences(this.name);
		}

		if (this.json) {
			List<JsonDto.ReferenceItem> items = refs.stream()
				.map((r) -> new JsonDto.ReferenceItem(r.filePath(), r.line(), r.column(), r.usageKind().name()))
				.toList();
			try {
				System.out.println(this.objectMapper
					.writeValueAsString(new JsonDto.RefsResponse("ok", this.name, refs.size(), items)));
			}
			catch (Exception ex) {
				System.err.println("{\"status\":\"error\",\"message\":\"" + ex.getMessage() + "\"}");
				return 1;
			}
		}
		else {
			if (refs.isEmpty()) {
				System.out.println("No references found for: " + this.name);
				return 0;
			}
			System.out.printf("%d reference(s) to %s:%n", refs.size(), this.name);
			refs.forEach((r) -> System.out.printf("  %-20s %s:%d%n", r.usageKind(), r.filePath(), r.line()));
		}
		return 0;
	}

}
