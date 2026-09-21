package com.reactcms.content.dto;

import java.time.LocalDateTime;

/** Slim recent-activity row for the admin dashboard (table-ready, including author name). */
public class AdminRecentActivityDto {
    public String id;
    public String title;
    public String authorId;
    /** Display name resolved via auth-service; may be empty if lookup fails. */
    public String authorName;
    public String status;
    public String contentTypeSlug;
    public LocalDateTime updatedAt;
}
