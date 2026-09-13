package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "post_metadata")
public class PostMetadataEntity extends PanacheEntityBase {

    @Id
    @Column(name = "id", length = 64)
    public String id;

    @Column(name = "post_id", nullable = false, length = 64)
    public String postId;

    @Column(name = "meta_key", nullable = false, length = 100)
    public String metaKey;

    @Column(name = "meta_value", columnDefinition = "text")
    public String metaValue;

    @Column(name = "json_value", columnDefinition = "json")
    public String jsonValue;
}
