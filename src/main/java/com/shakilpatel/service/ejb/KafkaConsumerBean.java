package com.shakilpatel.service.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.StringReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class KafkaConsumerBean {

    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerBean.class.getName());

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String GROUP_ID = "service-service-group";
    private static final String TOPIC_ORG_APPROVED = "org.approved";
    private static final String TOPIC_FEEDBACK_FORM_CREATED = "feedback.form.created";

    private KafkaConsumer<String, String> consumer;
    private volatile boolean running = true;

    @EJB
    private ServiceAnalyticsBean analyticsBean;
    private ExecutorService executor;
    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC_ORG_APPROVED));
        consumer.subscribe(Collections.singletonList(TOPIC_FEEDBACK_FORM_CREATED));

        // Subscribe to relevant topics
        consumer.subscribe(Arrays.asList(
                "feedback.form.created",
                "feedback.form.deleted",
                "qr.generated",
                "qr.deleted",
                "response.submitted"
        ));

        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::pollMessages);

        LOGGER.info("✅ Service Analytics Consumer started successfully");

        LOGGER.info("✅ Kafka Consumer initialized for service-service");
    }

    @PreDestroy
    public void cleanup() {
        running = false;
        if (consumer != null) {
            consumer.close();
            LOGGER.info("Kafka Consumer closed");
        }
    }

    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
    public void pollMessages() {
        if (!running) return;

        try {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                LOGGER.info("📨 Received event from topic: " + record.topic() +
                        " | Offset: " + record.offset());
                try {
                    processMessage(record.topic(), record.value());
                } catch (Exception e) {
                    LOGGER.severe("❌ Error processing message: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            LOGGER.severe("❌ Error polling Kafka: " + e.getMessage());
        }
    }


    private void processMessage(String topic, String message) {
        LOGGER.info("Processing message from topic: " + topic);

        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject payload = reader.readObject();

            try {
                switch (topic) {
                    case "feedback.form.created":
                        handleFormCreated(payload);
                        break;

                    case "feedback.form.deleted":
                        handleFormDeleted(payload);
                        break;

                    case "qr.generated":
                        handleQrGenerated(payload);
                        break;

                    case "qr.deleted":
                        handleQrDeleted(payload);
                        break;

                    case "response.submitted":
                        handleResponseSubmitted(payload);
                        break;

                    default:
                        LOGGER.warning("⚠️ Unhandled topic: " + topic);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "❌ Error handling message", e);
            }

        } catch (Exception e) {
            LOGGER.severe("❌ Error parsing JSON: " + e.getMessage());
        }
    }

    // ========================================
    // EVENT HANDLERS
    // ========================================

    private void handleFormCreated(JsonObject json) {
        Long serviceId = json.getJsonNumber("serviceId").longValue();
        if (serviceId != null) {
            analyticsBean.incrementFeedbackForms(serviceId);
            LOGGER.info("✅ Incremented feedback forms for service: " + serviceId);
        }
    }

    private void handleFormDeleted(JsonObject json) {
        Long serviceId = json.getJsonNumber("serviceId").longValue();
        if (serviceId != null) {
            analyticsBean.decrementFeedbackForms(serviceId);
            LOGGER.info("✅ Decremented feedback forms for service: " + serviceId);
        }
    }

    private void handleQrGenerated(JsonObject json) {
        Long serviceId = json.getJsonNumber("serviceId").longValue();
        if (serviceId != null) {
            analyticsBean.incrementQrCodes(serviceId);
            LOGGER.info("✅ Incremented QR codes for service: " + serviceId);
        }
    }

    private void handleQrDeleted(JsonObject json) {
        Long serviceId = json.getJsonNumber("serviceId").longValue();
        if (serviceId != null) {
            analyticsBean.decrementQrCodes(serviceId);
            LOGGER.info("✅ Decremented QR codes for service: " + serviceId);
        }
    }

    private void handleResponseSubmitted(JsonObject json) {
        Long serviceId = json.getJsonNumber("serviceId").longValue();
        if (serviceId != null) {
            analyticsBean.incrementFeedbackReceived(serviceId);
            LOGGER.info("✅ Incremented feedback received for service: " + serviceId);
        }
    }


    private void handleOrgApproved(JsonObject json) {
        Long orgId = json.getJsonNumber("orgId").longValue();
        String orgName = json.getString("orgName", "Unknown");

        LOGGER.info("🎉 Organization approved: " + orgName + " (ID: " + orgId + ")");

        // TODO: Future - Auto-create default services for new org
        // Example: "General Feedback", "Complaint Department", etc.

        LOGGER.info("✅ Org approval processed for org ID: " + orgId);
    }
}

//package com.shakilpatel.service.ejb;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import jakarta.ejb.Schedule;
//import jakarta.ejb.Singleton;
//import jakarta.ejb.Startup;
//import jakarta.inject.Inject;
//import jakarta.json.Json;
//import jakarta.json.JsonObject;
//import jakarta.json.JsonReader;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.apache.kafka.clients.consumer.KafkaConsumer;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.eclipse.microprofile.config.inject.ConfigProperty;
//
//import java.io.StringReader;
//import java.time.Duration;
//import java.util.Collections;
//import java.util.Properties;
//
//import java.util.logging.Logger;
//
//@Singleton
//@Startup
//public class KafkaConsumerBean {
//
//    private static final Logger LOGGER = Logger.getLogger(KafkaConsumerBean.class.getName());
//
//    @Inject
//    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
//    private String bootstrapServers;
//
//    @Inject
//    @ConfigProperty(name = "kafka.consumer.group.id", defaultValue = "service-service-group")
//    private String groupId;
//
//    @Inject
//    @ConfigProperty(name = "kafka.topic.org.approved", defaultValue = "org.approved")
//    private String topicOrgApproved;
//
//    @Inject
//    @ConfigProperty(name = "kafka.consumer.poll.interval.ms", defaultValue = "1000")
//    private Integer pollIntervalMs;
//
//    @Inject
//    @ConfigProperty(name = "feature.auto.create.default.services", defaultValue = "false")
//    private Boolean autoCreateDefaultServices;
//
//    private KafkaConsumer<String, String> consumer;
//    private volatile boolean running = true;
//
//    @PostConstruct
//    public void init() {
//        Properties props = new Properties();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
//        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
//
//        consumer = new KafkaConsumer<>(props);
//        consumer.subscribe(Collections.singletonList(topicOrgApproved));
//
//        LOGGER.info("✅ Kafka Consumer initialized");
//        LOGGER.info("Bootstrap Servers: " + bootstrapServers);
//        LOGGER.info("Group ID: " + groupId);
//        LOGGER.info("Subscribed to topic: " + topicOrgApproved);
//        LOGGER.info("Auto-create default services: " + autoCreateDefaultServices);
//    }
//
//    @PreDestroy
//    public void cleanup() {
//        running = false;
//        if (consumer != null) {
//            consumer.close();
//            LOGGER.info("Kafka Consumer closed");
//        }
//    }
//
//    @Schedule(second = "*/5", minute = "*", hour = "*", persistent = false)
//    public void pollMessages() {
//        if (!running) {
//            return;
//        }
//
//        try {
//            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(pollIntervalMs));
//
//            for (ConsumerRecord<String, String> record : records) {
//                LOGGER.info("📨 Received event from topic: " + record.topic()
//                        + " | Offset: " + record.offset());
//
//                try {
//                    processMessage(record.topic(), record.value());
//                } catch (Exception e) {
//                    LOGGER.severe("❌ Error processing message: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            }
//
//        } catch (Exception e) {
//            LOGGER.severe("❌ Error polling Kafka: " + e.getMessage());
//        }
//    }
//
//    private void processMessage(String topic, String message) {
//        try (JsonReader reader = Json.createReader(new StringReader(message))) {
//            JsonObject json = reader.readObject();
//
//            if (topicOrgApproved.equals(topic)) {
//                handleOrgApproved(json);
//            }
//
//        } catch (Exception e) {
//            LOGGER.severe("❌ Error parsing JSON: " + e.getMessage());
//        }
//    }
//
//    private void handleOrgApproved(JsonObject json) {
//        Long orgId = json.getJsonNumber("orgId").longValue();
//        String orgName = json.getString("orgName", "Unknown");
//
//        LOGGER.info("🎉 Organization approved: " + orgName + " (ID: " + orgId + ")");
//
//        if (autoCreateDefaultServices) {
//            // TODO: Auto-create default services
//            LOGGER.info("Creating default services for org: " + orgId);
//        } else {
//            LOGGER.info("Auto-create disabled. Skipping default service creation.");
//        }
//
//        LOGGER.info("✅ Org approval processed for org ID: " + orgId);
//    }
//}
