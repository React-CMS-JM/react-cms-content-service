package com.reactcms.content.service;

import com.reactcms.content.dto.AdminDashboardDto;
import com.reactcms.content.dto.AdminRecentActivityDto;
import com.reactcms.content.entity.CommentEntity;
import com.reactcms.content.entity.ContentTypeEntity;
import com.reactcms.content.entity.PostEntity;
import com.reactcms.content.entity.PostI18nEntity;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class DashboardService {

    private static final String DEFAULT_LANG = "en";
    private static final List<String> DASHBOARD_TYPE_SLUGS = List.of("post", "page", "service", "product");
    private static final int DEFAULT_RECENT_LIMIT = 6;
    private static final int MAX_RECENT_LIMIT = 20;

    public AdminDashboardDto getDashboard(String lang, Integer recentLimit) {
        String language = normalizeLang(lang);
        int limit = recentLimit == null
                ? DEFAULT_RECENT_LIMIT
                : Math.min(Math.max(recentLimit, 1), MAX_RECENT_LIMIT);

        AdminDashboardDto dto = new AdminDashboardDto();
        for (String slug : DASHBOARD_TYPE_SLUGS) {
            dto.counts.put(slug, countByTypeSlug(slug));
        }
        dto.pendingComments = CommentEntity.count("status", "pending");
        dto.recent = loadRecent(language, limit);
        return dto;
    }

    private long countByTypeSlug(String slug) {
        ContentTypeEntity type = ContentTypeEntity.find("slug", slug).firstResult();
        if (type == null) {
            return 0L;
        }
        return PostEntity.count("contentTypeId", type.id);
    }

    private List<AdminRecentActivityDto> loadRecent(String language, int limit) {
        // Include courses (same posts table) so Recent Activity is globally ordered by updatedAt.
        List<PostEntity> posts = PostEntity.find("ORDER BY updatedAt DESC").page(0, limit).list();
        return posts.stream().map(post -> toRecentRow(post, language)).toList();
    }

    private AdminRecentActivityDto toRecentRow(PostEntity post, String language) {
        AdminRecentActivityDto row = new AdminRecentActivityDto();
        row.id = post.id;
        row.authorId = post.authorId;
        row.status = post.status;
        row.updatedAt = post.updatedAt;

        ContentTypeEntity type = ContentTypeEntity.findById(post.contentTypeId);
        row.contentTypeSlug = type != null ? type.slug : "post";

        PostI18nEntity i18n = PostI18nEntity.find("postId = ?1 AND languageCode = ?2", post.id, language)
                .firstResult();
        if (i18n == null) {
            i18n = PostI18nEntity.find("postId", post.id).firstResult();
        }
        row.title = i18n != null && i18n.title != null ? i18n.title : "";
        return row;
    }

    private static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT_LANG;
        }
        return lang.trim().toLowerCase(Locale.ROOT);
    }
}
