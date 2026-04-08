package com.rustfs.xfilestoragerustfs.storage;

import java.nio.charset.StandardCharsets;

import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResponse upload(@RequestPart("file") MultipartFile file) {
        return storageService.upload(file);
    }

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> download(
            @RequestParam("url") String url,
            @RequestParam(name = "attachment", defaultValue = "false") boolean attachment) {

        FileInfo fileInfo = storageService.parseFileInfo(url);
        String filename = resolveFilename(fileInfo);
        MediaType mediaType = resolveMediaType(fileInfo);

        ContentDisposition disposition = ContentDisposition
                .builder(attachment ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        StreamingResponseBody body = outputStream -> storageService.downloadToStream(url, outputStream);

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType);

        if (fileInfo != null && fileInfo.getSize() != null && fileInfo.getSize() >= 0) {
            builder.contentLength(fileInfo.getSize());
        }

        return builder.body(body);
    }

    @GetMapping("/presign-download")
    public PresignedDownloadResponse presignDownload(
            @RequestParam("url") String url,
            @RequestParam(name = "attachment", defaultValue = "true") boolean attachment,
            @RequestParam(name = "expireSeconds", required = false) Long expireSeconds) {
        return storageService.generatePresignedDownloadUrl(url, attachment, expireSeconds);
    }

    private String resolveFilename(FileInfo fileInfo) {
        if (fileInfo == null) {
            return "download.bin";
        }
        if (StringUtils.hasText(fileInfo.getOriginalFilename())) {
            return fileInfo.getOriginalFilename();
        }
        if (StringUtils.hasText(fileInfo.getFilename())) {
            return fileInfo.getFilename();
        }
        return "download.bin";
    }

    private MediaType resolveMediaType(FileInfo fileInfo) {
        if (fileInfo == null || !StringUtils.hasText(fileInfo.getContentType())) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(fileInfo.getContentType());
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

