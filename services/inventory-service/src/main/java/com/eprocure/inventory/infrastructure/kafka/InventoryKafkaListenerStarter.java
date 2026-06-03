package com.eprocure.inventory.infrastructure.kafka;

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
@ConditionalOnProperty(name = "eprocure.inventory.integration.kafka-enabled", havingValue = "true")
public class InventoryKafkaListenerStarter {
    private static final Logger log = LogManager.getLogger(InventoryKafkaListenerStarter.class);

    private final KafkaListenerEndpointRegistry registry;
    private final boolean autoStartup;

    public InventoryKafkaListenerStarter(
            KafkaListenerEndpointRegistry registry,
            @Value("${eprocure.inventory.integration.kafka-auto-startup:true}") boolean autoStartup) {
        this.registry = registry;
        this.autoStartup = autoStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListenersAfterApplicationReady() {
        if (!autoStartup) {
            log.info("[KAFKA] Inventory Kafka listener startup disabled by configuration");
            return;
        }
        CompletableFuture.runAsync(this::startListenersSafely);
    }

    private void startListenersSafely() {
        try {
            log.info("[KAFKA] Starting inventory Kafka listeners");
            registry.start();
            log.info("[KAFKA] Inventory Kafka listeners started");
        } catch (RuntimeException exception) {
            log.warn("[KAFKA] Inventory Kafka listeners not started | reason={}", exception.getMessage());
        }
    }
}
