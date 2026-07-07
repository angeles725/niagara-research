/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson and JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/YAxisLayer
 */
define(["Promise", "d3", 'nmodule/webChart/rc/model/modelUtil', "nmodule/webChart/rc/webChartUtil"], function (Promise, d3, modelUtil, webChartUtil) {
  "use strict";
  /**
   * A Layer in a Line Chart to handle the Y-Axis.
   *
   * @class
   * @alias module:nmodule/webChart/rc/line/YAxisLayer
   * @param graph
   */

  var YAxisLayer = function YAxisLayer(graph) {
    var that = this;
    that.$graph = graph;
  };

  YAxisLayer.prototype.isOrientLeft = function () {
    return this.$graph.isYAxisOrientLeft();
  };

  YAxisLayer.className = function (d) {
    return d ? 'y-axis-minor' : 'y-axis-major';
  };
  /**
   * Rescale each of the Y Axis Scales.
   */


  YAxisLayer.prototype.rescale = function () {
    var that = this,
        graph = that.$graph,
        domain,
        fallbackDomain,
        widget = graph.widget(); // Reset the scales to map to the new dimensions of the container

    widget.model().mapValueScales(function (valueScale) {
      valueScale.scale().range([widget.dimensions().height, 0]);

      if ((widget.isDataZoom() || widget.isTimeZoom()) && !valueScale.isLocked()) {
        fallbackDomain = [0, 10];
        var minMax = valueScale.getMinMax(true, fallbackDomain);
        domain = minMax;
        valueScale.scale().domain(that.checkDomain(domain, fallbackDomain).newDomain);
      }
    });
  };
  /**
   * Redraw the YAxisLayer, don't return until layout is complete so that other layers can do proper
   * height/width calculations.
   * @returns {Promise}
   */


  YAxisLayer.prototype.redraw = function () {
    var that = this; // eslint-disable-next-line promise/avoid-new

    return new Promise(function (resolve) {
      var graph = that.$graph,
          chart = graph.chartd3(),
          widget = graph.widget(),
          settings = widget.settings(),
          ticksGroup = chart.selectAll('g.ticks'),
          ticksGroup2 = chart.selectAll('g.ticks2'),
          dom = graph.widget().jq3(),
          promises = [],
          model = widget.model(),
          primaryValueScale = model.primaryValueScale(),
          secondaryValueScale = model.secondaryValueScale(),
          primaryScale = primaryValueScale.scale(),
          secondaryScale = secondaryValueScale ? secondaryValueScale.scale() : null,
          primarySeries = primaryValueScale.primarySeries(),
          secondarySeries = secondaryValueScale ? secondaryValueScale.primarySeries() : null,
          isPrimaryDiscrete = primarySeries ? primarySeries.isDiscrete() : false,
          isSecondaryDiscrete = secondarySeries ? secondarySeries.isDiscrete() : false,
          gy,
          gye,
          gye2;

      var fy = function fy(d, i) {
        var selection = d3.select(this);

        if (primarySeries) {
          promises.push(primarySeries.resolveYDisplay(d).then(function (display) {
            selection.text(display);
          }));
        } else {
          promises.push(modelUtil.resolveNumericDisplay(d, {
            units: null,
            precision: 2
          }).then(function (display) {
            selection.text(display);
          }));
        }
      },
          ty = function ty(d) {
        return 'translate(0,' + primaryScale(d) + ')';
      },
          left = that.isOrientLeft(),
          width = widget.dimensions().width,
          fy2,
          lines,
          linesOther,
          ty2,
          gy2;

      that.rescale(); // Regenerate y-ticks…

      gy = ticksGroup.selectAll('g.y').data(primaryValueScale.scaleTicks(), function (d, i) {
        return primaryScale(d) + ":" + i; //ensures all d3 generated ticks are considered "enter data"
      }).attr('transform', ty);
      gy.select('text.primary').each(fy).attr('x', left ? -3 : 3).attr('text-anchor', left ? 'end' : 'start').attr('transform', left ? null : "translate(" + widget.dimensions().width + ", 0)");

      if (secondarySeries && (isSecondaryDiscrete || isPrimaryDiscrete)) {
        // Regenerate y-ticks…
        ty2 = function ty2(d) {
          var result = secondaryScale(d);
          return 'translate(0,' + result + ')';
        };

        gy2 = ticksGroup2.selectAll('g.y').data(secondaryValueScale.scaleTicks(), function (d, i) {
          return secondaryScale(d) + ":" + i; //ensures all d3 generated ticks are considered "enter data"
        }).attr('transform', ty2);

        fy2 = function fy2(d) {
          var selection = d3.select(this);
          promises.push(secondarySeries.resolveYDisplay(d).then(function (display) {
            selection.text(display);
          }));
        };

        gy2.select('text.secondary').each(fy2).attr('x', left ? 3 : -3).attr('text-anchor', left ? 'start' : 'end').attr('transform', left ? "translate(" + widget.dimensions().width + ", 0)" : null);
        gy.select('text.secondary').text("");
      } else {
        gy2 = ticksGroup2.selectAll('g.y').data([]);

        fy2 = function fy2(d, i) {
          if (!secondaryScale || !secondarySeries) {
            return null;
          }

          var value = secondaryScale.invert(primaryScale(d)),
              selection = d3.select(this);
          promises.push(secondarySeries.resolveYDisplay(value).then(function (display) {
            selection.text(display);
          }));
        };

        if (secondaryScale && secondarySeries) {
          gy.select('text.secondary').each(function () {
            if (secondarySeries && !isSecondaryDiscrete && !isPrimaryDiscrete) {
              fy2.apply(this, arguments);
            }
          }).attr('x', left ? 3 : -3).attr('text-anchor', left ? 'start' : 'end').attr('transform', left ? "translate(" + widget.dimensions().width + ", 0)" : null);
        } else {
          gy.select('text.secondary').text(""); //ensure ticks are blank if secondary is no longer used
        }
      }

      gy.select('line').attr('class', YAxisLayer.className);
      gy2.select('line').attr('class', YAxisLayer.className);
      gye = gy.enter().insert('g', '.lineContainer').attr('class', 'y').attr('transform', ty).attr('background-fill', '#FFEEB6');
      gye2 = gy2.enter().insert('g', '.lineContainer').attr('class', 'y').attr('transform', ty2).attr('background-fill', '#FFEEB6');
      gy.exit().remove();
      gy2.exit().remove();

      if (settings.hasShowGrid()) {
        gye2.append('line').attr('x1', 0).attr('class', function (d) {
          return YAxisLayer.className(secondaryScale(d));
        });
        gye.append('line').attr('x1', 0).attr('class', YAxisLayer.className);
      }

      function yaxisDown(primary) {
        if (primary && widget.model().primaryValueScale().isLocked()) {
          return;
        } else {
          var secondary = widget.model().secondaryValueScale();

          if (!primary && secondary && secondary.isLocked()) {
            return;
          }
        }

        var p = d3.mouse(chart.node());
        that.$yAxisDragData = p[1];
      }

      function yaxisDrag() {
        var p = d3.mouse(chart.node()),
            changeRatio,
            scaled = false;

        if (typeof that.$yAxisDragData === "number") {
          widget.model().mapValueScales(function (valueScale) {
            if (valueScale.isLocked() || valueScale.seriesList().length === 0 && widget.model().seriesList().length > 0) {
              return;
            }

            scaled = true;
            dom.style('cursor', 'n-resize');
            var scale = valueScale.scale(),
                diff = that.$yAxisDragData - p[1],
                translate,
                overlay,
                relativeCenter,
                zoom;

            if (!diff) {
              return;
            }

            graph.widget().manualZoom();
            overlay = d3.select(".line-container-overlay");
            relativeCenter = webChartUtil.getRelativeCenter(overlay);

            if (diff > 0) {
              //mouse up
              changeRatio = 1 + 0.148698354997035 / 4; // 25% of mousewheel

              translate = webChartUtil.zoomCenter(relativeCenter, changeRatio);
            } else {
              //mouse down
              changeRatio = 1 - 0.148698354997035 / 4;
              translate = webChartUtil.zoomCenter(relativeCenter, changeRatio);
            }

            zoom = d3.behavior.zoom();
            zoom.center(relativeCenter);
            zoom.y(scale);
            valueScale.zoom = zoom;
            zoom.scale(changeRatio).translate(translate);
            graph.widget().redrawSoon();
            d3.event.preventDefault();
            d3.event.stopPropagation();
          });
          that.$yAxisDragData = p[1];
        }

        return scaled;
      }

      function yaxisUp() {
        dom.style('cursor', 'auto');

        if (typeof that.$yAxisDragData === "number") {
          graph.widget().redrawSoon();
          that.$yAxisDragData = undefined;
          d3.event.preventDefault();
          d3.event.stopPropagation();
        }
      }

      gye.append('text').attr('class', 'axis y-axis primary').attr('x', left ? -3 : 3).attr('dy', '0.35em').attr('text-anchor', left ? 'end' : 'start').each(fy).attr('transform', left ? null : "translate(" + width + ", 0)");
      gye.append('text').attr('class', 'axis y-axis secondary').attr('x', left ? 3 : -3).attr('dy', '0.35em').attr('text-anchor', left ? 'start' : 'end').each(function () {
        if (secondarySeries && !isSecondaryDiscrete && !isPrimaryDiscrete) {
          fy2.apply(this, arguments);
        }
      }).attr('transform', left ? "translate(" + widget.dimensions().width + ", 0)" : null);
      gye2.append('text').attr('class', 'axis y-axis secondary').attr('x', left ? 3 : -3).attr('dy', '0.35em').attr('text-anchor', left ? 'start' : 'end').each(fy2).attr('transform', left ? "translate(" + width + ", 0)" : null);
      dom.on('mousemove.yaxisdrag', yaxisDrag).on('touchmove.yaxisdrag', yaxisDrag).on('mouseup.yaxisdrag', yaxisUp).on('touchend.yaxisdrag', yaxisUp);

      if (secondarySeries && !isSecondaryDiscrete && isPrimaryDiscrete) {
        lines = ticksGroup2.selectAll('g.y line');
        linesOther = ticksGroup.selectAll('g.y line');
      } else {
        lines = ticksGroup.selectAll('g.y line');
        linesOther = ticksGroup2.selectAll('g.y line');
      }

      if (settings.hasShowGrid()) {
        if (!lines.node()) {
          //need to add lines back
          lines.selectAll('g.y').append('line').attr('x1', 0).attr('class', YAxisLayer.className).attr('x2', width);
        } else {
          lines.attr('x2', width);
        }
      } else {
        lines.remove();
      }

      linesOther.remove();
      chart.selectAll('rect.primary-y-ticks').attr('y', -10).attr('height', widget.dimensions().height + 20) //.attr('width', widget.dimensions().width + 20) //wait for calculations computed in Labels Layer
      .on('mousedown.yaxisdrag', function () {
        yaxisDown(true);
      }).on('touchstart.yaxisdrag', function () {
        yaxisDown(true);
      });
      chart.selectAll('rect.secondary-y-ticks').attr('y', -10).attr('height', widget.dimensions().height + 20) //.attr('width', widget.dimensions().width + 20) //wait for calculations computed in Labels Layer
      .on('mousedown.yaxisdrag', function () {
        yaxisDown(false);
      }).on('touchstart.yaxisdrag', function () {
        yaxisDown(false);
      });
      resolve(Promise.all(promises));
    });
  };

  YAxisLayer.prototype.graphData = function () {};
  /**
   * Ensure that the current domain is reasonable.
   *
   * @param desiredDomain
   * @param [fallbackDomain]
   */


  YAxisLayer.prototype.checkDomain = function (desiredDomain, fallbackDomain) {
    var results = {
      newDomain: desiredDomain
    },
        y1 = desiredDomain[0],
        y2 = desiredDomain[1];

    if (y1 === undefined && y2 === undefined) {
      results.newDomain = fallbackDomain;
      results.notDefined = true;
    } else if (y1 === y2) {
      results.newDomain = [y1 - 4, y2 + 4];
      results.tooSmall = true;
    }

    results.newDomain = modelUtil.stretchDomain(this.$graph.widget().model(), results.newDomain);
    return results;
  };

  YAxisLayer.prototype.destroy = function () {
    var dom = this.$graph.widget().jq3();

    if (dom) {
      dom.on('mousemove.yaxisdrag', null).on('touchmove.yaxisdrag', null).on('mouseup.yaxisdrag', null).on('touchend.yaxisdrag', null);
    }
  };

  return YAxisLayer;
});
