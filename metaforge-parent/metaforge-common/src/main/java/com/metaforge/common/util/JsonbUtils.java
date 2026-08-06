package com.metaforge.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/**
 * JSON 序列化/反序列化工具类。
 *
 * <p>基于 Jackson {@link ObjectMapper} 提供对象与 JSON 字符串之间的转换，
 * 统一配置日期格式、时区和空值序列化策略。</p>
 */
public final class JsonbUtils {

    /** 全局共享的 ObjectMapper 实例（线程安全） */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();

        // 注册 Java 8 时间模块，支持 LocalDateTime 等类型
        OBJECT_MAPPER.registerModule(new JavaTimeModule());

        // 设置时区为 Asia/Shanghai
        OBJECT_MAPPER.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 序列化时忽略 null 字段
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 禁用将日期序列化为时间戳数字，改用字符串格式
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonbUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串。
     *
     * @param object 待序列化的对象
     * @return JSON 字符串
     * @throws RuntimeException 序列化失败时抛出，原始异常为 {@link JsonProcessingException}
     */
    public static String toJsonb(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的 Java 对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标泛型类型
     * @return 反序列化后的对象
     * @throws RuntimeException 反序列化失败时抛出，原始异常为 {@link JsonProcessingException}
     */
    public static <T> T fromJsonb(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败: " + clazz.getName(), e);
        }
    }

    /**
     * 将 JSON 数组字符串反序列化为 {@link List}。
     *
     * @param json  JSON 数组字符串
     * @param clazz 列表元素类型
     * @param <T>   元素泛型类型
     * @return 反序列化后的列表，JSON 为空时返回空列表
     * @throws RuntimeException 反序列化失败时抛出，原始异常为 {@link JsonProcessingException}
     */
    public static <T> List<T> fromJsonbList(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final var type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 数组反序列化失败: " + clazz.getName(), e);
        }
    }
}
