package com.rustfs.xfilestoragerustfs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "dromara.x-file-storage.default-platform=amazon-s3-1",
        "dromara.x-file-storage.amazon-s3[0].platform=amazon-s3-1",
        "dromara.x-file-storage.amazon-s3[0].enable-storage=true",
        "dromara.x-file-storage.amazon-s3[0].access-key=test-access-key",
        "dromara.x-file-storage.amazon-s3[0].secret-key=test-secret-key",
        "dromara.x-file-storage.amazon-s3[0].region=ap-southeast-1",
        "dromara.x-file-storage.amazon-s3[0].bucket-name=test-bucket"
})
class XFileStorageRustfsApplicationTests {

    @Test
    void contextLoads() {
    }
}
