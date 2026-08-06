package com.metaforge.metadata.interfaces.mcp;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;
import com.metaforge.metadata.api.dto.request.AttributeCondition;
import com.metaforge.metadata.api.dto.request.DiffRequest;
import com.metaforge.metadata.api.dto.request.MetadataQueryRequest;
import com.metaforge.metadata.api.dto.response.EntityVersionDto;
import com.metaforge.metadata.api.dto.response.MetadataEntityDto;
import com.metaforge.metadata.api.dto.response.VersionDiffDto;
import com.metaforge.metadata.api.service.MetadataHistoryService;
import com.metaforge.metadata.api.service.MetadataQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetadataMcpTools {

    private final MetadataQueryService queryService;
    private final MetadataHistoryService historyService;

    public MetadataMcpTools(MetadataQueryService queryService,
                            MetadataHistoryService historyService) {
        this.queryService = queryService;
        this.historyService = historyService;
    }

    @Tool(description = "按 FQN 精准查询生效元数据实体的完整内容")
    public MetadataEntityDto getMetadataEntity(
            @ToolParam(description = "元数据实体的 FQN，如 SalesOrder_001") String fqn) {
        return queryService.getByFqn(fqn);
    }

    @Tool(description = "按 FQN 前缀集合批量查询生效元数据，多个前缀按 OR 逻辑拼接")
    public PageResult<MetadataEntityDto> queryMetadataByPrefix(
            @ToolParam(description = "FQN 前缀列表，空列表返回全部") List<String> fqnPrefixes,
            @ToolParam(description = "页码，默认 1") Integer page,
            @ToolParam(description = "每页条数，默认 20") Integer size) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setFqnPrefixes(fqnPrefixes);
        request.setPageRequest(new PageRequest(
                page != null ? page : 1,
                size != null ? size : 20));
        return queryService.listByFqnPrefixes(request);
    }

    @Tool(description = "按所属元模型类型查询生效元数据列表")
    public PageResult<MetadataEntityDto> queryMetadataBySchema(
            @ToolParam(description = "EntitySchema FQN，如 order:1.0.0.pkg_order.Order") String entitySchemaFqn,
            @ToolParam(description = "页码，默认 1") Integer page,
            @ToolParam(description = "每页条数，默认 20") Integer size) {
        MetadataQueryRequest request = new MetadataQueryRequest();
        request.setEntitySchemaFqn(entitySchemaFqn);
        request.setPageRequest(new PageRequest(
                page != null ? page : 1,
                size != null ? size : 20));
        return queryService.listByEntitySchema(request);
    }

    @Tool(description = "按属性条件组合查询生效元数据，多个条件之间为 AND 关系")
    public PageResult<MetadataEntityDto> queryMetadataByAttribute(
            @ToolParam(description = "属性字段名列表") List<String> fields,
            @ToolParam(description = "属性值列表，与 fields 一一对应") List<String> values,
            @ToolParam(description = "页码，默认 1") Integer page,
            @ToolParam(description = "每页条数，默认 20") Integer size) {
        List<AttributeCondition> conditions = new java.util.ArrayList<>();
        if (fields != null && values != null) {
            int count = Math.min(fields.size(), values.size());
            for (int i = 0; i < count; i++) {
                AttributeCondition condition = new AttributeCondition();
                condition.setField(fields.get(i));
                condition.setValue(values.get(i));
                conditions.add(condition);
            }
        }
        return queryService.queryByAttributes(conditions, new PageRequest(
                page != null ? page : 1,
                size != null ? size : 20));
    }

    @Tool(description = "查询指定 FQN 的全历史版本列表，按版本号倒序排列")
    public PageResult<EntityVersionDto> getEntityVersionHistory(
            @ToolParam(description = "元数据实体的 FQN") String fqn,
            @ToolParam(description = "页码，默认 1") Integer page,
            @ToolParam(description = "每页条数，默认 20") Integer size) {
        return historyService.listVersions(fqn, new PageRequest(
                page != null ? page : 1,
                size != null ? size : 20));
    }

    @Tool(description = "查询指定 FQN + 版本号的完整历史版本详情")
    public EntityVersionDto getEntityVersionDetail(
            @ToolParam(description = "元数据实体的 FQN") String fqn,
            @ToolParam(description = "版本号") Integer version) {
        return historyService.getVersionDetail(fqn, version);
    }

    @Tool(description = "对比任意两个历史版本的字段级差异，按 ADDED/MODIFIED/DELETED 三类分类展示")
    public VersionDiffDto compareVersions(
            @ToolParam(description = "元数据实体的 FQN") String fqn,
            @ToolParam(description = "版本 A") Integer versionA,
            @ToolParam(description = "版本 B") Integer versionB) {
        DiffRequest request = new DiffRequest();
        request.setFqn(fqn);
        request.setVersionA(versionA);
        request.setVersionB(versionB);
        return historyService.compareVersions(request);
    }
}
