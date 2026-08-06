package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.valueobject.DataVersionAnchor;
import java.util.List;

public interface VersionAnchorService {

    List<DataVersionAnchor> resolveAnchors(List<String> bundleFqns);
}
