package com.eprocure.notification.infrastructure.kafka;

import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "eprocure.notification.integration.kafka-enabled", havingValue = "true")
public class NotificationKafkaListenerStarter {
    private static final Logger log = LogManager.getLogger(NotificationKafkaListenerStarter.class);

    private final KafkaListenerEndpointRegistry registry;
    private final boolean autoStartup;

    public NotificationKafkaListenerStarter(
            KafkaListenerEndpointRegistry registry,
            @Value("${eprocure.notification.integration.kafka-auto-startup:true}") boolean autoStartup) {
        this.registry = registry;
        this.autoStartup = autoStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListenersAfterApplicationReady() {
        if (!autoStartup) {
            log.info("[KAFKA] Notification Kafka listener startup disabled by configuration");
            return;
        }
        CompletableFuture.runAsync(this::startListenersSafely);
    }

    private void startListenersSafely() {
        try {
            log.info("[KAFKA] Starting notification Kafka listeners");
            registry.start();
            log.info("[KAFKA] Notification Kafka listeners started");
        } catch (RuntimeException exception) {
            log.warn("[KAFKA] Notification Kafka listeners not started | reason={}", exception.getMessage());
        }
    }
}
