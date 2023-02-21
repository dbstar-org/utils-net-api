package io.github.dbstarll.utils.net.api;

import org.apache.http.client.HttpResponseException;

public class ApiResponseException extends ApiProtocolException {
    private static final long serialVersionUID = 7892913062028304357L;

    private final int statusCode;
    private final String reasonPhrase;

    /**
     * 构建ApiResponseException.
     *
     * @param exception HttpResponseException
     */
    public ApiResponseException(final HttpResponseException exception) {
        super(exception.getMessage(), exception);
        this.statusCode = exception.getStatusCode();
        this.reasonPhrase = exception.getReasonPhrase();
    }

    /**
     * 获得异常的状态码.
     *
     * @return 状态码
     */
    public final int getStatusCode() {
        return this.statusCode;
    }

    /**
     * 获得异常的原因.
     *
     * @return 异常原因
     */
    public final String getReasonPhrase() {
        return this.reasonPhrase;
    }
}
