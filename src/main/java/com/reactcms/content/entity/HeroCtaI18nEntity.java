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
@Table(name = "hero_cta_i18n")
public class HeroCtaI18nEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "hero_cta_id", nullable = false, length = 64)
    public String heroCtaId;

    @Column(name = "language_code", nullable = false, length = 5)
    public String languageCode;

    @Column(name = "label", nullable = false)
    public String label;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
