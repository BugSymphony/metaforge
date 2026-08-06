package com.metaforge.agent.cognition.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.metaforge.agent.cognition.api.enums.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PerspectiveConfig {

    private static final Logger log = LoggerFactory.getLogger(PerspectiveConfig.class);

    private List<PerspectiveDefinition> perspectives = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("cognition/cognition-perspectives.yml");
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> root = mapper.readValue(is, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> perspectiveList = (List<Map<String, Object>>) root.get("perspectives");
                if (perspectiveList != null) {
                    for (Map<String, Object> p : perspectiveList) {
                        String id = (String) p.get("perspectiveId");
                        String scopeStr = (String) p.get("scope");
                        String description = (String) p.get("description");
                        perspectives.add(new PerspectiveDefinition(id,
                                PerspectiveScope.valueOf(scopeStr.toUpperCase()), description));
                    }
                }
            }
            log.info("加载认知视角配置完成，共 {} 个视角", perspectives.size());
        } catch (Exception e) {
            log.error("加载 cognition/cognition-perspectives.yml 失败", e);
            throw new RuntimeException("加载 cognition/cognition-perspectives.yml 失败: " + e.getMessage(), e);
        }
    }

    public List<PerspectiveDefinition> resolveActivePerspectives(
            ContextMode contextMode,
            List<PerspectiveCode> requestedPerspectiveIds,
            CognitionDepth depth,
            AgentArchetype archetype) {
        List<String> requestedIds = requestedPerspectiveIds != null
                ? requestedPerspectiveIds.stream().map(PerspectiveCode::getValue).collect(Collectors.toList())
                : null;

        return perspectives.stream()
                .filter(p -> isScopeCompatible(p.getScope(), contextMode))
                .filter(p -> requestedIds == null || requestedIds.isEmpty() || requestedIds.contains(p.getPerspectiveId()))
                .limit(depth != null ? depth.maxPerspectives() : 7)
                .collect(Collectors.toList());
    }

    private boolean isScopeCompatible(PerspectiveScope scope, ContextMode contextMode) {
        if (contextMode == ContextMode.BUNDLE_LEVEL) {
            return scope == PerspectiveScope.BUNDLE || scope == PerspectiveScope.BOTH;
        } else {
            return scope == PerspectiveScope.ENTITY || scope == PerspectiveScope.BOTH;
        }
    }

    public List<PerspectiveDefinition> getPerspectives() { return perspectives; }

    public static class PerspectiveDefinition {
        private final String perspectiveId;
        private final PerspectiveScope scope;
        private final String description;

        public PerspectiveDefinition(String perspectiveId, PerspectiveScope scope, String description) {
            this.perspectiveId = perspectiveId;
            this.scope = scope;
            this.description = description;
        }

        public String getPerspectiveId() { return perspectiveId; }
        public PerspectiveScope getScope() { return scope; }
        public String getDescription() { return description; }
    }
}
