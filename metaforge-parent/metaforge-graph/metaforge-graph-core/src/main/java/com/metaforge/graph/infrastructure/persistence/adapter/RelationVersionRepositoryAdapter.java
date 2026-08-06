package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.domain.model.aggregate.RelationVersion;
import com.metaforge.graph.domain.repository.RelationVersionRepository;
import com.metaforge.graph.infrastructure.converter.RelationVersionConverter;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationVersionJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationVersionJpo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 历史表仓储端口适配器。
 */
@Component
public class RelationVersionRepositoryAdapter implements RelationVersionRepository {

    private final RelationVersionJpaRepository jpaRepository;
    private final RelationVersionConverter converter;

    public RelationVersionRepositoryAdapter(RelationVersionJpaRepository jpaRepository,
                                             RelationVersionConverter converter) {
        this.jpaRepository = jpaRepository;
        this.converter = converter;
    }

    @Override
    public RelationVersion save(RelationVersion version) {
        RelationVersionJpo jpo = converter.toJpo(version);
        RelationVersionJpo saved = jpaRepository.save(jpo);
        return converter.toDomain(saved);
    }

    @Override
    public List<RelationVersion> findByFqnOrderByVersionDesc(String fqn) {
        return jpaRepository.findByFqnOrderByVersionDesc(fqn).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RelationVersion> findByFqnAndVersion(String fqn, Integer version) {
        return jpaRepository.findByFqnAndVersion(fqn, version).map(converter::toDomain);
    }

    @Override
    public List<RelationVersion> findByFqnIn(List<String> fqns) {
        return jpaRepository.findByFqnIn(fqns).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
}
