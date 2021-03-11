package org.bytedeco.embeddedpython;

import java.util.Arrays;

public class NpNdarrayByte extends NpNdarray {
    private static final long serialVersionUID = 1L;
    public final byte[] data;

    public NpNdarrayByte(byte[] data, int[] dimensions, int[] strides) {
        super(dimensions, strides);
        if (data == null) throw new NullPointerException("data = null");
        this.data = data;
    }

    /**
     * The bytes of element.
     */
    @Override
    public int itemsize() {
        return 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NpNdarrayByte)) return false;
        if (!super.equals(o)) return false;

        NpNdarrayByte that = (NpNdarrayByte) o;

        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public byte[] toArray() {
        if (ndim() != 1) throw new RuntimeException("ndim != 1");

        if (strides[0] == 1 && data.length == dimensions[0]) {
            return data;
        } else {
            int[] strides = this.strides;
            byte[] ary = new byte[dimensions[0]];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = data[i * strides[0]];
            }
            return ary;
        }
    }

    public byte[][] toArray2d() {
        if (ndim() != 2) throw new RuntimeException("ndim != 2");

        int[] strides = this.strides;
        byte[][] ary = new byte[dimensions[0]][dimensions[1]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                ary[i][j] = data[i * strides[0] + j * strides[1]];
            }
        }
        return ary;
    }

    public byte[][][] toArray3d() {
        if (ndim() != 3) throw new RuntimeException("ndim != 3");

        int[] strides = this.strides;
        byte[][][] ary = new byte[dimensions[0]][dimensions[1]][dimensions[2]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    ary[i][j][k] = data[i * strides[0] + j * strides[1] + k * strides[2]];
                }
            }
        }
        return ary;
    }
}
