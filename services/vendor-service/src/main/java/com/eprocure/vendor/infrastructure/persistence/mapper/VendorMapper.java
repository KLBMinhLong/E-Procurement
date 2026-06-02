package com.eprocure.vendor.infrastructure.persistence.mapper;

import com.eprocure.vendor.domain.repository.VendorFilter;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorContactDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorDbEntity;
import com.eprocure.vendor.infrastructure.persistence.entity.VendorScoreDbEntity;
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
public interface VendorMapper {

    @Select("""
            SELECT 'VND-' || LPAD(nextval('vendor.vendor_code_seq')::text, 3, '0')
            """)
    String nextVendorCode();

    @Select("""
            SELECT EXISTS (
                SELECT 1
                FROM vendor.vendors
                WHERE lower(tax_code) = lower(#{taxCode})
                  AND is_deleted = FALSE
            )
            """)
    boolean existsByTaxCode(@Param("taxCode") String taxCode);

    Optional<VendorDbEntity> findVendorById(@Param("id") UUID id);

    Optional<VendorDbEntity> findVendorByIdempotencyKey(@Param("idempotencyKey") UUID idempotencyKey);

    List<VendorDbEntity> findByFilter(@Param("filter") VendorFilter filter);

    long countByFilter(@Param("filter") VendorFilter filter);

    @Select("""
            SELECT
                id,
                vendor_id,
                name,
                role_name AS role,
                email,
                phone,
                is_primary AS primary,
                created_by
            FROM vendor.vendor_contacts
            WHERE vendor_id = #{vendorId}
              AND is_deleted = FALSE
            ORDER BY is_primary DESC, name ASC
            """)
    @Results(id = "vendorContactResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "vendorId", column = "vendor_id"),
            @Result(property = "name", column = "name"),
            @Result(property = "role", column = "role"),
            @Result(property = "email", column = "email"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "primary", column = "primary"),
            @Result(property = "createdBy", column = "created_by")
    })
    List<VendorContactDbEntity> findContactsByVendorId(@Param("vendorId") UUID vendorId);

    @Select("""
            SELECT
                quality_score,
                delivery_score,
                price_score,
                responsiveness_score,
                overall_score,
                last_evaluated_at,
                total_orders,
                on_time_delivery_rate
            FROM vendor.vendor_scores
            WHERE vendor_id = #{vendorId}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    @Results(id = "vendorScoreResult", value = {
            @Result(property = "qualityScore", column = "quality_score"),
            @Result(property = "deliveryScore", column = "delivery_score"),
            @Result(property = "priceScore", column = "price_score"),
            @Result(property = "responsivenessScore", column = "responsiveness_score"),
            @Result(property = "overallScore", column = "overall_score"),
            @Result(property = "lastEvaluatedAt", column = "last_evaluated_at"),
            @Result(property = "totalOrders", column = "total_orders"),
            @Result(property = "onTimeDeliveryRate", column = "on_time_delivery_rate")
    })
    Optional<VendorScoreDbEntity> findScoreByVendorId(@Param("vendorId") UUID vendorId);

    @Insert("""
            INSERT INTO vendor.vendors (
                id, vendor_code, name, tax_code, email, phone,
                address_street, address_district, address_city, address_country,
                categories, status, is_on_approved_vendor_list, notes,
                approved_by, approved_at, idempotency_key, created_at, created_by, updated_by
            ) VALUES (
                #{entity.id},
                #{entity.vendorCode},
                #{entity.name},
                #{entity.taxCode},
                #{entity.email},
                #{entity.phone},
                #{entity.addressStreet},
                #{entity.addressDistrict},
                #{entity.addressCity},
                #{entity.addressCountry},
                CAST(#{entity.categories, typeHandler=com.eprocure.vendor.infrastructure.persistence.typehandler.StringListJsonTypeHandler} AS jsonb),
                #{entity.status},
                #{entity.onApprovedVendorList},
                #{entity.notes},
                #{entity.approvedBy},
                #{entity.approvedAt},
                #{entity.idempotencyKey},
                #{entity.createdAt},
                #{entity.createdBy},
                #{entity.updatedBy}
            )
            """)
    void insertVendor(@Param("entity") VendorDbEntity entity);

    @Insert("""
            INSERT INTO vendor.vendor_contacts (
                id, vendor_id, name, role_name, email, phone, is_primary, created_by
            ) VALUES (
                #{entity.id},
                #{entity.vendorId},
                #{entity.name},
                #{entity.role},
                #{entity.email},
                #{entity.phone},
                #{entity.primary},
                #{entity.createdBy}
            )
            """)
    void insertContact(@Param("entity") VendorContactDbEntity entity);

    @Update("""
            UPDATE vendor.vendors
            SET status = #{entity.status},
                is_on_approved_vendor_list = #{entity.onApprovedVendorList},
                approved_by = #{entity.approvedBy},
                approved_at = #{entity.approvedAt},
                updated_by = #{entity.updatedBy}
            WHERE id = #{entity.id}
              AND is_deleted = FALSE
            """)
    int updateApproval(@Param("entity") VendorDbEntity entity);
}
