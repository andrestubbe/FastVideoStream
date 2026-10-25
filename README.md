# FastVideoStream 0.1.1 — Low-Overhead Native Video Streaming Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastVideoStream/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+%20%7C%20Linux%20%7C%20macOS-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.1-green.svg)](https://jitpack.io/#andrestubbe/FastVideoStream)

---

**⚡ High-performance, low-overhead native video streaming engine for the FastJava ecosystem.**

**FastVideoStream** provides a hardware-accelerated desktop video streaming engine for Java. Powered by DirectX 11 DXGI Desktop Duplication via **[FastScreen](https://github.com/andrestubbe/FastScreen)** and hardware video encoding (Intel QuickSync `h264_qsv`, NVIDIA `h264_nvenc`, AMD `h264_amf`, or CPU `libx264`), it streams desktop frames directly to RTMPS destinations (YouTube, Twitch, or custom endpoints) with minimal CPU and memory footprint.

---

## Quick Start

### 1. Programmatic Java API (`VideoStreamEngine`)

```java
import fastvideostream.*;

public class Demo {
    public static void main(String[] args) throws Exception {
        // 1. Instantiate native DirectX 11 / DXGI streaming engine
        VideoStreamEngine engine = new D3D11VideoStreamEngine();

        // 2. Configure session parameters
        StreamSessionConfig config = StreamSessionConfig.builder()
                .monitorIndex(0)
                .fps(30)
                .bitrateKbps(4000)
                .includeCursor(true)
                .encoder("h264_qsv") // Intel QuickSync (default), h264_nvenc, or libx264
                .rtmpsTargetUrl("rtmps://a.rtmps.youtube.com/live2/" + System.getenv("FAST_YOUTUBE_KEY"))
                .build();

        // 3. Start streaming with real-time lifecycle telemetry
        engine.start(config, new StreamStatusListener() {
            @Override
            public void onStarted(int width, int height, int fps) {
                System.out.printf("Stream active: %dx%d @ %d FPS%n", width, height, fps);
            }

            @Override
            public void onFramePushed(long frameNumber, long elapsedNanos) {
                if (frameNumber % 300 == 0) {
                    System.out.println("Frames pushed: " + frameNumber);
                }
            }

            @Override
            public void onError(Throwable error) {
                System.err.println("Stream error: " + error.getMessage());
            }

            @Override
            public void onStopped() {
                System.out.println("Stream stopped gracefully.");
            }
        });

        // Keep running or stop gracefully when desired
        Thread.sleep(30_000);
        engine.stop();
    }
}
```

### 2. Standalone CLI Launcher

```powershell
$env:FAST_YOUTUBE_KEY = "your-youtube-key"

# Quick launch with interactive defaults
.\run-demo.bat

# Or direct headless CLI execution
.\run-cli.bat --fps=60 --bitrate=6000 --encoder=h264_qsv
```

---

## Table of Contents

- [Why FastVideoStream?](#why-fastvideostream)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Architecture Overview](#architecture-overview)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
- [Technical Demos & Benchmarks](#technical-demos--benchmarks)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastVideoStream?

Desktop streaming often introduces unnecessary overhead between the Windows compositor, the video encoder, and the network output. FastVideoStream strips away heavy UI layers and delegates capture directly to native Windows backends:

- **Native DXGI Capture:** Direct DirectX 11 Desktop Duplication via FastScreen delivers sub-millisecond frame grabs.
- **Hardware Acceleration:** Native support for Intel QSV (`h264_qsv`), NVIDIA NVENC (`h264_nvenc`), and AMD AMF (`h264_amf`).
- **Zero-GC Hot Loop:** Reusable off-heap BGRA conversion buffers avoid per-frame allocation churn.
- **Clean Engine Interface:** Decoupled `VideoStreamEngine` interface allowing headless automation, microservices, and custom UI embedding.
- **Secure Credentials:** Stream keys and targets are passed dynamically at runtime and never persisted in source.

| Feature | Java Robot Screen Loop | Heavy Studio Apps (e.g. OBS) | FastVideoStream |
|:---|:---|:---|:---|
| **Capture Pipeline** | Slow GDI `Robot.createScreenCapture`| Heavy graphics hook injection | **DirectX 11 DXGI Duplication (`FastScreen`)**|
| **Architecture** | Synchronous blocking loop | Complex monolithic UI process | **Lean decoupled `VideoStreamEngine` API** |
| **Memory / CPU Footprint**| High GC churn (`BufferedImage`) | 500 MB–1.5 GB RAM footprint | **Ultra-lightweight (< 50 MB RAM)** |
| **Automation / Headless**| GUI thread required | Complex WebSocket/CLI plugins | **Native headless engine & script friendly** |

---

## Key Features

- 🖥️ **DirectX 11 Hardware Desktop Capture** — Captures selected monitors via `FastScreen` DXGI Duplication with optional cursor rendering.
- ⚡ **Hardware H.264 Encoding** — Built-in support for Intel QuickSync (`h264_qsv`), NVIDIA NVENC (`h264_nvenc`), AMD AMF (`h264_amf`), and CPU (`libx264`).
- 🔌 **Pluggable VideoStreamEngine API** — Clean, decoupled Java interface (`start`, `stop`, `isStreaming`, `close`) with non-blocking lifecycle callbacks.
- 🎨 **Zero-Copy Frame Interception** — `FrameInterceptor` callback allowing applications to inject webcam PiP, watermarks, or overlays directly into the raw pixel buffer before encoding.
- 📡 **Direct RTMPS Streaming** — Pipes uncompressed BGRA frames directly to hardware encoders targeting YouTube Live, Twitch, or custom RTMP/RTMPS ingests.
- 💻 **Headless CLI Operation** — Script-friendly CLI launcher designed for automated workflows and portable deployments.
- 📦 **Zero-Friction FastJava Integration** — Seamlessly integrates with the FastJava ecosystem via standard Maven and JitPack artifacts.

---

## Real-World Use Cases

- 🎮 **Low-Overhead Desktop Streaming**: Broadcast monitors to YouTube Live or Twitch with minimal CPU impact.
- 🤖 **Automated CI/CD & Headless Bot Streaming**: Run headless live video feeds from virtual desktops or test harnesses.
- 🔍 **Remote QA & Diagnostics**: Stream desktop sessions to RTMP ingest endpoints for verification and remote monitoring.
- 🧩 **Engine Core for Custom Applications**: Embed the lightweight `VideoStreamEngine` directly into custom tools, dashboards, and stream studios.

---

## Architecture Overview

```text
Windows Desktop
      |
      v
FastScreen (DirectX 11 DXGI Desktop Duplication)
      |
      v
Reusable BGRA Frame Buffer
      |
      v
D3D11VideoStreamEngine (pipe:0 stdin)
      |
      v
Hardware Encoder (h264_qsv / h264_nvenc / h264_amf / libx264)
      |
      v
RTMPS Ingest (YouTube / Twitch / Custom Ingest)
```

FastVideoStream acts as the low-overhead streaming edge. For applications requiring Google OAuth 2.0 provisioning, camera picture-in-picture, and interactive studio UIs, FastVideoStream serves as the foundational low-level video engine.

---

## Performance Benchmarks

The streaming engine leverages the zero-copy DXGI capture pipeline from `FastScreen` and hardware-accelerated encoding:

- **Capture Latency:** < 1 ms per frame via native DXGI Duplication.
- **Memory Footprint:** < 50 MB heap usage with reusable direct buffers.
- **CPU Overhead:** Offloaded to GPU hardware encoders (`h264_qsv` / `h264_nvenc`).

Measure a real setup with the intended monitor, encoder, resolution, and network target:
- Capture FPS vs. requested target FPS.
- Encoder load and hardware utilization.
- Output network bandwidth and dropped frames.

---

## API Quick Reference

| Class / Method | Signature | Description | Docs |
|:---|:---|:---|:---|
| `VideoStreamEngine` | `interface` | Core interface for video streaming lifecycle (`start`, `stop`, `isStreaming`). | [Reference](docs/REFERENCE.md) |
| `FrameInterceptor` | `interface` | Consumer callback (`onFrame(pixels, w, h)`) for custom overlays / webcam PiP. | [Reference](docs/REFERENCE.md) |
| `D3D11VideoStreamEngine` | `class` | DirectX 11 DXGI native streaming engine implementation. | [Reference](docs/REFERENCE.md) |
| `StreamSessionConfig` | `record` | Immutable configuration record with fluent `builder()`. | [Reference](docs/REFERENCE.md) |
| `StreamStatusListener` | `interface` | Lifecycle and telemetry callback (`onStarted`, `onFramePushed`, `onError`, `onStopped`). | [Reference](docs/REFERENCE.md) |
| `FrameInterceptor` | `interface` | Application callback for external frame compositing (e.g. Webcam PiP in Stream+). | [Reference](docs/REFERENCE.md) |
| `FastVideoStream.main(args)` | `void` | Standalone CLI entry point for rapid testing and headless execution. | [Reference](docs/REFERENCE.md) |

See [docs/REFERENCE.md](docs/REFERENCE.md) for full architectural specifications.

---

## Technical Demos & Benchmarks

| Case | Java Example | Launcher | Description |
|:---|:---|:---|:---|
| **Interactive Showcase Demo** | [Demo.java](examples/Demo/src/main/java/fastvideostream/demo/Demo.java) | `run-demo.bat` | End-to-end interactive desktop streaming showcase demo via `VideoStreamEngine`. |
| **JMH Microbenchmark Suite** | [Benchmark.java](examples/Benchmark/src/main/java/fastvideostream/benchmark/Benchmark.java) | `run-benchmark.bat` | Formal OpenJDK JMH throughput measurements across frame capture & buffer conversion. |

---

## Installation

### Option 1: Maven (Recommended via JitPack)

Add the JitPack repository and `FastVideoStream` to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastVideoStream Core Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastVideoStream</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastScreen DXGI Desktop Duplication -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastScreen</artifactId>
        <version>0.1.4</version>
    </dependency>

    <!-- FastCore (Unified Native FFM & JNI Loader) -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastVideoStream:0.1.1'
    implementation 'com.github.andrestubbe:FastScreen:0.1.4'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (Pre-built JAR)

Download the pre-compiled standalone JAR directly from GitHub Releases:

- 📦 [**FastVideoStream-0.1.1.jar**](https://github.com/andrestubbe/FastVideoStream/releases/download/0.1.1/FastVideoStream-0.1.1.jar)

---

## Documentation

- **[REFERENCE.md](docs/REFERENCE.md)**: Architecture, interfaces, and FFmpeg pipe contract.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Design principles and low-latency streaming rationale.
- **[ROADMAP.md](docs/ROADMAP.md)**: Development milestones and future GPU encoder backends.
- **[CHANGELOG.md](docs/CHANGELOG.md)**: Version history, release notes, and migration guides.
- **[COMPILE.md](docs/COMPILE.md)**: Build and packaging instructions.

---

## Platform Support

| Platform | Architecture | Status | Notes |
|:---|:---|:---|:---|
| Windows 10/11 | x64 | ✅ Fully Supported | DXGI desktop duplication, QSV / NVENC / AMF |
| Linux | x64, ARM64 | 🚧 Planned | FastScreen X11/Wayland backend required |
| macOS | Apple Silicon, x64 | 🚧 Planned | FastScreen ScreenCaptureKit backend required |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastScreen](https://github.com/andrestubbe/FastScreen) — DXGI desktop duplication and window capture.
- [FastScreenCapture](https://github.com/andrestubbe/FastScreenCapture) — Screenshots and local lossless recording.
- [FastCamera](https://github.com/andrestubbe/FastCamera) — Low-latency Windows camera capture.
- [FastAudioCapture](https://github.com/andrestubbe/FastAudioCapture) — Low-overhead WASAPI audio engine.
- [FastImage](https://github.com/andrestubbe/FastImage) — SIMD-accelerated off-heap image processing.

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋

