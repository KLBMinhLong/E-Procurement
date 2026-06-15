package com.eprocure.iam.infrastructure.persistence.mapper;

import com.eprocure.iam.infrastructure.persistence.entity.SessionDbEntity;
import com.eprocure.iam.infrastructure.persistence.entity.ActiveSessionDbEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SessionMapper {
    @Insert("""
            INSERT INTO iam.sessions (
                id, user_id, token_hash, ip_address, user_agent,
                issued_at, expires_at, is_revoked, created_by, updated_at
            )
            VALUES (
                #{session.id}, #{session.userId}, #{session.tokenHash},
                CAST(#{session.ipAddress,jdbcType=VARCHAR} AS INET), #{session.userAgent},
                #{session.issuedAt}, #{session.expiresAt}, #{session.revoked}, #{session.userId}, NOW()
            )
            """)
    void save(@Param("session") SessionDbEntity session);

    @Update("""
            UPDATE iam.sessions
            SET is_revoked = TRUE,
                revoked_at = #{revokedAt},
                revoked_by = #{revokedBy},
                updated_at = NOW()
            WHERE user_id = #{userId}
              AND is_revoked = FALSE
              AND expires_at > #{revokedAt}
              AND is_deleted = FALSE
            """)
    void revokeActiveByUserId(
            @Param("userId") UUID userId,
            @Param("revokedBy") UUID revokedBy,
            @Param("revokedAt") Instant revokedAt);

    @Update("""
            UPDATE iam.sessions
            SET is_revoked = TRUE,
                revoked_at = #{revokedAt},
                revoked_by = #{revokedBy},
                updated_at = NOW()
            WHERE token_hash = #{tokenHash}
              AND is_revoked = FALSE
              AND is_deleted = FALSE
            """)
    void revokeByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("revokedBy") UUID revokedBy,
            @Param("revokedAt") Instant revokedAt);

    @Update("""
            UPDATE iam.sessions
            SET is_revoked = TRUE,
                revoked_at = #{revokedAt},
                revoked_by = #{revokedBy},
                updated_at = NOW()
            WHERE id = #{sessionId}
              AND is_revoked = FALSE
              AND is_deleted = FALSE
            """)
    void revokeById(
            @Param("sessionId") UUID sessionId,
            @Param("revokedBy") UUID revokedBy,
            @Param("revokedAt") Instant revokedAt);

    @Select("""
            SELECT id, user_id, token_hash, ip_address, user_agent,
                   issued_at, expires_at, is_revoked AS revoked, revoked_at, revoked_by
            FROM iam.sessions
            WHERE token_hash = #{tokenHash}
              AND is_revoked = FALSE
              AND expires_at > #{now}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    SessionDbEntity findActiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Select("""
            SELECT id, user_id, token_hash, ip_address, user_agent,
                   issued_at, expires_at, is_revoked AS revoked, revoked_at, revoked_by
            FROM iam.sessions
            WHERE id = #{sessionId}
              AND is_revoked = FALSE
              AND expires_at > #{now}
              AND is_deleted = FALSE
            LIMIT 1
            """)
    SessionDbEntity findActiveById(@Param("sessionId") UUID sessionId, @Param("now") Instant now);

    @Select("""
            SELECT token_hash
            FROM iam.sessions
            WHERE user_id = #{userId}
              AND is_revoked = FALSE
              AND expires_at > #{now}
              AND is_deleted = FALSE
            ORDER BY issued_at DESC
            """)
    List<String> findActiveTokenHashesByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Select("""
            <script>
            SELECT s.id AS session_id,
                   s.user_id,
                   u.username,
                   u.full_name,
                   s.ip_address::TEXT AS ip_address,
                   s.user_agent,
                   s.issued_at,
                   GREATEST(s.issued_at, s.updated_at) AS last_activity_at,
                   s.expires_at,
                   COUNT(*) OVER() AS total_elements
            FROM iam.sessions s
            JOIN iam.users u ON u.id = s.user_id AND u.is_deleted = FALSE
            WHERE s.is_revoked = FALSE
              AND s.expires_at > #{now}
              AND s.is_deleted = FALSE
            <if test="userId != null">
              AND s.user_id = #{userId}
            </if>
            ORDER BY s.issued_at DESC, s.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ActiveSessionDbEntity> findActivePage(
            @Param("userId") UUID userId,
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("now") Instant now);
}
