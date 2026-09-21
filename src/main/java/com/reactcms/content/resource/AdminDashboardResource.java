package com.reactcms.content.resource;

import com.reactcms.content.dto.AdminDashboardDto;
import com.reactcms.content.service.DashboardService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

@Path("/api/admin/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminDashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @Authenticated
    public AdminDashboardDto get(
            @QueryParam("lang") String lang,
            @QueryParam("recentLimit") @DefaultValue("6") int recentLimit,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        return dashboardService.getDashboard(lang, recentLimit, authorization);
    }
}
