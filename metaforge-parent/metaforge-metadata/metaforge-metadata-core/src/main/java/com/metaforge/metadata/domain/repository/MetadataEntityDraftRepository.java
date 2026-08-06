package com.metaforge.metadata.domain.repository;

import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import java.util.Optional;

public interface MetadataEntityDraftRepository {
    MetadataEntityDraft save(MetadataEntityDraft draft);
    Optional<MetadataEntityDraft> findByFqn(String fqn);
    boolean existsByFqn(String fqn);
    void delete(MetadataEntityDraft draft);
    void deleteByFqn(String fqn);
}
