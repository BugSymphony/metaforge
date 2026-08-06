package com.metaforge.graph.application.service;

import com.metaforge.graph.api.constant.GraphConstants;
import com.metaforge.graph.api.dto.RelationCount;
import com.metaforge.graph.api.dto.TopologyValidationReport;
import com.metaforge.graph.api.dto.TopologyValidationRequest;
import com.metaforge.graph.api.service.RelationTopologyService;
import com.metaforge.graph.domain.service.DependencyCheckService;
import com.metaforge.graph.infrastructure.persistence.jpa.EntityRelationIndexJpaRepository;
import com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * 关系拓扑管理与校验服务实现。
 */
@Service
@Transactional(readOnly = true)
public class RelationTopologyServiceImpl implements RelationTopologyService {

    private static final Logger log = LoggerFactory.getLogger(RelationTopologyServiceImpl.class);

    private final DependencyCheckService dependencyCheckService;
    private final EntityRelationIndexJpaRepository indexJpaRepository;
    private final RelationInstanceJpaRepository instanceJpaRepository;

    public RelationTopologyServiceImpl(DependencyCheckService dependencyCheckService,
                                        EntityRelationIndexJpaRepository indexJpaRepository,
                                        RelationInstanceJpaRepository instanceJpaRepository) {
        this.dependencyCheckService = dependencyCheckService;
        this.indexJpaRepository = indexJpaRepository;
        this.instanceJpaRepository = instanceJpaRepository;
    }

    @Override
    public List<String> getDependentRelations(String entityFqn) {
        log.debug("查询实体关联依赖关系: entity={}", entityFqn);
        // 查询所有源端为该实体的 DEPENDENCY_INFLUENCE 关系
        return instanceJpaRepository.findBySourceEntityFqn(entityFqn).stream()
                .filter(jpo -> "DEPENDENCY_INFLUENCE".equals(jpo.getRelationType()))
                .map(jpo -> jpo.getFqn())
                .toList();
    }

    @Override
    public TopologyValidationReport validateTopology(TopologyValidationRequest request) {
        log.info("批量拓扑完整性校验: prefix={}, type={}", request.getFqnPrefix(), request.getRelationType());

        TopologyValidationReport report = TopologyValidationReport.empty();

        if (request.getFqnPrefix() != null && !request.getFqnPrefix().isEmpty()) {
            List<com.metaforge.graph.infrastructure.persistence.jpa.RelationInstanceJpo> jpos =
                    instanceJpaRepository.findAll(
                            com.metaforge.graph.infrastructure.persistence.adapter.RelationQuerySpecification
                                    .fqnPrefix(request.getFqnPrefix()),
                            PageRequest.of(0, Integer.MAX_VALUE)).getContent();

            report.setTotalChecked(jpos.size());
        }

        return report;
    }

    @Override
    public RelationCount getRelationCount(String entityFqn) {
        log.debug("查询实体关系计数: entity={}", entityFqn);
        long outbound = indexJpaRepository
                .findByEntityFqnAndDirection(entityFqn, GraphConstants.DIRECTION_OUTBOUND).size();
        long inbound = indexJpaRepository
                .findByEntityFqnAndDirection(entityFqn, GraphConstants.DIRECTION_INBOUND).size();
        return RelationCount.of(entityFqn, outbound, inbound);
    }
}
