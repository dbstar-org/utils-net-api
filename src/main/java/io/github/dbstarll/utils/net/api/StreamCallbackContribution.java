package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.net.api.index.Index;
import org.apache.hc.core5.concurrent.CallbackContribution;

import java.util.List;

abstract class StreamCallbackContribution<T, I extends Index<T>> extends CallbackContribution<List<T>>
        implements StreamFutureCallback<T, I> {
    private final StreamFutureCallback<T, I> callback;

    StreamCallbackContribution(final StreamFutureCallback<T, I> callback) {
        super(callback);
        this.callback = callback;
    }

    @Override
    public void stream(final I result) {
        callback.stream(result);
    }

    @Override
    public void completed(final List<T> result) {
        callback.completed(result);
    }
}
