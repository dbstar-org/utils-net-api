package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.request.AbsoluteUriResolver;
import io.github.dbstarll.utils.http.client.request.UriResolver;
import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import io.github.dbstarll.utils.http.client.response.ResponseHandlerFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class ApiClient {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String HTTP_CLIENT_IS_NULL_EX_MESSAGE = "httpClient is null";
    private static final String RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE = "responseHandlerFactory is null";
    private static final String URI_RESOLVER_IS_NULL_EX_MESSAGE = "uriResolver is null";
    private static final String CHARSET_IS_NULL_EX_MESSAGE = "charset is null";
    private static final String REQUEST_IS_NULL_EX_MESSAGE = "request is null";
    private static final String RESPONSE_HANDLER_IS_NULL_EX_MESSAGE = "responseHandler is null";

    private final HttpClient httpClient;

    private ResponseHandlerFactory responseHandlerFactory = new BasicResponseHandlerFactory();
    private UriResolver uriResolver = new AbsoluteUriResolver();
    private Charset charset = StandardCharsets.UTF_8;

    protected ApiClient(final HttpClient httpClient) {
        this.httpClient = notNull(httpClient, HTTP_CLIENT_IS_NULL_EX_MESSAGE);
    }

    protected final void setResponseHandlerFactory(final ResponseHandlerFactory responseHandlerFactory) {
        this.responseHandlerFactory = notNull(responseHandlerFactory, RESPONSE_HANDLER_FACTORY_IS_NULL_EX_MESSAGE);
    }

    protected final void setUriResolver(final UriResolver uriResolver) {
        this.uriResolver = notNull(uriResolver, URI_RESOLVER_IS_NULL_EX_MESSAGE);
    }

    protected final void setCharset(final Charset charset) {
        this.charset = notNull(charset, CHARSET_IS_NULL_EX_MESSAGE);
    }

    protected final RequestBuilder get(final URI uri) throws ApiException {
        return preProcessing(RequestBuilder.get(uri));
    }

    protected final RequestBuilder get(final String path) throws ApiException {
        return get(uriResolver.resolve(path));
    }

    protected final RequestBuilder post(final URI uri) throws ApiException {
        return preProcessing(RequestBuilder.post(uri));
    }

    protected final RequestBuilder post(final String path) throws ApiException {
        return post(uriResolver.resolve(path));
    }

    protected final RequestBuilder delete(final URI uri) throws ApiException {
        return preProcessing(RequestBuilder.delete(uri));
    }

    protected final RequestBuilder delete(final String path) throws ApiException {
        return delete(uriResolver.resolve(path));
    }

    /**
     * 在发出请求之前，预处理请求参数.
     *
     * @param builder RequestBuilder
     * @return RequestBuilder
     * @throws ApiException api处理异常
     */
    @SuppressWarnings("RedundantThrows")
    protected RequestBuilder preProcessing(final RequestBuilder builder) throws ApiException {
        return builder.addHeader("Connection", "close").setCharset(charset);
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
    protected <T> T postProcessing(final HttpUriRequest request, final T executeResult) throws ApiException {
        if (executeResult != null) {
            logger.trace("response: [{}]@{} with {}:[{}]", request, request.hashCode(),
                    executeResult.getClass().getName(), executeResult);
        } else {
            logger.trace("response: [{}]@{} with null", request, request.hashCode());
        }
        return executeResult;
    }

    protected final <T> T execute(final HttpUriRequest request, final ResponseHandler<T> responseHandler)
            throws IOException, ApiException {
        try {
            notNull(request, REQUEST_IS_NULL_EX_MESSAGE);
            notNull(responseHandler, RESPONSE_HANDLER_IS_NULL_EX_MESSAGE);
        } catch (NullPointerException ex) {
            throw new ApiParameterException(ex);
        }

        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
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

    protected final <T> T execute(final HttpUriRequest request, final Class<T> responseClass)
            throws IOException, ApiException {
        return execute(request, responseHandlerFactory.getResponseHandler(responseClass));
    }
}
