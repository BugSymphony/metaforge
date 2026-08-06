package com.metaforge.agent.cognition.api.dto.response;

public class BundleCatalogResponse {

    private ContextMeta contextMeta;
    private BundleDirectory bundleDirectory;

    public ContextMeta getContextMeta() { return contextMeta; }
    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public BundleDirectory getBundleDirectory() { return bundleDirectory; }
    public void setBundleDirectory(BundleDirectory bundleDirectory) { this.bundleDirectory = bundleDirectory; }
}
