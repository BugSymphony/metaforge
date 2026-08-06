package com.metaforge.agent.cognition.api.dto.response;

public class NavigateResponse {

    private ContextMeta contextMeta;
    private DomainNavigation domainNavigation;

    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public DomainNavigation getDomainNavigation() { return domainNavigation; }
    public void setDomainNavigation(DomainNavigation domainNavigation) { this.domainNavigation = domainNavigation; }
}
