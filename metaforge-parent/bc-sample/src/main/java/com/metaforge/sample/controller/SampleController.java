package com.metaforge.sample.controller;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.sample.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 示例 REST Controller，演示统一响应、参数校验、全局异常处理与缓存读写。
 */
@RestController
@RequestMapping("/api/sample")
@Tag(name = "bc-sample", description = "示例业务 BC 接口")
public class SampleController {
    private final SampleService sampleService;

    public SampleController(SampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("/hello")
    @Operation(summary = "Hello 端点", description = "验证统一响应体格式与 TraceId 自动注入")
    public ApiResponse<Map<String, String>> hello() {
        String message = sampleService.hello();
        return ApiResponse.success(Map.of("message", message, "service", "bc-sample"));
    }

    @PostMapping("/validate")
    @Operation(summary = "参数校验端点", description = "验证 @Valid 参数校验机制")
    public ApiResponse<String> validate(@Valid @RequestBody ValidateRequest request) {
        return ApiResponse.success("校验通过: " + request.getName());
    }

    @GetMapping("/error-test")
    @Operation(summary = "异常测试端点", description = "验证全局异常处理拦截能力")
    public ApiResponse<String> errorTest() {
        throw new IllegalArgumentException("测试自定义异常处理");
    }

    @GetMapping("/cache-test/{key}")
    @Operation(summary = "缓存读取", description = "验证 Caffeine 缓存读取")
    public ApiResponse<Map<String, String>> getCache(@PathVariable String key) {
        String value = sampleService.getCacheValue(key);
        return ApiResponse.success(Map.of("key", key, "value", value != null ? value : "null"));
    }

    @PostMapping("/cache-test")
    @Operation(summary = "缓存写入", description = "验证 Caffeine 缓存写入")
    public ApiResponse<String> setCache(@RequestBody Map<String, String> body) {
        sampleService.setCacheValue(body.get("key"), body.get("value"));
        return ApiResponse.success("缓存写入成功");
    }

    public static class ValidateRequest {
        @NotNull(message = "name 字段不能为空")
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
