package de.symeda.sormas.ui.dashboard.visualisation;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * The contact network diagram: an R htmlwidget (visNetwork binding 2.0.9 on vis-network 7.6.2) drawn from the JSON that
 * {@code VisualizationFacade.buildTransmissionChainJson} returns. The browser side is
 * {@code frontend/visualisation/network-diagram-connector.js}.
 * <p>
 * vis-network comes from npm (same version as the copy Vaadin 8 served); the htmlwidgets and visNetwork binding scripts are kept
 * in {@code frontend/visualisation/}.
 */
@Tag("div")
@NpmPackage(value = "vis-network", version = "7.6.2")
@JsModule("./visualisation/network-diagram-connector.js")
public class NetworkDiagram extends Component implements HasSize, HasStyle {

	private static final long serialVersionUID = 1L;

	private String jsonData;

	public NetworkDiagram() {
	}

	public void updateDiagram(final String jsonData) {
		this.jsonData = jsonData;
		if (isAttached()) {
			draw();
		}
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		draw();
	}

	private void draw() {
		getElement().executeJs("window.Vaadin.sormasNetworkDiagram.update(this, $0)", jsonData);
	}
}
