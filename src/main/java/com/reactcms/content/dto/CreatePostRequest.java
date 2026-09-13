package com.reactcms.content.dto;

import java.util.ArrayList;
import java.util.List;

public class CreatePostRequest {
    public String authorId;
    public Integer contentTypeId;
    public String contentTypeSlug;
    public String featuredImageUrl;
    public String accessLevel = "public";
    public String status = "draft";
    public List<Integer> categoryIds = new ArrayList<>();
    public List<Integer> tagIds = new ArrayList<>();
    public String languageCode = "en";
    public String title;
    public String slug;
    public String content;
    public String excerpt;
    public String metaTitle;
    public String metaDescription;
}
