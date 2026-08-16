package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TemplateFileWatcher {

    private static final Logger log = LoggerFactory.getLogger(TemplateFileWatcher.class);

    private final TemplateRegistry templateRegistry;
    private final TemplateYamlParser yamlParser;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "template-file-watcher");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final long DEFAULT_POLL_INTERVAL_MS = 5000;

    public TemplateFileWatcher(TemplateRegistry templateRegistry, TemplateYamlParser yamlParser) {
        this.templateRegistry = templateRegistry;
        this.yamlParser = yamlParser;
    }

    public void startWatching(String location) {
        if (running.get()) {
            return;
        }
        String path = location.startsWith("file:") ? location.substring(5) : location;
        Path dir = Paths.get(path);

        if (!Files.isDirectory(dir)) {
            log.debug("外部模板目录不存在，跳过文件监听: {}", path);
            return;
        }

        running.set(true);
        executor.scheduleWithFixedDelay(() -> pollDirectory(dir), 0, DEFAULT_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        log.info("模板文件监听已启动: {} (间隔: {}ms)", path, DEFAULT_POLL_INTERVAL_MS);
    }

    public void stopWatching() {
        running.set(false);
        executor.shutdown();
        log.info("模板文件监听已停止");
    }

    private void pollDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warn("模板目录已移除，监听器将停止: {}", dir);
            stopWatching();
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.yml")) {
            for (Path file : stream) {
                processFileChange(file);
            }
        } catch (IOException e) {
            log.warn("模板目录轮询失败: {}", dir, e);
        }
    }

    private void processFileChange(Path file) {
        try {
            if (!Files.exists(file)) {
                return;
            }

            String content = Files.readString(file, StandardCharsets.UTF_8);
            TemplateDefinition def = yamlParser.parse(content);

            if (def == null || def.getTemplateId() == null) {
                log.warn("模板解析失败 (写入可能未完成): {}", file);
                return;
            }

            try {
                def.validate();
            } catch (IllegalArgumentException e) {
                log.warn("模板校验失败，跳过注册: {}", e.getMessage());
                return;
            }

            templateRegistry.register(def);
            log.info("模板热加载成功: {} (source: {})", def.getTemplateId(), file);
        } catch (IOException e) {
            log.warn("模板文件读取失败: {}", file, e);
        }
    }
}
