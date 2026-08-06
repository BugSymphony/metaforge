package com.metaforge.metamodel.api.dto;

import java.util.Map;

public class AttributeDefinitionDto {

    private String name;
    private String type;
    private Map<String, Object> constraints;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getConstraints() { return constraints; }
    public void setConstraints(Map<String, Object> constraints) { this.constraints = constraints; }
}
