package org.alexmond.refinej.core.repository;

import java.util.List;
import java.util.Optional;

import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.SymbolKind;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for {@link SymbolEntity}.
 */
public interface SymbolRepository extends JpaRepository<SymbolEntity, Long> {

	Optional<SymbolEntity> findByQualifiedName(String qualifiedName);

	List<SymbolEntity> findByKind(SymbolKind kind);

	List<SymbolEntity> findByFilePathStartingWith(String pathPrefix);

	@Transactional
	void deleteByFilePath(String filePath);

}
