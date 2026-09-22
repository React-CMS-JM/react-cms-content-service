package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.LocalizedCategoryDto;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.entity.CategoryEntity;
import com.reactcms.content.entity.CategoryI18nEntity;
import com.reactcms.content.entity.PostCategoryEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CategoryService {

    public List<LocalizedCategoryDto> list(String lang) {
        String language = normalizeLang(lang);
        Map<Integer, Long> usageById = loadUsageCounts();
        return CategoryEntity.<CategoryEntity>listAll().stream()
                .map(c -> toLocalized(c, language, usageById.getOrDefault(c.id, 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public LocalizedCategoryDto create(TaxonomyUpsertRequest req) {
        validate(req);
        CategoryEntity category = new CategoryEntity();
        category.dbDescription = req.dbDescription != null ? req.dbDescription : req.name;
        category.persist();

        CategoryI18nEntity i18n = new CategoryI18nEntity();
        i18n.categoryId = category.id;
        i18n.languageCode = normalizeLang(req.languageCode);
        i18n.name = req.name;
        i18n.slug = resolveSlug(req);
        i18n.persist();
        return toLocalized(category, i18n.languageCode, 0L);
    }

    @Transactional
    public LocalizedCategoryDto update(Integer id, TaxonomyUpsertRequest req) {
        CategoryEntity category = CategoryEntity.findById(id);
        if (category == null) {
            throw new NotFoundException("Category not found: " + id);
        }
        if (req.dbDescription != null) {
            category.dbDescription = req.dbDescription;
        }
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
        return toLocalized(category, lang, PostCategoryEntity.count("categoryId", id));
    }

    @Transactional
    public void delete(Integer id) {
        CategoryEntity category = CategoryEntity.findById(id);
        if (category == null) {
            throw new NotFoundException("Category not found: " + id);
        }
        CategoryI18nEntity.delete("categoryId", id);
        PostCategoryEntity.delete("categoryId", id);
        category.delete();
    }

    private Map<Integer, Long> loadUsageCounts() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = PostCategoryEntity.getEntityManager()
                .createQuery(
                        "SELECT pc.categoryId, COUNT(pc) FROM PostCategoryEntity pc GROUP BY pc.categoryId")
                .getResultList();
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                counts.put((Integer) row[0], (Long) row[1]);
            }
        }
        return counts;
    }

    private LocalizedCategoryDto toLocalized(CategoryEntity category, String lang, long usageCount) {
        CategoryI18nEntity i18n = CategoryI18nEntity
                .find("categoryId = ?1 AND languageCode = ?2", category.id, lang)
                .firstResult();
        if (i18n == null) {
            i18n = CategoryI18nEntity.find("categoryId", category.id).firstResult();
        }
        LocalizedCategoryDto dto = new LocalizedCategoryDto();
        dto.id = category.id;
        dto.dbDescription = category.dbDescription;
        dto.usageCount = usageCount;
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
