package com.metaforge.metamodel.api.dto.request;

import com.metaforge.metamodel.api.dto.NativeAttributeDto;

import java.util.List;

public class CreateRelationSchemaRequest {

    private String bundleVersionFqn;
    private String packageFqn;
    private String segment;
    private String name;
    private String description;
    private String sourceFqn;
    private String targetFqn;
    private String associationType;
    private String cardinalitySource;
    private String cardinalityTarget;
    private List<NativeAttributeDto> nativeAttributes;
    private List<String> mountedTemplateFqns;

    public String getBundleVersionFqn() { return bundleVersionFqn; }
    public void setBundleVersionFqn(String bundleVersionFqn) { this.bundleVersionFqn = bundleVersionFqn; }
    public String getPackageFqn() { return packageFqn; }
    public void setPackageFqn(String packageFqn) { this.packageFqn = packageFqn; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
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
    public List<NativeAttributeDto> getNativeAttributes() { return nativeAttributes; }
    public void setNativeAttributes(List<NativeAttributeDto> nativeAttributes) { this.nativeAttributes = nativeAttributes; }
    public List<String> getMountedTemplateFqns() { return mountedTemplateFqns; }
    public void setMountedTemplateFqns(List<String> mountedTemplateFqns) { this.mountedTemplateFqns = mountedTemplateFqns; }
}
