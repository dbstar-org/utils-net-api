package io.github.dbstarll.utils.net.api.index;

import java.util.StringJoiner;

public final class EventStream {
    private String event;
    private String data;
    private String id;
    private Integer retry;

    EventStream() {
        // empty
    }

    void setEvent(final String event) {
        this.event = event;
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

    void setData(final String value) {
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

    void setId(final String id) {
        this.id = id;
    }

    /**
     * The event ID to set the EventSource object's last event ID value.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    void setRetry(final Integer value) {
        this.retry = value;
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
