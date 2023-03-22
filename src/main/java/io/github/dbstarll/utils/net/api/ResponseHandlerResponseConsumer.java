package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicReference;

final class ResponseHandlerResponseConsumer<T> extends AbstractCharResponseConsumer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHandlerResponseConsumer.class);

    private final HttpClientResponseHandler<T> responseHandler;

    private final AtomicReference<StringBuilder> refStringBuilder = new AtomicReference<>();
    private final AtomicReference<ClassicResponseBuilder> refClassicResponseBuilder = new AtomicReference<>();
    private final AtomicReference<ContentType> refContentType = new AtomicReference<>();

    static <T> ResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler) {
        return new ResponseHandlerResponseConsumer<>(responseHandler);
    }

    private ResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        this.refStringBuilder.set(new StringBuilder());
        this.refClassicResponseBuilder.set(ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders()));
        this.refContentType.set(contentType);
    }

    @Override
    protected T buildResult() throws IOException {
        try {
            return responseHandler.handleResponse(refClassicResponseBuilder.get()
                    .setEntity(refStringBuilder.get().toString(), refContentType.get())
                    .build());
        } catch (HttpException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected int capacityIncrement() {
        return 0;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) {
        LOGGER.trace("data: {}, endOfStream: {}", src.remaining(), endOfStream);
        refStringBuilder.get().append(src);
    }

    @Override
    public void releaseResources() {
        this.refStringBuilder.set(null);
        this.refClassicResponseBuilder.set(null);
        this.refContentType.set(null);
    }
}
