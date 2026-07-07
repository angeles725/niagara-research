/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson and JJ Frankovich
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/DataPopupLayer
 */
define(["jquery", "d3", "nmodule/webChart/rc/webChartUtil", "nmodule/webChart/rc/model/modelUtil", "nmodule/webChart/rc/localeUtil", "nmodule/webChart/rc/chartEvents", "baja!", "baja!gx:Color", 'log!nmodule.webChart.rc.line.DataPopupLayer'], function ($, d3, webChartUtil, modelUtil, localeUtil, events, baja, types, log) {
  "use strict";

  var logSevere = log.severe.bind(log);
  var INTERPOLATED_SECONDARY_COLOR = baja.$("gx:Color", 'white');
  /**
   * The pop up used for any padding in a pop up bubble.
   * @type {Number}
   * @private
   * @inner
   */

  var popupPadding = 5;
  /**
   * The Data Pop Up Layer for a Chart. To make managing the code for a chart easier,
   * a chart is split up into a number of layers. This layer handles one of the most
   * outer layers that pops up data to the user when they move a mouse over or clicks on a chart
   * for some information about a given point.
   *
   * @class
   * @param graph The associated chart graph.
   */

  var DataPopupLayer = function DataPopupLayer(graph) {
    var that = this;
    that.$graph = graph;
    that.$popupHidden = true;
    that.$enabled = true;
  };
  /**
   * Returns the focus data for a particular point on the chart.
   * This method is used to find what line on the chart is closest
   * to the point and return some data associated with it.
   *
   * @private
   * @inner
   *
   * @param  graph The graph for this layer.
   * @param  {Date} x0 The Date for the selected point x-axis.
   * @param  {Number} y0 The value for the selected date along the y-axis.
   * @param  {Number} x The original mouse x co-ordinate relative to the chart.
   * @param  {Number} y The original mouse y co-ordinate relative to the chart.
   * @param  {module:nmodule/webChart/rc/model/BaseSeries} [specifiedSeries] If defined, only get focus data for a particular series
   * @param  {module:nmodule/webChart/rc/model/BaseSeries} [previousSeries] If defined, give preference to this series
   * @returns {Object} If some focus data is returned, an object with the
   * x, y, Date, value, series and number of enabled plots will be returned. Null
   * will be returned if no focus data can be found.
   */


  function getFocusData(graph, x0, y0, x, y, specifiedSeries, previousSeries) {
    var j,
        d0,
        d1,
        d,
        i,
        data,
        discrete,
        bar,
        seriesList = graph.data(),
        widget = graph.widget(),
        model = widget.model(),
        scaleX = graph.getScaleX(),
        scaleY,
        bisectDate,
        focusData = null,
        plotSize = 0,
        dataLayer = graph.dataLayer(),
        areaY,
        top,
        bottom,
        points,
        barIndex = -1,
        barLayout,
        isLine = false,
        isFocusLine = false,
        dimensions = widget.dimensions(),
        betweenPoints = true,
        distancePoint,
        scaledX,
        scaledY;

    if (x0 !== undefined && y0 !== undefined) {
      bisectDate = d3.bisector(function bisector(d) {
        return d.x;
      }).left;

      for (i = 0; i < seriesList.length; ++i) {
        if (seriesList[i].isBar()) {
          barIndex++;

          if (!barIndex) {
            //initialize when first needed at index 0
            barLayout = modelUtil.getBarLayout(model);
          }
        }

        if (specifiedSeries && seriesList[i] !== specifiedSeries) {
          continue;
        }

        betweenPoints = true;
        distancePoint = null;
        isFocusLine = focusData && !focusData.series.isShade() && !focusData.series.isBar();
        isLine = !seriesList[i].isShade() && !seriesList[i].isBar();
        scaleY = seriesList[i].valueScale().scale();

        if (seriesList[i].isEnabled()) {
          discrete = seriesList[i].isShade();
          bar = seriesList[i].isBar();

          if (focusData && isFocusLine && !isLine) {
            continue; //give preference to line data
          }

          ++plotSize;
          points = seriesList[i].samplingPoints();
          j = bisectDate(points, x0, 1);
          d0 = points[j - 1];
          d1 = points[j];

          if (discrete) {
            if (j === 1 && d0 && d0.x > x0 || //time before first point, not needed
            j === points.length || //time is after last point, currently not needed
            d1 && d1.skip) {
              //time is not visual
              continue;
            }

            if (d0) {
              d = d0; //discrete requires no averaging
            } else {
              d = null; //don't let d be used from previous series
            }
          } else {
            if (!bar) {
              d = d1 ? x0 - d0.x > d1.x - x0 ? d1 : d0 : d0;
            } else {
              d = d0;
            }

            if (d) {
              scaledX = scaleX(d.x);
              scaledY = scaleY(d.y);
            } //is mouse between two x's on a line?


            betweenPoints = d1 && d0 && !(j === 1 && d0.x > x0) && !d1.skip && !d0.skip;

            if (betweenPoints && !seriesList[i].isDiscreteLine() && !bar) {
              var xPercent = (x0 - d0.x) / (d1.x - d0.x),
                  newY = (d1.y - d0.y) * xPercent + d0.y; //distance to line based on interpolated y intercept so mouse over is less jumpy

              distancePoint = {
                x: x,
                y: scaleY(newY)
              };
            }
          }

          var pointIndex = d === d0 ? j - 1 : j;

          if (d && d.interpolated && bar && modelUtil.isPointBeforeLastBarGroup(model, d, barLayout, scaleX)) {
            // The interpolated bar isn't displayed so use the last point non-interpolated point instead
            pointIndex--;
            d = points[pointIndex];
            scaledX = scaleX(d.x);
            scaledY = scaleY(d.y);
          }

          var isGap = modelUtil.isBarChartDataGap(points, pointIndex); // Find the date that's closet to the point.

          if (d) {
            data = {
              x: discrete ? x : scaledX,
              //use mouse cursor for discrete
              y: discrete ? y : scaledY,
              status: d.status,
              dx: d.x,
              dy: d.y,
              series: seriesList[i],
              color: seriesList[i].color(),
              circleOpacity: discrete || bar ? 0 : null,
              point: d,
              isGap: isGap,
              betweenPoints: betweenPoints,
              distancePoint: distancePoint
            };

            if (data.series.isBar()) {
              if (d && d.interpolated) {
                data.x = scaleX(d.x) + barIndex * modelUtil.getBarWidth(model) + modelUtil.getBarWidth(model) / 2;
              } else if (barLayout) {
                data.x = modelUtil.getBarX(data.dx, barIndex, barLayout, scaleX) + barLayout.barWidth(data.dx) / 2;
              }
            }

            if (!data.distancePoint) {
              data.distancePoint = {
                x: data.x,
                y: data.y
              };
            }

            if (!focusData && (!data.series.isShade() || specifiedSeries)) {
              focusData = data;
            } else if (data && focusData && !isFocusLine && isLine) {
              //give preference to lines
              focusData = data;
            } else {
              if (data.series.isShade()) {
                areaY = dataLayer.getAreaY(data.series);
                top = dimensions.height * areaY[1];
                bottom = dimensions.height * areaY[0];

                if (y <= top && y >= bottom) {
                  focusData = data;
                }
              } else {
                if (focusData && focusData.distance === undefined) {
                  focusData.distance = getWeightedDistance(focusData, x, y, previousSeries);
                }

                var distance = getWeightedDistance(data, x, y, previousSeries);

                if (data.y >= 0 && distance < focusData.distance) {
                  focusData = data;
                  focusData.distance = distance;
                }
              }
            }
          }
        }
      }
    }

    if (focusData) {
      focusData.plotSize = plotSize;

      if (focusData.distance === undefined) {
        focusData.distance = getWeightedDistance(focusData, x, y, previousSeries);
      } //Too far away for not between points


      if (!focusData.betweenPoints && focusData.distance > 3600) {
        return null;
      }

      if (bar && focusData && focusData.isGap && widget.settings().getShowDataGaps() === 'gap') {
        return null;
      }
    }

    return focusData;
  }
  /**
   * Get the weight distance for a line
   * Find the closest date to the original point. Use distance formula, but sqrt is not required since
   * we are comparing distances.
   * @param {Object} data
   * @param {Number} x
   * @param {Number} y
   * @param {module:nmodule/webChart/rc/model/BaseSeries} previousSeries
   * @returns {Number} distance Squared (save on math by not running sqrt)
   */


  function getWeightedDistance(data, x, y, previousSeries) {
    var distance,
        dataX = data.distancePoint.x,
        dataY = data.distancePoint.y;

    if (data.series.isBar()) {
      distance = Math.abs(dataX - x);
    } else {
      distance = Math.pow(dataY - y, 2) + Math.pow(dataX - x, 2);
    }

    if (data.series.isLine() && previousSeries === data.series) {
      distance -= 1500; //give 30 pixel weight to previousSeries
    }

    return distance;
  }
  /**
   * Translates the given y co-ordinate into a value that can be used
   * to position an HTML DOM element's left selector.
   *
   * @inner
   * @private
   *
   * @param  layer The graph for the layer.
   * @param  {Number} y The y-ordinate relative to the SVG chart.
   * @param  {Number} padding The amount of padding required during the translate
   * @returns {string} The CSS value for the CSS left Property.
    */


  function translateTopPx(layer, y, padding) {
    var margin = layer.$graph.widget().options.margin,
        jq = layer.$graph.widget().jq();
    return y + margin.top + jq.children(".widgetBar").height() + padding + "px";
  }
  /**
   * Position an HTML DOM's left and right style for a pop up bubble depending
   * on how much space there is to the left and the right of the pop up.
   * For instance, if the pop up doesn't have too much space to the right, it
   * will appear on the right hand side instead.
   *
   * @inner
   * @private
   *
   * @param  layer The data pop-up layer instance.
   * @param  {d3#selection} selection The d3 selection for the pop up.
   * @param  {Number} x The x co-ordinate.
   * @param  {Number} padding
   * @return {boolean} isRight
    */


  function positionPopupLeftRight(layer, selection, x, padding) {
    var popup = $(selection.node()),
        popupWidth = popup.width(),
        width = layer.$graph.chartd3().select(".line-chart-background").attr("width"),
        margin = layer.$graph.widget().options.margin,
        right;

    if (popupWidth < 200) {
      popupWidth = 200;
    } //when getting close to edge switch to other side


    right = x + popupWidth * 2 > width;

    if (right) {
      selection.style("left", null);
      selection.style("right", width - (x - margin.right - padding) + "px");
    } else {
      selection.style("left", x + margin.left + padding + "px");
      selection.style("right", null);
    }

    return right;
  }
  /**
   * Highlight or removeHighlight for mousing over a bar.
   * @param {d3} chart
   * @param {Object} highlightX
   * @param {boolean} on
   * @private
   * @internal
   */


  function highlightBar(chart, highlightPoint, on) {
    var groupFound = false;
    chart.selectAll('.lineContainer .bars .dataSet').each(function (series) {
      if (series.isEnabled()) {
        d3.select(this).selectAll("rect").each(function (point) {
          var barLine = d3.select(this),
              groupHighlight = barLine.attr("fixed-highlight") === "true",
              singleHighlight = false;

          if (groupHighlight) {
            groupFound = true;
          }

          if (on) {
            if (highlightPoint === point) {
              singleHighlight = true;
              barLine.attr("mouseover-highlight", true);
            }
          }

          if (!singleHighlight) {
            barLine.attr("mouseover-highlight", null);
          }

          barLine.style("opacity", singleHighlight || groupHighlight ? 1.0 : 0.5);
        });
      }
    }); //TODO: make this more efficient, but turn back on full opacity

    if (!groupFound && !on) {
      chart.selectAll('.lineContainer .bars .dataSet').each(function (series) {
        if (series.isEnabled()) {
          d3.select(this).selectAll("rect").each(function (point) {
            var barLine = d3.select(this);
            barLine.style("opacity", 1);
          });
        }
      });
    }
  }
  /**
   * Highlight or removeHighlight for mousing over a bar.
   * @param {d3} chart
   * @param {Object} highlightX
   * @param {boolean} on
   * @private
   * @internal
   */


  function highlightBars(chart, highlightX, on) {
    var highlightTime,
        singleFound = false;
    chart.selectAll('.lineContainer .bars .dataSet').each(function (series) {
      if (series.isEnabled()) {
        d3.select(this).selectAll("rect").each(function (point) {
          var barLine = d3.select(this),
              groupHighlight = false,
              singleHighlight = barLine.attr("mouseover-highlight") === "true";

          if (singleHighlight) {
            singleFound = true;
          }

          if (on) {
            if (highlightTime === undefined && highlightX) {
              highlightTime = highlightX.getTime();
            }

            if (highlightTime === point.x.getTime()) {
              groupHighlight = true;
              barLine.attr("fixed-highlight", true);
            }
          }

          if (!groupHighlight) {
            barLine.attr("fixed-highlight", null);
          }

          barLine.style("opacity", singleHighlight || groupHighlight ? 1.0 : 0.5);
        });
      } else {
        d3.select(this).selectAll("rect").attr("fixed-highlight", null).attr("mouseover-highlight", null);
      }
    }); //TODO: make this more efficient, but turn back on full opacity

    if (!singleFound && !on) {
      chart.selectAll('.lineContainer .bars .dataSet').each(function (series) {
        if (series.isEnabled()) {
          d3.select(this).selectAll("rect").each(function (point) {
            var barLine = d3.select(this);
            barLine.style("opacity", 1);
          });
        }
      });
    }
  }
  /**
   * Initialize the data pop-up. This initializes the data pop-up that
   * occurs when a user the mouse cursor over a line chart.
   *
   * @private
   * @internal
   *
   * @param layer The data pop-up layer instance.
   */


  function initDataPopUp(layer) {
    var graph = layer.$graph,
        chart = graph.chartd3(),
        group = chart.select(".line-container-overlay-group"),
        widget = graph.widget(),
        model = widget.model(),
        jq = widget.jq(),
        scaleX = graph.getScaleX();
    /**
     * Cache the update for the pop-up. This caches the d3 enter, update and remove
     * pattern so it can be re-run when-ever it's needed too.
     *
     * @private
     */

    layer.$popupUpdate = function () {
      var lineData = [],
          data = [],
          selection,
          focusData,
          x,
          y,
          scaleY = graph.getScaleY(),
          ignore = false,
          dimensions,
          previousData,
          previousSeries; // Update the data to be used by the data pop-up. This data will be
      // used in d3's enter, update and exit pattern to add, update and remove
      // the data pop-up.

      if (layer.$popupData && layer.isMouseOverEnabled(widget)) {
        x = scaleX(layer.$popupData.x);
        y = scaleY(layer.$popupData.y);
        previousData = group.selectAll(".data-popup-circle").data()[0];

        if (previousData) {
          previousSeries = previousData.series;
        }

        focusData = layer.$getFocusData(graph, layer.$popupData.x, layer.$popupData.y, x, y, null, previousSeries);

        if (focusData) {
          dimensions = widget.dimensions(); //below bottom

          if (focusData.y > dimensions.height) {
            if (focusData.series.isBar()) {
              focusData.y = dimensions.height;
            } else {
              ignore = true;
            }
          } //above top


          if (focusData.y < 0) {
            if (focusData.series.isBar()) {
              focusData.y = 0;
            } else {
              ignore = true;
            }
          } //too far left or right, always ignore


          if (focusData.x < 0 || focusData.x > dimensions.width) {
            ignore = true;
          }

          if (!ignore) {
            data.push(focusData);

            if (!focusData.series.isBar()) {
              //no line needed for bar
              lineData.push({
                x: x,
                y: y
              });
            }
          }
        }
      } // Mouse over line


      selection = group.selectAll(".data-popup-line").data(lineData);
      selection.enter().insert("line", ".line-container-overlay").attr("class", "data-popup-line").attr("x1", 0).attr("y1", 0).attr("x2", 0).attr("y2", "100%");
      selection.attr("transform", function (d) {
        return "translate(" + d.x + ",0)";
      });
      selection.exit().remove(); // Mouse over circle

      selection = group.selectAll(".data-popup-circle").data(data);
      selection.enter().insert("circle", ".line-container-overlay").attr("class", "data-popup-circle").attr("r", 4.5);
      selection.attr("transform", function (d) {
        return "translate(" + d.x + "," + d.y + ")";
      }).style("fill-opacity", function (d) {
        return d.circleOpacity;
      }).style("stroke-opacity", function (d) {
        return d.circleOpacity;
      });
      selection.call(function () {
        var d = this.data()[0];

        if (d) {
          var select = this,
              statusColor = webChartUtil.statusToColor(d.status),
              result = statusColor.bgColor;

          if (!result) {
            result = d.color;
          }

          select.style("fill", function (data) {
            if (data && data.point && data.point.interpolated) {
              return INTERPOLATED_SECONDARY_COLOR.toCssString();
            }

            return result;
          }).style("stroke", result).style("stroke-width", 2);

          if (d.series.isBar()) {
            highlightBar(chart, d.point, true);
          }
        }
      });
      var exit = selection.exit();
      exit.call(function () {
        var d = this.data()[0];

        if (d && d.point) {
          highlightBar(chart, d.point, false);
        }
      });
      exit.remove(); // Mouse over pop-up

      selection = d3.select(jq.get(0)).selectAll(".data-popup").data(data);
      var enterDataPopupElem = selection.enter().insert("div", ".data-popup-fixed") //ensure fixed is always on top
      .attr("class", "data-popup data-popup-continue").on("click.datapopup", function () {
        //barChart mouse over could be covering legend, so allow user to click to remove
        delete layer.$popupData;
        layer.$popupUpdate();
      });
      enterDataPopupElem.insert("div").attr("class", "data-popup-continue value");
      enterDataPopupElem.insert("div") //.style("display", "none")
      .attr("class", "data-popup-continue name");
      enterDataPopupElem.append("div").style("display", "none").attr("class", "webChart-pointer data-popup-continue");
      selection.style("top", function (d) {
        var padding = popupPadding;

        if (d.series.isBar()) {
          padding = -50; //TODO: extra padding is required for using webChart-pointer

          if (model.seriesList().length > 1) {
            padding += -17; //account for extra line for name
          }
        } else if (d.y + 20 > dimensions.height) {
          padding -= 40; //prevent scrollbars when not using bar
        }

        return translateTopPx(layer, d.y, padding);
      }).on("mouseout.datapopup", function () {
        //just route to overlay's mouseout
        var overlay = group.select(".line-container-overlay");
        overlay.on("mouseout.datapopup").apply(overlay.node());
      }).on("mousemove.datapopup", function () {
        //just route to overlay's mouse over in case the mouse catches up with the pop-up
        var overlay = group.select(".line-container-overlay");
        overlay.on("mousemove.datapopup").apply(overlay.node());
      }).call(function () {
        var d = this.data()[0];

        if (d) {
          var padding = popupPadding,
              select = this,
              pointer = select.select("div.webChart-pointer"),
              statusColor,
              bg,
              fg,
              moveBarRight;

          if (d.series.isBar()) {
            padding = -20;
            pointer.style("display", "");
          }

          moveBarRight = positionPopupLeftRight(layer, this, d.x, padding) && d.series.isBar();

          if (!moveBarRight) {
            $(pointer.node()).css("left", "");
          }

          statusColor = webChartUtil.statusToColor(d.status);
          bg = statusColor.bgColor;
          fg = statusColor.fgColor;

          if (bg && fg) {
            select.style("background-color", bg);
            select.style("color", fg);
            pointer.style("border-color", bg + " transparent");
          } else {
            select.style("background-color", null);
            select.style("color", null);
            pointer.style("border-color", null);
          }

          webChartUtil.statusToString(d.status).then(function (statusString) {
            return d.series.resolveYDisplay(d.dy).then(function (display) {
              select.select("div.name").text(function (d) {
                return model.seriesList().length > 1 ? d.series.shortDisplayName() : "";
              });
              select.select("div.value").text(function (d) {
                var date = model.timeScale().getTimeDisplay(d.dx);
                return display + " " + d.series.valueScale().displayUnitSymbol() + " " + statusString + " @ " + date;
              });
            });
          }).then(function () {
            if (moveBarRight) {
              $(pointer.node()).css("left", $(select.node()).width() - 24);
            }
          })["catch"](logSevere);
        }
      });
      selection.exit().remove();

      if (focusData) {
        jq.trigger(events.SERIES_FOCUS, [focusData.series, focusData.plotSize]);
      } else {
        jq.trigger(events.SERIES_BLUR);
      }
    }; // Arm some event handlers.


    group.select(".line-container-overlay").on("mouseout.datapopup", function () {
      // When the mouse moves out of the container overlay, 
      // remove everything.
      var event = d3.event;

      if (event) {
        var x = event.clientX,
            y = event.clientY,
            elementMouseIsOver = document.elementFromPoint(x, y); //prevent flashing on mouseover of own pop-up

        if (elementMouseIsOver && d3.select(elementMouseIsOver).classed("data-popup-continue")) {
          return;
        }
      }

      delete layer.$popupData;
      layer.$popupUpdate();
    }).on("mousemove.datapopup", function () {
      var enabled = layer.isMouseOverEnabled(widget); // When the mouse is moved over the chart, make 
      // the data pop-up appear if there's data.

      var pos = webChartUtil.getD3MouseEventPosition(this),
          x = pos.x,
          y = pos.y,
          cursor = group.style("cursor"),
          scaleY = graph.getScaleY();

      if (d3.event && cursor !== "move" && cursor !== "w-resize" && cursor !== "n-resize") {
        cursor = widget.settings().getChartCursor(); //ensure cursor is visible when no data is present

        if (cursor === "none" && (!model.hasPoints() || !enabled)) {
          cursor = "crosshair";
        }

        group.style("cursor", cursor);
      }

      if (!enabled) {
        return;
      }

      if (x !== undefined && y !== undefined) {
        layer.$popupData = {
          x: scaleX.invert(x),
          y: scaleY.invert(y)
        };
        layer.$popupUpdate();
      }
    });
  }
  /**
   * Finds a point for a given x co-ordinate along an SVG path.
   * This is used when attempting to find all of the y axix values
   * to a user when the user clicks on the chart and creates a fixed
   * line.
   *
   * To make this as efficient as possible, the principles
   * of a binomial distribution is used to narrow down the values.
   *
   * @private
   * @internal
   *
   * @param  {Number} x The x co-ordinate from a mouse click.
   * @param  path An SVG Path DOM element.
   * @returns {Object|null} The closest point at the y-axis. If no data can
   * be found then null is returned.
   */


  function findYatX(x, path) {
    var end = path.getTotalLength();

    if (end === 0) {
      return null;
    }

    var start = 0,
        startPoint = path.getPointAtLength(start),
        endPoint = path.getPointAtLength(end),
        point = path.getPointAtLength(end / 2),
        // get the middle point
    iterationsMax = 300,
        iterations = 0,
        error = 0.001; // Short circuit if the line falls outside out bounds.

    if (x < startPoint.x || x > endPoint.x) {
      return null;
    }

    while (x < point.x - error || x > point.x + error) {
      point = path.getPointAtLength((end + start) / 2);

      if (x < point.x) {
        end = (start + end) / 2;
      } else {
        start = (start + end) / 2;
      } // Increase iteration


      if (iterationsMax < ++iterations) {
        break;
      }
    }

    return point;
  }
  /**
   * Initialize the fixed data pop-up. This initializes the data pop-up that
   * occurs when a user clicks on a chart. When this happens a fixed line
   * appears on the chart with information about all of the values at that point
   * on the x-axis.
   *
   * @private
   * @internal
   *
   * @param layer The data pop-up layer instance.
   */


  function initFixedDataPopup(layer) {
    var graph = layer.$graph,
        widget = graph.widget(),
        model = widget.model(),
        jq = widget.jq(),
        chart = graph.chartd3(),
        group = chart.select(".line-container-overlay-group"),
        scaleX = graph.getScaleX(),
        scaleY = graph.getScaleY();
    /**
     * @private
     */

    layer.$getFocusData = function (graph, x0, y0, x, y, specifiedSeries, previousSeries) {
      return getFocusData(graph, x0, y0, x, y, specifiedSeries, previousSeries);
    };
    /**
     * @private
     */


    layer.$findYatX = function (x, path) {
      return findYatX(x, path);
    };
    /**
     * Cache the update for the fixed pop-up. This caches the d3 enter, update and remove
     * pattern so it can be re-run when-ever it's needed too.
     *
     * @private
     */


    layer.$popupFixedUpdate = function () {
      var that = this,
          showDataGaps = that.$graph.widget().settings().getShowDataGaps(),
          lineData = [],
          popupData = [],
          rowData = [],
          selection,
          xy,
          hasBar = false,
          barTime,
          shadeOnly = true,
          enter,
          i; // Update the data to be used by the fixed data pop-up. This data will be
      // used in d3's enter, update and exit pattern to add, update and remove
      // the data pop-up.

      if (layer.$popupFixedData && layer.isFixedPopupEnabled(widget)) {
        xy = {
          x: scaleX(layer.$popupFixedData.x),
          y: scaleY(layer.$popupFixedData.y)
        };

        if (xy.x >= 0 && xy.x <= chart.select(".line-chart-background").attr("width")) {
          var isGap = false; // Data for the fixed line.

          lineData.push(xy); // Data for the pop up dialog contents

          chart.selectAll('.lineContainer .dataSet').each(function (series) {
            if (series.isEnabled()) {
              var pathSelection = d3.select(this).selectAll('path'),
                  point,
                  focusPoint,
                  focusData,
                  dx,
                  dy,
                  status,
                  skip = false;

              if (!series.isShade()) {
                shadeOnly = false;
              }

              if (series.isShade() || series.isBar()) {
                focusData = getFocusData(graph, layer.$popupFixedData.x, layer.$popupFixedData.y, xy.x, xy.y, series);

                if (focusData) {
                  if (series.isBar()) {
                    dx = focusData.dx;
                  } else {
                    //NCCB-9146: Shade chart should display actual time on fixed pop-up, not closest point
                    dx = layer.$popupFixedData.x;
                  }

                  dy = focusData.dy;
                  status = focusData.status;
                  focusPoint = focusData.point;

                  if (series.isBar()) {
                    hasBar = true;
                  }
                }
              } else {
                pathSelection.each(function () {
                  if (!point) {
                    var path = d3.select(this).node();
                    point = layer.$findYatX(xy.x, path);
                  }
                });

                if (point) {
                  dx = scaleX.invert(point.x);
                  dy = series.valueScale().scale().invert(point.y);
                  focusData = layer.$getFocusData(graph, layer.$popupFixedData.x, layer.$popupFixedData.y, xy.x, xy.y, series);

                  if (focusData) {
                    status = focusData.status;
                    focusPoint = focusData.point;
                  }
                }
              }

              if (dx !== undefined && dy !== undefined) {
                //falsy would be bad for zero
                //ensure that the data is part of the same group and the closest group
                if (series.isBar() && rowData.length) {
                  for (i = rowData.length - 1; i >= 0; i--) {
                    if (rowData[i].series.isBar() && rowData[i].point.x.getTime() !== focusPoint.x.getTime()) {
                      if (Math.abs(xy.x - dx) < Math.abs(xy.x - rowData[i].dx)) {
                        rowData.splice(i, 1);
                        barTime = dx;
                      } else {
                        skip = true;
                      }
                    }
                  }
                } else if (series.isBar()) {
                  barTime = dx;
                }

                if (!skip) {
                  isGap = focusData && focusData.isGap;
                  rowData.unshift({
                    dx: dx,
                    dy: dy,
                    point: focusPoint,
                    color: series.color(),
                    series: series,
                    status: status,
                    isGap: isGap
                  });
                }
              }
            }
          }); // Data for the pop up dialog

          if (rowData.length > 0) {
            popupData.push({
              x: xy.x,
              y: xy.y,
              dx: rowData[0].dx,
              hasBar: hasBar,
              barTime: barTime,
              isGap: isGap
            });
          }
        }
      } // Fixed line


      selection = group.selectAll(".data-popup-line-fixed").data(lineData);
      selection.enter().insert("line", ".line-container-overlay").attr("class", "data-popup-line-fixed").attr("x1", 0).attr("y1", 0).attr("x2", 0).attr("y2", "100%");
      selection.attr("transform", function (d) {
        return "translate(" + d.x + ",0)";
      });
      selection.exit().remove(); // Pop up dialog

      selection = d3.select(jq.get(0)).selectAll(".data-popup-fixed").data(popupData);
      enter = selection.enter().append("div", ".data-popup") //ensure fixed is always on top
      .attr("class", "data-popup-fixed");
      enter.append("div").attr("class", "data-popup-fixed-title");
      enter.append("div").attr("class", "data-popup-fixed-close-outer").append("div").attr("class", "data-popup-fixed-close").html("&#10006;").on("mouseup.datapopupfixed", function () {
        delete layer.$popupFixedData;
        layer.$popupFixedUpdate();
      });
      enter.append("table").attr("class", "data-popup-fixed-rows");
      selection.style("top", function (d) {
        var top = translateTopPx(layer, 0, popupPadding);
        return top;
      }).call(function () {
        var d = this.data()[0],
            padding = popupPadding;

        if (d) {
          if (d.hasBar) {
            var barLayout = modelUtil.getBarLayout(widget.model());
            padding = barLayout.groupWidth(d.x);
            highlightBars(chart, d.barTime, true);
          }

          positionPopupLeftRight(layer, this, d.x, padding);
        }
      });
      selection.select(".data-popup-fixed-title").text(function (d, i) {
        if (d.hasBar) {
          //only bar an no interpolation here
          return model.timeScale().getFullTimeDisplay(d.barTime);
        } else if (shadeOnly) {
          return model.timeScale().getTimeDisplay(d.dx, true, true);
        } else {
          var timezone = model.timeScale().getTimeZone();
          return localeUtil.format(localeUtil.getD3LongDateTimeFormat(), d.dx, timezone);
        }
      });
      var exit = selection.exit();
      exit.call(function () {
        var d = this.data()[0];

        if (d && d.hasBar) {
          highlightBars(chart, null, false);
        }
      });
      exit.remove(); // Contents of the popup dialog.

      selection = d3.select(jq.get(0)).select(".data-popup-fixed-rows").selectAll(".data-popup-fixed-row").data(rowData);
      enter = selection.enter().append("tr").attr("class", "data-popup-fixed-row");
      enter.append("td").append("span").attr("class", "data-popup-fixed-row-img");
      enter.append("td").append("span").attr("class", "data-popup-fixed-row-name");
      enter.append("td").attr("class", "data-popup-fixed-row-value");
      selection.select(".data-popup-fixed-row-img").style("background-color", function (d) {
        return d.color;
      }).style("border-size", "1px").style("border-style", "solid").style("border-color", function (d) {
        if (d.point && d.point.interpolated) {
          return INTERPOLATED_SECONDARY_COLOR.toCssString();
        }

        if (d.isGap && showDataGaps === "dotted") {
          return INTERPOLATED_SECONDARY_COLOR.toCssString();
        }

        return d.color;
      });
      selection.select(".data-popup-fixed-row-name").text(function (d) {
        return d.series.shortDisplayName();
      });
      selection.select(".data-popup-fixed-row-value").each(function (d) {
        var select = d3.select(this);

        if (d) {
          webChartUtil.statusToString(d.status).then(function (statusString) {
            return d.series.resolveYDisplay(d.dy).then(function (display) {
              select.text(function (d) {
                return display + " " + d.series.valueScale().displayUnitSymbol() + " " + statusString;
              });
            });
          })["catch"](logSevere);
        }
      }); // Make any fixed data pop up text highlight if the data is selected.

      d3.select(jq.get(0)).selectAll(".data-popup-fixed-row-value").each(function (d) {
        var select = d3.select(this),
            statusColor = webChartUtil.statusToColor(d.status),
            bg = statusColor.bgColor,
            fg = statusColor.fgColor;

        if (bg && fg) {
          select.style("background-color", bg);
          select.style("color", fg);
        } else {
          select.style("background-color", null);
          select.style("color", null);
        }
      });
      selection.exit().remove();
    }; // Arm the event handlers for the fixed data pop-up.


    group.select(".line-container-overlay").on("mouseup.datapopupfixed", function () {
      if (layer.$mouseDownTicks !== undefined) {
        // Only invoke this if this has been clicked quickly.
        if (new Date().getTime() - layer.$mouseDownTicks > 500) {
          return;
        }
      }

      if (!layer.isFixedPopupEnabled(widget)) {
        return;
      }

      var pos = webChartUtil.getD3MouseEventPosition(this),
          x = pos.x,
          y = pos.y;

      if (x !== undefined && y !== undefined) {
        layer.$popupFixedData = {
          x: scaleX.invert(x),
          y: scaleY.invert(y)
        };
      } else {
        delete layer.$popupFixedData;
      }

      layer.$popupFixedUpdate();
    }).on("mousedown.datapopupfixed", function () {
      if (layer.isFixedPopupEnabled(widget)) {
        layer.$mouseDownTicks = new Date().getTime();
      }
    }); // Listen to when a Series is enabled or disabled so we can update the fixed
    // data pop-up.

    jq.on(events.SERIES_ENABLED + " " + events.SERIES_DISABLED, function () {
      if (layer.$popupFixedUpdate) {
        layer.$popupFixedUpdate();
      }
    }); // Listen for when a Series is given focus or not so we can update the text
    // on the fixed data pop-up.

    jq.on(events.SERIES_FOCUS + " " + events.SERIES_BLUR, function (event, series, plotSize) {
      plotSize = plotSize || 0; // Make any fixed data pop up text highlight if the data is selected.

      d3.select(jq.get(0)).selectAll(".data-popup-fixed-row-name").style("color", function (d) {
        return plotSize > 1 && series === d.series ? "#105ccd" : null;
      }).style("background-color", function (d) {
        return plotSize > 1 && series === d.series ? "#f9f9f9" : null;
      });
      d3.select(jq.get(0)).selectAll(".data-popup-fixed-row").on("mouseenter.datapopupfixed", function (d) {
        jq.trigger(events.SERIES_FOCUS, [d.series, $(".data-popup-fixed-row", jq).length]);
      }).on("mouseleave.datapopupfixed", function () {
        jq.trigger(events.SERIES_BLUR);
      });
    });
  }

  DataPopupLayer.prototype.initialize = function () {
    initDataPopUp(this);
    initFixedDataPopup(this);
  };

  DataPopupLayer.prototype.redraw = function () {
    var that = this;

    if (that.$popupUpdate) {
      that.$popupUpdate();
    }

    if (that.$popupFixedUpdate) {
      that.$popupFixedUpdate();
    }
  };

  DataPopupLayer.prototype.name = function () {
    return "Data Pop Up";
  };

  DataPopupLayer.prototype.isEnabled = function () {
    return this.$enabled;
  };

  DataPopupLayer.prototype.setEnabled = function (enabled) {
    var oldEnabled = this.$enabled;

    if (oldEnabled === enabled) {
      return;
    }

    this.$enabled = enabled;
  };

  DataPopupLayer.prototype.isMouseOverEnabled = function (widget) {
    return widget.settings().getDataMouseOver() === "on" && this.isEnabled();
  };

  DataPopupLayer.prototype.isFixedPopupEnabled = function (widget) {
    return widget.settings().getDataPopupLayout() === "on" && this.isEnabled();
  };

  return DataPopupLayer;
});
