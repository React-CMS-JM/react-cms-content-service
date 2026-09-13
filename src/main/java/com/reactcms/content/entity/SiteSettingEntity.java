package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "site_settings")
public class SiteSettingEntity extends PanacheEntityBase {

    @Id
    @Column(name = "setting_key", length = 100)
    public String settingKey;

    @Column(name = "setting_value", columnDefinition = "text")
    public String settingValue;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;
}
