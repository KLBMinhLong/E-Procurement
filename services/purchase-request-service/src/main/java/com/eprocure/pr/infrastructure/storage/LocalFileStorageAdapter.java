package com.eprocure.pr.infrastructure.storage;

import com.eprocure.pr.application.port.out.FileStoragePort;
import com.eprocure.pr.common.exception.BusinessException;
import com.eprocure.pr.common.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private static final Logger log = LogManager.getLogger(LocalFileStorageAdapter.class);

    private final Path rootLocation;

    public LocalFileStorageAdapter(@Value("${app.storage.attachment-dir:/tmp/eprocure/attachments}") String storageDir) {
        this.rootLocation = Paths.get(storageDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String storeFile(String originalFilename, InputStream content, String mimeType) {
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String storedFilename = UUID.randomUUID().toString() + ext;
        Path destinationFile = this.rootLocation.resolve(Paths.get(storedFilename)).normalize().toAbsolutePath();

        if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
            throw new SecurityException("Cannot store file outside current directory.");
        }

        try {
            Files.copy(content, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("[STORAGE] Stored file at {}", destinationFile);
            return destinationFile.toString();
        } catch (IOException e) {
            log.error("[STORAGE] Failed to store file", e);
            throw new BusinessException(ErrorCode.SYS_001);
        }
    }
}
