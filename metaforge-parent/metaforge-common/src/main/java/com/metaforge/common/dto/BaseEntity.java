package com.metaforge.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体 DTO 抽象类，提供公共审计字段。
 *
 * <p>所有业务 DTO 可继承此类以获得主键、创建时间、更新时间字段。
 * 本类为 common 层纯数据传输对象，不绑定 JPA 注解。
 * JPA 实体的审计字段应在框架层或 BC 层通过 {@code @MappedSuperclass} 独立定义。
 */
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    private Long id;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}
