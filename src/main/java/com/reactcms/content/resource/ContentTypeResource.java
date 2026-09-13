package com.reactcms.content.resource;

import com.reactcms.content.dto.ContentTypeDto;
import com.reactcms.content.service.ContentTypeService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/content-types")
@Produces(MediaType.APPLICATION_JSON)
public class ContentTypeResource {

    @Inject
    ContentTypeService contentTypeService;

    @GET
    public List<ContentTypeDto> list() {
        return contentTypeService.list();
    }
}
