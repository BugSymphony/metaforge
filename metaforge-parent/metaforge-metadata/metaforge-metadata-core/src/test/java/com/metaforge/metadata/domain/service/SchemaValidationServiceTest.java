package com.metaforge.metadata.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaforge.framework.test.BaseUnitTest;
import com.metaforge.metadata.api.dto.response.ValidationErrorDetailDto;
import com.metaforge.metadata.domain.exception.MetadataValidationException;
import com.metaforge.metadata.domain.repository.EntitySchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SchemaValidationService（java-json-tools 引擎）单元测试")
class SchemaValidationServiceTest extends BaseUnitTest {

    private static final String SCHEMA_FQN = "order:1.0.0.pkg_order.Order";

    private EntitySchemaRepository entitySchemaRepository;
    private SchemaValidationService service;

    @BeforeEach
    void setUp() {
        entitySchemaRepository = mock(EntitySchemaRepository.class);
        service = new SchemaValidationService(new ObjectMapper(), entitySchemaRepository);
    }

    private Map<String, Object> schema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> orderId = new HashMap<>();
        orderId.put("type", "string");
        orderId.put("pattern", "^SO-\\d{6}$");
        properties.put("orderId", orderId);
        Map<String, Object> amount = new HashMap<>();
        amount.put("type", "number");
        amount.put("minimum", 0);
        properties.put("amount", amount);
        schema.put("properties", properties);
        schema.put("required", List.of("orderId"));
        return schema;
    }

    @Test
    @DisplayName("合法内容通过校验，无错误")
    void validateValidContent() {
        when(entitySchemaRepository.getJsonSchema(SCHEMA_FQN)).thenReturn(Optional.of(schema()));

        Map<String, Object> content = new HashMap<>();
        content.put("orderId", "SO-123456");
        content.put("amount", 100.5);

        assertDoesNotThrow(() -> service.validate(SCHEMA_FQN, content));
    }

    @Test
    @DisplayName("正则违规返回结构化错误：字段路径 + 违规类型")
    void validatePatternViolation() {
        when(entitySchemaRepository.getJsonSchema(SCHEMA_FQN)).thenReturn(Optional.of(schema()));

        Map<String, Object> content = new HashMap<>();
        content.put("orderId", "INVALID");
        content.put("amount", 100.5);

        MetadataValidationException ex = assertThrows(MetadataValidationException.class,
                () -> service.validate(SCHEMA_FQN, content));
        List<ValidationErrorDetailDto> errors = castErrors(ex);
        assertFalse(errors.isEmpty());

        ValidationErrorDetailDto error = errors.stream()
                .filter(e -> "pattern".equals(e.getViolationType()))
                .findFirst()
                .orElse(null);
        assertNotNull(error, "应存在 pattern 违规记录");
        assertEquals("/orderId", error.getJsonPath());
        assertEquals("pattern", error.getViolationType());
        assertTrue(error.getRuleReference().contains("/properties/orderId"));
        assertNotNull(error.getMessage());
    }

    @Test
    @DisplayName("必填违规返回 keyword=required")
    void validateRequiredViolation() {
        when(entitySchemaRepository.getJsonSchema(SCHEMA_FQN)).thenReturn(Optional.of(schema()));

        Map<String, Object> content = new HashMap<>();
        content.put("amount", 100.5);

        MetadataValidationException ex = assertThrows(MetadataValidationException.class,
                () -> service.validate(SCHEMA_FQN, content));
        List<ValidationErrorDetailDto> errors = castErrors(ex);
        assertFalse(errors.isEmpty());

        ValidationErrorDetailDto error = errors.stream()
                .filter(e -> "required".equals(e.getViolationType()))
                .findFirst()
                .orElse(null);
        assertNotNull(error, "应存在 required 违规记录");
        assertEquals("required", error.getViolationType());
    }

    @Test
    @DisplayName("取值范围违规返回 keyword=minimum")
    void validateMinimumViolation() {
        when(entitySchemaRepository.getJsonSchema(SCHEMA_FQN)).thenReturn(Optional.of(schema()));

        Map<String, Object> content = new HashMap<>();
        content.put("orderId", "SO-123456");
        content.put("amount", -5);

        List<ValidationErrorDetailDto> errors = service.validateAndReturnErrors(SCHEMA_FQN, content);
        ValidationErrorDetailDto error = errors.stream()
                .filter(e -> "minimum".equals(e.getViolationType()))
                .findFirst()
                .orElse(null);
        assertNotNull(error, "应存在 minimum 违规记录");
        assertEquals("/amount", error.getJsonPath());
    }

    @Test
    @DisplayName("无 Schema 时跳过校验返回空列表")
    void validateWithoutSchema() {
        when(entitySchemaRepository.getJsonSchema(SCHEMA_FQN)).thenReturn(Optional.empty());

        Map<String, Object> content = new HashMap<>();
        content.put("orderId", "SO-123456");

        List<ValidationErrorDetailDto> errors = service.validateAndReturnErrors(SCHEMA_FQN, content);
        assertTrue(errors.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<ValidationErrorDetailDto> castErrors(MetadataValidationException ex) {
        return (List<ValidationErrorDetailDto>) ex.getData();
    }
}
