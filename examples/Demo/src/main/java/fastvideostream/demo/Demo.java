package fastvideostream.demo;

import fastvideostream.D3D11VideoStreamEngine;
import fastvideostream.StreamSessionConfig;
import fastvideostream.StreamStatusListener;
import fastvideostream.VideoStreamEngine;

/**
 * Standard Interactive Showcase Demo for FastVideoStream.
 */
public class Demo {

    public static void main(String[] args) throws Exception {
        System.out.println("=================================================");
        System.out.println("  FastVideoStream — Hardware Accelerated Stream ");
        System.out.println("  DirectX 11 DXGI Duplication & Hardware Encode  ");
        System.out.println("=================================================");

        String youtubeKey = System.getenv("FAST_YOUTUBE_KEY");
        String twitchKey = System.getenv("FAST_TWITCH_KEY");
        String targetUrl;

        if (youtubeKey != null && !youtubeKey.isBlank()) {
            targetUrl = "rtmps://a.rtmps.youtube.com/live2/" + youtubeKey.trim();
            System.out.println("[Demo] Streaming target: YouTube Live (rtmps://a.rtmps.youtube.com/live2/)");
        } else if (twitchKey != null && !twitchKey.isBlank()) {
            targetUrl = "rtmp://live.twitch.tv/app/" + twitchKey.trim();
            System.out.println("[Demo] Streaming target: Twitch (rtmp://live.twitch.tv/app/)");
        } else {
            System.out.println("[Demo] FAST_YOUTUBE_KEY or FAST_TWITCH_KEY not set.");
            System.out.println("[Demo] Using local loopback RTMP test sink for demo verification...");
            targetUrl = "rtmp://127.0.0.1:1935/live/test";
        }

        try (VideoStreamEngine engine = new D3D11VideoStreamEngine()) {
            StreamSessionConfig config = StreamSessionConfig.builder()
                    .monitorIndex(0)
                    .fps(30)
                    .bitrateKbps(4000)
                    .includeCursor(true)
                    .encoder("h264_qsv") // Intel QuickSync (default), fallbacks: h264_nvenc, libx264
                    .rtmpsTargetUrl(targetUrl)
                    .build();

            engine.start(config, new StreamStatusListener() {
                @Override
                public void onStarted(int width, int height, int fps) {
                    System.out.printf("[Engine] Desktop stream started: %d x %d @ %d FPS%n", width, height, fps);
                }

                @Override
                public void onFramePushed(long frameNumber, long elapsedNanos) {
                    if (frameNumber % 150 == 0) {
                        System.out.printf("[Telemetry] Pushed frame #%d (elapsed: %.2f s)%n",
                                frameNumber, elapsedNanos / 1_000_000_000.0);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    System.err.println("[Engine Error] " + throwable.getMessage());
                }

                @Override
                public void onStopped() {
                    System.out.println("[Engine] Stream stopped successfully.");
                }
            });

            System.out.println("[Demo] Stream running. Press ENTER to stop or waiting 10 seconds...");
            long start = System.currentTimeMillis();
            while (engine.isStreaming() && (System.currentTimeMillis() - start < 10_000)) {
                Thread.sleep(500);
            }
            engine.stop();
        }
        System.out.println("[Demo] Finished.");
    }
}
