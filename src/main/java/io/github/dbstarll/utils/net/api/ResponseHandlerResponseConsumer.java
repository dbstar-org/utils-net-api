package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicReference;

final class ResponseHandlerResponseConsumer<T> extends AbstractCharResponseConsumer<T> {
    private final HttpClientResponseHandler<T> responseHandler;

    private final AtomicReference<StringBuilder> stringBuilder = new AtomicReference<>();
    private final AtomicReference<ClassicResponseBuilder> classicResponseBuilder = new AtomicReference<>();
    private final AtomicReference<ContentType> contentType = new AtomicReference<>();

    static <T> ResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler) {
        return new ResponseHandlerResponseConsumer<>(responseHandler);
    }

    private ResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType type) {
        this.stringBuilder.set(new StringBuilder());
        this.classicResponseBuilder.set(ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders()));
        this.contentType.set(type);
    }

    @Override
    protected T buildResult() throws IOException {
        try {
            return responseHandler.handleResponse(classicResponseBuilder.get()
                    .setEntity(stringBuilder.get().toString(), contentType.get())
                    .build());
        } catch (HttpException e) {
            failed(e);
            return null;
        }
    }

    @Override
    protected int capacityIncrement() {
        return 0;
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) {
        stringBuilder.get().append(src);
    }

    @Override
    public void releaseResources() {
        // do nothing
    }
}
