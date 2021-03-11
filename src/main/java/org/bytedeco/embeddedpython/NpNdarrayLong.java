package org.bytedeco.embeddedpython;

import java.util.Arrays;

public class NpNdarrayLong extends NpNdarray {
    private static final long serialVersionUID = 1L;
    public final long[] data;

    public NpNdarrayLong(long[] data, int[] dimensions, int[] strides) {
        super(dimensions, strides);
        if (data == null) throw new NullPointerException("data = null");
        this.data = data;
    }

    /**
     * The bytes of element.
     */
    @Override
    public int itemsize() {
        return 8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NpNdarrayLong)) return false;
        if (!super.equals(o)) return false;

        NpNdarrayLong that = (NpNdarrayLong) o;

        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public long[] toArray() {
        if (ndim() != 1) throw new RuntimeException("ndim != 1");

        if (strides[0] == itemsize() && data.length == dimensions[0]) {
            return data;
        } else {
            int[] indexStrides = indexStrides();
            long[] ary = new long[dimensions[0]];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = data[i * indexStrides[0]];
            }
            return ary;
        }
    }

    public long[][] toArray2d() {
        if (ndim() != 2) throw new RuntimeException("ndim != 2");

        int[] indexStrides = indexStrides();
        long[][] ary = new long[dimensions[0]][dimensions[1]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                ary[i][j] = data[i * indexStrides[0] + j * indexStrides[1]];
            }
        }
        return ary;
    }

    public long[][][] toArray3d() {
        if (ndim() != 3) throw new RuntimeException("ndim != 3");

        int[] indexStrides = indexStrides();
        long[][][] ary = new long[dimensions[0]][dimensions[1]][dimensions[2]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    ary[i][j][k] = data[i * indexStrides[0] + j * indexStrides[1] + k * indexStrides[2]];
                }
            }
        }
        return ary;
    }
}
