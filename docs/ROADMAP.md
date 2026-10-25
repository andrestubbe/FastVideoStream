# FastVideoStream Roadmap

## Green 0.1.0: Native Desktop Duplication & Streaming Engine (Completed)

- [x] FastScreen DirectX 11 DXGI monitor capture.
- [x] Hardware H.264 video encoding (Intel QSV, NVIDIA NVENC, AMD AMF, CPU x264).
- [x] Hardware cursor compositing via FastScreenCapture.
- [x] Zero-allocation off-heap BGRA frame buffer loop.
- [x] Direct RTMPS ingest to YouTube Live and Twitch over FFmpeg pipe.
- [x] Maven / JitPack-compatible multi-project repository layout.

## Green 0.1.1: Architecture Decoupling & Pure Engine Model (Current)

- [x] Public `VideoStreamEngine` interface decoupling core streaming from UI/Audio.
- [x] `StreamSessionConfig` immutable setup record with fluent builder.
- [x] `StreamStatusListener` telemetry callback interface (`onStarted`, `onFramePushed`, `onError`, `onStopped`).
- [x] `FrameInterceptor` callback interface for zero-copy application compositing (Webcam PiP in Stream+).
- [x] Removed `FastCamera` and hardcoded PiP logic from engine core.
- [x] Streamlined dependencies to pure capture/encoding substrate.

## Yellow 0.2.0: Resilient Ingest & Transport

- [ ] Network backpressure handling and adaptive bitrate throttling.
- [ ] Ingest health telemetry (dropped frames, network buffer fill level).
- [ ] Automatic RTMPS reconnection on transient network drops.

## Orange 0.3.0: Native Memory & Direct GPU Surface Pumping

- [ ] Direct D3D11 texture surface sharing without intermediate CPU RAM roundtrips.
- [ ] AV1 hardware encoder support (`av1_qsv`, `av1_nvenc`).
- [ ] Bounded frame queues with custom drop policies.
