package io.github.dbstarll.utils.net.api;

import org.apache.commons.lang3.StringUtils;
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
        this(exception.getStatusCode(), exception.getReasonPhrase());
    }

    /**
     * 构造ApiResponseException.
     *
     * @param statusCode   错误码
     * @param reasonPhrase 错误原因
     */
    public ApiResponseException(final int statusCode, final String reasonPhrase) {
        super(formatMessage(statusCode, reasonPhrase));
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    private static String formatMessage(final int statusCode, final String reasonPhrase) {
        if (StringUtils.isBlank(reasonPhrase)) {
            return String.format("status code: %d", statusCode);
        } else {
            return String.format("status code: %d, reason phrase: %s", statusCode, reasonPhrase);
        }
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
