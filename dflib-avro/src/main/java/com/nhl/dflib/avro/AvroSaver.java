package com.nhl.dflib.avro;

import com.nhl.dflib.DataFrame;
import com.nhl.dflib.avro.types.AvroTypeExtensions;
import com.nhl.dflib.row.RowProxy;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SyncableFileOutputStream;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Saves DataFrames to binary ".avro" files with an embedded schema and optional compression.
 *
 * @since 0.11
 */
public class AvroSaver extends BaseSaver<AvroSaver> {

    private CodecFactory codec;
    private Schema schema;

    public AvroSaver codec(CodecFactory codec) {
        this.codec = codec;
        return this;
    }

    /**
     * Save data with the explicit Schema. If not set, a Schema will be generated automatically based on the
     * DataFrame contents.
     */
    public AvroSaver schema(Schema schema) {
        this.schema = schema;
        return this;
    }

    public void save(DataFrame df, OutputStream out) {

        Schema schema = getOrCreateSchema(df);
        try {
            doSave(df, schema, out);
        } catch (IOException e) {
            throw new RuntimeException("Error writing records as Avro: " + e.getMessage(), e);
        }
    }

    public void save(DataFrame df, File file) {

        createMissingDirsIfNeeded(file);

        // using SyncableFileOutputStream just like Avro does (though it should work with a regula FOS)
        try (SyncableFileOutputStream out = new SyncableFileOutputStream(file)) {
            save(df, out);
        } catch (IOException e) {
            throw new RuntimeException("Error writing Avro file '" + file + "': " + e.getMessage(), e);
        }
    }

    public void save(DataFrame df, String fileName) {
        save(df, new File(fileName));
    }

    protected Schema getOrCreateSchema(DataFrame df) {
        return this.schema != null ? this.schema : schemaBuilder.compileSchema(df);
    }

    protected void doSave(DataFrame df, Schema schema, OutputStream out) throws IOException {

        DataFrame avroReadyDf = convertUnmappedTypes(df, schema);

        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);

        // DataFileWriter includes Schema in the output
        try (DataFileWriter<GenericRecord> outWriter = new DataFileWriter<>(writer)) {

            if (codec != null) {
                outWriter.setCodec(codec);
            }

            outWriter.create(schema, out);

            // using flyweight wrapper around DFLib RowProxy
            RowToAvroRecordAdapter record = new RowToAvroRecordAdapter(schema);
            for (RowProxy r : avroReadyDf) {
                outWriter.append(record.resetRow(r));
            }
        }
    }

    protected DataFrame convertUnmappedTypes(DataFrame df, Schema schema) {

        // convert unmapped types to Strings so that they can be (de)serialized natively by Avro

        for (Schema.Field f : schema.getFields()) {
            Schema fSchema = f.schema().isUnion() ? unpackUnion(f.schema()) : f.schema();
            if (isUnmappedType(fSchema)) {
                df = df.convertColumn(f.name(), v -> v != null ? v.toString() : v);
            }
        }

        return df;
    }

    protected Schema unpackUnion(Schema union) {

        for (Schema child : union.getTypes()) {
            if (!child.isNullable()) {
                return child;
            }
        }

        return null;
    }

    protected boolean isUnmappedType(Schema schema) {

        if (schema == null) {
            return false;
        }

        LogicalType t = schema.getLogicalType();
        return t != null && t.getName().equals(AvroTypeExtensions.UNMAPPED_TYPE.getName());
    }
}