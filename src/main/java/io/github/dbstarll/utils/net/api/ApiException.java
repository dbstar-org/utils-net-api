package io.github.dbstarll.utils.net.api;

public class ApiException extends Exception {
  private static final long serialVersionUID = 4699417327765145867L;

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApiException(String message) {
    super(message);
  }

  public ApiException(Throwable cause) {
    super(cause);
  }
}
