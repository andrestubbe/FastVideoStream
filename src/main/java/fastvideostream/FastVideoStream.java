package fastvideostream;

import fastcamera.CameraDevice;
import fastcamera.FastCamera;
import fastaudio.FastAudioCapture;
import fastscreen.FastScreen;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/** CLI streamer using the existing FastScreen and FastCamera backends. */
public final class FastVideoStream {
    private FastVideoStream() {}

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options.listCameras) {
            List<CameraDevice> devices = FastCamera.enumerateDevices();
            for (int i = 0; i < devices.size(); i++) {
                System.out.printf("[%d] %s%n", i, devices.get(i));
            }
            return;
        }
        if (options.youtubeKey == null && options.twitchKey == null) {
            throw new IllegalArgumentException("Set FAST_YOUTUBE_KEY and/or FAST_TWITCH_KEY.");
        }

        FastScreen screen = options.source.equals("camera") ? null : new FastScreen(options.monitor);
        FastCamera camera = null;
        AudioInput microphone = null;
        AudioInput systemAudio = null;
        Process ffmpeg = null;
        try {
            int width = screen != null ? screen.getFrameWidth() : 1280;
            int height = screen != null ? screen.getFrameHeight() : 720;
            AtomicReference<byte[]> cameraFrame = new AtomicReference<>();
            AtomicReference<int[]> cameraSize = new AtomicReference<>(new int[]{0, 0});

            if (options.source.equals("screen-camera") || options.source.equals("camera")) {
                List<CameraDevice> devices = FastCamera.enumerateDevices();
                if (devices.isEmpty()) throw new IllegalStateException("No camera found.");
                if (options.cameraIndex < 0 || options.cameraIndex >= devices.size()) {
                    throw new IllegalArgumentException("Camera index out of range: " + options.cameraIndex
                            + " (available: 0-" + (devices.size() - 1) + ")");
                }
                camera = FastCamera.open(devices.get(options.cameraIndex).getId());
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
            command.addAll(List.of("-c:v", options.encoder));
            if ("h264_qsv".equals(options.encoder)) {
                command.addAll(List.of("-preset", "veryfast", "-b:v", options.bitrate + "k",
                        "-maxrate", options.bitrate + "k", "-bufsize", (options.bitrate * 2) + "k",
                        "-g", String.valueOf(options.fps * 2), "-pix_fmt", "nv12"));
            } else if ("h264_nvenc".equals(options.encoder)) {
                command.addAll(List.of("-preset", "p4", "-tune", "ll", "-rc", "cbr",
                        "-b:v", options.bitrate + "k", "-maxrate", options.bitrate + "k",
                        "-bufsize", (options.bitrate * 2) + "k", "-g", String.valueOf(options.fps * 2),
                        "-pix_fmt", "yuv420p"));
            } else {
                command.addAll(List.of("-preset", "veryfast", "-b:v", options.bitrate + "k",
                        "-maxrate", options.bitrate + "k", "-bufsize", (options.bitrate * 2) + "k",
                        "-g", String.valueOf(options.fps * 2), "-pix_fmt", "yuv420p"));
            }
            if (audioInputs > 0) {
                command.addAll(List.of("-c:a", "aac", "-b:a", "160k", "-ar", "48000", "-ac", "2"));
            } else {
                command.add("-an");
            }
            command.addAll(List.of("-f", "tee", buildOutputs(options)));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            ffmpeg = pb.start();
            if (microphone != null) microphone.start();
            if (systemAudio != null) systemAudio.start();
            OutputStream input = ffmpeg.getOutputStream();
            byte[] frameBytes = new byte[width * height * 4];
            AtomicBoolean running = new AtomicBoolean(true);
            Thread stopThread = new Thread(() -> {
                try {
                    System.in.read();
                } catch (Exception ignored) {}
                running.set(false);
            }, "fast-stream-stop");
            stopThread.setDaemon(true);
            stopThread.start();

            long frameInterval = 1_000_000_000L / options.fps;
            long nextFrame = System.nanoTime();

            int[] reusablePixels = (screen != null) ? new int[width * height] : null;
            if (screen != null) {
                screen.startStream(0, 0, width, height);
            }

            System.out.println(darkGray("========================================================================================="));
            System.out.println(" " + boldWhite("FastVideoStream") + darkGray(" — Low-Overhead High-FPS Live Streaming Engine"));
            System.out.printf(" %s %s @ %s via %s\n", darkGray("PIPELINE:"), boldWhite(width + "x" + height), boldWhite(options.fps + " FPS"), white(options.encoder));
            System.out.println(darkGray("========================================================================================="));
            System.out.println(darkGray(" [STATUS]") + " " + white("Streaming active.") + " " + darkGray("Press ENTER to stop."));
            while (ffmpeg.isAlive() && running.get()) {
                boolean hasFrame = false;
                if (screen != null) {
                    hasFrame = screen.getNextFrame(reusablePixels);
                    if (!hasFrame && reusablePixels != null) {
                        // Fallback to captureRaw if streaming buffer isn't populated on static screen
                        int[] raw = screen.captureRaw(0, 0, 0, 0);
                        if (raw != null) {
                            System.arraycopy(raw, 0, reusablePixels, 0, Math.min(raw.length, reusablePixels.length));
                            hasFrame = true;
                        }
                    }
                    if (hasFrame) {
                        if (options.source.equals("screen-camera")) {
                            blendCamera(reusablePixels, width, height, cameraFrame.get(), cameraSize.get(), options);
                            if (options.cursor) {
                                try {
                                    Class<?> cursorClass = Class.forName("fastscreencapture.FastCursor");
                                    cursorClass.getMethod("blendCursor", int[].class, int.class, int.class, int.class, int.class, boolean.class)
                                            .invoke(null, reusablePixels, width, height, 0, 0, false);
                                } catch (Throwable ignored) {}
                            }
                        }
                        writePixels(reusablePixels, frameBytes, input, width * height);
                    }
                } else if (options.source.equals("camera")) {
                    writeCamera(cameraFrame.get(), cameraSize.get(), frameBytes, input, width, height);
                }
                nextFrame += frameInterval;
                long sleepNanos = nextFrame - System.nanoTime();
                if (sleepNanos > 0) {
                    LockSupport.parkNanos(sleepNanos);
                } else if (sleepNanos < -frameInterval) {
                    nextFrame = System.nanoTime();
                }
            }
            input.close();
        } finally {
            if (microphone != null) microphone.close();
            if (systemAudio != null) systemAudio.close();
            if (ffmpeg != null && ffmpeg.isAlive()) ffmpeg.destroy();
            if (camera != null) camera.close();
            if (screen != null) {
                screen.stopStream();
                screen.dispose();
            }
        }
    }

    private static void writePixels(int[] pixels, byte[] frameBytes, OutputStream input, int pixelCount) throws Exception {
        for (int i = 0, j = 0; i < pixelCount && i < pixels.length; i++) {
            int pixel = pixels[i];
            frameBytes[j++] = (byte) pixel;
            frameBytes[j++] = (byte) (pixel >> 8);
            frameBytes[j++] = (byte) (pixel >> 16);
            frameBytes[j++] = (byte) (pixel >> 24);
        }
        input.write(frameBytes);
    }

    private static void writeCamera(byte[] camera, int[] size, byte[] frameBytes, OutputStream input,
                                    int width, int height) throws Exception {
        if (camera == null || size[0] <= 0 || size[1] <= 0) return;
        for (int y = 0, index = 0; y < height; y++) {
            int sourceY = y * size[1] / height;
            for (int x = 0; x < width; x++) {
                int sourceX = x * size[0] / width;
                int sourceIndex = (sourceY * size[0] + sourceX) * 4;
                if (sourceIndex + 3 >= camera.length) return;
                frameBytes[index++] = camera[sourceIndex];
                frameBytes[index++] = camera[sourceIndex + 1];
                frameBytes[index++] = camera[sourceIndex + 2];
                frameBytes[index++] = camera[sourceIndex + 3];
            }
        }
        input.write(frameBytes);
    }

    private static List<String> audioArguments(int port) {
        return List.of("-thread_queue_size", "1024", "-f", "s16le", "-ar", "48000", "-ac", "2",
                "-i", "udp://127.0.0.1:" + port + "?listen=1&fifo_size=1000000&overrun_nonfatal=1");
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
        int cw = size[0];
        int ch = size[1];
        int pipW = options.cameraWidth > 0 ? options.cameraWidth : screenWidth / 4;
        int pipH = options.cameraHeight > 0 ? options.cameraHeight : pipW * 9 / 16;
        int x0 = options.cameraWidth > 0 ? options.cameraX : screenWidth - pipW - 20;
        int y0 = options.cameraHeight > 0 ? options.cameraY : screenHeight - pipH - 20;

        try {
            int[] camInts = new int[cw * ch];
            int bIdx = 0;
            for (int i = 0; i < camInts.length; i++) {
                int b = camera[bIdx++] & 0xFF;
                int g = camera[bIdx++] & 0xFF;
                int r = camera[bIdx++] & 0xFF;
                int a = camera[bIdx++] & 0xFF;
                camInts[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            fastimage.FastImage camImg = fastimage.FastImage.fromPixels(camInts, cw, ch);
            camImg.resizeAreaAverage(pipW, pipH);
            int[] scaled = camImg.getPixels();
            camImg.dispose();

            for (int row = 0; row < pipH; row++) {
                int sy = y0 + row;
                if (sy < 0 || sy >= screenHeight) continue;
                int screenRowOffset = sy * screenWidth;
                int scaledRowOffset = row * pipW;
                for (int col = 0; col < pipW; col++) {
                    int sx = x0 + col;
                    if (sx < 0 || sx >= screenWidth) continue;
                    screen[screenRowOffset + sx] = scaled[scaledRowOffset + col];
                }
            }
        } catch (Throwable t) {
            for (int y = 0; y < pipH; y++) {
                int sourceY = y * ch / pipH;
                for (int x = 0; x < pipW; x++) {
                    int targetX = x0 + x;
                    int targetY = y0 + y;
                    int sourceX = x * cw / pipW;
                    int index = (sourceY * cw + sourceX) * 4;
                    if (targetX >= 0 && targetY >= 0 && targetX < screenWidth && targetY < screenHeight
                            && index + 3 < camera.length) {
                        int b = camera[index] & 255;
                        int g = camera[index + 1] & 255;
                        int r = camera[index + 2] & 255;
                        int a = camera[index + 3] & 255;
                        screen[targetY * screenWidth + targetX] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            }
        }
    }

    private static final class Options {
        int monitor = 0;
        int fps = 60;
        int bitrate = 6000;
        boolean camera;
        boolean listCameras;
        int cameraIndex;
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
        String source = "screen";

        static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                if (arg.equals("--camera")) {
                    options.camera = true;
                    options.source = "screen-camera";
                }
                else if (arg.startsWith("--source=")) {
                    options.source = arg.substring(9);
                    if (!List.of("screen", "screen-camera", "camera").contains(options.source)) {
                        throw new IllegalArgumentException("--source requires screen, screen-camera, or camera");
                    }
                }
                else if (arg.equals("--list-cameras")) options.listCameras = true;
                else if (arg.startsWith("--camera=") && !arg.substring(9).contains(",")) {
                    options.camera = true;
                    options.source = "screen-camera";
                    options.cameraIndex = Integer.parseInt(arg.substring(9));
                }
                else if (arg.startsWith("--camera=")) {
                    String[] values = arg.substring(9).split(",");
                    if (values.length != 4) throw new IllegalArgumentException("--camera requires N or x,y,w,h");
                    options.camera = true;
                    options.source = "screen-camera";
                    options.cameraX = Integer.parseInt(values[0]);
                    options.cameraY = Integer.parseInt(values[1]);
                    options.cameraWidth = Integer.parseInt(values[2]);
                    options.cameraHeight = Integer.parseInt(values[3]);
                }
                else if (arg.startsWith("--camera-index=")) {
                    options.camera = true;
                    options.cameraIndex = Integer.parseInt(arg.substring(15));
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

        private final byte[] pcmBuf = new byte[4096 * 4];
        private final DatagramPacket packet = new DatagramPacket(pcmBuf, 0, localhost, 0);

        private void start() {
            packet.setPort(port);
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
            int byteLen = samples.length * 2;
            byte[] targetBuf = (byteLen <= pcmBuf.length) ? pcmBuf : new byte[byteLen];
            for (int i = 0, j = 0; i < samples.length; i++) {
                short sample = samples[i];
                targetBuf[j++] = (byte) sample;
                targetBuf[j++] = (byte) (sample >> 8);
            }
            try {
                if (targetBuf == pcmBuf) {
                    packet.setData(pcmBuf, 0, byteLen);
                    socket.send(packet);
                } else {
                    socket.send(new DatagramPacket(targetBuf, byteLen, localhost, port));
                }
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

    private static String darkGray(String text) {
        return fastansi.FastANSI.fg(240) + text + fastansi.FastANSI.RESET;
    }

    private static String white(String text) {
        return fastansi.FastANSI.FG_BRIGHT_WHITE + text + fastansi.FastANSI.RESET;
    }

    private static String boldWhite(String text) {
        return fastansi.FastANSI.BOLD + fastansi.FastANSI.FG_BRIGHT_WHITE + text + fastansi.FastANSI.RESET;
    }
}