package com.metaforge.metamodel.domain.model.valueobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 校验结果值对象，承载校验通过/失败状态及错误信息列表。
 * 错误信息精确定位到 elementFqn + fieldName + message。
 */
public final class ValidationResult {

    private final boolean passed;
    private final List<ValidationError> errors;

    private ValidationResult(boolean passed, List<ValidationError> errors) {
        this.passed = passed;
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<>(errors))
                : Collections.emptyList();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean isPassed() {
        return passed;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    /**
     * 校验错误条目，精确定位到元素 FQN + 字段名。
     */
    public record ValidationError(
            String elementFqn,
            String fieldName,
            String message
    ) {
        public static ValidationError of(String elementFqn, String fieldName, String message) {
            return new ValidationError(elementFqn, fieldName, message);
        }
    }

    @Override
    public String toString() {
        if (passed) {
            return "ValidationResult{passed=true}";
        }
        return "ValidationResult{passed=false, errors=" + errors + "}";
    }
}
