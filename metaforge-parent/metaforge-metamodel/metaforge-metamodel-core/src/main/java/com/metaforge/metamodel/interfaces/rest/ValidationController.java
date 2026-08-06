package com.metaforge.metamodel.interfaces.rest;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.metamodel.domain.model.valueobject.ValidationResult;
import com.metaforge.metamodel.domain.service.ValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/metamodel")
@Tag(name = "metamodel-validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/versions/{fqn}/validate/save")
    @Operation(summary = "写入时轻量校验（FR-049）")
    public ApiResponse<ValidationResult> validateSave(@PathVariable String fqn) {
        ValidationResult result = validationService.validateSave(fqn);
        if (result.isPassed()) {
            return ApiResponse.success(result, "写入校验通过");
        }
        return ApiResponse.error(30108,
                "写入校验失败，共 " + result.getErrors().size() + " 个错误");
    }

    @PostMapping("/versions/{fqn}/validate/publish")
    @Operation(summary = "发布前全量校验（FR-050）")
    public ApiResponse<ValidationResult> validatePublish(@PathVariable String fqn) {
        ValidationResult result = validationService.validatePublish(fqn);
        if (result.isPassed()) {
            return ApiResponse.success(result, "发布校验通过");
        }
        return ApiResponse.error(30108,
                "发布校验失败，共 " + result.getErrors().size() + " 个错误");
    }

    @PostMapping("/versions/{fqn}/validate/preview")
    @Operation(summary = "预览模式：仅校验不落库")
    public ApiResponse<ValidationResult> preview(@PathVariable String fqn) {
        ValidationResult result = validationService.preview(fqn);
        if (result.isPassed()) {
            return ApiResponse.success(result, "预览校验通过");
        }
        return ApiResponse.error(30108,
                "预览校验失败，共 " + result.getErrors().size() + " 个错误");
    }
}
