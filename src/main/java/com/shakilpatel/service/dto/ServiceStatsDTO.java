package com.shakilpatel.service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceStatsDTO {

    private Long serviceId;
    private String serviceName;
    private Integer totalChildren;
    private Integer totalDescendants;
    private Integer totalFeedbackForms;
    private Integer totalQrCodes;
    private Integer totalFeedbackReceived;
    private Integer depth;
}
