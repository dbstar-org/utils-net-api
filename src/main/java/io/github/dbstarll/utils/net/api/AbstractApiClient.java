package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.request.AbsoluteUriResolver;
import io.github.dbstarll.utils.http.client.request.UriResolver;
import org.apache.hc.core5.http.support.AbstractRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class AbstractApiClient<C, R, B extends AbstractRequestBuilder<R>> {
    private static final String HTTP_CLIENT_IS_NULL_EX_MESSAGE = "httpClient is null";
    private static final String URI_RESOLVER_IS_NULL_EX_MESSAGE = "uriResolver is null";
    private static final String CHARSET_IS_NULL_EX_MESSAGE = "charset is null";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final C httpClient;

    private UriResolver uriResolver = new AbsoluteUriResolver();
    private Charset charset = StandardCharsets.UTF_8;

    protected AbstractApiClient(final C httpClient) {
        this.httpClient = notNull(httpClient, HTTP_CLIENT_IS_NULL_EX_MESSAGE);
    }

    protected final void setUriResolver(final UriResolver uriResolver) {
        this.uriResolver = notNull(uriResolver, URI_RESOLVER_IS_NULL_EX_MESSAGE);
    }

    protected final void setCharset(final Charset charset) {
        this.charset = notNull(charset, CHARSET_IS_NULL_EX_MESSAGE);
    }

    protected final B get(final URI uri) throws ApiException {
        return preProcessing(builderGet(uri));
    }

    protected final B get(final String path) throws ApiException {
        return get(uriResolver.resolve(path));
    }

    protected final B post(final URI uri) throws ApiException {
        return preProcessing(builderPost(uri));
    }

    protected final B post(final String path) throws ApiException {
        return post(uriResolver.resolve(path));
    }

    protected final B delete(final URI uri) throws ApiException {
        return preProcessing(builderDelete(uri));
    }

    protected final B delete(final String path) throws ApiException {
        return delete(uriResolver.resolve(path));
    }

    protected abstract B builderGet(URI uri);

    protected abstract B builderPost(URI uri);

    protected abstract B builderDelete(URI uri);

    /**
     * 在发出请求之前，预处理请求参数.
     *
     * @param builder AbstractRequestBuilder
     * @return AbstractRequestBuilder
     * @throws ApiException api处理异常
     */
    @SuppressWarnings("unchecked")
    protected B preProcessing(final B builder) throws ApiException {
        return (B) builder.setCharset(charset);
    }
}
