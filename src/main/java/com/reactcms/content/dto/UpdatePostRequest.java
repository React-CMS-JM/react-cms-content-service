package com.reactcms.content.dto;

import java.util.List;

public class UpdatePostRequest {
    public Integer contentTypeId;
    public String featuredImageUrl;
    public String accessLevel;
    public String status;
    public List<Integer> categoryIds;
    public List<Integer> tagIds;
    public String languageCode;
    public String title;
    public String slug;
    public String content;
    public String excerpt;
    public String metaTitle;
    public String metaDescription;
}
