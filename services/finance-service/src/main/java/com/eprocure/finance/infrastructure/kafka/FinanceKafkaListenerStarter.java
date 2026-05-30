package com.eprocure.finance.infrastructure.kafka;

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
@ConditionalOnProperty(name = "eprocure.finance.integration.kafka-enabled", havingValue = "true")
public class FinanceKafkaListenerStarter {
    private static final Logger log = LogManager.getLogger(FinanceKafkaListenerStarter.class);

    private final KafkaListenerEndpointRegistry registry;
    private final boolean autoStartup;

    public FinanceKafkaListenerStarter(
            KafkaListenerEndpointRegistry registry,
            @Value("${eprocure.finance.integration.kafka-auto-startup:true}") boolean autoStartup) {
        this.registry = registry;
        this.autoStartup = autoStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startListenersAfterApplicationReady() {
        if (!autoStartup) {
            log.info("[KAFKA] Finance Kafka listener startup disabled by configuration");
            return;
        }
        CompletableFuture.runAsync(this::startListenersSafely);
    }

    private void startListenersSafely() {
        try {
            log.info("[KAFKA] Starting finance Kafka listeners");
            registry.start();
            log.info("[KAFKA] Finance Kafka listeners started");
        } catch (RuntimeException exception) {
            log.warn("[KAFKA] Finance Kafka listeners not started | reason={}", exception.getMessage());
        }
    }
}
