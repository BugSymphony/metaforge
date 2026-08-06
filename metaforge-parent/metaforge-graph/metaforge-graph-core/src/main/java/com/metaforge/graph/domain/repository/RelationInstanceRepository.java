package com.metaforge.graph.domain.repository;

import com.metaforge.graph.domain.model.aggregate.RelationInstance;
import com.metaforge.graph.domain.model.valueobject.FQN;
import java.util.List;
import java.util.Optional;

/**
 * 主表仓储端口接口。
 */
public interface RelationInstanceRepository {

    Optional<RelationInstance> findByFqn(FQN fqn);

    Optional<RelationInstance> findByFqnString(String fqn);

    RelationInstance save(RelationInstance instance);

    void deleteByFqn(FQN fqn);

    boolean existsByFqn(FQN fqn);

    boolean existsByFqnString(String fqn);

    List<RelationInstance> findBySourceEntityFqn(String sourceEntityFqn);

    List<RelationInstance> findByTargetEntityFqn(String targetEntityFqn);

    List<RelationInstance> findByFqnPrefix(String fqnPrefix);

    long countByRelationTypeAndSourceEntityFqn(String relationType, String sourceEntityFqn);

    long countByRelationTypeAndTargetEntityFqn(String relationType, String targetEntityFqn);
}
