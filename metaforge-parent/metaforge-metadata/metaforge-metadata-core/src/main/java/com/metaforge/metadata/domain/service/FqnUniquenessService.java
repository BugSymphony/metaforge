package com.metaforge.metadata.domain.service;

import com.metaforge.metadata.domain.repository.MetadataEntityDraftRepository;
import com.metaforge.metadata.domain.repository.MetadataEntityRepository;
import org.springframework.stereotype.Component;

@Component
public class FqnUniquenessService {

    private final MetadataEntityRepository entityRepository;
    private final MetadataEntityDraftRepository draftRepository;

    public FqnUniquenessService(MetadataEntityRepository entityRepository,
                                MetadataEntityDraftRepository draftRepository) {
        this.entityRepository = entityRepository;
        this.draftRepository = draftRepository;
    }

    public boolean isFqnUnique(String fqn) {
        return !entityRepository.existsByFqn(fqn) && !draftRepository.existsByFqn(fqn);
    }

    public boolean existsInMainTable(String fqn) {
        return entityRepository.existsByFqn(fqn);
    }

    public boolean existsInDraftTable(String fqn) {
        return draftRepository.existsByFqn(fqn);
    }
}
