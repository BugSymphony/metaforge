package com.metaforge.graph.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 创建草稿请求。
 */
public class CreateDraftRequest {

    @NotBlank(message = "源实体 FQN 不能为空")
    private String sourceEntityFqn;

    @NotBlank(message = "关系类型 FQN 不能为空")
    private String relationTypeFqn;

    @NotBlank(message = "目标实体 FQN 不能为空")
    private String targetEntityFqn;

    @NotBlank(message = "关系名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "属性内容不能为空")
    private Map<String, Object> content;

    private List<Float> embedding;

    public CreateDraftRequest() {}

    public String getSourceEntityFqn() { return sourceEntityFqn; }
    public void setSourceEntityFqn(String sourceEntityFqn) { this.sourceEntityFqn = sourceEntityFqn; }

    public String getRelationTypeFqn() { return relationTypeFqn; }
    public void setRelationTypeFqn(String relationTypeFqn) { this.relationTypeFqn = relationTypeFqn; }

    public String getTargetEntityFqn() { return targetEntityFqn; }
    public void setTargetEntityFqn(String targetEntityFqn) { this.targetEntityFqn = targetEntityFqn; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getContent() { return content; }
    public void setContent(Map<String, Object> content) { this.content = content; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
}
