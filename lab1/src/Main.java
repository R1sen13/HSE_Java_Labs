import java.util.Scanner;

public class Main {
    static boolean flag = true;
    static Scanner scanner = new Scanner(System.in);

    static void main(String[] args) {
        System.out.print("What is the size of matrices > ");
        int size = scanner.nextInt();
        Matrix matrix1 = new Matrix(size);
        Matrix matrix2 = new Matrix(size);
        waitForEnter();
        clearScreen();
        while (flag) {
            try {
                options(matrix1, matrix2);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
            waitForEnter();
            clearScreen();
        }
    }

    static void waitForEnter() {
        scanner.nextLine();
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }

    static void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }

    static Complex createComplex() {
        System.out.print("Input real part of complex: ");
        double real = scanner.nextDouble();

        System.out.print("Input imaginary part of complex: ");
        double imaginary = scanner.nextDouble();

        return new Complex(real, imaginary);
    }

    static void rebuildMatrix(Matrix matrix) {
        int size = matrix.getSize();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.println("Row: " + i + ". Column: " + j);
                matrix.set(i, j, createComplex());
            }
        }
    }

    static void changeMatrixValue(Matrix matrix) {
        int size = matrix.getSize();
        System.out.println("Matrix size is " + size);
        System.out.print("Input row number (0,...): ");
        int row = scanner.nextInt();

        if (row < 0 || row >= size) {
            throw new IllegalArgumentException("Invalid row number");
        }

        System.out.print("Input column number (0,...): ");
        int column = scanner.nextInt();

        if (column < 0 || column >= size) {
            throw new IllegalArgumentException("Invalid column number");
        }

        matrix.set(row, column, createComplex());
    }

    static Matrix optionMatrix(Matrix matrix1, Matrix matrix2) {
        System.out.print("Which matrix do you want to use? 1 or 2? >");
        int innerChoice = scanner.nextInt();
        if (innerChoice == 1) {
            return matrix1;
        } else if (innerChoice == 2) {
            return matrix2;
        } else {
            throw new IllegalArgumentException("There is no matrix " + String.valueOf(innerChoice));
        }
    }

    static void options(Matrix matrix1, Matrix matrix2) {
        System.out.println("""
                
                ===== Matrix Calculator =====
                
                1. Create matrix
                2. Change matrix element
                3. Add matrices
                4. Subtract matrices
                5. Multiply matrices
                6. Divide matrices
                7. Transpose matrix
                8. Calculate determinant
                9. Display matrix
                0. Exit
                
                Choose an option:
                """);
        int choice = scanner.nextInt();
        Matrix matrix;
        switch (choice) {
            case 1:
                matrix = optionMatrix(matrix1, matrix2);
                rebuildMatrix(matrix);
                break;

            case 2:
                matrix = optionMatrix(matrix1, matrix2);
                changeMatrixValue(matrix);
                break;

            case 3:
                System.out.println(matrix1);
                System.out.println("+");
                System.out.println(matrix2);
                System.out.println("=");
                System.out.println(matrix1.add(matrix2));
                break;

            case 4:
                System.out.println(matrix1);
                System.out.println("-");
                System.out.println(matrix2);
                System.out.println("=");
                System.out.println(matrix1.sub(matrix2));
                break;

            case 5:
                System.out.println(matrix1);
                System.out.println("*");
                System.out.println(matrix2);
                System.out.println("=");
                System.out.println(matrix1.multiply(matrix2));
                break;

            case 6:
                System.out.println(matrix1);
                System.out.println("/");
                System.out.println(matrix2);
                System.out.println("=");
                System.out.println(matrix1.divide(matrix2));
                break;

            case 7:
                matrix = optionMatrix(matrix1, matrix2);
                System.out.println("Transposed: ");
                System.out.println(matrix.transpose());
                break;

            case 8:
                matrix = optionMatrix(matrix1, matrix2);
                System.out.print("Determinant: ");
                System.out.println(matrix.determinant());
                break;

            case 9:
                matrix = optionMatrix(matrix1, matrix2);
                System.out.println(matrix);
                break;

            case 0:
                flag = false;
                break;

            default:
                System.out.println("Invalid option!");
        }
    }
}
