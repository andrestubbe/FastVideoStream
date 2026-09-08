# Building FastVideoStream

## Prerequisites

- Windows 10 or newer, x64.
- JDK 17 or newer.
- Maven 3.9 or newer.
- FFmpeg available on `PATH` for runtime streaming.
- Published FastJava dependencies available through Maven/JitPack.

## Automated One-Click Build

From the repository root:

```text
compile.bat
```

The script runs a clean Maven install without tests.

## Maven Java Packaging

```text
mvn clean test
mvn clean package -DskipTests
```

The executable artifact is written to `target/FastVideoStream-0.1.0.jar`.

## CLI Smoke Test

Set stream keys without committing them:

```powershell
$env:FAST_YOUTUBE_KEY = "your-youtube-key"
$env:FAST_TWITCH_KEY = "your-twitch-key"
mvn exec:java -Dexec.args="--camera --fps=60 --bitrate=6000"
```

## Runtime Notes

Use `--ffmpeg=...` when FFmpeg is not on `PATH`. The default encoder is `h264_nvenc`; select another encoder explicitly when the installed FFmpeg build or GPU does not support NVENC.

## Troubleshooting

### FFmpeg is not found

Install FFmpeg and add its `bin` directory to `PATH`, or pass an absolute path with `--ffmpeg`.

### The encoder is unavailable

Run `ffmpeg -encoders` and choose an installed encoder, for example `h264_qsv`, `h264_amf`, or `libx264`.

### The camera is unavailable

Start without `--camera`, verify Windows camera permissions, and check that another application is not holding the device.
