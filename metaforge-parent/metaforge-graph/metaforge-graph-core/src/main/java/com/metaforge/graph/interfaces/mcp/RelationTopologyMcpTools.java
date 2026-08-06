package com.metaforge.graph.interfaces.mcp;

import com.metaforge.graph.api.dto.RelationVersionDto;
import com.metaforge.graph.api.dto.TopologyValidationReport;
import com.metaforge.graph.api.dto.TopologyValidationRequest;
import com.metaforge.graph.api.service.RelationHistoryService;
import com.metaforge.graph.api.service.RelationTopologyService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拓扑查询 MCP 工具提供者。
 *
 * <p>封装 get_relation_topology / list_relation_versions / compare_relation_versions
 * 三个工具，供 agent-consumption BC 注册到 MCP Server。
 */
@Component
public class RelationTopologyMcpTools {

    private final RelationTopologyService topologyService;
    private final RelationHistoryService historyService;

    public RelationTopologyMcpTools(RelationTopologyService topologyService,
                                     RelationHistoryService historyService) {
        this.topologyService = topologyService;
        this.historyService = historyService;
    }

    @Tool(description = "查询指定实体是否存在关联的生效依赖关系，返回 DEPENDENCY_INFLUENCE 类型关系列表")
    public List<String> getRelationTopology(
            @ToolParam(description = "实体 FQN") String entityFqn) {
        return topologyService.getDependentRelations(entityFqn);
    }

    @Tool(description = "查询指定关系 FQN 的全历史正式版本列表，按版本号倒序")
    public List<RelationVersionDto> listRelationVersions(
            @ToolParam(description = "关系 FQN") String fqn) {
        return historyService.listVersions(fqn);
    }

    @Tool(description = "对比指定关系的两个历史版本，返回字段级差异（新增/修改/删除）")
    public com.metaforge.graph.api.dto.VersionDiffDto compareRelationVersions(
            @ToolParam(description = "关系 FQN") String fqn,
            @ToolParam(description = "版本 A") int versionA,
            @ToolParam(description = "版本 B") int versionB) {
        com.metaforge.graph.api.dto.DiffRequest request = new com.metaforge.graph.api.dto.DiffRequest();
        request.setFqn(fqn);
        request.setVersionA(versionA);
        request.setVersionB(versionB);
        return historyService.compareVersions(request);
    }
}
