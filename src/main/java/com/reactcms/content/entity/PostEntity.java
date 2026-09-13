package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class PostEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "author_id", nullable = false, length = 64)
    public String authorId;

    @Column(name = "content_type_id", nullable = false)
    public Integer contentTypeId;

    @Column(name = "featured_image_url", columnDefinition = "text")
    public String featuredImageUrl;

    @Column(name = "access_level", length = 50)
    public String accessLevel = "public";

    @Column(name = "status", length = 50)
    public String status = "draft";

    @Column(name = "view_count")
    public Integer viewCount = 0;

    @Column(name = "published_at")
    public LocalDateTime publishedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
