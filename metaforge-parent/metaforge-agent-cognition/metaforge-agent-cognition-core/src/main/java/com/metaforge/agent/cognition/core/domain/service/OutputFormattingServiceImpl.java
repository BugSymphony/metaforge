package com.metaforge.agent.cognition.core.domain.service;

import com.metaforge.agent.cognition.api.enums.OutputFormat;
import com.metaforge.agent.cognition.core.domain.model.aggregate.GuidanceResult;
import com.metaforge.agent.cognition.core.domain.model.valueobject.ContextMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OutputFormattingServiceImpl implements OutputFormattingService {

    private static final Logger log = LoggerFactory.getLogger(OutputFormattingServiceImpl.class);

    @Override
    public String format(GuidanceResult result, OutputFormat format) {
        if (format == OutputFormat.PROMPT) {
            return formatAsPrompt(result);
        }
        return formatAsJson(result);
    }

    private String formatAsJson(GuidanceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("  \"contextMeta\": {\n");
        ContextMeta meta = result.getContextMeta();
        if (meta != null) {
            sb.append("    \"bundleFqns\": ").append(jsonList(meta.getBundleFqns())).append(",\n");
            sb.append("    \"entityFqn\": ").append(quote(meta.getEntityFqn())).append(",\n");
            sb.append("    \"contextMode\": ").append(quote(meta.getContextMode() != null
                    ? meta.getContextMode().name() : null)).append(",\n");
            sb.append("    \"queriedAt\": ").append(quote(meta.getQueriedAt() != null
                    ? meta.getQueriedAt().toString() : null)).append("\n");
        }
        sb.append("  },\n");

        sb.append("  \"perspectives\": {\n");
        Map<String, Object> chapters = result.getPerspectiveChapters();
        if (chapters != null) {
            int i = 0;
            for (Map.Entry<String, Object> entry : chapters.entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ");
                sb.append(jsonValue(entry.getValue()));
                if (++i < chapters.size()) sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String formatAsPrompt(GuidanceResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 认知查询结果\n\n");

        ContextMeta meta = result.getContextMeta();
        if (meta != null) {
            sb.append("### 上下文元信息\n\n");
            sb.append("- **Bundle FQN**: ").append(meta.getBundleFqns()).append("\n");
            sb.append("- **实体 FQN**: ").append(meta.getEntityFqn() != null ? meta.getEntityFqn() : "N/A").append("\n");
            sb.append("- **上下文模式**: ").append(meta.getContextMode()).append("\n");
            sb.append("- **查询时间**: ").append(meta.getQueriedAt()).append("\n");

            if (meta.getDataVersionAnchors() != null && !meta.getDataVersionAnchors().isEmpty()) {
                sb.append("- **数据版本锚**:\n");
                meta.getDataVersionAnchors().forEach(a ->
                        sb.append("  - ").append(a.bundleFqn()).append(" v")
                                .append(a.latestVersionNumber()).append("\n"));
            }

            if (meta.isTruncated() || meta.isTokenTrimmed()) {
                sb.append("\n> 注意: ");
                if (meta.isTruncated()) sb.append("部分视角结果被截断。");
                if (meta.isTokenTrimmed()) sb.append("Token 预算触发裁剪。");
                sb.append("\n");
            }

            if (meta.getTruncations() != null && !meta.getTruncations().isEmpty()) {
                sb.append("\n> 截断标注:\n");
                meta.getTruncations().forEach(t ->
                        sb.append("> - ").append(t.perspective().value())
                                .append(": ").append(t.reason()).append("\n"));
                sb.append("\n");
            }
            sb.append("\n");
        }

        sb.append("### 认知视角章节\n\n");
        Map<String, Object> chapters = result.getPerspectiveChapters();
        if (chapters != null) {
            for (Map.Entry<String, Object> entry : chapters.entrySet()) {
                sb.append("#### ").append(entry.getKey()).append("\n\n");
                sb.append("```json\n");
                sb.append(entry.getValue() != null ? entry.getValue().toString() : "{}");
                sb.append("\n```\n\n");
            }
        }

        return sb.toString();
    }

    private String jsonList(java.util.List<?> list) {
        if (list == null) return "[]";
        return list.stream().map(Object::toString).toList().toString();
    }

    private String quote(String s) {
        return s != null ? "\"" + s + "\"" : "null";
    }

    private String jsonValue(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + obj + "\"";
        return obj.toString();
    }
}
