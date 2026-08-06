package com.metaforge.agent.cognition.core.domain.model.entity;

import java.util.List;
import java.util.Map;

public class CapabilityCatalog {
    private List<CapabilityItem> capabilities;
    public static class CapabilityItem {
        private String capabilityFqn; private String name; private String description;
        private String interfaceSpec; private String callMethod; private List<ProtocolDetail> protocols;
        public String getCapabilityFqn() { return capabilityFqn; } public void setCapabilityFqn(String c) { this.capabilityFqn = c; }
        public String getName() { return name; } public void setName(String n) { this.name = n; }
        public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
        public String getInterfaceSpec() { return interfaceSpec; } public void setInterfaceSpec(String i) { this.interfaceSpec = i; }
        public String getCallMethod() { return callMethod; } public void setCallMethod(String c) { this.callMethod = c; }
        public List<ProtocolDetail> getProtocols() { return protocols; } public void setProtocols(List<ProtocolDetail> p) { this.protocols = p; }
        public static class ProtocolDetail {
            private String protocolFqn; private String protocolName; private String protocolDescription;
            private Map<String, Object> protocolContent;
            public String getProtocolFqn() { return protocolFqn; } public void setProtocolFqn(String p) { this.protocolFqn = p; }
            public String getProtocolName() { return protocolName; } public void setProtocolName(String p) { this.protocolName = p; }
            public String getProtocolDescription() { return protocolDescription; } public void setProtocolDescription(String p) { this.protocolDescription = p; }
            public Map<String, Object> getProtocolContent() { return protocolContent; } public void setProtocolContent(Map<String, Object> p) { this.protocolContent = p; }
        }
    }
    public List<CapabilityItem> getCapabilities() { return capabilities; } public void setCapabilities(List<CapabilityItem> c) { this.capabilities = c; }
}
