package com.reactcms.content.resource;

import com.reactcms.content.dto.UiStringBulkPatchRequest;
import com.reactcms.content.dto.UiStringDto;
import com.reactcms.content.dto.UiStringUpsertRequest;
import com.reactcms.content.service.UiStringService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/ui-strings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UiStringResource {

    @Inject
    UiStringService uiStringService;

    @GET
    public List<UiStringDto> list(
            @QueryParam("lang") String lang,
            @QueryParam("component") String component) {
        return uiStringService.list(lang, component);
    }

    @PUT
    @Path("/{stringKey}")
    public UiStringDto upsert(@PathParam("stringKey") String stringKey, UiStringUpsertRequest request) {
        return uiStringService.upsert(stringKey, request);
    }

    @PATCH
    public List<UiStringDto> bulkPatch(UiStringBulkPatchRequest request) {
        return uiStringService.bulkPatch(request);
    }
}
