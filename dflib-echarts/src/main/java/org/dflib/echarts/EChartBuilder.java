package org.dflib.echarts;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.echarts.model.AxisLabelModel;
import org.dflib.echarts.model.AxisModel;
import org.dflib.echarts.model.ContainerModel;
import org.dflib.echarts.model.DataSetModel;
import org.dflib.echarts.model.EncodeModel;
import org.dflib.echarts.model.ExternalScriptModel;
import org.dflib.echarts.model.RowModel;
import org.dflib.echarts.model.ScriptModel;
import org.dflib.echarts.model.SeriesModel;
import org.dflib.echarts.model.ValueModel;
import org.dflib.series.IntSequenceSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * A builder of HTML/JS code that renders DataFrame data using ECharts library.
 *
 * @since 1.0.0-M21
 */
public class EChartBuilder {
    private static final String DEFAULT_ECHARTS_SCRIPT_URL = "https://cdn.jsdelivr.net/npm/echarts@5.5.0/dist/echarts.min.js";

    private static final Mustache CONTAINER_TEMPLATE = loadTemplate("container.mustache");
    private static final Mustache EXTERNAL_SCRIPT_TEMPLATE = loadTemplate("external_script.mustache");
    private static final Mustache SCRIPT_TEMPLATE = loadTemplate("script.mustache");

    static Mustache loadTemplate(String name) {
        try (InputStream in = ECharts.class.getResourceAsStream(name)) {

            if (in == null) {
                throw new RuntimeException("ECharts 'cell.mustache' template is not found");
            }

            // not providing an explicit resolver of subtemplates.. assuming a single flat template for now
            try (Reader reader = new InputStreamReader(in)) {
                return new DefaultMustacheFactory().compile(reader, name);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading muustache template: " + name, e);
        }
    }

    // TODO: extract "RootOpts" or "Opts" from the builder similar to SeriesOpts, etc.

    private final Random rnd;
    private String theme;

    private String scriptUrl;
    private String title;
    private Integer width;
    private Integer height;
    private Boolean legend;

    private String xAxisColumn;
    private AxisOpts xAxisOpts;

    private AxisOpts yAxisOpts;

    private final Map<String, SeriesOpts> series;

    private final SeriesOpts defaultSeriesOpts;
    private SeriesOpts seriesOpts;

    protected EChartBuilder(SeriesOpts defaultSeriesOpts) {
        this.rnd = new SecureRandom();

        this.defaultSeriesOpts = defaultSeriesOpts;

        // keeping the "series" order predictable
        this.series = new LinkedHashMap<>();
    }

    /**
     * Specifies which DataFrame column should be used to label the X axis. This setting is optional. If not set,
     * series element indices will be used for X.
     */
    public EChartBuilder xAxis(String xAxisColumn) {
        this.xAxisColumn = xAxisColumn;
        return this;
    }

    public EChartBuilder xAxis(String xAxisColumn, AxisOpts opts) {
        this.xAxisColumn = Objects.requireNonNull(xAxisColumn);
        this.xAxisOpts = Objects.requireNonNull(opts);
        return this;
    }

    public EChartBuilder xAxisOpts(AxisOpts opts) {
        this.xAxisOpts = Objects.requireNonNull(opts);
        return this;
    }

    public EChartBuilder yAxisOpts(AxisOpts opts) {
        this.yAxisOpts = Objects.requireNonNull(opts);
        return this;
    }

    /**
     * Alters the default series options that will be used by all data series that don't have explicit options.
     */
    public EChartBuilder seriesOpts(SeriesOpts opts) {
        this.seriesOpts = opts;
        return this;
    }

    /**
     * Specifies a DataFrame column that should be plotted as a single data "series". Specifies option overrides
     * for this series.
     */
    public EChartBuilder series(String dataColumn, SeriesOpts opts) {
        series.put(dataColumn, opts);
        return this;
    }

    /**
     * Specifies which DataFrame columns should be plotted as data "series". Unless you override it later,
     * all series will be rendered with the default chart options.
     */
    public EChartBuilder series(String... dataColumns) {
        for (String c : dataColumns) {

            // can't use "computeIfAbsent", as it doesn't handle null properly
            if (!series.containsKey(c)) {
                series.put(c, null);
            }
        }

        return this;
    }

    /**
     * Sets an alternative URL of ECharts JavaScript source. If not set, the default public URL is provided.
     */
    public EChartBuilder scriptUrl(String url) {
        this.scriptUrl = url;
        return this;
    }

    public EChartBuilder title(String title) {
        this.title = title;
        return this;
    }

    public EChartBuilder legend() {
        this.legend = Boolean.TRUE;
        return this;
    }

    public EChartBuilder sizePixels(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * @see #darkTheme()
     */
    public EChartBuilder theme(String theme) {
        this.theme = theme;
        return this;
    }

    public EChartBuilder darkTheme() {
        return theme("dark");
    }

    public EChart plot(DataFrame dataFrame) {
        String id = newId();
        return new EChart(
                generateContainerHtml(id),
                generateExternalScriptHtml(),
                generateScriptHtml(id, dataFrame)
        );
    }

    protected String generateContainerHtml(String id) {
        ContainerModel model = new ContainerModel(
                id,
                this.width != null ? this.width : 600,
                this.height != null ? this.height : 400
        );
        return CONTAINER_TEMPLATE.execute(new StringWriter(), model).toString();
    }

    protected String generateExternalScriptHtml() {
        ExternalScriptModel model = new ExternalScriptModel(
                this.scriptUrl != null ? this.scriptUrl : DEFAULT_ECHARTS_SCRIPT_URL
        );
        return EXTERNAL_SCRIPT_TEMPLATE.execute(new StringWriter(), model).toString();
    }

    protected String generateScriptHtml(String id, DataFrame df) {

        ScriptModel model = new ScriptModel(
                id,
                this.title,
                dataset(df),
                xAxis(),
                yAxis(),
                seriesFromSeries(),
                this.theme,
                this.legend != null ? this.legend : false
        );
        return SCRIPT_TEMPLATE.execute(new StringWriter(), model).toString();
    }

    protected String newId() {
        return "dfl_ech_" + Math.abs(rnd.nextInt(10_000));
    }

    protected DataSetModel dataset(DataFrame df) {

        // DF columns become rows and rows become columns in the EChart dataset
        int w = df.height();
        int h = series.size();

        List<ValueModel> headerRow = new ArrayList<>(w + 1);
        List<RowModel> rows = new ArrayList<>(h + 1);

        String[] rowLabels = series.keySet().toArray(new String[0]);


        Series<?> columnLabels = this.xAxisColumn != null
                ? df.getColumn(this.xAxisColumn)
                : new IntSequenceSeries(1, w + 1);
        String columnLabelsLabel = this.xAxisColumn != null ? this.xAxisColumn : "labels";

        headerRow.add(new ValueModel(columnLabelsLabel, w == 0));
        for (int i = 0; i < w; i++) {
            headerRow.add(new ValueModel(columnLabels.get(i), i + 1 == w));
        }

        rows.add(new RowModel(headerRow, h == 0));

        for (int i = 0; i < h; i++) {
            List<ValueModel> row = new ArrayList<>(w + 1);
            row.add(new ValueModel(rowLabels[i], w == 0));
            Series<?> data = df.getColumn(rowLabels[i]);

            for (int j = 0; j < w; j++) {
                row.add(new ValueModel(data.get(j), j + 1 == w));
            }

            rows.add(new RowModel(row, i + 1 == h));
        }

        return new DataSetModel(rows);
    }

    protected AxisModel xAxis() {

        AxisOpts xAxis = this.xAxisOpts != null ? this.xAxisOpts : AxisOpts.defaultX();

        return new AxisModel(
                xAxis.getType().name(),
                xAxis.getLabel() != null ? new AxisLabelModel(xAxis.getLabel().getFormatter()) : null,
                xAxis.isBoundaryGap()
        );
    }

    protected AxisModel yAxis() {

        AxisOpts yAxis = this.yAxisOpts != null ? this.yAxisOpts : AxisOpts.defaultY();

        return new AxisModel(
                yAxis.getType().name(),
                yAxis.getLabel() != null ? new AxisLabelModel(yAxis.getLabel().getFormatter()) : null,
                yAxis.isBoundaryGap()
        );
    }

    protected List<SeriesModel> seriesFromSeries() {
        SeriesOpts defaultOpts = defaultSeriesOpts();
        String[] columns = series.keySet().toArray(new String[0]);

        int len = columns.length;

        List<SeriesModel> models = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {

            SeriesOpts columnOpts = series.get(columns[i]);
            SeriesOpts mergedOpts = columnOpts != null ? defaultOpts.merge(columnOpts) : defaultOpts;

            SeriesModel m = new SeriesModel(
                    columns[i],
                    mergedOpts.getType().name(),
                    new EncodeModel(0, i + 1),
                    // hardcoding "row" series layout. It corresponds to the dataset layout
                    SeriesLayoutType.row.name(),
                    mergedOpts.isAreaStyle(),
                    mergedOpts.isStack(),
                    mergedOpts.isSmooth(),
                    i + 1 == len
            );

            models.add(m);
        }
        return models;
    }

    protected SeriesOpts defaultSeriesOpts() {
        return this.seriesOpts != null
                ? this.defaultSeriesOpts.merge(this.seriesOpts)
                : this.defaultSeriesOpts;
    }
}