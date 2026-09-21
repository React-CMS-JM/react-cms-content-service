package com.reactcms.content.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/users")
@RegisterRestClient(configKey = "auth-service")
@Produces(MediaType.APPLICATION_JSON)
public interface AuthUserClient {

    @GET
    @Path("/by-ids")
    List<UserSummaryDto> byIds(
            @QueryParam("ids") String ids,
            @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization);
}
