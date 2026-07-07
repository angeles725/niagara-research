/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/LabelsLayer
 */
define(['log!nmodule.webChart.rc.line.LabelsLayer', 'd3', 'jquery', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/model/modelUtil', 'bajaux/events', 'nmodule/webChart/rc/menu/contextMenuUtil'], function (log, d3, $, webChartUtil, modelUtil, events, contextMenuUtil) {
  "use strict";

  var logSevere = log.severe.bind(log);
  /**
   * LabelsLayer organizes the labels around a chart.
   * @class
   */

  var LabelsLayer = function LabelsLayer(graph) {
    this.$graph = graph;
    this.$redrawRequested = false; //prevent 3+ redraw requests in a row
  };
  /**
   * Initialize the Labels.
   *
   * @param {webChart.SvgGraph} graph
   */


  LabelsLayer.prototype.initialize = function () {
    var graph = this.$graph,
        chart = graph.chartd3(),
        labels = chart.select('g.graphLabels'),
        xAxisLabel = labels.append('g').attr('class', 'xAxisLabel');
    xAxisLabel.append('text').attr('class', 'period');
    xAxisLabel.append('text').attr('class', 'sampling');
    labels.append('g').attr('class', 'yAxisLabel primary');
    labels.append('g').attr('class', 'yAxisLabel secondary');
    var primaryTicks = chart.select(".primary-y-ticks"),
        secondaryTicks = chart.select(".secondary-y-ticks");
    contextMenuUtil.registerContextMenu(primaryTicks, function () {
      return contextMenuUtil.scaleContextMenu(graph, primaryTicks, graph.widget().model().primaryValueScale());
    });
    contextMenuUtil.registerContextMenu(secondaryTicks, function () {
      return contextMenuUtil.scaleContextMenu(graph, secondaryTicks, graph.widget().model().secondaryValueScale());
    });
  };

  LabelsLayer.prototype.primaryValueScaleData = function () {
    var that = this,
        graph = that.$graph;
    return [graph.widget().model().primaryValueScale()];
  };

  LabelsLayer.prototype.secondaryValueScaleData = function () {
    var that = this,
        model = that.$graph.widget().model(),
        primary = model.primaryValueScale();
    return model.valueScales().filter(function (valueScale) {
      return valueScale !== primary && valueScale.requiresTicks();
    });
  };
  /**
   * Respond to changes that require re-graphing of the data.
   */


  LabelsLayer.prototype.graphData = function () {
    var that = this,
        graph = that.$graph,
        chart = graph.chartd3(),
        primaryData,
        secondaryData,
        model = graph.widget().model();
    primaryData = chart.select('.graphLabels .yAxisLabel.primary').selectAll('text').data(that.primaryValueScaleData());
    secondaryData = chart.select('.graphLabels .yAxisLabel.secondary').selectAll('text').data(that.secondaryValueScaleData());
    var primaryEnter = primaryData.enter().append("text").text(function (d) {
      return d.displayName();
    }).attr('transform', 'rotate(-90)').on("click", function (d, i) {
      //fire off a modify event for the primaryValueScale
      model.primaryValueScale(d);
    });
    var secondaryEnter = secondaryData.enter().append("text").text(function (d) {
      return d.displayName();
    }).attr('transform', 'rotate(-90)').on("click", function (d, i) {
      //fire off a modify event for the secondaryValueScale
      model.secondaryValueScale(d);
    }).each(function (d) {
      if (model.secondaryValueScale() !== d) {
        graph.tipLayer().addTip({
          anchor: $(d3.select(this).node()),
          key: "valueScaleTip"
        });
      }
    });

    var handleContextMenu = function handleContextMenu(d) {
      return contextMenuUtil.scaleContextMenu(graph, d3.select(this), d);
    };

    contextMenuUtil.registerContextMenu(primaryEnter, handleContextMenu);
    contextMenuUtil.registerContextMenu(secondaryEnter, handleContextMenu);
    secondaryData.exit().remove();
  };

  LabelsLayer.prototype.redraw = function () {
    var that = this,
        graph = that.$graph,
        chart = graph.chartd3(),
        widget = graph.widget(),
        model = widget.model(),
        dim = graph.widget().dimensions(),
        yAxisLabelWidthPrimary,
        yAxisLabelWidthSecondary,
        extraMargin = 20,
        neededMargin,
        xTrans,
        xTrans2,
        yAxisWidthPrimary = 0,
        yAxisWidthSecondary = 0,
        movementNeeded = false,
        yLabelWidth = 0,
        yLabelWidth2 = 0,
        yLabels = chart.select('.graphLabels .yAxisLabel.primary').selectAll('text'),
        yLabels2 = chart.select('.graphLabels .yAxisLabel.secondary').selectAll('text'),
        primaryValueScale = model.primaryValueScale(),
        secondaryValueScale = model.secondaryValueScale(),
        xAxisLabel;
    chart.select('rect.primary-y-ticks').classed('locked', model.primaryValueScale().isLocked());
    chart.select('rect.secondary-y-ticks').classed('locked', model.secondaryValueScale() !== null ? model.secondaryValueScale().isLocked() : false);

    function labelAttributes(d, elem, selected) {
      var color = "#cccccc"; //disabled looking grey;

      if (d === selected) {
        elem.style('cursor', 'default');

        if (d.seriesList().length > 0) {
          color = d.seriesList()[0].color();
        }
      } else {
        elem.style('cursor', 'pointer');
      }

      elem.attr('fill', color);
      d.unitQualityName().then(function (unitQualityName) {
        elem.append("svg:title").text(webChartUtil.toFriendly(unitQualityName) + " (" + webChartUtil.toFriendly(d.unitDescription()) + ")");
      })["catch"](logSevere);
    }

    xAxisLabel = chart.select('.graphLabels .xAxisLabel').attr('transform', 'translate(' + dim.width / 2 + ',' + dim.height + ')');
    xAxisLabel.select('text.period').text(graph.getPeriodDisplay()) //TODO: Timezone
    .attr('dy', '3em').attr("cursor", function (d, i) {
      //fire off a modify event for the Period Selector
      if (model.timeRange().getPeriod().getOrdinal() === 1) {
        return "pointer";
      } else {
        return "default";
      }
    }).on("click", function (d, i) {
      //fire off a modify event for the Period Selector
      if (model.timeRange().getPeriod().getOrdinal() === 1) {
        widget.jq().find(".js-tab-webChartTimeRange").trigger(events.MODIFY_EVENT);
      }
    });
    var samplingText = "",
        samplingPeriod;

    if (model.isSampling() || modelUtil.hasBar(model.seriesList())) {
      samplingPeriod = model.samplingPeriod();

      if (samplingPeriod) {
        samplingText = "[" + model.samplingPeriod() + "]";
      }
    }

    xAxisLabel.select('text.sampling').attr('dx', webChartUtil.getWidth(xAxisLabel.select('text.period').node()) / 2 + 10).attr('dy', '3em').on("click", function () {
      widget.settingsCommand().invoke();
      setTimeout(function () {
        $('.js-tab-title-' + webChartUtil.safeClassName(webChartUtil.lex.get("webChart.settingsCommand.samplingTabTitle"))).click();
      }, 100);
    }).text(samplingText);
    yLabels.text(function (d) {
      return d.displayName();
    }).attr('transform', function (d) {
      var elem = d3.select(this),
          previousWidth = yLabelWidth;
      yLabelWidth += webChartUtil.getWidth(elem.node()) + 15;
      return 'rotate(-90) translate(' + previousWidth + ')';
    }).each(function (d, i) {
      labelAttributes(d, d3.select(this), primaryValueScale);
    });
    yLabels2.text(function (d) {
      return d.displayName();
    }).attr('transform', function (d) {
      var elem = d3.select(this),
          previousWidth = yLabelWidth2;
      yLabelWidth2 += webChartUtil.getWidth(elem.node()) + 15;
      return 'rotate(-90) translate(' + previousWidth + ')';
    }).each(function (d, i) {
      labelAttributes(d, d3.select(this), secondaryValueScale);
    }); //determine max width for primary y-axis labels

    chart.selectAll('.y-axis.primary').each(function (d, i) {
      var width = webChartUtil.getWidth(this);

      if (width > yAxisWidthPrimary) {
        yAxisWidthPrimary = width;
      }
    }); //determine max width for secondary-axis labels

    chart.selectAll('.y-axis.secondary').each(function (d, i) {
      var width = webChartUtil.getWidth(this);

      if (width > yAxisWidthSecondary) {
        yAxisWidthSecondary = width;
      }
    }); //now we are ready to set the mouse over width based on the text size

    var left = graph.isYAxisOrientLeft();
    chart.selectAll('rect.primary-y-ticks').attr('x', left ? -yAxisWidthPrimary - 10 : dim.width).attr("width", yAxisWidthPrimary + 10);
    chart.selectAll('rect.secondary-y-ticks').attr('x', left ? dim.width : -yAxisWidthSecondary - 10).attr("width", yAxisWidthSecondary ? yAxisWidthSecondary + 10 : 0);
    yAxisLabelWidthPrimary = webChartUtil.getWidth(d3.select(".yAxisLabel.primary").node());
    yAxisLabelWidthSecondary = webChartUtil.getWidth(d3.select(".yAxisLabel.secondary").node()); //place Y Axis unit labels in center

    xTrans = -yAxisWidthPrimary - extraMargin + 10;

    if (!graph.isYAxisOrientLeft()) {
      xTrans = yAxisWidthPrimary + dim.width + extraMargin;
    }

    xTrans2 = -yAxisWidthSecondary - extraMargin + 10;

    if (graph.isYAxisOrientLeft()) {
      xTrans2 = yAxisWidthSecondary + dim.width + extraMargin;
    }

    chart.select('.graphLabels .yAxisLabel.primary').attr('transform', 'translate(' + xTrans + ' ' + (dim.height + yLabelWidth) / 2 + ')');
    chart.select('.graphLabels .yAxisLabel.secondary').attr('transform', 'translate(' + xTrans2 + ' ' + (dim.height + yLabelWidth2) / 2 + ')'); //change margins as need for unit labels

    neededMargin = yAxisWidthPrimary + yAxisLabelWidthPrimary + extraMargin;

    if (neededMargin > widget.margin.right && !graph.isYAxisOrientLeft()) {
      widget.margin.right = neededMargin;
      movementNeeded = true;
    } else if (neededMargin > widget.margin.left && graph.isYAxisOrientLeft()) {
      widget.margin.left = neededMargin;
      movementNeeded = true;
    }

    neededMargin = yAxisWidthSecondary + yAxisLabelWidthSecondary + extraMargin;

    if (neededMargin > widget.margin.left && !graph.isYAxisOrientLeft()) {
      widget.margin.left = neededMargin;
      movementNeeded = true;
    } else if (neededMargin > widget.margin.right && graph.isYAxisOrientLeft()) {
      widget.margin.right = neededMargin;
      movementNeeded = true;
    } //allow a second consecutive redraw request to handle dimensional changes, but no more.


    if (movementNeeded && !that.$redrawRequested) {
      graph.widget().jq3().select('.Line').attr('transform', 'translate(' + graph.widget().margin.left + ',' + graph.widget().margin.top + ')');
      that.$redrawRequested = true;
      widget.redrawSoon(); //required for dimensional changes
    } else {
      that.$redrawRequested = false;
    }
  };

  return LabelsLayer;
});
