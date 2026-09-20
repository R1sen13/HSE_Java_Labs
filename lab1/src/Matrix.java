public class Matrix {
    private final Complex[][] data;
    private final int size;

    public Matrix(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive integer!");
        }
        this.size = size;
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

    public int getSize() {
        return this.size;
    }

    public void set(int row, int column, Complex value) {
        data[row][column] = value;
    }

    public Matrix transpose() {
        Matrix result = new Matrix(size);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result.data[i][j] = this.data[j][i];
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

                result.data[resultRow][resultColumn] = this.data[i][j];

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
        } else if (size == 2) {
            return this.data[0][0].multiply(this.data[1][1]).sub(this.data[0][1].multiply(this.data[1][0]));
        } else {
            for (int j = 0; j < size; j++) {
                if (j % 2 == 0) {
                    result = result.add(this.data[0][j].multiply(this.minor(0, j).determinant()));
                } else {
                    result = result.sub(this.data[0][j].multiply(this.minor(0, j).determinant()));
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

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result.data[i][j] = this.data[i][j].add(other.data[i][j]);
            }
        }
        return result;
    }

    public Matrix sub(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Matrix result = new Matrix(size);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result.data[i][j] = this.data[i][j].sub(other.data[i][j]);
            }
        }
        return result;
    }

    public Matrix multiply(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Matrix result = new Matrix(size);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    result.data[i][j] = result.data[i][j].add(
                            this.data[i][k].multiply(other.data[k][j])
                    );
                }
            }
        }
        return result;
    }

    public Matrix divide(Complex number) {
        Matrix result = new Matrix(size);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result.data[i][j] = this.data[i][j].divide(number);
            }
        }

        return result;
    }

    public Matrix divide(Matrix other) {
        if (this.size != other.size) {
            throw new IllegalArgumentException("Matrix sizes must be equal");
        }

        Complex determinant = other.determinant();
        if (determinant.getReal() == 0 && determinant.getImaginary() == 0) {
            throw new IllegalArgumentException("Determinant can not be 0!");
        }

        if (size == 1) {
            Matrix result = new Matrix(1);
            Complex value = this.data[0][0].multiply(new Complex(1, 0).divide(other.data[0][0]));
            result.set(0, 0, value);
            return result;
        }

        Matrix temp = new Matrix(size);

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                Complex minorDeterminant = other.minor(i, j).determinant();

                if ((i + j) % 2 == 0) {
                    temp.data[i][j] = minorDeterminant;
                } else {
                    temp.data[i][j] = new Complex(0, 0).sub(minorDeterminant);
                }
            }
        }

        Matrix result = temp.transpose().divide(determinant);

        return this.multiply(result);
    }

    @Override
    public String toString() {
        String result = "";
        int[] columnWidths = new int[size];
        for (int j = 0; j < size; j++) {
            int maxLength = 0;
            for (int i = 0; i < size; i++) {
                int length = this.data[i][j].toString().length();
                if (length > maxLength) {
                    maxLength = length;
                }
            }
            columnWidths[j] = maxLength;
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                String value = this.data[i][j].toString();
                String formattedValue = String.format("%-" + columnWidths[j] + "s", value);
                if (size == 1) {
                    result += "[ " + formattedValue + " ]";
                } else if (j == 0) {
                    result += "[ " + formattedValue + " ";
                } else if (j == size - 1) {
                    result += formattedValue + " ]\n";
                } else {
                    result += formattedValue + " ";
                }
            }
        }
        return result;
    }
}
