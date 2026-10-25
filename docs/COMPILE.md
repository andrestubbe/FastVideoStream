# Building FastVideoStream

## Prerequisites

- Java 17 or higher
- Maven 3.9+
- FFmpeg installed and available on `PATH` (or specified via configuration)

## Automated Build & Installation

From the repository root:

```cmd
mvn clean install -DskipTests
```

The compiled library JAR is written to `target/FastVideoStream-0.1.1.jar` and installed into your local Maven cache (`.m2/repository/com/github/andrestubbe/FastVideoStream/0.1.1/`).

## CLI Smoke Test

Set a test stream key in the environment:

```powershell
$env:FAST_YOUTUBE_KEY = "your-youtube-key"
$env:FAST_TWITCH_KEY = "your-twitch-key"

# Run headless video streaming engine
mvn exec:java -Dexec.args="--fps=60 --bitrate=6000 --encoder=h264_qsv"
```

## Runtime Notes & Hardware Encoders

- **Hardware Acceleration:** By default, FastVideoStream prefers Intel QuickSync (`h264_qsv`). NVIDIA NVENC (`h264_nvenc`), AMD AMF (`h264_amf`), or CPU (`libx264`) can be selected via `--encoder=<name>`.
- **Custom FFmpeg Path:** Use `--ffmpeg=C:\path\to\ffmpeg.exe` if FFmpeg is not in your system `PATH`.
- **Compositing & Overlays:** Frame compositing (such as Webcam PiP, watermarks, or chat overlays) is handled via the `FrameInterceptor` interface in application layers (e.g. `Stream+`).

## Troubleshooting

### FFmpeg is not found
Install FFmpeg and add its `bin` directory to your system `PATH`, or pass an explicit binary path with `--ffmpeg`.

### The encoder is unavailable
Run `ffmpeg -encoders` to check hardware support. If Intel QSV is not available, switch to `h264_nvenc` (NVIDIA) or `libx264` (software).
