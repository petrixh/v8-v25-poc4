package de.symeda.sormas.ui.samples.sampleLink;

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ContentMode;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.components.MultilineLabel;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentLayout;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public class SampleListComponentLayout extends SideComponentLayout {

	public SampleListComponentLayout(SampleListComponent sampleListComponent, String infoText) {
		super(sampleListComponent);

		if (UiUtil.permitted(UserRight.SAMPLE_CREATE)) {
			sampleListComponent.addClassName(CssStyles.VSPACE_NONE);
			MultilineLabel sampleInfo =
				new MultilineLabel(infoText == null ? "" : HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " " + infoText, ContentMode.HTML);
			sampleInfo.addClassNames(CssStyles.VSPACE_2, CssStyles.VSPACE_TOP_4);
			add(sampleInfo);
		}
	}

	public SampleListComponentLayout(SampleListComponent sampleListComponent, String infoText, boolean isEditAllowed) {
		super(sampleListComponent);

		if (UiUtil.permitted(UserRight.SAMPLE_CREATE) && infoText != null) {
			sampleListComponent.addClassName(CssStyles.VSPACE_NONE);
			MultilineLabel sampleInfo = new MultilineLabel(HtmlRenderer.iconHtml(VaadinIcon.INFO_CIRCLE) + " " + infoText, ContentMode.HTML);
			sampleInfo.addClassNames(CssStyles.VSPACE_2, CssStyles.VSPACE_TOP_4);
			sampleInfo.setEnabled(isEditAllowed);
			add(sampleInfo);
		}

	}
}
