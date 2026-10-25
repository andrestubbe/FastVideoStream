package fastvideostream;

import java.util.List;

/**
 * Registry of official streaming destination presets and endpoints for FastVideoStream.
 */
public final class StreamProviderEndpoints {

    private StreamProviderEndpoints() {}

    // Cloud Platforms
    public static final String YOUTUBE_INGEST_PRIMARY = "rtmps://a.rtmps.youtube.com/live2/";
    public static final String YOUTUBE_INGEST_BACKUP = "rtmps://b.rtmps.youtube.com/live2?backup=1";

    public static final String TWITCH_INGEST_EU_CENTRAL = "rtmps://live.twitch.tv/app/";
    public static final String TWITCH_INGEST_FALLBACK = "rtmp://live.twitch.tv/app/";

    public static final String KICK_INGEST_PRIMARY = "rtmps://fa723fc1b171.global-contribute.live-video.net:443/app/";

    public static final String TIKTOK_INGEST_PRIMARY = "rtmps://open-api.tiktok.com/live/";

    public static final String X_TWITTER_INGEST_PRIMARY = "rtmps://prod-fastly-eu-central-1.video.pscp.tv:443/x/";

    public static final String FACEBOOK_INGEST_PRIMARY = "rtmps://live-api-s.facebook.com:443/rtmp/";

    public static final String LINKEDIN_INGEST_PRIMARY = "rtmps://127.0.0.1:443/live/";

    public static final String RESTREAM_INGEST_PRIMARY = "rtmp://live.restream.io/live/";

    // Broadcast & Local Low Latency
    public static final String DEFAULT_SRT_URL = "srt://127.0.0.1:9000?mode=caller&latency=20000";
    public static final String DEFAULT_RIST_URL = "rist://127.0.0.1:1968";
    public static final String DEFAULT_RTSP_URL = "rtsp://127.0.0.1:8554/live";
    public static final String DEFAULT_UDP_URL = "udp://127.0.0.1:5000?pkt_size=1316";

    /**
     * Returns all officially supported streaming provider presets.
     */
    public static List<StreamTargetType> getAvailableProviders() {
        return List.of(
                StreamTargetType.YOUTUBE,
                StreamTargetType.TWITCH,
                StreamTargetType.KICK,
                StreamTargetType.TIKTOK,
                StreamTargetType.X_TWITTER,
                StreamTargetType.FACEBOOK,
                StreamTargetType.LINKEDIN,
                StreamTargetType.RESTREAM,
                StreamTargetType.SRT,
                StreamTargetType.RIST,
                StreamTargetType.RTSP,
                StreamTargetType.UDP,
                StreamTargetType.HLS_LOCAL,
                StreamTargetType.CUSTOM
        );
    }
}
