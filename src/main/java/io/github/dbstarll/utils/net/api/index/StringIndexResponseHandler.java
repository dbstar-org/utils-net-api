package io.github.dbstarll.utils.net.api.index;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public final class StringIndexResponseHandler extends IndexBaseHttpClientResponseHandler<String, String, StringIndex> {
    /**
     * 构建StringIndexResponseHandler.
     *
     * @param stringResponseHandler ResponseHandler for String
     */
    public StringIndexResponseHandler(final HttpClientResponseHandler<String> stringResponseHandler) {
        super(stringResponseHandler);
    }

    @Override
    protected boolean supports(final ContentType contentType) {
        return true;
    }

    @Override
    protected StringIndex handleContent(final ContentType contentType, final String content, final boolean endOfStream) {
        final int index = StringUtils.indexOf(content, '\n');
        if (index >= 0) {
            return new StringIndex(content.substring(0, index), index + 1);
        } else if (endOfStream) {
            return new StringIndex(StringUtils.isBlank(content) ? null : content, index);
        } else {
            return null;
        }
    }
}
