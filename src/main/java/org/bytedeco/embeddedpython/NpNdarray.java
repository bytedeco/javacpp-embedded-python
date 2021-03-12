package org.bytedeco.embeddedpython;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Numpy np.ndarray.
 */
public abstract class NpNdarray implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ndarray.shape
     */
    public final int[] shape;
    /**
     * The unit of Numpy ndarray.strides is bytes, but the unit of this field is element.
     */
    public final int[] strides;

    public NpNdarray(int[] shape, int[] strides) {
        if (shape.length != strides.length)
            throw new IllegalArgumentException(
                    "shape.length = " + shape.length + ", strides.length = " + strides.length);

        this.shape = shape;
        this.strides = strides;
    }

    /**
     * The length of shape.
     */
    public int ndim() {
        return shape.length;
    }

    /**
     * The bytes of element.
     */
    public abstract int itemsize();

    long[] stridesInBytes() {
        return Arrays.stream(strides).mapToLong(s -> ((long) s) * ((long) itemsize())).toArray();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                ", shape=" + Arrays.toString(shape) +
                ", strides=" + Arrays.toString(strides) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NpNdarray)) return false;

        NpNdarray npNdarray = (NpNdarray) o;

        if (!Arrays.equals(shape, npNdarray.shape)) return false;
        return Arrays.equals(strides, npNdarray.strides);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(shape);
        result = 31 * result + Arrays.hashCode(strides);
        return result;
    }
}

