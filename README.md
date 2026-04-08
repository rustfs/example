# RustFS Multi‑Language Client Examples

This repository contains ready‑to‑run client examples in various programming languages, demonstrating how to interact with a [RustFS](https://github.com/rustfs/rustfs) server.

> **RustFS** is a high‑performance storage server written in Rust. It exposes an S3-compatible API and HTTP APIs for file operations.

## Supported Languages

We provide examples for the following languages. Click on the respective links for detailed instructions and environment setup:

- [**Java**](./java) (AWS S3 SDK, x-file-storage)
- [**Go**](./go)
- [**Python**](./python)
- [**PHP**](./php)
- [**Rust**](./rust)

## Quick Start

1. **Start a RustFS server** locally (default endpoint: `http://localhost:9000`)

2. **Clone this repository**
   ```bash
   git clone https://github.com/rustfs/examples.git
   cd examples
   ```

3. **Explore the examples**:
   Navigate into the directory of your preferred language to find specific usage instructions, dependencies, and run commands.
   
   For instance, to view the Java examples:
   ```bash
   cd java
   cat README.md
   ```

## What Each Example Shows

Depending on the specific language and project, the examples generally demonstrate:
- Connecting to a RustFS server using S3-compatible clients or direct HTTP APIs.
- Authenticating with the server.
- Performing core file operations: Uploading, Downloading, Deleting, and Listing.

## Requirements

- A running RustFS server v1.0+ (or a compatible mock server)
- Language‑specific runtimes (e.g., Java 21+, Go 1.24+, Python 3.12+, PHP 8.4+, Rust Stable)

## Contributing

Contributions are welcome! Feel free to add examples in other languages or improve the existing ones. Simply submit a pull request or open an issue.

## License

Apache 2.0 License
