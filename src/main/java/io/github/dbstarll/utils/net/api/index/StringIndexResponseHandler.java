package io.github.dbstarll.utils.net.api.index;

import org.apache.commons.lang3.StringUtils;

public final class StringIndexResponseHandler extends IndexBaseHttpClientResponseHandler<StringIndex> {
    @Override
    protected StringIndex handleContent(final String content, final boolean endOfStream) {
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
