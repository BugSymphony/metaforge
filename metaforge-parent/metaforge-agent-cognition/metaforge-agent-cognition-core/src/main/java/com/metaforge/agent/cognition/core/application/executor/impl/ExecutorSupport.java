package com.metaforge.agent.cognition.core.application.executor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.agent.cognition.core.domain.port.ComputeEngineClientPort;
import com.metaforge.agent.cognition.core.domain.port.MetadataClientPort;
import com.metaforge.agent.cognition.core.domain.port.MetamodelClientPort;
import com.metaforge.computeengine.api.dto.common.EntitySummary;
import com.metaforge.computeengine.api.dto.common.RelationSummary;
import com.metaforge.computeengine.api.dto.response.ClosureResult;
import com.metaforge.computeengine.api.dto.response.GraphQueryResult;
import com.metaforge.computeengine.api.dto.response.ImpactTraceResult;
import com.metaforge.computeengine.api.enums.AssociationType;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metamodel.api.dto.NativeAttributeDto;
import com.metaforge.metamodel.api.dto.response.BundleDto;
import com.metaforge.metamodel.api.dto.response.EntitySchemaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ExecutorSupport {

    private static final Logger log = LoggerFactory.getLogger(ExecutorSupport.class);

    private final MetadataClientPort metadataClient;
    private final MetamodelClientPort metamodelClient;
    private final ComputeEngineClientPort computeEngineClient;
    private final ObjectMapper objectMapper;

    public ExecutorSupport(MetadataClientPort metadataClient,
                           MetamodelClientPort metamodelClient,
                           ComputeEngineClientPort computeEngineClient,
                           ObjectMapper objectMapper) {
        this.metadataClient = metadataClient;
        this.metamodelClient = metamodelClient;
        this.computeEngineClient = computeEngineClient;
        this.objectMapper = objectMapper;
    }

    public MetadataClientPort metadata() {
        return metadataClient;
    }

    public MetamodelClientPort metamodel() {
        return metamodelClient;
    }

    public ComputeEngineClientPort compute() {
        return computeEngineClient;
    }

    @SuppressWarnings("unchecked")
    public List<BundleDto> bundles(Object raw) {
        return (List<BundleDto>) raw;
    }

    @SuppressWarnings("unchecked")
    public List<EntitySchemaDto> schemas(Object raw) {
        return (List<EntitySchemaDto>) raw;
    }

    @SuppressWarnings("unchecked")
    public List<MetadataEntityDto> entities(Object raw) {
        return (List<MetadataEntityDto>) raw;
    }

    public List<NativeAttributeDto> parseNativeAttributes(String json) {
        List<NativeAttributeDto> attributes = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return attributes;
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NativeAttributeDto.class));
        } catch (Exception e) {
            log.warn("解析 native_attributes 失败: json={}, error={}", json, e.getMessage());
            return attributes;
        }
    }

    public String resolveVersionedPrefix(String bundleCode) {
        if (bundleCode == null || bundleCode.isBlank()) {
            return null;
        }
        String version = metamodelClient.getLatestPublishedVersion(bundleCode);
        if (version != null && version.startsWith(bundleCode + ":")) {
            return version;
        }
        return bundleCode;
    }

    // ==================== 图查询 (compute-engine) ====================

    public GraphQueryResult adjacency(String sourceFqn, String direction, int maxDepth, List<String> relationTypes) {
        Object raw = computeEngineClient.queryAdjacency(sourceFqn, direction, maxDepth, relationTypes);
        return raw instanceof GraphQueryResult r ? r : emptyGraphResult();
    }

    public GraphQueryResult compositionTree(String rootFqn, String direction, int maxDepth) {
        Object raw = computeEngineClient.queryCompositionTree(rootFqn, direction, maxDepth);
        return raw instanceof GraphQueryResult r ? r : emptyGraphResult();
    }

    public ImpactTraceResult diffuseForward(String sourceFqn, List<String> relationTypes, int maxDepth) {
        Object raw = computeEngineClient.diffuseForward(sourceFqn, relationTypes, maxDepth);
        return raw instanceof ImpactTraceResult r ? r : null;
    }

    public ImpactTraceResult traceBackward(String sourceFqn, List<String> relationTypes, int maxDepth) {
        Object raw = computeEngineClient.traceBackward(sourceFqn, relationTypes, maxDepth);
        return raw instanceof ImpactTraceResult r ? r : null;
    }

    public ClosureResult closure(String sourceFqn, List<String> relationTypes) {
        Object raw = computeEngineClient.computeClosure(sourceFqn, relationTypes);
        return raw instanceof ClosureResult r ? r : null;
    }

    public List<RelationSummary> outboundRelations(String entityFqn) {
        GraphQueryResult result = adjacency(entityFqn, "FORWARD", 1, null);
        return result != null && result.relations() != null ? result.relations() : List.of();
    }

    public List<RelationSummary> inboundRelations(String entityFqn) {
        GraphQueryResult result = adjacency(entityFqn, "BACKWARD", 1, null);
        return result != null && result.relations() != null ? result.relations() : List.of();
    }

    public List<RelationSummary> neighborRelations(String entityFqn) {
        GraphQueryResult result = adjacency(entityFqn, "BOTH", 1, null);
        return result != null && result.relations() != null ? result.relations() : List.of();
    }

    public List<String> associationTypeNames(List<AssociationType> types) {
        if (types == null || types.isEmpty()) {
            return List.of();
        }
        return types.stream().map(Enum::name).toList();
    }

    private GraphQueryResult emptyGraphResult() {
        return new GraphQueryResult(Collections.emptyList(), Collections.emptyList(),
                Collections.emptyMap(), false, null, Collections.emptyList());
    }

    public List<EntitySummary> entitiesOf(GraphQueryResult result) {
        return result != null && result.entities() != null ? result.entities() : List.of();
    }

    public List<RelationSummary> relationsOf(GraphQueryResult result) {
        return result != null && result.relations() != null ? result.relations() : List.of();
    }
}
