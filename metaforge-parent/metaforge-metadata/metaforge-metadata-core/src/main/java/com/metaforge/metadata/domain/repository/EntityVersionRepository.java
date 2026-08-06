package com.metaforge.metadata.domain.repository;

import com.metaforge.metadata.domain.model.entity.EntityVersion;
import java.util.List;
import java.util.Optional;

public interface EntityVersionRepository {
    EntityVersion save(EntityVersion version);
    List<EntityVersion> findByFqnOrderByVersionDesc(String fqn);
    Optional<EntityVersion> findByFqnAndVersion(String fqn, int version);
}
