package com.team.dbms.index;

import com.team.dbms.core.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HashIndexTest {
    @Test
    void duplicateValuesDeletionAndReplacement() {
        HashIndex index = new HashIndex(new Column("country", DataType.STRING, false, true));
        assertTrue(index.find("USA").isEmpty());
        Row first = new Row().set("country", "USA");
        Row second = new Row().set("country", "USA");
        index.add(first);
        index.add(second);
        index.add(first);
        assertEquals(Arrays.asList(first, second), index.find("USA"));
        assertTrue(index.find("missing").isEmpty());
        index.find("USA").clear();
        assertEquals(2, index.find("USA").size());
        Row changed = new Row().set("country", "UK");
        index.replace(first, changed);
        assertEquals(Collections.singletonList(second), index.find("USA"));
        assertEquals(Collections.singletonList(changed), index.find("UK"));
        index.remove(second);
        assertTrue(index.find("USA").isEmpty());
        index.remove(second);
        index.remove(changed);
        index.add(first);
        assertEquals(Collections.singletonList(first), index.find("USA"));
    }

    @Test
    void replacementRetainsTableOrderIncludingUnchangedKey() {
        HashIndex index = new HashIndex(new Column("value", DataType.INTEGER, false, true));
        Row first = new Row().set("value", 1);
        Row second = new Row().set("value", 2);
        index.add(first);
        index.add(second);
        Row changed = new Row().set("value", 2);
        index.replace(first, changed);
        Row same = new Row().set("value", 2);
        index.replace(second, same);
        assertEquals(Arrays.asList(changed, same), index.find(2));
    }

    @Test
    void keysUseSchemaTypesAndSupportNull() {
        for (DataType type : DataType.values()) {
            Object value;
            switch (type) {
                case INTEGER: value = 42; break;
                case DOUBLE: value = 42.5; break;
                case BOOLEAN: value = true; break;
                case DATE: value = LocalDate.of(2024, 1, 2); break;
                default: value = "USA";
            }
            HashIndex index = new HashIndex(new Column("value", type, false, true));
            Row row = new Row().set("value", value);
            Row nullRow = new Row().set("value", null);
            index.add(row);
            index.add(nullRow);
            assertEquals(Collections.singletonList(row), index.find(value.toString()));
            assertEquals(Collections.singletonList(nullRow), index.find(null));
            index.remove(nullRow);
            assertTrue(index.find(null).isEmpty());
        }
    }
}
