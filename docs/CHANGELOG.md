# Changelog

All notable changes to FastVideoStream are documented here.

## [0.1.1] - 2026-10-25

### Added
- Pure `VideoStreamEngine` interface decoupling the streaming core from application-level UI and audio complexity.
- `StreamSessionConfig` immutable record with fluent builder for monitor index, FPS, bitrate, cursor toggle, encoder, and RTMPS target URL.
- `StreamStatusListener` telemetry callback interface (`onStarted`, `onFramePushed`, `onError`, `onStopped`).
- `D3D11VideoStreamEngine` concrete DirectX 11 / DXGI Desktop Duplication pipeline with FFmpeg hardware encoding (`h264_qsv`, `h264_nvenc`, `h264_amf`, `libx264`).

### Changed
- Refactored library scope into a focused, low-overhead native video engine for FastJava.
- Streamlined documentation, code examples, and installation instructions for 0.1.1.

## [0.1.0] - 2026-09-08

### Added

- Optional live microphone and WASAPI system-audio capture through FastAudioCapture.
- Optional stereo AAC encoding at 48 kHz, including microphone/system-audio mixing.
- Swing control window with native capture exclusion and separate headless CLI entry point.
- Configurable camera PiP rectangle through `--camera=x,y,w,h`.
- Java 17 Maven/JitPack project structure.

- Microphone and system audio are not yet mixed into the stream.
- Destination reconnect and per-destination health reporting are not yet implemented.
- Scenes, source lists, and the Swing streaming tab are not yet implemented.
