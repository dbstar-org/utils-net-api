package io.github.dbstarll.utils.net.api.index;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.entity.AbstractCharDataConsumer;

import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class IndexBaseHttpClientResponseHandler<I extends Index<?>>
        implements HttpClientResponseHandler<I> {
    private final HttpClientResponseHandler<String> stringResponseHandler;

    protected IndexBaseHttpClientResponseHandler(final HttpClientResponseHandler<String> stringResponseHandler) {
        this.stringResponseHandler = stringResponseHandler;
    }

    @Override
    public final I handleResponse(final ClassicHttpResponse response) throws HttpException, IOException {
        final String content = stringResponseHandler.handleResponse(response);
        final Header header = response.getHeader(AbstractCharDataConsumer.class.getName() + "@endOfStream");
        return handleContent(content, Boolean.parseBoolean(notNull(header, "header:endOfStream is null").getValue()));
    }

    protected abstract I handleContent(String content, boolean endOfStream) throws IOException;
}
