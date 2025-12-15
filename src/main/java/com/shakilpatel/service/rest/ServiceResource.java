package com.shakilpatel.service.rest;

import com.shakilpatel.service.dto.*;
import com.shakilpatel.service.ejb.ServiceBean;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.logging.Logger;

@Path("/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServiceResource {

    private static final Logger LOGGER = Logger.getLogger(ServiceResource.class.getName());

    @Inject
    private ServiceBean serviceBean;

    @Context
    private HttpServletRequest httpRequest;

    // ========================================
    // CREATE SERVICE
    // ========================================
    @POST
    public Response createService(CreateServiceRequest request,
                                  @HeaderParam("X-User-Id") Long userId,
                                  @HeaderParam("X-Org-Id") Long orgId,
                                  @HeaderParam("X-Email") String email,
                                  @HeaderParam("X-Roles") String roles) {
        try {
            LOGGER.info("========================================");
            LOGGER.info("CREATE SERVICE REQUEST");
            LOGGER.info("========================================");
            LOGGER.info("User ID: " + userId);
            LOGGER.info("Org ID: " + orgId);
            LOGGER.info("Email: " + email);
            LOGGER.info("Roles: " + roles);
            LOGGER.info("Service Name: " + request.getServiceName());

            if (request.getServiceName() == null || request.getServiceName().trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"Service name is required\"}")
                        .build();
            }

            if (userId == null || orgId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            ServiceDTO service = serviceBean.createService(request, orgId, userId);

            LOGGER.info("✅ Service created successfully!");
            LOGGER.info("Service ID: " + service.getId());
            LOGGER.info("Service Code: " + service.getServiceCode());
            LOGGER.info("========================================");

            return Response.status(Response.Status.CREATED).entity(service).build();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error creating service: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to create service: " + e.getMessage() + "\"}")
                    .build();
        }
    }

    // ========================================
    // GET SERVICE BY ID
    // ========================================
    @GET
    @Path("/{id}")
    public Response getServiceById(@PathParam("id") Long id,
                                   @HeaderParam("X-User-Id") Long userId) {
        try {
            LOGGER.info("Getting service by ID: " + id + " (requested by user: " + userId + ")");

            return serviceBean.getServiceById(id)
                    .map(service -> Response.ok(service).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Service not found\"}")
                            .build());
        } catch (Exception e) {
            LOGGER.severe("Error getting service: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get service\"}")
                    .build();
        }
    }

    // ========================================
    // GET SERVICE BY CODE
    // ========================================
    @GET
    @Path("/code/{code}")
    public Response getServiceByCode(@PathParam("code") String code,
                                     @HeaderParam("X-User-Id") Long userId) {
        try {
            LOGGER.info("Getting service by code: " + code + " (requested by user: " + userId + ")");

            return serviceBean.getServiceByCode(code)
                    .map(service -> Response.ok(service).build())
                    .orElse(Response.status(Response.Status.NOT_FOUND)
                            .entity("{\"error\": \"Service not found\"}")
                            .build());
        } catch (Exception e) {
            LOGGER.severe("Error getting service by code: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get service\"}")
                    .build();
        }
    }

    // ========================================
    // LIST SERVICES BY ORG
    // ========================================
    @GET
    @Path("/org/{orgId}")
    public Response listServicesByOrg(@PathParam("orgId") Long orgId,
                                      @HeaderParam("X-User-Id") Long userId,
                                      @HeaderParam("X-Org-Id") Long userOrgId,
                                      @HeaderParam("X-Roles") String roles) {
        try {
            LOGGER.info("Listing services for org: " + orgId + " (requested by user: " + userId + ")");

            // Verify user has access to this org (unless PLATFORM_ADMIN)
            if (roles == null || !roles.contains("PLATFORM_ADMIN")) {
                if (userOrgId == null || !orgId.equals(userOrgId)) {
                    LOGGER.warning("Access denied: User org " + userOrgId + " != requested org " + orgId);
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\": \"You can only access services for your organization\"}")
                            .build();
                }
            }

            List<ServiceDTO> services = serviceBean.listServicesByOrg(orgId);
            LOGGER.info("Found " + services.size() + " services for org " + orgId);

            return Response.ok(services).build();
        } catch (Exception e) {
            LOGGER.severe("Error listing services: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to list services\"}")
                    .build();
        }
    }

    // ========================================
    // GET SERVICE TREE
    // ========================================
    @GET
    @Path("/org/{orgId}/tree")
    public Response getServiceTree(@PathParam("orgId") Long orgId,
                                   @HeaderParam("X-User-Id") Long userId,
                                   @HeaderParam("X-Org-Id") Long userOrgId,
                                   @HeaderParam("X-Roles") String roles) {
        try {
            LOGGER.info("Getting service tree for org: " + orgId + " (requested by user: " + userId + ")");

            // Verify user has access to this org (unless PLATFORM_ADMIN)
            if (roles == null || !roles.contains("PLATFORM_ADMIN")) {
                if (userOrgId == null || !orgId.equals(userOrgId)) {
                    LOGGER.warning("Access denied: User org " + userOrgId + " != requested org " + orgId);
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\": \"You can only access services for your organization\"}")
                            .build();
                }
            }

            List<ServiceTreeDTO> tree = serviceBean.getServiceTree(orgId);
            LOGGER.info("Service tree retrieved successfully for org " + orgId);

            return Response.ok(tree).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting service tree: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get service tree\"}")
                    .build();
        }
    }

    // ========================================
    // GET CHILDREN
    // ========================================
    @GET
    @Path("/{id}/children")
    public Response getChildren(@PathParam("id") Long id,
                                @HeaderParam("X-User-Id") Long userId) {
        try {
            LOGGER.info("Getting children of service: " + id + " (requested by user: " + userId + ")");

            List<ServiceDTO> children = serviceBean.getChildren(id);
            LOGGER.info("Found " + children.size() + " children for service " + id);

            return Response.ok(children).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting children: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get children\"}")
                    .build();
        }
    }

    // ========================================
    // GET DESCENDANTS
    // ========================================
    @GET
    @Path("/{id}/descendants")
    public Response getDescendants(@PathParam("id") Long id,
                                   @HeaderParam("X-User-Id") Long userId) {
        try {
            LOGGER.info("Getting descendants of service: " + id + " (requested by user: " + userId + ")");

            List<ServiceDTO> descendants = serviceBean.getDescendants(id);
            LOGGER.info("Found " + descendants.size() + " descendants for service " + id);

            return Response.ok(descendants).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting descendants: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get descendants\"}")
                    .build();
        }
    }

    // ========================================
    // GET ANCESTORS (BREADCRUMB)
    // ========================================
    @GET
    @Path("/{id}/ancestors")
    public Response getAncestors(@PathParam("id") Long id,
                                 @HeaderParam("X-User-Id") Long userId) {
        try {
            LOGGER.info("Getting ancestors of service: " + id + " (requested by user: " + userId + ")");

            List<ServiceDTO> ancestors = serviceBean.getAncestors(id);
            LOGGER.info("Found " + ancestors.size() + " ancestors for service " + id);

            return Response.ok(ancestors).build();
        } catch (Exception e) {
            LOGGER.severe("Error getting ancestors: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to get ancestors\"}")
                    .build();
        }
    }

    // ========================================
    // UPDATE SERVICE
    // ========================================
    @PUT
    @Path("/{id}")
    public Response updateService(@PathParam("id") Long id,
                                  UpdateServiceRequest request,
                                  @HeaderParam("X-User-Id") Long userId,
                                  @HeaderParam("X-Email") String email) {
        try {
            LOGGER.info("========================================");
            LOGGER.info("UPDATE SERVICE REQUEST");
            LOGGER.info("Service ID: " + id);
            LOGGER.info("User ID: " + userId);
            LOGGER.info("Email: " + email);
            LOGGER.info("========================================");

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            ServiceDTO service = serviceBean.updateService(id, request, userId);

            LOGGER.info("✅ Service updated successfully! ID: " + id);

            return Response.ok(service).build();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error updating service: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to update service\"}")
                    .build();
        }
    }

    // ========================================
    // DELETE SERVICE
    // ========================================
    @DELETE
    @Path("/{id}")
    public Response deleteService(@PathParam("id") Long id,
                                  @HeaderParam("X-User-Id") Long userId,
                                  @HeaderParam("X-Email") String email) {
        try {
            LOGGER.info("========================================");
            LOGGER.info("DELETE SERVICE REQUEST");
            LOGGER.info("Service ID: " + id);
            LOGGER.info("User ID: " + userId);
            LOGGER.info("Email: " + email);
            LOGGER.info("========================================");

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            serviceBean.deleteService(id, userId);

            LOGGER.info("✅ Service deleted successfully! ID: " + id);

            return Response.ok("{\"message\": \"Service deleted successfully\"}").build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.warning("Error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error deleting service: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to delete service\"}")
                    .build();
        }
    }

    // ========================================
    // ACTIVATE SERVICE
    // ========================================
    @PUT
    @Path("/{id}/activate")
    public Response activateService(@PathParam("id") Long id,
                                    @HeaderParam("X-User-Id") Long userId,
                                    @HeaderParam("X-Email") String email) {
        try {
            LOGGER.info("Activating service ID: " + id + " by user: " + email);

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            ServiceDTO service = serviceBean.activateService(id, userId);

            LOGGER.info("✅ Service activated successfully! ID: " + id);

            return Response.ok(service).build();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error activating service: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to activate service\"}")
                    .build();
        }
    }

    // ========================================
    // DEACTIVATE SERVICE
    // ========================================
    @PUT
    @Path("/{id}/deactivate")
    public Response deactivateService(@PathParam("id") Long id,
                                      @HeaderParam("X-User-Id") Long userId,
                                      @HeaderParam("X-Email") String email) {
        try {
            LOGGER.info("Deactivating service ID: " + id + " by user: " + email);

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            ServiceDTO service = serviceBean.deactivateService(id, userId);

            LOGGER.info("✅ Service deactivated successfully! ID: " + id);

            return Response.ok(service).build();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error deactivating service: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to deactivate service\"}")
                    .build();
        }
    }

    // ========================================
    // SET MAINTENANCE MODE
    // ========================================
    @PUT
    @Path("/{id}/maintenance")
    public Response setMaintenanceMode(@PathParam("id") Long id,
                                       @HeaderParam("X-User-Id") Long userId,
                                       @HeaderParam("X-Email") String email) {
        try {
            LOGGER.info("Setting maintenance mode for service ID: " + id + " by user: " + email);

            if (userId == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\": \"Authentication required\"}")
                        .build();
            }

            ServiceDTO service = serviceBean.setMaintenanceMode(id, userId);

            LOGGER.info("✅ Maintenance mode set successfully! ID: " + id);

            return Response.ok(service).build();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Validation error: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        } catch (Exception e) {
            LOGGER.severe("Error setting maintenance mode: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\": \"Failed to set maintenance mode\"}")
                    .build();
        }
    }

    // ========================================
    // HELPER: Get Client IP
    // ========================================
    private String getClientIp() {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = httpRequest.getRemoteAddr();
        }
        return ip;
    }
}
