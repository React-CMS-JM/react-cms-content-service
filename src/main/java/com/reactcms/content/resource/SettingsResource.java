package com.reactcms.content.resource;

import com.reactcms.content.dto.SiteSettingsDto;
import com.reactcms.content.dto.SiteSettingsDto.HomeHeroDto;
import com.reactcms.content.dto.SiteSettingsDto.VisibilityOrderItemDto;
import com.reactcms.content.service.SettingsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SettingsResource {

    @Inject
    SettingsService settingsService;

    @GET
    public SiteSettingsDto get(@QueryParam("lang") String lang) {
        return settingsService.get(lang);
    }

    @PUT
    public SiteSettingsDto put(SiteSettingsDto body, @QueryParam("lang") String lang) {
        return settingsService.put(body, lang);
    }

    @GET
    @Path("/home-hero")
    public HomeHeroDto getHomeHero() {
        return settingsService.getHomeHero();
    }

    @PUT
    @Path("/home-hero")
    public HomeHeroDto putHomeHero(HomeHeroDto body) {
        return settingsService.putHomeHero(body);
    }

    @GET
    @Path("/home-sections")
    public List<VisibilityOrderItemDto> getHomeSections() {
        return settingsService.getHomeSections();
    }

    @PUT
    @Path("/home-sections")
    public List<VisibilityOrderItemDto> putHomeSections(List<VisibilityOrderItemDto> body) {
        return settingsService.putHomeSections(body);
    }

    @GET
    @Path("/main-menu")
    public List<VisibilityOrderItemDto> getMainMenu() {
        return settingsService.getMainMenu();
    }

    @PUT
    @Path("/main-menu")
    public List<VisibilityOrderItemDto> putMainMenu(List<VisibilityOrderItemDto> body) {
        return settingsService.putMainMenu(body);
    }
}
