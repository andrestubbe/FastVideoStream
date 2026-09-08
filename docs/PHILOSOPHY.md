# The Philosophy of FastVideoStream

> "Capture directly. Encode once. Keep the streaming edge small."

FastVideoStream is an orchestration layer, not a second capture engine. The performance-critical Windows work belongs in the existing FastJava backends. This repository owns the stream lifecycle, FFmpeg process, destination fan-out, and CLI contract.

## Core Tenets

1. **Reuse the proven capture substrate**

   FastScreen remains the source of desktop frames. FastVideoStream must not duplicate DXGI capture logic.

2. **One encoded stream, multiple destinations**

   YouTube and Twitch should share one encoder process whenever their output contract allows it.

3. **Bounded allocation in the frame pump**

   Reuse conversion buffers and keep per-frame allocation out of the hot loop.

4. **Externalize codec complexity**

   FFmpeg owns codec and container behavior. The Java layer supplies frames, configuration, lifecycle, and diagnostics.

5. **Keep audio live**

   Audio is captured through FastAudioCapture callbacks and fed directly into the running FFmpeg process. Recording to a temporary WAV file and muxing after the fact is not a streaming architecture.

6. **Credentials never belong in source**

   Stream keys come from environment variables or explicit runtime configuration and must never be persisted by the application.

7. **Truthful scope before feature claims**

   Audio, reconnect, scenes, and GUI controls are separate engineering problems. They must be implemented and tested before being advertised as supported.

8. **FastJava boundary discipline**

   FastVideoStream consumes FastScreen, FastCamera, FastAudioCapture, and related modules through published Maven/JitPack artifacts. It does not modify those repositories.
