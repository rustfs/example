# RustFS Multi‑Language Client Examples

This repository contains ready‑to‑run client examples in various programming languages, demonstrating how to interact with a [RustFS](https://github.com/rustfs/rustfs) server.

> **RustFS** is a high‑performance storage server written in Rust. It exposes an HTTP API for file operations (create, read, write, delete, list).

## Supported Languages

- **Python** (using `requests`)
- **Go** (using `net/http`)
- **Node.js** (using `fetch` / `axios`)
- **Java** (using `OkHttp`)
- **cURL** (command line)
- *C++ / Rust / Ruby examples are coming soon*

## Quick Start

1. **Start a RustFS server** locally (default endpoint: `http://localhost:9000`)

2. **Clone this repository**
   ```bash
   git clone https://github.com/rustfs/examples.git
   cd examples
   ```

3. **Run an example** of your choice:
    - Python: `python python/example.py`
    - Go: `cd go && go run main.go`
    - Node.js: `node nodejs/example.js`
    - Java: `cd java && javac -cp okhttp-4.12.0.jar:. Example.java && java Example`
    - cURL: `bash curl/example.sh`

> See each language subdirectory for detailed instructions and environment setup.

## What Each Example Shows

- Connecting to a RustFS server
- Authenticating (if required)
- Uploading a file (PUT /upload)
- Downloading a file (GET /download)
- Listing directory contents (GET /list)
- Deleting a file (DELETE /delete)

## Requirements

- RustFS server v1.0+ (or compatible mock server)
- Language‑specific runtime (Python 3.8+, Go 1.19+, Node.js 16+, Java 11+)

## Contributing

Feel free to add examples in other languages or improve existing ones. Submit a pull request or open an issue.

## License

Apache2.0 License 
