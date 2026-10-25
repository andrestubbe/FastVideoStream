# FastVideoStream Reference

## 1. Architecture & Engine Model

FastVideoStream provides a clean, decoupled low-level streaming engine designed for high-performance screen broadcast pipelines.

### Interfaces and Engine Classes

- **`fastvideostream.VideoStreamEngine`**: Pure AutoCloseable streaming interface:
  - `void start(StreamSessionConfig config, StreamStatusListener listener) throws Exception`
  - `void start(StreamSessionConfig config, FrameInterceptor interceptor, StreamStatusListener listener) throws Exception`
  - `boolean isStreaming()`
  - `void stop()`
  - `void close()`
- **`fastvideostream.FrameInterceptor`**: Functional callback (`onFrame(int[] pixels, int width, int height)`) for zero-copy frame manipulation (Webcam PiP, watermarks, custom overlays) before encoding.
- **`fastvideostream.D3D11VideoStreamEngine`**: DirectX 11 DXGI Desktop Duplication engine implementation that pipes raw BGRA frames directly to hardware encoders.
- **`fastvideostream.StreamSessionConfig`**: Immutable configuration record containing:
  - `monitorIndex`: Display index for DXGI capture.
  - `fps`: Target frame rate (e.g. 30 or 60 FPS).
  - `bitrateKbps`: Target video bitrate in kbit/s (e.g. 4000).
  - `includeCursor`: Boolean toggle for hardware mouse cursor compositing via `FastCursor`.
  - `encoder`: Hardware video encoder name (`h264_qsv`, `h264_nvenc`, `h264_amf`, or `libx264`).
  - `ffmpegPath`: Custom binary path or fallback to bundled/system FFmpeg.
  - `rtmpsTargetUrl`: Complete RTMPS destination URL including stream key.
  - `targetType`: Target provider type (`StreamTargetType`).
- **`fastvideostream.StreamTargetType`**: Provider target enumeration (`YOUTUBE`, `TWITCH`, `KICK`, `FACEBOOK`, `CUSTOM_RTMP`, `SRT`, `RTSP`).
- **`fastvideostream.StreamProviderEndpoints`**: Pre-configured RTMPS/RTMP endpoint URLs for streaming providers.
- **`fastvideostream.StreamStatusListener`**: Callback interface receiving real-time events:
  - `onStarted(int width, int height, int fps)`
  - `onFramePushed(long frameNumber, long elapsedNanos)`
  - `onError(Throwable throwable)`
  - `onStopped()`
- **`fastvideostream.FrameInterceptor`**: Functional callback interface allowing application layers (e.g. `Stream+`) to composite webcam PiP, watermarks, or overlays directly onto raw BGRA screen pixels before encoding.

## 2. Desktop Capture & Frame Pipeline

The desktop frame is captured through `FastScreen` using native DXGI Desktop Duplication:
1. Native DirectX 11 textures are accessed off-heap.
2. Pixel buffers are converted into the BGRA byte order expected by the encoder.
3. Frames are fed over `pipe:0` directly into FFmpeg with zero heap allocation in the hot loop.

## 3. Hardware Encoder Configuration

Hardware acceleration parameters are calibrated for low latency and smooth transmission:

- **Intel QuickSync (`h264_qsv`)**: `-preset veryfast`, CBR rate control, NV12 pixel format, 2-second GOP (`fps * 2`).
- **NVIDIA NVENC (`h264_nvenc`)**: `-preset ultrafast`, `-tune zerolatency`, CBR rate control, 2-second GOP.
- **Container Output**: Low-overhead FLV stream sent directly to the destination RTMPS URL.

## 4. Standalone CLI Launcher

The standalone CLI entry point (`fastvideostream.FastVideoStream`) supports direct script launches:

| Option | Default | Description |
|---|---:|---|
| `--monitor=N` | `0` | Monitor display index. |
| `--fps=N` | `30` | Capture and stream frame rate. |
| `--bitrate=N` | `4000` | Target H.264 video bitrate in kbps. |
| `--encoder=name` | `h264_qsv` | Video encoder (`h264_qsv`, `h264_nvenc`, `libx264`). |
| `--ffmpeg=path` | `ffmpeg` | FFmpeg executable path. |
| `--no-cursor` | `false` | Disable cursor compositing. |
| `--youtube-key=key` | env `FAST_YOUTUBE_KEY` | Explicit YouTube stream key. |
| `--twitch-key=key` | env `FAST_TWITCH_KEY` | Explicit Twitch stream key. |


At least one platform key is required.

## 5. Platform Support

The current implementation targets Windows 10/11 x64, Java 17+, and an FFmpeg build with the selected encoder. YouTube and Twitch require valid RTMPS stream credentials and sufficient upload bandwidth.

## 6. Guarantees and Limitations

The current release guarantees the described video path and optional live audio path. It does not yet guarantee automatic reconnect, stream health metrics, scene graphs, or synchronized multi-source composition.
