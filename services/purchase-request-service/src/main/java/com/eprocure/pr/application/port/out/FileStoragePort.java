package com.eprocure.pr.application.port.out;

import java.io.InputStream;

public interface FileStoragePort {
    /**
     * Stores a file and returns its stored path/identifier.
     */
    String storeFile(String originalFilename, InputStream content, String mimeType);
}
