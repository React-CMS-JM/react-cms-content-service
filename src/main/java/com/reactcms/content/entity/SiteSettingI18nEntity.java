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
@Table(name = "site_setting_i18n")
public class SiteSettingI18nEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "setting_key", nullable = false, length = 100)
    public String settingKey;

    @Column(name = "language_code", nullable = false, length = 5)
    public String languageCode;

    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    public String settingValue;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
