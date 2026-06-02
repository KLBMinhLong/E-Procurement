package com.eprocure.vendor.infrastructure.persistence.mapper;

import com.eprocure.vendor.infrastructure.persistence.entity.VendorQuoteDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorQuoteLineItemDbEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface VendorQuoteMapper {

    @Select("""
            SELECT
                id,
                rfq_id,
                vendor_id,
                vendor_name,
                total_amount,
                currency,
                valid_until,
                payment_terms,
                notes,
                submitted_at,
                evaluation_score,
                evaluation_note,
                evaluated_by,
                evaluated_at,
                idempotency_key,
                created_at,
                created_by,
                updated_by
            FROM vendor.vendor_quotes
            WHERE id = #{id}
              AND is_deleted = FALSE
            """)
    @Results(id = "vendorQuoteResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "rfqId", column = "rfq_id"),
            @Result(property = "vendorId", column = "vendor_id"),
            @Result(property = "vendorName", column = "vendor_name"),
            @Result(property = "totalAmount", column = "total_amount"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "validUntil", column = "valid_until"),
            @Result(property = "paymentTerms", column = "payment_terms"),
            @Result(property = "notes", column = "notes"),
            @Result(property = "submittedAt", column = "submitted_at"),
            @Result(property = "evaluationScore", column = "evaluation_score"),
            @Result(property = "evaluationNote", column = "evaluation_note"),
            @Result(property = "evaluatedBy", column = "evaluated_by"),
            @Result(property = "evaluatedAt", column = "evaluated_at"),
            @Result(property = "idempotencyKey", column = "idempotency_key"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "createdBy", column = "created_by"),
            @Result(property = "updatedBy", column = "updated_by")
    })
    Optional<VendorQuoteDbEntity> findQuoteById(@Param("id") UUID id);

    @Select("""
            SELECT
                id,
                rfq_id,
                vendor_id,
                vendor_name,
                total_amount,
                currency,
                valid_until,
                payment_terms,
                notes,
                submitted_at,
                evaluation_score,
                evaluation_note,
                evaluated_by,
                evaluated_at,
                idempotency_key,
                created_at,
                created_by,
                updated_by
            FROM vendor.vendor_quotes
            WHERE idempotency_key = #{idempotencyKey}
              AND is_deleted = FALSE
            """)
    @ResultMap("vendorQuoteResult")
    Optional<VendorQuoteDbEntity> findQuoteByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    @Select("""
            SELECT
                id,
                rfq_id,
                vendor_id,
                vendor_name,
                total_amount,
                currency,
                valid_until,
                payment_terms,
                notes,
                submitted_at,
                evaluation_score,
                evaluation_note,
                evaluated_by,
                evaluated_at,
                idempotency_key,
                created_at,
                created_by,
                updated_by
            FROM vendor.vendor_quotes
            WHERE rfq_id = #{rfqId}
              AND vendor_id = #{vendorId}
              AND is_deleted = FALSE
            """)
    @ResultMap("vendorQuoteResult")
    Optional<VendorQuoteDbEntity> findByRfqIdAndVendorId(
            @Param("rfqId") UUID rfqId,
            @Param("vendorId") UUID vendorId);

    @Select("""
            SELECT
                id,
                rfq_id,
                vendor_id,
                vendor_name,
                total_amount,
                currency,
                valid_until,
                payment_terms,
                notes,
                submitted_at,
                evaluation_score,
                evaluation_note,
                evaluated_by,
                evaluated_at,
                idempotency_key,
                created_at,
                created_by,
                updated_by
            FROM vendor.vendor_quotes
            WHERE rfq_id = #{rfqId}
              AND is_deleted = FALSE
            ORDER BY evaluation_score DESC NULLS LAST, total_amount ASC, submitted_at ASC
            """)
    @ResultMap("vendorQuoteResult")
    List<VendorQuoteDbEntity> findByRfqId(@Param("rfqId") UUID rfqId);

    @Select("""
            SELECT
                id,
                quote_id,
                rfq_line_item_id,
                item_name,
                quantity,
                unit_price,
                currency,
                total_price,
                delivery_days,
                warranty,
                created_by
            FROM vendor.vendor_quote_line_items
            WHERE quote_id = #{quoteId}
              AND is_deleted = FALSE
            ORDER BY created_at ASC, id ASC
            """)
    @Results(id = "vendorQuoteLineItemResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "quoteId", column = "quote_id"),
            @Result(property = "rfqLineItemId", column = "rfq_line_item_id"),
            @Result(property = "itemName", column = "item_name"),
            @Result(property = "quantity", column = "quantity"),
            @Result(property = "unitPrice", column = "unit_price"),
            @Result(property = "currency", column = "currency"),
            @Result(property = "totalPrice", column = "total_price"),
            @Result(property = "deliveryDays", column = "delivery_days"),
            @Result(property = "warranty", column = "warranty"),
            @Result(property = "createdBy", column = "created_by")
    })
    List<VendorQuoteLineItemDbEntity> findLineItemsByQuoteId(@Param("quoteId") UUID quoteId);

    @Insert("""
            INSERT INTO vendor.vendor_quotes (
                id, rfq_id, vendor_id, vendor_name, total_amount, currency, valid_until,
                payment_terms, notes, submitted_at, evaluation_score, evaluation_note,
                evaluated_by, evaluated_at, idempotency_key, created_at, created_by, updated_by
            ) VALUES (
                #{entity.id},
                #{entity.rfqId},
                #{entity.vendorId},
                #{entity.vendorName},
                #{entity.totalAmount},
                #{entity.currency},
                #{entity.validUntil},
                #{entity.paymentTerms},
                #{entity.notes},
                #{entity.submittedAt},
                #{entity.evaluationScore},
                #{entity.evaluationNote},
                #{entity.evaluatedBy},
                #{entity.evaluatedAt},
                #{entity.idempotencyKey},
                #{entity.createdAt},
                #{entity.createdBy},
                #{entity.updatedBy}
            )
            """)
    void insertQuote(@Param("entity") VendorQuoteDbEntity entity);

    @Insert("""
            INSERT INTO vendor.vendor_quote_line_items (
                id, quote_id, rfq_line_item_id, item_name, quantity,
                unit_price, currency, total_price, delivery_days, warranty, created_by
            ) VALUES (
                #{entity.id},
                #{entity.quoteId},
                #{entity.rfqLineItemId},
                #{entity.itemName},
                #{entity.quantity},
                #{entity.unitPrice},
                #{entity.currency},
                #{entity.totalPrice},
                #{entity.deliveryDays},
                #{entity.warranty},
                #{entity.createdBy}
            )
            """)
    void insertLineItem(@Param("entity") VendorQuoteLineItemDbEntity entity);

    @Update("""
            UPDATE vendor.vendor_quotes
            SET evaluation_score = #{entity.evaluationScore},
                evaluation_note = #{entity.evaluationNote},
                evaluated_by = #{entity.evaluatedBy},
                evaluated_at = #{entity.evaluatedAt},
                updated_by = #{entity.updatedBy}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    int updateEvaluation(@Param("entity") VendorQuoteDbEntity entity);
}
