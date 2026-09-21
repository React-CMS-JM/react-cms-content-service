package com.reactcms.content.client;

/** Slim user summary returned by auth-service GET /api/users/by-ids. */
public class UserSummaryDto {
    public String id;
    public String firstName;
    public String lastName;
    public String avatarColor;
}
