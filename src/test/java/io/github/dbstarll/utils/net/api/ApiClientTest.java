package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.HttpClientFactory;
import io.github.dbstarll.utils.http.client.request.RelativeUriResolver;
import io.github.dbstarll.utils.http.client.response.BasicResponseHandlerFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
            final HttpUriRequest request = client.get("/ping.html").build();
            assertEquals("ok", client.execute(request, String.class));
            assertEquals(1, server.getRequestCount());
            final RecordedRequest recorded = server.takeRequest();
            assertEquals("GET", recorded.getMethod());
            assertEquals("/ping.html", recorded.getPath());
        });
    }

    @Test
    void post() throws Throwable {
        useClient((server, client) -> {
            final HttpUriRequest request = client.post("/ping.html").build();
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
            final HttpUriRequest request = client.delete("/ping.html").build();
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
            final HttpUriRequest request = client.get("/ping.html").build();
            assertEquals("ok", client.execute(request, String.class));

            final IOException e = assertThrows(IOException.class, () -> client.execute(request, String.class));
            assertEquals(SocketTimeoutException.class, e.getClass());
            assertEquals("Read timed out", e.getMessage());
            assertEquals(2, server.getRequestCount());
        });
    }

    @Test
    void apiParameterException() throws Throwable {
        useClient((server, client) -> {
            final HttpUriRequest request = client.get("/ping.html").build();
            final Exception e = assertThrowsExactly(ApiParameterException.class, () -> client.execute(request, Integer.class));
            assertNotNull(e.getCause());
            assertEquals(NullPointerException.class, e.getCause().getClass());
            assertEquals("responseHandler is null", e.getCause().getMessage());
            assertEquals(0, server.getRequestCount());
        });
    }

    @Test
    void apiResponseException() throws Throwable {
        useClient((server, client) -> {
            final HttpUriRequest request = client.get("/ping.html").build();
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
            final HttpUriRequest request = client.get("/ping.html").build();
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
            final HttpUriRequest request = client.get("/ping.html").build();
            final Exception e = assertThrowsExactly(ApiException.class, () -> client.execute(request, Boolean.class));
            assertNotNull(e.getCause());
            assertEquals(UnsupportedOperationException.class, e.getCause().getClass());
            assertEquals("Unsupported", e.getCause().getMessage());
            assertEquals(1, server.getRequestCount());
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

    private static class MyResponseHandlerFactory extends BasicResponseHandlerFactory {
        public MyResponseHandlerFactory() {
            addResponseHandler(Long.class, new AbstractResponseHandler<Long>() {
                @Override
                public Long handleEntity(HttpEntity entity) throws IOException {
                    final String value = EntityUtils.toString(entity);
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
        }
    }
}