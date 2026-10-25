# FastVideoStream Demo & CLI

Launch the interactive streaming demo:

```powershell
# Set destination stream key
$env:FAST_YOUTUBE_KEY = "your-stream-key"

# Run interactive demo
.\run-demo.bat
```

Or execute directly via Maven:

```text
mvn exec:java -Dexec.args="--fps=60 --bitrate=6000 --encoder=h264_qsv"
```

