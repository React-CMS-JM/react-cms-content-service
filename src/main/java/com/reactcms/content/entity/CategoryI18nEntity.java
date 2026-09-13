package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category_i18n")
public class CategoryI18nEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "category_id", nullable = false)
    public Integer categoryId;

    @Column(name = "language_code", nullable = false, length = 5)
    public String languageCode;

    @Column(name = "name", nullable = false, length = 100)
    public String name;

    @Column(name = "slug", nullable = false, length = 100)
    public String slug;
}
