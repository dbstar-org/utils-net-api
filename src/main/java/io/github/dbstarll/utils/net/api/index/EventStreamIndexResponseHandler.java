package io.github.dbstarll.utils.net.api.index;

import org.apache.commons.lang3.StringUtils;

public final class EventStreamIndexResponseHandler extends IndexBaseHttpClientResponseHandler<EventStreamIndex> {
    @Override
    protected EventStreamIndex handleContent(final String content, final boolean endOfStream) {
        final int index = StringUtils.indexOf(content, "\n\n");
        if (index >= 0) {
            return new EventStreamIndex(new EventStream(content.substring(0, index)), index + 2);
        } else if (endOfStream) {
            return new EventStreamIndex(StringUtils.isBlank(content) ? null : new EventStream(content), index);
        } else {
            return null;
        }
    }
}
