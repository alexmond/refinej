package org.alexmond.refinej.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DiffGenerator}.
 */
class DiffGeneratorTests {

	private final DiffGenerator diffGenerator = new DiffGenerator();

	@Test
	void identicalContent_returnsEmptyDiff() {
		String content = "package com.example;\n\npublic class Foo {}\n";
		assertThat(this.diffGenerator.generateUnifiedDiff("Foo.java", content, content)).isEmpty();
	}

	@Test
	void singleLineChange_producesUnifiedDiff() {
		String original = "package com.example;\n\npublic class Foo {}\n";
		String modified = "package com.example;\n\npublic class Bar {}\n";

		String diff = this.diffGenerator.generateUnifiedDiff("Foo.java", original, modified);

		assertThat(diff).contains("--- a/Foo.java");
		assertThat(diff).contains("+++ b/Foo.java");
		assertThat(diff).contains("-public class Foo {}");
		assertThat(diff).contains("+public class Bar {}");
	}

	@Test
	void multipleChanges_producesCorrectDiff() {
		String original = "line1\nline2\nline3\nline4\nline5\n";
		String modified = "line1\nchanged2\nline3\nline4\nchanged5\n";

		String diff = this.diffGenerator.generateUnifiedDiff("test.txt", original, modified);

		assertThat(diff).contains("-line2");
		assertThat(diff).contains("+changed2");
		assertThat(diff).contains("-line5");
		assertThat(diff).contains("+changed5");
	}

	@Test
	void emptyOriginal_producesAdditionDiff() {
		String diff = this.diffGenerator.generateUnifiedDiff("new.java", "", "public class New {}\n");

		assertThat(diff).contains("+public class New {}");
	}

}
