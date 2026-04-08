#!/bin/zsh
set -euo pipefail

PROJECT_DIR=${PROJECT_DIR:-"$(cd "$(dirname "$0")/.." && pwd)"}
API_BASE_URL=${API_BASE_URL:-"http://127.0.0.1:8080"}
TEST_FILE=${TEST_FILE:-"download/pexels-userpascal-32488208.jpg"}
DOWNLOAD_TARGET=${DOWNLOAD_TARGET:-"$PROJECT_DIR/target/downloaded-test-file"}
START_APP=${START_APP:-"true"}
SERVER_LOG=${SERVER_LOG:-"$PROJECT_DIR/target/s3-smoke-test-server.log"}
UPLOAD_RESPONSE_FILE=${UPLOAD_RESPONSE_FILE:-"$PROJECT_DIR/target/s3-upload-response.json"}
ATTACHMENT=${ATTACHMENT:-"true"}

require_var() {
  local name="$1"
  local value=""
  eval "value=\${$name:-}"
  if [[ -z "$value" ]]; then
    echo "[ERROR] 缺少环境变量: $name" >&2
    exit 1
  fi
}

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]]; then
    kill "$SERVER_PID" >/dev/null 2>&1 || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

mkdir -p "$PROJECT_DIR/target"

require_var AWS_ACCESS_KEY_ID
require_var AWS_SECRET_ACCESS_KEY
require_var AWS_REGION
require_var AWS_S3_BUCKET

if [[ -z "$TEST_FILE" ]]; then
  echo "[ERROR] 请设置 TEST_FILE 指向要上传的本地文件" >&2
  exit 1
fi

if [[ ! -f "$TEST_FILE" ]]; then
  echo "[ERROR] 测试文件不存在: $TEST_FILE" >&2
  exit 1
fi

if [[ "$START_APP" == "true" ]]; then
  echo "[INFO] 启动应用中..."
  (
    cd "$PROJECT_DIR"
    ./mvnw spring-boot:run > "$SERVER_LOG" 2>&1
  ) &
  SERVER_PID=$!

  for _ in {1..60}; do
    if curl -s -o /dev/null "$API_BASE_URL/"; then
      echo "[INFO] 应用已启动: $API_BASE_URL"
      break
    fi
    sleep 2
  done

  if ! curl -s -o /dev/null "$API_BASE_URL/"; then
    echo "[ERROR] 应用启动失败，请检查日志: $SERVER_LOG" >&2
    exit 1
  fi
else
  echo "[INFO] 跳过启动应用，直接使用已运行服务: $API_BASE_URL"
fi

echo "[INFO] 上传文件: $TEST_FILE"
curl -sS -X POST "$API_BASE_URL/api/files" \
  -F "file=@$TEST_FILE" \
  -o "$UPLOAD_RESPONSE_FILE"

echo "[INFO] 上传响应已保存: $UPLOAD_RESPONSE_FILE"
cat "$UPLOAD_RESPONSE_FILE"
echo

FILE_URL=$(python3 - <<'PY' "$UPLOAD_RESPONSE_FILE"
import json
import pathlib
import sys
path = pathlib.Path(sys.argv[1])
data = json.loads(path.read_text(encoding='utf-8'))
url = data.get('url', '')
if not url:
    raise SystemExit(1)
print(url)
PY
)

if [[ -z "$FILE_URL" ]]; then
  echo "[ERROR] 上传响应中未找到 url 字段" >&2
  exit 1
fi

mkdir -p "$(dirname "$DOWNLOAD_TARGET")"

echo "[INFO] 下载文件: $FILE_URL"
curl -sS -G "$API_BASE_URL/api/files/download" \
  --data-urlencode "url=$FILE_URL" \
  --data-urlencode "attachment=$ATTACHMENT" \
  -o "$DOWNLOAD_TARGET"

if cmp -s "$TEST_FILE" "$DOWNLOAD_TARGET"; then
  echo "[SUCCESS] 上传和下载验证通过"
  echo "[SUCCESS] 下载文件保存到: $DOWNLOAD_TARGET"
else
  echo "[ERROR] 下载文件内容与原文件不一致" >&2
  exit 1
fi

