import java.time.LocalDate;

/** Поддерживаемые типы данных и их преобразование из SQL-значений. */
public enum DataType {
    INTEGER, DOUBLE, STRING, DATE, BOOLEAN;

    public Object convert(Object value) {
        if (value == null) return null;
        try {
            switch (this) {
                case INTEGER:
                    if (value instanceof Number) {
                        double number = ((Number) value).doubleValue();
                        if (!Double.isFinite(number) || number != Math.rint(number) ||
                            number < Integer.MIN_VALUE || number > Integer.MAX_VALUE)
                            throw new IllegalArgumentException("Invalid integer: " + value);
                        return ((Number) value).intValue();
                    }
                    return Integer.parseInt(value.toString());
                case DOUBLE:
                    double number = Double.parseDouble(value.toString());
                    if (!Double.isFinite(number)) throw new IllegalArgumentException("Invalid double: " + value);
                    return number;
                case STRING:
                    return value.toString();
                case DATE:
                    if (value instanceof LocalDate) return value;
                    return LocalDate.parse(value.toString());
                case BOOLEAN:
                    if (value instanceof Boolean) return value;
                    if ("true".equalsIgnoreCase(value.toString())) return true;
                    if ("false".equalsIgnoreCase(value.toString())) return false;
                    throw new IllegalArgumentException("Invalid boolean: " + value);
                default:
                    throw new IllegalArgumentException("Unsupported type");
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cannot convert '" + value + "' to " + this, e);
        }
    }
}
