package com.metaforge.agent.cognition.api.service;

import com.metaforge.agent.cognition.api.dto.response.GuidanceResult;
import java.util.List;

public interface TemplateRegistryService {

    GuidanceResult getTemplate(String templateId);

    List<GuidanceResult> listTemplates();

    boolean validateTemplate(String templateId);
}
