package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.domain.model.DelegationStatus;
import com.eprocure.iam.infrastructure.persistence.entity.DelegationDbEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DelegationMapper {
    DelegationDbEntity findById(@Param("id") UUID id);

    List<DelegationDbEntity> findByDelegatorId(@Param("delegatorId") UUID delegatorId);

    DelegationDbEntity findActiveForApproval(
            @Param("delegatorId") UUID delegatorId,
            @Param("requesterDepartmentId") UUID requesterDepartmentId,
            @Param("totalAmount") BigDecimal totalAmount,
            @Param("currency") String currency,
            @Param("categories") List<String> categories,
            @Param("effectiveAt") Instant effectiveAt);

    boolean hasActiveOverlap(
            @Param("delegatorId") UUID delegatorId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt);

    String findOrgPathByUserId(@Param("userId") UUID userId);

    void insert(@Param("entity") DelegationDbEntity entity, @Param("actorId") UUID actorId);

    void updateStatus(
            @Param("delegationId") UUID delegationId,
            @Param("status") DelegationStatus status,
            @Param("actorId") UUID actorId);
}
