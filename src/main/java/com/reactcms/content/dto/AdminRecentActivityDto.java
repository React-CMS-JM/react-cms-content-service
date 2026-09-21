package com.reactcms.content.dto;

import java.time.LocalDateTime;

/** Slim recent-activity row for the admin dashboard. */
public class AdminRecentActivityDto {
    public String id;
    public String title;
    public String authorId;
    public String status;
    public String contentTypeSlug;
    public LocalDateTime updatedAt;
}
