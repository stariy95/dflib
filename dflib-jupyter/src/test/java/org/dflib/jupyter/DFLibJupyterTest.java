package org.dflib.jupyter;

import org.dflib.DataFrame;
import org.dflib.Series;
import org.dflib.jjava.jupyter.kernel.display.DisplayData;
import org.dflib.jjava.jupyter.kernel.display.Renderer;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DFLibJupyterTest {

    private static Renderer rendererWithDFLib() {
        Renderer renderer = new Renderer();
        new DFLibJupyter().installRenderers(renderer);
        return renderer;
    }

    @Test
    public void installRenderers_DataFrame() {
        DisplayData data = rendererWithDFLib().render(DataFrame.foldByRow("a").of(1, 2));
        assertTrue(data.hasDataForType(MIMEType.TEXT_PLAIN));
        assertNotNull(data.getData(MIMEType.TEXT_PLAIN));
    }

    @Test
    public void installRenderers_Series() {
        DisplayData data = rendererWithDFLib().render(Series.ofInt(1, 2));
        assertTrue(data.hasDataForType(MIMEType.TEXT_PLAIN));
        assertNotNull(data.getData(MIMEType.TEXT_PLAIN));
    }

    @Test
    public void installRenderers_DataFrameInfo() {
        DisplayData data = rendererWithDFLib().render(DataFrame.foldByRow("a").of(1, 2).describe());
        assertTrue(data.hasDataForType(MIMEType.TEXT_PLAIN));
        assertTrue(data.getData(MIMEType.TEXT_PLAIN).toString().contains("null_count"));
    }

    @Test
    public void installRenderers_SeriesInfo() {
        DisplayData data = rendererWithDFLib().render(Series.ofInt(1, 2).describe());
        assertTrue(data.hasDataForType(MIMEType.TEXT_PLAIN));
        assertTrue(data.getData(MIMEType.TEXT_PLAIN).toString().contains("null_count"));
    }
}
