/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/LegendLayer
 */
define(['d3', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/menu/contextMenuUtil'], function (d3, webChartUtil, events, contextMenuUtil) {
  "use strict";
  /**
   * LegendLayer organizes the legend on a chart.
   *
   * @class
   * @param {Line}  graph the LineGraph
   */

  var LegendLayer = function LegendLayer(graph) {
    this.$graph = graph;
    this.$enabled = true;
  };
  /**
   * Initialize the Legend, register single onclick event for all legend entries, listen for focus and blur events
   */


  LegendLayer.prototype.initialize = function () {
    var graph = this.$graph,
        chart = graph.chartd3(),
        jq = graph.widget().jq(),
        chartWidth = jq.closest(":visible").width();

    if (!chartWidth) {
      chartWidth = 0;
    } //append the Legend Group


    chart.append('g').attr('class', 'legendGroup').attr('transform', 'translate(' + chartWidth + ',  -10)');
    jq.on(events.SERIES_FOCUS + " " + events.SERIES_BLUR, function (event, series, plotSize) {
      plotSize = plotSize || 0; // Make the selected legend bold on selection

      chart.selectAll(".legendEntry text").style("font-weight", function () {
        return plotSize > 1 && series && series.shortDisplayName() === this.textContent ? "bold" : null;
      }); //this does NOT work in javafx, but it seems to look the best
      //        .style("text-decoration", function () {
      //          return plotSize > 1 && series && series.shortDisplayName() === this.textContent ? "underline" : null;
      //        });
      //color is now based on lineColor
      //        .style("fill", function () {
      //          return plotSize > 1 && series && series.shortDisplayName() === this.textContent ? "#3d3d3d" : null;
      //        });
    });
  };
  /**
   * Legend is now always required.
   * @param graph
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries>}
   * @inner
   */


  function getLegendData(graph) {
    return graph.data();
  }
  /**
   * graph data registers the data agaisnt the legend group
   */


  LegendLayer.prototype.graphData = function () {
    var that = this,
        graph = that.$graph;
    var selection = graph.chartd3().select('.legendGroup').selectAll('.legendEntry').data(getLegendData(graph)); // Give each legend a color

    selection.select('rect').style('stroke', function (d) {
      return graph.dataLayer().fillColor(d);
    }).style('fill', function (d) {
      if (d.isEnabled()) {
        return graph.dataLayer().fillColor(d);
      } else {
        return "#ffffff";
      }
    });
    var enter = selection.enter(),
        exit = selection.exit();
    exit.selectAll("text").transition().duration(1000).style('fill-opacity', function () {
      return 0;
    }).style('stroke-opacity', function () {
      return 0;
    });
    exit.selectAll("rect").transition().duration(1000).style('fill-opacity', function () {
      return 0;
    }).style('stroke-opacity', function () {
      return 0;
    });
    exit.transition().each("end", function (d, i) {
      d3.select(this).remove();
      that.redraw();
    }).duration(900); // If a new group is needed, create it.

    var dataSet = enter.insert('g').attr('class', 'legendEntry');

    var handleContextMenu = function handleContextMenu(d) {
      return contextMenuUtil.seriesContextMenu(graph, d3.select(this), d);
    };

    contextMenuUtil.registerContextMenu(dataSet, handleContextMenu);
    dataSet.style('cursor', 'pointer').on("click", function (d, i) {
      contextMenuUtil.toggleEnabled(graph, d);
    }).append('rect').attr('height', '.9em').attr('width', '.9em').attr('rx', 3).attr('ry', 3).attr('x', "-1.25em").attr('y', "-.75em").style('stroke-width', "1.5").style('stroke', function (d) {
      return graph.dataLayer().fillColor(d);
    }).style('fill', function (d) {
      if (d.isEnabled()) {
        return graph.dataLayer().fillColor(d);
      } else {
        return "#ffffff";
      }
    }); //inner border for legend rect

    dataSet.append('rect').attr('height', '0.8em').attr('width', '0.8em').attr('rx', 3).attr('ry', 3).attr('x', '-1.20em').attr('y', '-0.70em').style('stroke', "#ffffff").style('fill-opacity', 0).style('stroke-width', "1.5");
    dataSet.append('text').style('fill', function (d, i) {
      return graph.dataLayer().fillColor(d);
    });
    dataSet.append("svg:title").text(function (d) {
      return webChartUtil.lex.get("webChart.toggle", d.displayPath());
    });
  };
  /**
   * redraw handles the layout transformations of the legend based on the available space
   */


  LegendLayer.prototype.redraw = function () {
    var that = this,
        graph = that.$graph,
        chart = graph.chartd3(),
        dim = graph.widget().dimensions(),
        end,
        width = 0,
        margin = 10,
        maxWidth = webChartUtil.getWidth(chart.select('.line-container-overlay-group').node()),
        titleWidth = webChartUtil.getWidth(chart.select('.titleGroup').node()),
        legendGroup = chart.select('.legendGroup'),
        selection = chart.selectAll(".legendEntry");

    if (titleWidth > 0) {
      titleWidth += 20;
    } //ensure title and legend do not overlap


    maxWidth = maxWidth - titleWidth;
    selection.select("text").style('fill', function (d) {
      return graph.dataLayer().fillColor(d);
    });
    selection.select("title").text(function (d, i) {
      return webChartUtil.lex.get("webChart.toggle", d.displayPath());
    });

    if (!legendGroup.attr("hideText")) {
      selection.select("text").text(function (d) {
        return d.shortDisplayName();
      });
      selection.attr('transform', function (d, i) {
        var prev = width,
            textWidth,
            d3Elem = d3.select(this),
            normalWidth,
            normalText = d3Elem.attr("normalText"),
            newText = d3Elem.select("text").text(); //compensate for bolding changing the width as long as text is the same

        normalWidth = d3Elem.attr("normalWidth");

        if (!normalWidth || !normalText || normalText !== newText) {
          textWidth = webChartUtil.getWidth(this);

          if (textWidth) {
            d3Elem.attr("normalWidth", textWidth);
            d3Elem.attr("normalText", newText);
          }
        } else {
          textWidth = parseInt(normalWidth);
        }

        width = width + textWidth + margin;
        return 'translate(' + prev + ')';
      });

      if (webChartUtil.getWidth(legendGroup.node()) > maxWidth) {
        width = 0;
        legendGroup.attr("hideText", true);
        legendGroup.selectAll("text").text("");
        selection.attr('transform', function (d, i) {
          var prev = width;
          width = width + margin + 15;
          return 'translate(' + prev + ')';
        });
      }
    } else {
      //already hideText
      selection.attr('transform', function (d, i) {
        var prev = width;
        width = width + margin + 15;
        return 'translate(' + prev + ')';
      });
    }

    end = dim.width - width + 28;
    var newTransform = 'translate(' + end + ' ' + -10 + ')',
        lastEnd = parseInt(legendGroup.attr("lastEnd")),
        same = Math.abs(lastEnd - end) <= 1;

    if (end <= 28 || same) {
      return;
    }

    legendGroup.transition().duration(1000).attr('lastEnd', end).attr('transform', newTransform);
  };

  LegendLayer.prototype.isEnabled = function () {
    return this.$enabled;
  };

  LegendLayer.prototype.setEnabled = function (enabled) {
    var oldEnabled = this.$enabled;

    if (oldEnabled === enabled) {
      return;
    }

    this.$enabled = enabled;
    this.$graph.widget().jq().find('.legendGroup').toggle(enabled);
  };

  LegendLayer.prototype.name = function () {
    return "Legend"; //TODO: lexicon
  };

  return LegendLayer;
});
