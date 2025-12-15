package com.shakilpatel.service.ejb;

import com.shakilpatel.service.dto.ServiceDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.logging.Logger;

@Singleton
@Startup
public class KafkaProducerBean {

    private static final Logger LOGGER = Logger.getLogger(KafkaProducerBean.class.getName());

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC_SERVICE_CREATED = "service.created";
    private static final String TOPIC_SERVICE_UPDATED = "service.updated";
    private static final String TOPIC_SERVICE_DELETED = "service.deleted";
    private static final String TOPIC_SERVICE_ACTIVATED = "service.activated";
    private static final String TOPIC_SERVICE_DEACTIVATED = "service.deactivated";

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        producer = new KafkaProducer<>(props);
        LOGGER.info("✅ Kafka Producer initialized for service-service");
    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            producer.close();
            LOGGER.info("Kafka Producer closed");
        }
    }

    // ========================================
    // SERVICE CREATED EVENT
    // ========================================
    public void publishServiceCreated(ServiceDTO service) {
        try {
            JsonObject payload = Json.createObjectBuilder()
                    .add("serviceId", service.getId())
                    .add("serviceCode", service.getServiceCode())
                    .add("serviceName", service.getServiceName())
                    .add("slug", service.getSlug())
                    .add("orgId", service.getOrgId())
                    .add("parentServiceId", service.getParentServiceId() != null ? service.getParentServiceId() : -1)
                    .add("level", service.getLevel())
                    .add("path", service.getPath() != null ? service.getPath() : "")
                    .add("fullPath", service.getFullPath() != null ? service.getFullPath() : "")
                    .add("serviceType", service.getServiceType().name())
                    .add("status", service.getStatus().name())
                    .add("isPublic", service.getIsPublic())
                    .add("isFeedbackEnabled", service.getIsFeedbackEnabled())
                    .add("createdBy", service.getCreatedBy())
                    .add("createdAt", service.getCreatedAt().toString())
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_SERVICE_CREATED,
                    service.getServiceCode(),
                    payload.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("❌ Failed to publish service.created event: " + exception.getMessage());
                } else {
                    LOGGER.info("✅ Published service.created | ID: " + service.getId() +
                            " | Offset: " + metadata.offset());
                }
            });

        } catch (Exception e) {
            LOGGER.severe("❌ Error publishing service.created event: " + e.getMessage());
        }
    }

    // ========================================
    // SERVICE UPDATED EVENT
    // ========================================
    public void publishServiceUpdated(ServiceDTO service) {
        try {
            JsonObject payload = Json.createObjectBuilder()
                    .add("serviceId", service.getId())
                    .add("serviceCode", service.getServiceCode())
                    .add("serviceName", service.getServiceName())
                    .add("slug", service.getSlug())
                    .add("orgId", service.getOrgId())
                    .add("status", service.getStatus().name())
                    .add("isPublic", service.getIsPublic())
                    .add("isFeedbackEnabled", service.getIsFeedbackEnabled())
                    .add("updatedAt", service.getUpdatedAt().toString())
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_SERVICE_UPDATED,
                    service.getServiceCode(),
                    payload.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("❌ Failed to publish service.updated event: " + exception.getMessage());
                } else {
                    LOGGER.info("✅ Published service.updated | ID: " + service.getId() +
                            " | Offset: " + metadata.offset());
                }
            });

        } catch (Exception e) {
            LOGGER.severe("❌ Error publishing service.updated event: " + e.getMessage());
        }
    }

    // ========================================
    // SERVICE DELETED EVENT
    // ========================================
    public void publishServiceDeleted(Long serviceId, Long orgId) {
        try {
            JsonObject payload = Json.createObjectBuilder()
                    .add("serviceId", serviceId)
                    .add("orgId", orgId)
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_SERVICE_DELETED,
                    String.valueOf(serviceId),
                    payload.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("❌ Failed to publish service.deleted event: " + exception.getMessage());
                } else {
                    LOGGER.info("✅ Published service.deleted | ID: " + serviceId +
                            " | Offset: " + metadata.offset());
                }
            });

        } catch (Exception e) {
            LOGGER.severe("❌ Error publishing service.deleted event: " + e.getMessage());
        }
    }

    // ========================================
    // SERVICE ACTIVATED EVENT
    // ========================================
    public void publishServiceActivated(ServiceDTO service) {
        try {
            JsonObject payload = Json.createObjectBuilder()
                    .add("serviceId", service.getId())
                    .add("serviceCode", service.getServiceCode())
                    .add("serviceName", service.getServiceName())
                    .add("orgId", service.getOrgId())
                    .add("status", "ACTIVE")
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_SERVICE_ACTIVATED,
                    service.getServiceCode(),
                    payload.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("❌ Failed to publish service.activated event: " + exception.getMessage());
                } else {
                    LOGGER.info("✅ Published service.activated | ID: " + service.getId() +
                            " | Offset: " + metadata.offset());
                }
            });

        } catch (Exception e) {
            LOGGER.severe("❌ Error publishing service.activated event: " + e.getMessage());
        }
    }

    // ========================================
    // SERVICE DEACTIVATED EVENT
    // ========================================
    public void publishServiceDeactivated(ServiceDTO service) {
        try {
            JsonObject payload = Json.createObjectBuilder()
                    .add("serviceId", service.getId())
                    .add("serviceCode", service.getServiceCode())
                    .add("serviceName", service.getServiceName())
                    .add("orgId", service.getOrgId())
                    .add("status", "INACTIVE")
                    .add("timestamp", System.currentTimeMillis())
                    .build();

            ProducerRecord<String, String> record = new ProducerRecord<>(
                    TOPIC_SERVICE_DEACTIVATED,
                    service.getServiceCode(),
                    payload.toString()
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.severe("❌ Failed to publish service.deactivated event: " + exception.getMessage());
                } else {
                    LOGGER.info("✅ Published service.deactivated | ID: " + service.getId() +
                            " | Offset: " + metadata.offset());
                }
            });

        } catch (Exception e) {
            LOGGER.severe("❌ Error publishing service.deactivated event: " + e.getMessage());
        }
    }
}

//package com.shakilpatel.service.ejb;
//
//import com.shakilpatel.service.dto.ServiceDTO;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import jakarta.ejb.Singleton;
//import jakarta.ejb.Startup;
//import jakarta.json.Json;
//import jakarta.json.JsonObject;
//import org.apache.kafka.clients.producer.KafkaProducer;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.clients.producer.ProducerRecord;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//
//import jakarta.inject.Inject;
//import java.util.Properties;
//
//import java.util.logging.Logger;
//
//@Singleton
//@Startup
//public class KafkaProducerBean {
//
//    private static final Logger LOGGER = Logger.getLogger(KafkaProducerBean.class.getName());
//
//    @Inject
//    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
//    private String bootstrapServers;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.service.created", defaultValue = "service.created")
//    private String topicServiceCreated;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.service.updated", defaultValue = "service.updated")
//    private String topicServiceUpdated;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.service.deleted", defaultValue = "service.deleted")
//    private String topicServiceDeleted;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.service.activated", defaultValue = "service.activated")
//    private String topicServiceActivated;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.service.deactivated", defaultValue = "service.deactivated")
//    private String topicServiceDeactivated;
//
//    private KafkaProducer<String, String> producer;
//
//    @PostConstruct
//    public void init() {
//        Properties props = new Properties();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        props.put(ProducerConfig.ACKS_CONFIG, "all");
//        props.put(ProducerConfig.RETRIES_CONFIG, 3);
//        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
//        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
//
//        producer = new KafkaProducer<>(props);
//        LOGGER.info("✅ Kafka Producer initialized");
//        LOGGER.info("Bootstrap Servers: " + bootstrapServers);
//        LOGGER.info("Topics: " + topicServiceCreated + ", " + topicServiceUpdated + ", "
//                + topicServiceDeleted + ", " + topicServiceActivated + ", " + topicServiceDeactivated);
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        if (producer != null) {
//            producer.close();
//            LOGGER.info("Kafka Producer closed");
//        }
//    }
//
//    public void publishServiceCreated(ServiceDTO service) {
//        publishEvent(topicServiceCreated, service.getServiceCode(), buildServicePayload(service, "CREATED"));
//    }
//
//    public void publishServiceUpdated(ServiceDTO service) {
//        publishEvent(topicServiceUpdated, service.getServiceCode(), buildServicePayload(service, "UPDATED"));
//    }
//
//    public void publishServiceDeleted(Long serviceId, Long orgId) {
//        JsonObject payload = Json.createObjectBuilder()
//                .add("serviceId", serviceId)
//                .add("orgId", orgId)
//                .add("timestamp", System.currentTimeMillis())
//                .build();
//        publishEvent(topicServiceDeleted, String.valueOf(serviceId), payload.toString());
//    }
//
//    public void publishServiceActivated(ServiceDTO service) {
//        publishEvent(topicServiceActivated, service.getServiceCode(), buildServicePayload(service, "ACTIVATED"));
//    }
//
//    public void publishServiceDeactivated(ServiceDTO service) {
//        publishEvent(topicServiceDeactivated, service.getServiceCode(), buildServicePayload(service, "DEACTIVATED"));
//    }
//
//    private void publishEvent(String topic, String key, String payload) {
//        try {
//            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
//
//            producer.send(record, (metadata, exception) -> {
//                if (exception != null) {
//                    LOGGER.severe("❌ Failed to publish to " + topic + ": " + exception.getMessage());
//                } else {
//                    LOGGER.info("✅ Published to " + topic + " | Key: " + key + " | Offset: " + metadata.offset());
//                }
//            });
//
//        } catch (Exception e) {
//            LOGGER.severe("❌ Error publishing to " + topic + ": " + e.getMessage());
//        }
//    }
//
//    private String buildServicePayload(ServiceDTO service, String eventType) {
//        return Json.createObjectBuilder()
//                .add("eventType", eventType)
//                .add("serviceId", service.getId())
//                .add("serviceCode", service.getServiceCode())
//                .add("serviceName", service.getServiceName())
//                .add("slug", service.getSlug())
//                .add("orgId", service.getOrgId())
//                .add("parentServiceId", service.getParentServiceId() != null ? service.getParentServiceId() : -1)
//                .add("level", service.getLevel())
//                .add("path", service.getPath() != null ? service.getPath() : "")
//                .add("fullPath", service.getFullPath() != null ? service.getFullPath() : "")
//                .add("serviceType", service.getServiceType().name())
//                .add("status", service.getStatus().name())
//                .add("isPublic", service.getIsPublic())
//                .add("isFeedbackEnabled", service.getIsFeedbackEnabled())
//                .add("timestamp", System.currentTimeMillis())
//                .build()
//                .toString();
//    }
//}
