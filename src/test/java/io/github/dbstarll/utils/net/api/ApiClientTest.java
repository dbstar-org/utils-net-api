package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.HttpClientFactory;
import io.github.dbstarll.utils.http.client.request.RelativeUriResolver;
import io.github.dbstarll.utils.http.client.response.AbstractResponseHandlerFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.impl.classic.AbstractHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

/**
 * test ApiClient
 */
class ApiClientTest {
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
            try (CloseableHttpClient client = new HttpClientFactory().build()) {
                consumer.accept(server, new MyClient(client, server.url("/").toString()));
            }
        }, customizers);
    }

    @Test
    void get() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            assertEquals("ok", client.execute(request, String.class));
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("GET", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void getNull() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            assertNull(client.execute(request, Float.class));
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
            assertEquals("ok", client.execute(request, String.class));
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
            assertEquals("ok", client.execute(request, String.class));
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void delete() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.delete("/ping.html").build();
            assertEquals("ok", client.execute(request, String.class));
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
            assertEquals("ok", client.execute(request, String.class));

            final IOException e = assertThrows(IOException.class, () -> client.execute(request, String.class));
            assertEquals(SocketTimeoutException.class, e.getClass());
            assertEquals("Read timed out", e.getMessage());
            assertEquals(2, server.getRequestCount());
        });
    }

    @Test
    void nullPointerException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final NullPointerException e = assertThrowsExactly(NullPointerException.class, () -> client.execute(request, Integer.class));
            assertEquals("responseHandler is null", e.getMessage());
            assertEquals(0, server.getRequestCount());
        });
    }

    @Test
    void apiResponseException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            assertEquals("ok", client.execute(request, String.class));

            final ApiResponseException e = assertThrowsExactly(ApiResponseException.class, () -> client.execute(request, String.class));
            assertEquals(404, e.getStatusCode());
            assertEquals("Client Error", e.getReasonPhrase());
            assertEquals("status code: 404, reason phrase: Client Error", e.getMessage());
            assertNotNull(e.getCause());
            assertEquals(HttpResponseException.class, e.getCause().getClass());
            assertEquals("status code: 404, reason phrase: Client Error", e.getCause().getMessage());
            assertEquals(2, server.getRequestCount());
        }, s -> s.enqueue(new MockResponse().setResponseCode(404)));
    }

    @Test
    void apiProtocolException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final ApiProtocolException e = assertThrowsExactly(ApiProtocolException.class, () -> client.execute(request, Long.class));
            assertNotNull(e.getCause());
            assertEquals(ClientProtocolException.class, e.getCause().getClass());
            assertEquals("not a Long value: ok", e.getCause().getMessage());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void apiException() throws Throwable {
        useClient((server, client) -> {
            final ClassicHttpRequest request = client.get("/ping.html").build();
            final Exception e = assertThrowsExactly(ApiException.class, () -> client.execute(request, Boolean.class));
            assertNotNull(e.getCause());
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            assertEquals("Unsupported", e.getCause().getMessage());
            assertEquals(1, server.getRequestCount());
        });
    }

    @Test
    void traceRequestNoRepeatable() throws Throwable {
        useClient((server, client) -> {
            final HttpEntity entity = EntityBuilder.create()
                    .setStream(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)))
                    .setContentType(ContentType.APPLICATION_JSON).setContentEncoding("UTF-8").build();
            final ClassicHttpRequest request = client.post("/ping.html").setEntity(entity).build();
            assertEquals("ok", client.execute(request, String.class));
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("POST", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    private static class MyClient extends ApiClient {
        public MyClient(final HttpClient httpClient, final String uriBase) {
            super(httpClient);
            setUriResolver(new RelativeUriResolver(uriBase));
            setCharset(StandardCharsets.UTF_8);
            setResponseHandlerFactory(new MyResponseHandlerFactory());
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
        }
    }
}