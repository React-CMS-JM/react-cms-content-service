package com.reactcms.content.service;

import com.reactcms.content.common.BadRequestException;
import com.reactcms.content.common.Ids;
import com.reactcms.content.dto.SiteSettingsDto;
import com.reactcms.content.dto.SiteSettingsDto.HeroCtaDto;
import com.reactcms.content.dto.SiteSettingsDto.HomeHeroDto;
import com.reactcms.content.dto.SiteSettingsDto.VisibilityOrderItemDto;
import com.reactcms.content.entity.HeroCtaEntity;
import com.reactcms.content.entity.HeroCtaI18nEntity;
import com.reactcms.content.entity.HomeSectionEntity;
import com.reactcms.content.entity.NavItemEntity;
import com.reactcms.content.entity.SiteSettingEntity;
import com.reactcms.content.entity.SiteSettingI18nEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class SettingsService {

    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_SITE_ICON = "site_icon_url";
    private static final String KEY_POSTS_PER_PAGE = "posts_per_page";
    private static final String KEY_HERO_TITLE_VISIBLE = "hero_title_visible";
    private static final String KEY_HERO_SUBTITLE_VISIBLE = "hero_subtitle_visible";
    private static final String KEY_SITE_SEO = "site_seo";

    public SiteSettingsDto get(String lang) {
        String language = normalizeLang(lang);
        SiteSettingsDto dto = new SiteSettingsDto();
        dto.siteName = getSetting(KEY_SITE_NAME, "React CMS");
        dto.siteIconUrl = getSetting(KEY_SITE_ICON, "");
        dto.siteDescription = getSettingI18n(KEY_SITE_SEO, language, "");
        dto.postsPerPage = parseInt(getSetting(KEY_POSTS_PER_PAGE, "9"), 9);
        dto.homeHero = getHomeHero();
        dto.homeSections = getHomeSections();
        dto.mainMenu = getMainMenu();
        return dto;
    }

    @Transactional
    public SiteSettingsDto put(SiteSettingsDto body, String lang) {
        if (body == null) {
            throw new BadRequestException("settings body is required");
        }
        String language = normalizeLang(lang);
        upsertSetting(KEY_SITE_NAME, body.siteName);
        upsertSetting(KEY_SITE_ICON, body.siteIconUrl != null ? body.siteIconUrl : "");
        upsertSetting(KEY_POSTS_PER_PAGE, String.valueOf(body.postsPerPage > 0 ? body.postsPerPage : 9));
        if (body.siteDescription != null) {
            upsertSettingI18n(KEY_SITE_SEO, language, body.siteDescription);
        }
        if (body.homeHero != null) {
            putHomeHero(body.homeHero);
        }
        if (body.homeSections != null) {
            putHomeSections(body.homeSections);
        }
        if (body.mainMenu != null) {
            putMainMenu(body.mainMenu);
        }
        return get(language);
    }

    public HomeHeroDto getHomeHero() {
        HomeHeroDto hero = new HomeHeroDto();
        hero.titleVisible = Boolean.parseBoolean(getSetting(KEY_HERO_TITLE_VISIBLE, "true"));
        hero.subtitleVisible = Boolean.parseBoolean(getSetting(KEY_HERO_SUBTITLE_VISIBLE, "true"));
        List<HeroCtaEntity> ctas = HeroCtaEntity.listAll();
        ctas.sort(Comparator.comparing(c -> c.sortOrder == null ? 0 : c.sortOrder));
        hero.ctas = ctas.stream().map(this::toHeroCtaDto).collect(Collectors.toList());
        return hero;
    }

    @Transactional
    public HomeHeroDto putHomeHero(HomeHeroDto hero) {
        if (hero == null) {
            throw new BadRequestException("homeHero body is required");
        }
        upsertSetting(KEY_HERO_TITLE_VISIBLE, String.valueOf(hero.titleVisible));
        upsertSetting(KEY_HERO_SUBTITLE_VISIBLE, String.valueOf(hero.subtitleVisible));

        List<HeroCtaEntity> existing = HeroCtaEntity.listAll();
        for (HeroCtaEntity cta : existing) {
            HeroCtaI18nEntity.delete("heroCtaId", cta.id);
            cta.delete();
        }

        if (hero.ctas != null) {
            int order = 0;
            for (HeroCtaDto ctaDto : hero.ctas) {
                HeroCtaEntity entity = new HeroCtaEntity();
                entity.id = (ctaDto.id != null && !ctaDto.id.isBlank()) ? ctaDto.id : Ids.uuid();
                entity.href = ctaDto.href != null ? ctaDto.href : "/";
                entity.textColor = ctaDto.textColor != null ? ctaDto.textColor : "#ffffff";
                entity.backgroundColor = ctaDto.backgroundColor != null ? ctaDto.backgroundColor : "#6366f1";
                entity.isVisible = ctaDto.visible;
                entity.sortOrder = order++;
                entity.persist();

                if (ctaDto.labels != null) {
                    for (Map.Entry<String, String> entry : ctaDto.labels.entrySet()) {
                        if (entry.getKey() == null || entry.getValue() == null) continue;
                        HeroCtaI18nEntity i18n = new HeroCtaI18nEntity();
                        i18n.heroCtaId = entity.id;
                        i18n.languageCode = entry.getKey().toLowerCase(Locale.ROOT);
                        i18n.label = entry.getValue();
                        i18n.persist();
                    }
                }
            }
        }
        return getHomeHero();
    }

    public List<VisibilityOrderItemDto> getHomeSections() {
        List<HomeSectionEntity> sections = HomeSectionEntity.listAll();
        sections.sort(Comparator.comparing(s -> s.sortOrder == null ? 0 : s.sortOrder));
        return sections.stream()
                .map(s -> new VisibilityOrderItemDto(s.sectionKey, Boolean.TRUE.equals(s.isVisible)))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<VisibilityOrderItemDto> putHomeSections(List<VisibilityOrderItemDto> items) {
        if (items == null) {
            throw new BadRequestException("homeSections body is required");
        }
        int order = 0;
        for (VisibilityOrderItemDto item : items) {
            if (item.id == null || item.id.isBlank()) continue;
            HomeSectionEntity section = HomeSectionEntity.find("sectionKey", item.id).firstResult();
            if (section == null) {
                section = new HomeSectionEntity();
                section.sectionKey = item.id;
                section.persist();
            }
            section.isVisible = item.visible;
            section.sortOrder = order++;
        }
        return getHomeSections();
    }

    public List<VisibilityOrderItemDto> getMainMenu() {
        List<NavItemEntity> items = NavItemEntity.listAll();
        items.sort(Comparator.comparing(n -> n.sortOrder == null ? 0 : n.sortOrder));
        return items.stream()
                .map(n -> new VisibilityOrderItemDto(n.itemKey, Boolean.TRUE.equals(n.isVisible)))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<VisibilityOrderItemDto> putMainMenu(List<VisibilityOrderItemDto> items) {
        if (items == null) {
            throw new BadRequestException("mainMenu body is required");
        }
        int order = 0;
        for (VisibilityOrderItemDto item : items) {
            if (item.id == null || item.id.isBlank()) continue;
            NavItemEntity nav = NavItemEntity.find("itemKey", item.id).firstResult();
            if (nav == null) {
                nav = new NavItemEntity();
                nav.itemKey = item.id;
                nav.persist();
            }
            nav.isVisible = item.visible;
            nav.sortOrder = order++;
        }
        return getMainMenu();
    }

    private HeroCtaDto toHeroCtaDto(HeroCtaEntity entity) {
        HeroCtaDto dto = new HeroCtaDto();
        dto.id = entity.id;
        dto.visible = Boolean.TRUE.equals(entity.isVisible);
        dto.href = entity.href;
        dto.textColor = entity.textColor;
        dto.backgroundColor = entity.backgroundColor;
        Map<String, String> labels = new HashMap<>();
        List<HeroCtaI18nEntity> i18nList = HeroCtaI18nEntity.list("heroCtaId", entity.id);
        for (HeroCtaI18nEntity i18n : i18nList) {
            labels.put(i18n.languageCode, i18n.label);
        }
        dto.labels = labels;
        return dto;
    }

    private String getSetting(String key, String defaultValue) {
        SiteSettingEntity entity = SiteSettingEntity.findById(key);
        if (entity == null || entity.settingValue == null) {
            return defaultValue;
        }
        return entity.settingValue;
    }

    private String getSettingI18n(String key, String lang, String defaultValue) {
        SiteSettingI18nEntity entity = SiteSettingI18nEntity
                .find("settingKey = ?1 AND languageCode = ?2", key, lang)
                .firstResult();
        if (entity == null) {
            entity = SiteSettingI18nEntity.find("settingKey", key).firstResult();
        }
        return entity != null ? entity.settingValue : defaultValue;
    }

    private void upsertSetting(String key, String value) {
        SiteSettingEntity entity = SiteSettingEntity.findById(key);
        if (entity == null) {
            entity = new SiteSettingEntity();
            entity.settingKey = key;
            entity.persist();
        }
        entity.settingValue = value;
    }

    private void upsertSettingI18n(String key, String lang, String value) {
        SiteSettingEntity parent = SiteSettingEntity.findById(key);
        if (parent == null) {
            parent = new SiteSettingEntity();
            parent.settingKey = key;
            parent.settingValue = null;
            parent.persist();
        }
        SiteSettingI18nEntity i18n = SiteSettingI18nEntity
                .find("settingKey = ?1 AND languageCode = ?2", key, lang)
                .firstResult();
        if (i18n == null) {
            i18n = new SiteSettingI18nEntity();
            i18n.settingKey = key;
            i18n.languageCode = lang;
            i18n.persist();
        }
        i18n.settingValue = value;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) return "en";
        return lang.trim().toLowerCase(Locale.ROOT);
    }
}
