package com.metaforge.agent.cognition.core.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(TemplateConfig.class);

    private List<TemplateDefinition> templates = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("cognition/cognition-templates.yml");
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> root = mapper.readValue(is, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> templateList = (List<Map<String, Object>>) root.get("templates");
                if (templateList != null) {
                    for (Map<String, Object> t : templateList) {
                        TemplateDefinition def = mapper.convertValue(t, TemplateDefinition.class);
                        templates.add(def);
                    }
                }
            }
            log.info("加载认知模板配置完成，共 {} 个模板", templates.size());
        } catch (Exception e) {
            log.error("加载 cognition/cognition-templates.yml 失败", e);
            throw new RuntimeException("加载 cognition/cognition-templates.yml 失败: " + e.getMessage(), e);
        }
    }

    public TemplateDefinition getTemplate(String templateId) {
        return templates.stream()
                .filter(t -> t.getTemplateId().equals(templateId))
                .findFirst()
                .orElse(null);
    }

    public List<TemplateDefinition> getAllTemplates() { return templates; }

    public static class TemplateDefinition {
        private String templateId;
        private String description;
        private List<String> perspectives;
        private String depthTrim;
        private String archetypeAdapt;
        private String outputFormat;
        private Integer maxTokens;
        private String contextMode;
        private String expand;

        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getPerspectives() { return perspectives; }
        public void setPerspectives(List<String> perspectives) { this.perspectives = perspectives; }
        public String getDepthTrim() { return depthTrim; }
        public void setDepthTrim(String depthTrim) { this.depthTrim = depthTrim; }
        public String getArchetypeAdapt() { return archetypeAdapt; }
        public void setArchetypeAdapt(String archetypeAdapt) { this.archetypeAdapt = archetypeAdapt; }
        public String getOutputFormat() { return outputFormat; }
        public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public String getContextMode() { return contextMode; }
        public void setContextMode(String contextMode) { this.contextMode = contextMode; }
        public String getExpand() { return expand; }
        public void setExpand(String expand) { this.expand = expand; }
    }
}
