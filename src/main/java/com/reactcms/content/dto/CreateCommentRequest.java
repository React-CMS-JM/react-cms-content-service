package com.reactcms.content.dto;

public class CreateCommentRequest {
    public String postId;
    public String userId;
    public String parentCommentId;
    public String content;
    /** Provisional language until Content Translation API detection is wired. */
    public String languageCode;
    public String status;
}
