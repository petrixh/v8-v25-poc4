/*
 * SORMAS NetworkDiagram (de.symeda.sormas.ui.dashboard.visualisation.NetworkDiagram): the contact network as an R htmlwidget
 * (visNetwork binding on vis-network). Same libraries and steps as the Vaadin 8 connector (networkDiagram-connector.js).
 *
 * The three libraries are classic scripts that define globals (vis, HTMLWidgets) and register the visNetwork binding,
 * so they run as classic scripts, in the Vaadin 8 order, once.
 */
import 'vis-network/styles/vis-network.min.css';
import visNetworkSource from 'vis-network/standalone/umd/vis-network.min.js?raw';
import htmlwidgetsSource from './htmlwidgets-1.5.1/htmlwidgets.js?raw';
import visNetworkBindingSource from './visNetwork-binding-2.0.9/visNetwork.js?raw';

let librariesLoaded = false;

function loadLibraries() {
  if (librariesLoaded) {
    return;
  }
  [visNetworkSource, htmlwidgetsSource, visNetworkBindingSource].forEach((source) => {
    const script = document.createElement('script');
    script.textContent = source;
    document.head.appendChild(script);
  });
  librariesLoaded = true;
}

function randomString(length, chars) {
  let result = '';
  for (let i = length; i > 0; --i) {
    result += chars[Math.floor(Math.random() * chars.length)];
  }
  return result;
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.sormasNetworkDiagram = {
  update(container, jsonData) {
    loadLibraries();

    if (!container.__htmlwidgetId) {
      container.__htmlwidgetId = `htmlwidget-${randomString(20, '0123456789abcdef')}`;
      container.classList.add('htmlwidget_container');
    }
    const htmlwidgetId = container.__htmlwidgetId;

    if (jsonData == null) {
      container.innerHTML = '<div class="v-label v-label-h2 h2">No data available</div>';
      return;
    }

    container.innerHTML = '';

    const el = document.createElement('div');
    el.id = htmlwidgetId;
    el.classList.add('visNetwork', 'html-widget');
    el.style.cssText = 'width:100%; height:100%';
    container.appendChild(el);

    const jsonEl = document.createElement('script');
    jsonEl.setAttribute('type', 'application/json');
    jsonEl.setAttribute('data-for', htmlwidgetId);
    jsonEl.textContent = jsonData;
    container.appendChild(jsonEl);

    const sizingEl = document.createElement('script');
    sizingEl.setAttribute('type', 'htmlwidget-sizing');
    sizingEl.setAttribute('data-for', htmlwidgetId);
    sizingEl.textContent =
      '{"viewer":{"width":"100%","height":"100%","padding":15,"fill":false},"browser":{"width":"100%","height":"100%","padding":40,"fill":false}}';
    container.appendChild(sizingEl);

    window.HTMLWidgets.staticRender();
  }
};
