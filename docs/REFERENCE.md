# FastStream Reference

## CLI

`fastvideostream.FastVideoStreamApp` reads `FAST_YOUTUBE_KEY` and `FAST_TWITCH_KEY`, captures one monitor through `FastScreen`, optionally adds the first camera through `FastCamera`, and sends one encoded H.264 video stream to both targets through FFmpeg's tee muxer.

## Current limitation

The initial CLI path is video-only. Live microphone and system-audio mixing is tracked in `docs/ROADMAP.md` and must be added before using it as a complete OBS replacement.
