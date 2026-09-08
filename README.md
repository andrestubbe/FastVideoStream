# FastVideoStream 0.1.0 [ALPHA-2026-09-08] - Low-Overhead CLI Video Streaming for Java

[![Status](https://img.shields.io/badge/status-0.1.0-orange.svg)](https://github.com/andrestubbe/FastVideoStream/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010%2B-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastVideoStream)

---

**The low-overhead video streaming layer for the FastJava ecosystem.**

FastVideoStream reuses the DXGI desktop capture path from **FastScreen**, adds optional **FastCamera** picture-in-picture, encodes once through FFmpeg, and sends the same H.264 stream to YouTube, Twitch, or both.

The project is intentionally headless and CLI-first. Its capture loop follows the same small, direct shape as FastScreenCapture: one capture loop, one reusable conversion buffer, and one encoder process.

---

## Quick Start

Requirements: Windows 10+, Java 17+, FFmpeg on `PATH`, and a YouTube and/or Twitch stream key.

```powershell
$env:FAST_YOUTUBE_KEY = "your-youtube-key"
$env:FAST_TWITCH_KEY = "your-twitch-key"
mvn clean package
java -jar target/FastVideoStream-0.1.0.jar
```

The JAR starts the Swing control window. Use `run-demo.bat` for the Swing launcher or `run-cli.bat --camera=20,20,480,270 --audio --fps=60 --bitrate=6000` for headless operation.

---

## Table of Contents

- [Why FastVideoStream?](#why-fastvideostream)
- [Quick Start](#quick-start)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Architecture & Pipeline](#architecture--pipeline)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
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

---

## Key Features

- Desktop capture from a selected monitor through FastScreen.
- Optional first-camera picture-in-picture overlay at `x,y,w,h` coordinates.
- Selectable source modes: screen, screen plus camera, or camera only.
- Optional cursor compositing.
- Optional live microphone and WASAPI system-audio capture through FastAudioCapture.
- Live microphone/system-audio mixing to stereo AAC at 48 kHz.
- Live microphone input with `--microphone`.
- Live Windows system-audio loopback with `--system-audio`.
- Simultaneous microphone and system-audio mixing with `--audio`.
- H.264 hardware encoding through NVENC by default.
- Configurable FFmpeg encoder, FPS, bitrate, monitor, and FFmpeg path.
- Simultaneous RTMPS output to YouTube and Twitch.
- Headless CLI operation suitable for scripts and portable Windows deployments.
- Swing control window excluded from FastScreen capture through native window affinity.
- Maven/JitPack-compatible Java 17 project.

---

## Real-World Use Cases

- **Low-overhead game or desktop streaming:** Capture one monitor and publish to one or two platforms.
- **Developer demos:** Stream a coding session with an optional camera overlay.
- **QA and support:** Share a reproducible desktop capture path without opening a full studio UI.
- **FastJava integration:** Use the CLI as the streaming edge around the FastScreen and FastCamera libraries.

---

## Architecture & Pipeline

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

| Entry point or option | Description |
|---|---|
| `fastvideostream.FastVideoStreamApp` | Swing control-window entry point. |
| `fastvideostream.FastVideoStreamCli` | Headless CLI entry point. |
| `--camera` | Adds camera `0` at the default bottom-right rectangle. |
| `--camera=N` | Adds camera index `N` at the default bottom-right rectangle. |
| `--camera=x,y,w,h` | Adds camera `0` at the given PiP rectangle. |
| `--camera-index=N` | Explicit alias for selecting camera index `N`. |
| `--list-cameras` | Lists available cameras with their indexes and exits. |
| `--source=screen` | Stream the selected screen only. |
| `--source=screen-camera` | Stream the selected screen with camera PiP. |
| `--source=camera` | Stream the selected camera as the full video source. |
| `--monitor=0` | Selects the monitor index. |
| `--fps=60` | Sets the input frame rate. |
| `--bitrate=6000` | Sets video bitrate in kbit/s. |
| `--encoder=h264_nvenc` | Selects the FFmpeg video encoder. |
| `--ffmpeg=C:\\path\\ffmpeg.exe` | Selects a specific FFmpeg executable. |
| `--no-cursor` | Disables cursor compositing. |
| `--microphone` | Adds the default WASAPI microphone. |
| `--system-audio` | Adds Windows WASAPI loopback audio. |
| `--audio` | Enables microphone and system audio together. |
| `FAST_YOUTUBE_KEY` | YouTube stream key from the environment. |
| `FAST_TWITCH_KEY` | Twitch stream key from the environment. |

See [docs/REFERENCE.md](docs/REFERENCE.md) for the full contract.

---

## Installation

### Option 1: Maven (Recommended)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>FastVideoStream</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Option 2: Build from Source

```powershell
git clone https://github.com/andrestubbe/FastVideoStream.git
cd FastVideoStream
mvn clean package
```

FFmpeg remains an external executable. Install a build with the selected encoder, or pass its location through `--ffmpeg`. FastAudioCapture supplies live PCM audio through the Maven/JitPack dependency.

### Option 3: Windows Launcher

```text
set FAST_YOUTUBE_KEY=your-youtube-key
set FAST_TWITCH_KEY=your-twitch-key
run-demo.bat

run-cli.bat --camera=20,20,480,270 --audio --fps=60 --bitrate=6000
```

---

## Documentation

- [CHANGELOG.md](docs/CHANGELOG.md) - Release history.
- [COMPILE.md](docs/COMPILE.md) - Build and packaging instructions.
- [PHILOSOPHY.md](docs/PHILOSOPHY.md) - Design principles.
- [REFERENCE.md](docs/REFERENCE.md) - CLI, pipeline, and output contract.
- [ROADMAP.md](docs/ROADMAP.md) - Planned work.

---

## Platform Support

| Platform | Status |
|---|---|
| Windows 10/11 x64 | Supported target |
| Linux | Not supported by the current FastScreen backend |
| macOS | Not supported by the current FastScreen backend |
| Java | 17 or newer |
| FFmpeg | Required at runtime |

---

## License

MIT. See [LICENSE](LICENSE).

---

## Related Projects

- [FastScreen](https://github.com/andrestubbe/FastScreen) - DXGI desktop capture.
- [FastScreenCapture](https://github.com/andrestubbe/FastScreenCapture) - Screenshots and local recording.
- [FastCamera](https://github.com/andrestubbe/FastCamera) - Windows camera capture.
- [FastAudioCapture](https://github.com/andrestubbe/FastAudioCapture) - WASAPI audio capture.
- [FastImage](https://github.com/andrestubbe/FastImage) - Off-heap image processing.
