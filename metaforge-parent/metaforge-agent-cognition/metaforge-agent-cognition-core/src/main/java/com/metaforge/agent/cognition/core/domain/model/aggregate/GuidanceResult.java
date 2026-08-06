package com.metaforge.agent.cognition.core.domain.model.aggregate;

import com.metaforge.agent.cognition.core.domain.model.valueobject.ContextMeta;
import java.util.LinkedHashMap;
import java.util.Map;

public class GuidanceResult {

    private ContextMeta contextMeta;
    private final Map<String, Object> perspectiveChapters = new LinkedHashMap<>();

    private GuidanceResult(ContextMeta contextMeta) {
        this.contextMeta = contextMeta;
    }

    public static GuidanceResult create(ContextMeta contextMeta) {
        return new GuidanceResult(contextMeta);
    }

    public void setContextMeta(ContextMeta contextMeta) { this.contextMeta = contextMeta; }
    public ContextMeta getContextMeta() { return contextMeta; }

    public void putPerspectiveChapter(String perspectiveId, Object chapter) {
        perspectiveChapters.put(perspectiveId, chapter);
    }

    public Object getPerspectiveChapter(String perspectiveId) {
        return perspectiveChapters.get(perspectiveId);
    }

    public Map<String, Object> getPerspectiveChapters() { return perspectiveChapters; }

    public void setPerspectiveChapters(Map<String, Object> perspectiveChapters) {
        this.perspectiveChapters.clear();
        if (perspectiveChapters != null) {
            this.perspectiveChapters.putAll(perspectiveChapters);
        }
    }
}
