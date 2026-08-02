package org.dflib.jupyter.render;

import org.dflib.DataFrame;
import org.dflib.DataFrameInfo;
import org.dflib.Series;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataFrameInfoRendererTest {

    private static final String LS = System.lineSeparator();

    private Object render(DataFrameInfo info) {
        Renderer renderer = new Renderer();
        renderer.createRegistration(DataFrameInfo.class)
                .preferring(MIMEType.TEXT_PLAIN)
                .supporting(MIMEType.TEXT_PLAIN)
                .register(new DataFrameInfoRenderer(new MutableTabularPrinter()));

        DisplayData data = renderer.render(info);
        return data.getData(MIMEType.TEXT_PLAIN);
    }

    @Test
    public void render() {
        DataFrame df = DataFrame
                .byColumn("i", "s")
                .of(
                        Series.ofInt(5, 1, 3),
                        Series.of("a", null, "c"));

        assertEquals(LS +
                        "index name type             nullable null_count  min  avg  max" + LS +
                        "----- ---- ---------------- -------- ---------- ---- ---- ----" + LS +
                        "    0 i    int                 false       null  1.0  3.0  5.0" + LS +
                        "    1 s    java.lang.String     true          1 null null null" + LS +
                        "2 rows x 8 columns",
                render(df.describe()));
    }
}
