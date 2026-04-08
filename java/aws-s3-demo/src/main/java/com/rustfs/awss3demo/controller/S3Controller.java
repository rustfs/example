package com.rustfs.awss3demo.controller;

import com.amazonaws.services.s3.model.S3Object;
import com.rustfs.awss3demo.common.ApiResponse;
import com.rustfs.awss3demo.service.S3Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * S3 File Operation REST API
 * <pre>
 * POST   /api/s3/upload           Upload a single file
 * POST   /api/s3/upload/batch     Batch upload files
 * GET    /api/s3/download.jpg/{key}   Directly download file (streaming)
 * GET    /api/s3/presign/{key}    Get presigned download URL
 * GET    /api/s3/list             List files
 * DELETE /api/s3/delete/{key}     Delete file
 * </pre>
 */
@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Upload a single file
     * <p>Optional parameter key: Specify the storage path. If not passed, it will be automatically generated (timestamp_originalFilename)</p>
     */
    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "key", required = false) String key) throws IOException {

        if (file.isEmpty()) {
            return ApiResponse.fail("File cannot be empty");
        }

        String storedKey = StringUtils.hasText(key)
                ? s3Service.uploadFile(file, key)
                : s3Service.uploadFile(file);

        return ApiResponse.success("File uploaded successfully", Map.of(
                "key", storedKey,
                "bucket", s3Service.getBucketName(),
                "originalFilename", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                "size", String.valueOf(file.getSize())
        ));
    }

    /**
     * Batch upload files
     */
    @PostMapping("/upload/batch")
    public ApiResponse<Map<String, Object>> uploadBatch(
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        if (files == null || files.isEmpty()) {
            return ApiResponse.fail("File list cannot be empty");
        }

        List<String> keys = s3Service.uploadFiles(files);
        return ApiResponse.success("Batch upload successful", Map.of(
                "count", keys.size(),
                "keys", keys
        ));
    }

    /**
     * Directly download file (returns a stream, suitable for small files)
     *
     * @param key File key, supports path, such as images/photo.png (separated by "/" in the URL)
     */
    @GetMapping("/download/**")
    public ResponseEntity<byte[]> download(@RequestParam("key") String key) throws IOException {
        S3Object s3Object = s3Service.downloadFile(key);

        byte[] content = s3Object.getObjectContent().readAllBytes();
        String contentType = s3Object.getObjectMetadata().getContentType();
        String filename = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(content.length)
                .body(content);
    }

    /**
     * Get presigned download URL
     *
     * @param key         File key
     * @param expiresMins Expiration time (minutes), default is 60
     */
    @GetMapping("/presign")
    public ApiResponse<Map<String, Object>> presign(
            @RequestParam("key") String key,
            @RequestParam(value = "expiresMins", defaultValue = "60") int expiresMins) {

        String url = s3Service.generatePresignedUrl(key, expiresMins);
        return ApiResponse.success(Map.of(
                "url", url,
                "key", key,
                "expiresMins", expiresMins
        ));
    }

    /**
     * List files in the Bucket
     *
     * @param prefix Prefix filter, empty by default (list all)
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(value = "prefix", defaultValue = "") String prefix) {

        List<Map<String, Object>> objects = s3Service.listObjects(prefix);
        return ApiResponse.success(objects);
    }

    /**
     * Delete file
     *
     * @param key File key
     */
    @DeleteMapping("/delete")
    public ApiResponse<String> delete(@RequestParam("key") String key) {
        s3Service.deleteFile(key);
        return ApiResponse.success("File deleted successfully", key);
    }
}
