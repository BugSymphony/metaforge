package com.metaforge.server.web;

import java.util.stream.Collectors;

import com.metaforge.server.spi.SpiRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.metaforge.common.dto.ApiResponse;
import com.metaforge.common.exception.BaseException;

/**
 * 全局异常处理器
 * <p>统一处理控制器层抛出的各类异常，将其转换为标准的 {@link ApiResponse} 格式返回。
 * 通过 SPI 机制优先使用业务 BC 注册的自定义异常处理器，基座默认处理器作为 fallback。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired(required = false)
    private SpiRegistry spiRegistry;

    /**
     * 处理参数校验异常
     * <p>当 @Valid 校验失败时抛出 MethodArgumentNotValidException，提取字段错误信息返回。</p>
     *
     * @param ex 参数校验异常
     * @return ApiResponse 错误码 20001
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error(20001, "参数校验失败: " + message);
    }

    /**
     * 处理 JSON 请求体解析异常
     * <p>当请求体格式错误无法反序列化时抛出 HttpMessageNotReadableException。</p>
     *
     * @param ex JSON 解析异常
     * @return ApiResponse 错误码 20003
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<?> handleNotReadable(HttpMessageNotReadableException ex) {
        return ApiResponse.error(20003, "请求体格式错误");
    }

    /**
     * 处理 BaseException 及其子类异常
     * <p>根据错误码映射对应的 HTTP 状态码：</p>
     * <ul>
     *   <li>10000-19999 → 500 系统错误</li>
     *   <li>20000-29999 → 400 请求错误</li>
     *   <li>其他 → 200</li>
     * </ul>
     *
     * @param ex 基础异常
     * @return 包含 HTTP 状态码和 ApiResponse 的 ResponseEntity
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<?>> handleBaseException(BaseException ex) {
        int httpStatus = mapCodeToHttpStatus(ex.getCode());
        ApiResponse<?> body = ex.getData() != null
            ? ApiResponse.of(ex.getCode(), ex.getMessage(), ex.getData())
            : ApiResponse.error(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(httpStatus).body(body);
    }

    /**
     * 兜底处理所有未捕获的异常
     *
     * @param ex 异常
     * @return ApiResponse 错误码 10000
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception ex) {
        // 优先使用 SPI 扩展的自定义异常处理器
        if (spiRegistry != null) {
            ApiResponse<?> spiResponse = spiRegistry.handleException(ex);
            if (spiResponse != null) {
                return spiResponse;
            }
        }
        // 兜底处理：基座默认异常处理
        log.error("未处理的异常", ex);
        return ApiResponse.error(10000, "系统内部错误");
    }

    /**
     * 根据错误码映射 HTTP 状态码
     *
     * @param code 业务错误码
     * @return HTTP 状态码
     */
    private int mapCodeToHttpStatus(int code) {
        if (code >= 10000 && code < 20000) return 500;
        if (code >= 20000 && code < 30000) return 400;
        return switch (code) {
            // compute-engine (303xx)
            case 30301 -> 404;
            case 30302 -> 422;
            case 30303, 30308 -> 200;
            case 30304, 30305, 30306, 30310 -> 400;
            case 30307 -> 422;
            case 30309 -> 503;
            // agent-cognition (340xx)
            case 34001 -> 404;
            case 34002, 34011 -> 422;
            case 34003, 34005, 34007, 34010, 34012, 34013 -> 400;
            case 34004 -> 403;
            case 34006 -> 500;
            case 34008 -> 503;
            case 34009 -> 400;
            // 其他业务错误码保持 HTTP 200
            default -> 200;
        };
    }
}
