package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;
import org.apache.hc.core5.http.nio.entity.AbstractCharDataConsumer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractResponseHandlerResponseConsumer<H, T> extends AbstractCharResponseConsumer<T> {
    private final AtomicReference<HttpResponse> refHttpResponse = new AtomicReference<>();
    protected final AtomicReference<ContentType> refContentType = new AtomicReference<>();

    private final HttpClientResponseHandler<H> responseHandler;
    private final Charset charset;

    protected AbstractResponseHandlerResponseConsumer(final HttpClientResponseHandler<H> responseHandler,
                                                      final Charset charset) {
        this.responseHandler = responseHandler;
        this.charset = charset;
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        this.refHttpResponse.set(response);
        this.refContentType.set(parseContentType(response, contentType));
    }

    private ContentType parseContentType(final HttpResponse response, final ContentType contentType) {
        final Header h = response.getFirstHeader(HttpHeaders.CONTENT_TYPE);
        final boolean defaultCharsetNotMatch = h == null && !charset.equals(contentType.getCharset());
        final boolean customCharsetNotSet = h != null && contentType.getCharset() == null;
        if (defaultCharsetNotMatch || customCharsetNotSet) {
            setCharset(charset);
            return ContentType.create(contentType.getMimeType(), charset);
        } else {
            return contentType;
        }
    }

    protected final H handleResponse(final String content, final boolean endOfStream) throws IOException {
        final HttpResponse response = refHttpResponse.get();
        final ClassicHttpResponse classicHttpResponse = ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders())
                .setEntity(content, refContentType.get())
                .setHeader(AbstractCharDataConsumer.class.getName() + "@endOfStream", Boolean.toString(endOfStream))
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
