package com.metaforge.agent.cognition.core.domain.service;

import java.util.List;

public interface FqnValidationService {

    void validateBundleFqns(List<String> bundleFqns);

    String resolveBundleFromEntityFqn(String entityFqn);
}
