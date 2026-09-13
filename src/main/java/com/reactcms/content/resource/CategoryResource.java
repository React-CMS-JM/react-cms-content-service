package com.reactcms.content.resource;

import com.reactcms.content.dto.LocalizedCategoryDto;
import com.reactcms.content.dto.TaxonomyUpsertRequest;
import com.reactcms.content.service.CategoryService;
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

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @GET
    public List<LocalizedCategoryDto> list(@QueryParam("lang") String lang) {
        return categoryService.list(lang);
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
