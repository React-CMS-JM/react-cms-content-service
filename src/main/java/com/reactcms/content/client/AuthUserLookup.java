package com.reactcms.content.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthUserLookup {

    private static final Logger LOG = Logger.getLogger(AuthUserLookup.class);

    @Inject
    @RestClient
    AuthUserClient authUserClient;

    /**
     * Resolves author summaries from auth-service. Returns an empty map if the call fails
     * so dashboard counts/recent still succeed without author names.
     */
    public Map<String, UserSummaryDto> findByIds(Collection<String> ids, String authorizationHeader) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> unique = ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (unique.isEmpty()) {
            return Map.of();
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Map.of();
        }
        try {
            List<UserSummaryDto> rows = authUserClient.byIds(String.join(",", unique), authorizationHeader);
            if (rows == null || rows.isEmpty()) {
                return Map.of();
            }
            return rows.stream()
                    .filter(u -> u != null && u.id != null)
                    .collect(Collectors.toMap(u -> u.id, u -> u, (a, b) -> a));
        } catch (Exception e) {
            LOG.warnf(e, "Failed to resolve users from auth-service for %d id(s)", unique.size());
            return Collections.emptyMap();
        }
    }

    public static String displayName(UserSummaryDto user) {
        if (user == null) {
            return "";
        }
        String first = user.firstName != null ? user.firstName.trim() : "";
        String last = user.lastName != null ? user.lastName.trim() : "";
        return (first + " " + last).trim();
    }
}
