package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.Ids;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.CommentDto;
import com.reactcms.content.dto.CommentTranslationDto;
import com.reactcms.content.dto.CreateCommentRequest;
import com.reactcms.content.dto.UpdateCommentRequest;
import com.reactcms.content.entity.CommentEntity;
import com.reactcms.content.entity.CommentI18nEntity;
import com.reactcms.content.entity.PostEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommentService {

    private static final String DEFAULT_LANG = "en";

    public List<CommentDto> list(String postId, String status) {
        StringBuilder q = new StringBuilder("1=1");
        List<Object> params = new ArrayList<>();
        int idx = 1;
        if (postId != null && !postId.isBlank()) {
            q.append(" AND postId = ?").append(idx++);
            params.add(postId);
        }
        if (status != null && !status.isBlank()) {
            q.append(" AND status = ?").append(idx);
            params.add(status.trim());
        }
        q.append(" ORDER BY createdAt DESC");
        List<CommentEntity> comments = CommentEntity.<CommentEntity>find(q.toString(), params.toArray()).list();
        Map<String, List<CommentI18nEntity>> i18nByComment = loadActiveI18n(comments);
        return comments.stream()
                .map(c -> toDto(c, i18nByComment.getOrDefault(c.id, List.of())))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto create(CreateCommentRequest req, String fallbackUserId) {
        if (req == null || req.postId == null || req.postId.isBlank()) {
            throw new BadRequestException("postId is required");
        }
        if (req.content == null || req.content.isBlank()) {
            throw new BadRequestException("content is required");
        }
        if (PostEntity.findById(req.postId) == null) {
            throw new NotFoundException("Post not found: " + req.postId);
        }
        String userId = req.userId != null && !req.userId.isBlank() ? req.userId : fallbackUserId;
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required");
        }

        String lang = normalizeLang(req.languageCode);

        CommentEntity comment = new CommentEntity();
        comment.id = Ids.uuid();
        comment.postId = req.postId;
        comment.userId = userId;
        comment.parentCommentId = req.parentCommentId;
        comment.status = req.status != null && !req.status.isBlank() ? req.status : "approved";
        comment.persist();

        CommentI18nEntity i18n = new CommentI18nEntity();
        i18n.commentId = comment.id;
        i18n.languageCode = lang;
        i18n.content = req.content;
        i18n.isOriginal = true;
        i18n.markedForDeletion = false;
        i18n.persist();

        return toDto(comment, List.of(i18n));
    }

    /**
     * Replaces comment text: flags all active i18n rows for deletion batch cleanup,
     * then stores a new original row (language detection will be wired later).
     */
    @Transactional
    public CommentDto updateContent(String id, UpdateCommentRequest req) {
        if (req == null || req.content == null || req.content.isBlank()) {
            throw new BadRequestException("content is required");
        }
        CommentEntity comment = CommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }

        List<CommentI18nEntity> active = CommentI18nEntity.listActive(id);
        for (CommentI18nEntity row : active) {
            row.markedForDeletion = true;
        }
        // Flush so active_language_code unique slot is freed before inserting the new original.
        CommentI18nEntity.getEntityManager().flush();

        CommentI18nEntity i18n = new CommentI18nEntity();
        i18n.commentId = id;
        i18n.languageCode = normalizeLang(req.languageCode);
        i18n.content = req.content;
        i18n.isOriginal = true;
        i18n.markedForDeletion = false;
        i18n.persist();

        return toDto(comment, List.of(i18n));
    }

    /**
     * Returns a cached translation from comment_i18n only (no external Translation API).
     */
    public CommentTranslationDto getTranslation(String id, String lang) {
        CommentEntity comment = CommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }
        String language = normalizeLang(lang);
        CommentI18nEntity i18n = CommentI18nEntity.findActiveByLang(id, language);
        if (i18n == null) {
            throw new NotFoundException("Translation not found for comment " + id + " lang=" + language);
        }
        CommentTranslationDto dto = new CommentTranslationDto();
        dto.commentId = id;
        dto.languageCode = i18n.languageCode;
        dto.content = i18n.content;
        dto.isOriginal = i18n.isOriginal;
        return dto;
    }

    /**
     * Persists a translation row when one does not already exist for the language.
     * Intended for future Content Translation API results; does not call any external API.
     */
    @Transactional
    public CommentTranslationDto upsertTranslation(String id, String lang, String content) {
        if (content == null || content.isBlank()) {
            throw new BadRequestException("content is required");
        }
        CommentEntity comment = CommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }
        String language = normalizeLang(lang);
        CommentI18nEntity i18n = CommentI18nEntity.findActiveByLang(id, language);
        if (i18n == null) {
            i18n = new CommentI18nEntity();
            i18n.commentId = id;
            i18n.languageCode = language;
            i18n.isOriginal = false;
            i18n.markedForDeletion = false;
            i18n.persist();
        }
        i18n.content = content;
        CommentTranslationDto dto = new CommentTranslationDto();
        dto.commentId = id;
        dto.languageCode = i18n.languageCode;
        dto.content = i18n.content;
        dto.isOriginal = i18n.isOriginal;
        return dto;
    }

    @Transactional
    public CommentDto patchStatus(String id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        CommentEntity comment = CommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }
        comment.status = status.trim();
        return toDto(comment, CommentI18nEntity.listActive(id));
    }

    @Transactional
    public void delete(String id) {
        CommentEntity comment = CommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }
        CommentI18nEntity.delete("commentId", id);
        comment.delete();
    }

    private Map<String, List<CommentI18nEntity>> loadActiveI18n(List<CommentEntity> comments) {
        Map<String, List<CommentI18nEntity>> map = new HashMap<>();
        if (comments.isEmpty()) {
            return map;
        }
        List<String> ids = comments.stream().map(c -> c.id).collect(Collectors.toList());
        for (CommentI18nEntity row : CommentI18nEntity.listActiveForComments(ids)) {
            map.computeIfAbsent(row.commentId, k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private CommentDto toDto(CommentEntity entity, List<CommentI18nEntity> i18nRows) {
        CommentDto dto = new CommentDto();
        dto.id = entity.id;
        dto.postId = entity.postId;
        dto.userId = entity.userId;
        dto.parentCommentId = entity.parentCommentId;
        dto.status = entity.status;
        dto.createdAt = entity.createdAt;
        dto.updatedAt = entity.updatedAt;

        CommentI18nEntity original = null;
        List<String> translations = new ArrayList<>();
        for (CommentI18nEntity row : i18nRows) {
            if (row.isOriginal && original == null) {
                original = row;
            } else if (!row.isOriginal) {
                translations.add(row.languageCode);
            }
        }
        if (original == null && !i18nRows.isEmpty()) {
            original = i18nRows.get(0);
        }
        if (original != null) {
            dto.content = original.content;
            dto.languageCode = original.languageCode;
        } else {
            dto.content = "";
            dto.languageCode = DEFAULT_LANG;
        }
        dto.availableTranslationLanguages = translations;
        return dto;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT_LANG;
        }
        return lang.trim().toLowerCase();
    }
}
