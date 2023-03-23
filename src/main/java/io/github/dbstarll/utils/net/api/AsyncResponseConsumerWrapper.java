package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class AsyncResponseConsumerWrapper<T> implements AsyncResponseConsumer<T> {
    private final AsyncResponseConsumer<T> consumer;

    protected AsyncResponseConsumerWrapper(final AsyncResponseConsumer<T> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void consumeResponse(final HttpResponse response, final EntityDetails entityDetails,
                                final HttpContext context, final FutureCallback<T> resultCallback)
            throws HttpException, IOException {
        consumer.consumeResponse(response, entityDetails, context, resultCallback);
    }

    @Override
    public void informationResponse(final HttpResponse response, final HttpContext context)
            throws HttpException, IOException {
        consumer.informationResponse(response, context);
    }

    @Override
    public void failed(final Exception cause) {
        consumer.failed(cause);
    }

    @Override
    public void updateCapacity(final CapacityChannel capacityChannel) throws IOException {
        consumer.updateCapacity(capacityChannel);
    }

    @Override
    public void consume(final ByteBuffer src) throws IOException {
        consumer.consume(src);
    }

    @Override
    public void streamEnd(final List<? extends Header> trailers) throws HttpException, IOException {
        consumer.streamEnd(trailers);
    }

    @Override
    public void releaseResources() {
        consumer.releaseResources();
    }
}
