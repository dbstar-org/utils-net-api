package io.github.dbstarll.utils.net.api;

public class ApiParameterException extends ApiException {
  private static final long serialVersionUID = 6900784991407549281L;

  /**
   * 构建ApiParameterException.
   *
   * @param cause the cause (which is saved for later retrieval by the
   *              {@link #getCause()} method).  (A <tt>null</tt> value is
   *              permitted, and indicates that the cause is nonexistent or
   *              unknown.)
   */
  public ApiParameterException(final Throwable cause) {
    super(cause);
  }
}
