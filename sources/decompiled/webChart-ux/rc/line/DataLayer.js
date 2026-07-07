function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/DataLayer
 */
define(["d3", "nmodule/webChart/rc/model/modelUtil", "nmodule/webChart/rc/webChartUtil", "nmodule/webChart/rc/chartEvents"], function (d3, modelUtil, webChartUtil, events) {
  "use strict";

  var isBarChartDataGap = modelUtil.isBarChartDataGap;
  /**
   * @class
   * @alias module:nmodule/webChart/rc/line/DataLayer
   * @param graph
   */

  var DataLayer = function DataLayer(graph) {
    var that = this;
    that.$graph = graph;
  };

  DataLayer.prototype.initialize = function () {
    var chart = this.$graph.chartd3(),
        jq = this.$graph.widget().jq();
    jq.on(events.SERIES_FOCUS, function (event, series, plotSize) {
      plotSize = plotSize || 0; // Make any fixed data pop up text highlight if the data is selected.

      chart.selectAll('.lineContainer .dataSet path').style("stroke-width", function (d) {
        if (plotSize <= 1) {
          return null;
        }

        return series === d ? 3 : 1;
      });
    });
    jq.on(events.SERIES_BLUR, function () {
      // Make any fixed data pop up text highlight if the data is selected.
      chart.selectAll('.lineContainer .dataSet path').style("stroke-width", function () {
        return null;
      });
    });
  };
  /**
   * Get the Fill Opacity for a particular path.
   * @param {webChart.Line} graph
   * @param {Number} pathIndex
   * @return {Number} between 0 and 1 (%)
   */


  DataLayer.prototype.getFillOpacity = function (series, pathIndex) {
    var that = this,
        result,
        length,
        seriesList;

    if (!series.isEnabled()) {
      return 0;
    }

    if (series.isShade()) {
      // This depth calculation ensures that the interpolated / normal graphs have the same fill opacity.
      var shadeDepth = Math.floor(pathIndex / 2) + 1;

      if (series.isBoolean()) {
        if (pathIndex < 2) {
          result = 0.2;
        } else {
          result = 0.7;
        }
      } else if (series.isDiscrete()) {
        length = modelUtil.getEnumRangeLength(series); //add 1 so that ordinal 0 is not opacity zero (that is skip data).

        result = 0.7 / length * shadeDepth;
      } else {
        //Numeric Shade gets split into 5 categories
        length = 5;
        result = 0.7 / length * shadeDepth;
      }

      seriesList = that.$graph.data();
      return modelUtil.hasLine(seriesList) || modelUtil.hasBar(seriesList) ? result * 2 / 3 : result; //reduce opacity if a line is also present
    }

    return null;
  };

  DataLayer.prototype.graphData = function (values) {
    var that = this,
        graph = that.$graph,
        lineSelection,
        areaSelection,
        barSelection,
        lineDataSet,
        areaDataSet,
        barDataSet; // One dataSet is a representation of an object containing a name, facets, and points
    // Create the data selection and separate it into its component parts.

    lineSelection = graph.chartd3().select('.lines').selectAll('.dataSet').data(modelUtil.getLines(values), function (d) {
      return d.ord();
    });
    areaSelection = graph.chartd3().select('.areas').selectAll('.dataSet').data(modelUtil.getAreas(values), function (d) {
      return d.ord();
    });
    barSelection = graph.chartd3().select('.bars').selectAll('.dataSet').data(modelUtil.getBars(values), function (d) {
      return d.ord();
    });
    lineDataSet = lineSelection.enter().append('g').attr('class', 'dataSet');
    lineDataSet.insert('path').attr('class', 'normal');
    lineDataSet.insert('path').attr('class', 'interpolated');
    areaDataSet = areaSelection.enter().append('g').attr('class', 'dataSet');
    barDataSet = barSelection.enter().append('g').attr('class', 'dataSet');
    areaDataSet.each(function (d, i) {
      if (!d) {
        return;
      }

      var select = d3.select(this),
          j;
      select.insert('path').attr('class', 'normal');
      select.insert('path').attr('class', 'interpolated');

      if (d.isBoolean()) {
        select.insert('path').attr('class', 'normal');
        select.insert('path').attr('class', 'interpolated');
      } else if (d.isDiscrete()) {
        for (j = 1; j < modelUtil.getEnumRangeLength(d); j++) {
          select.insert('path').attr('class', 'normal');
          select.insert('path').attr('class', 'interpolated');
        }
      } else {
        for (j = 1; j < 5; j++) {
          //5 total categories for mapping non-discrete to discrete
          select.insert('path').attr('class', 'normal');
          select.insert('path').attr('class', 'interpolated');
        }
      }

      addPatternToGraph(graph, d, that.fillColor(d));
    });
    barDataSet.each(function (d, i) {
      addPatternToGraph(graph, d, that.fillColor(d));
    });

    function configureSelection(selection) {
      // Give each line a color.
      selection.selectAll('path').classed('line', function (d) {
        return !d.isShade();
      }).classed('area', function (d) {
        return d.isShade();
      }).style('fill', function (d) {
        if (d.isShade()) {
          return that.fillColor(d);
        } else {
          return null;
        }
      }).style('fill-opacity', function (d, i) {
        if (d.isShade()) {
          return that.getFillOpacity(d, i);
        } else {
          return null;
        }
      }).style('stroke', function (d) {
        if (d.isShade()) {
          return null;
        } else {
          return d.color();
        }
      });
      selection.exit().remove();
    }

    configureSelection(lineSelection);
    configureSelection(areaSelection);
    areaSelection.selectAll('.interpolated').style('fill', function (d) {
      return 'url(#' + getPatternId(d) + ')';
    });
    barSelection.exit().remove();
  };

  DataLayer.prototype.redraw = function () {
    var that = this,
        graph = that.$graph,
        seriesList = graph.data(),
        widget = graph.widget(),
        model = widget.model(),
        settings = widget.settings(),
        barIndex = 0,
        statusBarIndex = 0;
    seriesList.forEach(function (series) {
      if (!series.isShade() && !series.isBar()) {
        return;
      }

      var rect = graph.chartd3().select("pattern#" + getPatternId(series) + " rect");
      rect.attr('fill', that.fillColor(series));
    }); // Redraw the lines

    graph.chartd3().selectAll('.lineContainer .dataSet').each(function (series, seriesIndex) {
      //lines and areas
      var svg = d3.select(this),
          normalPathSelection = svg.selectAll("path.normal"),
          interpolatedPathSelection = svg.selectAll("path.interpolated"),
          points = series.samplingPoints(),
          scale = series.valueScale().scale(),
          areaY,
          statusSelection,
          valueMin,
          valueMax,
          valueBlock,
          height,
          xScale,
          topArea,
          bottomArea,
          barLayout; //shade setup

      if (series.isShade()) {
        areaY = that.getAreaY(series);
        height = widget.dimensions().height;
        xScale = graph.getScaleX();
        topArea = height * areaY[0];
        bottomArea = height * areaY[1]; //numeric shade settup

        if (!series.isDiscrete()) {
          var _series$valueScale$ge = series.valueScale().getMinMax(false, [0, 10]);

          var _series$valueScale$ge2 = _slicedToArray(_series$valueScale$ge, 2);

          valueMin = _series$valueScale$ge2[0];
          valueMax = _series$valueScale$ge2[1];
          valueBlock = (valueMax - valueMin) / 4.0;

          if (!valueBlock) {
            valueBlock = 1;
          }

          for (var j = 0; j < points.length; j++) {
            if (points[j].discreteY === undefined) {
              points[j].discreteY = parseInt((points[j].y - valueMin) / valueBlock);
            }
          }
        }
      } // Interpolated tail handling


      if (series.isLine()) {
        interpolatedPathSelection.attr('d', function () {
          return d3.svg.line().interpolate(series.getLineInterpolation()).x(function (d) {
            return graph.getScaleX()(d.x);
          }).y(function (d, i) {
            return scale(d.y);
          }).defined(function (d, i) {
            return isDefinedForInterpolatedGraph(d, widget, points, i, true);
          })(points);
        }).style("stroke-opacity", function (d) {
          return d.isEnabled() ? 1 : 0;
        });
      } else if (series.isShade()) {
        var discrete = series.isDiscrete();
        interpolatedPathSelection.attr('d', function (data, index) {
          return d3.svg.area().interpolate("step-after").x(function (d) {
            return xScale(d.x);
          }).y1(function (d, i) {
            return topArea;
          }).y0(function (d, i) {
            if (discrete && d.y === index) {
              return bottomArea;
            } else if (!discrete && d.discreteY === index) {
              return bottomArea;
            } else {
              return topArea;
            }
          }).defined(function (d, i) {
            return isDefinedForInterpolatedGraph(d, widget, points, i, false);
          })(points);
        });
      } //lines and shades


      normalPathSelection.attr('d', function (data, index) {
        if (!points.length) {
          //instead of return null, return empty path to avoid d3 error: "Error: Problem parsing d=""
          return "M0 0";
        }

        var discrete = series.isDiscrete();

        if (series.isShade()) {
          //areas like enum and boolean
          return d3.svg.area().interpolate("step-after").x(function (d) {
            return xScale(d.x);
          }).y1(function (d, i) {
            return topArea;
          }).y0(function (d, i) {
            if (discrete && d.y === index) {
              return bottomArea;
            } else if (!discrete && d.discreteY === index) {
              return bottomArea;
            } else {
              return topArea;
            }
          }).defined(function (d) {
            return isDefinedForNormalGraph(d, widget);
          })(points);
        } else {
          //lines
          return d3.svg.line().interpolate(series.getLineInterpolation()).x(function (d) {
            return graph.getScaleX()(d.x);
          }).y(function (d, i) {
            return scale(d.y);
          }).defined(function (d) {
            return isDefinedForNormalGraph(d, widget);
          })(points);
        }
      }).style("stroke-opacity", function (d) {
        return d.isEnabled() ? 1 : 0;
      }); //bars

      if (series.isBar()) {
        height = widget.dimensions().height;
        var barSelection = d3.select(this).selectAll("rect.bar").data(points),
            y2 = scale(0),
            showDataGaps = widget.settings().getShowDataGaps(),
            color = series.color();

        if (!barLayout) {
          barLayout = modelUtil.getBarLayout(model);
        }

        barSelection.enter().append("rect").attr("class", "bar");
        barSelection.exit().remove();
        barSelection.style("opacity", function () {
          return series.isEnabled() ? null : 0;
        }).style("fill", function (point, index) {
          var patternText = 'url(#' + getPatternId(series) + ')';

          if (point.interpolated) {
            return patternText;
          }

          var isSkipBar = isBarChartDataGap(points, index);

          if (isSkipBar && showDataGaps === "dotted") {
            return patternText;
          }

          return color;
        }).attr("x", function (d) {
          if (d.interpolated) {
            return graph.getScaleX()(d.x) + barIndex * modelUtil.getBarWidth();
          }

          return modelUtil.getBarX(d.x, barIndex, barLayout, graph.getScaleX());
        }).attr("y", function (d) {
          if (d.y < 0) {
            return scale(0);
          }

          var y1 = scale(d.y); // ensure that barGraphs have a minimum height of 4 pixels to help orientate users.

          if (Math.abs(y2 - y1) < 4) {
            if (y2 - y1 >= 0) {
              y1 -= 4;
            }
          }

          return y1;
        }).attr("width", function (d, i) {
          if (!d.interpolated) {
            return Math.max(barLayout.barWidth(d.x), 0);
          }

          var scaleX = graph.getScaleX();

          if (modelUtil.isPointBeforeLastBarGroup(model, d, barLayout, scaleX)) {
            return 0;
          }

          return modelUtil.getBarWidth(model);
        }).attr("height", function (d, index) {
          if (d.skip) {
            return 0;
          }

          if (isBarChartDataGap(points, index) && showDataGaps === "gap") {
            return 0;
          }

          var y1 = scale(d.y); // ensure that barGraphs have a minimum height of 4 pixels to help orientate users.

          if (Math.abs(y2 - y1) < 4) {
            if (y2 - y1 < 0) {
              y1 += 4;
            } else {
              y1 -= 4; //slightly negative means to add extra below the line.
            }
          }

          return Math.abs(y2 - y1);
        });
        barIndex++;
      } //statusColoring


      if (settings.getStatusColoring() === 'off' || series.isShade() && modelUtil.hasLine(seriesList)) {
        points = [];
      }

      if (series.isShade() && areaY !== undefined) {
        //status area
        var y = widget.dimensions().height * areaY[0] + 6;
        statusSelection = d3.select(this).selectAll("line.status").data(points);
        statusSelection.enter().append("line").attr("class", "status");
        statusSelection.exit().remove();
        statusSelection.attr("y1", y).attr("y2", y).style("stroke-opacity", function () {
          return series.isEnabled() ? 1 : 0;
        }).style("stroke-width", function (d) {
          return webChartUtil.hasStatusColor(d.status) && !d.skip ? 12 : 0; //about 1em
        }).attr("x1", function (d) {
          return graph.getScaleX()(d.x);
        }).attr("x2", function (d) {
          var next = points[points.indexOf(d) + 1];

          if (next && !next.skip) {
            return graph.getScaleX()(next.x);
          }

          return graph.getScaleX()(d.x);
        }).each(function (d) {
          var select = d3.select(this),
              statusColor = webChartUtil.statusToColor(d.status);
          select.style("stroke", function () {
            var result = statusColor.bgColor;

            if (!result) {
              result = that.fillColor(series);
            }

            return result;
          });
        });
      } else if (series.isBar()) {
        statusSelection = d3.select(this).selectAll("line.status").data(points);
        statusSelection.enter().append("line").attr("class", "status");
        statusSelection.exit().remove();
        statusSelection.style("opacity", function (point) {
          return series.isEnabled() ? null : 0;
        }).style("stroke-width", function (point, i) {
          var scaleX = graph.getScaleX();

          if (!webChartUtil.hasStatusColor(point.status)) {
            return 0;
          }

          if (isBarChartDataGap(points, i) && showDataGaps === "gap") {
            return 0;
          }

          if (!point.interpolated) {
            return !point.skip ? barLayout.barWidth(point.x) : 0;
          }

          return modelUtil.isPointBeforeLastBarGroup(model, point, barLayout, scaleX) ? 0 : modelUtil.getBarWidth(model);
        }).each(function (point) {
          //grouping positioning offset
          var elem = d3.select(this),
              barWidth = point.interpolated ? modelUtil.getBarWidth(model) : barLayout.barWidth(point.x),
              x = point.interpolated ? graph.getScaleX()(point.x) + (barIndex - 1) * modelUtil.getBarWidth() + barWidth / 2 : modelUtil.getBarX(point.x, statusBarIndex, barLayout, graph.getScaleX()) + barWidth / 2,
              y1 = scale(point.y),
              y2;

          if (point.y >= 0) {
            y2 = Math.min(scale(0), y1 + 12);
          } else {
            y2 = Math.max(scale(0), y1 - 12);
          }

          elem.attr({
            x1: x,
            x2: x,
            y1: y1,
            y2: y2
          }); //coloring

          var select = d3.select(this),
              statusColor = webChartUtil.statusToColor(point.status);
          select.style("stroke", function () {
            var result = statusColor.bgColor;

            if (!result) {
              result = that.fillColor(series);
            }

            return result;
          });
        });
        statusBarIndex++;
      } else {
        //status line
        statusSelection = d3.select(this).selectAll("circle").data(points);
        statusSelection.enter().append("circle").attr("class", "dot").attr("r", 2.75);
        statusSelection.exit().remove();
        statusSelection.style("fill-opacity", function () {
          return series.isEnabled() ? 1 : 0;
        }).attr("cx", function (d) {
          return graph.getScaleX()(d.x);
        }).attr("cy", function (d) {
          return scale(d.y);
        }).each(function (d) {
          var select = d3.select(this),
              statusColor = webChartUtil.statusToColor(d.status);
          select.style("fill", function () {
            var result = statusColor.bgColor;

            if (!result) {
              result = that.fillColor(series);
            }

            return result;
          });
        });
      }
    });
  };
  /**
   * Get the top and bottom percent of the height
   * @param {BaseSeries} series
   * @returns {Array.<Number>}
   */


  DataLayer.prototype.getAreaY = function (series) {
    var that = this,
        graph = that.$graph,
        i,
        seriesList = graph.data(),
        discreteCount = 0,
        discreteIndex = -1;

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].ord() === series.ord()) {
        discreteIndex = discreteCount;
        discreteCount++;
      } else if (seriesList[i].isShade()) {
        discreteCount++;
      }
    }

    return [discreteIndex / discreteCount, (discreteIndex + 1) / discreteCount];
  };
  /**
   * Returns the color of the BaseSeries.
   *
   * @param {BaseSeries} d
   * @returns {String}
   */


  DataLayer.prototype.fillColor = function (d) {
    return d.color();
  };

  function isDefinedForNormalGraph(d, widget) {
    if (widget.settings().getShowStartTrendGaps() === "solid" && d.startTrend) {
      return true;
    }

    if (widget.settings().getShowDataGaps() === "solid" && d.skip && !d.startTrend) {
      return true;
    }

    return !d.skip && !d.interpolated;
  }

  function isDefinedForInterpolatedGraph(d, widget, points, index, isLine) {
    var previousPointIndex = index - 1,
        nextPointIndex = index + 1;

    if (widget.isInterpolateTail()) {
      if (d.interpolated) {
        return true;
      }

      if (nextPointIndex < points.length && points[nextPointIndex].interpolated) {
        return true;
      }
    }

    if (widget.settings().getShowStartTrendGaps() === "dotted") {
      if (d.startTrend) {
        return true;
      }

      if (previousPointIndex >= 0 && points[previousPointIndex].startTrend) {
        return true;
      }

      if (nextPointIndex < points.length && points[nextPointIndex].startTrend) {
        return true;
      }
    }

    if (widget.settings().getShowDataGaps() === "dotted") {
      if (d.skip && !d.startTrend) {
        return true;
      }

      if (previousPointIndex >= 0 && points[previousPointIndex].skip && !points[previousPointIndex].startTrend) {
        return true;
      }

      if (isLine && nextPointIndex < points.length && points[nextPointIndex].skip && !points[nextPointIndex].startTrend) {
        return true;
      }
    }

    return false;
  }

  function getPatternId(series) {
    return "series_" + series.ord().toString().replace(/[^a-zA-Z0-9]/g, '');
  }

  function addPatternToGraph(graph, series, color) {
    graph.chartd3().append("defs").append("pattern").attr({
      id: getPatternId(series),
      width: "8",
      height: "8",
      patternUnits: "userSpaceOnUse",
      patternTransform: "rotate(60)"
    }).append("rect").attr({
      width: "4",
      height: "8",
      transform: "translate(0,0)",
      fill: color
    });
  }

  return DataLayer;
});
