package com.shakilpatel.service.dto;

import com.shakilpatel.service.enums.ServiceStatus;
import com.shakilpatel.service.enums.ServiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceDTO {

    private Long id;
    private String serviceCode;
    private String displayCode;
    private String serviceName;
    private String slug;

    private Long orgId;
    private Long parentServiceId;
    private Integer level;
    private String path;
    private String fullPath;

    private ServiceType serviceType;
    private String description;
    private String shortDescription;

    private String iconUrl;
    private String coverImageUrl;

    private String contactPersonName;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String city;
    private String state;
    private String pincode;

    private String operatingHours;

    private ServiceStatus status;
    private Boolean isPublic;
    private Boolean isFeedbackEnabled;

    private Integer totalFeedbackForms;
    private Integer totalQrCodes;
    private Integer totalFeedbackReceived;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String metadata;
}
