> [!WARNING]
> **🚧 WORK IN PROGRESS (WIP) — Active Streaming Engine Development & Calibration**
> 
> * **Active Calibration:** DXGI surface format alignment, WASAPI dual-audio mixing (`amix`), and low-latency hardware encoder parameters (`h264_nvenc`, `h264_qsv`) are undergoing continuous optimization for live production broadcasts.
> * **FastJava Media Pipeline:** Direct GPU-to-GPU zero-copy encoding integration with **[FastVulkan](https://github.com/andrestubbe/FastVulkan)** and **[FastGraphics](https://github.com/andrestubbe/FastGraphics)** is currently in progress.

# FastVideoStream 0.1.1 [ALPHA-2026-09-30] — Low-Overhead CLI Video Streaming for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastVideoStream/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+%20%7C%20Linux%20%7C%20macOS-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.1-green.svg)](https://jitpack.io/#andrestubbe/FastVideoStream)

---

**The low-overhead video streaming layer for the FastJava ecosystem.**

FastVideoStream reuses the DXGI desktop capture path from **FastScreen**, adds optional **FastCamera** picture-in-picture, encodes once through FFmpeg, and sends the same H.264 stream to YouTube, Twitch, or both.

The project is intentionally headless and CLI-first. Its capture loop follows the same small, direct shape as FastScreenCapture: one capture loop, one reusable conversion buffer, and one encoder process.

Watch Demo (YouTube) | Watch JMH Benchmark (YouTube)

---

## Quick Start — Example

Requirements: Windows 10+, Java 17+, FFmpeg on `PATH`, and a YouTube and/or Twitch stream key.

### 1. Java API Example

```java
import fastvideostream.FastVideoStream;

public class Demo {
    public static void main(String[] args) throws Exception {
        // Launch streaming pipeline with camera PiP, audio mixing at 60 FPS
        String[] streamArgs = {
            "--camera",
            "--audio",
            "--fps=60",
            "--bitrate=6000",
            "--encoder=h264_nvenc"
        };

        // Streams directly to $env:FAST_YOUTUBE_KEY and/or $env:FAST_TWITCH_KEY
        FastVideoStream.main(streamArgs);
    }
}
```

### 2. Standalone CLI Launcher

```powershell
$env:FAST_YOUTUBE_KEY = "your-youtube-key"
$env:FAST_TWITCH_KEY = "your-twitch-key"

# Quick launch with interactive defaults
run-demo.bat

# Or direct headless CLI execution
run-cli.bat --camera --audio --fps=60 --bitrate=6000
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


Desktop streaming often adds unnecessary layers between the Windows compositor, the encoder, and the network output. FastVideoStream keeps the orchestration small and delegates the performance-critical capture work to the existing FastJava backends.

- **Native desktop capture:** FastScreen uses DXGI Desktop Duplication instead of a Java screenshot loop.
- **One encode, multiple destinations:** FFmpeg's tee muxer sends one encoded stream to YouTube and Twitch.
- **Low allocation capture loop:** The Java side reuses the frame conversion buffer and avoids creating a new byte array per frame.
- **Optional camera composition:** FastCamera provides an asynchronous camera callback for bottom-right PiP.
- **No credentials in source:** Stream keys are read from environment variables or command-line overrides and are never stored in the repository.

This release is a focused audio/video streamer, not a complete OBS replacement. Automatic per-destination reconnect, scenes, and the Swing streaming tab remain planned work.

| Feature | Java Robot Screen Loop | OBS Studio (Full App) | FastVideoStream |
|:---|:---|:---|:---|
| **Capture Pipeline** | Slow GDI `Robot.createScreenCapture`| Heavy graphics hook inject | **DXGI Desktop Duplication (`FastScreen`)**|
| **Multi-Platform Stream**| Not supported | Multiple encoder passes / plugin| **Single-encode FFmpeg tee (YouTube + Twitch)**|
| **Memory / CPU Footprint**| High GC churn (BufferedImage) | 500 MB–1.5 GB RAM footprint | **Ultra-lightweight CLI (< 50 MB RAM)** |
| **Automation / Headless**| GUI thread required | Complex WebSocket/CLI plugins | **Native headless CLI / script friendly** |

---

## Key Features

- 🖥️ **DXGI Hardware Desktop Capture** — Captures a selected monitor through `FastScreen` with optional cursor compositing and selectable source modes (`screen`, `screen + camera`, or `camera only`).
- 🎥 **Picture-in-Picture Camera Overlay** — Real-time asynchronous webcam compositing (`FastCamera`) at custom `x,y,w,h` coordinates.
- 🎙️ **WASAPI Audio & Loopback Mixing** — Live microphone (`--microphone`) and Windows system-audio loopback (`--system-audio`) via `FastAudioCapture`, mixed simultaneously (`--audio`) into stereo AAC at 48 kHz.
- ⚡ **Hardware H.264 NVENC Encoding** — High-speed NVENC GPU encoding by default, with configurable FFmpeg encoder, FPS, bitrate, monitor, and custom FFmpeg binary paths.
- 📡 **Simultaneous Dual-Stream Output** — Single-encode FFmpeg tee muxer broadcasting simultaneously to YouTube and Twitch RTMPS endpoints.
- 💻 **Headless CLI Operation** — Script-friendly CLI launcher designed for automated workflows and portable Windows deployments.
- 🪟 **Native Window Exclusion Affinity** — Swing control window is automatically excluded from capture via native Windows affinity so tool UIs remain invisible on stream.
- 📦 **Zero-Friction Java 17 Substrate** — Clean Maven and JitPack-compatible architecture deeply integrated with the FastJava ecosystem.

---

## Real-World Use Cases

- 🎮 **Low-Overhead Game & Desktop Streaming**: Capture one monitor and publish to YouTube, Twitch, or both simultaneously without OBS CPU overhead.
- 💻 **Live Coding & Developer Demos**: Stream coding sessions, IDEs, and terminal sessions with optional real-time camera picture-in-picture overlays.
- 🔍 **Reproducible QA & Desktop Diagnostics**: Run headless desktop capture and verification paths without opening a full studio UI.
- 🤖 **FastJava Ecosystem Edge**: Serve as the high-performance streaming and broadcasting edge around `FastScreen` and `FastCamera`.

---

## Architecture Overview

```text
Windows Desktop
      |
      v
FastScreen / DXGI Desktop Duplication
      |
      +--> optional FastCamera callback --> CPU PiP composition
      |
      v
Reusable BGRA conversion buffer
      |
      v
FFmpeg stdin --> H.264 encoder --> tee muxer
                                  |-- YouTube RTMPS
                                  |-- Twitch RTMPS
```

FastAudioCapture is consumed as a published FastJava module. Each enabled source sends 48 kHz, 16-bit stereo PCM to a local FFmpeg input; FFmpeg performs the optional `amix` stage and encodes AAC alongside the video stream.

---

## Performance Benchmarks

No formal FastVideoStream benchmark result is published yet. The relevant performance baseline is the existing FastScreenCapture benchmark and the DXGI capture implementation in FastScreen.

Measure a real setup with the intended monitor, encoder, resolution, and network target. The most useful values are:

- Capture FPS versus requested FPS.
- FFmpeg process health and encoder load.
- Dropped frames and output reconnects.
- CPU/GPU utilization and upload bandwidth.

Do not compare the CLI to OBS using different encoder settings, resolutions, or platform bitrates.

---

## API Quick Reference

| Entry Point / Option | Type | Description | Docs |
|:---|:---|:---|:---|
| `FastVideoStream.main(String[] args)` | `void` | Primary entry point for CLI and headless streaming pipeline. | [Wiki](docs/REFERENCE.md) |
| `--camera` | `flag` | Enables camera `0` with default bottom-right PiP overlay. | [Wiki](docs/REFERENCE.md) |
| `--camera=N` | `option` | Adds camera index `N` at the default bottom-right rectangle. | [Wiki](docs/REFERENCE.md) |
| `--camera=x,y,w,h` | `option` | Adds camera `0` at custom pixel PiP rectangle. | [Wiki](docs/REFERENCE.md) |
| `--camera-index=N` | `option` | Explicit alias for selecting camera device index `N`. | [Wiki](docs/REFERENCE.md) |
| `--list-cameras` | `flag` | Lists all detected camera devices with indices and exits. | [Wiki](docs/REFERENCE.md) |
| `--source=screen` | `option` | Streams the selected display monitor only. | [Wiki](docs/REFERENCE.md) |
| `--source=screen-camera` | `option` | Streams display monitor with real-time camera PiP overlay. | [Wiki](docs/REFERENCE.md) |
| `--source=camera` | `option` | Streams selected camera as the exclusive full-screen video source. | [Wiki](docs/REFERENCE.md) |
| `--monitor=N` | `option` | Selects display monitor index `N` for DXGI capture. | [Wiki](docs/REFERENCE.md) |
| `--fps=N` | `option` | Sets capture and stream frame rate (default: `60`). | [Wiki](docs/REFERENCE.md) |
| `--bitrate=N` | `option` | Sets target H.264 stream bitrate in kbit/s (default: `6000`). | [Wiki](docs/REFERENCE.md) |
| `--encoder=name` | `option` | Sets hardware encoder (`h264_nvenc`, `h264_qsv`, `libx264`). | [Wiki](docs/REFERENCE.md) |
| `--ffmpeg=path` | `option` | Specifies explicit path to custom `ffmpeg.exe` binary. | [Wiki](docs/REFERENCE.md) |
| `--no-cursor` | `flag` | Disables mouse cursor compositing in desktop capture. | [Wiki](docs/REFERENCE.md) |
| `--microphone` | `flag` | Enables default WASAPI microphone capture. | [Wiki](docs/REFERENCE.md) |
| `--system-audio` | `flag` | Enables Windows WASAPI loopback audio capture. | [Wiki](docs/REFERENCE.md) |
| `--audio` | `flag` | Enables microphone and system audio combined with `amix`. | [Wiki](docs/REFERENCE.md) |
| `FAST_YOUTUBE_KEY` | `env` | YouTube RTMPS stream key read from environment. | [Wiki](docs/REFERENCE.md) |
| `FAST_TWITCH_KEY` | `env` | Twitch RTMPS stream key read from environment. | [Wiki](docs/REFERENCE.md) |

See [docs/REFERENCE.md](docs/REFERENCE.md) for the full contract.

---

## Technical Demos & Benchmarks

| Case | Java Example | Launcher | Description |
|:---|:---|:---|:---|
| **Headless CLI Streamer** | [FastVideoStream.java](src/main/java/fastvideostream/FastVideoStream.java) | `run-cli.bat` | Production low-latency streaming pipeline to YouTube and Twitch. |
| **Interactive Demo** | [FastVideoStream.java](src/main/java/fastvideostream/FastVideoStream.java) | `run-demo.bat` | Packaged runnable launcher for desktop streaming. |

---

## Installation

### Option 1: Maven (Recommended via JitPack)

Add the JitPack repository and the complete dependency stack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastVideoStream Headless Streaming Engine -->
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

    <!-- FastCamera Native Windows Camera Capture -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCamera</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastScreenCapture Video & Screenshot Pipeline -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastScreenCapture</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastAudioCapture Live WASAPI Audio Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastAudioCapture</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- FastImage Off-Heap Image Processing -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastImage</artifactId>
        <version>0.1.4</version>
    </dependency>

    <!-- FastTheme Native Window Styling -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastTheme</artifactId>
        <version>0.1.4</version>
    </dependency>

    <!-- FastANSI Fast Terminal Formatting -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastANSI</artifactId>
        <version>0.1.3</version>
    </dependency>

    <!-- FastCore Unified Native JNI Loader -->
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
    implementation 'com.github.andrestubbe:FastCamera:0.1.1'
    implementation 'com.github.andrestubbe:FastScreenCapture:0.1.1'
    implementation 'com.github.andrestubbe:FastAudioCapture:0.1.0'
    implementation 'com.github.andrestubbe:FastImage:0.1.4'
    implementation 'com.github.andrestubbe:FastTheme:0.1.4'
    implementation 'com.github.andrestubbe:FastANSI:0.1.3'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (Pre-built JAR)

Download the pre-compiled standalone JAR directly from the GitHub Release:

- 📦 [**FastVideoStream-0.1.1.jar**](https://github.com/andrestubbe/FastVideoStream/releases/download/0.1.1/FastVideoStream-0.1.1.jar)
- ⚙️ [**fastcore-0.1.0.jar**](https://github.com/andrestubbe/FastCore/releases) (Unified JNI Loader)

### Option 4: Build from Source

```powershell
git clone https://github.com/andrestubbe/FastVideoStream.git
cd FastVideoStream
mvn clean package
```

FFmpeg remains an external executable. Install a build with the selected encoder, or pass its location through `--ffmpeg`. FastAudioCapture supplies live PCM audio through the Maven/JitPack dependency.

### Option 5: Windows Launcher

```text
set FAST_YOUTUBE_KEY=your-youtube-key
set FAST_TWITCH_KEY=your-twitch-key
run-demo.bat

run-cli.bat --camera --fps=60 --bitrate=6000
```

---

## Documentation

- **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version notes.
- **[COMPILE.md](docs/COMPILE.md)**: Build and packaging instructions.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Design principles and architecture.
- **[REFERENCE.md](docs/REFERENCE.md)**: CLI, pipeline, and output contract.
- **[ROADMAP.md](docs/ROADMAP.md)**: Planned work and roadmap.

---

## Platform Support

| Platform | Architecture | Status | Notes |
|:---|:---|:---|:---|
| Windows 10/11 | x64 | ✅ Fully Supported | DXGI desktop duplication, WASAPI audio, QSV/NVENC |
| Linux | x64, ARM64 | 🚧 Planned | FastScreen X11/Wayland backend required |
| macOS | Apple Silicon, x64 | 🚧 Planned | FastScreen ScreenCaptureKit backend required |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastScreen](https://github.com/andrestubbe/FastScreen) — DXGI desktop capture.
- [FastScreenCapture](https://github.com/andrestubbe/FastScreenCapture) — Screenshots and local recording.
- [FastCamera](https://github.com/andrestubbe/FastCamera) — Windows camera capture.
- [FastAudioCapture](https://github.com/andrestubbe/FastAudioCapture) — WASAPI audio capture.
- [FastImage](https://github.com/andrestubbe/FastImage) — Off-heap image processing.

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋

