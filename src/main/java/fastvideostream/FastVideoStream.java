package fastvideostream;

import fastscreen.FastScreen;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lean FastVideoStream headless CLI streamer with Intel Iris / QSV / libx264 support.
 * Pure video engine: Compositing, camera blending, and audio mixing belong to application layers.
 */
public final class FastVideoStream {
    private FastVideoStream() {}

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options.youtubeKey == null && options.twitchKey == null) {
            throw new IllegalArgumentException("Set FAST_YOUTUBE_KEY and/or FAST_TWITCH_KEY.");
        }

        FastScreen screen = new FastScreen(options.monitor);
        Process ffmpeg = null;
        try {
            int width = screen.getFrameWidth();
            int height = screen.getFrameHeight();

            List<String> command = new ArrayList<>();
            command.add(options.ffmpeg);
            command.addAll(List.of("-hide_banner", "-loglevel", "info", "-y", "-f", "rawvideo",
                    "-pix_fmt", "bgra", "-video_size", width + "x" + height,
                    "-framerate", String.valueOf(options.fps), "-i", "pipe:0"));

            // Add silent audio stream so YouTube Ingest accepts the feed immediately
            command.addAll(List.of("-f", "lavfi", "-i", "anullsrc=channel_layout=stereo:sample_rate=48000"));

            command.addAll(List.of("-map", "0:v", "-map", "1:a"));

            command.addAll(List.of("-c:v", options.encoder));
            if ("h264_qsv".equals(options.encoder)) {
                command.addAll(List.of("-preset", "veryfast",
                        "-b:v", options.bitrate + "k", "-maxrate", options.bitrate + "k",
                        "-bufsize", (options.bitrate * 2) + "k", "-g", String.valueOf(options.fps * 2),
                        "-pix_fmt", "nv12"));
            } else {
                command.addAll(List.of("-preset", "ultrafast", "-tune", "zerolatency",
                        "-b:v", options.bitrate + "k", "-maxrate", options.bitrate + "k",
                        "-bufsize", (options.bitrate * 2) + "k", "-g", String.valueOf(options.fps * 2),
                        "-pix_fmt", "yuv420p"));
            }

            command.addAll(List.of("-c:a", "aac", "-b:a", "128k", "-ar", "48000", "-ac", "2"));

            // Direct RTMPS to YouTube (port 443 required by YouTube Studio)
            List<String> targets = new ArrayList<>();
            if (options.youtubeKey != null) {
                targets.add("rtmps://a.rtmps.youtube.com/live2/" + options.youtubeKey.trim());
            }
            if (options.twitchKey != null) {
                targets.add("rtmps://live.twitch.tv/app/" + options.twitchKey.trim());
            }

            if (targets.size() == 1) {
                command.addAll(List.of("-f", "flv", targets.get(0)));
            } else {
                List<String> teeTargets = new ArrayList<>();
                for (String t : targets) teeTargets.add("[f=flv:onfail=ignore]" + t);
                command.addAll(List.of("-f", "tee", String.join("|", teeTargets)));
            }

            System.out.printf("Starting streaming %dx%d @ %d FPS via %s...%n", width, height, options.fps, options.encoder);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            ffmpeg = pb.start();
            OutputStream input = new BufferedOutputStream(ffmpeg.getOutputStream(), 4 * 1024 * 1024);
            byte[] frameBytes = new byte[width * height * 4];
            long frameInterval = 1_000_000_000L / options.fps;
            long nextFrame = System.nanoTime();

            int[] pixels = new int[width * height];
            screen.startStream(0, 0, width, height);
            int[] initRaw = screen.captureRaw(0, 0, 0, 0);
            if (initRaw != null) System.arraycopy(initRaw, 0, pixels, 0, Math.min(initRaw.length, pixels.length));

            System.out.println("Streaming active. Press ENTER to stop.");
            while (ffmpeg.isAlive() && System.in.available() == 0) {
                screen.getNextFrame(pixels);
                if (options.cursor) {
                    try {
                        Class<?> cursorClass = Class.forName("fastscreencapture.FastCursor");
                        cursorClass.getMethod("blendCursor", int[].class, int.class, int.class, int.class, int.class, boolean.class)
                                .invoke(null, pixels, width, height, 0, 0, false);
                    } catch (Throwable ignored) {}
                }
                int bIdx = 0;
                for (int i = 0; i < width * height && i < pixels.length; i++) {
                    int p = pixels[i];
                    frameBytes[bIdx++] = (byte) p;
                    frameBytes[bIdx++] = (byte) (p >> 8);
                    frameBytes[bIdx++] = (byte) (p >> 16);
                    frameBytes[bIdx++] = (byte) (p >> 24);
                }
                input.write(frameBytes);

                nextFrame += frameInterval;
                long sleepNanos = nextFrame - System.nanoTime();
                if (sleepNanos > 1_000_000L) {
                    Thread.sleep(sleepNanos / 1_000_000L);
                }
            }
            input.flush();
            input.close();
        } finally {
            if (ffmpeg != null && ffmpeg.isAlive()) ffmpeg.destroy();
            try { screen.stopStream(); } catch (Throwable ignored) {}
            screen.dispose();
        }
    }

    private static final class Options {
        int monitor = 0;
        int fps = 30;
        int bitrate = 4000;
        boolean cursor = true;
        String encoder = "h264_qsv";
        String ffmpeg = "ffmpeg";
        String youtubeKey = environment("FAST_YOUTUBE_KEY");
        String twitchKey = environment("FAST_TWITCH_KEY");

        static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                if (arg.equals("--no-cursor")) options.cursor = false;
                else if (arg.startsWith("--fps=")) options.fps = Integer.parseInt(arg.substring(6));
                else if (arg.startsWith("--bitrate=")) options.bitrate = Integer.parseInt(arg.substring(10));
                else if (arg.startsWith("--monitor=")) options.monitor = Integer.parseInt(arg.substring(10));
                else if (arg.startsWith("--encoder=")) options.encoder = arg.substring(10);
                else if (arg.startsWith("--ffmpeg=")) options.ffmpeg = arg.substring(9);
                else if (arg.startsWith("--youtube-key=")) options.youtubeKey = arg.substring(14);
                else if (arg.startsWith("--twitch-key=")) options.twitchKey = arg.substring(13);
            }
            return options;
        }

        private static String environment(String name) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
