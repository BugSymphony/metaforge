package com.metaforge.common.dto;

/**
 * 分页请求 DTO，用于接收前端传递的分页参数。
 *
 * <p>页码为 1-based，每页条数限制在 1-100 之间，
 * 排序字段格式为 "field:asc" 或 "field:desc"。
 */
public class PageRequest {

    /** 默认页码 */
    private static final int DEFAULT_PAGE = 1;
    /** 默认每页条数 */
    private static final int DEFAULT_SIZE = 20;
    /** 每页最小条数 */
    private static final int MIN_SIZE = 1;
    /** 每页最大条数 */
    private static final int MAX_SIZE = 100;

    /** 页码，1-based，最小为 1 */
    private int page;

    /** 每页条数，范围 [1, 100] */
    private int size;

    /** 排序字段与方向，格式 "field:asc|desc"，允许为 null */
    private String sort;

    /**
     * 无参构造方法，使用默认值：page=1, size=20。
     */
    public PageRequest() {
        this.page = DEFAULT_PAGE;
        this.size = DEFAULT_SIZE;
    }

    /**
     * 构造方法，指定页码与每页条数，不指定排序。
     *
     * @param page 页码，若小于 1 则置为 1
     * @param size 每页条数，若小于 1 则置为 1，若大于 100 则置为 100
     */
    public PageRequest(int page, int size) {
        this(page, size, null);
    }

    /**
     * 构造方法，指定页码、每页条数与排序规则。
     *
     * @param page 页码，若小于 1 则置为 1
     * @param size 每页条数，若小于 1 则置为 1，若大于 100 则置为 100
     * @param sort 排序字段与方向，格式 "field:asc|desc"，可为 null
     */
    public PageRequest(int page, int size, String sort) {
        this.page = Math.max(page, DEFAULT_PAGE);
        this.size = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        this.sort = sort;
    }

    // ==================== Getters & Setters ====================

    public int getPage() {
        return page;
    }

    /**
     * 设置页码，若传入值小于 1 则自动置为 1。
     */
    public void setPage(int page) {
        this.page = Math.max(page, DEFAULT_PAGE);
    }

    public int getSize() {
        return size;
    }

    /**
     * 设置每页条数，自动限制在 [1, 100] 范围内。
     */
    public void setSize(int size) {
        this.size = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
