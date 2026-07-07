/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/ZoomLayer
 */
define(["d3", "nmodule/webChart/rc/line/XAxisLayer", "nmodule/webChart/rc/webChartUtil"], function (d3, XAxisLayer, webChartUtil) {
  "use strict";
  /**
   * A Layer in a Line Chart to handle Zooming
   *
   * @class
   * @alias module:nmodule/webChart/rc/line/ZoomLayer
   */

  var ZoomLayer = function ZoomLayer(graph) {
    this.$graph = graph;
    this.$zoomYOnly = false; //now alt
  };

  ZoomLayer.prototype.redraw = function () {
    // Bind events to the line-area of the chart
    var that = this,
        graph = that.$graph,
        jq = graph.widget().jq().find(".line-container-overlay-group"),
        chart = graph.chartd3(),
        zoom = d3.behavior.zoom(),
        scaleX = graph.getScaleX(),
        model = graph.widget().model();
    var selection = chart.select('.line-container-overlay'); //partial workaround for NCCB-10166: alt+mousedown is broken in workbench/javafx eventing

    if (webChartUtil.isJavaFx()) {
      d3.select('body').on("keydown.zoomModifiers", function () {
        var sourceEvent, keycode;

        if (!that.$zoomYOnly) {
          sourceEvent = d3.event;
          keycode = sourceEvent.keyCode ? sourceEvent.keyCode : sourceEvent.which;

          if (keycode === 18 || keycode === 89) {
            //hold down alt or y
            that.$zoomYOnly = true;
          }
        }
      }).on("keyup.zoomModifiers", function () {
        var sourceEvent, keycode; //workaround for NCCB-10166: alt+mousedown javaFx is broken due to

        if (that.$zoomYOnly && webChartUtil.isJavaFx()) {
          sourceEvent = d3.event;
          keycode = sourceEvent.keyCode ? sourceEvent.keyCode : sourceEvent.which;

          if (keycode === 18 || keycode === 89) {
            ////alt or y
            that.$zoomYOnly = false;
          }
        }
      });
    }

    zoom(selection);

    function zoomStart() {
      var hasShiftOrCtrl = false,
          hasAlt = false,
          sourceEvent = d3.event.sourceEvent,
          javaFx = webChartUtil.isJavaFx(),
          valueScalesLocked = false;
      hasAlt = !javaFx && sourceEvent && sourceEvent.altKey || javaFx && that.$zoomYOnly;
      hasShiftOrCtrl = sourceEvent && (sourceEvent.shiftKey || sourceEvent.ctrlKey);
      model.mapValueScales(function (valueScale) {
        if (valueScale.isLocked()) {
          valueScalesLocked = true;
        }
      });

      if (valueScalesLocked) {
        hasShiftOrCtrl = true;
        hasAlt = false;
      }

      if (!hasAlt) {
        zoom.x(scaleX);
      }

      var index = 0;
      model.mapValueScales(function (valueScale) {
        if (!hasShiftOrCtrl) {
          var valueZoom = d3.behavior.zoom();

          if (!hasShiftOrCtrl && index === 0) {
            valueZoom = zoom; //no x to use as base zoom, so use first valueScale
          }

          valueZoom.y(valueScale.scale());
          valueScale.zoom = valueZoom;
        } else {
          valueScale.zoom = null;
        }

        index++;
      });
      chart.select('.line-container-overlay-group').on("mousedown.pan", function () {
        //store original cursor
        var selection = d3.select(this),
            altCursor;
        selection.attr("originalCursor", jq.css("cursor"));

        if (!hasAlt && !hasShiftOrCtrl) {
          altCursor = "move";
        } else if (hasAlt) {
          altCursor = "n-resize";
        } else if (hasShiftOrCtrl) {
          altCursor = "w-resize";
        } else {//altCursor
        }

        selection.attr("altCursor", altCursor);
      }).on("mouseup.pan", function () {
        var select = d3.select(this);
        jq.css("cursor", select.attr("originalCursor"));
        select.attr("originalCursor", null);
        select.attr("altCursor", null);
      });
    }

    zoom.on('zoomstart', zoomStart); //for easier testing

    selection[0][0]._zoomStart = zoomStart;
    zoom.on('zoom', function () {
      var scaleFactor,
          translate,
          oldDomain,
          checkDomainResults,
          zoomModified = false;

      if (d3.event) {
        scaleFactor = d3.event.scale;
        translate = [d3.event.translate[0], d3.event.translate[1]];
      }

      webChartUtil.zoomCenter(d3.mouse(selection.node()), scaleFactor);
      model.mapValueScales(function (valueScale) {
        var valueZoom = valueScale.zoom;

        if (valueZoom) {
          valueZoom.scale(scaleFactor).translate(translate);
          zoomModified = true;
        }
      });
      oldDomain = zoom.lastXZoomDomain || scaleX.domain();
      checkDomainResults = XAxisLayer.checkDomain(scaleX.domain(), oldDomain);

      if (checkDomainResults.tooHigh || checkDomainResults.tooLow || checkDomainResults.tooSmall) {
        scaleX.domain(checkDomainResults.newDomain);
        zoom.x(scaleX);
      } else {
        zoom.lastXZoomDomain = scaleX.domain();
        zoomModified = true;
      }

      if (zoomModified) {
        graph.widget().manualZoom();
      }
    });
    chart.select('.line-container-overlay-group').on("mousemove.pan", function () {
      var select = d3.select(this);
      jq.css("cursor", select.attr("altCursor"));
      select.on("mousemove.pan", null);
    });
  };
  /**
   * Cleanup key handlers on body
   */


  ZoomLayer.prototype.destroy = function () {
    //remove key listeners on body
    if (webChartUtil.isJavaFx()) {
      d3.select('body').on("keydown.zoomModifiers", null).on("keyup.zoomModifiers", null);
    }
  };

  return ZoomLayer;
});
