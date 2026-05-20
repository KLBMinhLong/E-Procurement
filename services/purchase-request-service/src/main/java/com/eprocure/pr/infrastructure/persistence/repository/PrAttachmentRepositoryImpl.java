package com.eprocure.pr.infrastructure.persistence.repository;

import com.eprocure.pr.domain.model.PrAttachment;
import com.eprocure.pr.domain.repository.PrAttachmentRepository;
import com.eprocure.pr.infrastructure.persistence.entity.PrAttachmentDbEntity;
import com.eprocure.pr.infrastructure.persistence.mapper.PrAttachmentMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PrAttachmentRepositoryImpl implements PrAttachmentRepository {

    private final PrAttachmentMapper mapper;

    public PrAttachmentRepositoryImpl(PrAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(PrAttachment attachment) {
        PrAttachmentDbEntity entity = new PrAttachmentDbEntity();
        entity.setId(attachment.getId());
        entity.setPrId(attachment.getPrId().orElse(null));
        entity.setFileName(attachment.getFileName());
        entity.setFilePath(attachment.getFilePath());
        entity.setFileSize(attachment.getFileSize());
        entity.setMimeType(attachment.getMimeType());
        entity.setUploadedBy(attachment.getUploadedBy());
        entity.setUploadedAt(attachment.getUploadedAt());
        entity.setDeleted(attachment.isDeleted());
        entity.setDeletedAt(attachment.getDeletedAt().orElse(null));

        mapper.insert(entity);
    }
}
