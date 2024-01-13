package org.dflib.slice;

import org.dflib.ColumnDataFrame;
import org.dflib.ColumnSet;
import org.dflib.DataFrame;
import org.dflib.Exp;
import org.dflib.Index;
import org.dflib.RowMapper;
import org.dflib.RowToValueMapper;
import org.dflib.Series;
import org.dflib.index.LabelDeduplicator;
import org.dflib.row.MultiArrayRowBuilder;
import org.dflib.series.RowMappedSeries;
import org.dflib.series.SingleValueSeries;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * @since 1.0.0-M19
 */
public class FixedColumnSet implements ColumnSet {

    protected final DataFrame source;
    protected final Index sourceColumnsIndex;
    protected final Series[] sourceColumns;
    protected final String[] csIndex;

    public static FixedColumnSet of(DataFrame source, Series<?>[] sourceColumns, Index csIndex) {
        return new FixedColumnSet(source, sourceColumns, csIndex.getLabels());
    }

    public static FixedColumnSet of(DataFrame source, Series<?>[] sourceColumns, String[] csIndex) {
        return new FixedColumnSet(source, sourceColumns, csIndex);
    }

    public static FixedColumnSet ofAdd(DataFrame source, Series<?>[] sourceColumns, String[] csIndex) {

        Index index = source.getColumnsIndex();
        int csLen = csIndex.length;

        LabelDeduplicator deduplicator = LabelDeduplicator.of(index, csLen);
        String[] csIndexAdd = new String[csLen];
        for (int i = 0; i < csLen; i++) {
            csIndexAdd[i] = deduplicator.nonConflictingName(csIndex[i]);
        }

        // TODO: an "append-only" version of FixedColumnSet, as we are guaranteed to not replace any existing columns
        return new FixedColumnSet(source, sourceColumns, csIndexAdd);
    }

    public static FixedColumnSet of(DataFrame source, Series<?>[] sourceColumns, int[] csIndex) {

        Index index = source.getColumnsIndex();

        int sLen = index.size();
        int csLen = csIndex.length;

        String[] csLabelsIndex = new String[csLen];
        for (int i = 0; i < csLen; i++) {
            csLabelsIndex[i] = csIndex[i] < sLen
                    ? index.getLabel(csIndex[i])
                    : String.valueOf(csIndex[i]);
        }

        return new FixedColumnSet(source, sourceColumns, csLabelsIndex);
    }

    protected FixedColumnSet(DataFrame source, Series<?>[] sourceColumns, String[] csIndex) {
        this.source = source;
        this.sourceColumnsIndex = source.getColumnsIndex();
        this.sourceColumns = sourceColumns;
        this.csIndex = csIndex;
    }

    @Override
    public DataFrame rename(String... newColumnNames) {
        int w = newColumnNames.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'rename': column names size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        Map<String, String> oldToNewMap = new HashMap<>((int) Math.ceil(w / 0.75));
        for (int i = 0; i < w; i++) {
            oldToNewMap.put(csIndex[i], newColumnNames[i]);
        }

        return rename(oldToNewMap);
    }

    @Override
    public DataFrame rename(UnaryOperator<String> renameFunction) {

        Map<String, String> oldToNewMap = new HashMap<>((int) Math.ceil(csIndex.length / 0.75));
        for (String l : csIndex) {
            oldToNewMap.put(l, renameFunction.apply(l));
        }

        return rename(oldToNewMap);
    }

    @Override
    public DataFrame rename(Map<String, String> oldToNewNames) {

        int w = csIndex.length;
        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = getOrCreateColumn(source, i);
        }

        return merge(columns, oldToNewNames);
    }

    @Override
    public DataFrame fill(Object... values) {

        int w = values.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'fill': values size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        int h = source.height();

        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = Series.ofVal(values[i], h);
        }

        return merge(columns);
    }

    @Override
    public DataFrame fillNulls(Object value) {

        if (value == null) {
            return source;
        }

        int w = csIndex.length;
        int h = source.height();

        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = getOrCreateColumn(source, i,
                    e -> ((Series<Object>) e).fillNulls(value),
                    () -> Series.ofVal(value, h));
        }

        return merge(columns);
    }

    @Override
    public DataFrame fillNullsBackwards() {

        int w = csIndex.length;
        int h = source.height();
        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = getOrCreateColumn(
                    source,
                    i,
                    e -> e.fillNullsBackwards(),
                    () -> new SingleValueSeries<>(null, h));
        }

        return merge(columns);
    }

    @Override
    public DataFrame fillNullsForward() {

        int w = csIndex.length;
        int h = source.height();
        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = getOrCreateColumn(
                    source,
                    i,
                    e -> e.fillNullsForward(),
                    () -> new SingleValueSeries<>(null, h));
        }

        return merge(columns);
    }

    @Override
    public DataFrame fillNullsFromSeries(Series<?> series) {

        int w = csIndex.length;
        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            Series s = getOrCreateColumn(source, i);
            columns[i] = s.fillNullsFromSeries(series);
        }

        return merge(columns);
    }

    @Override
    public DataFrame select() {

        int w = csIndex.length;
        Series<?>[] columns = new Series[w];

        for (int i = 0; i < w; i++) {
            columns[i] = getOrCreateColumn(source, i);
        }

        return select(columns);
    }

    @Override
    public DataFrame map(Exp<?>... exps) {
        return merge(doMap(exps));
    }

    @Override
    public DataFrame select(Exp<?>... exps) {
        return select(doMap(exps));
    }

    private Series<?>[] doMap(Exp<?>[] exps) {

        int w = exps.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'map': Exp[] size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = exps[i].eval(source);
        }

        return columns;
    }

    @Override
    public DataFrame map(Series<?>... columns) {

        int w = csIndex.length;

        if (columns.length != w) {
            throw new IllegalArgumentException(
                    "Can't perform 'map': Series[] size is different from the ColumnSet size: " + columns.length + " vs. " + w);
        }

        int h = source.height();
        for (int i = 0; i < w; i++) {
            if (columns[i].size() != h) {
                throw new IllegalArgumentException("The mapped column height (" + columns[i].size() + ") is different from the DataFrame height (" + h + ")");
            }
        }

        return merge(columns);
    }

    @Override
    public DataFrame map(RowToValueMapper<?>... exps) {
        return merge(doMap(exps));
    }

    @Override
    public DataFrame select(RowToValueMapper<?>... exps) {
        return merge(doMap(exps));
    }

    private Series<?>[] doMap(RowToValueMapper<?>[] mappers) {

        int w = mappers.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'map': RowToValueMappers size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = new RowMappedSeries<>(source, mappers[i]);
        }

        return columns;
    }

    @Override
    public DataFrame map(RowMapper mapper) {

        MultiArrayRowBuilder b = new MultiArrayRowBuilder(Index.of(csIndex), source.height());

        source.forEach(from -> {
            b.next();
            mapper.map(from, b);
        });

        return merge(b.getData());
    }

    @Override
    public DataFrame select(RowMapper mapper) {
        MultiArrayRowBuilder b = new MultiArrayRowBuilder(Index.of(csIndex), source.height());

        source.forEach(from -> {
            b.next();
            mapper.map(from, b);
        });

        return select(b.getData());
    }

    @Override
    public DataFrame mapIterables(Exp<? extends Iterable<?>> mapper) {
        return merge(doMapIterables(mapper));
    }

    @Override
    public DataFrame selectIterables(Exp<? extends Iterable<?>> mapper) {
        return select(doMapIterables(mapper));
    }

    private Series<?>[] doMapIterables(Exp<? extends Iterable<?>> mapper) {

        Series<? extends Iterable<?>> ranges = mapper.eval(source);

        int w = csIndex.length;
        int h = source.height();
        Object[][] data = new Object[w][h];
        for (int j = 0; j < w; j++) {
            data[j] = new Object[h];
        }

        for (int i = 0; i < h; i++) {

            Iterable<?> r = ranges.get(i);
            if (r == null) {
                continue;
            }

            Iterator<?> rit = r.iterator();
            for (int j = 0; j < w && rit.hasNext(); j++) {
                data[j][i] = rit.next();
            }
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = Series.of(data[i]);
        }

        return columns;
    }

    @Override
    public DataFrame mapArrays(Exp<? extends Object[]> mapper) {
        return merge(doMapArrays(mapper));
    }

    @Override
    public DataFrame selectArrays(Exp<? extends Object[]> mapper) {
        return select(doMapArrays(mapper));
    }

    private Series<?>[] doMapArrays(Exp<? extends Object[]> mapper) {
        Series<? extends Object[]> ranges = mapper.eval(source);

        int w = csIndex.length;
        int h = source.height();
        Object[][] data = new Object[w][h];
        for (int j = 0; j < w; j++) {
            data[j] = new Object[h];
        }

        for (int i = 0; i < h; i++) {

            Object[] r = ranges.get(i);
            if (r == null) {
                continue;
            }

            int rw = Math.min(r.length, w);
            for (int j = 0; j < rw; j++) {
                data[j][i] = r[j];
            }
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = Series.of(data[i]);
        }

        return columns;
    }

    @Override
    public DataFrame map(String... existingColumns) {
        int w = existingColumns.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'rename': existing columns size is different from the provided column set size: " + w + " vs. " + csIndex.length);
        }

        // columns will be present in the same order and untransformed from the original
        int dfw = source.width();
        Series<?>[] columns = new Series[dfw];
        for (int i = 0; i < dfw; i++) {
            columns[i] = source.getColumn(i);
        }

        // index must undergo renaming
        Map<String, String> renameMap = new HashMap<>();
        for (int i = 0; i < w; i++) {
            renameMap.put(existingColumns[i], csIndex[i]);
        }

        return replace(columns, renameMap);
    }

    @Override
    public DataFrame map(int... existingColumns) {
        int w = existingColumns.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'rename': existing columns size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        // columns will be present in the same order and untransformed from the original
        int dfw = source.width();
        Series<?>[] columns = new Series[dfw];
        for (int i = 0; i < dfw; i++) {
            columns[i] = source.getColumn(i);
        }

        // index must undergo renaming
        Map<String, String> renameMap = new HashMap<>();
        Index sourceIndex = source.getColumnsIndex();

        for (int i = 0; i < w; i++) {
            renameMap.put(sourceIndex.getLabel(existingColumns[i]), csIndex[i]);
        }

        return replace(columns, renameMap);
    }

    @Override
    public DataFrame select(String... existingColumns) {
        int w = existingColumns.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'rename': existing columns size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = source.getColumn(existingColumns[i]);
        }

        return select(columns);
    }

    @Override
    public DataFrame select(int... existingColumns) {
        int w = existingColumns.length;
        if (w != csIndex.length) {
            throw new IllegalArgumentException(
                    "Can't perform 'rename': existing columns size is different from the ColumnSet size: " + w + " vs. " + csIndex.length);
        }

        Series<?>[] columns = new Series[w];
        for (int i = 0; i < w; i++) {
            columns[i] = source.getColumn(existingColumns[i]);
        }

        return select(columns);
    }

    public Series<?> getOrCreateColumn(DataFrame source, int pos) {
        String name = csIndex[pos];
        return sourceColumnsIndex.hasLabel(name)
                ? source.getColumn(name)
                : new SingleValueSeries<>(null, source.height());
    }

    public Series<?> getOrCreateColumn(
            DataFrame source,
            int pos,
            UnaryOperator<Series<?>> andApplyToExisting,
            Supplier<Series<?>> createNew) {

        String name = csIndex[pos];
        return sourceColumnsIndex.hasLabel(name)
                ? andApplyToExisting.apply(source.getColumn(name))
                : createNew.get();
    }

    protected DataFrame merge(Series<?>[] columns) {
        return merger().merge(sourceColumnsIndex.getLabels(), sourceColumns, csIndex, columns);
    }

    protected DataFrame merge(Series<?>[] columns, Map<String, String> oldToNewNames) {
        return merger().merge(
                sourceColumnsIndex.getLabels(),
                sourceColumns,
                Index.of(csIndex).rename(oldToNewNames).getLabels(),
                columns);
    }

    protected DataFrame select(Series<?>[] columns) {
        return new ColumnDataFrame(null, Index.ofDeduplicated(csIndex), columns);
    }

    protected DataFrame replace(Series<?>[] columns, Map<String, String> oldToNewNames) {
        return new ColumnDataFrame(null, sourceColumnsIndex.rename(oldToNewNames), columns);
    }

    protected ColumnSetMerger merger() {
        return ColumnSetMerger.of(sourceColumnsIndex, csIndex);
    }
}