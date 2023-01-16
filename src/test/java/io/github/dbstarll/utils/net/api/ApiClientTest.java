package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.HttpClientFactory;
import io.github.dbstarll.utils.http.client.request.RelativeUriResolver;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * test ApiClient
 */
class ApiClientTest {
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("ok"));
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        server = null;
    }

    @org.junit.jupiter.api.Test
    void get() throws Exception {
        try (CloseableHttpClient client = new HttpClientFactory().build()) {
            final MyClient c = new MyClient(client, server.url("/").toString());
            final HttpUriRequest request = c.get("/ping.html").build();
            assertEquals("ok", c.execute(request, String.class));
        }
    }

    private static class MyClient extends ApiClient {
        public MyClient(HttpClient httpClient, String uriBase) {
            super(httpClient);
            setUriResolver(new RelativeUriResolver(uriBase));
        }
    }
}