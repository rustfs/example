package com.rustfs.xfilestoragerustfs.storage;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.AmazonS3FileStorage;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {

    private static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final FileStorageService fileStorageService;
    private final StoragePresignProperties storagePresignProperties;

    public StorageService(FileStorageService fileStorageService, StoragePresignProperties storagePresignProperties) {
        this.fileStorageService = fileStorageService;
        this.storagePresignProperties = storagePresignProperties;
    }

    public UploadFileResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Upload file cannot be empty");
        }

        String path = LocalDate.now().format(PATH_FORMATTER) + "/";
        FileInfo fileInfo = fileStorageService.of(file)
                .setPath(path)
                .upload();

        if (fileInfo == null || !StringUtils.hasText(fileInfo.getUrl())) {
            throw new StorageException("File upload failed, please check S3 configuration");
        }
        return UploadFileResponse.from(fileInfo, tryGenerateUploadPresignedDownload(fileInfo));
    }

    public void downloadToStream(String url, OutputStream outputStream) {
        try {
            fileStorageService.download(url).outputStream(outputStream);
        } catch (Exception exception) {
            throw new StorageException("File download failed: " + url, exception);
        }
    }

    public FileInfo parseFileInfo(String url) {
        FileInfo fileInfo = fileStorageService.getFileInfoByUrl(url);
        if (fileInfo == null || !StringUtils.hasText(fileInfo.getFilename())) {
            throw new StorageException("Unable to parse file information: " + url);
        }
        return fileInfo;
    }

    public PresignedDownloadResponse generatePresignedDownloadUrl(String url, boolean attachment, Long expireSeconds) {
        FileInfo fileInfo = parseFileInfo(url);
        return generatePresignedDownloadUrl(fileInfo, attachment, expireSeconds);
    }

    private PresignedDownloadResponse generatePresignedDownloadUrl(FileInfo fileInfo, boolean attachment, Long expireSeconds) {
        AmazonS3FileStorage amazonS3FileStorage = resolveAmazonS3FileStorage(fileInfo);
        String objectKey = buildObjectKey(amazonS3FileStorage, fileInfo);
        long normalizedExpireSeconds = normalizeExpireSeconds(expireSeconds);
        Instant expiresAt = Instant.now().plusSeconds(normalizedExpireSeconds);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                amazonS3FileStorage.getBucketName(),
                objectKey,
                HttpMethod.GET
        );
        request.setExpiration(Date.from(expiresAt));
        request.setResponseHeaders(buildResponseHeaders(fileInfo, attachment));

        AmazonS3 amazonS3 = amazonS3FileStorage.getClient();
        URL presignedUrl = amazonS3.generatePresignedUrl(request);
        return new PresignedDownloadResponse(
                presignedUrl.toString(),
                expiresAt,
                normalizedExpireSeconds,
                resolveFilename(fileInfo),
                resolveContentType(fileInfo)
        );
    }

    private PresignedDownloadResponse tryGenerateUploadPresignedDownload(FileInfo fileInfo) {
        try {
            return generatePresignedDownloadUrl(fileInfo, true, null);
        } catch (Exception exception) {
            return null;
        }
    }

    private AmazonS3FileStorage resolveAmazonS3FileStorage(FileInfo fileInfo) {
        FileStorage fileStorage = fileStorageService.getFileStorageVerify(fileInfo);
        if (fileStorage instanceof AmazonS3FileStorage amazonS3FileStorage) {
            return amazonS3FileStorage;
        }
        throw new StorageException("The current file storage platform does not support Amazon S3 presigned downloads");
    }

    private String buildObjectKey(AmazonS3FileStorage amazonS3FileStorage, FileInfo fileInfo) {
        StringBuilder builder = new StringBuilder();
        appendPathSegment(builder, amazonS3FileStorage.getBasePath());
        appendPathSegment(builder, fileInfo.getPath());
        if (!StringUtils.hasText(fileInfo.getFilename())) {
            throw new StorageException("File is missing a storage filename, cannot generate a presigned download address");
        }
        builder.append(fileInfo.getFilename());
        return builder.toString();
    }

    private void appendPathSegment(StringBuilder builder, String segment) {
        if (!StringUtils.hasText(segment)) {
            return;
        }
        String normalized = segment.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '/') {
            builder.append('/');
        }
        builder.append(normalized).append('/');
    }

    private long normalizeExpireSeconds(Long expireSeconds) {
        long resolved = expireSeconds == null ? storagePresignProperties.getExpireSeconds() : expireSeconds;
        if (resolved <= 0) {
            throw new StorageException("expireSeconds must be greater than 0");
        }
        long maxExpireSeconds = storagePresignProperties.getMaxExpireSeconds();
        if (maxExpireSeconds > 0 && resolved > maxExpireSeconds) {
            throw new StorageException("expireSeconds cannot be greater than " + maxExpireSeconds);
        }
        return resolved;
    }

    private ResponseHeaderOverrides buildResponseHeaders(FileInfo fileInfo, boolean attachment) {
        ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
        String filename = resolveFilename(fileInfo);
        String dispositionType = attachment ? "attachment" : "inline";
        overrides.setContentDisposition(dispositionType + "; filename*=UTF-8''" + encodeFilename(filename));
        if (StringUtils.hasText(fileInfo.getContentType())) {
            overrides.setContentType(fileInfo.getContentType());
        }
        return overrides;
    }

    private String resolveFilename(FileInfo fileInfo) {
        if (StringUtils.hasText(fileInfo.getOriginalFilename())) {
            return fileInfo.getOriginalFilename();
        }
        return fileInfo.getFilename();
    }

    private String resolveContentType(FileInfo fileInfo) {
        if (StringUtils.hasText(fileInfo.getContentType())) {
            return fileInfo.getContentType();
        }
        return "application/octet-stream";
    }

    private String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
