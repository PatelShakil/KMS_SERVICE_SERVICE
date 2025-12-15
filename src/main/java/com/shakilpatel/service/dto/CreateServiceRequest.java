package com.shakilpatel.service.dto;

import com.shakilpatel.service.enums.ServiceStatus;
import com.shakilpatel.service.enums.ServiceType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateServiceRequest {

    private String serviceName;
    private String displayCode;
    private Long parentServiceId;
    private ServiceType serviceType;
    private String description;
    private String shortDescription;

    // Contact
    private String contactPersonName;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;

    // Location
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String city;
    private String state;
    private String pincode;

    // Operating hours (JSON string)
    private String operatingHours;

    // Status
    private Boolean isPublic;
    private Boolean isFeedbackEnabled;

    // Metadata (JSON string)
    private String metadata;
}
