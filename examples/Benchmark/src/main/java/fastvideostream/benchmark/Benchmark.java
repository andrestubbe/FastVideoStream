package fastvideostream.benchmark;

import fastscreen.FastScreen;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Standard OpenJDK JMH Microbenchmark Suite for FastVideoStream capture substrate.
 * Measures throughput and latency of raw frame acquisition and pixel buffer staging.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Benchmark {

    private FastScreen screen;
    private int width;
    private int height;
    private int[] pixels;
    private byte[] frameBytes;

    @Setup
    public void setup() {
        screen = new FastScreen(0);
        width = screen.getFrameWidth();
        height = screen.getFrameHeight();
        if (width <= 0 || height <= 0) {
            width = 1920;
            height = 1080;
        }
        pixels = new int[width * height];
        frameBytes = new byte[width * height * 4];
        screen.startStream(0, 0, width, height);
    }

    @TearDown
    public void tearDown() {
        if (screen != null) {
            try { screen.stopStream(); } catch (Throwable ignored) {}
            try { screen.dispose(); } catch (Throwable ignored) {}
        }
    }

    @org.openjdk.jmh.annotations.Benchmark
    public byte[] benchmarkFrameCaptureAndConversion() {
        screen.getNextFrame(pixels);
        int bIdx = 0;
        for (int i = 0; i < width * height && i < pixels.length; i++) {
            int p = pixels[i];
            frameBytes[bIdx++] = (byte) p;
            frameBytes[bIdx++] = (byte) (p >> 8);
            frameBytes[bIdx++] = (byte) (p >> 16);
            frameBytes[bIdx++] = (byte) (p >> 24);
        }
        return frameBytes;
    }
}
