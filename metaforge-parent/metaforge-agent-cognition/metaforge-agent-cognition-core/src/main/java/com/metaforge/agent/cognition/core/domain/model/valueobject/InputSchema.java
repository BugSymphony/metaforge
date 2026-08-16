package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.*;

public class InputSchema {

    private Map<String, Object> properties = new LinkedHashMap<>();
    private List<String> required = new ArrayList<>();

    public InputSchema() {
    }

    public InputSchema(Map<String, Object> properties, List<String> required) {
        this.properties = properties != null ? properties : new LinkedHashMap<>();
        this.required = required != null ? required : new ArrayList<>();
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new LinkedHashMap<>();
    }

    public List<String> getRequired() {
        return required;
    }

    public void setRequired(List<String> required) {
        this.required = required != null ? required : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InputSchema that)) return false;
        return Objects.equals(properties, that.properties) && Objects.equals(required, that.required);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties, required);
    }

    @Override
    public String toString() {
        return "InputSchema{properties=" + properties + ", required=" + required + '}';
    }
}
