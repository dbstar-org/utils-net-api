package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.net.api.index.Index;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class StreamResponseHandlerResponseConsumer<T, I extends Index<T>> extends
        AbstractResponseHandlerResponseConsumer<I, List<T>> {
    private final List<T> results = new ArrayList<>();

    private final StreamCallback<T> callback;
    private final AtomicReference<StringBuilder> refStringBuilder = new AtomicReference<>();

    private StreamResponseHandlerResponseConsumer(final HttpClientResponseHandler<I> responseHandler,
                                                  final StreamCallback<T> callback) {
        super(responseHandler);
        this.callback = callback;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        super.start(response, contentType);
        this.refStringBuilder.set(new StringBuilder());
    }

    @Override
    protected List<T> buildResult() {
        return results;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) throws IOException {
        final StringBuilder builder = refStringBuilder.get().append(src);
        while (builder.length() > 0) {
            final I result = handleResponse(builder.toString());
            final int index = result.getIndex();
            builder.delete(0, index > 0 ? index : builder.length());
            final T data = result.getData();
            if (data != null) {
                results.add(data);
                callback.stream(result.getData());
            }
        }
    }

    @Override
    public void releaseResources() {
        super.releaseResources();
        this.refStringBuilder.set(null);
    }

    static <T, I extends Index<T>> StreamResponseHandlerResponseConsumer<T, I> create(
            final HttpClientResponseHandler<I> handler, final StreamCallback<T> callback) {
        return new StreamResponseHandlerResponseConsumer<>(handler, callback);
    }
}
