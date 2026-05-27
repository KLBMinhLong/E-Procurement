package com.eprocure.approval.infrastructure.persistence.repository;

import com.eprocure.approval.domain.repository.EventProcessingLogRepository;
import com.eprocure.approval.infrastructure.persistence.mapper.EventProcessingLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EventProcessingLogRepositoryImpl implements EventProcessingLogRepository {
    private final EventProcessingLogMapper mapper;

    public EventProcessingLogRepositoryImpl(EventProcessingLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return mapper.existsByEventId(eventId);
    }

    @Override
    public void markProcessed(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        mapper.insert(eventId, topic, partitionId, offsetValue, handlerName, "PROCESSED");
    }

    @Override
    public void markSkipped(String eventId, String topic, Integer partitionId, Long offsetValue, String handlerName) {
        mapper.insert(eventId, topic, partitionId, offsetValue, handlerName, "SKIPPED");
    }
}
