package com.metaforge.framework.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页工具类，在 common 页 PageRequest / PageResult 与 Spring Pageable / Page 之间互转。
 */
public final class PageHelper {

    private PageHelper() {
    }

    /**
     * 将 common PageRequest 转换为 Spring Pageable。
     * common 层 page 为 1-based，Spring Pageable 为 0-based，此处做偏移转换。
     *
     * @param request common 层分页请求，可为 null
     * @return Spring Pageable 对象；request 为 null 时返回 Pageable.unpaged()
     */
    public static Pageable toSpringPageable(PageRequest request) {
        if (request == null) {
            return Pageable.unpaged();
        }

        int page = request.getPage() > 0 ? request.getPage() - 1 : 0;
        int size = request.getSize();

        Sort sort = buildSort(request.getSort());
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }

    /**
     * 将 Spring Page 转换为 common PageResult。
     * Spring page number 为 0-based，common PageResult page 转回 1-based。
     *
     * @param springPage Spring Data 分页结果
     * @param <T>        数据类型
     * @return common 层分页结果
     */
    public static <T> PageResult<T> fromSpringPage(Page<T> springPage) {
        return new PageResult<>(
            springPage.getContent(),
            springPage.getTotalElements(),
            springPage.getNumber() + 1,
            springPage.getSize()
        );
    }

    /**
     * 解析排序字符串 "field:asc" 或 "field:desc" 为 Spring Sort。
     *
     * @param sortStr 排序字符串，支持逗号分隔多个字段
     * @return Spring Sort 对象；sortStr 为空时返回 Sort.unsorted()
     */
    private static Sort buildSort(String sortStr) {
        if (sortStr == null || sortStr.trim().isEmpty()) {
            return Sort.unsorted();
        }

        String[] parts = sortStr.split(",");
        List<Sort.Order> orders = new ArrayList<>();
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] fieldAndDir = part.split(":");
            String field = fieldAndDir[0].trim();
            Sort.Direction direction = Sort.Direction.ASC;
            if (fieldAndDir.length > 1 && "desc".equalsIgnoreCase(fieldAndDir[1].trim())) {
                direction = Sort.Direction.DESC;
            }
            orders.add(new Sort.Order(direction, field));
        }
        return Sort.by(orders);
    }
}
