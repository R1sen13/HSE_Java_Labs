public class Matrix {
    private Complex[][] data;
    private int size;

    public Matrix(int size) {
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


}
