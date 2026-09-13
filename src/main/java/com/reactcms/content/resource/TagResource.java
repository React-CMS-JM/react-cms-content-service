package com.reactcms.content.resource;

import com.reactcms.content.dto.LocalizedTagDto;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.service.TagService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/tags")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TagResource {

    @Inject
    TagService tagService;

    @GET
    public List<LocalizedTagDto> list(@QueryParam("lang") String lang) {
        return tagService.list(lang);
    }

    @POST
    public Response create(TaxonomyUpsertRequest request) {
        LocalizedTagDto created = tagService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public LocalizedTagDto update(@PathParam("id") Integer id, TaxonomyUpsertRequest request) {
        return tagService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        tagService.delete(id);
        return Response.noContent().build();
    }
}
