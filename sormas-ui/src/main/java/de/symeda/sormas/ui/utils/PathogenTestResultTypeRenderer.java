package de.symeda.sormas.ui.utils;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

public class PathogenTestResultTypeRenderer extends TextRenderer {
    @Override
    public String encode(Object value) {
        if (value == null) {
            return super.encode(I18nProperties.getCaption(Captions.notTestedYet));
        }
        return super.encode(value);
    }
}
