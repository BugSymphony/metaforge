package com.metaforge.graph.interfaces.mcp;

import com.metaforge.common.dto.PageResult;
import com.metaforge.graph.api.dto.RelationInstanceDto;
import com.metaforge.graph.api.dto.RelationQueryRequest;
import com.metaforge.graph.api.service.RelationQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关系查询 MCP 工具提供者。
 *
 * <p>封装 get_relation_by_fqn / list_outbound_relations / list_inbound_relations / multi_filter_relations
 * 四个工具，供 agent-consumption BC 注册到 MCP Server。
 */
@Component
public class RelationQueryMcpTools {

    private final RelationQueryService queryService;

    public RelationQueryMcpTools(RelationQueryService queryService) {
        this.queryService = queryService;
    }

    @Tool(description = "通过 FQN 精准查询单条生效关系实例的完整属性")
    public RelationInstanceDto getRelationByFqn(
            @ToolParam(description = "关系 FQN") String fqn) {
        return queryService.getByFqn(fqn);
    }

    @Tool(description = "查询指定实体的出边关系列表，可选按关系类型过滤")
    public List<RelationInstanceDto> listOutboundRelations(
            @ToolParam(description = "源实体 FQN") String entityFqn,
            @ToolParam(description = "关系类型过滤（可选）") String relationType,
            @ToolParam(description = "目标实体类型过滤（可选）") String targetEntityType) {
        return queryService.getOutboundRelations(entityFqn, relationType, targetEntityType);
    }

    @Tool(description = "查询指定实体的入边关系列表，可选按关系类型过滤")
    public List<RelationInstanceDto> listInboundRelations(
            @ToolParam(description = "目标实体 FQN") String entityFqn,
            @ToolParam(description = "关系类型过滤（可选）") String relationType,
            @ToolParam(description = "源实体类型过滤（可选）") String sourceEntityType) {
        return queryService.getInboundRelations(entityFqn, relationType, sourceEntityType);
    }

    @Tool(description = "多维过滤查询生效关系，维度间 AND 维度内 OR，支持分页")
    public PageResult<RelationInstanceDto> multiFilterRelations(
            @ToolParam(description = "多维过滤查询请求") RelationQueryRequest request) {
        return queryService.multiFilter(request);
    }
}
