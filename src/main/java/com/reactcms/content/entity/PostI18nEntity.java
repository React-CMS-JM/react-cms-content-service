package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_i18n")
public class PostI18nEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "post_id", nullable = false, length = 64)
    public String postId;

    @Column(name = "language_code", nullable = false, length = 5)
    public String languageCode;

    @Column(name = "title", nullable = false)
    public String title;

    @Column(name = "slug", nullable = false)
    public String slug;

    @Column(name = "content", nullable = false, columnDefinition = "longtext")
    public String content;

    @Column(name = "excerpt", columnDefinition = "text")
    public String excerpt;

    @Column(name = "meta_title")
    public String metaTitle;

    @Column(name = "meta_description", columnDefinition = "text")
    public String metaDescription;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
