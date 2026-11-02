package com.team.dbms.core;

import java.time.LocalDate;

public enum DataType {
    INTEGER, DOUBLE, STRING, DATE, BOOLEAN;

    public Object convert(Object value) {
        if (value == null) return null;
        try {
            switch (this) {
                case INTEGER:
                    if (value instanceof Number) return ((Number) value).intValue();
                    return Integer.parseInt(value.toString());
                case DOUBLE:
                    if (value instanceof Number) return ((Number) value).doubleValue();
                    return Double.parseDouble(value.toString());
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
