package com.rustfs.xfilestoragerustfs.storage;

import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.spring.SpringFileStorageProperties;
import org.dromara.x.file.storage.spring.file.MultipartFileWrapperAdapter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XFileStorageConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "dromara.x-file-storage")
    public SpringFileStorageProperties springFileStorageProperties() {
        return new SpringFileStorageProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.storage.presign")
    public StoragePresignProperties storagePresignProperties() {
        return new StoragePresignProperties();
    }

    @Bean
    public FileStorageService fileStorageService(SpringFileStorageProperties properties) {
        // Build service manually to avoid relying on external auto-configuration compatibility.
        return FileStorageServiceBuilder.create(properties.toFileStorageProperties())
                .useDefault()
                .addFileWrapperAdapter(new MultipartFileWrapperAdapter())
                .build();
    }
}
