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
@Table(name = "nav_items")
public class NavItemEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "item_key", nullable = false, length = 50, unique = true)
    public String itemKey;

    @Column(name = "href")
    public String href;

    @Column(name = "page_id", length = 64)
    public String pageId;

    @Column(name = "sort_order", nullable = false)
    public Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    public Boolean isVisible = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
