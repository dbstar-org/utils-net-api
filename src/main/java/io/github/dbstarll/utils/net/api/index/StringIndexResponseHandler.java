package io.github.dbstarll.utils.net.api.index;

import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;

public final class StringIndexResponseHandler implements HttpClientResponseHandler<StringIndex> {
    private static final HttpClientResponseHandler<String> BASIC_HANDLER = new BasicHttpClientResponseHandler();

    @Override
    public StringIndex handleResponse(final ClassicHttpResponse response) throws HttpException, IOException {
        final String data = BASIC_HANDLER.handleResponse(response);
        if (data == null) {
            return new StringIndex(null, -1);
        }
        final int index = data.indexOf('\n');
        if (index < 0) {
            return new StringIndex(data, index);
        } else {
            return new StringIndex(data.substring(0, index), index + 1);
        }
    }
}
