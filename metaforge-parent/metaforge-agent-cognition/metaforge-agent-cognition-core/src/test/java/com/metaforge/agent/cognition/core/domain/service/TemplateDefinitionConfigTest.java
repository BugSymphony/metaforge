package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TemplateDefinition config 双层结构测试")
class TemplateDefinitionConfigTest {

    @Test
    @DisplayName("双层 config：global 与 operators 分别解析")
    void shouldResolveDoubleLayerConfig() {
        TemplateDefinition def = new TemplateDefinition();
        def.setConfig(Map.of(
                "global", Map.of("versionAnchors", List.of("order:1.0.0")),
                "operators", Map.of(
                        "ontological.domain-drilldown", Map.of("levelAliases", Map.of("L1", "metaforge:1.0.0.common.SubjectDomainGroup")),
                        "capability.tool-discovery", Map.of("relationSchemaFqns", List.of("metaforge:1.0.0.agent.CapabilityAssignedTo"))
                )
        ));

        assertThat(def.getGlobalConfig()).containsEntry("versionAnchors", List.of("order:1.0.0"));
        assertThat(def.getOperatorConfig("ontological.domain-drilldown"))
                .containsKey("levelAliases");
        assertThat(def.getOperatorConfig("capability.tool-discovery"))
                .containsEntry("relationSchemaFqns", List.of("metaforge:1.0.0.agent.CapabilityAssignedTo"));
        assertThat(def.getOperatorConfig("ontological.entity-profile")).isEmpty();
    }

    @Test
    @DisplayName("单层 config（向后兼容）视为全局配置")
    void shouldTreatSingleLayerAsGlobal() {
        TemplateDefinition def = new TemplateDefinition();
        def.setConfig(Map.of("levelAliases", Map.of("L1", "metaforge:1.0.0.common.SubjectDomainGroup")));

        assertThat(def.getGlobalConfig()).containsKey("levelAliases");
        assertThat(def.getOperatorConfig("ontological.domain-drilldown")).isEmpty();
    }

    @Test
    @DisplayName("config 为 null 时返回空 Map")
    void shouldReturnEmptyWhenConfigNull() {
        TemplateDefinition def = new TemplateDefinition();
        assertThat(def.getGlobalConfig()).isEmpty();
        assertThat(def.getOperatorConfig("any.operator")).isEmpty();
    }
}
