package com.rustfs.xfilestoragerustfs.storage;

import java.time.Instant;

public record PresignedDownloadResponse(
        String url,
        Instant expiresAt,
        long expireSeconds,
        String filename,
        String contentType
) {
}

