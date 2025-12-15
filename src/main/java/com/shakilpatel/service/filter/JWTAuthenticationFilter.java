package com.shakilpatel.service.filter;

import com.shakilpatel.service.util.JWTParser;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JWTAuthenticationFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        String method = requestContext.getMethod();

        LOGGER.info("========================================");
        LOGGER.info("JWT Authentication Filter");
        LOGGER.info("========================================");
        LOGGER.info("Path: " + path);
        LOGGER.info("Method: " + method);

        // Skip OPTIONS requests (CORS preflight)
        if ("OPTIONS".equals(method)) {
            LOGGER.info("Skipping OPTIONS request");
            return;
        }

        // Extract Authorization header
        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader == null || authHeader.trim().isEmpty()) {
            LOGGER.warning("❌ Missing Authorization header");
            abortWithUnauthorized(requestContext, "Missing Authorization header");
            return;
        }

        if (!authHeader.startsWith("Bearer ")) {
            LOGGER.warning("❌ Invalid Authorization header format");
            abortWithUnauthorized(requestContext, "Invalid Authorization header format. Expected: Bearer <token>");
            return;
        }

        // Extract token
        String token = authHeader.substring(7).trim();
        LOGGER.info("Token extracted (length: " + token.length() + ")");

        // Parse and validate token
        Claims claims = JWTParser.parseToken(token);

        if (claims == null) {
            LOGGER.severe("❌ Failed to parse/validate JWT token");
            abortWithUnauthorized(requestContext, "Invalid or expired token");
            return;
        }

        // Extract user information
        Long userId = JWTParser.getUserId(claims);
        Long orgId = JWTParser.getOrgId(claims);
        String email = JWTParser.getEmail(claims);
        String name = JWTParser.getName(claims);
        List<String> roles = JWTParser.getRoles(claims);

        LOGGER.info("✅ JWT Validated Successfully!");
        LOGGER.info("User ID: " + userId);
        LOGGER.info("Org ID: " + orgId);
        LOGGER.info("Email: " + email);
        LOGGER.info("Name: " + name);
        LOGGER.info("Roles: " + roles);

        // Check if user has required roles
        if (roles == null || roles.isEmpty()) {
            LOGGER.warning("❌ No roles found in token");
            abortWithForbidden(requestContext, "No roles assigned to user");
            return;
        }

        boolean hasValidRole = JWTParser.hasAnyRole(claims,
                "PLATFORM_ADMIN", "ORG_OWNER", "ORG_ADMIN");

        if (!hasValidRole) {
            LOGGER.warning("❌ User does not have required role");
            LOGGER.warning("User roles: " + roles);
            LOGGER.warning("Required roles: PLATFORM_ADMIN, ORG_OWNER, or ORG_ADMIN");
            abortWithForbidden(requestContext, "Insufficient permissions. Required role: PLATFORM_ADMIN, ORG_OWNER, or ORG_ADMIN");
            return;
        }

        // Inject user information as headers for downstream processing
        requestContext.getHeaders().putSingle("X-User-Id", userId.toString());
        if (orgId != null) {
            requestContext.getHeaders().putSingle("X-Org-Id", orgId.toString());
        }
        requestContext.getHeaders().putSingle("X-Email", email);
        if (name != null) {
            requestContext.getHeaders().putSingle("X-Name", name);
        }
        requestContext.getHeaders().putSingle("X-Roles", String.join(",", roles));

        // Store claims in request property for later use
        requestContext.setProperty("jwt.claims", claims);

        LOGGER.info("✅ Authentication successful! User authorized.");
        LOGGER.info("========================================");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        LOGGER.warning("Aborting request with 401 Unauthorized: " + message);
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"" + message + "\", \"status\": 401}")
                        .build()
        );
    }

    private void abortWithForbidden(ContainerRequestContext requestContext, String message) {
        LOGGER.warning("Aborting request with 403 Forbidden: " + message);
        requestContext.abortWith(
                Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\": \"" + message + "\", \"status\": 403}")
                        .build()
        );
    }
}
