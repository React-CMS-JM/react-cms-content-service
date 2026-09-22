package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.LocalizedTagDto;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.entity.PostTagEntity;
import com.reactcms.content.entity.TagEntity;
import com.reactcms.content.entity.TagI18nEntity;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheManager;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class TagService {

    public static final int POPULAR_LIMIT = 100;
    public static final int SEARCH_LIMIT = 20;
    public static final int ADMIN_PAGE_SIZE = 10;

    @Inject
    CacheManager cacheManager;

    @CacheResult(cacheName = "popular-tags")
    public List<LocalizedTagDto> listPopular(String lang) {
        String language = normalizeLang(lang);
        List<TagEntity> rows = TagEntity.findAll(
                        Sort.by("usageCount").descending()
                                .and("updatedAt").descending()
                                .and("createdAt").descending())
                .page(0, POPULAR_LIMIT)
                .list();
        return rows.stream().map(t -> toLocalized(t, language)).collect(Collectors.toList());
    }

    public List<LocalizedTagDto> search(String q, String lang, int limit) {
        String language = normalizeLang(lang);
        String query = CategoryService.normalizeSearchQuery(q);
        if (query == null) {
            return List.of();
        }
        int safeLimit = limit > 0 ? Math.min(limit, SEARCH_LIMIT) : SEARCH_LIMIT;
        String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        @SuppressWarnings("unchecked")
        List<Integer> ids = TagEntity.getEntityManager()
                .createQuery(
                        "SELECT DISTINCT i.tagId FROM TagI18nEntity i "
                                + "WHERE LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q")
                .setParameter("q", pattern)
                .setMaxResults(safeLimit * 4)
                .getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, TagEntity> byId = TagEntity.<TagEntity>list("id in ?1", ids).stream()
                .collect(Collectors.toMap(t -> t.id, t -> t, (a, b) -> a));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .sorted(TagService::compareUsageThenDates)
                .limit(safeLimit)
                .map(t -> toLocalized(t, language))
                .collect(Collectors.toList());
    }

    public List<LocalizedTagDto> listByIds(Collection<Integer> ids, String lang) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String language = normalizeLang(lang);
        List<Integer> unique = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (unique.isEmpty()) {
            return List.of();
        }
        return TagEntity.<TagEntity>list("id in ?1", unique).stream()
                .map(t -> toLocalized(t, language))
                .collect(Collectors.toList());
    }

    public PageResult<LocalizedTagDto> listAdmin(String lang, int page, int size, String q) {
        String language = normalizeLang(lang);
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 50) : ADMIN_PAGE_SIZE;
        String query = CategoryService.normalizeSearchQuery(q);

        long total;
        List<TagEntity> rows;
        if (query == null) {
            total = TagEntity.count();
            rows = TagEntity.findAll(
                            Sort.by("usageCount").descending()
                                    .and("updatedAt").descending()
                                    .and("createdAt").descending())
                    .page(safePage, safeSize)
                    .list();
        } else {
            String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
            Query countQuery = TagEntity.getEntityManager()
                    .createQuery(
                            "SELECT COUNT(DISTINCT t.id) FROM TagEntity t, TagI18nEntity i "
                                    + "WHERE i.tagId = t.id "
                                    + "AND (LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q)");
            countQuery.setParameter("q", pattern);
            total = (Long) countQuery.getSingleResult();

            @SuppressWarnings("unchecked")
            List<Integer> pageIds = TagEntity.getEntityManager()
                    .createQuery(
                            "SELECT t.id FROM TagEntity t, TagI18nEntity i "
                                    + "WHERE i.tagId = t.id "
                                    + "AND (LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q) "
                                    + "GROUP BY t.id, t.usageCount, t.updatedAt, t.createdAt "
                                    + "ORDER BY t.usageCount DESC, t.updatedAt DESC, t.createdAt DESC")
                    .setParameter("q", pattern)
                    .setFirstResult(safePage * safeSize)
                    .setMaxResults(safeSize)
                    .getResultList();
            if (pageIds.isEmpty()) {
                rows = List.of();
            } else {
                Map<Integer, TagEntity> byId = TagEntity.<TagEntity>list("id in ?1", pageIds).stream()
                        .collect(Collectors.toMap(t -> t.id, t -> t, (a, b) -> a));
                rows = pageIds.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }

        List<LocalizedTagDto> items =
                rows.stream().map(t -> toLocalized(t, language)).collect(Collectors.toList());
        return new PageResult<>(items, safePage, safeSize, total);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-tags")
    public LocalizedTagDto create(TaxonomyUpsertRequest req) {
        validate(req);
        LocalDateTime now = LocalDateTime.now();
        TagEntity tag = new TagEntity();
        tag.dbDescription = req.dbDescription != null ? req.dbDescription : req.name;
        tag.usageCount = 0;
        tag.createdAt = now;
        tag.updatedAt = now;
        tag.persist();

        TagI18nEntity i18n = new TagI18nEntity();
        i18n.tagId = tag.id;
        i18n.languageCode = normalizeLang(req.languageCode);
        i18n.name = req.name;
        i18n.slug = resolveSlug(req);
        i18n.persist();
        return toLocalized(tag, i18n.languageCode);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-tags")
    public LocalizedTagDto update(Integer id, TaxonomyUpsertRequest req) {
        TagEntity tag = TagEntity.findById(id);
        if (tag == null) {
            throw new NotFoundException("Tag not found: " + id);
        }
        if (req.dbDescription != null) {
            tag.dbDescription = req.dbDescription;
        }
        tag.updatedAt = LocalDateTime.now();
        String lang = normalizeLang(req.languageCode);
        TagI18nEntity i18n = TagI18nEntity.find("tagId = ?1 AND languageCode = ?2", id, lang).firstResult();
        if (i18n == null) {
            if (req.name == null || req.name.isBlank()) {
                throw new BadRequestException("name is required when creating a translation");
            }
            i18n = new TagI18nEntity();
            i18n.tagId = id;
            i18n.languageCode = lang;
            i18n.name = req.name;
            i18n.slug = resolveSlug(req);
            i18n.persist();
        } else {
            if (req.name != null) i18n.name = req.name;
            if (req.slug != null) i18n.slug = req.slug;
            else if (req.name != null) i18n.slug = slugify(req.name);
        }
        return toLocalized(tag, lang);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-tags")
    public void delete(Integer id) {
        TagEntity tag = TagEntity.findById(id);
        if (tag == null) {
            throw new NotFoundException("Tag not found: " + id);
        }
        TagI18nEntity.delete("tagId", id);
        PostTagEntity.delete("tagId", id);
        tag.delete();
    }

    public void invalidatePopularCache() {
        cacheManager.getCache("popular-tags").ifPresent(cache ->
                cache.invalidateAll().await().indefinitely());
    }

    @Transactional
    public void refreshUsageCounts(Collection<Integer> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Integer id : tagIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())) {
            TagEntity tag = TagEntity.findById(id);
            if (tag == null) continue;
            long count = PostTagEntity.count("tagId", id);
            tag.usageCount = (int) Math.min(count, Integer.MAX_VALUE);
            tag.updatedAt = LocalDateTime.now();
        }
        invalidatePopularCache();
    }

    private LocalizedTagDto toLocalized(TagEntity tag, String lang) {
        TagI18nEntity i18n = TagI18nEntity.find("tagId = ?1 AND languageCode = ?2", tag.id, lang).firstResult();
        if (i18n == null) {
            i18n = TagI18nEntity.find("tagId", tag.id).firstResult();
        }
        LocalizedTagDto dto = new LocalizedTagDto();
        dto.id = tag.id;
        dto.dbDescription = tag.dbDescription;
        dto.usageCount = tag.usageCount;
        if (i18n != null) {
            dto.languageCode = i18n.languageCode;
            dto.name = i18n.name;
            dto.slug = i18n.slug;
        } else {
            dto.languageCode = lang;
            dto.name = "";
            dto.slug = "";
        }
        return dto;
    }

    private void validate(TaxonomyUpsertRequest req) {
        if (req == null || req.name == null || req.name.isBlank()) {
            throw new BadRequestException("name is required");
        }
    }

    private String resolveSlug(TaxonomyUpsertRequest req) {
        if (req.slug != null && !req.slug.isBlank()) {
            return req.slug.trim();
        }
        return slugify(req.name);
    }

    static int compareUsageThenDates(TagEntity a, TagEntity b) {
        int byUsage = Integer.compare(b.usageCount, a.usageCount);
        if (byUsage != 0) return byUsage;
        int byUpdated = compareNullableDates(b.updatedAt, a.updatedAt);
        if (byUpdated != 0) return byUpdated;
        return compareNullableDates(b.createdAt, a.createdAt);
    }

    private static int compareNullableDates(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) return "en";
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
