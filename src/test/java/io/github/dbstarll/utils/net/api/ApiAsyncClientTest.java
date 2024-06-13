package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.HttpClientFactory;
import io.github.dbstarll.utils.http.client.request.RelativeUriResolver;
import io.github.dbstarll.utils.http.client.response.AbstractResponseHandlerFactory;
import io.github.dbstarll.utils.net.api.index.EventStream;
import io.github.dbstarll.utils.net.api.index.UnsupportedContentTypeException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test ApiAsyncClient
 */
class ApiAsyncClientTest {
    @SafeVarargs
    private final void useServer(final ThrowingConsumer<MockWebServer> consumer,
                                 final ThrowingConsumer<MockWebServer>... customizers) throws Throwable {
        try (final MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("好")
                    .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.create("text/plain", StandardCharsets.UTF_8)));
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
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            final Future<String> future = client.execute(request, String.class, callback);
            assertEquals("好", future.get());
            callback.assertResult("好");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("GET", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void getWithCharset() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final Future<String> future = client.execute(request, String.class, (FutureCallback<String>) null);
            assertEquals("好", future.get());

            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            final Future<String> future2 = client.charset(StandardCharsets.ISO_8859_1, callback);
            assertEquals("abc", future2.get());
            callback.assertResult("abc");
        }, s -> s.enqueue(new MockResponse().setBody("abc")));
    }

    @Test
    void getWithoutCharset() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final Future<String> future = client.execute(request, String.class, (FutureCallback<String>) null);
            assertEquals("好", future.get());

            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            final Future<String> future2 = client.charset(StandardCharsets.ISO_8859_1, callback);
            assertEquals("abc", future2.get());
            callback.assertResult("abc");
        }, s -> s.enqueue(new MockResponse().setBody("abc").setHeader(HttpHeaders.CONTENT_TYPE, "text/plain")));
    }

    @Test
    void getNull() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<Float> callback = new MyFutureCallback<>();
            assertNull(client.execute(request, Float.class, callback).get());
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
            final ClassicHttpRequest request = client.post("/ping.html").build();
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("好", client.execute(request, String.class, callback).get());
            callback.assertResult("好");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void postJson() throws Throwable {
        useClient((server, client) -> {
            final HttpEntity entity = EntityBuilder.create().setText("{}")
                    .setContentType(ContentType.APPLICATION_JSON).setContentEncoding("UTF-8").build();
            final ClassicHttpRequest request = client.post("/ping.html").setEntity(entity).build();
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("好", client.execute(request, String.class, callback).get());
            callback.assertResult("好");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("{}", recorded.getBody().readUtf8());
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void delete() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.delete("/ping.html").build();
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("好", client.execute(request, String.class, callback).get());
            callback.assertResult("好");
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("DELETE", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void socketTimeoutException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<String> callback = new MyFutureCallback<>();
            assertEquals("好", client.execute(request, String.class, callback).get());
            callback.assertResult("好");

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
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<Integer> callback = new MyFutureCallback<>();
            final NullPointerException e = assertThrowsExactly(NullPointerException.class, () -> client.execute(request, Integer.class, callback).get());
            assertEquals("responseHandler is null", e.getMessage());
            assertFalse(callback.called);
            assertEquals(0, server.getRequestCount());
        });
    }

    @Test
    void apiResponseException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            assertEquals("好", client.execute(request, String.class, (FutureCallback<String>) null).get());

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
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<Long> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, Long.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(ClientProtocolException.class, e.getCause().getClass());
            assertEquals("not a Long value: 好", e.getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void apiException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<Boolean> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, Boolean.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            assertEquals("Unsupported", e.getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void httpException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyFutureCallback<BigInteger> callback = new MyFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, BigInteger.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(IOException.class, e.getCause().getClass());
            assertNotNull(e.getCause().getCause());
            assertEquals(HttpException.class, e.getCause().getCause().getClass());
            assertEquals("test throw HttpException", e.getCause().getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void stream() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("https://httpbin.org/stream/3").build();
            final MyStreamFutureCallback<String> callback = new MyStreamFutureCallback<>();
            final Future<Void> future = client.execute(request, String.class, callback);
            assertNull(future.get());
            callback.assertResult(future.get());
            assertEquals(3, callback.results.size());
        });
    }

    @Test
    void streamMore() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyStreamFutureCallback<String> callback = new MyStreamFutureCallback<>();
            final Future<Void> future = client.execute(request, String.class, callback);
            assertNull(future.get());
            callback.assertResult(future.get());
            assertEquals(1, callback.results.size());
            assertEquals("好", callback.results.get(0));

            final MyStreamFutureCallback<String> callback2 = new MyStreamFutureCallback<>();
            final Future<Void> future2 = client.execute(request, String.class, callback2);
            assertNull(future2.get());
            callback2.assertResult(future2.get());
            assertEquals(0, callback2.results.size());
        }, s -> s.enqueue(new MockResponse().setBody("  ")));
    }

    @Test
    void streamCancel() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyStreamFutureCallback<String> callback = new MyStreamFutureCallback<>();
            final Future<Void> future = client.execute(request, String.class, callback);
            assertTrue(future.cancel(true));
            callback.assertCancelled(true);
            assertEquals(0, callback.results.size());
        });
    }

    @Test
    void streamException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            assertEquals("好", client.execute(request, String.class, (FutureCallback<String>) null).get());

            final MyStreamFutureCallback<String> callback = new MyStreamFutureCallback<>();
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, () -> client.execute(request, String.class, callback).get());
            assertNotNull(e.getCause());
            assertEquals(HttpResponseException.class, e.getCause().getClass());
            assertEquals("status code: 404, reason phrase: Not Found", e.getCause().getMessage());
            callback.assertException(e.getCause());
            assertEquals(0, callback.results.size());
        }, s -> s.enqueue(new MockResponse().setResponseCode(404).setBody("not found")));
    }

    @Test
    void streamNull() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyStreamFutureCallback<Long> callback = new MyStreamFutureCallback<>();
            final Exception e = assertThrowsExactly(NullPointerException.class, () -> client.execute(request, Long.class, callback));
            assertEquals("streamResponseClass is null", e.getMessage());
        });
    }

    @Test
    void eventStream() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyStreamFutureCallback<EventStream> callback = new MyStreamFutureCallback<>();
            final Future<Void> future = client.execute(request, EventStream.class, callback);
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, future::get);
            assertEquals(IOException.class, e.getCause().getClass());
            assertNotNull(e.getCause().getCause());
            assertEquals(UnsupportedContentTypeException.class, e.getCause().getCause().getClass());
            final UnsupportedContentTypeException e2 = (UnsupportedContentTypeException) e.getCause().getCause();
            assertEquals("Unsupported Content-Type: text/plain; charset=UTF-8 for class java.lang.String", e2.getMessage());
            assertEquals("text/plain; charset=UTF-8", e2.getContentType().toString());
            assertEquals(String.class, e2.getContentClass());
            callback.assertException(e.getCause());

            final MyStreamFutureCallback<EventStream> callback2 = new MyStreamFutureCallback<>();
            final Future<Void> future2 = client.execute(request, EventStream.class, callback2);
            assertNull(future2.get());
            callback2.assertResult(future2.get());
            assertEquals(1, callback2.results.size());
            assertEquals("EventStream[event='test', data='abc\ndef', id='id', retry='5000']", callback2.results.get(0).toString());
        }, s -> s.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_EVENT_STREAM)
                .setBody("id: id\nevent: test\ndata: abc\ndata: def\nretry: 5000\n\nretry: abc\n\n   ")));
    }

    @Test
    void eventStreamCharset() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final MyStreamFutureCallback<EventStream> callback = new MyStreamFutureCallback<>();
            final Future<Void> future = client.execute(request, EventStream.class, callback);
            final ExecutionException e = assertThrowsExactly(ExecutionException.class, future::get);
            assertEquals(IOException.class, e.getCause().getClass());
            assertNotNull(e.getCause().getCause());
            assertEquals(UnsupportedContentTypeException.class, e.getCause().getCause().getClass());
            assertEquals("Unsupported Content-Type: text/plain; charset=UTF-8 for class java.lang.String", e.getCause().getCause().getMessage());
            callback.assertException(e.getCause());

            final MyStreamFutureCallback<EventStream> callback2 = new MyStreamFutureCallback<>();
            final Future<Void> future2 = client.execute(request, EventStream.class, callback2);
            assertNull(future2.get());
            callback2.assertResult(future2.get());
            assertEquals(1, callback2.results.size());
            assertEquals("EventStream[event='中文', data='中文', id='中文', retry='null']", callback2.results.get(0).toString());
        }, s -> s.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, ContentType.TEXT_EVENT_STREAM)
                .setBody("id: 中文\nevent: 中文\ndata: 中文")));
    }

    private static class MyClient extends ApiAsyncClient {
        public MyClient(final HttpAsyncClient httpClient, final String uriBase) {
            super(httpClient, false);
            setUriResolver(new RelativeUriResolver(uriBase));
            setCharset(StandardCharsets.UTF_8);
            setResponseCharset(StandardCharsets.UTF_8);
            setResponseHandlerFactory(new MyResponseHandlerFactory());
        }

        public Future<String> charset(final Charset charset, FutureCallback<String> callback) throws ApiException, IOException {
            setResponseCharset(charset);
            return execute(get("/ping.html").build(), String.class, callback);
        }
    }

    private static class MyResponseHandlerFactory extends AbstractResponseHandlerFactory {
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
            addResponseHandler(BigInteger.class, response -> {
                throw new HttpException("test throw HttpException");
            });
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

        public void assertCancelled(boolean cancelled) {
            waitCall();
            assertSame(cancelled, this.cancelled);
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

    private static class MyStreamFutureCallback<T> extends MyFutureCallback<Void>
            implements StreamFutureCallback<T> {
        private final List<T> results = new ArrayList<>();

        @Override
        public void stream(HttpResponse response, ContentType contentType, boolean endOfStream, T result) {
            results.add(result);
        }

        @Override
        public void completed(Void result) {
            StreamFutureCallback.super.completed(result);
            super.completed(result);
        }

        @Override
        public void failed(Exception ex) {
            StreamFutureCallback.super.failed(ex);
            super.failed(ex);
        }

        @Override
        public void cancelled() {
            StreamFutureCallback.super.cancelled();
            super.cancelled();
        }
    }
}