package com.shakilpatel.service.entity;

import com.shakilpatel.service.enums.ServiceStatus;
import com.shakilpatel.service.enums.ServiceType;
import com.shakilpatel.service.util.ServiceStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services", schema = "service_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", unique = true, nullable = false, length = 100)
    private String serviceCode;

    @Column(name = "display_code", length = 100)
    private String displayCode;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "slug", unique = true, nullable = false, length = 250)
    private String slug;

    // Hierarchy
    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "parent_service_id")
    private Long parentServiceId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "path", columnDefinition = "TEXT")
    private String path;

    @Column(name = "full_path", columnDefinition = "TEXT")
    private String fullPath;

    // Service details
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, columnDefinition = "service_service.service_type")
    private ServiceType serviceType = ServiceType.DEPARTMENT;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    // Media
    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    // Contact
    @Column(name = "contact_person_name", length = 100)
    private String contactPersonName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_address", columnDefinition = "TEXT")
    private String contactAddress;

    // Location
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    // Operating hours (JSONB stored as String)
    @Column(name = "operating_hours", columnDefinition = "JSONB")
    private String operatingHours;

    // Status
    @Convert(converter = ServiceStatusConverter.class)
    @Column(name = "status", columnDefinition = "service_service.service_status")
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Column(name = "is_feedback_enabled")
    private Boolean isFeedbackEnabled = true;

    // Metrics
    @Column(name = "total_feedback_forms")
    private Integer totalFeedbackForms = 0;

    @Column(name = "total_qr_codes")
    private Integer totalQrCodes = 0;

    @Column(name = "total_feedback_received")
    private Integer totalFeedbackReceived = 0;

    // Audit
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Metadata (JSONB stored as String)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;
}
