package com.reactcms.content.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteSettingsDto {
    public String siteName;
    public String siteIconUrl;
    public String siteDescription;
    public int postsPerPage = 9;
    public HomeHeroDto homeHero = new HomeHeroDto();
    public List<VisibilityOrderItemDto> homeSections = new ArrayList<>();
    public List<VisibilityOrderItemDto> mainMenu = new ArrayList<>();

    public static class HomeHeroDto {
        public boolean titleVisible = true;
        public boolean subtitleVisible = true;
        public List<HeroCtaDto> ctas = new ArrayList<>();
    }

    public static class HeroCtaDto {
        public String id;
        public boolean visible = true;
        public String href;
        public String textColor;
        public String backgroundColor;
        public Map<String, String> labels = new HashMap<>();
    }

    public static class VisibilityOrderItemDto {
        public String id;
        public boolean visible = true;

        public VisibilityOrderItemDto() {
        }

        public VisibilityOrderItemDto(String id, boolean visible) {
            this.id = id;
            this.visible = visible;
        }
    }
}
