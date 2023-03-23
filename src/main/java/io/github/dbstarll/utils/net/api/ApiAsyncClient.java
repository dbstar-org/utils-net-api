package io.github.dbstarll.utils.net.api;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiAsyncClient extends AbstractApiClient<HttpAsyncClient> {
    private static final String RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE = "responseConsumer is null";
    private static final String ENTITY_FORMAT
            = "[Content-Length: %s, Content-Type: %s, Content-Encoding: %s, chunked: %s]";

    protected ApiAsyncClient(final HttpAsyncClient httpClient) {
        super(httpClient);
    }

    protected final <T> Future<T> execute(final ClassicHttpRequest request,
                                          final AsyncResponseConsumer<T> responseConsumer,
                                          final FutureCallback<T> callback) throws IOException {
        notNull(responseConsumer, RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE);

        traceRequest(request);

        final AsyncEntityProducer dataProducer;
        final HttpEntity entity = request.getEntity();
        if (entity != null) {
            final byte[] data = IOUtils.readFully(entity.getContent(), (int) entity.getContentLength());
            dataProducer = AsyncEntityProducers.create(data, ContentType.parse(entity.getContentType()));
        } else {
            dataProducer = null;
        }
        final AsyncRequestProducer requestProducer = new BasicRequestProducer(request, dataProducer);

        return httpClient.execute(requestProducer, new AsyncResponseConsumerWrapper<T>(responseConsumer) {
            @Override
            public void consumeResponse(final HttpResponse response, final EntityDetails entityDetails,
                                        final HttpContext context, final FutureCallback<T> resultCallback)
                    throws HttpException, IOException {
                final String traceEntity = format(entityDetails);
                logger.trace("response: [{}]@{} with {}", request, request.hashCode(), traceEntity);
                super.consumeResponse(response, entityDetails, context, resultCallback);
            }

            @Override
            public void consume(final ByteBuffer src) throws IOException {
                logger.trace("consume: [{}]@{} with {} bytes", request, request.hashCode(), src.remaining());
                super.consume(src);
            }
        }, null, null, callback);
    }

    private String format(final EntityDetails entity) {
        return String.format(ENTITY_FORMAT, entity.getContentLength(), entity.getContentType(),
                entity.getContentEncoding(), entity.isChunked());
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

    protected final <T> Future<List<T>> execute(final ClassicHttpRequest request,
                                                final HttpClientResponseHandler<T> responseHandler,
                                                final StreamFutureCallback<T> callback) throws IOException {
        notNull(responseHandler, "responseHandler is null");
        notNull(callback, "callback is null");
        return execute(request, StreamResponseHandlerResponseConsumer.create(responseHandler, callback), callback);
    }

    protected final <T> Future<List<T>> execute(final ClassicHttpRequest request,
                                                final Class<T> responseClass,
                                                final StreamFutureCallback<T> callback) throws IOException {
        notNull(responseClass, "responseClass is null");
        notNull(callback, "callback is null");
        return execute(request, getResponseHandler(responseClass), callback);
    }
}
