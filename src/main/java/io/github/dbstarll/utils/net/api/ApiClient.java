package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiClient extends AbstractApiClient<HttpClient> {
    private static final String RESPONSE_HANDLER_IS_NULL_EX_MESSAGE = "responseHandler is null";

    protected ApiClient(final HttpClient httpClient, final boolean alwaysProcessEntity) {
        super(httpClient, alwaysProcessEntity);
    }

    @Override
    protected ClassicRequestBuilder preProcessing(final ClassicRequestBuilder builder) throws ApiException {
        return super.preProcessing(builder).addHeader("Connection", "close");
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
        notNull(responseHandler, RESPONSE_HANDLER_IS_NULL_EX_MESSAGE);

        traceRequest(request);

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

    /**
     * 根据请求结果类来获得相应的请求结果.
     *
     * @param request       request
     * @param responseClass 请求结果类
     * @param <T>           请求结果类型
     * @return 请求结果
     * @throws IOException  IOException
     * @throws ApiException api处理异常
     */
    protected <T> T execute(final ClassicHttpRequest request, final Class<T> responseClass)
            throws IOException, ApiException {
        notNull(responseClass, "responseClass is null");
        return execute(request, getResponseHandler(responseClass));
    }
}
