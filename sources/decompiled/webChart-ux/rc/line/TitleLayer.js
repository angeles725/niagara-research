/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/TitleLayer
 */
define(['d3', 'jquery', 'Promise', 'lex!'], function (d3, $, Promise, lex) {
  "use strict";
  /**
   * TitleLayer organizes the title on a chart.
   *
   * @class
   * @param {Line}  graph the LineGraph
   */

  var TitleLayer = function TitleLayer(graph) {
    this.$graph = graph;
    this.$enabled = true;
    this.$editing = false;
  };
  /**
   * Initialize the Title by just creating a group
   */


  TitleLayer.prototype.initialize = function () {
    var graph = this.$graph,
        chart = graph.chartd3(); //append the title Group

    chart.append('g').attr('class', 'titleGroup');
  };
  /**
   * Resolve the current title for this widget and whether or not the title is readonly
   * @param {ChartWidget} widget
   * @returns {Promise} which resolves to an Object {title: (String), readonly: (boolean)}
   */


  TitleLayer.resolveTitleInfo = function (widget) {
    var propertyTitle = widget.properties().getValue("title", ""),
        widgetTitle = widget.title(),
        title = ""; //title is readonly when specified as a property

    if (propertyTitle) {
      return lex.format(propertyTitle).then(function (result) {
        return {
          title: result,
          readonly: true
        };
      });
    }

    if (widgetTitle) {
      title = widgetTitle;
    } else if (widget.model().seriesList().length > 0) {
      title = widget.model().seriesList()[0].displayName();
    }

    return Promise.resolve({
      title: title,
      readonly: false
    });
  };
  /**
   * Handler for when the title is clicked.
   *
   * @inner
   * @private
   *
   * @param {LineGraph} graph
   * @param {d3} elem from click
   * @param {Object} data from click
   * @param {Number} data index from click
   * @param {TitleLayer} Current Title Layer
   */


  function titleClick(graph, elem, data, index, layer) {
    var widget = graph.widget(),
        margin = widget.margin,
        jq = widget.jq(),
        div = $('<div><input type="text" value="' + data + '"></input></div>'),
        input = div.find("input");
    elem.style('fill-opacity', '0');
    div.addClass("titleEdit").css({
      top: margin.top - 10,
      left: margin.left - 8
    });
    jq.append(div);
    input.focus();

    function changeTitle() {
      var val = input.val();
      graph.widget().title(val);
      div.remove();
      TitleLayer.resolveTitleInfo(widget).then(function (titleInfo) {
        elem.style('fill-opacity', '1');
        elem.data([titleInfo.title]);
        elem.select("text").text(function (d) {
          return d;
        });
      })["finally"](function () {
        layer.$editing = false;
      });
    }

    input.blur(function () {
      changeTitle();
    });
    input.keypress(function (e) {
      if (e.which === 13) {
        //enter
        changeTitle();
        return false;
      }
    });
    input.keyup(function (e) {
      if (e.which === 27) {
        //escape
        div.remove();
        elem.style('fill-opacity', '1');
        layer.$editing = false;
      }
    });
  }
  /**
   * Redraw the Title based on the TitleInfo
   * @param {TitleLayer} layer
   * @param {{title: String, readonly: Boolean}} titleInfo
   * @inner
   */


  function redrawTitle(layer, titleInfo) {
    var graph = layer.$graph,
        chart = graph.chartd3(),
        data = chart.select('.titleGroup').selectAll('.titleDisplay').data([titleInfo.title]),
        enter = data.enter(),
        selection;
    data.exit().transition().duration(1000).style('fill-opacity', '0').remove();
    data.select("text").text(function (d) {
      return d;
    }); // If a new group is needed, create it.

    selection = enter.insert('g').attr('class', 'titleDisplay').style('cursor', titleInfo.readonly ? 'default' : 'pointer').on("click", function (d, i) {
      if (!titleInfo.readonly && !layer.$editing) {
        layer.$editing = true;
        titleClick(graph, d3.select(this), d, i, layer);
      }
    });
    selection.append('text').text(function (d) {
      return d;
    }); //position the legend

    chart.selectAll('.titleGroup').attr('transform', 'translate(' + 0 + ' ' + -10 + ')');
  }
  /**
   * Redraw handles the Title layout
   */


  TitleLayer.prototype.redraw = function () {
    var that = this,
        widget = that.$graph.widget();
    return TitleLayer.resolveTitleInfo(widget).then(function (titleInfo) {
      redrawTitle(that, titleInfo);
    });
  };
  /**
   * Return true if enabled.
   *
   * @returns {Boolean}
   */


  TitleLayer.prototype.isEnabled = function () {
    return this.$enabled;
  };
  /**
   * Set whether the title is enabled or not.
   *
   * @returns {Boolean}
   */


  TitleLayer.prototype.setEnabled = function (enabled) {
    var that = this;

    if (that.$enabled !== enabled) {
      that.$enabled = enabled;
      that.$graph.widget().jq().find('.titleGroup').toggle(enabled);
    }
  };
  /**
   * Return the layer's name.
   *
   * @returns {String}
   */


  TitleLayer.prototype.name = function () {
    return "Title";
  };

  return TitleLayer;
});
