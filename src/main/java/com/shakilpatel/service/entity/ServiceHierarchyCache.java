package com.shakilpatel.service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "service_hierarchy_cache", schema = "service_service")
@IdClass(ServiceHierarchyCache.ServiceHierarchyCacheId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ServiceHierarchyCache {

    @Id
    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    @Id
    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    // Composite Primary Key Class
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ServiceHierarchyCacheId implements Serializable {
        private Long ancestorId;
        private Long descendantId;
    }
}
