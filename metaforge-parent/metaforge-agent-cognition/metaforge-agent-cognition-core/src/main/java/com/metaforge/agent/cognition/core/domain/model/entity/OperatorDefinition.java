package com.metaforge.agent.cognition.core.domain.model.entity;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;

import java.util.*;

public class OperatorDefinition {

    private String operatorId;
    private String name;
    private String description;
    private int priority;
    private boolean required;
    private long timeoutMs;
    private Set<AgentArchetype> archetypes;

    public OperatorDefinition() {
        this.archetypes = new HashSet<>();
        this.timeoutMs = 10000;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < 0) {
            throw new IllegalArgumentException("priority 必须 >= 0");
        }
        this.priority = priority;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Set<AgentArchetype> getArchetypes() {
        return archetypes;
    }

    public void setArchetypes(Set<AgentArchetype> archetypes) {
        if (archetypes != null) {
            for (AgentArchetype a : archetypes) {
                if (a == null) {
                    throw new IllegalArgumentException("archetypes 包含 null 值");
                }
            }
        }
        this.archetypes = archetypes != null ? archetypes : new HashSet<>();
    }

    public boolean supportsArchetype(AgentArchetype archetype) {
        return archetypes.isEmpty() || archetypes.contains(archetype);
    }

    public void validate() {
        if (operatorId == null || operatorId.isBlank()) {
            throw new IllegalArgumentException("operatorId 不能为空");
        }
        if (!operatorId.matches("[A-Za-z][A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("operatorId 格式无效: '" + operatorId
                    + "'，须满足 [A-Za-z][A-Za-z0-9._-]+");
        }
        if (!operatorId.contains(".")) {
            throw new IllegalArgumentException("operatorId 缺少分类前缀分隔符: '" + operatorId + "'");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority 必须 >= 0，当前值: " + priority);
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs 必须 > 0，当前值: " + timeoutMs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperatorDefinition that)) return false;
        return priority == that.priority && required == that.required && timeoutMs == that.timeoutMs
                && Objects.equals(operatorId, that.operatorId) && Objects.equals(archetypes, that.archetypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operatorId, priority, required, timeoutMs, archetypes);
    }

    @Override
    public String toString() {
        return "OperatorDefinition{id='" + operatorId + "', priority=" + priority
                + ", required=" + required + ", timeout=" + timeoutMs + ", archetypes=" + archetypes + '}';
    }
}
