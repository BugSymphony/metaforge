package com.metaforge.agent.cognition.core.application.service;

import com.metaforge.agent.cognition.api.service.TemplateRegistryService;
import com.metaforge.agent.cognition.core.infrastructure.config.TemplateConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateRegistryServiceImpl implements TemplateRegistryService {

    private static final Logger log = LoggerFactory.getLogger(TemplateRegistryServiceImpl.class);

    private final TemplateConfig templateConfig;

    public TemplateRegistryServiceImpl(TemplateConfig templateConfig) {
        this.templateConfig = templateConfig;
    }

    @Override
    public com.metaforge.agent.cognition.api.dto.response.GuidanceResult getTemplate(String templateId) {
        TemplateConfig.TemplateDefinition def = templateConfig.getTemplate(templateId);
        if (def == null) {
            log.warn("模板未找到: {}", templateId);
            return null;
        }
        com.metaforge.agent.cognition.api.dto.response.GuidanceResult result =
                new com.metaforge.agent.cognition.api.dto.response.GuidanceResult();
        result.setTemplateId(templateId);
        return result;
    }

    @Override
    public List<com.metaforge.agent.cognition.api.dto.response.GuidanceResult> listTemplates() {
        return templateConfig.getAllTemplates().stream().map(t -> {
            com.metaforge.agent.cognition.api.dto.response.GuidanceResult r =
                    new com.metaforge.agent.cognition.api.dto.response.GuidanceResult();
            r.setTemplateId(t.getTemplateId());
            return r;
        }).toList();
    }

    @Override
    public boolean validateTemplate(String templateId) {
        return templateConfig.getTemplate(templateId) != null;
    }
}
