package com.metaforge.common.util;

import com.metaforge.common.dto.PageRequest;
import com.metaforge.common.dto.PageResult;

import java.util.Collections;
import java.util.List;

/**
 * 内存分页工具类。
 *
 * <p>对已加载到内存的 {@link List} 进行 subList 切片分页，适用于小数据量场景。</p>
 */
public final class PageUtils {

    private PageUtils() {
    }

    /**
     * 对列表进行内存分页。
     *
     * <ul>
     *   <li>list 为 {@code null} → 视为空列表</li>
     *   <li>request 为 {@code null} → 使用默认参数（第 1 页，每页 20 条）</li>
     *   <li>页码越界（超出总页数）→ 返回空结果，但仍包含正确的 total</li>
     * </ul>
     *
     * @param list    待分页的完整列表
     * @param request 分页请求参数
     * @param <T>     列表元素类型
     * @return 分页结果
     */
    public static <T> PageResult<T> paginate(List<T> list, PageRequest request) {
        if (list == null) {
            list = Collections.emptyList();
        }

        if (request == null) {
            request = new PageRequest();
        }

        final int totalSize = list.size();
        final int page = request.getPage();
        final int size = request.getSize();

        final int fromIndex = (page - 1) * size;

        if (fromIndex >= totalSize) {
            return new PageResult<>(Collections.emptyList(), totalSize, page, size);
        }

        final int toIndex = Math.min(fromIndex + size, totalSize);
        final List<T> pageRecords = list.subList(fromIndex, toIndex);

        return new PageResult<>(pageRecords, totalSize, page, size);
    }
}
