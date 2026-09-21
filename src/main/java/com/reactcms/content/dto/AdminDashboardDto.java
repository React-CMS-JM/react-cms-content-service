package com.reactcms.content.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Slim admin dashboard payload: type counts, pending comments, recent activity. */
public class AdminDashboardDto {
    /** Counts keyed by content-type slug (post, page, service, product). */
    public Map<String, Long> counts = new LinkedHashMap<>();
    public long pendingComments;
    public List<AdminRecentActivityDto> recent = new ArrayList<>();
}
