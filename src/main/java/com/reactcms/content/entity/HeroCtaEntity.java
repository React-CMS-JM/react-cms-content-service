package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "hero_ctas")
public class HeroCtaEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "href", nullable = false, length = 500)
    public String href;

    @Column(name = "text_color", nullable = false, length = 20)
    public String textColor = "#ffffff";

    @Column(name = "background_color", nullable = false, length = 20)
    public String backgroundColor = "#6366f1";

    @Column(name = "sort_order", nullable = false)
    public Integer sortOrder = 0;

    @Column(name = "is_visible", nullable = false)
    public Boolean isVisible = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
