package fastvideostream;

/**
 * Consumer callback to intercept or modify screen frames before encoding.
 * Allows applications (such as Stream+) to composite webcam PiP, watermarks, or overlays.
 */
@FunctionalInterface
public interface FrameInterceptor {
    /**
     * Called on the capture thread with raw screen BGRA int pixels.
     *
     * @param pixels Screen pixel buffer (ARGB / BGRA packed integers)
     * @param width Screen width
     * @param height Screen height
     */
    void onFrame(int[] pixels, int width, int height);
}
