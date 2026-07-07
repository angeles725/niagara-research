function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson and JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/line/XAxisLayer
 */
define(["d3", "nmodule/webChart/rc/webChartUtil", "nmodule/webChart/rc/localeUtil", "nmodule/webChart/rc/model/modelUtil", "moment"], function (d3, webChartUtil, localeUtil, modelUtil, moment) {
  "use strict";

  var dateTimeUtil = require('bajaScript/baja/obj/dateTimeUtil');

  function trimDatesToBoundaries(ticks, startDate, endDate) {
    while (ticks[0].valueOf() < startDate.valueOf() && ticks.length > 0) {
      ticks.splice(0, 1);
    }

    while (ticks[ticks.length - 1].valueOf() > endDate.valueOf() && ticks.length > 0) {
      ticks.splice(ticks.length - 1, 1);
    }
  }

  function padDatesToBoundaries(ticks, startDate, endDate) {
    var startMoment = moment(startDate),
        endMoment = moment(endDate),
        targetIntervalHours = moment(ticks[1]).diff(moment(ticks[0]), 'hours'),
        targetIntervalDays = moment(ticks[1]).diff(moment(ticks[0]), 'days');

    if (targetIntervalDays >= 28 && targetIntervalDays <= 31) {
      while (moment(ticks[0]).diff(startMoment, 'months') >= 1) {
        ticks.unshift(moment(ticks[0]).subtract(1, 'months').toDate());
      }
    } else if (targetIntervalDays >= 1 && targetIntervalDays <= 10) {
      while (moment(ticks[0]).diff(startMoment, 'days') >= targetIntervalDays) {
        ticks.unshift(moment(ticks[0]).subtract(targetIntervalDays, 'days').toDate());
      }
    } else if (targetIntervalHours < 24 && targetIntervalHours >= 1) {
      while (moment(ticks[0]).diff(startMoment, 'hours') >= targetIntervalHours) {
        ticks.unshift(moment(ticks[0]).subtract(targetIntervalHours, 'hours').toDate());
      }

      while (endMoment.diff(moment(ticks[ticks.length - 1]), 'hours') >= targetIntervalHours) {
        ticks.push(moment(ticks[ticks.length - 1]).add(targetIntervalHours, 'hours').toDate());
      }
    }
  }
  /**
   * A Layer in a Line Chart to handle the X-Axis.
   *
   * @class
   * @alias module:nmodule/webChart/rc/line/XAxisLayer
   */


  var XAxisLayer = function XAxisLayer(graph) {
    var that = this;
    that.$graph = graph;
  };

  XAxisLayer.className = function (d) {
    return d ? 'x-axis-minor' : 'x-axis-major';
  };
  /**
   * Provide a fallback Time for domain calculation. Times are slightly broadened if bar is use to show the full bar
   *
   * @param graph
   * @param propertyName
   * @param {moment} fallbackValue, domain value to use if time unknown
   * @returns {moment} the domain value to use
   * @inner
   */


  function fixedTime(graph, seriesList, propertyName, fallbackValue) {
    var timeRange = graph.widget().model().timeRange(),
        timezone = graph.widget().model().timeScale().getTimeZone();

    if (timeRange.getPeriod().getOrdinal() > 0 && graph.widget().isTimeZoom() || graph.widget().isDataZoom() && !graph.widget().model().hasPoints() && timeRange.getPeriod().getOrdinal() !== 0) {
      if (timeRange.getPeriod().getOrdinal() === 1) {
        //timeRange
        return timeRange.get(propertyName).getJsDate();
      } else {
        var results = webChartUtil.getStartAndEndDateFromPeriod(timeRange.getPeriod(), timezone);

        if (propertyName === "startTime") {
          return results.start;
        } else if (propertyName === "endTime") {
          return results.end;
        }
      }
    } else if (!graph.data().length || !graph.widget().model().hasPoints() && timeRange.getPeriod().getOrdinal() === 0) {
      return fallbackValue;
    }
  }
  /**
   * Broaden the times if bar (to show the full bar period) or shape (to show extra point)
   * @param {Line} graph
   * @param {Array.<BaseSeries>} seriesList
   * @param {Array.<Number>} domain
   * @returns {Array.<Number|Date>} adjusted Domain if bar is present
   */


  XAxisLayer.broadenDomain = function (graph, seriesList, domain) {
    var newDomain = [domain[0], domain[1]];

    if (modelUtil.hasBar(seriesList) && graph.widget().model().samplingMillis()) {
      var amount = graph.widget().model().samplingMillis();

      if (amount) {
        newDomain[0] = modelUtil.safeMomentAdd(newDomain[0], -amount / 2).toDate();
        newDomain[1] = modelUtil.safeMomentAdd(newDomain[1], amount).toDate();
      }
    }

    if (modelUtil.hasVisibleShade(seriesList, graph.widget().model().timeScale().getTimeZone(), graph.widget().model().timeRange())) {
      newDomain[1] = new Date(Math.max(moment(newDomain[1]).toDate(), modelUtil.safeMomentAdd(domain[1], modelUtil.getTimeForPixels(graph.widget().model(), 6)).toDate()));
    }

    return newDomain;
  };
  /**
   * Setup domain based on the present information, data will usually be used, but fallbackDomaincan be utilized if the data doesn't provide it.
    * @param {module:nmodule/webChart/rc/line/Line} graph
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries> | module:nmodule/webChart/rc/model/BaseSeries} seriesList or series for domain calculation
   * @param {Array.<moment>} fallbackDomain
   * @returns {Array.<moment>} adjusted Domain
   * @inner
   */


  XAxisLayer.domain = function (graph, seriesList, fallbackDomain) {
    var widget = graph.widget(),
        timezone = widget.model().timeScale().getTimeZone();

    if (!(seriesList instanceof Array)) {
      seriesList = [seriesList];
    }

    var minFixed = fixedTime(graph, seriesList, "startTime", fallbackDomain[0]);
    var maxFixed = fixedTime(graph, seriesList, "endTime", fallbackDomain[1]);

    if (minFixed !== undefined && maxFixed !== undefined) {
      return [minFixed, maxFixed];
    }

    var min = Number.POSITIVE_INFINITY,
        max = Number.NEGATIVE_INFINITY;

    for (var i = 0; i < seriesList.length; i++) {
      var points = seriesList[i].points();

      if (points && points[0] !== undefined) {
        min = Math.min(min, points[0].x.getTime());
      }

      if (points && points[points.length - 1] !== undefined) {
        max = Math.max(max, points[points.length - 1].x.getTime());
      }
    }

    if (widget.isDataZoom()) {
      var timeRange = widget.model().timeRange();
      var startAndEnd = webChartUtil.getStartAndEndDateFromTimeRange(timeRange, timezone);
      var isStartSet = !startAndEnd.start.isSame(moment(new Date(0)));
      var isEndSet = !startAndEnd.end.isSame(moment(new Date(0)));

      if (isStartSet && isEndSet) {
        if (!webChartUtil.isInTimeRange(startAndEnd.start, startAndEnd.end, moment(min))) {
          min = startAndEnd.start;
        }

        if (!webChartUtil.isInTimeRange(startAndEnd.start, startAndEnd.end, moment(max))) {
          max = startAndEnd.end;
        }
      } else if (isStartSet) {
        if (startAndEnd.start.isAfter(moment(min))) {
          min = startAndEnd.start;
        }
      } else if (isEndSet) {
        if (moment(max).isAfter(startAndEnd.end)) {
          max = startAndEnd.end;
        }
      }
    }

    if (widget.isInterpolateTail()) {
      max = Math.max(moment().valueOf(), max.valueOf());
    }

    if (min === Number.POSITIVE_INFINITY && max === Number.NEGATIVE_INFINITY) {
      return fallbackDomain;
    }

    return [moment(min), moment(max)];
  };
  /**
   * Ensure a domain is in range
   * @param fallbackDomain
   * @param desiredDomain
   * @return {{tooHigh: boolean, tooLow: boolean, tooSmall: boolean, newDomain: *}}
   */


  XAxisLayer.checkDomain = function (desiredDomain, fallbackDomain) {
    var results = {
      tooHigh: false,
      tooLow: false,
      tooSmall: false,
      newDomain: desiredDomain
    },
        x1 = desiredDomain[0],
        x2 = desiredDomain[1],
        d;

    if (x1 instanceof Date) {
      if (x1.getFullYear() < 1900 || x2.getFullYear() <= 1900) {
        results.tooLow = true;
        d = new Date();
        d.setFullYear(1900);
        results.newDomain[0] = d;

        if (x2.getFullYear() <= 1900) {
          d = new Date();
          d.setFullYear(1901);
          results.newDomain[1] = d;
        }
      }

      if (x1.getFullYear() > 2200 || x2.getFullYear() >= 2200) {
        results.tooHigh = true;
        d = new Date();
        d.setFullYear(2200);
        results.newDomain[1] = moment(d).endOf("year").toDate();

        if (x1.getFullYear() >= 2200) {
          d = new Date();
          d.setFullYear(2199);
          results.newDomain[1] = moment(d).endOf("year").toDate();
        }
      } else if (moment(x2).diff(moment(x1)) < 5000) {
        //time difference is too small (5 seconds)
        results.tooSmall = true;
        results.newDomain = fallbackDomain;
      }
    }

    if (!results.newDomain[0]) {
      results.newDomain[0] = fallbackDomain[0];
    }

    if (!results.newDomain[1]) {
      results.newDomain[1] = fallbackDomain[1];
    }

    return results;
  };

  XAxisLayer.prototype.getPeriodDisplay = function () {
    var that = this,
        timeScale = that.$graph.widget().model().timeScale(),
        timezone = timeScale.getTimeZone(),
        xdomain = that.$scale.domain(),
        start = moment(xdomain[0]),
        end = moment(xdomain[1]),
        dateFormat = localeUtil.getD3DateFormat(),
        timeFormat = localeUtil.getD3TimeFormat(),
        hourMinuteFormat = localeUtil.getD3HourMinuteFormat(),
        startTimezoneDisplayName = timezone ? timezone.getShortDisplayName(dateTimeUtil.isDstActive(start.toDate(), timezone)) : "",
        endTimezoneDisplayName = timezone ? timezone.getShortDisplayName(dateTimeUtil.isDstActive(end.toDate(), timezone)) : "",
        startDisplay = localeUtil.format(dateFormat, start, timezone),
        endDisplay = localeUtil.format(dateFormat, end, timezone);

    if (startDisplay === "Invalid date" || endDisplay === "Invalid date") {
      return "";
    } else if (startDisplay === endDisplay) {
      if (localeUtil.format(hourMinuteFormat, start, timezone) !== localeUtil.format(hourMinuteFormat, end, timezone)) {
        //same day, but at least a different minute
        if (start.format("h") === end.format("h") && localeUtil.hasPeriod()) {
          //am and pm is same to add to time summary
          return startDisplay + " " + start.format("a") + " " + startTimezoneDisplayName;
        } else {
          return startDisplay + " " + startTimezoneDisplayName;
        }
      } else {
        //minute (and second) display is required on high zoom level
        return startDisplay + " (" + webChartUtil.lex.get("dateTo", localeUtil.format(timeFormat, start, timezone), localeUtil.format(timeFormat, end, timezone)) + ")";
      }
    } else {
      return webChartUtil.lex.get("dateTo", startDisplay + " " + startTimezoneDisplayName, endDisplay + " " + endTimezoneDisplayName);
    }
  };
  /**
   * Get the primary data set for the x axis, pick the first series with data to zoom in on
   * @returns {module:nmodule/webChart/rc/model/BaseSeries}
   */


  XAxisLayer.prototype.getPrimarySeries = function () {
    var that = this,
        graph = that.$graph,
        seriesList = graph.data(),
        i;

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].points().length) {
        return seriesList[i];
      }
    }

    for (i = 0; i < seriesList.length; i++) {
      if (!seriesList[i].points().length) {
        return seriesList[i];
      }
    }
  };
  /**
   * Initialize the XAxisLayer to cache the models timeScale.
   */


  XAxisLayer.prototype.initialize = function () {
    var that = this,
        graph = that.$graph;
    that.$scale = graph.widget().model().timeScale().scale();
  };
  /**
   * Gets the ticks to be displayed. Cleans the ticks if they are from set intervals
   * such as 6 AM and 12 PM to make sure these times match the timezone of the
   * timeScale.
   * @private
   * @param {number} tickCount
   * @returns {Array.<Date>}
   */


  XAxisLayer.prototype.$getTicks = function (tickCount) {
    var that = this,
        scale = that.$scale,
        graph = that.$graph,
        widget = graph.widget(),
        ticks = scale.ticks(tickCount),
        domain = scale.domain();

    if (ticks.length >= 2) {
      var firstDate = moment(ticks[0]),
          secondDate = moment(ticks[1]),
          diffHours = secondDate.diff(firstDate, 'hours'); // This handles dates that need to be offset due to the timezone possibly
      // being different than where the ticks were generated by d3.

      if (diffHours > 1) {
        for (var i = 0; i < ticks.length; i++) {
          var utcOffset = widget.model().timeScale().getDisplayedTimezoneOffset(ticks[i]);
          var momentInTimeZone = moment(ticks[i]).utcOffset(utcOffset, true);
          ticks[i] = momentInTimeZone.toDate();
        }

        padDatesToBoundaries(ticks, domain[0], domain[1]);
        trimDatesToBoundaries(ticks, domain[0], domain[1]);
      }
    }

    return ticks;
  };
  /**
   * Returns the fallback domain
   * @private
   * @returns {Array.<Date>}
   */


  XAxisLayer.prototype.$getFallbackDomain = function () {
    var that = this,
        timeScale = that.$graph.widget().model().timeScale(),
        timezone = timeScale.getTimeZone(),
        startFallback = webChartUtil.startOf('day', timezone, moment()),
        endFallback = webChartUtil.endOf('day', timezone, moment());
    return [startFallback, endFallback];
  };

  XAxisLayer.prototype.redraw = function () {
    var that = this,
        graph = that.$graph,
        chart = graph.chartd3(),
        widget = graph.widget(),
        settings = widget.settings(),
        ticksGroup = chart.selectAll('g.ticks'),
        dom = graph.widget().jq3(),
        seriesList,
        gx,
        gxe,
        desiredDomain,
        fallbackDomain,
        scale = that.$scale,
        fx = function fx(date) {
      return scale.tickFormat(10)(date);
    },
        tx = function tx(d) {
      return 'translate(' + scale(d) + ',0)';
    }; // Reset the scales to map to the new dimensions of the container


    scale.range([0, widget.dimensions().width]);

    if (widget.isDataZoom() || widget.isTimeZoom()) {
      if (settings.getDataZoomScope() === 'primary') {
        seriesList = that.getPrimarySeries();

        if (seriesList) {
          seriesList = [seriesList]; //ensure its an array if present
        }
      } else {
        seriesList = graph.data();
      }

      fallbackDomain = that.$getFallbackDomain();
      desiredDomain = XAxisLayer.domain(graph, seriesList, fallbackDomain);
      scale.domain(XAxisLayer.checkDomain(desiredDomain, fallbackDomain).newDomain);
    } //now that focus data is available, calculate the sampling data for rest of draw


    widget.model().startSampling();

    function xaxisDown() {
      graph.widget().manualZoom();
      var p = d3.mouse(chart.node());
      that.$xAxisDragData = scale.invert(p[0]);
    }

    function xaxisDrag() {
      var p, newDomain, dragDistance, checkDomainResults, rupx, xaxis1, xaxis2;

      if (_typeof(that.$xAxisDragData) === "object") {
        p = d3.mouse(chart.node());
        graph.widget().manualZoom();
        dom.style('cursor', 'w-resize');
        rupx = +scale.invert(p[0]);
        xaxis1 = +scale.domain()[0];
        xaxis2 = +scale.domain()[1]; //xextent = xaxis2 - xaxis1;

        if (rupx !== 0) {
          dragDistance = that.$xAxisDragData - rupx; //newDomain = [new Date(xaxis1), new Date(xaxis1 + xextent + dragDistance)]; //zoom into future

          newDomain = [new Date(xaxis1 + dragDistance), new Date(xaxis2)]; //zoom into past

          checkDomainResults = XAxisLayer.checkDomain(newDomain, scale.domain());

          if (checkDomainResults.tooSmall || checkDomainResults.tooHigh || checkDomainResults.tooLow) {
            newDomain = checkDomainResults.newDomain;
          }

          scale.domain(newDomain);
          graph.widget().redrawSoon();
        }

        d3.event.preventDefault();
        d3.event.stopPropagation();
      }
    }

    function xaxisUp() {
      dom.style('cursor', 'auto');

      if (_typeof(that.$xAxisDragData) === "object") {
        graph.widget().redrawSoon();
        that.$xAxisDragData = undefined;
        d3.event.preventDefault();
        d3.event.stopPropagation();
      }
    }

    widget.model().addExtraPoints();

    if (widget.isDataZoom() || widget.isTimeZoom()) {
      var existingDomain = scale.domain(),
          newDomain = XAxisLayer.broadenDomain(graph, seriesList, existingDomain);
      scale.domain(newDomain);
    }

    var tickCount = Math.floor(widget.dimensions().width / 80);
    var ticks = that.$getTicks(tickCount); // Regenerate x-ticks…

    gx = ticksGroup.selectAll('g.x').data(ticks).attr('transform', tx);
    gx.select('text').text(fx);
    gxe = gx.enter().insert('g', 'a').attr('class', 'x').attr('transform', tx);

    if (settings.hasShowGrid()) {
      gxe.append('line').attr('y1', 0).attr('class', XAxisLayer.className);
    }

    gxe.append('text').attr('class', 'axis x-axis').attr('dy', '1.5em').attr('text-anchor', 'middle').text(fx);
    dom.on('mousemove.xaxisdrag', xaxisDrag).on('touchmove.xaxisdrag', xaxisDrag).on('mouseup.xaxisdrag', xaxisUp).on('touchend.xaxisdrag', xaxisUp);
    var lines = ticksGroup.selectAll('g.x line');

    if (settings.hasShowGrid()) {
      if (!lines.node()) {
        //need to add lines back
        ticksGroup.selectAll('g.x').append('line').attr('y1', 0).attr('class', XAxisLayer.className).attr('y2', widget.dimensions().height);
      } else {
        lines.attr('y2', widget.dimensions().height);
      }
    } else {
      lines.remove();
    }

    ticksGroup.selectAll('g.x text').attr('y', widget.dimensions().height);
    chart.selectAll('rect.x-ticks').attr('x', -20).attr('y', widget.dimensions().height).attr('height', "1.75em").attr('width', widget.dimensions().width + 25).on('mousedown.xaxisdrag', xaxisDown).on('touchstart.xaxisdrag', xaxisDown);
    gx.exit().remove();
  };

  XAxisLayer.prototype.getScale = function () {
    return this.$scale;
  };

  XAxisLayer.prototype.destroy = function () {
    var dom = this.$graph.widget().jq3();

    if (dom) {
      dom.on('mousemove.xaxisdrag', null).on('touchmove.xaxisdrag', null).on('mouseup.xaxisdrag', null).on('touchend.xaxisdrag', null);
    }
  };

  return XAxisLayer;
});
