package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.LocalizedCategoryDto;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.entity.CategoryEntity;
import com.reactcms.content.entity.CategoryI18nEntity;
import com.reactcms.content.entity.PostCategoryEntity;
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
public class CategoryService {

    public static final int POPULAR_LIMIT = 100;
    public static final int SEARCH_LIMIT = 20;
    public static final int ADMIN_PAGE_SIZE = 10;

    @Inject
    CacheManager cacheManager;

    /** Top N by usage (Caffeine-backed). Prefer for autocomplete/dropdowns. */
    @CacheResult(cacheName = "popular-categories")
    public List<LocalizedCategoryDto> listPopular(String lang) {
        String language = normalizeLang(lang);
        List<CategoryEntity> rows = CategoryEntity.findAll(
                        Sort.by("usageCount").descending()
                                .and("updatedAt").descending()
                                .and("createdAt").descending())
                .page(0, POPULAR_LIMIT)
                .list();
        return rows.stream().map(c -> toLocalized(c, language)).collect(Collectors.toList());
    }

    /** Name/slug substring search (uncached). Requires ≥2 non-space characters. */
    public List<LocalizedCategoryDto> search(String q, String lang, int limit) {
        String language = normalizeLang(lang);
        String query = normalizeSearchQuery(q);
        if (query == null) {
            return List.of();
        }
        int safeLimit = limit > 0 ? Math.min(limit, SEARCH_LIMIT) : SEARCH_LIMIT;
        String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        @SuppressWarnings("unchecked")
        List<Integer> ids = CategoryEntity.getEntityManager()
                .createQuery(
                        "SELECT DISTINCT i.categoryId FROM CategoryI18nEntity i "
                                + "WHERE LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q")
                .setParameter("q", pattern)
                .setMaxResults(safeLimit * 4)
                .getResultList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<CategoryEntity> entities = CategoryEntity.list("id in ?1", ids);
        Map<Integer, CategoryEntity> byId = entities.stream()
                .collect(Collectors.toMap(c -> c.id, c -> c, (a, b) -> a));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .sorted(CategoryService::compareUsageThenDates)
                .limit(safeLimit)
                .map(c -> toLocalized(c, language))
                .collect(Collectors.toList());
    }

    public List<LocalizedCategoryDto> listByIds(Collection<Integer> ids, String lang) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        String language = normalizeLang(lang);
        List<Integer> unique = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (unique.isEmpty()) {
            return List.of();
        }
        return CategoryEntity.<CategoryEntity>list("id in ?1", unique).stream()
                .map(c -> toLocalized(c, language))
                .collect(Collectors.toList());
    }

    /** Admin table: paginated, never served from popular cache. */
    public PageResult<LocalizedCategoryDto> listAdmin(String lang, int page, int size, String q) {
        String language = normalizeLang(lang);
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? Math.min(size, 50) : ADMIN_PAGE_SIZE;
        String query = normalizeSearchQuery(q);

        long total;
        List<CategoryEntity> rows;
        if (query == null) {
            total = CategoryEntity.count();
            rows = CategoryEntity.findAll(
                            Sort.by("usageCount").descending()
                                    .and("updatedAt").descending()
                                    .and("createdAt").descending())
                    .page(safePage, safeSize)
                    .list();
        } else {
            String pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
            Query countQuery = CategoryEntity.getEntityManager()
                    .createQuery(
                            "SELECT COUNT(DISTINCT c.id) FROM CategoryEntity c, CategoryI18nEntity i "
                                    + "WHERE i.categoryId = c.id "
                                    + "AND (LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q)");
            countQuery.setParameter("q", pattern);
            total = (Long) countQuery.getSingleResult();

            @SuppressWarnings("unchecked")
            List<Integer> pageIds = CategoryEntity.getEntityManager()
                    .createQuery(
                            "SELECT c.id FROM CategoryEntity c, CategoryI18nEntity i "
                                    + "WHERE i.categoryId = c.id "
                                    + "AND (LOWER(i.name) LIKE :q OR LOWER(i.slug) LIKE :q) "
                                    + "GROUP BY c.id, c.usageCount, c.updatedAt, c.createdAt "
                                    + "ORDER BY c.usageCount DESC, c.updatedAt DESC, c.createdAt DESC")
                    .setParameter("q", pattern)
                    .setFirstResult(safePage * safeSize)
                    .setMaxResults(safeSize)
                    .getResultList();
            if (pageIds.isEmpty()) {
                rows = List.of();
            } else {
                Map<Integer, CategoryEntity> byId = CategoryEntity.<CategoryEntity>list("id in ?1", pageIds)
                        .stream()
                        .collect(Collectors.toMap(c -> c.id, c -> c, (a, b) -> a));
                rows = pageIds.stream().map(byId::get).filter(Objects::nonNull).collect(Collectors.toList());
            }
        }

        List<LocalizedCategoryDto> items =
                rows.stream().map(c -> toLocalized(c, language)).collect(Collectors.toList());
        return new PageResult<>(items, safePage, safeSize, total);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-categories")
    public LocalizedCategoryDto create(TaxonomyUpsertRequest req) {
        validate(req);
        LocalDateTime now = LocalDateTime.now();
        CategoryEntity category = new CategoryEntity();
        category.dbDescription = req.dbDescription != null ? req.dbDescription : req.name;
        category.usageCount = 0;
        category.createdAt = now;
        category.updatedAt = now;
        category.persist();

        CategoryI18nEntity i18n = new CategoryI18nEntity();
        i18n.categoryId = category.id;
        i18n.languageCode = normalizeLang(req.languageCode);
        i18n.name = req.name;
        i18n.slug = resolveSlug(req);
        i18n.persist();
        return toLocalized(category, i18n.languageCode);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-categories")
    public LocalizedCategoryDto update(Integer id, TaxonomyUpsertRequest req) {
        CategoryEntity category = CategoryEntity.findById(id);
        if (category == null) {
            throw new NotFoundException("Category not found: " + id);
        }
        if (req.dbDescription != null) {
            category.dbDescription = req.dbDescription;
        }
        category.updatedAt = LocalDateTime.now();
        String lang = normalizeLang(req.languageCode);
        CategoryI18nEntity i18n = CategoryI18nEntity.find("categoryId = ?1 AND languageCode = ?2", id, lang)
                .firstResult();
        if (i18n == null) {
            if (req.name == null || req.name.isBlank()) {
                throw new BadRequestException("name is required when creating a translation");
            }
            i18n = new CategoryI18nEntity();
            i18n.categoryId = id;
            i18n.languageCode = lang;
            i18n.name = req.name;
            i18n.slug = resolveSlug(req);
            i18n.persist();
        } else {
            if (req.name != null) i18n.name = req.name;
            if (req.slug != null) i18n.slug = req.slug;
            else if (req.name != null) i18n.slug = slugify(req.name);
        }
        return toLocalized(category, lang);
    }

    @Transactional
    @CacheInvalidateAll(cacheName = "popular-categories")
    public void delete(Integer id) {
        CategoryEntity category = CategoryEntity.findById(id);
        if (category == null) {
            throw new NotFoundException("Category not found: " + id);
        }
        CategoryI18nEntity.delete("categoryId", id);
        PostCategoryEntity.delete("categoryId", id);
        category.delete();
    }

    @CacheInvalidateAll(cacheName = "popular-categories")
    public void invalidatePopularCache() {
        cacheManager.getCache("popular-categories").ifPresent(cache ->
                cache.invalidateAll().await().indefinitely());
    }

    @Transactional
    public void refreshUsageCounts(Collection<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }
        for (Integer id : categoryIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList())) {
            CategoryEntity category = CategoryEntity.findById(id);
            if (category == null) continue;
            long count = PostCategoryEntity.count("categoryId", id);
            category.usageCount = (int) Math.min(count, Integer.MAX_VALUE);
            category.updatedAt = LocalDateTime.now();
        }
        invalidatePopularCache();
    }

    private LocalizedCategoryDto toLocalized(CategoryEntity category, String lang) {
        CategoryI18nEntity i18n = CategoryI18nEntity
                .find("categoryId = ?1 AND languageCode = ?2", category.id, lang)
                .firstResult();
        if (i18n == null) {
            i18n = CategoryI18nEntity.find("categoryId", category.id).firstResult();
        }
        LocalizedCategoryDto dto = new LocalizedCategoryDto();
        dto.id = category.id;
        dto.dbDescription = category.dbDescription;
        dto.usageCount = category.usageCount;
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

    /** Returns null when the query is too short (&lt; 2 non-space chars). */
    static String normalizeSearchQuery(String q) {
        if (q == null) return null;
        String trimmed = q.trim();
        if (trimmed.replaceAll("\\s+", "").length() < 2) {
            return null;
        }
        return trimmed;
    }

    static int compareUsageThenDates(CategoryEntity a, CategoryEntity b) {
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

    public static List<Integer> parseIds(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<Integer> ids = new ArrayList<>();
        for (String part : csv.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            try {
                ids.add(Integer.parseInt(p));
            } catch (NumberFormatException ignored) {
                // skip invalid
            }
        }
        return ids;
    }
}
