package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public interface StreamCallback<T> {
    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param contentType ContentType
     * @param result      the data packet.
     * @throws IOException in case of a problem or the connection was aborted
     */
    void stream(ContentType contentType, T result) throws IOException;
}
