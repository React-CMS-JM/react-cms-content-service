package com.reactcms.content.resource;

import com.reactcms.content.dto.CreatePostRequest;
import com.reactcms.content.dto.LocalizedPostDto;
import com.reactcms.content.dto.MetadataPutRequest;
import com.reactcms.content.dto.PageResult;
import com.reactcms.content.dto.PostMetadataDto;
import com.reactcms.content.dto.StatusRequest;
import com.reactcms.content.dto.UpdatePostRequest;
import com.reactcms.content.service.PostService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PostResource {

    @Inject
    PostService postService;

    @Inject
    JsonWebToken jwt;

    @GET
    public PageResult<LocalizedPostDto> list(
            @QueryParam("type") String type,
            @QueryParam("status") String status,
            @QueryParam("lang") String lang,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return postService.list(type, status, lang, page, size);
    }

    @GET
    @Path("/{id}")
    public LocalizedPostDto getById(@PathParam("id") String id, @QueryParam("lang") String lang) {
        return postService.getById(id, lang);
    }

    @GET
    @Path("/by-slug/{slug}")
    public LocalizedPostDto getBySlug(
            @PathParam("slug") String slug,
            @QueryParam("type") String type,
            @QueryParam("lang") String lang) {
        return postService.getBySlug(slug, type, lang);
    }

    @POST
    public Response create(CreatePostRequest request) {
        LocalizedPostDto created = postService.create(request, subjectOrNull());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public LocalizedPostDto update(@PathParam("id") String id, UpdatePostRequest request) {
        return postService.update(id, request);
    }

    @PATCH
    @Path("/{id}/status")
    public LocalizedPostDto patchStatus(@PathParam("id") String id, StatusRequest request) {
        return postService.patchStatus(id, request != null ? request.status : null);
    }

    @POST
    @Path("/{id}/view")
    public LocalizedPostDto incrementView(@PathParam("id") String id) {
        return postService.incrementView(id);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        postService.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/metadata")
    public List<PostMetadataDto> getMetadata(@PathParam("id") String id) {
        return postService.getMetadata(id);
    }

    @PUT
    @Path("/{id}/metadata")
    public List<PostMetadataDto> putMetadata(@PathParam("id") String id, MetadataPutRequest request) {
        return postService.putMetadata(id, request);
    }

    private String subjectOrNull() {
        try {
            String sub = jwt.getSubject();
            return sub != null && !sub.isBlank() ? sub : null;
        } catch (Exception e) {
            return null;
        }
    }
}
