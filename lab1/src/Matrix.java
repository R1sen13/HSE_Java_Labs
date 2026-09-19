public class Matrix {
    private Complex[][] data;
    private final int size;

    public Matrix(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive integer!");
        }
        this.size=size;
        this.data = new Complex[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                this.data[i][j] = new Complex();
            }
        }
    }

    public Complex get(int row, int column) {
        return data[row][column];
    }
    public void set(int row, int column, Complex value) {
        data[row][column] = value;
    }

    public Matrix transpose() {
        Matrix result = new Matrix(size);

        for (int i = 0; i<size; i++) {
            for (int j = 0; j<size; j++) {
                result.data[i][j]=this.data[j][i];
            }
        }

        return result;
    }

    public Matrix minor(int excludedRow, int excludedColumn) {
        Matrix result = new Matrix(size - 1);

        int resultRow = 0;
        int resultColumn = 0;

        for (int i = 0; i < size; i++) {

            if (i == excludedRow) {
                continue;
            }

            for (int j = 0; j < size; j++) {

                if (j == excludedColumn) {
                    continue;
                }

                result.data[resultRow][resultColumn]=this.data[i][j];

                resultColumn++;
            }

            resultRow++;
            resultColumn = 0;
        }

        return result;
    }

    public Complex determinant() {
        Complex result = new Complex();
        if (size == 1) {
            return this.data[0][0];
        }

        else if (size == 2) {
            return this.data[0][0].multiply(this.data[1][1]).sub(this.data[0][1].multiply(this.data[1][0]));
        }
        else {
            for (int j = 0; j<size; j++) {
                if (j%2==0) {
                    result = result.add(this.data[0][j].multiply(this.minor(0,j).determinant()));
                }
                else {
                    result = result.sub(this.data[0][j].multiply(this.minor(0,j).determinant()));
                }
            }
        }
        return result;
    }

    public Matrix add(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Matrix result = new Matrix(size);

        for (int i = 0; i<size; i++) {
            for (int j = 0; j<size; j++) {
                result.data[i][j]=this.data[i][j].add(other.data[i][j]);
            }
        }
        return result;
    }

    public Matrix sub(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Matrix result = new Matrix(size);

        for (int i = 0; i<size; i++) {
            for (int j = 0; j<size; j++) {
                result.data[i][j]=this.data[i][j].sub(other.data[i][j]);
            }
        }
        return result;
    }

    public Matrix multiply(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Matrix result = new Matrix(size);

        for (int i = 0; i<size; i++) {
            for (int j = 0; j<size; j++) {
                for (int k = 0; k<size; k++) {
                    result.data[i][j]=result.data[i][j].add(
                            this.data[i][k].multiply(other.data[k][j])
                    );
                }
            }
        }
        return result;
    }
}
