package com.metaforge.agent.cognition.api.service;

import com.metaforge.agent.cognition.api.dto.request.CognitionRequest;
import com.metaforge.agent.cognition.api.dto.response.GuidanceResult;

public interface CognitionQueryService {

    GuidanceResult execute(String templateId, CognitionRequest request);
}
