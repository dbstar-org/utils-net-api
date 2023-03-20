package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import io.github.dbstarll.utils.http.client.response.ResponseHandlerFactory;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.net.URI;
import java.util.concurrent.Future;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiAsyncClient extends
        AbstractApiClient<HttpAsyncClient, AsyncRequestProducer, AsyncRequestBuilder> {
    private static final String RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE = "responseHandlerFactory is null";
    private static final String REQUEST_PRODUCER_IS_NULL_EX_MESSAGE = "requestProducer is null";
    private static final String RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE = "responseConsumer is null";

    private ResponseHandlerFactory responseHandlerFactory = new BasicResponseHandlerFactory();

    protected ApiAsyncClient(final HttpAsyncClient httpClient) {
        super(httpClient);
    }

    protected final void setResponseHandlerFactory(final ResponseHandlerFactory responseHandlerFactory) {
        this.responseHandlerFactory = notNull(responseHandlerFactory, RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE);
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

    protected final <T> Future<T> execute(final AsyncRequestProducer requestProducer,
                                          final AsyncResponseConsumer<T> responseConsumer,
                                          final FutureCallback<T> callback) throws ApiException {
        try {
            notNull(requestProducer, REQUEST_PRODUCER_IS_NULL_EX_MESSAGE);
            notNull(responseConsumer, RESPONSE_CONSUMER_IS_NULL_EX_MESSAGE);
        } catch (NullPointerException ex) {
            throw new ApiParameterException(ex);
        }

//        if (requestProducer.getEntity() != null) {
//            final HttpEntity entity = requestProducer.getEntity();
//            logger.trace("request: [{}]@{} with {}:{}", requestProducer, requestProducer.hashCode(),
//                    entity.getClass().getName(), entity);
//        } else {
        logger.trace("request: [{}]@{}", requestProducer, requestProducer.hashCode());
//        }

        final HttpContext context = HttpClientContext.create();
        try {
            return httpClient.execute(requestProducer, responseConsumer, null, context, callback);
        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }
}
