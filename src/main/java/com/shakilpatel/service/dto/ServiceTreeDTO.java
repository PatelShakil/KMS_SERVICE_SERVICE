package com.shakilpatel.service.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceTreeDTO {

    private Long id;
    private String serviceCode;
    private String serviceName;
    private String slug;
    private Integer level;
    private String fullPath;
    private String serviceType;
    private String status;
    private Boolean isPublic;
    private Boolean isFeedbackEnabled;
    private Integer totalFeedbackForms;
    private Integer totalQrCodes;

    private List<ServiceTreeDTO> children = new ArrayList<>();
}
