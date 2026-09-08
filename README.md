# FastVideoStream 0.1.0 - Low-Overhead CLI Video Streaming for Java

[![Status](https://img.shields.io/badge/status-0.1.0-orange.svg)](https://github.com/andrestubbe/FastVideoStream)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.java.com)
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastVideoStream)

FastVideoStream is the CLI video-streaming layer for the FastJava ecosystem. It
reuses the DXGI capture path from FastScreen, optionally adds a FastCamera
picture-in-picture overlay, encodes once through FFmpeg, and fans out to
YouTube and Twitch through the tee muxer.

It is intentionally a small, headless streaming engine rather than a Swing
application. The capture loop remains close to FastScreenCapture: one reusable
BGRA frame buffer, one capture thread, and one encoder process.

## Current Scope

- Windows 10+
- Java 17+
- One monitor selected by index
- Optional first-camera bottom-right PiP
- Optional cursor compositing
- One H.264 encode sent to YouTube, Twitch, or both
- NVIDIA NVENC by default, with any installed FFmpeg encoder selectable
- Stream keys supplied through environment variables only

Live microphone and system-audio mixing, automatic per-destination reconnect,
scene composition, and the Swing streaming tab are planned follow-up features;
this release does not claim to replace the complete OBS feature set.

## Build

```powershell
mvn clean test
```

## Configuration

Set `FAST_YOUTUBE_KEY` and/or `FAST_TWITCH_KEY` as environment variables. Stream keys are intentionally not stored in the repository.

Example:

```powershell
set FAST_YOUTUBE_KEY=your-youtube-key
set FAST_TWITCH_KEY=your-twitch-key
mvn exec:java -Dexec.args="--camera --fps=60 --bitrate=6000"
```

The stream keys are intentionally read from environment variables and are never
stored in the repository. FFmpeg must be available on `PATH` or passed with
`--ffmpeg=C:\\path\\to\\ffmpeg.exe`.

For a portable build, use `run-demo.bat`. Do not put stream keys in command
history, source files, README files, or GitHub Actions logs.

## Options

- `--camera` enables the first available camera as bottom-right PiP.
- `--fps=60` selects the capture frame rate.
- `--bitrate=6000` selects the video bitrate in kbit/s.
- `--encoder=h264_nvenc` selects the FFmpeg video encoder.
- `--monitor=0` selects the monitor index.
- `--no-cursor` disables cursor compositing.

## FFmpeg Profiles

For Twitch, `6000` kbit/s at 1080p60 is a practical ceiling. For YouTube,
choose the bitrate according to the target resolution and available upload
bandwidth. The default GOP is two seconds (`fps * 2`) and the video rate is
constant bitrate (`CBR`).

## Maven Dependency

```xml
<dependency>
	<groupId>com.github.andrestubbe</groupId>
	<artifactId>FastVideoStream</artifactId>
	<version>0.1.0</version>
</dependency>
```

The capture backends are consumed as published Maven/JitPack artifacts. This
repository does not modify FastScreen, FastCamera, or FastScreenCapture.

## Related Projects

- [FastScreen](https://github.com/andrestubbe/FastScreen) - DXGI desktop capture
- [FastScreenCapture](https://github.com/andrestubbe/FastScreenCapture) - screenshots and local recording
- [FastCamera](https://github.com/andrestubbe/FastCamera) - Windows camera capture
- [FastAudioCapture](https://github.com/andrestubbe/FastAudioCapture) - WASAPI audio capture

## License

MIT. See [LICENSE](LICENSE).
