package org.alexmond.refinej.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity for a persisted Java symbol. Maps to the {@code symbol} table. The in-memory
 * domain object is {@link Symbol}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "symbol")
public class SymbolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SymbolKind kind;

	@Column(name = "simple_name", length = 255)
	private String simpleName;

	@Column(name = "qualified_name", nullable = false, unique = true, length = 1000)
	private String qualifiedName;

	@Column(name = "file_path", length = 1000)
	private String filePath;

	@Column(name = "line_start")
	private int lineStart;

	@Column(name = "line_end")
	private int lineEnd;

	@Column(name = "type_fqn", length = 1000)
	private String typeFqn;

}
