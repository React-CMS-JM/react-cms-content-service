package com.reactcms.content.common;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

public final class ExceptionMappers {

    private ExceptionMappers() {
    }

    @Provider
    public static class NotFoundMapper implements ExceptionMapper<NotFoundException> {
        @Override
        public Response toResponse(NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @Provider
    public static class BadRequestMapper implements ExceptionMapper<BadRequestException> {
        @Override
        public Response toResponse(BadRequestException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
