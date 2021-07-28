package io.github.dbstarll.utils.net.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpResponseException;

public class ApiResponseException extends ApiProtocolException {
  private static final long serialVersionUID = 7892913062028304357L;

  private final int statusCode;
  private final String reasonPhrase;

  public ApiResponseException(HttpResponseException exception) {
    this(exception.getStatusCode(), exception.getReasonPhrase());
  }

  /**
   * 构造ApiResponseException.
   *
   * @param statusCode   错误码
   * @param reasonPhrase 错误原因
   */
  public ApiResponseException(final int statusCode, final String reasonPhrase) {
    super(String.format("status code: %d" + (StringUtils.isBlank(reasonPhrase) ? "" : ", reason phrase: %s"),
            statusCode, reasonPhrase));
    this.statusCode = statusCode;
    this.reasonPhrase = reasonPhrase;
  }

  public int getStatusCode() {
    return this.statusCode;
  }

  public String getReasonPhrase() {
    return this.reasonPhrase;
  }
}
