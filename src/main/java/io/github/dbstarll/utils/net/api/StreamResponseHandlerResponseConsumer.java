package io.github.dbstarll.utils.net.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

final class StreamResponseHandlerResponseConsumer<T> extends AbstractCharResponseConsumer<List<T>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamResponseHandlerResponseConsumer.class);

    private final HttpClientResponseHandler<T> responseHandler;
    private final StreamFutureCallback<T> callback;

    private final AtomicReference<HttpResponse> refHttpResponse = new AtomicReference<>();
    private final AtomicReference<ContentType> refContentType = new AtomicReference<>();
    private final List<T> results = new ArrayList<>();

    static <T> StreamResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler,
                                                               final StreamFutureCallback<T> callback) {
        return new StreamResponseHandlerResponseConsumer<>(responseHandler, callback);
    }

    private StreamResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler,
                                                  final StreamFutureCallback<T> callback) {
        this.responseHandler = responseHandler;
        this.callback = callback;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        this.refHttpResponse.set(response);
        this.refContentType.set(contentType);
        this.results.clear();
    }

    @Override
    protected List<T> buildResult() {
        return new ArrayList<>(results);
    }

    @Override
    protected int capacityIncrement() {
        return 0;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) throws IOException {
        LOGGER.trace("data: {}, endOfStream: {}", src.remaining(), endOfStream);
        final HttpResponse response = refHttpResponse.get();
        for (final String s : src.toString().split("\n")) {
            if (StringUtils.isNotBlank(s)) {
                final ClassicHttpResponse classicHttpResponse = ClassicResponseBuilder.create(response.getCode())
                        .setVersion(response.getVersion())
                        .setHeaders(response.getHeaders())
                        .setEntity(s, refContentType.get())
                        .build();
                final T result;
                try {
                    result = responseHandler.handleResponse(classicHttpResponse);
                } catch (HttpException e) {
                    throw new IOException(e);
                }
                results.add(result);
                callback.stream(result);
            }
        }
    }

    @Override
    public void releaseResources() {
        this.refHttpResponse.set(null);
        this.refContentType.set(null);
        this.results.clear();
    }
}
