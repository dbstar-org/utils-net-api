package io.github.dbstarll.utils.net.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

final class StreamResponseHandlerResponseConsumer<T> extends AbstractResponseHandlerResponseConsumer<T, List<T>> {
    private final List<T> results = new ArrayList<>();

    private final StreamFutureCallback<T> callback;

    private StreamResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler,
                                                  final StreamFutureCallback<T> callback) {
        super(responseHandler);
        this.callback = callback;
    }

    @Override
    protected List<T> buildResult() {
        return results;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) throws IOException {
        for (final String s : src.toString().split("\n")) {
            if (StringUtils.isNotBlank(s)) {
                final T result = handleResponse(s);
                results.add(result);
                callback.stream(result);
            }
        }
    }

    static <T> StreamResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler,
                                                               final StreamFutureCallback<T> callback) {
        return new StreamResponseHandlerResponseConsumer<>(responseHandler, callback);
    }
}
