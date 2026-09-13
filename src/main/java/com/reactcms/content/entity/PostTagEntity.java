package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "posts_tags")
@IdClass(PostTagEntity.Pk.class)
public class PostTagEntity extends PanacheEntityBase {

    @Id
    @Column(name = "post_id", length = 64)
    public String postId;

    @Id
    @Column(name = "tag_id")
    public Integer tagId;

    public static class Pk implements Serializable {
        public String postId;
        public Integer tagId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(postId, pk.postId) && Objects.equals(tagId, pk.tagId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, tagId);
        }
    }
}
