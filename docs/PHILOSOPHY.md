# The Philosophy of FastVideoStream

> "Capture directly. Encode once. Keep the streaming edge small."

FastVideoStream is an orchestration layer, not a second capture engine. The performance-critical Windows work belongs in the existing FastJava backends. This repository owns the stream lifecycle, FFmpeg process, destination fan-out, and CLI contract.

## Core Tenets

1. **Reuse the proven capture substrate**

   FastScreen remains the source of desktop frames. FastVideoStream must not duplicate DXGI capture logic.

2. **Decoupled Low-Level Engine Architecture**

   Keep FastVideoStream strictly focused as a reusable video streaming engine (`VideoStreamEngine`). High-level UI, complex scenes, and platform authentication belong in higher application layers like Stream+.

3. **Bounded allocation in the frame pump**

   Reuse conversion buffers and keep per-frame allocation out of the hot loop.

4. **Externalize codec complexity**

   FFmpeg and native hardware encoders own codec and container behavior. The Java layer supplies frames, configuration, lifecycle, and diagnostics.

5. **Hardware encoding first**

   Prioritize native GPU hardware acceleration (Intel QuickSync `h264_qsv`, NVIDIA `h264_nvenc`, AMD `h264_amf`) with minimal CPU overhead.

6. **Credentials never belong in source**

   Stream keys and destination URLs come from runtime configuration or environment variables and must never be persisted in the repository.

7. **FastJava boundary discipline**

   FastVideoStream consumes FastScreen, FastScreenCapture, and related core modules through published Maven/JitPack artifacts. It does not modify those repositories and leaves application compositing to tools like Stream+.
