package org.dflib.jupyter.render;

import org.dflib.SeriesInfo;
import org.dflib.jjava.jupyter.kernel.display.RenderContext;
import org.dflib.jjava.jupyter.kernel.display.RenderFunction;
import org.dflib.jjava.jupyter.kernel.display.mime.MIMEType;
import org.dflib.print.Printer;

/**
 * @since 2.0.0
 */
public class SeriesInfoRenderer implements RenderFunction<SeriesInfo> {

    private final Printer printer;

    public SeriesInfoRenderer(Printer printer) {
        this.printer = printer;
    }

    @Override
    public void render(SeriesInfo info, RenderContext context) {
        context.renderIfRequested(MIMEType.TEXT_PLAIN, () -> printer.print(info.toDataFrame()));
    }
}
