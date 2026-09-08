# FastVideoStream Reference

## 1. Runtime Model

`fastvideostream.FastVideoStreamApp` is a headless Java 17 CLI. It owns one FastScreen instance, an optional FastCamera instance, one FFmpeg child process, and one frame-pump loop.

## 2. Input and Composition

### Desktop Capture

The selected monitor is captured through `FastScreen.captureRaw(0, 0, 0, 0)`. The returned ARGB pixels are converted into the BGRA byte order expected by the FFmpeg raw-video input.

### Camera PiP

`--camera` opens the first enumerated FastCamera device and places its BGRA callback frame in a 16:9 overlay at the bottom right of the desktop frame.

### Cursor

Cursor compositing is enabled by default and can be disabled with `--no-cursor`.

## 3. FFmpeg Contract

The process receives raw video on `pipe:0`:

- Pixel format: `bgra`
- Frame rate: selected by `--fps`
- Size: selected monitor dimensions
- Encoder rate control: CBR
- Keyframe interval: two seconds
- Output: FFmpeg `tee` muxer with FLV/RTMPS slaves

## 4. CLI Specification

| Option | Default | Meaning |
|---|---:|---|
| `--camera` | off | Enable first-camera PiP. |
| `--monitor=INDEX` | `0` | Select monitor. |
| `--fps=FPS` | `60` | Capture and encoder frame rate. |
| `--bitrate=KBIT` | `6000` | Video bitrate in kbit/s. |
| `--encoder=NAME` | `h264_nvenc` | FFmpeg video encoder. |
| `--ffmpeg=PATH` | `ffmpeg` | FFmpeg executable. |
| `--no-cursor` | off | Disable cursor compositing. |
| `FAST_YOUTUBE_KEY` | unset | YouTube stream key. |
| `FAST_TWITCH_KEY` | unset | Twitch stream key. |

At least one platform key is required.

## 5. Platform Support

The current implementation targets Windows 10/11 x64, Java 17+, and an FFmpeg build with the selected encoder. YouTube and Twitch require valid RTMPS stream credentials and sufficient upload bandwidth.

## 6. Guarantees and Limitations

The current release guarantees only the video path described above. It does not yet guarantee live audio, automatic reconnect, stream health metrics, scene graphs, or synchronized multi-source composition.
