# x-file-storage-rustfs

Spring Boot file upload/download example based on `x-file-storage` + `Amazon S3`.

## 1. Environment Variables

Please configure S3 access parameters before starting the application:

- `AWS_ACCESS_KEY_ID`: S3 Access Key
- `AWS_SECRET_ACCESS_KEY`: S3 Secret Key
- `AWS_REGION`: e.g. `ap-southeast-1`
- `AWS_S3_BUCKET`: Target bucket name
- `AWS_S3_ENDPOINT`: Optional, usually leave empty for AWS native S3; fill in when using S3 compatible services
- `AWS_S3_DOMAIN`: Optional, custom access domain name

A complete example file is provided in the project: `.env.example`

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
cp .env.example .env
set -a
source .env
set +a
```

Note: Spring Boot does not automatically load `.env` by default. You need to inject these environment variables through your shell, IDE, or run configuration.

## 2. Start the Application

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
./mvnw spring-boot:run
```

If you wish to use the local example configuration, you can also enable the `local` profile:

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Corresponding example file: `src/main/resources/application-local.properties`

## 3. Interfaces

- Upload: `POST /api/files`
  - `multipart/form-data`
  - File field name: `file`
- Download: `GET /api/files/download?url={url}&attachment={true|false}`
- Presigned download address: `GET /api/files/presign-download?url={url}&attachment={true|false}&expireSeconds=600`

## 4. Manual Invocation Example

### Upload

```bash
curl -X POST http://localhost:8080/api/files \
  -F "file=@/tmp/demo.txt"
```

Return example:

```json
{
  "url": "https://<bucket>.s3.<region>.amazonaws.com/uploads/2026/04/08/xxx.txt",
  "filename": "xxx.txt",
  "originalFilename": "demo.txt",
  "contentType": "text/plain",
  "size": 123,
  "platform": "amazon-s3-1",
  "path": "uploads/2026/04/08/",
  "presignedDownload": {
    "url": "https://<signed-url>",
    "expiresAt": "2026-04-08T08:15:00Z",
    "expireSeconds": 900,
    "filename": "demo.txt",
    "contentType": "text/plain"
  }
}
```

Description:

- The top-level `url` is the original file access address
- `presignedDownload.url` is a temporary downloadable address returned immediately after a successful upload

### Download

```bash
curl -G http://localhost:8080/api/files/download \
  --data-urlencode "url=https://<bucket>.s3.<region>.amazonaws.com/uploads/2026/04/08/xxx.txt" \
  --data-urlencode "attachment=true" \
  -o ./downloaded-demo.txt
```

### Generate Presigned Download Address

```bash
curl -G http://localhost:8080/api/files/presign-download \
  --data-urlencode "url=https://<bucket>.s3.<region>.amazonaws.com/uploads/2026/04/08/xxx.txt" \
  --data-urlencode "attachment=true" \
  --data-urlencode "expireSeconds=600"
```

Return example:

```json
{
  "url": "https://<signed-url>",
  "expiresAt": "2026-04-08T08:15:00Z",
  "expireSeconds": 600,
  "filename": "demo.txt",
  "contentType": "text/plain"
}
```

## 5. One-Click Smoke Test for Real S3

The project provides a script: `scripts/s3-smoke-test.sh`

It executes the following steps:

1. Verify S3 environment variables
2. Start the Spring Boot application (can be disabled)
3. Call the upload interface
4. Extract `url` from the response
5. Call the download interface
6. Compare if the downloaded file matches the original file content

### Usage 1: Script starts the application

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_REGION="ap-southeast-1"
export AWS_S3_BUCKET="your-bucket"
export TEST_FILE="/tmp/demo.txt"
chmod +x ./scripts/s3-smoke-test.sh
./scripts/s3-smoke-test.sh
```

### Usage 2: Application is already running, script only verifies upload/download

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
export AWS_ACCESS_KEY_ID="your-access-key"
export AWS_SECRET_ACCESS_KEY="your-secret-key"
export AWS_REGION="ap-southeast-1"
export AWS_S3_BUCKET="your-bucket"
export TEST_FILE="/tmp/demo.txt"
export START_APP="false"
export API_BASE_URL="http://127.0.0.1:8080"
./scripts/s3-smoke-test.sh
```

### Optional Parameters

- `API_BASE_URL`: Interface address, default `http://127.0.0.1:8080`
- `TEST_FILE`: Path to the file to be uploaded, required
- `DOWNLOAD_TARGET`: Download save path
- `START_APP`: Whether the script starts the application, default `true`
- `ATTACHMENT`: Whether to use attachment mode when downloading, default `true`

## 6. Makefile Shortcuts

The project provides a `Makefile`, common commands are as follows:

```bash
cd /Users/zhi/Documents/code/java/aws-s3-demo/x-file-storage-rustfs
make test
make run
make run-local
make smoke TEST_FILE=/tmp/demo.txt
make curl-upload TEST_FILE=/tmp/demo.txt
make curl-presign FILE_URL="https://<bucket>.s3.<region>.amazonaws.com/uploads/2026/04/08/xxx.txt"
```

If you have already executed `source .env`, these commands will directly reuse the environment variables in the current shell.

## 7. Common Issues

- If uploading fails, first check if `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_S3_BUCKET` are correct.
- If it's AWS native S3, `AWS_S3_ENDPOINT` usually does not need to be filled.
- If the script prompts that the application failed to start, check the log file: `target/s3-smoke-test-server.log`
- If the downloaded file is inconsistent with the original file, first confirm bucket permissions, region, and whether the generated `url` can be correctly parsed back by `x-file-storage`.
- The presigned address expiration time is defaulted by `app.storage.presign.expire-seconds`, and the maximum value is controlled by `app.storage.presign.max-expire-seconds`.
