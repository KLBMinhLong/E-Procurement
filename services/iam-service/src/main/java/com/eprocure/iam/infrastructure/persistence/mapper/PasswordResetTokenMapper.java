package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.PasswordResetTokenDbEntity;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordResetTokenMapper {
    @Insert("""
            INSERT INTO iam.password_reset_tokens (
                id, user_id, token_hash, expires_at, used_at, created_at, created_by
            ) VALUES (
                #{token.id}, #{token.userId}, #{token.tokenHash}, #{token.expiresAt},
                #{token.usedAt}, #{token.createdAt}, #{actorId}
            )
            """)
    void insert(@Param("token") PasswordResetTokenDbEntity token, @Param("actorId") UUID actorId);

    @Select("""
            SELECT id, user_id, token_hash, expires_at, used_at, created_at
            FROM iam.password_reset_tokens
            WHERE token_hash = #{tokenHash}
              AND used_at IS NULL
              AND expires_at > #{now}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    PasswordResetTokenDbEntity findActiveByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now);

    @Update("""
            UPDATE iam.password_reset_tokens
            SET used_at = #{usedAt},
                updated_by = #{actorId}
            WHERE id = #{tokenId}
              AND used_at IS NULL
              AND is_deleted = FALSE
            """)
    void markUsed(
            @Param("tokenId") UUID tokenId,
            @Param("actorId") UUID actorId,
            @Param("usedAt") Instant usedAt);

    @Update("""
            UPDATE iam.password_reset_tokens
            SET is_deleted = TRUE,
                deleted_at = #{revokedAt},
                deleted_by = #{actorId},
                updated_by = #{actorId}
            WHERE user_id = #{userId}
              AND used_at IS NULL
              AND expires_at > #{revokedAt}
              AND is_deleted = FALSE
            """)
    void revokeActiveByUserId(
            @Param("userId") UUID userId,
            @Param("actorId") UUID actorId,
            @Param("revokedAt") Instant revokedAt);
}
