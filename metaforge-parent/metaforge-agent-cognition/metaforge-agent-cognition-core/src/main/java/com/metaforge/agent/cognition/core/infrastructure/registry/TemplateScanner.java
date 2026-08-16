package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Component
public class TemplateScanner {

    private static final Logger log = LoggerFactory.getLogger(TemplateScanner.class);

    private final TemplateRegistry templateRegistry;
    private final TemplateYamlParser yamlParser;
    private final TemplateFileWatcher fileWatcher;

    @Value("${metaforge.agent-cognition.templates.classpath-location:classpath:cognition/templates/}")
    private String classpathLocation;

    @Value("${metaforge.agent-cognition.templates.external-location:}")
    private String externalLocation;

    public TemplateScanner(TemplateRegistry templateRegistry, TemplateYamlParser yamlParser) {
        this.templateRegistry = templateRegistry;
        this.yamlParser = yamlParser;
        this.fileWatcher = new TemplateFileWatcher(templateRegistry, yamlParser);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("模板扫描器启动: classpath={}, external={}", classpathLocation, externalLocation);
        scanClasspathTemplates();
        scanExternalTemplates();
        if (externalLocation != null && !externalLocation.isBlank()) {
            fileWatcher.startWatching(externalLocation);
        }
        log.info("模板注册表已就绪: {} 个模板已注册", templateRegistry.size());
    }

    void scanClasspathTemplates() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = classpathLocation;
            if (!pattern.endsWith("/")) pattern += "/";
            pattern += "*.yml";

            Resource[] resources = resolver.getResources(pattern);
            log.info("classpath 模板扫描: 找到 {} 个 YAML 文件", resources.length);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    registerFromContent(content, "classpath:" + resource.getFilename());
                } catch (IOException e) {
                    log.warn("classpath 模板读取失败: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.warn("classpath 模板扫描异常: {}", e.getMessage());
        }
    }

    void scanExternalTemplates() {
        if (externalLocation == null || externalLocation.isBlank()) {
            return;
        }

        String path = extractFilePath(externalLocation);
        Path dir = Paths.get(path);
        if (!Files.isDirectory(dir)) {
            log.debug("外部模板目录不存在: {}", path);
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.yml")) {
            for (Path file : stream) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    registerFromContent(content, file.toString());
                } catch (IOException e) {
                    log.warn("外部模板读取失败: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.warn("外部模板目录遍历失败: {}", path, e);
        }
    }

    void registerFromContent(String content, String source) {
        TemplateDefinition def = yamlParser.parse(content);
        if (def == null || def.getTemplateId() == null) {
            log.warn("模板解析失败或 templateId 为空: {}", source);
            return;
        }

        try {
            def.validate();
        } catch (IllegalArgumentException e) {
            log.warn("模板校验失败 ({}): {}", source, e.getMessage());
            return;
        }

        TemplateDefinition existing = templateRegistry.resolve(def.getTemplateId());
        if (existing != null && source.startsWith("classpath:") && !hasExternalOverride(def.getTemplateId())) {
            log.debug("classpath 模板被外部模板覆盖: {}", def.getTemplateId());
            return;
        }

        templateRegistry.register(def);
        log.info("模板注册成功: {} (source: {})", def.getTemplateId(), source);
    }

    private boolean hasExternalOverride(String templateId) {
        return templateRegistry.resolve(templateId) != null;
    }

    private String extractFilePath(String location) {
        if (location.startsWith("file:")) {
            return location.substring(5);
        }
        return location;
    }

    public void triggerFullRescan() {
        log.info("触发全量重扫描");
        scanClasspathTemplates();
        scanExternalTemplates();
    }
}
