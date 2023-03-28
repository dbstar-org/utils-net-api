package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.net.api.index.Index;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.core5.concurrent.CallbackContribution;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiAsyncClient extends AbstractApiClient<HttpAsyncClient> {
    private static final String RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE = "responseConsumer is null";

    protected ApiAsyncClient(final HttpAsyncClient httpClient) {
        super(httpClient);
        setResponseHandlerFactory(new BasicIndexResponseHandlerFactory());
    }

    private AsyncRequestProducer buildRequestProducer(final ClassicHttpRequest request) throws IOException {
        final HttpEntity entity = request.getEntity();
        if (entity != null) {
            final byte[] data = IOUtils.readFully(entity.getContent(), (int) entity.getContentLength());
            final ContentType contentType = ContentType.parse(entity.getContentType());
            return new BasicRequestProducer(request, AsyncEntityProducers.create(data, contentType));
        } else {
            return new BasicRequestProducer(request, null);
        }
    }

    /**
     * Triggered to signal receipt of a response message head.
     *
     * @param request       the request
     * @param response      the response message head.
     * @param entityDetails the response entity details or {@code null} if the response
     *                      does not enclose an entity.
     * @throws HttpException HttpException
     * @throws IOException   IOException
     */
    @SuppressWarnings("RedundantThrows")
    protected void consumeResponse(final ClassicHttpRequest request, final HttpResponse response,
                                   final EntityDetails entityDetails) throws HttpException, IOException {
        final String traceEntity = format(entityDetails);
        logger.trace("response: [{}]@{} with {}:{}", request, request.hashCode(), response.getCode(), traceEntity);
    }

    /**
     * Triggered to pass incoming data to the data consumer.
     *
     * @param request       the request
     * @param entityDetails the response entity details or {@code null} if the response
     *                      does not enclose an entity.
     * @param src           data source.
     * @throws IOException IOException
     */
    @SuppressWarnings("RedundantThrows")
    protected void consume(final ClassicHttpRequest request, final EntityDetails entityDetails, final ByteBuffer src)
            throws IOException {
        logger.trace("consume: [{}]@{} with [{}]:{} bytes", request, request.hashCode(), entityDetails.getContentType(),
                src.remaining());
    }

    /**
     * Triggered to pass incoming data packet to the data consumer.
     *
     * @param request the request
     * @param result  the data packet.
     * @param <T>     type of result
     * @return result
     */
    protected <T> T stream(final ClassicHttpRequest request, final T result) {
        logger.trace("stream: [{}]@{} with {}:[{}]", request, request.hashCode(), result.getClass().getName(), result);
        return result;
    }

    /**
     * 在获取请求结果之后，对结果进行包装.
     *
     * @param request the request to execute
     * @param result  请求结果
     * @param <T>     请求结果类型
     * @return 请求结果
     */
    protected <T> T completed(final ClassicHttpRequest request, final T result) {
        if (result != null) {
            logger.trace("completed: [{}]@{} with {}:[{}]", request, request.hashCode(),
                    result.getClass().getName(), result);
        } else {
            logger.trace("completed: [{}]@{} with null", request, request.hashCode());
        }
        return result;
    }

    protected final <T> Future<T> execute(final ClassicHttpRequest request,
                                          final AsyncResponseConsumer<T> responseConsumer,
                                          final FutureCallback<T> callback) throws IOException {
        notNull(responseConsumer, RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE);

        traceRequest(request);

        return httpClient.execute(buildRequestProducer(request), new AsyncResponseConsumerWrapper<T>(responseConsumer) {
            private final AtomicReference<EntityDetails> refEntityDetails = new AtomicReference<>();

            @Override
            public void consumeResponse(final HttpResponse response, final EntityDetails entityDetails,
                                        final HttpContext context, final FutureCallback<T> resultCallback)
                    throws HttpException, IOException {
                this.refEntityDetails.set(entityDetails);
                ApiAsyncClient.this.consumeResponse(request, response, entityDetails);
                super.consumeResponse(response, entityDetails, context, resultCallback);
            }

            @Override
            public void consume(final ByteBuffer src) throws IOException {
                final int position = src.position();
                try {
                    ApiAsyncClient.this.consume(request, refEntityDetails.get(), src);
                } finally {
                    src.position(position);
                }
                super.consume(src);
            }

            @Override
            public void releaseResources() {
                this.refEntityDetails.set(null);
                super.releaseResources();
            }
        }, null, null, new CallbackContribution<T>(callback) {
            @Override
            public void completed(final T result) {
                final T finalResult = ApiAsyncClient.this.completed(request, result);
                if (callback != null) {
                    callback.completed(finalResult);
                }
            }
        });
    }

    protected final <T> Future<T> execute(final ClassicHttpRequest request,
                                          final HttpClientResponseHandler<T> responseHandler,
                                          final FutureCallback<T> callback) throws IOException {
        notNull(responseHandler, "responseHandler is null");
        return execute(request, ResponseHandlerResponseConsumer.create(responseHandler), callback);
    }

    protected final <T> Future<T> execute(final ClassicHttpRequest request,
                                          final Class<T> responseClass,
                                          final FutureCallback<T> callback) throws IOException {
        notNull(responseClass, "responseClass is null");
        return execute(request, getResponseHandler(responseClass), callback);
    }

    protected final <T> Future<Void> execute(final ClassicHttpRequest request,
                                             final HttpClientResponseHandler<? extends Index<T>> responseHandler,
                                             final StreamFutureCallback<T> callback) throws IOException {
        notNull(responseHandler, "responseHandler is null");
        notNull(callback, "callback is null");
        return execute(request, StreamResponseHandlerResponseConsumer.create(responseHandler,
                result -> callback.stream(ApiAsyncClient.this.stream(request, result))), callback);
    }

    protected final <T> Future<Void> execute(final ClassicHttpRequest request, final Class<T> responseClass,
                                             final StreamFutureCallback<T> callback) throws IOException {
        notNull(responseClass, "responseClass is null");
        notNull(callback, "callback is null");
        final Class<? extends Index<T>> streamResponseClass = getStreamResponseClass(responseClass);
        notNull(streamResponseClass, "streamResponseClass is null");
        return execute(request, getResponseHandler(streamResponseClass), callback);
    }

    @SuppressWarnings("unchecked")
    protected final <T> Class<? extends Index<T>> getStreamResponseClass(final Class<T> responseClass) {
        for (Class<?> c : responseClassIterator()) {
            if (Index.class.isAssignableFrom(c)
                    && responseClass == ((ParameterizedType) c.getGenericSuperclass()).getActualTypeArguments()[0]) {
                return (Class<? extends Index<T>>) c;
            }
        }
        return null;
    }
}
