package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
public class CommentEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "post_id", nullable = false, length = 64)
    public String postId;

    @Column(name = "user_id", nullable = false, length = 64)
    public String userId;

    @Column(name = "parent_comment_id", length = 64)
    public String parentCommentId;

    @Column(name = "status", length = 50)
    public String status = "approved";

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
