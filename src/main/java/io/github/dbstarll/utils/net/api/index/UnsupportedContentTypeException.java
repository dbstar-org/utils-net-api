package io.github.dbstarll.utils.net.api.index;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ProtocolException;

public final class UnsupportedContentTypeException extends ProtocolException {
    private static final long serialVersionUID = 134343442150902042L;

    private final ContentType contentType;
    private final Class<?> contentClass;

    /**
     * 构造UnsupportedContentTypeException.
     *
     * @param contentType  type of Content
     * @param contentClass class of Content
     */
    public UnsupportedContentTypeException(final ContentType contentType, final Class<?> contentClass) {
        super(String.format("Unsupported Content-Type: %s for %s", contentType, contentClass));
        this.contentType = contentType;
        this.contentClass = contentClass;
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
     * 获得class of Content.
     *
     * @return class of Content
     */
    public Class<?> getContentClass() {
        return contentClass;
    }
}
