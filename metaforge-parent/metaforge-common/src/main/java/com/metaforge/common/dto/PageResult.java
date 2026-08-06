package com.metaforge.common.dto;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果 DTO，封装分页查询的返回数据。
 *
 * <p>包含当前页数据列表、总记录数、分页元信息。
 * 总页数通过 total 和 size 自动计算（ceil(total/size)）。
 *
 * @param <T> 数据列表元素类型
 */
public class PageResult<T> {

    /** 当前页数据列表 */
    private final List<T> content;

    /** 总记录数 */
    private final long total;

    /** 当前页码（1-based） */
    private final int page;

    /** 每页条数 */
    private final int size;

    /** 总页数，由 total 与 size 自动计算 */
    private final int totalPages;

    /**
     * 构造分页结果，自动计算总页数。
     *
     * @param content 当前页数据列表
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页条数
     */
    public PageResult(List<T> content, long total, int page, int size) {
        this.content = content != null ? content : Collections.emptyList();
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = computeTotalPages(total, size);
    }

    /**
     * 计算总页数：ceil(total / size)。
     * 当 total 为 0 或 size 小于等于 0 时返回 0。
     */
    private static int computeTotalPages(long total, int size) {
        if (total <= 0 || size <= 0) {
            return 0;
        }
        return (int) ((total + size - 1) / size);
    }

    // ==================== Getters ====================

    public List<T> getContent() {
        return content;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
