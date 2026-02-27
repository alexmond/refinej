package org.alexmond.refinej.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * JPA entity for a persisted symbol reference. Maps to the {@code reference} table. The
 * in-memory domain object is {@link Reference}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "symbol")
@Entity
@Table(name = "reference")
public class ReferenceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JoinColumn(name = "symbol_id", nullable = false)
	private SymbolEntity symbol;

	@Column(name = "file_path", nullable = false, length = 1000)
	private String filePath;

	private int line;

	@Column(name = "col")
	private int col;

	@Enumerated(EnumType.STRING)
	@Column(name = "usage_kind", nullable = false, length = 30)
	private UsageKind usageKind;

}
