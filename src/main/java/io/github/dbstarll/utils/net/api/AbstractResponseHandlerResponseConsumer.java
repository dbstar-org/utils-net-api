package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractResponseHandlerResponseConsumer<H, T> extends AbstractCharResponseConsumer<T> {
    private final AtomicReference<HttpResponse> refHttpResponse = new AtomicReference<>();
    private final AtomicReference<ContentType> refContentType = new AtomicReference<>();

    private final HttpClientResponseHandler<H> responseHandler;

    protected AbstractResponseHandlerResponseConsumer(final HttpClientResponseHandler<H> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        this.refHttpResponse.set(response);
        this.refContentType.set(contentType);
    }

    protected final H handleResponse(final String content) throws IOException {
        final HttpResponse response = refHttpResponse.get();
        final ClassicHttpResponse classicHttpResponse = ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders())
                .setEntity(content, refContentType.get())
                .build();
        try {
            return responseHandler.handleResponse(classicHttpResponse);
        } catch (HttpException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected int capacityIncrement() {
        return 0;
    }

    @Override
    public void releaseResources() {
        this.refHttpResponse.set(null);
        this.refContentType.set(null);
    }
}
