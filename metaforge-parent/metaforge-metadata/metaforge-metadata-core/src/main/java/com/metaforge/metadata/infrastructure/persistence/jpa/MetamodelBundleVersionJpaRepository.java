package com.metaforge.metadata.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetamodelBundleVersionJpaRepository extends JpaRepository<MetamodelBundleVersionJpo, Long> {

    Optional<MetamodelBundleVersionJpo> findByFqn(String fqn);

    List<MetamodelBundleVersionJpo> findByBundleFqnAndStatusOrderByCreatedTimeDesc(String bundleFqn, String status);
}
