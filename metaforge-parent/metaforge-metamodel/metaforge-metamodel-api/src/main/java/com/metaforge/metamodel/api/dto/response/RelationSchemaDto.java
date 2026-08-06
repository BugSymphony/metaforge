package com.metaforge.metamodel.api.dto.response;

import java.time.LocalDateTime;

public class RelationSchemaDto {

    private String fqn;
    private String packageFqn;
    private String bundleVersionFqn;
    private String name;
    private String description;
    private String sourceFqn;
    private String targetFqn;
    private String associationType;
    private String cardinalitySource;
    private String cardinalityTarget;
    private String nativeAttributes;
    private String mountedTemplateFqns;
    private String jsonSchema;
    private boolean enabled;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFqn() { return fqn; }
    public void setFqn(String fqn) { this.fqn = fqn; }
    public String getPackageFqn() { return packageFqn; }
    public void setPackageFqn(String packageFqn) { this.packageFqn = packageFqn; }
    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceFqn() { return sourceFqn; }
    public void setSourceFqn(String sourceFqn) { this.sourceFqn = sourceFqn; }
    public String getTargetFqn() { return targetFqn; }
    public void setTargetFqn(String targetFqn) { this.targetFqn = targetFqn; }
    public String getAssociationType() { return associationType; }
    public void setAssociationType(String associationType) { this.associationType = associationType; }
    public String getCardinalitySource() { return cardinalitySource; }
    public void setCardinalitySource(String cardinalitySource) { this.cardinalitySource = cardinalitySource; }
    public String getCardinalityTarget() { return cardinalityTarget; }
    public void setCardinalityTarget(String cardinalityTarget) { this.cardinalityTarget = cardinalityTarget; }
    public String getNativeAttributes() { return nativeAttributes; }
    public void setNativeAttributes(String nativeAttributes) { this.nativeAttributes = nativeAttributes; }
    public String getMountedTemplateFqns() { return mountedTemplateFqns; }
    public void setMountedTemplateFqns(String mountedTemplateFqns) { this.mountedTemplateFqns = mountedTemplateFqns; }
    public String getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(String jsonSchema) { this.jsonSchema = jsonSchema; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}
