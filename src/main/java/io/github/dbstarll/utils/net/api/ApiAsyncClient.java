package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;

import java.net.URI;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiAsyncClient extends
        AbstractApiClient<HttpAsyncClient, AsyncRequestProducer, AsyncRequestBuilder> {
    private static final String REQUEST_BUILDER_IS_NULL_EX_MESSAGE = "requestBuilder is null";
    private static final String RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE = "responseConsumer is null";
    private static final String ENTITY_FORMAT
            = "[Content-Length: %s, Content-Type: %s, Content-Encoding: %s, chunked: %s]";

    protected ApiAsyncClient(final HttpAsyncClient httpClient) {
        super(httpClient);
    }

    @Override
    protected AsyncRequestBuilder builderGet(final URI uri) {
        return AsyncRequestBuilder.get(uri);
    }

    @Override
    protected AsyncRequestBuilder builderPost(final URI uri) {
        return AsyncRequestBuilder.post(uri);
    }

    @Override
    protected AsyncRequestBuilder builderDelete(final URI uri) {
        return AsyncRequestBuilder.delete(uri);
    }

    protected final <T> Future<T> execute(final AsyncRequestBuilder requestBuilder,
                                          final AsyncResponseConsumer<T> responseConsumer,
                                          final FutureCallback<T> callback) {
        notNull(requestBuilder, REQUEST_BUILDER_IS_NULL_EX_MESSAGE);
        notNull(responseConsumer, RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE);

        if (requestBuilder.getEntity() != null) {
            final AsyncEntityProducer entity = requestBuilder.getEntity();
            final String entityString = String.format(ENTITY_FORMAT, entity.getContentLength(), entity.getContentType(),
                    entity.getContentEncoding(), entity.isChunked());
            logger.trace("request: [{} {}]@{} with {}:{}", requestBuilder.getMethod(), requestBuilder.getUri(),
                    requestBuilder.hashCode(), entity.getClass().getName(), entityString);
        } else {
            logger.trace("request: [{} {}]@{}", requestBuilder.getMethod(), requestBuilder.getUri(),
                    requestBuilder.hashCode());
        }
// response: [POST http://localhost:62052/ping.html]@402695541 with java.lang.String:[ok]

        return httpClient.execute(requestBuilder.build(), responseConsumer, null, null, callback);
    }

    protected final <T> Future<T> execute(final AsyncRequestBuilder requestBuilder,
                                          final HttpClientResponseHandler<T> responseHandler,
                                          final FutureCallback<T> callback) {
        notNull(responseHandler, "responseHandler is null");
        return execute(requestBuilder, ResponseHandlerResponseConsumer.create(responseHandler), callback);
    }

    protected final <T> Future<T> execute(final AsyncRequestBuilder requestBuilder,
                                          final Class<T> responseClass,
                                          final FutureCallback<T> callback) {
        notNull(responseClass, "responseClass is null");
        return execute(requestBuilder, getResponseHandler(responseClass), callback);
    }
}
