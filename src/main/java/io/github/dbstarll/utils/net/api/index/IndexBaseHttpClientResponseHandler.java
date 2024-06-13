package io.github.dbstarll.utils.net.api.index;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.entity.AbstractCharDataConsumer;

import java.io.IOException;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class IndexBaseHttpClientResponseHandler<S, D, I extends Index<D>>
        implements HttpClientResponseHandler<I> {
    private final HttpClientResponseHandler<S> sourceResponseHandler;

    protected IndexBaseHttpClientResponseHandler(final HttpClientResponseHandler<S> sourceResponseHandler) {
        this.sourceResponseHandler = notNull(sourceResponseHandler, "sourceResponseHandler is null");
    }

    @Override
    public final I handleResponse(final ClassicHttpResponse response) throws HttpException, IOException {
        final ContentType contentType = parseContentType(response);
        if (supports(contentType)) {
            return handleContent(contentType, parseContent(response), parseEndOfStream(response));
        } else {
            throw new UnsupportedContentTypeException(contentType, getClass());
        }
    }

    private ContentType parseContentType(final MessageHeaders headers) {
        return Optional.ofNullable(headers.getFirstHeader(HttpHeaders.CONTENT_TYPE))
                .map(NameValuePair::getValue)
                .map(ContentType::parse)
                .orElse(null);
    }

    private S parseContent(final ClassicHttpResponse response) throws HttpException, IOException {
        return sourceResponseHandler.handleResponse(response);
    }

    private boolean parseEndOfStream(final MessageHeaders headers) {
        final Header header = headers.getFirstHeader(AbstractCharDataConsumer.class.getName() + "@endOfStream");
        return Boolean.parseBoolean(notNull(header, "header:endOfStream is null").getValue());
    }

    protected abstract boolean supports(ContentType contentType);

    protected abstract I handleContent(ContentType contentType, S content, boolean endOfStream)
            throws HttpException, IOException;
}
