API_BASE_URL ?= http://127.0.0.1:8080
TEST_FILE ?= /tmp/demo.txt
DOWNLOAD_TARGET ?= ./target/downloaded-demo.txt
FILE_URL ?=
EXPIRE_SECONDS ?= 900
ATTACHMENT ?= true

.PHONY: run run-local test smoke curl-upload curl-download curl-presign

run:
	./mvnw spring-boot:run

run-local:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

test:
	./mvnw test -q

smoke:
	TEST_FILE="$(TEST_FILE)" API_BASE_URL="$(API_BASE_URL)" DOWNLOAD_TARGET="$(DOWNLOAD_TARGET)" ./scripts/s3-smoke-test.sh

curl-upload:
	curl -X POST "$(API_BASE_URL)/api/files" \
	  -F "file=@$(TEST_FILE)"

curl-download:
	@if [ -z "$(FILE_URL)" ]; then \
	  echo "FILE_URL is required, usage: make curl-download FILE_URL='https://...'"; \
	  exit 1; \
	fi
	curl -G "$(API_BASE_URL)/api/files/download" \
	  --data-urlencode "url=$(FILE_URL)" \
	  --data-urlencode "attachment=$(ATTACHMENT)" \
	  -o "$(DOWNLOAD_TARGET)"

curl-presign:
	@if [ -z "$(FILE_URL)" ]; then \
	  echo "FILE_URL is required, usage: make curl-presign FILE_URL='https://...'"; \
	  exit 1; \
	fi
	curl -G "$(API_BASE_URL)/api/files/presign-download" \
	  --data-urlencode "url=$(FILE_URL)" \
	  --data-urlencode "attachment=$(ATTACHMENT)" \
	  --data-urlencode "expireSeconds=$(EXPIRE_SECONDS)"

