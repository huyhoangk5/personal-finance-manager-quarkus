package com.finance.pfm.resource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.util.Map;

@Path("/api/health")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "timestamp", LocalDateTime.now().toString(),
                "service", "personal-finance-manager-quarkus"
        );
    }
}
