package io.github.dbstarll.utils.net.api;

public class ApiException extends Exception {
    private static final long serialVersionUID = 4699417327765145867L;

    /**
     * 构建ApiException.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public ApiException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * 构建ApiException.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).  (A <tt>null</tt> value is
     *              permitted, and indicates that the cause is nonexistent or
     *              unknown.)
     */
    public ApiException(final Throwable cause) {
        super(cause);
    }
}
