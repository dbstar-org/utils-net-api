package io.github.dbstarll.utils.net.api;

import org.apache.hc.core5.concurrent.FutureCallback;

import java.util.List;

public interface StreamFutureCallback<T> extends FutureCallback<List<T>>, StreamCallback<T> {
}
