package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.NotFoundException;
import com.reactcms.content.dto.UiStringBulkPatchRequest;
import com.reactcms.content.dto.UiStringDto;
import com.reactcms.content.dto.UiStringUpsertRequest;
import com.reactcms.content.entity.ParamUiStringEntity;
import com.reactcms.content.entity.ParamUiStringI18nEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class UiStringService {

    /** Loaded only for staff admin shell; excluded from default public list. */
    public static final String ADMIN_SIDEBAR_COMPONENT = "AdminSidebar";

    public List<UiStringDto> list(String lang, String component) {
        String language = normalizeLang(lang);
        List<ParamUiStringEntity> keys;
        if (component != null && !component.isBlank()) {
            keys = ParamUiStringEntity.list("uiComponent", component.trim());
        } else {
            // Public default: every scope except admin sidebar labels.
            keys = ParamUiStringEntity.list("uiComponent <> ?1", ADMIN_SIDEBAR_COMPONENT);
        }

        List<UiStringDto> result = new ArrayList<>();
        for (ParamUiStringEntity key : keys) {
            ParamUiStringI18nEntity i18n = ParamUiStringI18nEntity
                    .find("stringKey = ?1 AND languageCode = ?2", key.stringKey, language)
                    .firstResult();
            if (i18n == null) {
                continue;
            }
            result.add(toDto(key, i18n));
        }
        return result;
    }

    @Transactional
    public UiStringDto upsert(String stringKey, UiStringUpsertRequest req) {
        if (req == null || req.languageCode == null || req.languageCode.isBlank()) {
            throw new BadRequestException("languageCode is required");
        }
        if (req.stringValue == null) {
            throw new BadRequestException("stringValue is required");
        }
        ParamUiStringEntity key = ParamUiStringEntity.findById(stringKey);
        if (key == null) {
            throw new NotFoundException("UI string key not found: " + stringKey);
        }
        String lang = normalizeLang(req.languageCode);
        ParamUiStringI18nEntity i18n = ParamUiStringI18nEntity
                .find("stringKey = ?1 AND languageCode = ?2", stringKey, lang)
                .firstResult();
        if (i18n == null) {
            i18n = new ParamUiStringI18nEntity();
            i18n.stringKey = stringKey;
            i18n.languageCode = lang;
            i18n.persist();
        }
        i18n.stringValue = req.stringValue;
        return toDto(key, i18n);
    }

    @Transactional
    public List<UiStringDto> bulkPatch(UiStringBulkPatchRequest request) {
        if (request == null || request.items == null) {
            throw new BadRequestException("items are required");
        }
        List<UiStringDto> updated = new ArrayList<>();
        for (UiStringBulkPatchRequest.Item item : request.items) {
            UiStringUpsertRequest req = new UiStringUpsertRequest();
            req.languageCode = item.languageCode;
            req.stringValue = item.stringValue;
            updated.add(upsert(item.stringKey, req));
        }
        return updated;
    }

    private UiStringDto toDto(ParamUiStringEntity key, ParamUiStringI18nEntity i18n) {
        UiStringDto dto = new UiStringDto();
        dto.stringKey = key.stringKey;
        dto.uiComponent = key.uiComponent;
        dto.languageCode = i18n.languageCode;
        dto.stringValue = i18n.stringValue;
        dto.updatedAt = i18n.updatedAt;
        return dto;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) return "en";
        return lang.trim().toLowerCase(Locale.ROOT);
    }
}
