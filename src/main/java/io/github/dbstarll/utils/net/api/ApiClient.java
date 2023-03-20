package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import io.github.dbstarll.utils.http.client.response.ResponseHandlerFactory;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.net.URI;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiClient extends AbstractApiClient<HttpClient, ClassicHttpRequest, ClassicRequestBuilder> {
    private static final String RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE = "responseHandlerFactory is null";
    private static final String REQUEST_IS_NULL_EX_MESSAGE = "request is null";
    private static final String RESPONSE_HANDLER_IS_NULL_EX_MESSAGE = "responseHandler is null";

    private ResponseHandlerFactory responseHandlerFactory = new BasicResponseHandlerFactory();

    protected ApiClient(final HttpClient httpClient) {
        super(httpClient);
    }

    protected final void setResponseHandlerFactory(final ResponseHandlerFactory responseHandlerFactory) {
        this.responseHandlerFactory = notNull(responseHandlerFactory, RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE);
    }

    @Override
    protected ClassicRequestBuilder preProcessing(final ClassicRequestBuilder builder) throws ApiException {
        return super.preProcessing(builder).addHeader("Connection", "close");
    }

    @Override
    protected ClassicRequestBuilder builderGet(final URI uri) {
        return ClassicRequestBuilder.get(uri);
    }

    @Override
    protected ClassicRequestBuilder builderPost(final URI uri) {
        return ClassicRequestBuilder.post(uri);
    }

    @Override
    protected ClassicRequestBuilder builderDelete(final URI uri) {
        return ClassicRequestBuilder.delete(uri);
    }

    /**
     * 在获取请求结果之后，对结果进行包装.
     *
     * @param request       the request to execute
     * @param executeResult 请求结果
     * @param <T>           请求结果类型
     * @return 请求结果
     * @throws ApiException api处理异常
     */
    @SuppressWarnings("RedundantThrows")
    protected <T> T postProcessing(final ClassicHttpRequest request, final T executeResult) throws ApiException {
        if (executeResult != null) {
            logger.trace("response: [{}]@{} with {}:[{}]", request, request.hashCode(),
                    executeResult.getClass().getName(), executeResult);
        } else {
            logger.trace("response: [{}]@{} with null", request, request.hashCode());
        }
        return executeResult;
    }

    protected final <T> T execute(final ClassicHttpRequest request, final HttpClientResponseHandler<T> responseHandler)
            throws IOException, ApiException {
        try {
            notNull(request, REQUEST_IS_NULL_EX_MESSAGE);
            notNull(responseHandler, RESPONSE_HANDLER_IS_NULL_EX_MESSAGE);
        } catch (NullPointerException ex) {
            throw new ApiParameterException(ex);
        }

        if (request.getEntity() != null) {
            final HttpEntity entity = request.getEntity();
            logger.trace("request: [{}]@{} with {}:{}", request, request.hashCode(),
                    entity.getClass().getName(), entity);
        } else {
            logger.trace("request: [{}]@{}", request, request.hashCode());
        }

        try {
            return postProcessing(request, httpClient.execute(request, responseHandler));
        } catch (HttpResponseException ex) {
            throw new ApiResponseException(ex);
        } catch (ClientProtocolException ex) {
            throw new ApiProtocolException(ex);
        } catch (IOException | ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ex);
        }
    }

    protected final <T> T execute(final ClassicHttpRequest request, final Class<T> responseClass)
            throws IOException, ApiException {
        return execute(request, responseHandlerFactory.getResponseHandler(responseClass));
    }
}
