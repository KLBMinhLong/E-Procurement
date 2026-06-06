package com.eprocure.finance.infrastructure.persistence.mapper;

import com.eprocure.finance.domain.model.PoPrConversionCallbackStatus;
import com.eprocure.finance.infrastructure.persistence.entity.PoPrConversionCallbackDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PoPrConversionCallbackMapper {
    @Insert("""
            INSERT INTO finance.po_pr_conversion_callbacks (
                id, po_id, pr_id, idempotency_key, status, attempts,
                next_retry_at, delivered_at, last_error, created_at
            ) VALUES (
                #{entity.id}, #{entity.poId}, #{entity.prId}, #{entity.idempotencyKey},
                #{entity.status}, #{entity.attempts}, #{entity.nextRetryAt},
                #{entity.deliveredAt}, #{entity.lastError}, #{entity.createdAt}
            )
            """)
    void insert(@Param("entity") PoPrConversionCallbackDbEntity entity);

    @Select("""
            SELECT id, po_id, pr_id, idempotency_key, status, attempts,
                   next_retry_at, delivered_at, last_error, created_at
            FROM finance.po_pr_conversion_callbacks
            WHERE id = #{callbackId}
            """)
    Optional<PoPrConversionCallbackDbEntity> findById(@Param("callbackId") UUID callbackId);

    @Select("""
            SELECT id, po_id, pr_id, idempotency_key, status, attempts,
                   next_retry_at, delivered_at, last_error, created_at
            FROM finance.po_pr_conversion_callbacks
            WHERE po_id = #{poId}
            """)
    Optional<PoPrConversionCallbackDbEntity> findByPoId(@Param("poId") UUID poId);

    @Select("""
            SELECT status
            FROM finance.po_pr_conversion_callbacks
            WHERE po_id = #{poId}
            """)
    Optional<PoPrConversionCallbackStatus> findStatusByPoId(@Param("poId") UUID poId);

    List<PoPrConversionCallbackDbEntity> findByPoIds(@Param("poIds") List<UUID> poIds);

    @Select("""
            SELECT id, po_id, pr_id, idempotency_key, status, attempts,
                   next_retry_at, delivered_at, last_error, created_at
            FROM finance.po_pr_conversion_callbacks
            WHERE status IN ('PENDING', 'FAILED_RETRYABLE')
              AND (next_retry_at IS NULL OR next_retry_at <= #{now})
            ORDER BY created_at ASC
            LIMIT #{limit}
            """)
    List<PoPrConversionCallbackDbEntity> findDispatchable(
            @Param("now") Instant now,
            @Param("limit") int limit);

    @Update("""
            UPDATE finance.po_pr_conversion_callbacks
            SET status = 'DELIVERED',
                delivered_at = #{deliveredAt},
                last_error = NULL
            WHERE id = #{callbackId}
            """)
    void markDelivered(@Param("callbackId") UUID callbackId, @Param("deliveredAt") Instant deliveredAt);

    @Update("""
            UPDATE finance.po_pr_conversion_callbacks
            SET status = 'FAILED_RETRYABLE',
                attempts = #{attempts},
                next_retry_at = #{nextRetryAt},
                last_error = #{lastError}
            WHERE id = #{callbackId}
            """)
    void markRetryable(
            @Param("callbackId") UUID callbackId,
            @Param("attempts") int attempts,
            @Param("nextRetryAt") Instant nextRetryAt,
            @Param("lastError") String lastError);

    @Update("""
            UPDATE finance.po_pr_conversion_callbacks
            SET status = 'FAILED_EXHAUSTED',
                attempts = #{attempts},
                next_retry_at = NULL,
                last_error = #{lastError}
            WHERE id = #{callbackId}
            """)
    void markExhausted(
            @Param("callbackId") UUID callbackId,
            @Param("attempts") int attempts,
            @Param("lastError") String lastError);
}
