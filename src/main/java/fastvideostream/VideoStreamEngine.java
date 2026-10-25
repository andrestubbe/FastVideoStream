package fastvideostream;

/**
 * Pure, reusable Video Streaming Engine interface for FastJava.
 * Decoupled from application-specific cameras, audio mixing, and UI logic.
 */
public interface VideoStreamEngine extends AutoCloseable {

    /**
     * Starts streaming the configured desktop region/screen to the RTMPS target.
     */
    void start(StreamSessionConfig config, StreamStatusListener listener) throws Exception;

    /**
     * Checks if the engine is actively streaming.
     */
    boolean isStreaming();

    /**
     * Gracefully stops the stream and releases hardware/pipe resources.
     */
    void stop();

    @Override
    default void close() {
        stop();
    }
}
