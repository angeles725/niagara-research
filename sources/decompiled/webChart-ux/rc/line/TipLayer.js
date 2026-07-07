/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/TipLayer
 */
define(['d3', 'jquery', 'baja!', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/optionsManager', 'lex!', 'log!nmodule.webChart.rc.line.TipLayer'], function (d3, $, baja, webChartUtil, optionsManager, lex, log) {
  "use strict";

  var logSevere = log.severe.bind(log);
  /**
   * TipLayer organizes the Tips on a chart.
   *
   * A Layer in a Line Chart to drawing tips for the user and repositioning those tips as the anchor elements move around
   *
   * @class
   * @param {Line}  graph the LineGraph
   */

  var TipLayer = function TipLayer(graph) {
    this.$graph = graph;
    this.$tips = [];
  };
  /**
   * See is a tip needs to be shown
   * @param {Object} tip
   * @param {String} tip.key unique key to identify the tip in the webchart Lexicon
   * @param {jQuery} tip.anchor the dom element to anchor the tip over.
   */


  TipLayer.prototype.addTip = function (tip) {
    if (!this.isNeverShow(tip.key)) {
      this.$tips = [tip];
      this.redraw();
    }
  };
  /**
   * Tip no longer needs to be shown
   * @param {Object} tip
   */


  TipLayer.prototype.removeTip = function (tip) {
    this.$tips = [];
    this.redraw();
  };
  /**
   * Should a tip be shown?
   * @param {String} key
   * @returns {boolean}
   */


  TipLayer.prototype.isNeverShow = function (key) {
    var options = optionsManager.loadOptions("webChart.tips");

    if (!options) {
      return false;
    }

    key = baja.SlotPath.escape(key);
    return !options.get(key);
  };
  /**
   * Stop showing for a given key
   * @param {String} key
   */


  TipLayer.prototype.neverShow = function (key) {
    var options = optionsManager.loadOptions("webChart.tips");
    key = baja.SlotPath.escape(key);

    if (!options) {
      options = new baja.Component();
    }

    var exists = options.get(key);

    if (!exists) {
      options.add(key, true);
    }

    optionsManager.saveOptions("webChart.tips", options);
  };
  /**
   * Redraw handles the Tip layout
   */


  TipLayer.prototype.redraw = function () {
    var that = this,
        graph = that.$graph,
        jq = graph.widget().jq();
    var selection = d3.select(jq.get(0)).selectAll(".webChart-tip").data(that.$tips);

    function positionTip(div, elemJq) {
      var windowWidth = $(window).width(),
          windowHeight = $(window).height(),
          elemOffset = elemJq.offset(),
          elemLeft = elemOffset.left,
          elemTop = elemOffset.top > 30 ? elemOffset.top : 30,
          bottom = windowHeight - elemTop + 25,
          top = elemTop,
          alignLeft = windowWidth / 2 > elemLeft,
          alignTop = windowHeight / 4 > elemTop,
          right = windowWidth - elemLeft - 25,
          left = elemLeft - jq.offset().left - 10;

      if (alignTop) {
        if (alignLeft) {
          div.css({
            top: top,
            left: left,
            bottom: ""
          });
        } else {
          div.css({
            top: top,
            right: right,
            bottom: ""
          });
        }
      } else {
        if (alignLeft) {
          div.css({
            top: "",
            bottom: bottom,
            left: left
          });
        } else {
          div.css({
            top: "",
            bottom: bottom,
            right: right
          });
        }
      }

      return {
        alignLeft: alignLeft,
        alignTop: alignTop
      };
    }

    selection.each(function (tip) {
      var elemJq = tip.anchor,
          div = $(d3.select(this).node()),
          pointer = $('<div></div>');
      var align = positionTip(div, elemJq);

      if (align.alignTop && $('.webChart-pointer', div)[0]) {
        div.find('.webChart-pointer').remove();
        pointer.addClass("webChart-pointer-top");
        div.prepend(pointer);
      } else if (!align.alignTop && $('.webChart-pointer-top', div)[0]) {
        div.find('.webChart-pointer-top').remove();
        pointer.addClass("webChart-pointer");
        div.append(pointer);
      }
    });
    selection.exit().remove();
    selection.enter().append('div').attr('class', 'webChart-tip').each(function (tip) {
      var elemJq = tip.anchor,
          div = $(d3.select(this).node()),
          pointer = $('<div></div>');
      lex.module("webChart").then(function (webChartLex) {
        var message = webChartLex.getSafe({
          key: "webChart.tips." + tip.key,
          def: "missing lexicon for webChart.tips." + tip.key,
          args: tip.args
        }),
            neverShowAgain = webChartLex.getSafe("webChart.tips.neverShowAgain"),
            ok = webChartLex.getSafe("webChart.tips.confirm"),
            button = $("<button type='button' class='webChart-tip-button'>" + ok + "</button>"),
            align = positionTip(div, elemJq),
            alignLeft = align.alignLeft,
            alignTop = align.alignTop;
        div.append("<div class='webChart-tip-message'>" + webChartUtil.nl2br(message) + "</div>");
        button.click(function () {
          try {
            if (div.find('input').is(':checked')) {
              that.neverShow(tip.key);
            }

            that.removeTip();
          } catch (ignore) {}

          return false;
        });
        div.append("<div></div>").append(button);
        div.append("<div class='webChart-tip-neverAgain'><label><input type='checkbox'/>" + neverShowAgain + "</div></label>");

        if (alignTop) {
          pointer.addClass("webChart-pointer-top");
          div.prepend(pointer);
        } else {
          pointer.addClass("webChart-pointer");
          div.append(pointer);
        }

        if (!alignLeft) {
          pointer.css("left", div.width() - 20);
        }
      })["catch"](logSevere);
    });
  };
  /**
   * Return true if enabled.
   *
   * @returns {Boolean}
   */


  TipLayer.prototype.isEnabled = function () {
    return this.$enabled;
  };
  /**
   * Set whether the Tip is enabled or not.
   *
   * @returns {Boolean}
   */


  TipLayer.prototype.setEnabled = function (enabled) {
    var that = this;

    if (that.$enabled !== enabled) {
      that.$enabled = enabled;
      that.$graph.widget().jq().find('.TipGroup').toggle(enabled);
    }
  };
  /**
   * Return the layer's name.
   *
   * @returns {String}
   */


  TipLayer.prototype.name = function () {
    return "Tip";
  };

  return TipLayer;
});
