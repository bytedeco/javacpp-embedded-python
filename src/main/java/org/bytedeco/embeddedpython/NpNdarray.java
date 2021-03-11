package org.bytedeco.embeddedpython;

import java.io.Serializable;
import java.util.Arrays;

public abstract class NpNdarray implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int[] dimensions;
    public final int[] strides;

    public NpNdarray(int[] dimensions, int[] strides) {
        if (dimensions.length != strides.length)
            throw new IllegalArgumentException(
                    "dimensions.length = " + dimensions.length +
                    ", strides.length = " + strides.length);

        this.dimensions = dimensions;
        this.strides = strides;
    }

    /**
     * The length of dimensions.
     */
    public int ndim() {
        return dimensions.length;
    }

    /**
     * The bytes of element.
     */
    public abstract int itemsize();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                ", dimensions=" + Arrays.toString(dimensions) +
                ", strides=" + Arrays.toString(strides) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NpNdarray)) return false;

        NpNdarray npNdarray = (NpNdarray) o;

        if (!Arrays.equals(dimensions, npNdarray.dimensions)) return false;
        return Arrays.equals(strides, npNdarray.strides);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(dimensions);
        result = 31 * result + Arrays.hashCode(strides);
        return result;
    }

    int[] indexStrides() {
        return Arrays.stream(strides).map(s -> s / itemsize()).toArray();
    }
}

