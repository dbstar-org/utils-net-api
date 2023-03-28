package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.concurrent.FutureCallback;

public interface StreamFutureCallback<T> extends FutureCallback<Void>, StreamCallback<T> {
}
