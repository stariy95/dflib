package com.nhl.dflib.column.join;

import com.nhl.dflib.DataFrame;
import com.nhl.dflib.GroupBy;
import com.nhl.dflib.Index;
import com.nhl.dflib.column.concat.ColumnHConcat;
import com.nhl.dflib.join.JoinType;
import com.nhl.dflib.map.Hasher;
import com.nhl.dflib.row.RowProxy;
import com.nhl.dflib.series.ListSeries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A DataFrame joiner using <a href="https://en.wikipedia.org/wiki/Hash_join">"hash join"</a> algorithm. It requires
 * two custom "hash" functions for the rows on the left and the right sides of the join, each producing values, whose
 * equality can be used as a join condition. Should theoretically have O(N + M) performance.
 */
public class ColumnarHashJoiner {

    private static final String ROW_INDEX_COLUMN = "$row_index_" + System.currentTimeMillis();

    private Hasher leftHasher;
    private Hasher rightHasher;
    private JoinType semantics;

    public ColumnarHashJoiner(
            Hasher leftHasher,
            Hasher rightHasher,
            JoinType semantics) {

        this.leftHasher = leftHasher;
        this.rightHasher = rightHasher;
        this.semantics = semantics;
    }

    public Index joinIndex(Index li, Index ri) {
        return ColumnHConcat.zipIndex(li, ri);
    }

    public JoinMerger joinMerger(DataFrame lf, DataFrame rf) {
        switch (semantics) {
            case inner:
                return innerJoin(lf, rf);
            case left:
                return leftJoin(lf, rf);
            case right:
                return rightJoin(lf, rf);
            case full:
                return fullJoin(lf, rf);
            default:
                throw new IllegalStateException("Unsupported join semantics: " + semantics);
        }
    }

    private JoinMerger innerJoin(DataFrame lf, DataFrame rf) {

        List<Integer> li = new ArrayList<>();
        List<Integer> ri = new ArrayList<>();

        DataFrame rfi = rf.addRowIndexColumn(ROW_INDEX_COLUMN);
        int rfip = rfi.getColumns().position(ROW_INDEX_COLUMN).position();

        GroupBy rightIndex = rfi.groupBy(rightHasher);

        int i = 0;
        for (RowProxy lr : lf) {

            Object lKey = leftHasher.map(lr);
            DataFrame rightMatches = rightIndex.getGroup(lKey);
            if (rightMatches != null) {
                for (RowProxy rr : rightMatches) {
                    li.add(i);
                    ri.add((Integer) rr.get(rfip));
                }
            }

            i++;
        }

        return new JoinMerger(new ListSeries<>(li), new ListSeries<>(ri));
    }

    private JoinMerger leftJoin(DataFrame lf, DataFrame rf) {

        List<Integer> li = new ArrayList<>();
        List<Integer> ri = new ArrayList<>();

        DataFrame rfi = rf.addRowIndexColumn(ROW_INDEX_COLUMN);
        int rfip = rfi.getColumns().position(ROW_INDEX_COLUMN).position();

        GroupBy rightIndex = rfi.groupBy(rightHasher);

        int i = 0;
        for (RowProxy lr : lf) {

            Object lKey = leftHasher.map(lr);
            DataFrame rightMatches = rightIndex.getGroup(lKey);

            if (rightMatches != null) {
                for (RowProxy rr : rightMatches) {
                    li.add(i);
                    ri.add((Integer) rr.get(rfip));
                }
            } else {
                li.add(i);
                ri.add(-1);
            }

            i++;
        }

        return new JoinMerger(new ListSeries<>(li), new ListSeries<>(ri));
    }

    private JoinMerger rightJoin(DataFrame lf, DataFrame rf) {

        List<Integer> li = new ArrayList<>();
        List<Integer> ri = new ArrayList<>();

        DataFrame lfi = lf.addRowIndexColumn(ROW_INDEX_COLUMN);
        int lfip = lfi.getColumns().position(ROW_INDEX_COLUMN).position();

        GroupBy leftIndex = lfi.groupBy(leftHasher);

        int i = 0;
        for (RowProxy rr : rf) {

            Object rKey = rightHasher.map(rr);
            DataFrame leftMatches = leftIndex.getGroup(rKey);

            if (leftMatches != null) {
                for (RowProxy lr : leftMatches) {
                    li.add((Integer) lr.get(lfip));
                    ri.add(i);
                }
            } else {
                li.add(-1);
                ri.add(i);
            }

            i++;
        }

        return new JoinMerger(new ListSeries<>(li), new ListSeries<>(ri));
    }

    private JoinMerger fullJoin(DataFrame lf, DataFrame rf) {

        List<Integer> li = new ArrayList<>();
        List<Integer> ri = new ArrayList<>();

        DataFrame rfi = rf.addRowIndexColumn(ROW_INDEX_COLUMN);
        int rfip = rfi.getColumns().position(ROW_INDEX_COLUMN).position();

        GroupBy rightIndex = rfi.groupBy(rightHasher);
        Set<Object> seenRightKeys = new LinkedHashSet<>();

        int i = 0;
        for (RowProxy lr : lf) {

            Object lKey = leftHasher.map(lr);
            DataFrame rightMatches = rightIndex.getGroup(lKey);

            if (rightMatches != null) {
                seenRightKeys.add(lKey);
                for (RowProxy rr : rightMatches) {
                    li.add(i);
                    ri.add((Integer) rr.get(rfip));
                }
            } else {
                li.add(i);
                ri.add(-1);
            }

            i++;
        }

        // add missing right rows
        for (Object key : rightIndex.getGroups()) {
            if (!seenRightKeys.contains(key)) {
                for (RowProxy rr : rightIndex.getGroup(key)) {
                    li.add(-1);
                    ri.add((Integer) rr.get(rfip));
                }
            }
        }

        return new JoinMerger(new ListSeries<>(li), new ListSeries<>(ri));
    }
}
