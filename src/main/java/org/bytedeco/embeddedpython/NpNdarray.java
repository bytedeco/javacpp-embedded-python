package org.bytedeco.embeddedpython;

import java.io.Serializable;
import java.util.Arrays;

public abstract class NpNdarray implements Serializable {
    public static final long serialVersionUID = 1L;

    public final int ndim;
    public final int[] dimensions;
    public final int[] strides;
    public final int itemsize;

    public NpNdarray(int[] dimensions, int[] strides, int itemsize) {
        if (dimensions.length != strides.length)
            throw new IllegalArgumentException(
                    "dimensions.length = " + dimensions.length +
                    ", strides.length = " + strides.length);
        if (itemsize <= 0)
            throw new IllegalArgumentException("itemsize = " + itemsize);

        this.ndim = dimensions.length;
        this.dimensions = dimensions;
        this.strides = strides;
        this.itemsize = itemsize;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "ndim=" + ndim +
                ", dimensions=" + Arrays.toString(dimensions) +
                ", strides=" + Arrays.toString(strides) +
                ", itemsize=" + itemsize +
                '}';
    }

    int[] indexStrides() {
        return Arrays.stream(strides).map(s -> s / itemsize).toArray();
    }
}

