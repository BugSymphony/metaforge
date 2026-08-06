package com.metaforge.graph.infrastructure.persistence.adapter;

import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.model.valueobject.FQN;
import com.metaforge.graph.domain.repository.RelationInstanceRepository;
import com.metaforge.graph.infrastructure.converter.RelationInstanceConverter;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 主表仓储端口适配器。
 */
@Component
public class RelationInstanceRepositoryAdapter implements RelationInstanceRepository {

    private final RelationInstanceJpaRepository jpaRepository;
    private final RelationInstanceConverter converter;

    public RelationInstanceRepositoryAdapter(RelationInstanceJpaRepository jpaRepository,
                                              RelationInstanceConverter converter) {
        this.jpaRepository = jpaRepository;
        this.converter = converter;
    }

    @Override
    public Optional<RelationInstance> findByFqn(FQN fqn) {
        return jpaRepository.findByFqn(fqn.getValue()).map(converter::toDomain);
    }

    @Override
    public Optional<RelationInstance> findByFqnString(String fqn) {
        return jpaRepository.findByFqn(fqn).map(converter::toDomain);
    }

    @Override
    public RelationInstance save(RelationInstance instance) {
        RelationInstanceJpo jpo = converter.toJpo(instance);
        RelationInstanceJpo saved = jpaRepository.save(jpo);
        return converter.toDomain(saved);
    }

    @Override
    public void deleteByFqn(FQN fqn) {
        jpaRepository.deleteByFqn(fqn.getValue());
    }

    @Override
    public boolean existsByFqn(FQN fqn) {
        return jpaRepository.existsByFqn(fqn.getValue());
    }

    @Override
    public boolean existsByFqnString(String fqn) {
        return jpaRepository.existsByFqn(fqn);
    }

    @Override
    public List<RelationInstance> findBySourceEntityFqn(String sourceEntityFqn) {
        return jpaRepository.findBySourceEntityFqn(sourceEntityFqn).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RelationInstance> findByTargetEntityFqn(String targetEntityFqn) {
        return jpaRepository.findByTargetEntityFqn(targetEntityFqn).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RelationInstance> findByFqnPrefix(String fqnPrefix) {
        return jpaRepository.findByFqnStartingWith(fqnPrefix).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByRelationTypeAndSourceEntityFqn(String relationType, String sourceEntityFqn) {
        return jpaRepository.countByRelationTypeAndSourceEntityFqn(relationType, sourceEntityFqn);
    }

    @Override
    public long countByRelationTypeAndTargetEntityFqn(String relationType, String targetEntityFqn) {
        return jpaRepository.countByRelationTypeAndTargetEntityFqn(relationType, targetEntityFqn);
    }
}
