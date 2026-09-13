package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "param_ui_strings")
public class ParamUiStringEntity extends PanacheEntityBase {

    @Id
    @Column(name = "string_key", length = 150)
    public String stringKey;

    @Column(name = "ui_component", length = 100)
    public String uiComponent;

    @Column(name = "description", columnDefinition = "text")
    public String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;
}
