# Changelog

All notable changes to FastVideoStream are documented here.

## [0.1.2] - 2026-10-25

### Added
- **Multi-Provider Stream Ingest Mapping**: Added `StreamTargetType` (`YOUTUBE`, `TWITCH`, `KICK`, `FACEBOOK`, `CUSTOM_RTMP`, `SRT`, `RTSP`) and `StreamProviderEndpoints` for multi-platform broadcasting.
- **Direct FastCursor Native Blending**: Replaced reflection-based cursor loading with direct, hardware-accelerated `FastCursor.blendCursor(...)` from `FastCursor:0.1.2`.
- **Target Protocol Format Dispatch**: Automatic container mapping (`flv`, `mpegts`, `rtsp`, `hls`) based on target URL protocol in `D3D11VideoStreamEngine`.

### Changed
- **Compiler Target Upgrade**: Upgraded Java source and target release to Java 21 LTS across `pom.xml`.
- **Dependency Cleansing**: Stripped legacy `FastScreenCapture` dependency; `FastCursor` is now directly linked.

## [0.1.1] - 2026-10-25

### Removed
- **FastCamera Dependency**: Stripped out `FastCamera` and all internal camera capture logic.
- **Webcam PiP Blending**: Removed hardcoded camera scaling and picture-in-picture compositing from the core streaming pipeline.
- **CLI Camera Flags**: Removed `--camera` argument from `FastVideoStream.java` (compositing delegated to applications).

### Added
- **FrameInterceptor Interface**: Lightweight callback allowing external applications to manipulate or composite frames (e.g. Webcam PiP, overlays) before hardware encoding.
- **VideoStreamEngine Architecture**: Public interface (`VideoStreamEngine`, `D3D11VideoStreamEngine`) for reusable, headless streaming embedding.
- **StreamSessionConfig & StreamStatusListener**: Immutable session setup record with fluent builder and real-time lifecycle event listeners.

### Changed
- Refactored `FastVideoStream` into a pure, low-overhead native video-encoding engine with zero UI or camera bloat.

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
