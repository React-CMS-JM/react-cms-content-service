package com.reactcms.content.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "comment_i18n")
public class CommentI18nEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;

    @Column(name = "comment_id", nullable = false, length = 64)
    public String commentId;

    @Column(name = "language_code", nullable = false, length = 5)
    public String languageCode;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    public String content;

    @Column(name = "is_original", nullable = false)
    public boolean isOriginal = false;

    @Column(name = "marked_for_deletion", nullable = false)
    public boolean markedForDeletion = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    public LocalDateTime updatedAt;

    public static CommentI18nEntity findActiveOriginal(String commentId) {
        return find("commentId = ?1 AND isOriginal = true AND markedForDeletion = false", commentId)
                .firstResult();
    }

    public static CommentI18nEntity findActiveByLang(String commentId, String languageCode) {
        return find(
                "commentId = ?1 AND languageCode = ?2 AND markedForDeletion = false",
                commentId,
                languageCode)
                .firstResult();
    }

    public static List<CommentI18nEntity> listActive(String commentId) {
        return list("commentId = ?1 AND markedForDeletion = false", commentId);
    }

    public static List<CommentI18nEntity> listActiveForComments(List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return List.of();
        }
        return list("commentId IN ?1 AND markedForDeletion = false", commentIds);
    }
}
