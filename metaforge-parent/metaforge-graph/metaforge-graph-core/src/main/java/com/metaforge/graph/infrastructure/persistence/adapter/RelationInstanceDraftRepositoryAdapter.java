package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.domain.model.aggregate.RelationInstanceDraft;
import com.metaforge.graph.domain.repository.RelationInstanceDraftRepository;
import com.metaforge.graph.infrastructure.converter.RelationDraftConverter;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceDraftJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceDraftJpo;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 草稿表仓储端口适配器。
 */
@Component
public class RelationInstanceDraftRepositoryAdapter implements RelationInstanceDraftRepository {

    private final RelationInstanceDraftJpaRepository jpaRepository;
    private final RelationDraftConverter converter;

    public RelationInstanceDraftRepositoryAdapter(RelationInstanceDraftJpaRepository jpaRepository,
                                                   RelationDraftConverter converter) {
        this.jpaRepository = jpaRepository;
        this.converter = converter;
    }

    @Override
    public Optional<RelationInstanceDraft> findByFqn(String fqn) {
        return jpaRepository.findByFqn(fqn).map(converter::toDomain);
    }

    @Override
    public RelationInstanceDraft save(RelationInstanceDraft draft) {
        RelationInstanceDraftJpo jpo = converter.toJpo(draft);
        RelationInstanceDraftJpo saved = jpaRepository.save(jpo);
        return converter.toDomain(saved);
    }

    @Override
    public void deleteByFqn(String fqn) {
        jpaRepository.deleteByFqn(fqn);
    }

    @Override
    public boolean existsByFqn(String fqn) {
        return jpaRepository.existsByFqn(fqn);
    }
}
