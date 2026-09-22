package com.reactcms.content.dto;

public class LocalizedCategoryDto {
    public Integer id;
    public String dbDescription;
    public String languageCode;
    public String name;
    public String slug;
    /** Number of posts linked via posts_categories. */
    public long usageCount;
}
