package io.github.dbstarll.utils.net.api;

import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.impl.nio.DefaultHttpResponseFactory;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncResponseConsumerWrapperTest {
    @Test
    void informationResponse() throws IOException, HttpException {
        final HttpResponse myResponse = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(200);
        final HttpContext myContext = HttpClientContext.create();
        final AtomicBoolean called = new AtomicBoolean();
        new AsyncResponseConsumerWrapper<String>(new AbstractResponseHandlerResponseConsumer<String, String>(new BasicHttpClientResponseHandler()) {
            @Override
            public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
                super.informationResponse(response, context);
                assertSame(myResponse, response);
                assertSame(myContext, context);
                called.set(true);
            }

            @Override
            protected String buildResult() {
                return null;
            }

            @Override
            protected void data(CharBuffer src, boolean endOfStream) {
            }
        }) {
        }.informationResponse(myResponse, myContext);
        assertTrue(called.get());
    }

    @Test
    void updateCapacity() throws IOException {
        final AtomicBoolean called = new AtomicBoolean();
        final AtomicInteger refIncrement = new AtomicInteger();
        new AsyncResponseConsumerWrapper<String>(new AbstractResponseHandlerResponseConsumer<String, String>(new BasicHttpClientResponseHandler()) {
            @Override
            protected int capacityIncrement() {
                called.set(true);
                return 10 + super.capacityIncrement();
            }

            @Override
            protected String buildResult() {
                return null;
            }

            @Override
            protected void data(CharBuffer src, boolean endOfStream) {
            }
        }) {
        }.updateCapacity(refIncrement::set);
        assertTrue(called.get());
        assertEquals(10, refIncrement.get());
    }
}