package io.github.dbstarll.utils.net.api.index;

public interface Index<T> {
    /**
     * 获得数据.
     *
     * @return 数据
     */
    T getData();

    /**
     * 获得数据解析位置.
     *
     * @return 位置
     */
    int getIndex();
}
