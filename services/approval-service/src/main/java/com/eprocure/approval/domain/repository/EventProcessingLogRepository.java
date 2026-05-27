package com.eprocure.approval.domain.repository;

public interface EventProcessingLogRepository {
    boolean existsByEventId(String eventId);

    void markProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);

    void markSkipped(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName);
}
