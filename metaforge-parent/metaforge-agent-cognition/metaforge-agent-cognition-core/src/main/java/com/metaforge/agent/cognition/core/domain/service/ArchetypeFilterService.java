package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.AgentArchetype;
import com.metaforge.agent.cognition.core.domain.model.entity.OperatorDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArchetypeFilterService {

    private static final Logger log = LoggerFactory.getLogger(ArchetypeFilterService.class);

    public List<OperatorDefinition> filter(List<OperatorDefinition> operators, AgentArchetype agentArchetype) {
        if (operators == null || operators.isEmpty()) {
            return List.of();
        }

        if (agentArchetype == null) {
            log.debug("agentArchetype 为 null，返回全部算子");
            return new ArrayList<>(operators);
        }

        List<OperatorDefinition> filtered = operators.stream()
                .filter(op -> supportsArchetype(op, agentArchetype))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            log.warn("无算子支持 agentArchetype {}: 模板中 {} 个算子无一匹配白名单",
                    agentArchetype, operators.size());
            return filtered;
        }

        log.debug("Archetype 过滤: {} → {} 个算子 (archetype={})",
                operators.size(), filtered.size(), agentArchetype);
        return filtered;
    }

    private boolean supportsArchetype(OperatorDefinition op, AgentArchetype archetype) {
        Set<AgentArchetype> whitelist = op.getArchetypes();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        return whitelist.contains(archetype);
    }
}
