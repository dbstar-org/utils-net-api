package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;

import java.io.IOException;
import java.nio.CharBuffer;

final class ResponseHandlerResponseConsumer<T> extends AbstractCharResponseConsumer<T> {
    private final HttpClientResponseHandler<T> responseHandler;

    private volatile StringBuilder stringBuilder;
    private volatile ClassicResponseBuilder classicResponseBuilder;
    private volatile ContentType contentType;

    static <T> ResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler) {
        return new ResponseHandlerResponseConsumer<>(responseHandler);
    }

    private ResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType type) {
        this.stringBuilder = new StringBuilder();
        this.classicResponseBuilder = ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders());
        this.contentType = type;
    }

    @Override
    protected T buildResult() throws IOException {
        final String content = stringBuilder.toString();
        if (contentType != null) {
            classicResponseBuilder.setEntity(content, contentType);
        } else {
            classicResponseBuilder.setEntity(content);
        }
        try {
            return responseHandler.handleResponse(classicResponseBuilder.build());
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
        stringBuilder.append(src);
    }

    @Override
    public void releaseResources() {
    }
}
