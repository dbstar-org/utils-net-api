package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.net.api.index.Index;
import org.apache.hc.core5.concurrent.FutureCallback;

import java.util.List;

public interface StreamFutureCallback<T, I extends Index<T>> extends FutureCallback<List<T>> {
    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param result the data packet.
     */
    void stream(I result);
}
