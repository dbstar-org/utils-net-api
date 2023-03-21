package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.HttpClientFactory;
import io.github.dbstarll.utils.http.client.request.RelativeUriResolver;
import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * test ApiAsyncClient
 */
class ApiAsyncClientTest {
    @SafeVarargs
    private final void useServer(final ThrowingConsumer<MockWebServer> consumer,
                                 final ThrowingConsumer<MockWebServer>... customizers) throws Throwable {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("ok"));
            for (ThrowingConsumer<MockWebServer> c : customizers) {
                c.accept(server);
            }
            server.start();
            consumer.accept(server);
        }
    }

    @SafeVarargs
    private final void useClient(final ThrowingBiConsumer<MockWebServer, MyClient> consumer,
                                 final ThrowingConsumer<MockWebServer>... customizers) throws Throwable {
        useServer(server -> {
            try (CloseableHttpAsyncClient client = new HttpClientFactory().setAutomaticRetries(false).buildAsync()) {
                client.start();
                consumer.accept(server, new MyClient(client, server.url("/").toString()));
            }
        }, customizers);
    }

    @Test
    void get() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            final Future<String> future = client.execute(client.get("/ping.html"), String.class, callback);
            assertEquals("ok", future.get());
            callback.assertResult("ok");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("GET", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void getNull() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<Float> callback = new MyFutureCallback<>();
            assertNull(client.execute(client.get("/ping.html"), Float.class, callback).get());
            callback.assertResult(null);
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("GET", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void post() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("ok", client.execute(client.post("/ping.html"), String.class, callback).get());
            callback.assertResult("ok");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void postJson() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("ok", client.execute(client.post("/ping.html")
                    .setEntity("{}", ContentType.APPLICATION_JSON), String.class, callback).get());
            callback.assertResult("ok");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void delete() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("ok", client.execute(client.delete("/ping.html"), String.class, callback).get());
            callback.assertResult("ok");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("DELETE", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void socketTimeoutException() throws Throwable {
        useClient((server, client) -> {
            final AsyncRequestBuilder request = client.get("/ping.html");
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("ok", client.execute(request, String.class, callback).get());
            callback.assertResult("ok");

            final MyFutureCallback<String> callback2 = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, String.class, callback2).get());
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
            assertEquals(2, server.getRequestCount());
            callback2.assertException(e.getCause());
        });
    }

    @Test
    void nullPointerException() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<Integer> callback = new MyFutureCallback<>();
            final NullPointerException e = assertThrowsExactly(NullPointerException.class, () -> client.execute(client.get("/ping.html"), Integer.class, callback).get());
            assertEquals("responseHandler is null", e.getMessage());
            assertFalse(callback.called);
            assertEquals(0, server.getRequestCount());
        });
    }

    @Test
    void apiResponseException() throws Throwable {
        useClient((server, client) -> {
            final AsyncRequestBuilder request = client.get("/ping.html");
            assertEquals("ok", client.execute(request, String.class, null).get());

            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, String.class, callback).get());
            assertSame(HttpResponseException.class, e.getCause().getClass());
            final HttpResponseException e2 = (HttpResponseException) e.getCause();
            assertEquals(404, e2.getStatusCode());
            assertEquals("Not Found", e2.getReasonPhrase());
            assertEquals("status code: 404, reason phrase: Not Found", e2.getMessage());
            assertNull(e2.getCause());
            callback.assertException(e2);
            assertEquals(2, server.getRequestCount());
        }, s -> s.enqueue(new MockResponse().setResponseCode(404)));
    }

    @Test
    void apiProtocolException() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<Long> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(client.get("/ping.html"), Long.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(ClientProtocolException.class, e.getCause().getClass());
            assertEquals("not a Long value: ok", e.getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void apiException() throws Throwable {
        useClient((server, client) -> {
            final MyFutureCallback<Boolean> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(client.get("/ping.html"), Boolean.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            assertEquals("Unsupported", e.getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(1, server.getRequestCount());
        });
    }

    private static class MyClient extends ApiAsyncClient {
        public MyClient(final HttpAsyncClient httpClient, final String uriBase) {
            super(httpClient);
            setUriResolver(new RelativeUriResolver(uriBase));
            setCharset(StandardCharsets.UTF_8);
            setResponseHandlerFactory(new MyResponseHandlerFactory());
        }
    }

    private static class MyResponseHandlerFactory extends BasicResponseHandlerFactory {
        public MyResponseHandlerFactory() {
            addResponseHandler(Long.class, new AbstractHttpClientResponseHandler<Long>() {
                @Override
                public Long handleEntity(HttpEntity entity) throws IOException {
                    final String value;
                    try {
                        value = EntityUtils.toString(entity);
                    } catch (final ParseException ex) {
                        throw new ClientProtocolException(ex);
                    }
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        throw new ClientProtocolException("not a Long value: " + value, e);
                    }
                }
            });
            addResponseHandler(Boolean.class, response -> {
                throw new UnsupportedOperationException("Unsupported");
            });
            addResponseHandler(Float.class, response -> null);
        }
    }

    private static class MyFutureCallback<T> implements FutureCallback<T> {
        private final Object lock = new Object();

        private volatile boolean called;

        private volatile T result;
        private volatile Exception ex;
        private volatile boolean cancelled;

        @Override
        public void completed(T result) {
            this.result = result;
            synchronized (lock) {
                this.called = true;
                lock.notify();
            }
        }

        @Override
        public void failed(Exception ex) {
            this.ex = ex;
            synchronized (lock) {
                this.called = true;
                lock.notify();
            }
        }

        @Override
        public void cancelled() {
            this.cancelled = true;
            synchronized (lock) {
                this.called = true;
                lock.notify();
            }
        }

        public void assertResult(T result) {
            waitCall();
            assertEquals(result, this.result);
        }

        public void assertException(Throwable ex) {
            waitCall();
            assertSame(ex, this.ex);
        }

        private void waitCall() {
            while (!called) {
                synchronized (lock) {
                    try {
                        lock.wait(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }
            }
        }
    }
}