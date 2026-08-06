package com.metaforge.metamodel.api.dto;

import java.util.List;

/**
 * 元素查询请求 DTO，支持 FQN 前缀集合批量过滤。
 */
public class ElementQueryRequest {

    /** FQN 前缀列表（OR 逻辑拼接） */
    private List<String> fqnPrefixes;

    /** 页码（1-based） */
    private int page = 1;

    /** 每页条数 */
    private int size = 20;

    public List<String> getFqnPrefixes() { return fqnPrefixes; }
    public void setFqnPrefixes(List<String> fqnPrefixes) { this.fqnPrefixes = fqnPrefixes; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
