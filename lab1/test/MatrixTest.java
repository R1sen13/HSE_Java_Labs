import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MatrixTest {

    Matrix createSecondMatrix() {
        Matrix matrix = new Matrix(2);

        matrix.set(0, 0, new Complex(-1, 0));
        matrix.set(0, 1, new Complex(-2, 0));
        matrix.set(1, 0, new Complex(-3, 0));
        matrix.set(1, 1, new Complex(-4, 0));

        return matrix;
    }

    Matrix createFirstMatrix() {
        Matrix matrix = new Matrix(2);

        matrix.set(0, 0, new Complex(1, 0));
        matrix.set(0, 1, new Complex(2, 0));
        matrix.set(1, 0, new Complex(3, 0));
        matrix.set(1, 1, new Complex(4, 0));

        return matrix;
    }

    void assertMatrixEquals(Matrix expected, Matrix actual) {
        assertEquals(expected.getSize(), actual.getSize());

        for (int i = 0; i < expected.getSize(); i++) {
            for (int j = 0; j < expected.getSize(); j++) {
                assertEquals(
                        expected.get(i, j).getReal(),
                        actual.get(i, j).getReal()
                );

                assertEquals(
                        expected.get(i, j).getImaginary(),
                        actual.get(i, j).getImaginary()
                );
            }
        }
    }

    void assertComplexEquals(Complex expected, Complex actual) {
        assertEquals(expected.getReal(), actual.getReal());
        assertEquals(expected.getImaginary(), actual.getImaginary());
    }

    @Test
    void testDeterminant() {
        Matrix matrix = createFirstMatrix();
        assertEquals(-2, matrix.determinant().getReal());
        assertEquals(0, matrix.determinant().getImaginary());
    }

    @Test
    void testDeterminantSizeOne() {
        Matrix matrix = new Matrix(1);
        matrix.set(0,0, new Complex(1,0));
        assertEquals(1, matrix.determinant().getReal());
        assertEquals(0, matrix.determinant().getImaginary());
    }

    @Test
    void testDeterminantSizeThree() {
        Matrix matrix = new Matrix(3);
        matrix.set(0,0, new Complex(1,0));
        matrix.set(0,1, new Complex(2,0));
        matrix.set(0,2, new Complex(3,0));
        matrix.set(1,0, new Complex(0,0));
        matrix.set(1,1, new Complex(1,0));
        matrix.set(1,2, new Complex(4,0));
        matrix.set(2,0, new Complex(5,0));
        matrix.set(2,1, new Complex(6,0));
        matrix.set(2,2, new Complex(0,0));
        assertComplexEquals(new Complex(1,0), matrix.determinant());
    }

    @Test
    void addMatrix() {
        Matrix matrix1 = createFirstMatrix();
        Matrix matrix2 = createSecondMatrix();

        Matrix result = matrix1.add(matrix2);
        Matrix expected = new Matrix(2);
        expected.set(0,0,new Complex(0,0));
        expected.set(0,1,new Complex(0,0));
        expected.set(1,0,new Complex(0,0));
        expected.set(1,1,new Complex(0,0));


        assertMatrixEquals(expected, result);
    }

    @Test
    void subMatrix() {
        Matrix matrix1 = createFirstMatrix();
        Matrix matrix2 = createSecondMatrix();

        Matrix result = matrix1.sub(matrix2);
        Matrix expected = new Matrix(2);
        expected.set(0,0,new Complex(2,0));
        expected.set(0,1,new Complex(4,0));
        expected.set(1,0,new Complex(6,0));
        expected.set(1,1,new Complex(8,0));

        assertMatrixEquals(expected, result);
    }

    @Test
    void multiplyMatrix() {
        Matrix matrix1 = createFirstMatrix();
        Matrix matrix2 = createSecondMatrix();

        Matrix result = matrix1.multiply(matrix2);
        Matrix expected = new Matrix(2);
        expected.set(0,0,new Complex(-7,0));
        expected.set(0,1,new Complex(-10,0));
        expected.set(1,0,new Complex(-15,0));
        expected.set(1,1,new Complex(-22,0));

        assertMatrixEquals(expected, result);
    }

    @Test
    void divideMatrix() {
        Matrix matrix1 = createFirstMatrix();
        Matrix matrix2 = createSecondMatrix();

        Matrix result = matrix1.divide(matrix2);
        Matrix expected = new Matrix(2);
        expected.set(0,0,new Complex(-1,0));
        expected.set(0,1,new Complex(0,0));
        expected.set(1,0,new Complex(0,0));
        expected.set(1,1,new Complex(-1,0));

        assertMatrixEquals(expected, result);
    }

    @Test
    void divideMatrixError() {
        Matrix matrix1 = createFirstMatrix();
        Matrix matrix2 = new Matrix(2);
        matrix2.set(0,0, new Complex(1,0));
        matrix2.set(0,1, new Complex(2,0));
        matrix2.set(1,0, new Complex(2,0));
        matrix2.set(1,1, new Complex(4,0));

        assertThrows(IllegalArgumentException.class, () -> {
            matrix1.divide(matrix2);
        });
    }

    @Test
    void transposeMatrix() {
        Matrix matrix1 = createFirstMatrix();

        Matrix result = matrix1.transpose();
        Matrix expected = new Matrix(2);
        expected.set(0,0,new Complex(1,0));
        expected.set(0,1,new Complex(3,0));
        expected.set(1,0,new Complex(2,0));
        expected.set(1,1,new Complex(4,0));

        assertMatrixEquals(expected, result);
    }

    @Test
    void addComplex() {
        Complex complex1 = new Complex(1, 1);
        Complex complex2 = new Complex(1, 1);
        Complex result = complex1.add(complex2);

        Complex expected = new Complex(2,2);
        assertComplexEquals(expected, result);
    }

    @Test
    void subComplex() {
        Complex complex1 = new Complex(1, 1);
        Complex complex2 = new Complex(1, 1);
        Complex result = complex1.sub(complex2);

        Complex expected = new Complex(0,0);
        assertComplexEquals(expected, result);
    }

    @Test
    void multiplyComplex() {
        Complex complex1 = new Complex(1, 1);
        Complex complex2 = new Complex(1, 1);
        Complex result = complex1.multiply(complex2);

        Complex expected = new Complex(0,2);
        assertComplexEquals(expected, result);
    }

    @Test
    void divideComplex() {
        Complex complex1 = new Complex(1, 1);
        Complex complex2 = new Complex(1, 1);
        Complex result = complex1.divide(complex2);

        Complex expected = new Complex(1,0);
        assertComplexEquals(expected, result);
    }

    @Test
    void divideComplexError() {
        Complex complex1 = new Complex(1, 1);
        Complex complex2 = new Complex(0, 0);

        assertThrows(IllegalArgumentException.class, () -> {
            complex1.divide(complex2);
        });
    }
}