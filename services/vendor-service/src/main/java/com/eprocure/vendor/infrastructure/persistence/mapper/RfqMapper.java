package com.eprocure.vendor.infrastructure.persistence.mapper;

import com.eprocure.vendor.domain.repository.RfqFilter;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqInvitationDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.RfqLineItemDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RfqMapper {

    @Select("""
            SELECT 'RFQ-' || to_char(current_date, 'YYYY-MM') || '-' || LPAD(nextval('vendor.rfq_number_seq')::text, 5, '0')
            """)
    String nextRfqNumber();

    Optional<RfqDbEntity> findRfqById(@Param("id") UUID id);

    Optional<RfqDbEntity> findRfqByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    List<RfqDbEntity> findByFilter(@Param("filter") RfqFilter filter);

    long countByFilter(@Param("filter") RfqFilter filter);

    @Select("""
            SELECT
                id,
                rfq_id,
                pr_line_item_id,
                item_name,
                category_code,
                quantity,
                unit,
                specifications,
                created_by
            FROM vendor.rfq_line_items
            WHERE rfq_id = #{rfqId}
              AND is_deleted = FALSE
            ORDER BY created_at ASC, id ASC
            """)
    @Results(id = "rfqLineItemResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "rfqId", column = "rfq_id"),
            @Result(property = "prLineItemId", column = "pr_line_item_id"),
            @Result(property = "itemName", column = "item_name"),
            @Result(property = "categoryCode", column = "category_code"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "unit", column = "unit"),
            @Result(property = "specifications", column = "specifications"),
            @Result(property = "createdBy", column = "created_by")
    })
    List<RfqLineItemDbEntity> findLineItemsByRfqId(@Param("rfqId") UUID rfqId);

    @Select("""
            SELECT
                id,
                rfq_id,
                vendor_id,
                vendor_name,
                invited_at,
                has_submitted,
                submitted_at,
                created_by
            FROM vendor.rfq_invitations
            WHERE rfq_id = #{rfqId}
              AND is_deleted = FALSE
            ORDER BY invited_at ASC, vendor_name ASC
            """)
    @Results(id = "rfqInvitationResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "rfqId", column = "rfq_id"),
            @Result(property = "vendorId", column = "vendor_id"),
            @Result(property = "vendorName", column = "vendor_name"),
            @Result(property = "invitedAt", column = "invited_at"),
            @Result(property = "hasSubmitted", column = "has_submitted"),
            @Result(property = "submittedAt", column = "submitted_at"),
            @Result(property = "createdBy", column = "created_by")
    })
    List<RfqInvitationDbEntity> findInvitationsByRfqId(@Param("rfqId") UUID rfqId);

    @Insert("""
            INSERT INTO vendor.rfqs (
                id, rfq_number, pr_id, pr_number, title, status, submission_deadline,
                requirements, awarded_vendor_id, awarded_quote_id, award_reason,
                closed_at, idempotency_key, created_at, created_by, updated_by
            ) VALUES (
                #{entity.id},
                #{entity.rfqNumber},
                #{entity.prId},
                #{entity.prNumber},
                #{entity.title},
                #{entity.status},
                #{entity.submissionDeadline},
                #{entity.requirements},
                #{entity.awardedVendorId},
                #{entity.awardedQuoteId},
                #{entity.awardReason},
                #{entity.closedAt},
                #{entity.idempotencyKey},
                #{entity.createdAt},
                #{entity.createdBy},
                #{entity.updatedBy}
            )
            """)
    void insertRfq(@Param("entity") RfqDbEntity entity);

    @Insert("""
            INSERT INTO vendor.rfq_line_items (
                id, rfq_id, pr_line_item_id, item_name, category_code,
                quantity, unit, specifications, created_by
            ) VALUES (
                #{entity.id},
                #{entity.rfqId},
                #{entity.prLineItemId},
                #{entity.itemName},
                #{entity.categoryCode},
                #{entity.quantity},
                #{entity.unit},
                #{entity.specifications},
                #{entity.createdBy}
            )
            """)
    void insertLineItem(@Param("entity") RfqLineItemDbEntity entity);

    @Insert("""
            INSERT INTO vendor.rfq_invitations (
                id, rfq_id, vendor_id, vendor_name, invited_at,
                has_submitted, submitted_at, created_by
            ) VALUES (
                #{entity.id},
                #{entity.rfqId},
                #{entity.vendorId},
                #{entity.vendorName},
                #{entity.invitedAt},
                #{entity.hasSubmitted},
                #{entity.submittedAt},
                #{entity.createdBy}
            )
            """)
    void insertInvitation(@Param("entity") RfqInvitationDbEntity entity);

    @Update("""
            UPDATE vendor.rfqs
            SET status = #{entity.status},
                closed_at = #{entity.closedAt},
                awarded_vendor_id = #{entity.awardedVendorId},
                awarded_quote_id = #{entity.awardedQuoteId},
                award_reason = #{entity.awardReason},
                updated_by = #{entity.updatedBy}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    int updateStatus(@Param("entity") RfqDbEntity entity);

    @Update("""
            UPDATE vendor.rfq_invitations
            SET has_submitted = TRUE,
                submitted_at = #{submittedAt},
                updated_by = #{actorId}
            WHERE rfq_id = #{rfqId}
              AND vendor_id = #{vendorId}
              AND is_deleted = FALSE
            """)
    int markInvitationSubmitted(
            @Param("rfqId") UUID rfqId,
            @Param("vendorId") UUID vendorId,
            @Param("submittedAt") java.time.Instant submittedAt,
            @Param("actorId") UUID actorId);
}
