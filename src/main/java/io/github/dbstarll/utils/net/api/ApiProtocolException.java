package io.github.dbstarll.utils.net.api;

public class ApiProtocolException extends ApiException {
  private static final long serialVersionUID = 6900784991407549281L;

  public ApiProtocolException(String message) {
    super(message);
  }

  public ApiProtocolException(Throwable cause) {
    super(cause);
  }

  public ApiProtocolException(String message, Throwable cause) {
    super(message, cause);
  }
}
