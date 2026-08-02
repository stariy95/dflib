package org.dflib.jupyter.render;

import org.dflib.Series;
import org.dflib.SeriesInfo;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SeriesInfoRendererTest {

    private static final String LS = System.lineSeparator();

    private Object render(SeriesInfo info) {
        Renderer renderer = new Renderer();
        renderer.createRegistration(SeriesInfo.class)
                .preferring(MIMEType.TEXT_PLAIN)
                .supporting(MIMEType.TEXT_PLAIN)
                .register(new SeriesInfoRenderer(new MutableTabularPrinter()));

        DisplayData data = renderer.render(info);
        return data.getData(MIMEType.TEXT_PLAIN);
    }

    @Test
    public void render() {
        assertEquals(LS +
                        "type nullable null_count min avg max" + LS +
                        "---- -------- ---------- --- --- ---" + LS +
                        "int     false null       1.0 3.0 5.0" + LS +
                        "1 row x 6 columns",
                render(Series.ofInt(5, 1, 3).describe()));
    }

    @Test
    public void render_NoStats() {
        assertEquals(LS +
                        "type             nullable null_count min  avg  max " + LS +
                        "---------------- -------- ---------- ---- ---- ----" + LS +
                        "java.lang.String     true          0 null null null" + LS +
                        "1 row x 6 columns",
                render(Series.of("a", "b").describe()));
    }
}
