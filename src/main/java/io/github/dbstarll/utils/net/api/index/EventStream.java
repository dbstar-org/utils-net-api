package io.github.dbstarll.utils.net.api.index;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

public final class EventStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventStream.class);

    private static final String FIELD_EVENT = "event";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_ID = "id";
    private static final String FIELD_RETRY = "retry";

    private String event;
    private String data;
    private String id;
    private Integer retry;

    EventStream(final String content) {
        for (final String split : StringUtils.split(content, '\n')) {
            final int idxField = split.indexOf(':');
            if (idxField < 0) {
                setField(split, "");
            } else {
                setField(split.substring(0, idxField), split.substring(idxField + 1).trim());
            }
        }
    }

    private void setField(final String field, final String value) {
        switch (field) {
            case FIELD_EVENT:
                this.event = value;
                break;
            case FIELD_DATA:
                setData(value);
                break;
            case FIELD_ID:
                this.id = value;
                break;
            case FIELD_RETRY:
                setRetry(value);
                break;
            default:
                LOGGER.warn("unknown field: {}={}", field, value);
        }
    }

    /**
     * A string identifying the type of event described. If this is specified, an event will be dispatched on the
     * browser to the listener for the specified event name; the website source code should use addEventListener()
     * to listen for named events. The onmessage handler is called if no event name is specified for a message.
     *
     * @return event
     */
    public String getEvent() {
        return event;
    }

    private void setData(final String value) {
        if (data == null) {
            data = value;
        } else {
            data += '\n' + value;
        }
    }

    /**
     * The data field for the message. When the EventSource receives multiple consecutive lines that begin with data:,
     * it concatenates them, inserting a newline character between each one. Trailing newlines are removed.
     *
     * @return data
     */
    public String getData() {
        return data;
    }

    /**
     * The event ID to set the EventSource object's last event ID value.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    private void setRetry(final String value) {
        try {
            this.retry = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("retry not an integer: " + value, e);
        }
    }

    /**
     * The reconnection time. If the connection to the server is lost, the browser will wait for the specified time
     * before attempting to reconnect. This must be an integer, specifying the reconnection time in milliseconds.
     * If a non-integer value is specified, the field is ignored.
     *
     * @return retry
     */
    public Integer getRetry() {
        return retry;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventStream.class.getSimpleName() + "[", "]")
                .add("event='" + getEvent() + "'")
                .add("data='" + getData() + "'")
                .add("id='" + getId() + "'")
                .add("retry='" + getRetry() + "'")
                .toString();
    }
}
