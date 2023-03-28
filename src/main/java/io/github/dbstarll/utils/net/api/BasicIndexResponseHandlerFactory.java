package io.github.dbstarll.utils.net.api;

import io.github.dbstarll.utils.http.client.response.AbstractResponseHandlerFactory;
import io.github.dbstarll.utils.net.api.index.EventStreamIndex;
import io.github.dbstarll.utils.net.api.index.EventStreamIndexResponseHandler;
import io.github.dbstarll.utils.net.api.index.StringIndex;
import io.github.dbstarll.utils.net.api.index.StringIndexResponseHandler;

public class BasicIndexResponseHandlerFactory extends AbstractResponseHandlerFactory {
    /**
     * 构造IndexResponseHandlerFactory.
     */
    public BasicIndexResponseHandlerFactory() {
        addResponseHandler(StringIndex.class, new StringIndexResponseHandler());
        addResponseHandler(EventStreamIndex.class, new EventStreamIndexResponseHandler());
    }
}
