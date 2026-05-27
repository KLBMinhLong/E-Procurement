package com.eprocure.approval.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EventProcessingLogMapper {
    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM approval.event_processing_log
                WHERE event_id = #{eventId}
            )
            """)
    boolean existsByEventId(@Param("eventId") String eventId);

    @Insert("""
            INSERT INTO approval.event_processing_log (
                event_id,
                topic,
                partition_id,
                offset_value,
                handler_name,
                status
            ) VALUES (
                #{eventId},
                #{topic},
                #{partitionId},
                #{offsetValue},
                #{handlerName},
                #{status}
            )
            ON CONFLICT (event_id) DO NOTHING
            """)
    void insert(
            @Param("eventId") String eventId,
            @Param("topic") String topic,
            @Param("partitionId") Integer partitionId,
            @Param("offsetValue") Long offsetValue,
            @Param("handlerName") String handlerName,
            @Param("status") String status);
}
