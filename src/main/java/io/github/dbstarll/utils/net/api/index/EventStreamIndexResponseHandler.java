package io.github.dbstarll.utils.net.api.index;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class EventStreamIndexResponseHandler extends IndexBaseHttpClientResponseHandler<EventStreamIndex> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventStreamIndexResponseHandler.class);

    private static final String FIELD_EVENT = "event";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_ID = "id";
    private static final String FIELD_RETRY = "retry";

    /**
     * 构建EventStreamIndexResponseHandler.
     *
     * @param stringResponseHandler ResponseHandler for String
     */
    public EventStreamIndexResponseHandler(final HttpClientResponseHandler<String> stringResponseHandler) {
        super(stringResponseHandler);
    }

    @Override
    protected EventStreamIndex handleContent(final String content, final boolean endOfStream) {
        final int index = StringUtils.indexOf(content, "\n\n");
        if (index >= 0) {
            return new EventStreamIndex(parseEventStream(content.substring(0, index)), index + 2);
        } else if (endOfStream) {
            return new EventStreamIndex(parseEventStream(content), index);
        } else {
            return null;
        }
    }

    private EventStream parseEventStream(final String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }

        final EventStream eventStream = new EventStream();
        final long setAny = Arrays.stream(StringUtils.split(content, '\n')).map(split -> {
            final int idxField = split.indexOf(':');
            if (idxField < 0) {
                return setField(eventStream, split, "");
            } else {
                return setField(eventStream, split.substring(0, idxField), split.substring(idxField + 1).trim());
            }
        }).filter(b -> b).count();
        return setAny > 0 ? eventStream : null;
    }

    private boolean setField(final EventStream eventStream, final String field, final String value) {
        switch (field) {
            case FIELD_EVENT:
                eventStream.setEvent(value);
                return true;
            case FIELD_DATA:
                eventStream.setData(value);
                return true;
            case FIELD_ID:
                eventStream.setId(value);
                return true;
            case FIELD_RETRY:
                try {
                    eventStream.setRetry(Integer.valueOf(value));
                    return true;
                } catch (NumberFormatException e) {
                    LOGGER.warn("retry not an integer: " + value, e);
                    return false;
                }
            default:
                LOGGER.warn("unknown field: {}=[{}]", field, value);
                return false;
        }
    }
}
