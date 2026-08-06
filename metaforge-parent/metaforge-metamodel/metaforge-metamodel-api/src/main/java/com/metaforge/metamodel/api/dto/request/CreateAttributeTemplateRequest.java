package com.metaforge.metamodel.api.dto.request;

import com.metaforge.metamodel.api.dto.AttributeDefinitionDto;

import java.util.List;

public class CreateAttributeTemplateRequest {

    private String bundleVersionFqn;
    private String segment;
    private String name;
    private String description;
    private List<AttributeDefinitionDto> attributeDefinitions;

    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<AttributeDefinitionDto> getAttributeDefinitions() { return attributeDefinitions; }
    public void setAttributeDefinitions(List<AttributeDefinitionDto> attributeDefinitions) { this.attributeDefinitions = attributeDefinitions; }
}
