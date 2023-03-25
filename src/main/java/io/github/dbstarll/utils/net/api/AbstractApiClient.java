package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.request.AbsoluteUriResolver;
import io.github.dbstarll.utils.http.client.request.UriResolver;
import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import io.github.dbstarll.utils.http.client.response.ResponseHandlerFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class AbstractApiClient<C> {
    private static final String HTTP_CLIENT_IS_NULL_EX_MESSAGE = "httpClient is null";
    private static final String URI_RESOLVER_IS_NULL_EX_MESSAGE = "uriResolver is null";
    private static final String CHARSET_IS_NULL_EX_MESSAGE = "charset is null";
    private static final String RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE = "responseHandlerFactory is null";
    private static final String REQUEST_IS_NULL_EX_MESSAGE = "request is null";
    private static final String ENTITY_FORMAT = "[Length: %s, Type: %s, Encoding: %s, chunked: %s]";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final C httpClient;

    private UriResolver uriResolver = new AbsoluteUriResolver();
    private Charset charset = StandardCharsets.UTF_8;
    private ResponseHandlerFactory responseHandlerFactory = new BasicResponseHandlerFactory();

    protected AbstractApiClient(final C httpClient) {
        this.httpClient = notNull(httpClient, HTTP_CLIENT_IS_NULL_EX_MESSAGE);
    }

    protected final void setUriResolver(final UriResolver uriResolver) {
        this.uriResolver = notNull(uriResolver, URI_RESOLVER_IS_NULL_EX_MESSAGE);
    }

    protected final void setCharset(final Charset charset) {
        this.charset = notNull(charset, CHARSET_IS_NULL_EX_MESSAGE);
    }

    protected final void setResponseHandlerFactory(final ResponseHandlerFactory responseHandlerFactory) {
        this.responseHandlerFactory = notNull(responseHandlerFactory, RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE);
    }

    protected final <T> HttpClientResponseHandler<T> getResponseHandler(final Class<T> responseClass) {
        return responseHandlerFactory.getResponseHandler(responseClass);
    }

    protected final ClassicRequestBuilder get(final URI uri) throws ApiException {
        return preProcessing(ClassicRequestBuilder.get(uri));
    }

    protected final ClassicRequestBuilder get(final String path) throws ApiException {
        return get(uriResolver.resolve(path));
    }

    protected final ClassicRequestBuilder post(final URI uri) throws ApiException {
        return preProcessing(ClassicRequestBuilder.post(uri));
    }

    protected final ClassicRequestBuilder post(final String path) throws ApiException {
        return post(uriResolver.resolve(path));
    }

    protected final ClassicRequestBuilder delete(final URI uri) throws ApiException {
        return preProcessing(ClassicRequestBuilder.delete(uri));
    }

    protected final ClassicRequestBuilder delete(final String path) throws ApiException {
        return delete(uriResolver.resolve(path));
    }

    /**
     * 在发出请求之前，预处理请求参数.
     *
     * @param builder ClassicRequestBuilder
     * @return ClassicRequestBuilder
     * @throws ApiException api处理异常
     */
    protected ClassicRequestBuilder preProcessing(final ClassicRequestBuilder builder) throws ApiException {
        return builder.setCharset(charset);
    }

    protected final void traceRequest(final ClassicHttpRequest request) throws IOException, ApiException {
        notNull(request, REQUEST_IS_NULL_EX_MESSAGE);

        final HttpEntity entity = request.getEntity();
        if (entity != null) {
            final String traceEntity = format(request.getEntity());
            if (entity.isRepeatable()) {
                final String content;
                try {
                    content = EntityUtils.toString(request.getEntity());
                } catch (ParseException e) {
                    throw new ApiProtocolException(e);
                }
                logger.trace("request: [{}]@{} with {}:{}:{}", request, request.hashCode(),
                        request.getEntity().getClass().getSimpleName(), traceEntity, content);
            } else {
                logger.trace("request: [{}]@{} with {}:{}", request, request.hashCode(),
                        request.getEntity().getClass().getSimpleName(), traceEntity);
            }
        } else {
            logger.trace("request: [{}]@{}", request, request.hashCode());
        }
    }

    protected final String format(final EntityDetails entity) {
        return String.format(ENTITY_FORMAT, entity.getContentLength(), entity.getContentType(),
                entity.getContentEncoding(), entity.isChunked());
    }
}
