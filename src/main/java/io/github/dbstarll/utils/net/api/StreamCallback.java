package io.github.dbstarll.utils.net.api;

public interface StreamCallback<T> {
    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param result the data packet.
     */
    void stream(T result);
}
