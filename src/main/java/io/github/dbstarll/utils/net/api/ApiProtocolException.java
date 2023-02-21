package io.github.dbstarll.utils.net.api;

public class ApiProtocolException extends ApiException {
    private static final long serialVersionUID = 6900784991407549281L;

    /**
     * 构建ApiProtocolException.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ApiProtocolException(final String message) {
        super(message);
    }

    /**
     * 构建ApiProtocolException.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).  (A <tt>null</tt> value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     */
    public ApiProtocolException(final Throwable cause) {
        super(cause);
    }

    /**
     * 构建ApiProtocolException.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public ApiProtocolException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
