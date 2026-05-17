package com.eprocure.iam.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PasswordHistoryMapper {
    @Select("""
            SELECT password_hash
            FROM iam.password_history
            WHERE user_id = #{userId}
              AND is_deleted = FALSE
            ORDER BY changed_at DESC, created_at DESC
            LIMIT #{limit}
            """)
    List<String> findRecentHashesByUserId(@Param("userId") UUID userId, @Param("limit") int limit);

    @Insert("""
            INSERT INTO iam.password_history (
                id, user_id, password_hash, changed_at, created_by
            ) VALUES (
                gen_random_uuid(), #{userId}, #{passwordHash}, #{changedAt}, #{actorId}
            )
            """)
    void insert(
            @Param("userId") UUID userId,
            @Param("passwordHash") String passwordHash,
            @Param("actorId") UUID actorId,
            @Param("changedAt") Instant changedAt);
}
