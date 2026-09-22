package com.reactcms.content.resource;

import com.reactcms.content.dto.LocalizedTagDto;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.service.CategoryService;
import com.reactcms.content.service.TagService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

    /** Top 100 by usage (Caffeine-cached). Used by autocomplete / dropdowns. */
    @GET
    public List<LocalizedTagDto> listPopular(@QueryParam("lang") String lang) {
        return tagService.listPopular(lang);
    }

    @GET
    @Path("/search")
    public List<LocalizedTagDto> search(
            @QueryParam("q") String q,
            @QueryParam("lang") String lang,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return tagService.search(q, lang, limit);
    }

    @GET
    @Path("/by-ids")
    public List<LocalizedTagDto> byIds(
            @QueryParam("ids") String ids,
            @QueryParam("lang") String lang) {
        return tagService.listByIds(CategoryService.parseIds(ids), lang);
    }

    /** Admin table: paginated, never from popular cache. Default size 10. */
    @GET
    @Path("/admin")
    public PageResult<LocalizedTagDto> listAdmin(
            @QueryParam("lang") String lang,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("q") String q) {
        return tagService.listAdmin(lang, page, size, q);
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
