package com.metaforge.graph.domain.repository;

import com.metaforge.graph.domain.model.aggregate.RelationInstanceDraft;
import java.util.Optional;

/**
 * 草稿表仓储端口接口。
 */
public interface RelationInstanceDraftRepository {

    Optional<RelationInstanceDraft> findByFqn(String fqn);

    RelationInstanceDraft save(RelationInstanceDraft draft);

    void deleteByFqn(String fqn);

    boolean existsByFqn(String fqn);
}
