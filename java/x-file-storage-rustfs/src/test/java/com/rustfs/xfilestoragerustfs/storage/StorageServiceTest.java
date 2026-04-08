package com.rustfs.xfilestoragerustfs.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.AmazonS3FileStorage;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private StoragePresignProperties storagePresignProperties;

    @Mock
    private UploadPretreatment uploadPretreatment;

    @Mock
    private AmazonS3FileStorage amazonS3FileStorage;

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private StorageService storageService;

    @Test
    void uploadReturnsPresignedDownload() throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello".getBytes()
        );
        FileInfo fileInfo = new FileInfo()
                .setUrl("https://example-bucket.s3.amazonaws.com/uploads/2026/04/08/stored-hello.txt")
                .setFilename("stored-hello.txt")
                .setOriginalFilename("hello.txt")
                .setContentType("text/plain")
                .setSize(5L)
                .setPlatform("amazon-s3-1")
                .setPath("2026/04/08/");

        when(fileStorageService.of(multipartFile)).thenReturn(uploadPretreatment);
        when(uploadPretreatment.setPath(anyString())).thenReturn(uploadPretreatment);
        when(uploadPretreatment.upload()).thenReturn(fileInfo);
        when(fileStorageService.getFileStorageVerify(fileInfo)).thenReturn(amazonS3FileStorage);
        when(amazonS3FileStorage.getBucketName()).thenReturn("example-bucket");
        when(amazonS3FileStorage.getBasePath()).thenReturn("uploads/");
        when(amazonS3FileStorage.getClient()).thenReturn(amazonS3);
        when(storagePresignProperties.getExpireSeconds()).thenReturn(900L);
        when(storagePresignProperties.getMaxExpireSeconds()).thenReturn(604800L);
        when(amazonS3.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(URI.create("https://signed.example.com/download").toURL());

        UploadFileResponse response = storageService.upload(multipartFile);

        assertNotNull(response.presignedDownload());
        assertEquals("https://signed.example.com/download", response.presignedDownload().url());
        assertEquals("hello.txt", response.presignedDownload().filename());
        assertEquals("text/plain", response.presignedDownload().contentType());

        ArgumentCaptor<GeneratePresignedUrlRequest> requestCaptor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(amazonS3).generatePresignedUrl(requestCaptor.capture());
        assertEquals("example-bucket", requestCaptor.getValue().getBucketName());
        assertEquals("uploads/2026/04/08/stored-hello.txt", requestCaptor.getValue().getKey());
    }

    @Test
    void uploadStillSucceedsWhenPresignedDownloadGenerationFails() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "hello.txt",
                "text/plain",
                "hello".getBytes()
        );
        FileInfo fileInfo = new FileInfo()
                .setUrl("https://example-bucket.s3.amazonaws.com/uploads/2026/04/08/stored-hello.txt")
                .setFilename("stored-hello.txt")
                .setOriginalFilename("hello.txt")
                .setContentType("text/plain")
                .setSize(5L)
                .setPlatform("amazon-s3-1")
                .setPath("2026/04/08/");

        when(fileStorageService.of(multipartFile)).thenReturn(uploadPretreatment);
        when(uploadPretreatment.setPath(anyString())).thenReturn(uploadPretreatment);
        when(uploadPretreatment.upload()).thenReturn(fileInfo);
        when(fileStorageService.getFileStorageVerify(fileInfo)).thenThrow(new StorageException("presign failed"));

        UploadFileResponse response = storageService.upload(multipartFile);

        assertEquals(fileInfo.getUrl(), response.url());
        assertNull(response.presignedDownload());
    }
}

