package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.entity.ImpactTrace;

public interface ChangeWatchService {

    ImpactTrace handleMetadataChange(String changedEntityFqn);

    ImpactTrace handleRelationChange(String changedRelationFqn);
}
