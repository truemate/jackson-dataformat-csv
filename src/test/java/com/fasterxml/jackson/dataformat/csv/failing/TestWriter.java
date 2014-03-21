package com.fasterxml.jackson.dataformat.csv.failing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.ModuleTestBase;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.lang.String;

// [Issue#33]
public class TestWriter extends ModuleTestBase
{
    private final CsvSchema SCHEMA = new CsvSchema.Builder()
        .addColumn("timestamp", CsvSchema.ColumnType.STRING)
        .addColumn("value", CsvSchema.ColumnType.NUMBER)
        .addColumn("id", CsvSchema.ColumnType.STRING)
        .build();
    final ObjectWriter WRITER = new CsvMapper().writer().withSchema(SCHEMA);
    
    @Test
    public void testWrite_NoNulls() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42, "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42,hello\n", csv);
    }

    @Test
    public void testWrite_NullFirstColumn() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("value", 42, "id", "hello"));
        assertEquals(",42,hello\n", csv);
    }

    @Test
    public void testWrite_NullSecondColumn() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "id", "hello"));

        assertEquals("\"2014-03-10T23:32:47+00:00\",,hello\n", csv);
    }

    @Test
    public void testWrite_NullThirdColumn() throws JsonProcessingException {
        final String csv = WRITER.writeValueAsString(
                ImmutableMap.of("timestamp", "2014-03-10T23:32:47+00:00",
                        "value", 42));

        assertEquals("\"2014-03-10T23:32:47+00:00\",42\n", csv);
    }
}
