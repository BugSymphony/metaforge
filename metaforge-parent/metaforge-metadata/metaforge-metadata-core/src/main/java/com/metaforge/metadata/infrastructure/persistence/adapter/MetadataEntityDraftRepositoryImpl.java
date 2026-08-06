package com.metaforge.metadata.infrastructure.persistence.adapter;

import com.metaforge.metadata.domain.model.aggregate.MetadataEntityDraft;
import com.metaforge.metadata.domain.repository.MetadataEntityDraftRepository;
import com.metaforge.metadata.infrastructure.mapper.MetadataDraftMapper;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityDraftJpo;
import com.metaforge.metadata.infrastructure.persistence.jpa.MetadataEntityDraftJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class MetadataEntityDraftRepositoryImpl implements MetadataEntityDraftRepository {

    private final MetadataEntityDraftJpaRepository jpaRepository;
    private final MetadataDraftMapper mapper;

    public MetadataEntityDraftRepositoryImpl(MetadataEntityDraftJpaRepository jpaRepository, MetadataDraftMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MetadataEntityDraft save(MetadataEntityDraft draft) {
        MetadataEntityDraftJpo jpo = mapper.toJpo(draft);
        MetadataEntityDraftJpo saved = jpaRepository.save(jpo);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MetadataEntityDraft> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(mapper::toDomain);
    }

    @Override
    public boolean existsByFqn(String fqn) {
        return jpaRepository.existsByFqn(fqn);
    }

    @Override
    public void delete(MetadataEntityDraft draft) {
        MetadataEntityDraftJpo jpo = mapper.toJpo(draft);
        jpaRepository.delete(jpo);
    }

    @Override
    public void deleteByFqn(String fqn) {
        jpaRepository.deleteByFqn(fqn);
    }
}
