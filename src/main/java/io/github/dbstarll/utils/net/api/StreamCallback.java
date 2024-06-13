package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;

import java.io.IOException;

public interface StreamCallback<T> {
    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param response    HttpResponse
     * @param contentType ContentType
     * @param endOfStream 数据流是否结束
     * @param result      the data packet.
     * @throws IOException in case of a problem or the connection was aborted
     */
    void stream(HttpResponse response, ContentType contentType, boolean endOfStream, T result) throws IOException;
}
