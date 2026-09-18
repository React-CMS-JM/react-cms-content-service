package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.Ids;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.CreatePostRequest;
import com.reactcms.content.dto.LocalizedPostDto;
import com.reactcms.content.dto.MetadataPutRequest;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.PostMetadataDto;
import com.reactcms.content.dto.UpdatePostRequest;
import com.reactcms.content.entity.ContentTypeEntity;
import com.reactcms.content.entity.PostCategoryEntity;
import com.reactcms.content.entity.PostEntity;
import com.reactcms.content.entity.PostI18nEntity;
import com.reactcms.content.entity.PostMetadataEntity;
import com.reactcms.content.entity.PostTagEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostService {

    public static final String COURSE_SLUG = "course";
    public static final Set<String> OWNED_TYPE_SLUGS = Set.of("post", "page", "service", "product");
    private static final String DEFAULT_LANG = "en";

    public PageResult<LocalizedPostDto> list(String type, String status, String lang, int page, int size) {
        String language = normalizeLang(lang);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        StringBuilder where = new StringBuilder(
                "SELECT p FROM PostEntity p, ContentTypeEntity ct WHERE p.contentTypeId = ct.id AND ct.slug <> ?1");
        List<Object> params = new ArrayList<>();
        params.add(COURSE_SLUG);
        int idx = 2;

        if (type != null && !type.isBlank()) {
            String slug = type.trim().toLowerCase(Locale.ROOT);
            if (COURSE_SLUG.equals(slug) || !OWNED_TYPE_SLUGS.contains(slug)) {
                throw new BadRequestException("Unsupported content type: " + slug);
            }
            where.append(" AND ct.slug = ?").append(idx++);
            params.add(slug);
        } else {
            where.append(" AND ct.slug IN (?")
                    .append(idx++)
                    .append(", ?")
                    .append(idx++)
                    .append(", ?")
                    .append(idx++)
                    .append(", ?")
                    .append(idx++)
                    .append(")");
            params.addAll(List.of("post", "page", "service", "product"));
        }

        if (status != null && !status.isBlank()) {
            where.append(" AND p.status = ?").append(idx);
            params.add(status.trim());
        }

        String listQuery = where + " ORDER BY p.createdAt DESC";
        String countQuery = where.toString().replace("SELECT p FROM", "SELECT COUNT(p) FROM");

        var countEm = PostEntity.getEntityManager().createQuery(countQuery, Long.class);
        for (int i = 0; i < params.size(); i++) {
            countEm.setParameter(i + 1, params.get(i));
        }
        long total = countEm.getSingleResult();

        var listEm = PostEntity.getEntityManager().createQuery(listQuery, PostEntity.class);
        for (int i = 0; i < params.size(); i++) {
            listEm.setParameter(i + 1, params.get(i));
        }
        List<PostEntity> posts = listEm
                .setFirstResult(safePage * safeSize)
                .setMaxResults(safeSize)
                .getResultList();

        List<LocalizedPostDto> items = posts.stream()
                .map(p -> toLocalized(p, language, false))
                .collect(Collectors.toList());
        attachMetadata(items);
        return new PageResult<>(items, safePage, safeSize, total);
    }

    public LocalizedPostDto getById(String id, String lang) {
        PostEntity post = requireOwnedPost(id);
        return toLocalized(post, normalizeLang(lang));
    }

    public LocalizedPostDto getBySlug(String slug, String type, String lang) {
        String language = normalizeLang(lang);
        PostI18nEntity i18n = PostI18nEntity.find("slug = ?1 AND languageCode = ?2", slug, language).firstResult();
        if (i18n == null) {
            i18n = PostI18nEntity.find("slug", slug).firstResult();
        }
        if (i18n == null) {
            throw new NotFoundException("Post not found for slug: " + slug);
        }
        PostEntity post = requireOwnedPost(i18n.postId);
        if (type != null && !type.isBlank()) {
            ContentTypeEntity ct = ContentTypeEntity.findById(post.contentTypeId);
            if (ct == null || !type.equalsIgnoreCase(ct.slug)) {
                throw new NotFoundException("Post not found for slug/type: " + slug + "/" + type);
            }
        }
        return toLocalized(post, language);
    }

    @Transactional
    public LocalizedPostDto create(CreatePostRequest req, String fallbackAuthorId) {
        if (req.title == null || req.title.isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (req.content == null) {
            throw new BadRequestException("content is required");
        }
        ContentTypeEntity ct = resolveContentType(req.contentTypeId, req.contentTypeSlug);
        String authorId = req.authorId != null && !req.authorId.isBlank() ? req.authorId : fallbackAuthorId;
        if (authorId == null || authorId.isBlank()) {
            throw new BadRequestException("authorId is required");
        }

        String lang = normalizeLang(req.languageCode);
        String slug = (req.slug == null || req.slug.isBlank()) ? slugify(req.title) : req.slug.trim();

        PostEntity post = new PostEntity();
        post.id = Ids.uuid();
        post.authorId = authorId;
        post.contentTypeId = ct.id;
        post.featuredImageUrl = req.featuredImageUrl;
        post.accessLevel = req.accessLevel != null ? req.accessLevel : "public";
        post.status = req.status != null ? req.status : "draft";
        post.viewCount = 0;
        if ("published".equals(post.status)) {
            post.publishedAt = LocalDateTime.now();
        }
        post.persist();

        PostI18nEntity i18n = new PostI18nEntity();
        i18n.postId = post.id;
        i18n.languageCode = lang;
        i18n.title = req.title;
        i18n.slug = slug;
        i18n.content = req.content;
        i18n.excerpt = req.excerpt;
        i18n.metaTitle = req.metaTitle;
        i18n.metaDescription = req.metaDescription;
        i18n.persist();

        replaceCategories(post.id, req.categoryIds);
        replaceTags(post.id, req.tagIds);

        return toLocalized(post, lang);
    }

    @Transactional
    public LocalizedPostDto update(String id, UpdatePostRequest req) {
        PostEntity post = requireOwnedPost(id);
        if (req.contentTypeId != null) {
            ContentTypeEntity ct = resolveContentType(req.contentTypeId, null);
            post.contentTypeId = ct.id;
        }
        if (req.featuredImageUrl != null) {
            post.featuredImageUrl = req.featuredImageUrl;
        }
        if (req.accessLevel != null) {
            post.accessLevel = req.accessLevel;
        }
        if (req.status != null) {
            applyStatus(post, req.status);
        }
        if (req.categoryIds != null) {
            replaceCategories(post.id, req.categoryIds);
        }
        if (req.tagIds != null) {
            replaceTags(post.id, req.tagIds);
        }

        String lang = normalizeLang(req.languageCode);
        boolean hasI18n = req.title != null || req.slug != null || req.content != null
                || req.excerpt != null || req.metaTitle != null || req.metaDescription != null;
        if (hasI18n) {
            PostI18nEntity i18n = PostI18nEntity.find("postId = ?1 AND languageCode = ?2", post.id, lang)
                    .firstResult();
            if (i18n == null) {
                i18n = new PostI18nEntity();
                i18n.postId = post.id;
                i18n.languageCode = lang;
                i18n.title = req.title != null ? req.title : "";
                i18n.slug = req.slug != null ? req.slug : slugify(i18n.title);
                i18n.content = req.content != null ? req.content : "";
                i18n.persist();
            }
            if (req.title != null) i18n.title = req.title;
            if (req.slug != null) i18n.slug = req.slug;
            if (req.content != null) i18n.content = req.content;
            if (req.excerpt != null) i18n.excerpt = req.excerpt;
            if (req.metaTitle != null) i18n.metaTitle = req.metaTitle;
            if (req.metaDescription != null) i18n.metaDescription = req.metaDescription;
        }
        return toLocalized(post, lang);
    }

    @Transactional
    public LocalizedPostDto patchStatus(String id, String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        PostEntity post = requireOwnedPost(id);
        applyStatus(post, status.trim());
        return toLocalized(post, DEFAULT_LANG);
    }

    @Transactional
    public LocalizedPostDto incrementView(String id) {
        PostEntity post = requireOwnedPost(id);
        post.viewCount = (post.viewCount == null ? 0 : post.viewCount) + 1;
        return toLocalized(post, DEFAULT_LANG);
    }

    @Transactional
    public void delete(String id) {
        PostEntity post = requireOwnedPost(id);
        PostI18nEntity.delete("postId", post.id);
        PostMetadataEntity.delete("postId", post.id);
        PostCategoryEntity.delete("postId", post.id);
        PostTagEntity.delete("postId", post.id);
        post.delete();
    }

    public List<PostMetadataDto> getMetadata(String postId) {
        requireOwnedPost(postId);
        return PostMetadataEntity.<PostMetadataEntity>list("postId", postId).stream()
                .map(this::toMetadataDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PostMetadataDto> putMetadata(String postId, MetadataPutRequest request) {
        requireOwnedPost(postId);
        PostMetadataEntity.delete("postId", postId);
        List<PostMetadataDto> result = new ArrayList<>();
        if (request != null && request.entries != null) {
            for (MetadataPutRequest.MetadataEntry entry : request.entries) {
                if (entry.metaKey == null || entry.metaKey.isBlank()) {
                    continue;
                }
                PostMetadataEntity meta = new PostMetadataEntity();
                meta.id = Ids.uuid();
                meta.postId = postId;
                meta.metaKey = entry.metaKey;
                meta.metaValue = entry.metaValue;
                meta.persist();
                result.add(toMetadataDto(meta));
            }
        }
        return result;
    }

    private void applyStatus(PostEntity post, String status) {
        post.status = status;
        if ("published".equals(status) && post.publishedAt == null) {
            post.publishedAt = LocalDateTime.now();
        }
    }

    private ContentTypeEntity resolveContentType(Integer id, String slug) {
        ContentTypeEntity ct = null;
        if (id != null) {
            ct = ContentTypeEntity.findById(id);
        } else if (slug != null && !slug.isBlank()) {
            ct = ContentTypeEntity.find("slug", slug.trim().toLowerCase(Locale.ROOT)).firstResult();
        }
        if (ct == null) {
            throw new BadRequestException("contentTypeId or contentTypeSlug is required");
        }
        if (COURSE_SLUG.equals(ct.slug) || !OWNED_TYPE_SLUGS.contains(ct.slug)) {
            throw new BadRequestException("Content type not managed by content-service: " + ct.slug);
        }
        return ct;
    }

    private PostEntity requireOwnedPost(String id) {
        PostEntity post = PostEntity.findById(id);
        if (post == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        ContentTypeEntity ct = ContentTypeEntity.findById(post.contentTypeId);
        if (ct == null || COURSE_SLUG.equals(ct.slug) || !OWNED_TYPE_SLUGS.contains(ct.slug)) {
            throw new NotFoundException("Post not found: " + id);
        }
        return post;
    }

    private void replaceCategories(String postId, List<Integer> categoryIds) {
        PostCategoryEntity.delete("postId", postId);
        if (categoryIds == null) {
            return;
        }
        Set<Integer> unique = new HashSet<>(categoryIds);
        for (Integer categoryId : unique) {
            if (categoryId == null) continue;
            PostCategoryEntity link = new PostCategoryEntity();
            link.postId = postId;
            link.categoryId = categoryId;
            link.persist();
        }
    }

    private void replaceTags(String postId, List<Integer> tagIds) {
        PostTagEntity.delete("postId", postId);
        if (tagIds == null) {
            return;
        }
        Set<Integer> unique = new HashSet<>(tagIds);
        for (Integer tagId : unique) {
            if (tagId == null) continue;
            PostTagEntity link = new PostTagEntity();
            link.postId = postId;
            link.tagId = tagId;
            link.persist();
        }
    }

    private LocalizedPostDto toLocalized(PostEntity post, String lang) {
        return toLocalized(post, lang, true);
    }

    private LocalizedPostDto toLocalized(PostEntity post, String lang, boolean includeMetadata) {
        PostI18nEntity i18n = PostI18nEntity.find("postId = ?1 AND languageCode = ?2", post.id, lang).firstResult();
        if (i18n == null) {
            i18n = PostI18nEntity.find("postId", post.id).firstResult();
        }
        LocalizedPostDto dto = new LocalizedPostDto();
        dto.id = post.id;
        dto.authorId = post.authorId;
        dto.contentTypeId = post.contentTypeId;
        dto.featuredImageUrl = post.featuredImageUrl;
        dto.accessLevel = post.accessLevel;
        dto.status = post.status;
        dto.viewCount = post.viewCount;
        dto.publishedAt = post.publishedAt;
        dto.createdAt = post.createdAt;
        dto.updatedAt = post.updatedAt;
        dto.categoryIds = PostCategoryEntity.<PostCategoryEntity>list("postId", post.id).stream()
                .map(pc -> pc.categoryId)
                .collect(Collectors.toList());
        dto.tagIds = PostTagEntity.<PostTagEntity>list("postId", post.id).stream()
                .map(pt -> pt.tagId)
                .collect(Collectors.toList());
        if (i18n != null) {
            dto.languageCode = i18n.languageCode;
            dto.title = i18n.title;
            dto.slug = i18n.slug;
            dto.content = i18n.content;
            dto.excerpt = i18n.excerpt;
            dto.metaTitle = i18n.metaTitle;
            dto.metaDescription = i18n.metaDescription;
        } else {
            dto.languageCode = lang;
            dto.title = "";
            dto.slug = "";
            dto.content = "";
            dto.excerpt = "";
            dto.metaTitle = "";
            dto.metaDescription = "";
        }
        if (includeMetadata) {
            dto.metadata = PostMetadataEntity.<PostMetadataEntity>list("postId", post.id).stream()
                    .map(this::toMetadataDto)
                    .collect(Collectors.toList());
        }
        return dto;
    }

    private void attachMetadata(List<LocalizedPostDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<String> postIds = items.stream().map(item -> item.id).collect(Collectors.toList());
        List<PostMetadataEntity> rows = PostMetadataEntity.list("postId in ?1", postIds);
        Map<String, List<PostMetadataDto>> byPostId = new HashMap<>();
        for (PostMetadataEntity row : rows) {
            byPostId.computeIfAbsent(row.postId, ignored -> new ArrayList<>()).add(toMetadataDto(row));
        }
        for (LocalizedPostDto item : items) {
            item.metadata = byPostId.getOrDefault(item.id, Collections.emptyList());
        }
    }

    private PostMetadataDto toMetadataDto(PostMetadataEntity entity) {
        PostMetadataDto dto = new PostMetadataDto();
        dto.id = entity.id;
        dto.postId = entity.postId;
        dto.metaKey = entity.metaKey;
        dto.metaValue = entity.metaValue;
        return dto;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT_LANG;
        }
        return lang.trim().toLowerCase(Locale.ROOT);
    }

    private static String slugify(String text) {
        return text.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^\\w\\s-]", "")
                .replaceAll("[\\s_-]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
