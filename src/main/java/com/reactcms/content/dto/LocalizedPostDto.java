package com.reactcms.content.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LocalizedPostDto {
    public String id;
    public String authorId;
    public Integer contentTypeId;
    public String featuredImageUrl;
    public String accessLevel;
    public String status;
    public Integer viewCount;
    public LocalDateTime publishedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public List<Integer> categoryIds = new ArrayList<>();
    public List<Integer> tagIds = new ArrayList<>();
    public String languageCode;
    public String title;
    public String slug;
    public String content;
    public String excerpt;
    public String metaTitle;
    public String metaDescription;
}
