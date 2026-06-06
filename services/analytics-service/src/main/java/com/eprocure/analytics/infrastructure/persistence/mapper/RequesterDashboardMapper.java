package com.eprocure.analytics.infrastructure.persistence.mapper;

import com.eprocure.analytics.infrastructure.persistence.entity.RecentPurchaseRequestDbEntity;
import com.eprocure.analytics.infrastructure.persistence.entity.RequesterPrStatsDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RequesterDashboardMapper {
    @Select("""
            SELECT
                COUNT(pr.pr_id)::INTEGER AS submitted
            FROM analytics.pr_submitted_projections pr
            WHERE pr.is_deleted = FALSE
              AND pr.requester_id = #{requesterId}
            """)
    Optional<RequesterPrStatsDbEntity> findPrStats(@Param("requesterId") UUID requesterId);

    @Select("""
            SELECT
                pr.pr_number,
                pr.priority,
                pr.total_amount,
                pr.currency,
                pr.submitted_at
            FROM analytics.pr_submitted_projections pr
            WHERE pr.is_deleted = FALSE
              AND pr.requester_id = #{requesterId}
            ORDER BY pr.submitted_at DESC
            LIMIT 10
            """)
    List<RecentPurchaseRequestDbEntity> findRecentPrs(@Param("requesterId") UUID requesterId);
}
