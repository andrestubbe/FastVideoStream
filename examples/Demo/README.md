# FastVideoStream CLI Demo

Set `FAST_YOUTUBE_KEY` and/or `FAST_TWITCH_KEY`, ensure `ffmpeg` is on `PATH`, then run:

```text
mvn exec:java -Dexec.args="--camera --fps=60 --bitrate=6000"
```
