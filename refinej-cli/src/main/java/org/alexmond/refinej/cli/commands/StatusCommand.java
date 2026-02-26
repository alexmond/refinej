package org.alexmond.refinej.cli.commands;

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.alexmond.refinej.core.engine.api.RefactoringEngine;
import org.alexmond.refinej.core.model.JsonDto;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code refinej status} — show the active engine and current index statistics.
 */
@Slf4j
@Component
@Command(name = "status", description = "Show the active engine and index state.", mixinStandardHelpOptions = true)
public class StatusCommand implements Callable<Integer> {

	@Autowired
	private RefactoringEngine engine;

	private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	@Option(names = "--json", description = "Output as JSON.")
	private boolean json;

	@Override
	public Integer call() {
		long symbolCount = this.engine.getAllSymbols().size();
		long refCount = this.engine.getAllReferences().size();
		String engineName = this.engine.getType().name().toLowerCase();
		String ver = readVersion();

		if (this.json) {
			try {
				System.out.println(this.objectMapper.writeValueAsString(
						new JsonDto.StatusResponse("ok", engineName, ".refinej/index", symbolCount, refCount, ver)));
			}
			catch (Exception ex) {
				System.err.println("{\"status\":\"error\",\"message\":\"" + ex.getMessage() + "\"}");
				return 1;
			}
		}
		else {
			System.out.printf("""
					Engine  : %s
					Index   : .refinej/index
					Symbols : %d
					Refs    : %d
					Version : %s
					""", engineName, symbolCount, refCount, ver);
		}
		return 0;
	}

	private String readVersion() {
		try (InputStream is = getClass()
			.getResourceAsStream("/META-INF/maven/org.alexmond.refinej/refinej-cli/pom.properties")) {
			if (is != null) {
				Properties p = new Properties();
				p.load(is);
				return p.getProperty("version", "0.1.0-SNAPSHOT");
			}
		}
		catch (Exception ex) {
			log.debug("Could not read pom.properties: {}", ex.getMessage());
		}
		return "0.1.0-SNAPSHOT";
	}

}
