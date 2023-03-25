package io.github.dbstarll.utils.net.api.index;

import java.util.StringJoiner;

public abstract class AbstractIndex<T> implements Index<T> {
    private final T data;
    private final int index;

    protected AbstractIndex(final T data, final int index) {
        this.data = data;
        this.index = index;
    }

    @Override
    public final T getData() {
        return data;
    }

    @Override
    public final int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("index=" + getIndex())
                .add("data=" + getData())
                .toString();
    }
}
