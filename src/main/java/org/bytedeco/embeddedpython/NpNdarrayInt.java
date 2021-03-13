package org.bytedeco.embeddedpython;

import java.util.Arrays;

public class NpNdarrayInt extends NpNdarray {
    private static final long serialVersionUID = 1L;
    public final int[] data;

    public NpNdarrayInt(int[] data, int[] shape, int[] strides) {
        super(shape, strides);
        if (data == null) throw new NullPointerException("data = null");
        this.data = data;
    }

    public NpNdarrayInt(int[] ary) {
        super(new int[]{ary.length});
        this.data = ary.clone();
    }

    public NpNdarrayInt(int[][] ary) {
        super(new int[]{ary.length, ary[0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                data[i * strides[0] + j * strides[1]] = ary[i][j];
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    data[i * strides[0] + j * strides[1] + k * strides[2]] = ary[i][j][k];
                }
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length, ary[0][0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    for (int l = 0; l < shape[3]; l++) {
                        data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3]] = ary[i][j][k][l];
                    }
                }
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length, ary[0][0][0].length, ary[0][0][0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    for (int l = 0; l < shape[3]; l++) {
                        for (int m = 0; m < shape[4]; m++) {
                            data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4]] = ary[i][j][k][l][m];
                        }
                    }
                }
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][][][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length, ary[0][0][0].length, ary[0][0][0][0].length, ary[0][0][0][0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    for (int l = 0; l < shape[3]; l++) {
                        for (int m = 0; m < shape[4]; m++) {
                            for (int n = 0; n < shape[5]; n++) {
                                data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5]] = ary[i][j][k][l][m][n];
                            }
                        }
                    }
                }
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][][][][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length, ary[0][0][0].length, ary[0][0][0][0].length, ary[0][0][0][0][0].length, ary[0][0][0][0][0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    for (int l = 0; l < shape[3]; l++) {
                        for (int m = 0; m < shape[4]; m++) {
                            for (int n = 0; n < shape[5]; n++) {
                                for (int o = 0; o < shape[6]; o++) {
                                    data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5] + o * strides[6]] = ary[i][j][k][l][m][n][o];
                                }
                            }
                        }
                    }
                }
            }
        }
        this.data = data;
    }

    public NpNdarrayInt(int[][][][][][][][] ary) {
        super(new int[]{ary.length, ary[0].length, ary[0][0].length, ary[0][0][0].length, ary[0][0][0][0].length, ary[0][0][0][0][0].length, ary[0][0][0][0][0][0].length, ary[0][0][0][0][0][0][0].length});
        int[] shape = this.shape;
        int[] strides = this.strides;
        int[] data = new int[intAryProduct(shape)];
        for (int i = 0; i < shape[0]; i++) {
            for (int j = 0; j < shape[1]; j++) {
                for (int k = 0; k < shape[2]; k++) {
                    for (int l = 0; l < shape[3]; l++) {
                        for (int m = 0; m < shape[4]; m++) {
                            for (int n = 0; n < shape[5]; n++) {
                                for (int o = 0; o < shape[6]; o++) {
                                    for (int p = 0; p < shape[7]; p++) {
                                        data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5] + o * strides[6] + p * strides[7]] = ary[i][j][k][l][m][n][o][p];
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        this.data = data;
    }

    @Override
    public int itemsize() {
        return 4;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NpNdarrayInt)) return false;
        if (!super.equals(o)) return false;

        NpNdarrayInt that = (NpNdarrayInt) o;

        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    public int[] toArray() {
        if (ndim() != 1) throw new RuntimeException("ndim != 1");

        if (strides[0] == 1 && data.length == shape[0]) {
            return data;
        } else {
            int[] strides = this.strides;
            int[] ary = new int[shape[0]];
            for (int i = 0; i < ary.length; i++) {
                ary[i] = data[i * strides[0]];
            }
            return ary;
        }
    }

    public int[][] toArray2d() {
        if (ndim() != 2) throw new RuntimeException("ndim != 2");

        int[] strides = this.strides;
        int[][] ary = new int[shape[0]][shape[1]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                ary[i][j] = data[i * strides[0] + j * strides[1]];
            }
        }
        return ary;
    }

    public int[][][] toArray3d() {
        if (ndim() != 3) throw new RuntimeException("ndim != 3");

        int[] strides = this.strides;
        int[][][] ary = new int[shape[0]][shape[1]][shape[2]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    ary[i][j][k] = data[i * strides[0] + j * strides[1] + k * strides[2]];
                }
            }
        }
        return ary;
    }

    public int[][][][] toArray4d() {
        if (ndim() != 4) throw new RuntimeException("ndim != 4");

        int[] strides = this.strides;
        int[][][][] ary = new int[shape[0]][shape[1]][shape[2]][shape[3]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    for (int l = 0; l < ary[i][j][k].length; l++) {
                        ary[i][j][k][l] = data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3]];
                    }
                }
            }
        }
        return ary;
    }

    public int[][][][][] toArray5d() {
        if (ndim() != 5) throw new RuntimeException("ndim != 5");

        int[] strides = this.strides;
        int[][][][][] ary = new int[shape[0]][shape[1]][shape[2]][shape[3]][shape[4]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    for (int l = 0; l < ary[i][j][k].length; l++) {
                        for (int m = 0; m < ary[i][j][k][l].length; m++) {
                            ary[i][j][k][l][m] = data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4]];
                        }
                    }
                }
            }
        }
        return ary;
    }

    public int[][][][][][] toArray6d() {
        if (ndim() != 6) throw new RuntimeException("ndim != 6");

        int[] strides = this.strides;
        int[][][][][][] ary = new int[shape[0]][shape[1]][shape[2]][shape[3]][shape[4]][shape[5]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    for (int l = 0; l < ary[i][j][k].length; l++) {
                        for (int m = 0; m < ary[i][j][k][l].length; m++) {
                            for (int n = 0; n < ary[i][j][k][l][m].length; n++) {
                                ary[i][j][k][l][m][n] = data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5]];
                            }
                        }
                    }
                }
            }
        }
        return ary;
    }

    public int[][][][][][][] toArray7d() {
        if (ndim() != 7) throw new RuntimeException("ndim != 7");

        int[] strides = this.strides;
        int[][][][][][][] ary = new int[shape[0]][shape[1]][shape[2]][shape[3]][shape[4]][shape[5]][shape[6]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    for (int l = 0; l < ary[i][j][k].length; l++) {
                        for (int m = 0; m < ary[i][j][k][l].length; m++) {
                            for (int n = 0; n < ary[i][j][k][l][m].length; n++) {
                                for (int o = 0; o < ary[i][j][k][l][m][n].length; o++) {
                                    ary[i][j][k][l][m][n][o] = data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5] + o * strides[6]];
                                }
                            }
                        }
                    }
                }
            }
        }
        return ary;
    }

    public int[][][][][][][][] toArray8d() {
        if (ndim() != 8) throw new RuntimeException("ndim != 8");

        int[] strides = this.strides;
        int[][][][][][][][] ary = new int[shape[0]][shape[1]][shape[2]][shape[3]][shape[4]][shape[5]][shape[6]][shape[7]];
        for (int i = 0; i < ary.length; i++) {
            for (int j = 0; j < ary[i].length; j++) {
                for (int k = 0; k < ary[i][j].length; k++) {
                    for (int l = 0; l < ary[i][j][k].length; l++) {
                        for (int m = 0; m < ary[i][j][k][l].length; m++) {
                            for (int n = 0; n < ary[i][j][k][l][m].length; n++) {
                                for (int o = 0; o < ary[i][j][k][l][m][n].length; o++) {
                                    for (int p = 0; p < ary[i][j][k][l][m][n][o].length; p++) {
                                        ary[i][j][k][l][m][n][o][p] = data[i * strides[0] + j * strides[1] + k * strides[2] + l * strides[3] + m * strides[4] + n * strides[5] + o * strides[6] + p * strides[7]];
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return ary;
    }
}
