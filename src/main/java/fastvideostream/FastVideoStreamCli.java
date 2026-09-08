package fastvideostream;

import fastcamera.CameraDevice;
import fastcamera.FastCamera;
import fastaudio.FastAudioCapture;
import fastscreen.FastScreen;
import fastscreencapture.FastCursor;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** CLI streamer using the existing FastScreen and FastCamera backends. */
public final class FastVideoStreamCli {
    private FastVideoStreamCli() {}

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options.youtubeKey == null && options.twitchKey == null) {
            throw new IllegalArgumentException("Set FAST_YOUTUBE_KEY and/or FAST_TWITCH_KEY.");
        }

        FastScreen screen = new FastScreen(options.monitor);
        FastCamera camera = null;
        AudioInput microphone = null;
        AudioInput systemAudio = null;
        Process ffmpeg = null;
        try {
            int width = screen.getFrameWidth();
            int height = screen.getFrameHeight();
            AtomicReference<byte[]> cameraFrame = new AtomicReference<>();
            AtomicReference<int[]> cameraSize = new AtomicReference<>(new int[]{0, 0});

            if (options.camera) {
                List<CameraDevice> devices = FastCamera.enumerateDevices();
                if (devices.isEmpty()) throw new IllegalStateException("No camera found.");
                camera = FastCamera.open(devices.get(0).getId());
                camera.setListener((frame, frameWidth, frameHeight, timestamp) -> {
                    cameraFrame.set(frame);
                    cameraSize.set(new int[]{frameWidth, frameHeight});
                });
                if (!camera.startCapture(1280, 720, Math.min(options.fps, 60), FastCamera.FORMAT_BGRA)) {
                    throw new IllegalStateException("Camera capture could not be started.");
                }
            }

            List<String> command = new ArrayList<>();
            command.add(options.ffmpeg);
            command.addAll(List.of("-hide_banner", "-loglevel", "warning", "-y", "-f", "rawvideo",
                    "-pix_fmt", "bgra", "-video_size", width + "x" + height,
                    "-framerate", String.valueOf(options.fps), "-i", "pipe:0"));
            if (options.microphone) {
                microphone = new AudioInput(false);
                command.addAll(audioArguments(microphone.port));
            }
            if (options.systemAudio) {
                systemAudio = new AudioInput(true);
                command.addAll(audioArguments(systemAudio.port));
            }
            int audioInputs = (options.microphone ? 1 : 0) + (options.systemAudio ? 1 : 0);
            if (audioInputs == 2) {
                command.addAll(List.of("-filter_complex", "[1:a][2:a]amix=inputs=2:duration=longest:dropout_transition=2[audio_mix]"));
            }
            command.addAll(List.of("-map", "0:v"));
            if (audioInputs == 2) {
                command.addAll(List.of("-map", "[audio_mix]"));
            } else if (audioInputs == 1) {
                command.addAll(List.of("-map", "1:a"));
            }
            command.addAll(List.of("-c:v", options.encoder, "-preset", "p4", "-tune", "ll", "-rc", "cbr",
                    "-b:v", options.bitrate + "k", "-maxrate", options.bitrate + "k",
                    "-bufsize", (options.bitrate * 2) + "k", "-g", String.valueOf(options.fps * 2),
                    "-pix_fmt", "yuv420p"));
            if (audioInputs > 0) {
                command.addAll(List.of("-c:a", "aac", "-b:a", "160k", "-ar", "48000", "-ac", "2"));
            } else {
                command.add("-an");
            }
            command.addAll(List.of("-f", "tee", buildOutputs(options)));

            ffmpeg = new ProcessBuilder(command).inheritIO().start();
            if (microphone != null) microphone.start();
            if (systemAudio != null) systemAudio.start();
            OutputStream input = ffmpeg.getOutputStream();
            byte[] frameBytes = new byte[width * height * 4];
            long frameInterval = 1_000_000_000L / options.fps;
            long nextFrame = System.nanoTime();

            System.out.printf("Streaming %dx%d @ %d FPS via %s%n", width, height, options.fps, options.encoder);
            System.out.println("Press ENTER to stop.");
            while (ffmpeg.isAlive() && System.in.available() == 0) {
                int[] pixels = screen.captureRaw(0, 0, 0, 0);
                if (pixels != null) {
                    if (options.camera) blendCamera(pixels, width, height, cameraFrame.get(), cameraSize.get(), options);
                    if (options.cursor) FastCursor.blendCursor(pixels, width, height, 0, 0, false);
                    for (int i = 0, j = 0; i < width * height && i < pixels.length; i++) {
                        int pixel = pixels[i];
                        frameBytes[j++] = (byte) pixel;
                        frameBytes[j++] = (byte) (pixel >> 8);
                        frameBytes[j++] = (byte) (pixel >> 16);
                        frameBytes[j++] = (byte) (pixel >> 24);
                    }
                    input.write(frameBytes);
                }
                nextFrame += frameInterval;
                long sleepNanos = nextFrame - System.nanoTime();
                if (sleepNanos > 1_000_000L) Thread.sleep(sleepNanos / 1_000_000L);
            }
            input.close();
        } finally {
            if (microphone != null) microphone.close();
            if (systemAudio != null) systemAudio.close();
            if (ffmpeg != null && ffmpeg.isAlive()) ffmpeg.destroy();
            if (camera != null) camera.close();
            screen.dispose();
        }
    }

    private static List<String> audioArguments(int port) {
        return List.of("-f", "s16le", "-ar", "48000", "-ac", "2",
                "-i", "udp://127.0.0.1:" + port + "?listen=1");
    }

    private static String buildOutputs(Options options) {
        List<String> outputs = new ArrayList<>();
        if (options.youtubeKey != null) {
            outputs.add("[f=flv:onfail=ignore]rtmps://a.rtmps.youtube.com/live2/" + options.youtubeKey);
        }
        if (options.twitchKey != null) {
            outputs.add("[f=flv:onfail=ignore]rtmps://live.twitch.tv/app/" + options.twitchKey);
        }
        return String.join("|", outputs);
    }

    private static void blendCamera(int[] screen, int screenWidth, int screenHeight,
                                    byte[] camera, int[] size, Options options) {
        if (camera == null || size[0] <= 0 || size[1] <= 0) return;
        int width = options.cameraWidth > 0 ? options.cameraWidth : screenWidth / 4;
        int height = options.cameraHeight > 0 ? options.cameraHeight : width * 9 / 16;
        int x0 = options.cameraWidth > 0 ? options.cameraX : screenWidth - width - 20;
        int y0 = options.cameraHeight > 0 ? options.cameraY : screenHeight - height - 20;
        for (int y = 0; y < height; y++) {
            int sourceY = y * size[1] / height;
            for (int x = 0; x < width; x++) {
                int targetX = x0 + x;
                int targetY = y0 + y;
                int sourceX = x * size[0] / width;
                int index = (sourceY * size[0] + sourceX) * 4;
                if (targetX >= 0 && targetY >= 0 && targetX < screenWidth && targetY < screenHeight
                        && index + 3 < camera.length) {
                    screen[targetY * screenWidth + targetX] = (camera[index + 3] & 255) << 24
                            | (camera[index] & 255) << 16 | (camera[index + 1] & 255) << 8
                            | (camera[index + 2] & 255);
                }
            }
        }
    }

    private static final class Options {
        int monitor = 0;
        int fps = 60;
        int bitrate = 6000;
        boolean camera;
        int cameraX;
        int cameraY;
        int cameraWidth;
        int cameraHeight;
        boolean cursor = true;
        boolean microphone;
        boolean systemAudio;
        String encoder = "h264_nvenc";
        String ffmpeg = "ffmpeg";
        String youtubeKey = environment("FAST_YOUTUBE_KEY");
        String twitchKey = environment("FAST_TWITCH_KEY");

        static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                if (arg.equals("--camera")) options.camera = true;
                else if (arg.startsWith("--camera=")) {
                    String[] values = arg.substring(9).split(",");
                    if (values.length != 4) throw new IllegalArgumentException("--camera requires x,y,w,h");
                    options.camera = true;
                    options.cameraX = Integer.parseInt(values[0]);
                    options.cameraY = Integer.parseInt(values[1]);
                    options.cameraWidth = Integer.parseInt(values[2]);
                    options.cameraHeight = Integer.parseInt(values[3]);
                }
                else if (arg.equals("--no-cursor")) options.cursor = false;
                else if (arg.equals("--microphone")) options.microphone = true;
                else if (arg.equals("--system-audio")) options.systemAudio = true;
                else if (arg.equals("--audio")) {
                    options.microphone = true;
                    options.systemAudio = true;
                }
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

    private static final class AudioInput implements AutoCloseable {
        private static final int SAMPLE_RATE = 48000;
        private static final int CHANNELS = 2;
        private final boolean loopback;
        private final DatagramSocket socket;
        private final InetAddress localhost = InetAddress.getLoopbackAddress();
        private final int port;
        private FastAudioCapture capture;

        private AudioInput(boolean loopback) throws Exception {
            this.loopback = loopback;
            this.port = findFreePort();
            this.socket = new DatagramSocket();
        }

        private static int findFreePort() throws Exception {
            try (DatagramSocket probe = new DatagramSocket(0)) {
                return probe.getLocalPort();
            }
        }

        private void start() {
            capture = new FastAudioCapture();
            capture.setAudioCallback(this::send);
            boolean started = loopback
                    ? capture.startSystemRecording(SAMPLE_RATE, CHANNELS, 16)
                    : capture.startRecording(SAMPLE_RATE, CHANNELS, 16);
            if (!started) {
                close();
                throw new IllegalStateException(loopback
                        ? "System audio capture could not be started."
                        : "Microphone capture could not be started.");
            }
        }

        private void send(short[] samples, long timestamp) {
            byte[] pcm = new byte[samples.length * 2];
            for (int i = 0, j = 0; i < samples.length; i++) {
                pcm[j++] = (byte) samples[i];
                pcm[j++] = (byte) (samples[i] >> 8);
            }
            try {
                socket.send(new DatagramPacket(pcm, pcm.length, localhost, port));
            } catch (Exception ignored) {
            }
        }

        @Override
        public void close() {
            if (capture != null) {
                try {
                    capture.stopRecording();
                } catch (Exception ignored) {
                }
                capture.close();
                capture = null;
            }
            socket.close();
        }
    }
}