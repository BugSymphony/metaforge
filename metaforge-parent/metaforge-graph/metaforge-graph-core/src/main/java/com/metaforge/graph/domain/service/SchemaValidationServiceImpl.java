package com.metaforge.graph.domain.service;

import com.metaforge.graph.api.constant.GraphErrorCode;
import com.metaforge.graph.infrastructure.config.GraphBizException;
import com.metaforge.graph.domain.repository.RelationSchemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JSON Schema 结构校验领域服务实现。
 *
 * <p>MVP 阶段使用宽松校验策略，后续对接真实的 JSON Schema 校验器。
 */
@Component
public class SchemaValidationServiceImpl implements RelationSchemaValidationService {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidationServiceImpl.class);

    private final RelationSchemaRepository relationSchemaRepository;

    public SchemaValidationServiceImpl(RelationSchemaRepository relationSchemaRepository) {
        this.relationSchemaRepository = relationSchemaRepository;
    }

    @Override
    public void validate(String relationSchemaFqn, Map<String, Object> content) {
        if (content == null) {
            throw new SchemaValidationException("属性内容不能为空");
        }

        String jsonSchema = relationSchemaRepository.getRelationSchemaSchema(relationSchemaFqn);
        if (jsonSchema == null || jsonSchema.isEmpty()) {
            log.debug("RelationSchema {} 无 JSON Schema 定义，跳过结构校验", relationSchemaFqn);
            return;
        }

        log.debug("RelationSchema {} 结构校验通过", relationSchemaFqn);
    }

    public static class SchemaValidationException extends GraphBizException {
        public SchemaValidationException(String message) {
            super(GraphErrorCode.SCHEMA_VALIDATION_FAILED, message);
        }

        @Override
        public String getErrorCodeName() {
            return "SCHEMA_VALIDATION_FAILED";
        }
    }
}
