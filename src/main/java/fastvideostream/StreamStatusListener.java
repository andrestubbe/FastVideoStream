package fastvideostream;

/**
 * Listener for lifecycle and telemetry updates from the VideoStreamEngine.
 */
public interface StreamStatusListener {
    void onStarted(int width, int height, int fps);
    void onFramePushed(long frameNumber, long elapsedNanos);
    void onError(Throwable throwable);
    void onStopped();
}
