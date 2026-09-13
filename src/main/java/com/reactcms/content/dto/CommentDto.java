package com.reactcms.content.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDto {
    public String id;
    public String postId;
    public String userId;
    public String parentCommentId;
    /** Original comment body (is_original row in comment_i18n). */
    public String content;
    /** Language of the original comment. */
    public String languageCode;
    /** Active non-original language codes already cached in comment_i18n. */
    public List<String> availableTranslationLanguages = new ArrayList<>();
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
