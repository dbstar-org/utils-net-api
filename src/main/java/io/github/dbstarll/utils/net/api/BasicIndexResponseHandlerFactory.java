package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.response.AbstractResponseHandlerFactory;
import io.github.dbstarll.utils.net.api.index.EventStreamIndex;
import io.github.dbstarll.utils.net.api.index.EventStreamIndexResponseHandler;
import io.github.dbstarll.utils.net.api.index.StringIndex;
import io.github.dbstarll.utils.net.api.index.StringIndexResponseHandler;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class BasicIndexResponseHandlerFactory extends AbstractResponseHandlerFactory {
    /**
     * 构造BasicIndexResponseHandlerFactory.
     *
     * @param stringResponseHandler ResponseHandler for String
     */
    public BasicIndexResponseHandlerFactory(final HttpClientResponseHandler<String> stringResponseHandler) {
        addResponseHandler(StringIndex.class, new StringIndexResponseHandler(stringResponseHandler));
        addResponseHandler(EventStreamIndex.class, new EventStreamIndexResponseHandler(stringResponseHandler));
    }
}
