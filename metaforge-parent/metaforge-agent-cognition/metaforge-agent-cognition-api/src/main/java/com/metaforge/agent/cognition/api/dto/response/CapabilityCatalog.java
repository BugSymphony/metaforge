package com.metaforge.agent.cognition.api.dto.response;

import java.util.List;
import java.util.Map;

public class CapabilityCatalog {

    private List<CapabilityItem> capabilities;

    public List<CapabilityItem> getCapabilities() { return capabilities; }
    public void setCapabilities(List<CapabilityItem> capabilities) { this.capabilities = capabilities; }

    public static class CapabilityItem {
        private String capabilityFqn;
        private String name;
        private String description;
        private String interfaceSpec;
        private String callMethod;
        private List<ProtocolDetail> protocols;

        public String getCapabilityFqn() { return capabilityFqn; }
        public void setCapabilityFqn(String capabilityFqn) { this.capabilityFqn = capabilityFqn; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getInterfaceSpec() { return interfaceSpec; }
        public void setInterfaceSpec(String interfaceSpec) { this.interfaceSpec = interfaceSpec; }
        public String getCallMethod() { return callMethod; }
        public void setCallMethod(String callMethod) { this.callMethod = callMethod; }
        public List<ProtocolDetail> getProtocols() { return protocols; }
        public void setProtocols(List<ProtocolDetail> protocols) { this.protocols = protocols; }

        public static class ProtocolDetail {
            private String protocolFqn;
            private String protocolName;
            private String protocolDescription;
            private Map<String, Object> protocolContent;

            public String getProtocolFqn() { return protocolFqn; }
            public void setProtocolFqn(String protocolFqn) { this.protocolFqn = protocolFqn; }
            public String getProtocolName() { return protocolName; }
            public void setProtocolName(String protocolName) { this.protocolName = protocolName; }
            public String getProtocolDescription() { return protocolDescription; }
            public void setProtocolDescription(String protocolDescription) { this.protocolDescription = protocolDescription; }
            public Map<String, Object> getProtocolContent() { return protocolContent; }
            public void setProtocolContent(Map<String, Object> protocolContent) { this.protocolContent = protocolContent; }
        }
    }
}
