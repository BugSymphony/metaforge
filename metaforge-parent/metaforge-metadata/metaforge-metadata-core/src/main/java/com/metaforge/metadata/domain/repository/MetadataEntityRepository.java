package com.metaforge.metadata.domain.repository;

import com.metaforge.metadata.domain.model.aggregate.MetadataEntity;
import java.util.List;
import java.util.Optional;

public interface MetadataEntityRepository {
    MetadataEntity save(MetadataEntity entity);
    Optional<MetadataEntity> findByFqn(String fqn);
    List<MetadataEntity> findByFqnPrefixIn(List<String> fqnPrefixes);
    List<MetadataEntity> findByEntitySchemaFqn(String entitySchemaFqn);
    boolean existsByFqn(String fqn);
    boolean existsByFqnPrefix(String fqnPrefix);
    List<String> findFqnsByParentFqnPrefix(String parentFqnPrefix);
    void delete(MetadataEntity entity);
    void deleteByFqn(String fqn);
}
