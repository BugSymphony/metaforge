package com.metaforge.metamodel.api.dto.request;

import com.metaforge.metamodel.api.dto.NativeAttributeDto;

import java.util.List;

public class UpdateEntitySchemaRequest {

    private String name;
    private String description;
    private List<NativeAttributeDto> nativeAttributes;
    private List<String> mountedTemplateFqns;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<NativeAttributeDto> getNativeAttributes() { return nativeAttributes; }
    public void setNativeAttributes(List<NativeAttributeDto> nativeAttributes) { this.nativeAttributes = nativeAttributes; }
    public List<String> getMountedTemplateFqns() { return mountedTemplateFqns; }
    public void setMountedTemplateFqns(List<String> mountedTemplateFqns) { this.mountedTemplateFqns = mountedTemplateFqns; }
}
