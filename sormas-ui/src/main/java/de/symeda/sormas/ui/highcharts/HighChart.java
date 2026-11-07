/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.highcharts;

import java.util.LinkedHashMap;
import java.util.Map;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;

/**
 * <p>
 * A chart drawn by Vaadin Charts from Highcharts options written as JavaScript.
 * </p>
 * <p>
 * Until Vaadin 8 this was a JavaScript component around Highcharts 8 (based on https://github.com/xylo/highcharts-vaadin7,
 * Apache 2.0 license, by Stefan Endrullis). It is now the Vaadin Charts component, so the chart users keep building their options
 * as before: {@link #setHcjs(String)} takes JavaScript code that binds the Highcharts options to a variable called
 * <code>options</code>, e.g. <code>chart.setHcjs("var options = { chart: { type: 'column' }, series: [...] };")</code>. The code
 * may use functions (formatters, menu item handlers) and read <code>Highcharts</code> (e.g. <code>Highcharts.theme</code>).
 * The options replace the whole chart configuration in the browser (<code>highchart-connector.js</code>); the Java configuration
 * model of {@link Chart} is not used.
 * </p>
 */
@JsModule("./charts/highchart-connector.js")
public class HighChart extends Chart {

	private static final long serialVersionUID = 7738496276049495017L;

	protected static int currChartId = 0;

	public static int nextChartId() {
		return ++currChartId;
	}

	protected int chartId = nextChartId();

	private String hcjs = "";

	private final Map<String, SerializableRunnable> jsFunctions = new LinkedHashMap<>();

	private Registration pendingUpdate;

	/**
	 * Creates the chart object.
	 */
	public HighChart() {
		setId(getDomId());
	}

	/**
	 * Returns the DOM ID of the chart component.
	 *
	 * @return the DOM ID of the chart component
	 */
	public String getDomId() {
		return "highchart_" + chartId;
	}

	/**
	 * <p>
	 * Sets the Highcharts JavaScript code describing the chart.
	 * Note that this code needs to bind the the JSON definition of the chart to a JS variable called <code>options</code>.
	 * </p>
	 * <p>
	 * Example:
	 * </p>
	 * 
	 * <pre>
	 * chart.setHcjs("var options = { chart: { title: 'my title' } };")
	 * </pre>
	 *
	 * @param hcjs
	 *            Highcharts JavaScript code describing the chart
	 */
	public void setHcjs(String hcjs) {
		this.hcjs = hcjs != null ? hcjs : "";
		scheduleUpdate();
	}

	/**
	 * Defines the browser function <code>window.&lt;name&gt;()</code>, which runs <code>action</code> on the server. Chart options
	 * call it from their JavaScript, e.g. a context menu item
	 * <code>{ onclick: function() { window.&lt;name&gt;(); } }</code>. Replaces the Vaadin 8
	 * <code>JavaScript.getCurrent().addFunction(name, ...)</code>; the name should be unique in the page.
	 */
	public void addJsFunction(String name, SerializableRunnable action) {
		jsFunctions.put(name, action);
		scheduleUpdate();
	}

	@ClientCallable
	private void runJsFunction(String name) {
		SerializableRunnable action = jsFunctions.get(name);
		if (action != null) {
			action.run();
		}
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// Chart draws its own (empty) configuration before the client response; the update registered after it replaces it
		super.onAttach(attachEvent);
		scheduleUpdate();
	}

	private void scheduleUpdate() {
		getUI().ifPresent(ui -> {
			if (pendingUpdate != null) {
				pendingUpdate.remove();
			}
			pendingUpdate = ui.beforeClientResponse(this, context -> {
				pendingUpdate = null;
				getElement().executeJs(
					"window.Vaadin.sormasHighChart.update(this, $0, $1)",
					hcjs,
					String.join(",", jsFunctions.keySet()));
			});
		});
	}
}
