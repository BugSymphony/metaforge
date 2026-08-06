package com.metaforge.metamodel.api.dto.request;

import java.util.List;

public class UpdateExportManifestRequest {

    private List<String> packageFqns;

    public List<String> getPackageFqns() { return packageFqns; }
    public void setPackageFqns(List<String> packageFqns) { this.packageFqns = packageFqns; }
}
