/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */
define(['d3', 'jquery', 'baja!', 'bajaux/Widget', 'nmodule/webChart/rc/menu/MenuWidget', 'bajaux/commands/Command', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/command/SetAxisValuesCommand', 'nmodule/webChart/rc/command/LockAxisCommand', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/tab/Tab', 'nmodule/webChart/rc/chartEvents', 'nmodule/webEditors/rc/util/htmlUtils', 'baja!' + 'gx:Color'], function (d3, $, baja, Widget, MenuWidget, Command, webChartUtil, SetAxisValuesCommand, LockAxisCommand, modelUtil, Tab, chartEvents, htmlUtils) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A set of utility functions for the series right click menu
   *
   * @exports nmodule/webChart/rc/menu/seriesContextMenu
   */

  var contextMenuUtil = {};
  /**
   * scaleContextMenu handles what happens when a unit label gets right clicked
   * @param {webChart.LineGraph} graph
   * @param {d3} elem from right click
   * @param {module:nmodule/webChart/rc/model/ValueScale} valueScale scale from right click
   */

  contextMenuUtil.scaleContextMenu = function (graph, elem, valueScale) {
    var widget = graph.widget(),
        model = widget.model(),
        div = $('<div></div>'),
        menu = new MenuWidget(),
        valueScales = model.valueScales();
    var hasSeries = false,
        i;

    for (i = 0; i < valueScales.length; i++) {
      if (valueScales[i].seriesList().length > 0) {
        hasSeries = true;
      }
    } //no dialog for no scale


    if (!hasSeries) {
      d3.event.preventDefault();
      return false;
    }

    this.positionDiv(div);
    var removeCommand = new Command({
      module: "webChart",
      lex: "webChart.removeFromChart",
      func: function func() {
        var seriesList = valueScale.seriesList(),
            i;

        for (i = seriesList.length - 1; i >= 0; i--) {
          model.removeSeries(seriesList[i]);
        }

        widget.jq().trigger(chartEvents.FILE_MODIFY_EVENT);
        model.reloadAll(widget.getSubscriber());
      }
    });
    var setAsPrimaryCommand = new Command({
      module: "webChart",
      lex: "webChart.setAsPrimary",
      func: function func() {
        model.secondaryValueScale(null);
        model.primaryValueScale(valueScale);
        widget.graphData(model.seriesList());
        widget.jq().trigger(chartEvents.FILE_MODIFY_EVENT);
      }
    });
    menu.addCommand(new SetAxisValuesCommand(widget, valueScale));
    menu.addCommand(new LockAxisCommand(widget, valueScale));

    if (valueScale !== model.primaryValueScale()) {
      menu.addCommand(setAsPrimaryCommand);
    }

    menu.addCommand(removeCommand);
    menu.initialize(div).then(function () {
      return menu.load(valueScale);
    })["catch"](baja.error);
    d3.event.preventDefault();
    return false; //prevent default context menu
  };
  /**
   * seriesContextMenu handles what happens when a series gets right clicked
   * @param {LineGraph} graph
   * @param {d3} elem from right click
   * @param {BaseSeries} series from right click
   */


  contextMenuUtil.seriesContextMenu = function (graph, elem, series) {
    var widget = graph.widget(),
        model = widget.model(),
        div = $('<div></div>'),
        menu = new MenuWidget(),
        valueScale = series.valueScale();
    this.positionDiv(div); //show Legend Entry Display Name

    var titleWidget = new Widget();

    titleWidget.doLoad = function () {
      var jq = this.jq(),
          svg = $('<svg width="2.2em" height="1.5em"></svg>'),
          group = $('<g></g>'),
          span = $('<span></span>'),
          unitSymbolText = series.valueScale().unitSymbol(),
          d3Group;
      jq.addClass("webChart-contextMenuTitle");
      jq.css("color", graph.dataLayer().fillColor(series).encodeToString());

      if (unitSymbolText) {
        unitSymbolText = " (" + unitSymbolText + ") ";
      }

      svg.append(group);
      jq.append(svg);
      jq.append(span);
      span.text(series.shortDisplayName() + unitSymbolText);
      d3Group = d3.select(svg[0]);
      d3Group.data([series]);
      contextMenuUtil.seriesEnableBox(graph, d3Group, series);
      jq.on("click", function () {
        contextMenuUtil.toggleEnabled(graph, series);
      });
    };

    menu.addTab(new Tab({
      item: titleWidget,
      title: "displayName"
    }));
    menu.addCommand(new SetAxisValuesCommand(widget, valueScale));
    var removeCommand = new Command({
      module: "webChart",
      lex: "webChart.removeFromChart",
      func: function func() {
        model.removeSeries(series);
        widget.jq().trigger(chartEvents.FILE_MODIFY_EVENT);
        model.reloadAll(widget.getSubscriber());
      }
    });
    menu.addCommand(removeCommand);
    var removeOthersCommand = new Command({
      module: "webChart",
      lex: "webChart.removeOthers",
      func: function func() {
        model.removeOtherSeries(series);
        widget.jq().trigger(chartEvents.FILE_MODIFY_EVENT);
        model.reloadAll(widget.getSubscriber());
      }
    });
    menu.addCommand(removeOthersCommand);
    var setAsPrimaryCommand = new Command({
      module: "webChart",
      lex: "webChart.setAsPrimary",
      func: function func() {
        var originalTimeZone = model.timeScale().getTimeZone(); //a particular series is now primary, so re-order the list
        //since sort changes indexes, indexes must be cached for sort

        modelUtil.sortSeriesList(model, model.seriesList(), null, series); //ensure the valueScale array sorting is consistent

        modelUtil.sortSeriesList(model, series.valueScale().seriesList(), null, series); //handle changing the valueScales

        if (valueScale !== model.primaryValueScale()) {
          model.secondaryValueScale(null);
          model.primaryValueScale(valueScale);
        }

        widget.jq().trigger(chartEvents.FILE_MODIFY_EVENT);

        if (originalTimeZone !== model.timeScale().getTimeZone()) {
          return model.reloadAll();
        } else {
          return widget.graphData(model.seriesList());
        }
      }
    });

    if (valueScale !== model.primaryValueScale() || series !== model.seriesList()[0]) {
      menu.addCommand(setAsPrimaryCommand);
    }

    menu.initialize(div).then(function () {
      return menu.load(series);
    })["catch"](baja.error);
    d3.event.preventDefault();
    return false; //prevent default context menu
  };

  contextMenuUtil.positionDiv = function (div) {
    var windowWidth = $(window).width(),
        clientX = d3.event.clientX;

    if (windowWidth - clientX < 175) {
      //TODO: formalize this
      div.css({
        top: d3.event.clientY,
        right: 0
      });
    } else {
      div.css({
        top: d3.event.clientY,
        left: d3.event.clientX
      });
    }

    div.addClass("titleEdit").appendTo('body');
  };

  contextMenuUtil.seriesEnableBox = function (graph, selection, series) {
    var dataLayer = graph.dataLayer();
    selection.append('rect').attr('height', '.9em').attr('width', '.9em').attr('rx', 3).attr('ry', 3).attr('x', '.80em').attr('y', '.50em').style('stroke-width', '1.5').style('stroke', function (d) {
      return dataLayer.fillColor(d);
    }).style('fill', function (d) {
      if (d.isEnabled()) {
        return dataLayer.fillColor(d);
      } else {
        return "#ffffff";
      }
    }); //inner border for legend rect

    selection.append('rect').attr('height', '0.8em').attr('width', '0.8em').attr('rx', 3).attr('ry', 3).attr('x', '0.85em').attr('y', '.55em').style('stroke', "#ffffff").style('fill-opacity', 0).style('stroke-width', "1.5");
    selection.append('text').style('fill', function (d) {
      return dataLayer.fillColor(d);
    });
    selection.append("svg:title").text(function (d) {
      return "Toggle " + d.displayName(); //TODO: lexicon
    });
  };
  /**
   * This provides a registration for contextmenu that still works in JavaFx with the context menu disabled
   * @param d3Elem
   * @param callback that takes a standard data
   */


  contextMenuUtil.registerContextMenu = function (d3Elem, callback) {
    //Only JavaFx outside of webstart has contextMenu disabled
    if (webChartUtil.isJavaFx() && !webChartUtil.isWebStart()) {
      d3Elem.on("mousedown.javaFxContextMenu", function (d, i) {
        d3Elem.attr("lastMouse", d3.event.button);
      }).on("mouseup.javaFxContextMenu", function (d, i) {
        if (d3Elem.attr("lastMouse") === "2") {
          return callback(d, i);
        } else {
          return true; //don't interrupt
        }
      });
    } else {
      d3Elem.on("contextmenu", function (d, i) {
        return callback(d, i);
      });
    }

    htmlUtils.contextMenuOnLongPress($(d3Elem.node()), {
      'nativeEvents': true
    });
  };
  /**
   * Toggle Enabled handles what happens when a legend is clicked
   * @param {LineGraph} graph
   * @param {d3} elem from click
   * @param {BaseSeries} series from click
   */


  contextMenuUtil.toggleEnabled = function (graph, series) {
    var chart = graph.chartd3(),
        dataLayer = graph.dataLayer(),
        selection; //toggle enabled

    series.setEnabled(!series.isEnabled()); //fix opacity for status circles and lines

    chart.selectAll('.lineContainer .dataSet').each(function (dataSet, dataIndex) {
      var opacity = dataSet.isEnabled() ? 1 : 0;
      d3.select(this).selectAll("circle").transition().duration(1000).style('fill-opacity', opacity);
      d3.select(this).selectAll("line").transition().duration(1000).style('stroke-opacity', opacity);
    }); // ensure the fill is set in case it changed while a graph was hidden

    chart.select('.lineContainer').selectAll('.dataSet').style('fill', function (d) {
      return dataLayer.fillColor(d);
    }); //fix colors for paths

    chart.select('.lineContainer').selectAll('.dataSet').transition().duration(1000).selectAll("path").style('stroke-opacity', function (d) {
      return d.isEnabled() ? 1 : 0;
    }).style('fill-opacity', function (d, i) {
      return d.isEnabled() ? dataLayer.getFillOpacity(d, i) : 0;
    }); // animate the bar chart

    chart.select('.lineContainer').selectAll('.dataSet').each(function (dataSet) {
      var opacity = dataSet.isEnabled() ? 1 : 0;
      d3.select(this).selectAll("rect,line").transition().duration(1000).style('opacity', opacity);
    }); //fix colors for legend

    selection = graph.chartd3().select('.legendGroup').selectAll('.legendEntry'); // ensure the color is set in case it changed while the legend was hidden

    selection.select('rect').style('fill', function (d) {
      return dataLayer.fillColor(d);
    });
    selection.transition().duration(1000).select('rect').style('fill-opacity', function (d) {
      return d.isEnabled() ? 1 : 0;
    });
  };

  return contextMenuUtil;
});
