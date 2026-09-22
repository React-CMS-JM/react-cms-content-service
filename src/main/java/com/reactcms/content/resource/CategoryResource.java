package com.reactcms.content.resource;

import com.reactcms.content.dto.LocalizedCategoryDto;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.service.CategoryService;
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

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    /** Top 100 by usage (Caffeine-cached). Used by autocomplete / dropdowns. */
    @GET
    public List<LocalizedCategoryDto> listPopular(@QueryParam("lang") String lang) {
        return categoryService.listPopular(lang);
    }

    @GET
    @Path("/search")
    public List<LocalizedCategoryDto> search(
            @QueryParam("q") String q,
            @QueryParam("lang") String lang,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        return categoryService.search(q, lang, limit);
    }

    @GET
    @Path("/by-ids")
    public List<LocalizedCategoryDto> byIds(
            @QueryParam("ids") String ids,
            @QueryParam("lang") String lang) {
        return categoryService.listByIds(CategoryService.parseIds(ids), lang);
    }

    /** Admin table: paginated, never from popular cache. Default size 10. */
    @GET
    @Path("/admin")
    public PageResult<LocalizedCategoryDto> listAdmin(
            @QueryParam("lang") String lang,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("q") String q) {
        return categoryService.listAdmin(lang, page, size, q);
    }

    @POST
    public Response create(TaxonomyUpsertRequest request) {
        LocalizedCategoryDto created = categoryService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public LocalizedCategoryDto update(@PathParam("id") Integer id, TaxonomyUpsertRequest request) {
        return categoryService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        categoryService.delete(id);
        return Response.noContent().build();
    }
}
