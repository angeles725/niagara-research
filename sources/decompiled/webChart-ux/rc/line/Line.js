/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/Line
 */
define(['d3', 'Promise', 'jquery', 'nmodule/webChart/rc/line/LegendLayer', 'nmodule/webChart/rc/line/LabelsLayer', 'nmodule/webChart/rc/line/TitleLayer', 'nmodule/webChart/rc/line/XAxisLayer', 'nmodule/webChart/rc/line/YAxisLayer', 'nmodule/webChart/rc/line/ZoomLayer', 'nmodule/webChart/rc/line/DataLayer', 'nmodule/webChart/rc/line/DataPopupLayer', 'nmodule/webChart/rc/line/TipLayer', 'bajaux/commands/ToggleCommand', 'bajaux/events', 'nmodule/webChart/rc/chartEvents', 'css!nmodule/webChart/rc/line/LineStyle', 'baja!baja:Month'], function (d3, Promise, $, LegendLayer, LabelsLayer, TitleLayer, XAxisLayer, YAxisLayer, ZoomLayer, DataLayer, DataPopupLayer, TipLayer, ToggleCommand, events, chartEvents) {
  "use strict";
  /**
   * A Line Graph for Charting Trend Data
   *
   * @class
   * @alias module:nmodule/webChart/rc/line/Line
   */

  var Line = function Line(chartWidget) {
    var that = this;
    that.$chartWidget = chartWidget;
    that.$y = new YAxisLayer(that);
    that.$x = new XAxisLayer(that); // Compose the chart into a series of Layers.

    that.$layers = [that.$x, that.$y, new LabelsLayer(that), that.$zoomLayer = new ZoomLayer(that), that.$dataLayer = new DataLayer(that), that.$titleLayer = new TitleLayer(that), that.$legendLayer = new LegendLayer(that), that.$dataPopupLayer = new DataPopupLayer(that), that.$tipLayer = new TipLayer(that)];
  };
  /**
   * Return an array of Commands.
   */


  Line.prototype.getCommands = function () {
    var that = this;
    that.$liveCommand = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleLiveCmd",
      selected: !that.widget().model().isLive()
    });
    that.$liveCommand.title = "liveCommand";
    that.$liveCommand.on(events.command.SELECTION_EVENT, function () {
      that.widget().model().setLive(!that.$liveCommand.isSelected());
    });
    return [that.$liveCommand];
  };
  /**
   * Call a function on each layer if its implemented. That that if a layer returns promise, the next layer won't run
   * until the promise is resolved.
   * @param {Line} graph
   * @param {String} name Function to call if its available on the layer.
   * @inner
   * @return {Promise}
   */


  function runLayers(graph, name) {
    var args = Array.prototype.slice.call(arguments, 2),
        layers = graph.$layers;
    return layers.reduce(function (prom, layer) {
      return prom.then(function () {
        if (typeof layer[name] === "function") {
          return layer[name].apply(layer, args);
        }
      });
    }, Promise.resolve());
  }
  /**
   * Creates the DOM element to show the chart
   *
   * @returns {Promise|*}
   */


  Line.prototype.initialize = function (chartd3) {
    var that = this,
        lineContainer,
        lineContainerGroup,
        lineContainerOverlay,
        chartWidget = that.$chartWidget,
        jq = chartWidget.jq(),
        model = chartWidget.model(),
        tickOverlay;
    that.$chartd3 = chartd3;
    chartd3.attr('class', 'Line'); // Create the line-area background rectangle

    chartd3.append('rect').attr('class', 'line-chart-background'); // Create a polyline for the two-sided border

    chartd3.append('polyline').attr('class', 'line-chart-border'); // Create a group to ensure the ticks are behind the lines

    chartd3.append('g').attr('class', 'ticks');
    chartd3.append('g').attr('class', 'ticks2');
    tickOverlay = chartd3.append('g').attr('class', 'tick-overlay');
    tickOverlay.append('rect').attr('class', 'x-ticks');
    tickOverlay.append('rect').attr('class', 'y-ticks primary-y-ticks');
    tickOverlay.append('rect').attr('class', 'y-ticks secondary-y-ticks');
    chartd3.append('g').attr('class', 'graphLabels'); // Create a sub-svg element to mask our lines

    lineContainer = chartd3.append('svg').attr('class', 'lineContainer').attr('top', 0).attr('left', 0);
    lineContainer.append('g').attr('class', 'areas');
    lineContainer.append('g').attr('class', 'bars');
    lineContainer.append('g').attr('class', 'lines');
    lineContainerGroup = lineContainer.append('g').attr('class', 'line-container-overlay-group');
    lineContainerOverlay = lineContainerGroup.append("rect").attr("class", "line-container-overlay").attr("top", 0).attr("left", 0).attr("width", "100%").attr("height", "100%"); //prevent a drag down zoom from refreshing in android for just the lineContainerGroup (this allows space on the white space of the chart to
    //scroll past the ChartWidget down to the next widget

    $(lineContainerOverlay[0][0]).on("touchmove", function (event) {
      event.preventDefault();
      event.stopPropagation();
    }); // Listen to when a Model is live so we can update the Live Command

    jq.on(chartEvents.MODEL_LIVE + " " + chartEvents.MODEL_NOT_LIVE, function () {
      var live = model.isLive() && !model.isStopped();

      if (live) {
        if (!model.isLoading()) {
          //only subscribe if not currently loading, otherwise wait for end of load
          model.subscribe(chartWidget.getSubscriber());
        }

        if (!chartWidget.hasManualZoomed()) {
          chartWidget.toggleAutoZoomCmd.setSelected(true);
        }
      } else {
        model.unsubscribe(chartWidget.getSubscriber()); //unsubscribe right way
      }

      that.$liveCommand.setSelected(!live);
    });
    return runLayers(that, "initialize");
  };
  /**
   * Load new data into the Line
   * @param  {Array} value An array of data sets containing points, names and facets.
   * @return {Promise}
   */


  Line.prototype.graphData = function (value) {
    return runLayers(this, "graphData", value);
  };
  /**
   * Redraw the graph after user-input, such as zooming or panning
   * @return {Promise}
   */


  Line.prototype.redraw = function () {
    var that = this,
        chartd3 = that.$chartd3,
        chartWidget = that.$chartWidget,
        model = chartWidget.model(),
        dimensions = chartWidget.dimensions(); // Resize the plot rectangle

    chartd3.select('.line-chart-background').attr('width', dimensions.width).attr('height', dimensions.height); // Resize the plot-border

    if (model.secondaryValueScale()) {
      chartd3.select('polyline.line-chart-border').attr('points', '0,0 ' + ' 0,' + dimensions.height + ' ' + dimensions.width + ',' + dimensions.height + ' ' + dimensions.width + ',0');
    } else if (that.isYAxisOrientLeft()) {
      chartd3.select('polyline.line-chart-border').attr('points', '0,0 ' + ' 0,' + dimensions.height + ' ' + dimensions.width + ',' + dimensions.height);
    } else {
      chartd3.select('polyline.line-chart-border').attr('points', '0,' + dimensions.height + ' ' + dimensions.width + ',' + dimensions.height + ' ' + dimensions.width + ',0');
    } // Position the line container


    chartd3.select('.lineContainer').attr('width', dimensions.width).attr('height', dimensions.height).attr('viewBox', '0 0 ' + dimensions.width + ' ' + dimensions.height);
    return runLayers(that, "redraw");
  };
  /**
   * Access the X-axis scale
   * @returns {d3.Scale}
   */


  Line.prototype.getScaleX = function () {
    return this.$x.getScale();
  };
  /**
   * Access the Primary Value Scale
   * @returns {d3.Scale}
   */


  Line.prototype.getScaleY = function () {
    var that = this,
        primarySeries = that.$chartWidget.model().primarySeries();

    if (primarySeries) {
      return primarySeries.valueScale().scale();
    }

    return that.widget().model().valueScales()[0].scale();
  };
  /**
   * Return true if Y Axis should be on Left
   * @returns {boolean}
   */


  Line.prototype.isYAxisOrientLeft = function () {
    return this.$chartWidget.settings().getYOrient() === 'left';
  };
  /**
   * Access to Display String for Time Period
   * @returns {String}
   */


  Line.prototype.getPeriodDisplay = function () {
    return this.$x.getPeriodDisplay();
  };
  /**
   * Access to ChartWidget
   * @returns {ChartWidget}
   */


  Line.prototype.widget = function () {
    return this.$chartWidget;
  };
  /**
   * Accessor for d3 selection of ChartWidget's dom
   * @returns {d3}
   */


  Line.prototype.chartd3 = function () {
    return this.$chartd3;
  };
  /**
   * Accessor for model's seriesList
   * @returns {Array.<BaseSeries>}
   */


  Line.prototype.data = function () {
    return this.$chartWidget.model().seriesList();
  };
  /**
   * Return the Layers Array
   * @param i
   * @returns {Array.<Object>}
   */


  Line.prototype.layers = function (i) {
    return this.$layers;
  };
  /**
   * Return the DataLayer
   * @returns {DataLayer}
   */


  Line.prototype.dataLayer = function () {
    return this.$dataLayer;
  };
  /**
   * Return the ZoomLayer
   * @returns {ZoomLayer}
   */


  Line.prototype.zoomLayer = function () {
    return this.$zoomLayer;
  };
  /**
   * Return the TipLayer
   * @returns {TipLayer}
   */


  Line.prototype.tipLayer = function () {
    return this.$tipLayer;
  };
  /**
   * Return the dataPopupLayer
   * @returns {DataPopupLayer}
   */


  Line.prototype.dataPopupLayer = function () {
    return this.$dataPopupLayer;
  };
  /**
   * Return the xAxisLayer
   * @returns {module:nmodule/webChart/rc/line/XAxisLayer}
   */


  Line.prototype.xAxisLayer = function () {
    return this.$x;
  };
  /**
   * Give the layers a chance to destroy if method is implemented
   * @return {Promise}
   */


  Line.prototype.destroy = function () {
    this.$liveCommand.off();
    this.$chartWidget.jq().find(".line-container-overlay").off();
    return runLayers(this, "destroy");
  };

  return Line;
});
