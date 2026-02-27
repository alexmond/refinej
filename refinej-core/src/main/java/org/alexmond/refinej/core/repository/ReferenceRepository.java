package org.alexmond.refinej.core.repository;

import java.util.List;

import org.alexmond.refinej.core.domain.ReferenceEntity;
import org.alexmond.refinej.core.domain.SymbolEntity;
import org.alexmond.refinej.core.domain.UsageKind;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JPA repository for {@link ReferenceEntity}.
 */
public interface ReferenceRepository extends JpaRepository<ReferenceEntity, Long> {

	List<ReferenceEntity> findBySymbol(SymbolEntity symbol);

	List<ReferenceEntity> findBySymbolAndUsageKind(SymbolEntity symbol, UsageKind usageKind);

	@Query("SELECT r FROM ReferenceEntity r WHERE r.symbol.qualifiedName = :qualifiedName")
	List<ReferenceEntity> findByQualifiedName(@Param("qualifiedName") String qualifiedName);

	List<ReferenceEntity> findByFilePath(String filePath);

	@Transactional
	void deleteByFilePath(String filePath);

}
