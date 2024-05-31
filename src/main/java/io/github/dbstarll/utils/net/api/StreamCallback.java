package io.github.dbstarll.utils.net.api;

import java.io.IOException;

public interface StreamCallback<T> {
    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param result the data packet.
     * @throws IOException in case of a problem or the connection was aborted
     */
    void stream(T result) throws IOException;
}
