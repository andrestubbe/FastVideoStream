# Changelog

All notable changes to FastVideoStream are documented here.

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
