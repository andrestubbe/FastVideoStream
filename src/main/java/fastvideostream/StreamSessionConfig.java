package fastvideostream;

/**
 * Configuration for an active VideoStream session.
 */
public record StreamSessionConfig(
        int monitorIndex,
        int fps,
        int bitrateKbps,
        boolean includeCursor,
        String encoder,
        String ffmpegPath,
        String rtmpsTargetUrl,
        StreamTargetType targetType
) {
    public StreamSessionConfig(
            int monitorIndex,
            int fps,
            int bitrateKbps,
            boolean includeCursor,
            String encoder,
            String ffmpegPath,
            String rtmpsTargetUrl
    ) {
        this(monitorIndex, fps, bitrateKbps, includeCursor, encoder, ffmpegPath, rtmpsTargetUrl, StreamTargetType.CUSTOM);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int monitorIndex = 0;
        private int fps = 30;
        private int bitrateKbps = 4000;
        private boolean includeCursor = true;
        private String encoder = "h264_qsv";
        private String ffmpegPath = "bin/ffmpeg.exe";
        private String rtmpsTargetUrl;
        private StreamTargetType targetType = StreamTargetType.CUSTOM;

        public Builder monitorIndex(int val) { this.monitorIndex = val; return this; }
        public Builder fps(int val) { this.fps = val; return this; }
        public Builder bitrateKbps(int val) { this.bitrateKbps = val; return this; }
        public Builder includeCursor(boolean val) { this.includeCursor = val; return this; }
        public Builder encoder(String val) { this.encoder = val; return this; }
        public Builder ffmpegPath(String val) { this.ffmpegPath = val; return this; }
        public Builder rtmpsTargetUrl(String val) { this.rtmpsTargetUrl = val; return this; }
        public Builder targetType(StreamTargetType type) { this.targetType = type; return this; }

        public Builder provider(StreamTargetType type, String streamKeyOrAddress) {
            this.targetType = type;
            this.rtmpsTargetUrl = type.buildTargetUrl(streamKeyOrAddress);
            return this;
        }

        public StreamSessionConfig build() {
            if (rtmpsTargetUrl == null || rtmpsTargetUrl.isBlank()) {
                throw new IllegalArgumentException("rtmpsTargetUrl must not be empty.");
            }
            return new StreamSessionConfig(monitorIndex, fps, bitrateKbps, includeCursor, encoder, ffmpegPath, rtmpsTargetUrl, targetType);
        }
    }
}
