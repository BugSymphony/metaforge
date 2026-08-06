package com.metaforge.agent.cognition.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.agent.cognition.api.service.CognitionOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CognitionOutputServiceImpl implements CognitionOutputService {

    private static final Logger log = LoggerFactory.getLogger(CognitionOutputServiceImpl.class);

    private final ObjectMapper objectMapper;

    public CognitionOutputServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String formatJson(Object result) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("JSON 格式化失败", e);
            return "{}";
        }
    }

    @Override
    public String formatPrompt(Object result) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
            return convertToPrompt(json, result);
        } catch (Exception e) {
            log.error("Prompt 格式化失败", e);
            return "";
        }
    }

    private String convertToPrompt(String json, Object result) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 认知查询结果\n\n");

        try {
            var node = objectMapper.readTree(json);
            if (node.has("templateId")) {
                sb.append("**模板ID**: ").append(node.get("templateId").asText()).append("\n\n");
            }
            if (node.has("contextMeta")) {
                sb.append("### 上下文元信息\n\n");
                sb.append("```json\n");
                sb.append(objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(node.get("contextMeta")));
                sb.append("\n```\n\n");
            }
            if (node.has("perspectives")) {
                sb.append("### 认知视角章节\n\n");
                var perspectives = node.get("perspectives");
                var fieldNames = perspectives.fieldNames();
                while (fieldNames.hasNext()) {
                    String key = fieldNames.next();
                    sb.append("#### ").append(key).append("\n\n");
                    sb.append("```json\n");
                    sb.append(objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(perspectives.get(key)));
                    sb.append("\n```\n\n");
                }
            }
        } catch (Exception e) {
            sb.append("```json\n").append(json).append("\n```\n");
        }

        return sb.toString();
    }
}
