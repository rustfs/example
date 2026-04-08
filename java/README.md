# RustFS Java Client Examples

This directory contains examples demonstrating how to interact with a [RustFS](https://github.com/rustfs/rustfs) server using Java.

## Prerequisites

- Java 11+
- A running RustFS server (default: `http://localhost:9000`)

## Usage

You can run the examples using your build tool (e.g., Maven, Gradle) or directly via `javac`/`java`. For example, if using a standalone file with OkHttp:

```bash
javac -cp okhttp-4.12.0.jar:. Example.java && java Example
```

*(Note: Adjust the compilation and execution commands based on the exact structure and dependencies of the Java examples provided.)*
