package com.reactcms.content.dto;

import java.util.List;

public class PageResult<T> {
    public List<T> items;
    public int page;
    public int size;
    public long total;

    public PageResult() {
    }

    public PageResult(List<T> items, int page, int size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }
}
