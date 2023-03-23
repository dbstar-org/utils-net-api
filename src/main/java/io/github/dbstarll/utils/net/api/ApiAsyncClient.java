package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
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

        final String traceRequest = String.format("[%s %s]@%s", requestBuilder.getMethod(), requestBuilder.getUri(),
                requestBuilder.hashCode());

        if (requestBuilder.getEntity() != null) {
            final String traceEntity = format(requestBuilder.getEntity());
            logger.trace("request: {} with {}", traceRequest, traceEntity);
        } else {
            logger.trace("request: {}", traceRequest);
        }

        return httpClient.execute(requestBuilder.build(), new AsyncResponseConsumerWrapper<T>(responseConsumer) {
            @Override
            public void consumeResponse(final HttpResponse response, final EntityDetails entityDetails,
                                        final HttpContext context, final FutureCallback<T> resultCallback)
                    throws HttpException, IOException {
                final String traceEntity = format(entityDetails);
                logger.trace("response: {} with {}", traceRequest, traceEntity);
                super.consumeResponse(response, entityDetails, context, resultCallback);
            }

            @Override
            public void consume(final ByteBuffer src) throws IOException {
                logger.trace("consume: {} with {} bytes", traceRequest, src.remaining());
                super.consume(src);
            }
        }, null, null, callback);
    }

    private String format(final EntityDetails entity) {
        return String.format(ENTITY_FORMAT, entity.getContentLength(), entity.getContentType(),
                entity.getContentEncoding(), entity.isChunked());
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

    protected final <T> Future<List<T>> execute(final AsyncRequestBuilder requestBuilder,
                                                final HttpClientResponseHandler<T> responseHandler,
                                                final StreamFutureCallback<T> callback) {
        notNull(responseHandler, "responseHandler is null");
        notNull(callback, "callback is null");
        return execute(requestBuilder,
                StreamResponseHandlerResponseConsumer.create(responseHandler, callback), callback);
    }

    protected final <T> Future<List<T>> execute(final AsyncRequestBuilder requestBuilder,
                                                final Class<T> responseClass,
                                                final StreamFutureCallback<T> callback) {
        notNull(responseClass, "responseClass is null");
        notNull(callback, "callback is null");
        return execute(requestBuilder, getResponseHandler(responseClass), callback);
    }
}
