# FastVideoStream Roadmap

## Green 0.1.0: CLI Video Stream (Completed)

- [x] FastScreen monitor capture.
- [x] Optional FastCamera PiP.
- [x] Cursor compositing.
- [x] One FFmpeg encode to YouTube and Twitch.
- [x] Maven/JitPack-compatible repository layout.

## Yellow 0.2.0: Audio and Reliability

- [ ] Add FastAudioCapture microphone input.
- [ ] Add WASAPI system-audio loopback.
- [ ] Synchronize audio and video timestamps.
- [ ] Report encoder and destination health.
- [ ] Reconnect failed destinations independently.

## Orange 0.5.0: Composition API

- [ ] Introduce reusable scene/source model.
- [ ] Support configurable camera position and scale.
- [ ] Add region and window capture configuration.
- [ ] Add bounded frame queues and backpressure policy.

## Red 1.0.0: Studio Integration

- [ ] Add the Swing streaming tab in the application layer.
- [ ] Add portable Windows distribution with FFmpeg licensing notices.
- [ ] Add integration tests with a local RTMP test sink.
- [ ] Publish performance and reliability benchmarks.
