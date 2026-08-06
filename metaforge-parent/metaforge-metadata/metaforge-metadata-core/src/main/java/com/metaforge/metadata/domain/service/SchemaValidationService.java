package com.metaforge.metadata.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.metaforge.metadata.api.dto.response.ValidationErrorDetailDto;
import com.metaforge.metadata.domain.exception.MetadataValidationException;
import com.metaforge.metadata.domain.repository.EntitySchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class SchemaValidationService {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidationService.class);

    private final ObjectMapper objectMapper;
    private final EntitySchemaRepository entitySchemaRepository;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidationService(ObjectMapper objectMapper,
                                   EntitySchemaRepository entitySchemaRepository) {
        this.objectMapper = objectMapper;
        this.entitySchemaRepository = entitySchemaRepository;
        this.schemaFactory = JsonSchemaFactory.byDefault();
    }

    public void validate(String entitySchemaFqn, Map<String, Object> content) {
        List<ValidationErrorDetailDto> errors = validateAndReturnErrors(entitySchemaFqn, content);
        if (!errors.isEmpty()) {
            throw new MetadataValidationException(
                    "JSON Schema 校验失败: " + errors.size() + " 项违规", errors);
        }
    }

    public List<ValidationErrorDetailDto> validateAndReturnErrors(String entitySchemaFqn,
                                                                   Map<String, Object> content) {
        if (entitySchemaFqn == null || entitySchemaFqn.isEmpty()) {
            return Collections.emptyList();
        }
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> jsonSchema = entitySchemaRepository.getJsonSchema(entitySchemaFqn)
                .orElse(null);
        if (jsonSchema == null || jsonSchema.isEmpty()) {
            log.debug("未找到 EntitySchema JSON Schema: {}, 跳过校验", entitySchemaFqn);
            return Collections.emptyList();
        }

        try {
            JsonNode schemaNode = objectMapper.valueToTree(jsonSchema);
            JsonSchema schema = schemaFactory.getJsonSchema(schemaNode);
            JsonNode contentNode = objectMapper.valueToTree(content);
            ProcessingReport report = schema.validate(contentNode);

            List<ValidationErrorDetailDto> errors = new ArrayList<>();
            for (ProcessingMessage msg : report) {
                errors.add(toErrorDetail(msg, entitySchemaFqn));
            }
            return errors;
        } catch (Exception e) {
            log.error("JSON Schema 校验执行异常: entitySchemaFqn={}", entitySchemaFqn, e);
            ValidationErrorDetailDto detail = new ValidationErrorDetailDto();
            detail.setJsonPath("$");
            detail.setViolationType("VALIDATION_ERROR");
            detail.setRuleReference(entitySchemaFqn);
            detail.setMessage("校验引擎异常: " + e.getMessage());
            return List.of(detail);
        }
    }

    private ValidationErrorDetailDto toErrorDetail(ProcessingMessage msg, String entitySchemaFqn) {
        JsonNode message = msg.asJson();
        ValidationErrorDetailDto detail = new ValidationErrorDetailDto();
        detail.setJsonPath(pointerOf(message, "instance"));
        detail.setViolationType(keywordOf(message));
        detail.setRuleReference(schemaRefOf(message, entitySchemaFqn));
        detail.setMessage(String.valueOf(message.get("message")));
        return detail;
    }

    private String pointerOf(JsonNode message, String section) {
        JsonNode pointer = message.path(section).path("pointer");
        return pointer.isMissingNode() || pointer.isNull() ? "$" : pointer.asText();
    }

    private String keywordOf(JsonNode message) {
        JsonNode keyword = message.get("keyword");
        return keyword == null || keyword.isNull() ? "VALIDATION_ERROR" : keyword.asText();
    }

    private String schemaRefOf(JsonNode message, String entitySchemaFqn) {
        JsonNode pointer = message.path("schema").path("pointer");
        if (pointer.isMissingNode() || pointer.isNull() || pointer.asText().isEmpty()) {
            return entitySchemaFqn;
        }
        JsonNode keyword = message.get("keyword");
        String keywordText = keyword == null || keyword.isNull() ? "" : keyword.asText();
        return pointer.asText() + (keywordText.isEmpty() ? "" : "/" + keywordText);
    }
}
