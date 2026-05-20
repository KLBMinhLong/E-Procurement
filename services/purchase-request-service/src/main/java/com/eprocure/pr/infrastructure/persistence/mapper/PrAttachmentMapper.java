package com.eprocure.pr.infrastructure.persistence.mapper;

import com.eprocure.pr.infrastructure.persistence.entity.PrAttachmentDbEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrAttachmentMapper {

    @Insert("""
        INSERT INTO pr.pr_attachments (
            id, pr_id, file_name, file_path, file_size, mime_type, uploaded_by, uploaded_at, is_deleted, deleted_at
        ) VALUES (
            #{id}, #{prId}, #{fileName}, #{filePath}, #{fileSize}, #{mimeType}, #{uploadedBy}, #{uploadedAt}, #{isDeleted}, #{deletedAt}
        )
    """)
    void insert(PrAttachmentDbEntity entity);
}
