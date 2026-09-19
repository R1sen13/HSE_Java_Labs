public class Complex {
    private double real;
    private double imaginary;

    public Complex() {
        this.real=0;
        this.imaginary=0;
    }
    public Complex(double re, double im) {
        this.real=re;
        this.imaginary=im;
    }

    public double getReal() {
        return this.real;
    }
    public double getImaginary() {
        return this.imaginary;
    }

    public Complex add(Complex other) {
        return new Complex(
                this.real+other.real,
                this.imaginary+other.imaginary
        );
    }
    public Complex sub(Complex other) {
        return new Complex(
                this.real-other.real,
                this.imaginary-other.imaginary
        );
    }
    public Complex multiply(Complex other) {
        return new Complex(
                this.real*other.real-this.imaginary*other.imaginary,
                this.real*other.imaginary+this.imaginary*other.real
        );
    }
    public Complex divide(Complex other) {
        if (other.real==0 && other.imaginary==0) {
            throw new IllegalArgumentException("You cannot divide by zero!");
        }
        double real = (this.real*other.real+this.imaginary*other.imaginary)/(other.real* other.real + other.imaginary*other.imaginary);
        double imaginary = (this.imaginary*other.real-this.real*other.imaginary)/(other.real* other.real + other.imaginary*other.imaginary);
        return new Complex(
                real, imaginary
        );
    }
}
