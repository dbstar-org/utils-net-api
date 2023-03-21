package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import io.github.dbstarll.utils.http.client.response.ResponseHandlerFactory;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicResponseBuilder;
import org.apache.hc.core5.http.nio.AsyncEntityConsumer;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

        try {
            return httpClient.execute(requestProducer, responseConsumer, null, null, callback);
        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }

    protected final <T> Future<Message<HttpResponse, T>> execute(
            final AsyncRequestProducer requestProducer, final AsyncEntityConsumer<T> dataConsumer,
            final FutureCallback<Message<HttpResponse, T>> callback) throws ApiException {
        return execute(requestProducer, new BasicResponseConsumer<>(dataConsumer), callback);
    }

    protected final <T> Future<T> execute(final AsyncRequestProducer requestProducer,
                                          final HttpClientResponseHandler<T> responseHandler,
                                          final FutureCallback<T> callback) throws ApiException {
        notNull(responseHandler, "responseHandler is null");
        final Future<Message<HttpResponse, String>> future = execute(requestProducer, new StringAsyncEntityConsumer(),
                new FutureCallback<Message<HttpResponse, String>>() {
                    @Override
                    public void completed(final Message<HttpResponse, String> result) {
                        if (callback != null) {
                            final T res;
                            try {
                                res = parse(result, responseHandler);
                            } catch (IOException | HttpException e) {
                                failed(e);
                                return;
                            }
                            callback.completed(res);
                        }
                    }

                    @Override
                    public void failed(final Exception ex) {
                        if (callback != null) {
                            callback.failed(ex);
                        }
                    }

                    @Override
                    public void cancelled() {
                        if (callback != null) {
                            callback.cancelled();
                        }
                    }
                });
        return new Future<T>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return future.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return future.isCancelled();
            }

            @Override
            public boolean isDone() {
                return future.isDone();
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                try {
                    return parse(future.get(), responseHandler);
                } catch (IOException | HttpException e) {
                    throw new ExecutionException(e);
                }
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                try {
                    return parse(future.get(timeout, unit), responseHandler);
                } catch (IOException | HttpException e) {
                    throw new ExecutionException(e);
                }
            }
        };
    }

    protected final <T> Future<T> execute(final AsyncRequestProducer requestProducer, final Class<T> responseClass,
                                          final FutureCallback<T> callback) throws ApiException {
        notNull(responseClass, "responseClass is null");
        return execute(requestProducer, responseHandlerFactory.getResponseHandler(responseClass), callback);
    }

    private <T> T parse(final Message<HttpResponse, String> message, final HttpClientResponseHandler<T> responseHandler)
            throws IOException, HttpException {
        final HttpResponse response = message.getHead();
        System.out.println(Arrays.toString(response.getHeaders()));
        final ClassicResponseBuilder builder = ClassicResponseBuilder.create(response.getCode())
                .setVersion(response.getVersion())
                .setHeaders(response.getHeaders())
                .setEntity(message.getBody());
        return responseHandler.handleResponse(builder.build());
    }
}
