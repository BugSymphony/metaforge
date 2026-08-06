package com.metaforge.graph.api.service;

import com.metaforge.graph.api.dto.RelationCount;
import com.metaforge.graph.api.dto.TopologyValidationReport;
import com.metaforge.graph.api.dto.TopologyValidationRequest;

import java.util.List;

/**
 * 关系拓扑管理与校验服务。
 */
public interface RelationTopologyService {

    List<String> getDependentRelations(String entityFqn);

    TopologyValidationReport validateTopology(TopologyValidationRequest request);

    RelationCount getRelationCount(String entityFqn);
}
