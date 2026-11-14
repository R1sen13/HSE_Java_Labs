import java.util.*;

public class Condition {
    public enum Operator { EQ, NE, GT, LT, GE, LE }

    private String column;
    private Operator operator;
    private Object value;

    public Condition() {}

    public Condition(String column, Operator operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public String getColumn() { return column; }
    public Operator getOperator() { return operator; }
    public Object getValue() { return value; }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean matches(Row row) {
        Object actual = row.get(column);
        if (actual == null || value == null) {
            boolean equal = Objects.equals(actual, value);
            return operator == Operator.EQ ? equal :
                   operator == Operator.NE && !equal;
        }

        int cmp;
        if (actual instanceof Number && value instanceof Number) {
            cmp = Double.compare(((Number) actual).doubleValue(),
                                 ((Number) value).doubleValue());
        } else if (actual instanceof Comparable && actual.getClass().isInstance(value)) {
            cmp = ((Comparable) actual).compareTo(value);
        } else {
            cmp = actual.toString().compareTo(value.toString());
        }

        switch (operator) {
            case EQ: return cmp == 0;
            case NE: return cmp != 0;
            case GT: return cmp > 0;
            case LT: return cmp < 0;
            case GE: return cmp >= 0;
            case LE: return cmp <= 0;
            default: return false;
        }
    }
}
