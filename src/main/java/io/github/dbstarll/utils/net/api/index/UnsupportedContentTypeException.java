package io.github.dbstarll.utils.net.api.index;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ProtocolException;

public final class UnsupportedContentTypeException extends ProtocolException {
    private static final long serialVersionUID = 134343442150902042L;

    private final ContentType contentType;
    private final Class<?> responseHandlerClass;

    /**
     * 构造UnsupportedContentTypeException.
     *
     * @param contentType          type of Content
     * @param responseHandlerClass class of ResponseHandler
     */
    public UnsupportedContentTypeException(final ContentType contentType, final Class<?> responseHandlerClass) {
        super(String.format("Unsupported Content-Type: %s for %s", contentType, responseHandlerClass));
        this.contentType = contentType;
        this.responseHandlerClass = responseHandlerClass;
    }

    /**
     * 获得type of Content.
     *
     * @return type of Content
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * 获得class of ResponseHandler.
     *
     * @return class of ResponseHandler
     */
    public Class<?> getResponseHandlerClass() {
        return responseHandlerClass;
    }
}
