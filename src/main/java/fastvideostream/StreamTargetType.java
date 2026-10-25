package fastvideostream;

/**
 * Comprehensive registry of supported streaming target providers across the FastJava streaming ecosystem.
 */
public enum StreamTargetType {
    // 1. Cloud Live Streaming Platforms (RTMPS / RTMP)
    YOUTUBE("YouTube Live", "rtmps://a.rtmps.youtube.com/live2/", true, 443, "RTMPS"),
    TWITCH("Twitch Live", "rtmps://live.twitch.tv/app/", true, 443, "RTMPS"),
    KICK("Kick Live", "rtmps://fa723fc1b171.global-contribute.live-video.net:443/app/", true, 443, "RTMPS"),
    TIKTOK("TikTok Live", "rtmps://open-api.tiktok.com/live/", true, 443, "RTMPS"),
    X_TWITTER("X (Twitter) Live", "rtmps://prod-fastly-eu-central-1.video.pscp.tv:443/x/", true, 443, "RTMPS"),
    FACEBOOK("Facebook Live", "rtmps://live-api-s.facebook.com:443/rtmp/", true, 443, "RTMPS"),
    LINKEDIN("LinkedIn Live", "rtmps://127.0.0.1:443/live/", true, 443, "RTMPS"),
    RESTREAM("Restream.io", "rtmp://live.restream.io/live/", false, 1935, "RTMP"),

    // 2. Pro Broadcast & Low-Latency Network Ingest
    SRT("Secure Reliable Transport", "srt://", false, 9000, "SRT"),
    RIST("Reliable Internet Stream Transport", "rist://", false, 1968, "RIST"),
    RTSP("RTSP Publish", "rtsp://", false, 8554, "RTSP"),
    UDP("Raw UDP Multicast / Unicast", "udp://", false, 5000, "UDP"),
    HLS_LOCAL("Local HLS Webstream", "http://127.0.0.1:8080/live/stream.m3u8", false, 8080, "HLS"),

    // 3. Flexible Custom Endpoint (WHIP, WebRTC, Nginx, Local Sink)
    CUSTOM("Custom Endpoint", "", false, 0, "Custom");

    private final String label;
    private final String defaultIngestBaseUrl;
    private final boolean sslRequired;
    private final int defaultPort;
    private final String protocol;

    StreamTargetType(String label, String defaultIngestBaseUrl, boolean sslRequired, int defaultPort, String protocol) {
        this.label = label;
        this.defaultIngestBaseUrl = defaultIngestBaseUrl;
        this.sslRequired = sslRequired;
        this.defaultPort = defaultPort;
        this.protocol = protocol;
    }

    public String label() {
        return label;
    }

    public String defaultIngestBaseUrl() {
        return defaultIngestBaseUrl;
    }

    public boolean isSslRequired() {
        return sslRequired;
    }

    public int defaultPort() {
        return defaultPort;
    }

    public String protocol() {
        return protocol;
    }

    public boolean isNetworkStream() {
        return this == SRT || this == RIST || this == UDP || this == RTSP || this == HLS_LOCAL;
    }

    /**
     * Constructs the full destination ingest URL for a given stream key or target host.
     */
    public String buildTargetUrl(String keyOrAddress) {
        if (keyOrAddress == null || keyOrAddress.isBlank()) {
            return defaultIngestBaseUrl;
        }
        String clean = keyOrAddress.trim();
        if (this == CUSTOM || isNetworkStream()) {
            return clean;
        }
        return defaultIngestBaseUrl.endsWith("/") ? defaultIngestBaseUrl + clean : defaultIngestBaseUrl + "/" + clean;
    }

    /**
     * Resolves provider enum by case-insensitive name or alias.
     */
    public static StreamTargetType parse(String value) {
        if (value == null || value.isBlank()) return YOUTUBE;
        String v = value.trim().toLowerCase();
        return switch (v) {
            case "youtube", "yt" -> YOUTUBE;
            case "twitch", "tw" -> TWITCH;
            case "kick" -> KICK;
            case "tiktok", "tt" -> TIKTOK;
            case "x", "twitter", "x_twitter" -> X_TWITTER;
            case "facebook", "fb" -> FACEBOOK;
            case "linkedin" -> LINKEDIN;
            case "restream" -> RESTREAM;
            case "srt" -> SRT;
            case "rist" -> RIST;
            case "rtsp" -> RTSP;
            case "udp" -> UDP;
            case "hls", "hls_local" -> HLS_LOCAL;
            default -> CUSTOM;
        };
    }
}
