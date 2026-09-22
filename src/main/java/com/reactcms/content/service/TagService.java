package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.LocalizedTagDto;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.entity.PostTagEntity;
import com.reactcms.content.entity.TagEntity;
import com.reactcms.content.entity.TagI18nEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class TagService {

    public List<LocalizedTagDto> list(String lang) {
        String language = normalizeLang(lang);
        Map<Integer, Long> usageById = loadUsageCounts();
        return TagEntity.<TagEntity>listAll().stream()
                .map(t -> toLocalized(t, language, usageById.getOrDefault(t.id, 0L)))
                .collect(Collectors.toList());
    }

    @Transactional
    public LocalizedTagDto create(TaxonomyUpsertRequest req) {
        validate(req);
        TagEntity tag = new TagEntity();
        tag.dbDescription = req.dbDescription != null ? req.dbDescription : req.name;
        tag.persist();

        TagI18nEntity i18n = new TagI18nEntity();
        i18n.tagId = tag.id;
        i18n.languageCode = normalizeLang(req.languageCode);
        i18n.name = req.name;
        i18n.slug = resolveSlug(req);
        i18n.persist();
        return toLocalized(tag, i18n.languageCode, 0L);
    }

    @Transactional
    public LocalizedTagDto update(Integer id, TaxonomyUpsertRequest req) {
        TagEntity tag = TagEntity.findById(id);
        if (tag == null) {
            throw new NotFoundException("Tag not found: " + id);
        }
        if (req.dbDescription != null) {
            tag.dbDescription = req.dbDescription;
        }
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
        return toLocalized(tag, lang, PostTagEntity.count("tagId", id));
    }

    @Transactional
    public void delete(Integer id) {
        TagEntity tag = TagEntity.findById(id);
        if (tag == null) {
            throw new NotFoundException("Tag not found: " + id);
        }
        TagI18nEntity.delete("tagId", id);
        PostTagEntity.delete("tagId", id);
        tag.delete();
    }

    private Map<Integer, Long> loadUsageCounts() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = PostTagEntity.getEntityManager()
                .createQuery("SELECT pt.tagId, COUNT(pt) FROM PostTagEntity pt GROUP BY pt.tagId")
                .getResultList();
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                counts.put((Integer) row[0], (Long) row[1]);
            }
        }
        return counts;
    }

    private LocalizedTagDto toLocalized(TagEntity tag, String lang, long usageCount) {
        TagI18nEntity i18n = TagI18nEntity.find("tagId = ?1 AND languageCode = ?2", tag.id, lang).firstResult();
        if (i18n == null) {
            i18n = TagI18nEntity.find("tagId", tag.id).firstResult();
        }
        LocalizedTagDto dto = new LocalizedTagDto();
        dto.id = tag.id;
        dto.dbDescription = tag.dbDescription;
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
