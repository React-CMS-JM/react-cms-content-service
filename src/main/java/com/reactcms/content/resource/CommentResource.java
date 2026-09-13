package com.reactcms.content.resource;

import com.reactcms.content.dto.CommentDto;
import com.reactcms.content.dto.CommentTranslationDto;
import com.reactcms.content.dto.CreateCommentRequest;
import com.reactcms.content.dto.StatusRequest;
import com.reactcms.content.dto.UpdateCommentRequest;
import com.reactcms.content.service.CommentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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
import java.util.Map;

@Path("/api/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    @Inject
    CommentService commentService;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<CommentDto> list(
            @QueryParam("postId") String postId,
            @QueryParam("status") String status) {
        return commentService.list(postId, status);
    }

    @POST
    public Response create(CreateCommentRequest request) {
        CommentDto created = commentService.create(request, subjectOrNull());
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public CommentDto update(@PathParam("id") String id, UpdateCommentRequest request) {
        return commentService.updateContent(id, request);
    }

    @GET
    @Path("/{id}/translations/{lang}")
    public CommentTranslationDto getTranslation(@PathParam("id") String id, @PathParam("lang") String lang) {
        return commentService.getTranslation(id, lang);
    }

    /**
     * Stores a translation already produced elsewhere (e.g. future Translation API).
     * Body: { "content": "..." }
     */
    @PUT
    @Path("/{id}/translations/{lang}")
    public CommentTranslationDto putTranslation(
            @PathParam("id") String id,
            @PathParam("lang") String lang,
            Map<String, String> body) {
        String content = body != null ? body.get("content") : null;
        return commentService.upsertTranslation(id, lang, content);
    }

    @PATCH
    @Path("/{id}/status")
    public CommentDto patchStatus(@PathParam("id") String id, StatusRequest request) {
        return commentService.patchStatus(id, request != null ? request.status : null);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        commentService.delete(id);
        return Response.noContent().build();
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
