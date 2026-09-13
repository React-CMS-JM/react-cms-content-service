package com.reactcms.content.dto;

public class ContentTypeDto {
    public Integer id;
    public String name;
    public String slug;
    public String description;

    public ContentTypeDto() {
    }

    public ContentTypeDto(Integer id, String name, String slug, String description) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
    }
}
