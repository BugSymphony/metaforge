package com.metaforge.graph.api.dto;

import com.metaforge.common.dto.PageRequest;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 多维过滤查询请求——维度间 AND，维度内 OR。
 */
public class RelationQueryRequest {

    private List<String> relationTypes;
    private List<String> sourceEntityTypes;
    private List<String> targetEntityTypes;
    private List<String> sourceEntityFqns;
    private List<String> targetEntityFqns;
    private List<String> relationSchemaFqns;
    private String relationSchemaFqnPrefix;
    private String nameKeyword;
    private String descriptionKeyword;
    private LocalDateTime createdAtStart;
    private LocalDateTime createdAtEnd;
    private LocalDateTime updatedAtStart;
    private LocalDateTime updatedAtEnd;
    private PageRequest pageRequest;

    public RelationQueryRequest() {}

    public List<String> getRelationTypes() { return relationTypes; }
    public void setRelationTypes(List<String> relationTypes) { this.relationTypes = relationTypes; }

    public List<String> getSourceEntityTypes() { return sourceEntityTypes; }
    public void setSourceEntityTypes(List<String> sourceEntityTypes) { this.sourceEntityTypes = sourceEntityTypes; }

    public List<String> getTargetEntityTypes() { return targetEntityTypes; }
    public void setTargetEntityTypes(List<String> targetEntityTypes) { this.targetEntityTypes = targetEntityTypes; }

    public List<String> getSourceEntityFqns() { return sourceEntityFqns; }
    public void setSourceEntityFqns(List<String> sourceEntityFqns) { this.sourceEntityFqns = sourceEntityFqns; }

    public List<String> getTargetEntityFqns() { return targetEntityFqns; }
    public void setTargetEntityFqns(List<String> targetEntityFqns) { this.targetEntityFqns = targetEntityFqns; }

    public List<String> getRelationSchemaFqns() { return relationSchemaFqns; }
    public void setRelationSchemaFqns(List<String> relationSchemaFqns) { this.relationSchemaFqns = relationSchemaFqns; }

    public String getRelationSchemaFqnPrefix() { return relationSchemaFqnPrefix; }
    public void setRelationSchemaFqnPrefix(String relationSchemaFqnPrefix) { this.relationSchemaFqnPrefix = relationSchemaFqnPrefix; }

    public String getNameKeyword() { return nameKeyword; }
    public void setNameKeyword(String nameKeyword) { this.nameKeyword = nameKeyword; }

    public String getDescriptionKeyword() { return descriptionKeyword; }
    public void setDescriptionKeyword(String descriptionKeyword) { this.descriptionKeyword = descriptionKeyword; }

    public LocalDateTime getCreatedAtStart() { return createdAtStart; }
    public void setCreatedAtStart(LocalDateTime createdAtStart) { this.createdAtStart = createdAtStart; }

    public LocalDateTime getCreatedAtEnd() { return createdAtEnd; }
    public void setCreatedAtEnd(LocalDateTime createdAtEnd) { this.createdAtEnd = createdAtEnd; }

    public LocalDateTime getUpdatedAtStart() { return updatedAtStart; }
    public void setUpdatedAtStart(LocalDateTime updatedAtStart) { this.updatedAtStart = updatedAtStart; }

    public LocalDateTime getUpdatedAtEnd() { return updatedAtEnd; }
    public void setUpdatedAtEnd(LocalDateTime updatedAtEnd) { this.updatedAtEnd = updatedAtEnd; }

    public PageRequest getPageRequest() { return pageRequest; }
    public void setPageRequest(PageRequest pageRequest) { this.pageRequest = pageRequest; }
}
