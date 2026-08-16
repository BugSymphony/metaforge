package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import com.metaforge.agent.cognition.core.infrastructure.registry.OperatorRegistry;
import com.metaforge.agent.cognition.core.infrastructure.registry.TemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TemplateResolutionService {

    private static final Logger log = LoggerFactory.getLogger(TemplateResolutionService.class);

    private final TemplateRegistry templateRegistry;
    private final OperatorRegistry operatorRegistry;

    public TemplateResolutionService(TemplateRegistry templateRegistry,
                                      @Autowired(required = false) OperatorRegistry operatorRegistry) {
        this.templateRegistry = templateRegistry;
        this.operatorRegistry = operatorRegistry;
    }

    public TemplateDefinition resolve(String templateId) {
        TemplateDefinition def = templateRegistry.resolve(templateId);
        if (def == null) {
            log.warn("模板未在注册表中找到: {}", templateId);
            return null;
        }

        TemplateDefinition resolved = new TemplateDefinition();
        resolved.setTemplateId(def.getTemplateId());
        resolved.setTemplateName(def.getTemplateName());
        resolved.setDescription(def.getDescription());
        resolved.setInputSchema(def.getInputSchema());
        resolved.setOutputSchema(def.getOutputSchema());
        resolved.setConfig(def.getConfig() != null ? new java.util.LinkedHashMap<>(def.getConfig()) : null);

        if (def.getScopeBehavior() != null) {
            var sb = new com.metaforge.agent.cognition.core.domain.model.valueobject.ScopeBehavior();
            sb.setAcceptsScope(def.getScopeBehavior().isAcceptsScope());
            sb.setScopeRequired(def.getScopeBehavior().isScopeRequired());
            sb.setProducesUpdatedScope(def.getScopeBehavior().isProducesUpdatedScope());
            sb.setScopeFields(def.getScopeBehavior().getScopeFields());
            sb.validate();
            resolved.setScopeBehavior(sb);
        }

        List<OperatorDefinition> operators = def.getOperators().stream()
                .map(this::injectDefaults)
                .toList();
        resolved.setOperators(operators);

        validateOperatorReferences(resolved);

        return resolved;
    }

    private OperatorDefinition injectDefaults(OperatorDefinition op) {
        OperatorDefinition enriched = new OperatorDefinition();
        enriched.setOperatorId(op.getOperatorId());
        enriched.setName(op.getName());
        enriched.setDescription(op.getDescription());
        enriched.setPriority(op.getPriority());
        enriched.setRequired(op.isRequired());
        enriched.setTimeoutMs(op.getTimeoutMs() > 0 ? op.getTimeoutMs() : 10000);

        Set<AgentArchetype> archetypes = op.getArchetypes();
        if (archetypes == null || archetypes.isEmpty()) {
            enriched.setArchetypes(Set.of(AgentArchetype.values()));
        } else {
            enriched.setArchetypes(archetypes);
        }

        return enriched;
    }

    private void validateOperatorReferences(TemplateDefinition def) {
        if (operatorRegistry == null) {
            log.info("OperatorRegistry 未就绪，跳过算子存在性校验 (运行时懒解析)");
            return;
        }

        for (OperatorDefinition op : def.getOperators()) {
            if (operatorRegistry.resolve(op.getOperatorId()) == null) {
                log.warn("算子未在 OperatorRegistry 中注册，将跳过: {}", op.getOperatorId());
            }
        }
    }
}
