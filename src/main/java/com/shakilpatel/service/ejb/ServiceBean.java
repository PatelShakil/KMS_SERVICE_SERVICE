package com.shakilpatel.service.ejb;

import com.shakilpatel.service.dto.*;
import com.shakilpatel.service.entity.Service;
import com.shakilpatel.service.entity.ServiceHierarchyCache;
import com.shakilpatel.service.enums.ServiceStatus;
import com.shakilpatel.service.util.ServiceCodeGenerator;
import com.shakilpatel.service.util.SlugGenerator;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class ServiceBean {

    private static final Logger LOGGER = Logger.getLogger(ServiceBean.class.getName());

    @PersistenceContext(unitName = "ServicePU")
    private EntityManager em;

    @Inject
    private KafkaProducerBean kafkaProducer;

    // ========================================
    // CREATE SERVICE
    // ========================================
    public ServiceDTO createService(CreateServiceRequest request, Long orgId, Long createdBy) {
        LOGGER.info("Creating service: " + request.getServiceName() + " for org: " + orgId);

        // Validate parent service if provided
        if (request.getParentServiceId() != null) {
            Service parent = em.find(Service.class, request.getParentServiceId());
            if (parent == null) {
                throw new IllegalArgumentException("Parent service not found");
            }
            if (!parent.getOrgId().equals(orgId)) {
                throw new IllegalArgumentException("Parent service belongs to different organization");
            }
        }

        // Generate unique service code and slug
        String serviceCode = ServiceCodeGenerator.generate();
        String baseSlug = SlugGenerator.generate(request.getServiceName());
        String slug = ensureUniqueSlug(baseSlug, orgId, null);

        // Use native SQL to avoid EclipseLink issues
        String sql = "INSERT INTO service_service.services " +
                "(service_code, display_code, service_name, slug, org_id, parent_service_id, " +
                "service_type, description, short_description, " +
                "contact_person_name, contact_phone, contact_email, contact_address, " +
                "latitude, longitude, address, city, state, pincode, " +
                "operating_hours, is_public, is_feedback_enabled, " +
                "created_by, created_at, updated_at, metadata) " +
                "VALUES (?, ?, ?, ?, ?, CAST(? AS BIGINT), ?::service_service.service_type, ?, ?, " +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "CAST(? AS JSONB), ?, ?, ?, ?, ?, CAST(? AS JSONB)) " +
                "RETURNING id";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceCode);
        query.setParameter(2, request.getDisplayCode());
        query.setParameter(3, request.getServiceName());
        query.setParameter(4, slug);
        query.setParameter(5, orgId);
        query.setParameter(6, request.getParentServiceId());
        query.setParameter(7, request.getServiceType() != null ? request.getServiceType().name() : "DEPARTMENT");
        query.setParameter(8, request.getDescription());
        query.setParameter(9, request.getShortDescription());
        query.setParameter(10, request.getContactPersonName());
        query.setParameter(11, request.getContactPhone());
        query.setParameter(12, request.getContactEmail());
        query.setParameter(13, request.getContactAddress());
        query.setParameter(14, request.getLatitude());
        query.setParameter(15, request.getLongitude());
        query.setParameter(16, request.getAddress());
        query.setParameter(17, request.getCity());
        query.setParameter(18, request.getState());
        query.setParameter(19, request.getPincode());
        query.setParameter(20, request.getOperatingHours());
        query.setParameter(21, request.getIsPublic() != null ? request.getIsPublic() : true);
        query.setParameter(22, request.getIsFeedbackEnabled() != null ? request.getIsFeedbackEnabled() : true);
        query.setParameter(23, createdBy);
        query.setParameter(24, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(25, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(26, request.getMetadata());

        Long serviceId = ((Number) query.getSingleResult()).longValue();

        // Build hierarchy cache
        buildHierarchyCache(serviceId, request.getParentServiceId());

        // Log audit
        logAudit(serviceId, "CREATE", createdBy, null, null);
        em.flush();

        // Get created service
        Service service = em.find(Service.class, serviceId);
        ServiceDTO dto = mapToDTO(service);

        // Publish Kafka event
        kafkaProducer.publishServiceCreated(dto);

        LOGGER.info("Service created successfully with ID: " + serviceId);

        return dto;
    }

    // ========================================
    // GET SERVICE BY ID
    // ========================================
    public Optional<ServiceDTO> getServiceById(Long serviceId) {
        Service service = em.find(Service.class, serviceId);
        if (service == null || service.getDeletedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(mapToDTO(service));
    }

    // ========================================
    // GET SERVICE BY CODE
    // ========================================
    public Optional<ServiceDTO> getServiceByCode(String serviceCode) {
        try {
            TypedQuery<Service> query = em.createQuery(
                    "SELECT s FROM Service s WHERE s.serviceCode = :code AND s.deletedAt IS NULL",
                    Service.class
            );
            query.setParameter("code", serviceCode);
            Service service = query.getSingleResult();
            return Optional.of(mapToDTO(service));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ========================================
    // LIST SERVICES BY ORG
    // ========================================
    public List<ServiceDTO> listServicesByOrg(Long orgId) {
        TypedQuery<Service> query = em.createQuery(
                "SELECT s FROM Service s WHERE s.orgId = :orgId AND s.deletedAt IS NULL ORDER BY s.createdAt DESC",
                Service.class
        );
        query.setParameter("orgId", orgId);

        return query.getResultList().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // GET SERVICE TREE (HIERARCHY)
    // ========================================
    public List<ServiceTreeDTO> getServiceTree(Long orgId) {
        // Get all services for org
        TypedQuery<Service> query = em.createQuery(
                "SELECT s FROM Service s WHERE s.orgId = :orgId AND s.deletedAt IS NULL ORDER BY s.level, s.serviceName",
                Service.class
        );
        query.setParameter("orgId", orgId);
        List<Service> allServices = query.getResultList();

        // Build tree structure
        return buildTree(allServices, null);
    }

    // ========================================
    // GET CHILDREN
    // ========================================
    public List<ServiceDTO> getChildren(Long serviceId) {
        TypedQuery<Service> query = em.createQuery(
                "SELECT s FROM Service s WHERE s.parentServiceId = :parentId AND s.deletedAt IS NULL ORDER BY s.serviceName",
                Service.class
        );
        query.setParameter("parentId", serviceId);

        return query.getResultList().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // GET DESCENDANTS (ALL CHILDREN RECURSIVELY)
    // ========================================
    public List<ServiceDTO> getDescendants(Long serviceId) {
        String sql = "SELECT s.* FROM service_service.services s " +
                "JOIN service_service.service_hierarchy_cache shc ON s.id = shc.descendant_id " +
                "WHERE shc.ancestor_id = ? AND shc.depth > 0 AND s.deleted_at IS NULL " +
                "ORDER BY shc.depth, s.service_name";

        Query query = em.createNativeQuery(sql, Service.class);
        query.setParameter(1, serviceId);

        @SuppressWarnings("unchecked")
        List<Service> descendants = query.getResultList();

        return descendants.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // GET ANCESTORS (BREADCRUMB)
    // ========================================
    public List<ServiceDTO> getAncestors(Long serviceId) {
        String sql = "SELECT s.* FROM service_service.services s " +
                "JOIN service_service.service_hierarchy_cache shc ON s.id = shc.ancestor_id " +
                "WHERE shc.descendant_id = ? AND shc.depth > 0 AND s.deleted_at IS NULL " +
                "ORDER BY shc.depth DESC";

        Query query = em.createNativeQuery(sql, Service.class);
        query.setParameter(1, serviceId);

        @SuppressWarnings("unchecked")
        List<Service> ancestors = query.getResultList();

        return ancestors.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ========================================
    // UPDATE SERVICE
    // ========================================
    public ServiceDTO updateService(Long serviceId, UpdateServiceRequest request, Long updatedBy) {
        Service service = em.find(Service.class, serviceId);
        if (service == null || service.getDeletedAt() != null) {
            throw new IllegalArgumentException("Service not found");
        }

        // Update fields
        if (request.getServiceName() != null) {
            service.setServiceName(request.getServiceName());
            String baseSlug = SlugGenerator.generate(request.getServiceName());
            service.setSlug(ensureUniqueSlug(baseSlug, service.getOrgId(), serviceId));
        }
        if (request.getDisplayCode() != null) service.setDisplayCode(request.getDisplayCode());
        if (request.getServiceType() != null) service.setServiceType(request.getServiceType());
        if (request.getDescription() != null) service.setDescription(request.getDescription());
        if (request.getShortDescription() != null) service.setShortDescription(request.getShortDescription());

        if (request.getContactPersonName() != null) service.setContactPersonName(request.getContactPersonName());
        if (request.getContactPhone() != null) service.setContactPhone(request.getContactPhone());
        if (request.getContactEmail() != null) service.setContactEmail(request.getContactEmail());
        if (request.getContactAddress() != null) service.setContactAddress(request.getContactAddress());

        if (request.getLatitude() != null) service.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) service.setLongitude(request.getLongitude());
        if (request.getAddress() != null) service.setAddress(request.getAddress());
        if (request.getCity() != null) service.setCity(request.getCity());
        if (request.getState() != null) service.setState(request.getState());
        if (request.getPincode() != null) service.setPincode(request.getPincode());

        if (request.getOperatingHours() != null) service.setOperatingHours(request.getOperatingHours());
        if (request.getStatus() != null) service.setStatus(request.getStatus());
        if (request.getIsPublic() != null) service.setIsPublic(request.getIsPublic());
        if (request.getIsFeedbackEnabled() != null) service.setIsFeedbackEnabled(request.getIsFeedbackEnabled());
        if (request.getMetadata() != null) service.setMetadata(request.getMetadata());

        service.setUpdatedAt(LocalDateTime.now());

        em.merge(service);
        em.flush();

        logAudit(serviceId, "UPDATE", updatedBy, null, null);

        ServiceDTO dto = mapToDTO(service);
        kafkaProducer.publishServiceUpdated(dto);

        return dto;
    }

    // ========================================
    // DELETE SERVICE (SOFT DELETE)
    // ========================================
    public void deleteService(Long serviceId, Long deletedBy) {
        Service service = em.find(Service.class, serviceId);
        if (service == null || service.getDeletedAt() != null) {
            throw new IllegalArgumentException("Service not found");
        }

        // Check if has children
        TypedQuery<Long> countQuery = em.createQuery(
                "SELECT COUNT(s) FROM Service s WHERE s.parentServiceId = :parentId AND s.deletedAt IS NULL",
                Long.class
        );
        countQuery.setParameter("parentId", serviceId);
        Long childCount = countQuery.getSingleResult();

        if (childCount > 0) {
            throw new IllegalStateException("Cannot delete service with active children. Delete children first.");
        }

        // Soft delete
        service.setDeletedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        em.merge(service);
        em.flush();

        logAudit(serviceId, "DELETE", deletedBy, null, null);

        kafkaProducer.publishServiceDeleted(serviceId, service.getOrgId());
    }

    // ========================================
    // ACTIVATE SERVICE
    // ========================================
    public ServiceDTO activateService(Long serviceId, Long activatedBy) {
        return updateServiceStatus(serviceId, ServiceStatus.ACTIVE, activatedBy, "ACTIVATE");
    }

    // ========================================
    // DEACTIVATE SERVICE
    // ========================================
    public ServiceDTO deactivateService(Long serviceId, Long deactivatedBy) {
        return updateServiceStatus(serviceId, ServiceStatus.INACTIVE, deactivatedBy, "DEACTIVATE");
    }

    // ========================================
    // SET MAINTENANCE MODE
    // ========================================
    public ServiceDTO setMaintenanceMode(Long serviceId, Long updatedBy) {
        return updateServiceStatus(serviceId, ServiceStatus.UNDER_MAINTENANCE, updatedBy, "MAINTENANCE");
    }

    // ========================================
    // UPDATE STATUS
    // ========================================
    private ServiceDTO updateServiceStatus(Long serviceId, ServiceStatus status, Long updatedBy, String action) {
        Service service = em.find(Service.class, serviceId);
        if (service == null || service.getDeletedAt() != null) {
            throw new IllegalArgumentException("Service not found");
        }

        service.setStatus(status);
        service.setUpdatedAt(LocalDateTime.now());
        em.merge(service);
        em.flush();

        logAudit(serviceId, action, updatedBy, null, null);

        ServiceDTO dto = mapToDTO(service);

        if (status == ServiceStatus.ACTIVE) {
            kafkaProducer.publishServiceActivated(dto);
        } else if (status == ServiceStatus.INACTIVE) {
            kafkaProducer.publishServiceDeactivated(dto);
        }

        return dto;
    }

    // ========================================
    // HELPER: BUILD HIERARCHY CACHE
    // ========================================
    private void buildHierarchyCache(Long serviceId, Long parentServiceId) {
        // Insert self-reference
        String selfSql = "INSERT INTO service_service.service_hierarchy_cache " +
                "(ancestor_id, descendant_id, depth) VALUES (?, ?, 0)";
        Query selfQuery = em.createNativeQuery(selfSql);
        selfQuery.setParameter(1, serviceId);
        selfQuery.setParameter(2, serviceId);
        selfQuery.executeUpdate();

        // Insert all ancestors
        if (parentServiceId != null) {
            String ancestorSql = "INSERT INTO service_service.service_hierarchy_cache " +
                    "(ancestor_id, descendant_id, depth) " +
                    "SELECT ancestor_id, ?, depth + 1 " +
                    "FROM service_service.service_hierarchy_cache " +
                    "WHERE descendant_id = ?";
            Query ancestorQuery = em.createNativeQuery(ancestorSql);
            ancestorQuery.setParameter(1, serviceId);
            ancestorQuery.setParameter(2, parentServiceId);
            ancestorQuery.executeUpdate();
        }
    }

    // ========================================
    // HELPER: ENSURE UNIQUE SLUG
    // ========================================
    private String ensureUniqueSlug(String baseSlug, Long orgId, Long excludeId) {
        String slug = baseSlug;
        int counter = 1;

        while (true) {
            TypedQuery<Long> query;
            if (excludeId != null) {
                query = em.createQuery(
                        "SELECT COUNT(s) FROM Service s WHERE s.slug = :slug AND s.orgId = :orgId AND s.id != :excludeId",
                        Long.class
                );
                query.setParameter("excludeId", excludeId);
            } else {
                query = em.createQuery(
                        "SELECT COUNT(s) FROM Service s WHERE s.slug = :slug AND s.orgId = :orgId",
                        Long.class
                );
            }
            query.setParameter("slug", slug);
            query.setParameter("orgId", orgId);

            Long count = query.getSingleResult();
            if (count == 0) {
                return slug;
            }

            slug = baseSlug + "-" + counter++;
        }
    }

    // ========================================
    // HELPER: BUILD TREE
    // ========================================
    private List<ServiceTreeDTO> buildTree(List<Service> services, Long parentId) {
        List<ServiceTreeDTO> tree = new ArrayList<>();

        for (Service service : services) {
            Long serviceParentId = service.getParentServiceId();
            if ((parentId == null && serviceParentId == null) ||
                    (parentId != null && parentId.equals(serviceParentId))) {

                ServiceTreeDTO node = new ServiceTreeDTO();
                node.setId(service.getId());
                node.setServiceCode(service.getServiceCode());
                node.setServiceName(service.getServiceName());
                node.setSlug(service.getSlug());
                node.setLevel(service.getLevel());
                node.setFullPath(service.getFullPath());
                node.setServiceType(service.getServiceType().name());
                node.setStatus(service.getStatus().name());
                node.setIsPublic(service.getIsPublic());
                node.setIsFeedbackEnabled(service.getIsFeedbackEnabled());
                node.setTotalFeedbackForms(service.getTotalFeedbackForms());
                node.setTotalQrCodes(service.getTotalQrCodes());

                // Recursively build children
                node.setChildren(buildTree(services, service.getId()));

                tree.add(node);
            }
        }

        return tree;
    }

    // ========================================
    // HELPER: LOG AUDIT
    // ========================================
    private void logAudit(Long serviceId, String action, Long changedBy, String ipAddress, String userAgent) {
        String sql = "INSERT INTO service_service.service_audit " +
                "(service_id, action, changed_by, timestamp, ip_address, user_agent, changes) " +
                "VALUES (?, ?, ?, ?, CAST(? AS INET), ?, CAST(? AS JSONB))";

        Query query = em.createNativeQuery(sql);
        query.setParameter(1, serviceId);
        query.setParameter(2, action);
        query.setParameter(3, changedBy);
        query.setParameter(4, Timestamp.valueOf(LocalDateTime.now()));
        query.setParameter(5, ipAddress);
        query.setParameter(6, userAgent);
        query.setParameter(7, null);
        query.executeUpdate();
    }

    // ========================================
    // HELPER: MAP TO DTO
    // ========================================
    private ServiceDTO mapToDTO(Service service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setServiceCode(service.getServiceCode());
        dto.setDisplayCode(service.getDisplayCode());
        dto.setServiceName(service.getServiceName());
        dto.setSlug(service.getSlug());
        dto.setOrgId(service.getOrgId());
        dto.setParentServiceId(service.getParentServiceId());
        dto.setLevel(service.getLevel());
        dto.setPath(service.getPath());
        dto.setFullPath(service.getFullPath());
        dto.setServiceType(service.getServiceType());
        dto.setDescription(service.getDescription());
        dto.setShortDescription(service.getShortDescription());
        dto.setIconUrl(service.getIconUrl());
        dto.setCoverImageUrl(service.getCoverImageUrl());
        dto.setContactPersonName(service.getContactPersonName());
        dto.setContactPhone(service.getContactPhone());
        dto.setContactEmail(service.getContactEmail());
        dto.setContactAddress(service.getContactAddress());
        dto.setLatitude(service.getLatitude());
        dto.setLongitude(service.getLongitude());
        dto.setAddress(service.getAddress());
        dto.setCity(service.getCity());
        dto.setState(service.getState());
        dto.setPincode(service.getPincode());
        dto.setOperatingHours(service.getOperatingHours());
        dto.setStatus(service.getStatus());
        dto.setIsPublic(service.getIsPublic());
        dto.setIsFeedbackEnabled(service.getIsFeedbackEnabled());
        dto.setTotalFeedbackForms(service.getTotalFeedbackForms());
        dto.setTotalQrCodes(service.getTotalQrCodes());
        dto.setTotalFeedbackReceived(service.getTotalFeedbackReceived());
        dto.setCreatedBy(service.getCreatedBy());
        dto.setCreatedAt(service.getCreatedAt());
        dto.setUpdatedAt(service.getUpdatedAt());
        dto.setMetadata(service.getMetadata());
        return dto;
    }
}
