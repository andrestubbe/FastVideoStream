package fastvideostream;

import fastscreen.FastScreen;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Standard implementation of {@link VideoStreamEngine} utilizing Direct3D11 / DXGI Desktop Duplication
 * coupled directly with a bundled or system FFmpeg process over pipe:0.
 */
public class D3D11VideoStreamEngine implements VideoStreamEngine {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;
    private Process ffmpegProcess;
    private FastScreen screen;

    @Override
    public synchronized void start(StreamSessionConfig config, StreamStatusListener listener) throws Exception {
        start(config, null, listener);
    }

    @Override
    public synchronized void start(StreamSessionConfig config, FrameInterceptor interceptor, StreamStatusListener listener) throws Exception {
        if (running.get()) {
            throw new IllegalStateException("VideoStreamEngine is already running.");
        }

        Path ffmpegBin = Path.of(config.ffmpegPath());
        if (!Files.exists(ffmpegBin)) {
            // Check in system PATH if not found at specified path
            ffmpegBin = Path.of("ffmpeg");
        }

        screen = new FastScreen(config.monitorIndex());
        int width = screen.getFrameWidth();
        int height = screen.getFrameHeight();

        List<String> command = new ArrayList<>();
        command.add(ffmpegBin.toString());
        command.addAll(List.of(
                "-hide_banner", "-loglevel", "warning", "-y",
                "-f", "rawvideo",
                "-pix_fmt", "bgra",
                "-video_size", width + "x" + height,
                "-framerate", String.valueOf(config.fps()),
                "-i", "pipe:0",
                "-f", "lavfi",
                "-i", "anullsrc=channel_layout=stereo:sample_rate=48000",
                "-map", "0:v", "-map", "1:a",
                "-c:v", config.encoder()
        ));

        if ("h264_qsv".equalsIgnoreCase(config.encoder())) {
            command.addAll(List.of(
                    "-preset", "veryfast",
                    "-b:v", config.bitrateKbps() + "k",
                    "-maxrate", config.bitrateKbps() + "k",
                    "-bufsize", (config.bitrateKbps() * 2) + "k",
                    "-g", String.valueOf(config.fps() * 2),
                    "-pix_fmt", "nv12"
            ));
        } else {
            command.addAll(List.of(
                    "-preset", "ultrafast",
                    "-tune", "zerolatency",
                    "-b:v", config.bitrateKbps() + "k",
                    "-maxrate", config.bitrateKbps() + "k",
                    "-bufsize", (config.bitrateKbps() * 2) + "k",
                    "-g", String.valueOf(config.fps() * 2),
                    "-pix_fmt", "yuv420p"
            ));
        }

        command.addAll(List.of(
                "-c:a", "aac",
                "-b:a", "128k",
                "-ar", "48000",
                "-ac", "2"
        ));

        // Format mapping based on target protocol & provider type
        String targetUrl = config.rtmpsTargetUrl();
        if (targetUrl.startsWith("srt://") || targetUrl.startsWith("udp://") || targetUrl.startsWith("rist://")) {
            command.addAll(List.of("-f", "mpegts", targetUrl));
        } else if (targetUrl.startsWith("rtsp://")) {
            command.addAll(List.of("-f", "rtsp", "-rtsp_transport", "tcp", targetUrl));
        } else if (targetUrl.endsWith(".m3u8")) {
            command.addAll(List.of("-f", "hls", "-hls_time", "2", "-hls_list_size", "5", targetUrl));
        } else {
            command.addAll(List.of("-f", "flv", targetUrl));
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        ffmpegProcess = pb.start();

        running.set(true);

        workerThread = new Thread(() -> {
            try {
                if (listener != null) listener.onStarted(width, height, config.fps());

                OutputStream input = new BufferedOutputStream(ffmpegProcess.getOutputStream(), 4 * 1024 * 1024);
                byte[] frameBytes = new byte[width * height * 4];
                int[] pixels = new int[width * height];
                screen.startStream(0, 0, width, height);

                long frameInterval = 1_000_000_000L / config.fps();
                long nextFrame = System.nanoTime();
                long frameNumber = 0;

                while (running.get() && ffmpegProcess.isAlive()) {
                    screen.getNextFrame(pixels);

                    if (interceptor != null) {
                        try {
                            interceptor.onFrame(pixels, width, height);
                        } catch (Throwable ignored) {}
                    }

                    if (config.includeCursor()) {
                        fastcursor.FastCursor.blendCursor(pixels, width, height, 0, 0, false);
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

                    frameNumber++;
                    if (listener != null) listener.onFramePushed(frameNumber, System.nanoTime() - nextFrame);

                    nextFrame += frameInterval;
                    long sleepNanos = nextFrame - System.nanoTime();
                    if (sleepNanos > 1_000_000L) {
                        Thread.sleep(sleepNanos / 1_000_000L);
                    }
                }

                input.flush();
                input.close();
            } catch (Throwable t) {
                if (listener != null && running.get()) listener.onError(t);
            } finally {
                cleanup();
                if (listener != null) listener.onStopped();
            }
        }, "FastVideoStream-Worker");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    @Override
    public boolean isStreaming() {
        return running.get() && ffmpegProcess != null && ffmpegProcess.isAlive();
    }

    @Override
    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        cleanup();
    }

    private synchronized void cleanup() {
        if (ffmpegProcess != null) {
            try {
                if (ffmpegProcess.isAlive()) {
                    ffmpegProcess.destroy();
                }
            } catch (Throwable ignored) {}
            ffmpegProcess = null;
        }
        if (screen != null) {
            try { screen.stopStream(); } catch (Throwable ignored) {}
            try { screen.dispose(); } catch (Throwable ignored) {}
            screen = null;
        }
    }
}
