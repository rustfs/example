package com.rustfs.awss3demo.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.rustfs.awss3demo.config.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public S3Service(AmazonS3 amazonS3, S3Properties s3Properties) {
        this.amazonS3 = amazonS3;
        this.s3Properties = s3Properties;
    }

    // ===================== Upload =====================

    /**
     * Upload a single file, the default key is the original file name (with a timestamp prefix to avoid overwriting)
     *
     * @param file MultipartFile uploaded by the frontend
     * @return The key of the file in S3
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String key = generateKey(Objects.requireNonNull(file.getOriginalFilename()));
        return uploadFile(file, key);
    }

    /**
     * Upload a single file and specify the key (i.e. storage path)
     *
     * @param file MultipartFile uploaded by the frontend
     * @param key  Storage path, e.g. "images/avatar.png"
     * @return The key of the file in S3
     */
    public String uploadFile(MultipartFile file, String key) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(
                    s3Properties.getBucketName(), key, inputStream, metadata);
            amazonS3.putObject(request);
        }

        log.info("File uploaded successfully: bucket={}, key={}", s3Properties.getBucketName(), key);
        return key;
    }

    /**
     * Batch upload files
     *
     * @param files List of files
     * @return List of keys corresponding to each file
     */
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> keys = new ArrayList<>();
        for (MultipartFile file : files) {
            keys.add(uploadFile(file));
        }
        return keys;
    }

    // ===================== Download =====================

    /**
     * Download file, returns S3Object (caller is responsible for closing the stream)
     *
     * @param key File key
     * @return S3Object
     */
    public S3Object downloadFile(String key) {
        log.info("Download file: bucket={}, key={}", s3Properties.getBucketName(), key);
        return amazonS3.getObject(s3Properties.getBucketName(), key);
    }

    /**
     * Generate presigned download URL (default validity 60 minutes)
     *
     * @param key        File key
     * @param expiresMins Expiration time (minutes)
     * @return Presigned URL string
     */
    public String generatePresignedUrl(String key, int expiresMins) {
        Date expiration = new Date(System.currentTimeMillis() + (long) expiresMins * 60 * 1000);
        GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(
                s3Properties.getBucketName(), key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        URL url = amazonS3.generatePresignedUrl(urlRequest);
        log.info("Generate presigned URL: key={}, url={}", key, url);
        return url.toString();
    }

    // ===================== List / Delete =====================

    /**
     * List objects in the Bucket (up to 1000 items)
     *
     * @param prefix Prefix filter, pass "" to list all
     * @return List of object summaries
     */
    public List<Map<String, Object>> listObjects(String prefix) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(s3Properties.getBucketName())
                .withPrefix(prefix);
        ListObjectsV2Result result = amazonS3.listObjectsV2(request);

        return result.getObjectSummaries().stream().map(s -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("key", s.getKey());
            item.put("size", s.getSize());
            item.put("lastModified", s.getLastModified());
            item.put("etag", s.getETag());
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * Delete a single file
     *
     * @param key File key
     */
    public void deleteFile(String key) {
        amazonS3.deleteObject(s3Properties.getBucketName(), key);
        log.info("File deleted successfully: bucket={}, key={}", s3Properties.getBucketName(), key);
    }

    // ===================== Utility Methods =====================

    /**
     * Generate key with a timestamp prefix to avoid filename conflict overwriting
     */
    private String generateKey(String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return timestamp + "_" + originalFilename;
    }

    public String getBucketName() {
        return s3Properties.getBucketName();
    }
}
