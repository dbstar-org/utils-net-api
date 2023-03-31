package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

final class ResponseHandlerResponseConsumer<T> extends AbstractResponseHandlerResponseConsumer<T, T> {
    private final AtomicReference<StringBuilder> refStringBuilder = new AtomicReference<>();

    private ResponseHandlerResponseConsumer(final HttpClientResponseHandler<T> responseHandler, final Charset charset) {
        super(responseHandler, charset);
    }

    @Override
    protected void start(final HttpResponse response, final ContentType contentType) {
        super.start(response, contentType);
        this.refStringBuilder.set(new StringBuilder());
    }

    @Override
    protected T buildResult() throws IOException {
        return handleResponse(refStringBuilder.get().toString(), true);
    }

    @Override
    protected void data(final CharBuffer src, final boolean endOfStream) {
        refStringBuilder.get().append(src);
    }

    @Override
    public void releaseResources() {
        super.releaseResources();
        this.refStringBuilder.set(null);
    }

    static <T> ResponseHandlerResponseConsumer<T> create(final HttpClientResponseHandler<T> responseHandler,
                                                         final Charset charset) {
        return new ResponseHandlerResponseConsumer<>(responseHandler, charset);
    }
}
