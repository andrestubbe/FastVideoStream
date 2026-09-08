# Changelog

All notable changes to FastVideoStream are documented here.

## [0.1.0] - 2026-09-08

### Added

- Initial CLI video streaming entry point.
- FastScreen monitor capture with reusable BGRA conversion buffer.
- Optional FastCamera bottom-right PiP overlay.
- Optional cursor compositing.
- FFmpeg H.264 hardware-encoder selection.
- Simultaneous YouTube and Twitch RTMPS output through the tee muxer.
- Java 17 Maven/JitPack project structure.

### Known Limitations

- Microphone and system audio are not yet mixed into the stream.
- Destination reconnect and per-destination health reporting are not yet implemented.
- Scenes, source lists, and the Swing streaming tab are not yet implemented.
