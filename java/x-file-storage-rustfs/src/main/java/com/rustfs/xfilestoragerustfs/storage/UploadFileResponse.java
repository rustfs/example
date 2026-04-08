package com.rustfs.xfilestoragerustfs.storage;

import org.dromara.x.file.storage.core.FileInfo;

public record UploadFileResponse(
        String url,
        String filename,
        String originalFilename,
        String contentType,
        Long size,
        String platform,
        String path,
        PresignedDownloadResponse presignedDownload
) {

    public static UploadFileResponse from(FileInfo fileInfo) {
        return from(fileInfo, null);
    }

    public static UploadFileResponse from(FileInfo fileInfo, PresignedDownloadResponse presignedDownload) {
        return new UploadFileResponse(
                fileInfo.getUrl(),
                fileInfo.getFilename(),
                fileInfo.getOriginalFilename(),
                fileInfo.getContentType(),
                fileInfo.getSize(),
                fileInfo.getPlatform(),
                fileInfo.getPath(),
                presignedDownload
        );
    }
}

