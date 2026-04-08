package com.rustfs.xfilestoragerustfs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.dromara.x.file.storage.core.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private FileController fileController;

    @Test
    void uploadReturnsServiceResult() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );
        PresignedDownloadResponse presignedDownload = new PresignedDownloadResponse(
                "https://signed.example.com/download",
                Instant.parse("2026-04-08T08:00:00Z"),
                600,
                "hello.txt",
                "text/plain"
        );
        UploadFileResponse expected = new UploadFileResponse(
                "https://example-bucket.s3.amazonaws.com/uploads/hello.txt",
                "hello.txt",
                "hello.txt",
                "text/plain",
                5L,
                "amazon-s3-1",
                "uploads/",
                presignedDownload
        );

        when(storageService.upload(multipartFile)).thenReturn(expected);

        UploadFileResponse actual = fileController.upload(multipartFile);

        assertEquals(expected, actual);
        verify(storageService).upload(multipartFile);
    }

    @Test
    void downloadStreamsFileAndSetsHeaders() throws Exception {
        String url = "https://example-bucket.s3.amazonaws.com/uploads/hello.txt";
        FileInfo fileInfo = new FileInfo()
                .setUrl(url)
                .setOriginalFilename("hello.txt")
                .setFilename("hello.txt")
                .setContentType("text/plain")
                .setSize(5L);

        when(storageService.parseFileInfo(url)).thenReturn(fileInfo);
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(1);
            outputStream.write("hello".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(storageService).downloadToStream(org.mockito.ArgumentMatchers.eq(url), org.mockito.ArgumentMatchers.any(OutputStream.class));

        ResponseEntity<StreamingResponseBody> response = fileController.download(url, true);

        assertNotNull(response.getBody());
        assertEquals("text/plain", response.getHeaders().getContentType().toString());
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);
        assertEquals("hello", output.toString(StandardCharsets.UTF_8));
    }

    @Test
    void presignDownloadReturnsServiceResult() {
        String url = "https://example-bucket.s3.amazonaws.com/uploads/hello.txt";
        PresignedDownloadResponse expected = new PresignedDownloadResponse(
                "https://signed.example.com/download",
                Instant.parse("2026-04-08T08:00:00Z"),
                600,
                "hello.txt",
                "text/plain"
        );

        when(storageService.generatePresignedDownloadUrl(url, true, 600L)).thenReturn(expected);

        PresignedDownloadResponse actual = fileController.presignDownload(url, true, 600L);

        assertEquals(expected, actual);
        verify(storageService).generatePresignedDownloadUrl(url, true, 600L);
    }
}
