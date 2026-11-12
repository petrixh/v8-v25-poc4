/*
 * SORMAS HighChart (de.symeda.sormas.ui.highcharts.HighChart) on Vaadin Charts.
 *
 * The chart users build their Highcharts options as JavaScript that defines `var options = {...}` (Vaadin 8 connector:
 * highcharts-connector.js). This evaluates that code and gives the options to the <vaadin-chart> element as its whole
 * configuration (updateConfiguration with reset), so the result is the same chart as with plain Highcharts.
 */
// The Highcharts instance that @vaadin/charts loads (same module), so that the code can read `Highcharts` as before
import Highcharts from 'highcharts/es-modules/masters/highstock.src.js';
// "Download CSV" and "Download XLS" in the export menu (Highcharts export-data module; Vaadin 8 loaded it too). It adds
// its menu items only when the exporting module is already loaded, so load that first (vaadin-chart loads it too).
import 'highcharts/es-modules/masters/modules/exporting.src.js';
import 'highcharts/es-modules/masters/modules/export-data.src.js';

// Highcharts 8 default series colours: the charts that set no colours look as before (Highcharts 12 has another palette)
const HIGHCHARTS_8_COLORS = ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9', '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1'];

function evaluateOptions(hcjs) {
  // The code declares `var options` and may read `Highcharts` (e.g. Highcharts.theme), so it gets Highcharts as a parameter
  // (the bundler renames module variables, so eval could not see the import)
  // eslint-disable-next-line no-new-func
  return new Function('Highcharts', `${hcjs}\n;return options;`)(Highcharts);
}

// Vaadin 8 drew the chart in the page, so the SORMAS colour classes that the options give a point as `className` (e.g. the
// disease colours of CssStyles.getDiseaseColor, which set the CSS `fill`) coloured it. The chart is now drawn inside the
// shadow root of <vaadin-chart>, which the page's CSS does not reach: such a point gets the fill of its classes as `color`.
function applyClassNameColors(options) {
  const series = Array.isArray(options.series) ? options.series : [];
  let probe;
  let plainFill;
  series.forEach((serie) => {
    (Array.isArray(serie.data) ? serie.data : []).forEach((point) => {
      if (!point || typeof point !== 'object' || !point.className || point.color !== undefined) {
        return;
      }
      if (!probe) {
        probe = document.createElement('div');
        probe.style.display = 'none';
        document.body.appendChild(probe);
        plainFill = getComputedStyle(probe).fill;
      }
      probe.className = point.className;
      const fill = getComputedStyle(probe).fill;
      if (fill && fill !== plainFill) {
        point.color = fill;
      }
    });
  });
  if (probe) {
    probe.remove();
  }
}

window.Vaadin = window.Vaadin || {};
window.Vaadin.sormasHighChart = {
  update(chart, hcjs, functionNames) {
    // Browser functions that menu items and other option callbacks call (HighChart.addJsFunction)
    (functionNames ? functionNames.split(',') : []).forEach((name) => {
      window[name] = () => chart.$server.runJsFunction(name);
    });

    if (!hcjs) {
      return;
    }
    const options = evaluateOptions(hcjs);
    // Highcharts colours from the options, as in Vaadin 8 (Vaadin Charts' default is styled mode with the theme's colours)
    options.chart = { styledMode: false, ...options.chart };
    if (!options.colors) {
      options.colors = HIGHCHARTS_8_COLORS;
    }
    applyClassNameColors(options);
    // A chart without data says so, as in Vaadin 8 (Highcharts' default text, English in every language; Vaadin Charts'
    // default is blank). In the options, not through <vaadin-chart>'s emptyText, which redraws every new configuration.
    options.lang = { noData: 'No data to display', ...options.lang };
    chart.updateConfiguration(options, true);
  }
};

export { Highcharts };
