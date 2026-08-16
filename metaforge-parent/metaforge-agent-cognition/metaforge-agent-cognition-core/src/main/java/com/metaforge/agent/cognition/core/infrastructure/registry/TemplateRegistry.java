package com.metaforge.agent.cognition.core.infrastructure.registry;

import com.metaforge.agent.cognition.core.domain.model.entity.TemplateDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TemplateRegistry {

    private static final Logger log = LoggerFactory.getLogger(TemplateRegistry.class);

    private final ConcurrentHashMap<String, TemplateDefinition> cache = new ConcurrentHashMap<>();

    public TemplateDefinition resolve(String templateId) {
        TemplateDefinition def = cache.get(templateId);
        if (def == null) {
            log.debug("模板未注册: {}", templateId);
        }
        return def;
    }

    public void register(TemplateDefinition template) {
        cache.put(template.getTemplateId(), template);
        log.debug("模板已注册: {}", template.getTemplateId());
    }

    public void unregister(String templateId) {
        cache.remove(templateId);
        log.debug("模板已注销: {}", templateId);
    }

    public List<TemplateDefinition> listAll() {
        return new ArrayList<>(cache.values());
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}
