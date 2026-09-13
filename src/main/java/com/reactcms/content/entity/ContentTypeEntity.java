package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "content_types")
public class ContentTypeEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "name", nullable = false, length = 50)
    public String name;

    @Column(name = "slug", nullable = false, length = 50, unique = true)
    public String slug;

    @Column(name = "description", columnDefinition = "text")
    public String description;
}
