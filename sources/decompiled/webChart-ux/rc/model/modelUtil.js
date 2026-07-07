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
define(['lex!baja', 'd3', 'Promise', 'baja!', 'moment', 'underscore', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/chunkUtil', 'nmodule/webChart/rc/chartEvents', 'nmodule/webEditors/rc/fe/baja/util/numberUtils', 'nmodule/webEditors/rc/fe/baja/util/rangeUtils', 'baja!' + 'history:TrendFlags,' + 'gx:Color'], function (lexs, d3, Promise, baja, moment, _, webChartUtil, chunkUtil, chartEvents, numberUtils, rangeUtils) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A set of utility functions for common things the webChart model.
   *
   * @exports nmodule/webChart/rc/model/modelUtil
   */

  var modelUtil = {},
      SECOND_MILLIS = 1000,
      MINUTE_MILLIS = SECOND_MILLIS * 60,
      HOUR_MILLIS = MINUTE_MILLIS * 60,
      DAY_MILLIS = HOUR_MILLIS * 24,
      MONTH_MILLIS = DAY_MILLIS * 30,
      YEAR_MILLIS = DAY_MILLIS * 364,
      bajaLex = lexs[0],
      FALLBACK_NICE = {
    key: "millisecond",
    increment: 1,
    millis: 1
  },
      NICE_LEVELS = [{
    key: "year",
    increment: 1,
    millis: YEAR_MILLIS
  }, {
    key: "month",
    increment: 1,
    millis: MONTH_MILLIS
  }, {
    key: "day",
    increment: 1,
    millis: DAY_MILLIS
  }, {
    key: "hour",
    increment: 1,
    millis: HOUR_MILLIS
  }, {
    key: "minutes",
    increment: 30,
    millis: MINUTE_MILLIS * 30
  }, {
    key: "minutes",
    increment: 15,
    millis: MINUTE_MILLIS * 15
  }, {
    key: "minutes",
    increment: 5,
    millis: MINUTE_MILLIS * 5
  }, {
    key: "minute",
    increment: 1,
    millis: MINUTE_MILLIS
  }, {
    key: "seconds",
    increment: 30,
    millis: SECOND_MILLIS * 30
  }, {
    key: "seconds",
    increment: 15,
    millis: SECOND_MILLIS * 15
  }, {
    key: "second",
    increment: 1,
    millis: SECOND_MILLIS
  }, {
    key: "milliseconds",
    increment: 500,
    millis: 500
  }, {
    key: "milliseconds",
    increment: 100,
    millis: 100
  }, FALLBACK_NICE];
  /**
   * Return a resolved promise.
   * @returns {Promise}
   */

  modelUtil.resolvedPromise = function () {
    return Promise.resolve();
  };
  /**
   * Return the converted number based on standard unit conversion and precision
   * @param {Number} y
   * @param {Object} params
   *
   * @returns {Promise.<String>}
   */


  modelUtil.resolveNumericDisplay = function (y, params) {
    var units = params.units;
    return numberUtils.convertUnitTo(y, units, baja.getUnitConversion()).then(function (converted) {
      return numberUtils.toDisplay(converted, _.omit(params, 'units'));
    });
  };
  /**
   * @param {Object} obj the object literal for this method
   * @param {String} obj.ord
   * @param {baja.AbsTime} [obj.start]
   * @param {baja.AbsTime} [obj.end]
   * @param {Boolean} [obj.delta]
   * @param {String} [obj.delimiter] Defaults to question mark, but can be optionally passed in
   * @param {webChart.WebChartTimeRange} [obj.timeRange] if a period is provided and not auto, it overrides the start and end
   */


  modelUtil.getFullOrd = function (obj) {
    var ord = obj.ord,
        ordSuffix = "",
        start = obj.start,
        end = obj.end,
        delta = obj.delta,
        timeRange = obj.timeRange,
        period,
        delimiter = obj.delimiter ? obj.delimiter : "?";

    if (!start && !end && timeRange && timeRange.getPeriod().getOrdinal() > 1) {
      period = timeRange.getPeriod();
      ordSuffix = delimiter + "period=" + period.getTag();
    } else {
      if (!start && !end && timeRange && timeRange.getPeriod().getOrdinal() === 1) {
        if (timeRange.get("startFixed")) {
          start = timeRange.getStartTime();
        }

        if (timeRange.get("endFixed")) {
          end = timeRange.getEndTime();
        }
      }

      if (start) {
        if (ordSuffix.length) {
          ordSuffix += ";";
        } else {
          ordSuffix += delimiter;
        }

        ordSuffix += "start=" + start.encodeToString();
      }

      if (end) {
        if (ordSuffix.length) {
          ordSuffix += ";";
        } else {
          ordSuffix += delimiter;
        }

        ordSuffix += "end=" + end.encodeToString();
      }
    }

    if (delta) {
      if (ordSuffix.length) {
        ordSuffix += ";";
      } else {
        ordSuffix += delimiter;
      }

      ordSuffix += "delta=true";
    }

    return ord + ordSuffix;
  };
  /**
   * getFullScheduleOrd
   * @param {Object} obj the object literal for this method
   * @param {String} obj.ord
   * @param {baja.AbsTime} [obj.start]
   * @param {baja.AbsTime} [obj.end]
   * @param {Boolean} [obj.delta]
   * @param {String} [obj.delimiter] Defaults to question mark, but can be optionally passed in
   * @param {webChart.WebChartTimeRange} [obj.timeRange] if a period is provided and not auto, it overrides the start and end
   */


  modelUtil.getFullScheduleOrd = function (obj) {
    obj.delimiter = "|view:?";
    return modelUtil.getFullOrd(obj);
  };
  /**
   * In order to make the line discontinuous, a clone of the first point is required, except that skip is turned on.
   * @param {Object} obj
   * @returns {Object} obj
   */


  modelUtil.getSkipPoint = function (obj) {
    var result = {
      x: obj.x,
      y: obj.y,
      skip: true
    };

    if (obj.status) {
      result.status = obj.status;
    }

    return result;
  };

  modelUtil.sameRanges = function (series1, series2) {
    var range1 = modelUtil.getEnumRange(series1),
        range2 = modelUtil.getEnumRange(series2);

    if (range1 && range2) {
      return range1.equals(range2);
    }

    return false;
  };
  /**
   *
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @returns {baja.EnumRange|null}
   */


  modelUtil.getEnumRange = function (series) {
    var facets = series.facets();

    if (facets) {
      return facets.get("range");
    }

    return null;
  };
  /**
   * Get the Enum range for a dataset, if its not available, use fallback of 2
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @returns {*}
   */


  modelUtil.getEnumRangeLength = function (series) {
    var result,
        range = modelUtil.getEnumRange(series);

    if (range) {
      result = range.getOrdinals().length;
    } else if (series.points().length > 0) {
      var domain = series.valueScale().scale().domain();
      result = domain[0] - domain[1];
    }

    if (!result || result < 2) {
      result = 2;
    }

    return result;
  };
  /**
   * prepareDate
   * @return {Date}
   * @param timestamp
   */


  modelUtil.prepareDate = function (timestamp) {
    return moment(timestamp).toDate();
  };
  /**
   * If this is an enum, just use the ordinal as the raw value
   * @param value
   * @returns {*}
   */


  modelUtil.getValue = function (value) {
    if (value.getOrdinal) {
      //enum check
      return value.getOrdinal();
    } else if (!isFinite(value)) {
      return null;
    }

    return value;
  };
  /**
   * Append a Point received from a the servlet and append a module:nmodule/webChart/rc/model/BaseSeries~Point to
   * points array passed in. If the passed in point has a timestamp that matches the point that is
   * currently last, the current last point will be replaced.
   * @param {Object} raw object literal for the data
   * @param {Object} raw.v value
   * @param {Object} raw.t timestamp
   * @param {Object} raw.r trend flags
   * @param {Object} raw.s status
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} points existing point data for the series
   * @param {module:nmodule/webChart/rc/model/BaseModel} [model] if the model is provided options from the model can be used.
   */


  modelUtil.prepareServletPoint = function (raw, points, model) {
    var trend = raw.r,
        status = raw.s,
        point = {
      x: modelUtil.prepareDate(raw.t.encodeToString()),
      y: raw.v,
      skip: false
    },
        skipInfo = modelUtil.getSkipInfo(point.y, status, trend, model);

    if (status) {
      point.status = status;
    }

    var lastPoint = points[points.length - 1];

    if (webChartUtil.traceOn) {
      if (lastPoint && point.x.getTime() < lastPoint.x.getTime()) {
        webChartUtil.trace("backwards:" + points.length + ":" + point.x + "<" + lastPoint.x);
      }
    }

    if (lastPoint && lastPoint.x.getTime() === point.x.getTime()) {
      // This ensures that points coming from a remote history are not adding to the graph more than
      // once due to polling. See NCCB-54925.
      points.pop();
    }

    if (skipInfo.startTrend && !skipInfo.skip) {
      var skipPoint = modelUtil.getSkipPoint(point);
      skipPoint.startTrend = true;
      points.push(skipPoint);
    }

    if (!skipInfo.skip) {
      points.push(point);
    } else {
      //go to last point and make a skip copy if there isn't one already
      if (points.length) {
        if (!lastPoint.skip) {
          points.push(modelUtil.getSkipPoint(lastPoint));
        }
      }
    }
  };
  /**
   * Returns startTrend=true when there the trend flags include a start flag
   * Returns skip=true when the record is hidden, has a null status, or has a non-finite value.
   *
   * @see {@link nmodule/webChart/rc/ChartSettings.getShowStartTrendGaps} for details regarding the expected behavior of 'getShowStartTrendGaps' setting.
   * @see {@link nmodule/webChart/rc/ChartSettings.getShowDataGaps} for details regarding the expected behavior of 'getShowDataGaps' setting.
   * @param {baja.value} y
   * @param {Number} [status]
   * @param {Number} [trend]
   * @param {module:nmodule/webChart/rc/model/BaseModel} [model] if the model is provided options from the model can be used.
   * @returns {{startTrend: boolean, skip: boolean}}
   */


  modelUtil.getSkipInfo = function (y, status, trend, model) {
    var results = {
      startTrend: false,
      skip: false
    };

    if (trend) {
      results.startTrend = (trend & 0x01) !== 0;
      results.skip = (trend & 0x04) !== 0; //hidden trend flag
    }

    if (status) {
      if ((status & 0x40) !== 0) {
        results.skip = true; //null status should not be shown
      }
    }

    if (!isFinite(y) || y === null) {
      results.skip = true; //ignore nan, inf+, inf-, null values
    }

    return results;
  };
  /**
   * Append a Point received from a Live Subscription and append a module:nmodule/webChart/rc/model/BaseSeries~Point to
   * points array passed in.
   * @param {baja.Component} raw `history:TrendRecord`
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} points existing point data for the series
   * @param {module:nmodule/webChart/rc/model/BaseModel} [model] if the model is provided options from the model can be used.
   */


  modelUtil.prepareLivePoint = function (raw, points, model) {
    var point = {
      x: modelUtil.prepareDate(raw.get("timestamp").encodeToString()),
      y: modelUtil.getValue(raw.get("value")),
      skip: false
    },
        status = raw.get("status"),
        trend = raw.get("trendFlags"),
        skipInfo;

    if (status) {
      status = status.getBits(); //store at bits to make rollup more efficient
    }

    skipInfo = modelUtil.getSkipInfo(point.y, status, trend ? trend.getBits() : 0, model);

    if (status) {
      point.status = status;
    } // Ensure the new data point is after the last recorded timestamp.


    if (!points.length || +point.x > +points[points.length - 1].x) {
      if (skipInfo.startTrend && !skipInfo.skip) {
        points.push(modelUtil.getSkipPoint(point));
      }

      if (!skipInfo.skip) {
        points.push(point);
      } else if (model && model.settings().getShowDataGaps() === "yes") {
        //go to last point and make that a that a skip to ensure a break in data
        if (points.length) {
          points.push(modelUtil.getSkipPoint(points[points.length - 1]));
        }
      }
    }
  };
  /**
   * baja.Format a pattern and resolve the results
   * @param {String} pattern
   * @returns {Promise}
   */


  modelUtil.doFormat = function (pattern) {
    return baja.Format.format({
      pattern: pattern
    });
  };
  /**
   * Compute the true/false display text to use from the given facets.
   * @param {baja.Facets} facets
   * @returns {Promise} promise to be resolves to trueText/falseText
   */


  modelUtil.getTrueFalseText = function (facets) {
    var trueText = facets.get('trueText') || bajaLex.get('true'),
        falseText = facets.get('falseText') || bajaLex.get('false');
    return Promise.all([modelUtil.doFormat(trueText), modelUtil.doFormat(falseText)]).then(function (_ref) {
      var _ref2 = _slicedToArray(_ref, 2),
          trueText = _ref2[0],
          falseText = _ref2[1];

      return {
        trueText: trueText,
        falseText: falseText
      };
    });
  };
  /**
   * Based on the series, ordinal and facets, resolve the display name of the enum ordinal. If the value is not an
   * integer, then round to the closest one.
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @param {Number} ordinal
   * @param {baja.Facets} facets
   * @returns {Promise.<String>} resolves to display text
   */


  modelUtil.resolveEnumDisplay = function (series, ordinal, facets) {
    ordinal = Math.round(parseFloat(ordinal));

    if (series.isBoolean()) {
      return modelUtil.getTrueFalseText(facets).then(function (result) {
        return ordinal ? result.trueText : result.falseText;
      });
    } else {
      return rangeUtils.getEnumRangeDisplay(ordinal, facets.get("range"));
    }
  };
  /**
   * Get the series used for areas
   * @returns {Array.<BaseSeries>}
   * @param seriesList
   */


  modelUtil.getAreas = function (seriesList) {
    var i,
        areas = [];

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isShade()) {
        areas.push(seriesList[i]);
      }
    }

    return areas;
  };
  /**
   * Get the series and make sure area is first for better dom layout
   * @returns {Array.<BaseSeries>}
   * @param seriesList
   */


  modelUtil.getLines = function (seriesList) {
    var i,
        lines = [];

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isLine()) {
        lines.push(seriesList[i]);
      }
    }

    return lines;
  };
  /**
   * Return true if the seriesList contains any enabled line series
   * @param {Array.<BaseSeries>} seriesList
   * @returns {boolean}
   */


  modelUtil.hasLine = function (seriesList) {
    for (var i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isLine() && seriesList[i].isEnabled()) {
        return true;
      }
    }

    return false;
  };
  /**
   * Get the series and make sure area is first for better dom layout
   * @param {Array.<BaseSeries>} seriesList
   * @returns {Array.<BaseSeries>}
   */


  modelUtil.getBars = function (seriesList) {
    var i,
        lines = [];

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isBar()) {
        lines.push(seriesList[i]);
      }
    }

    return lines;
  };
  /**
   * Return the amount of time required for the number of pixels specified.
   * If no time can be found, this falls back to the number of pixels provided.
   * If the number is greater than zero, but smaller than 1, it will be rounded up to 1.
   *
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {Number} [pixels=1]
   * @returns {Number}
   */


  modelUtil.getTimeForPixels = function (model, pixels) {
    if (!pixels) {
      pixels = 1;
    }

    var xScale = model.timeScale().scale();
    var domainWidth = xScale.domain()[1] - xScale.domain()[0],
        rangeWidth = xScale.range()[1] - xScale.range()[0];

    if (!rangeWidth) {
      return pixels;
    }

    return Math.max(1, domainWidth / rangeWidth * pixels || pixels);
  };
  /**
   * Return true if the seriesList contains any enabled shade series
   * @param {Array.<BaseSeries>} seriesList
   * @param {module:baja/obj/TimeZone} timezone
   * @param {webChart:WebChartTimeRange} [timeRange]
   * @returns {boolean}
   */


  modelUtil.hasVisibleShade = function (seriesList, timezone, timeRange) {
    if (!seriesList) {
      return false;
    }

    var startAndEnd = webChartUtil.getStartAndEndDateFromTimeRange(timeRange, timezone);

    for (var i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isShade() && seriesList[i].isEnabled() && seriesList[i].samplingPoints().length > 0) {
        if (!timeRange) {
          return true;
        }

        var points = seriesList[i].samplingPoints();

        for (var pointIndex = 0; pointIndex < points.length; pointIndex++) {
          var targetDate = moment(points[pointIndex].x);

          if (webChartUtil.isInTimeRange(startAndEnd.start, startAndEnd.end, targetDate)) {
            return true;
          }
        }
      }
    }

    return false;
  };
  /**
   * Return true if the seriesList contains any enabled bar series
   * @param {Array.<BaseSeries>} seriesList
   * @returns {boolean}
   */


  modelUtil.hasBar = function (seriesList) {
    if (!seriesList) {
      return false;
    }

    for (var i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isBar() && seriesList[i].isEnabled()) {
        return true;
      }
    }

    return false;
  };
  /**
   * Return true if seriesList has a discrete Series in it.
   * @param {Array.<BaseSeries>} seriesList
   * @returns {boolean}
   */


  modelUtil.hasDiscrete = function (seriesList) {
    if (!seriesList) {
      return false;
    }

    for (var i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isDiscrete()) {
        return true;
      }
    }

    return false;
  };
  /**
   * Given the millis, obtain the best space available for each bar grouping. If the millis is zero,
   * default to 10 so something is visible for the user to see.
   * @param {BaseModel} model
   * @param {Number} millis
   * @return {Number} barWidth
   */


  modelUtil.getBarWidth = function (model, millis) {
    if (!millis) {
      return 10;
    }

    var bars = modelUtil.getBars(model.seriesList()).length; //get the minimal bar width

    if (bars > 0) {
      var scale = model.timeScale().scale(),
          range = scale.range(),
          domain = scale.domain(),
          width = range[1] - range[0],
          domainWidth,
          eachBar;

      if (width) {
        domainWidth = domain[1] - domain[0];
        eachBar = width / domainWidth * millis;
        return eachBar;
      }
    }

    return 10;
  };
  /**
   * Add some milliseconds to a JSDate and ensure moment doesn't go into an infinite loop
   * @param {Date|moment} time starting Date/Time
   * @param {Number} duration milliseconds, this can be negative to subtract, but it can't be zero
   * @returns {moment}
   */


  modelUtil.safeMomentAdd = function (time, duration) {
    var oldTime = moment(time),
        result;

    if (!isFinite(duration) || !duration || typeof duration !== 'number') {
      //note this could be an array
      throw new Error("Cannot add duration of " + duration + " to " + time);
    } else if (duration >= DAY_MILLIS || duration <= DAY_MILLIS * -1) {
      //this ensures that if daylight savings occurs that we keep the same time of day
      result = moment(time).add(duration / DAY_MILLIS | 0, 'days');
    } else {
      result = moment(time).add(duration, 'milliseconds');
    }

    if (result.isSame(oldTime)) {
      throw new Error("moment bug:" + result.toDate() + ":" + duration);
    }

    return result;
  };
  /**
   * Based on the currentTime and duration, determine the corresponding startTime to use.
   * @param {LineModel} model
   * @param {moment} currentTime
   * @param {Number} [duration] If not provided (or zero), uses 'model.samplingMillis()'.
   * @returns {moment}
   */


  modelUtil.getStartTime = function (model, currentTime, duration) {
    var samplingMillis = duration || model.samplingMillis(),
        targetStartTime,
        timezone = model.timeScale().getTimeZone();

    if (samplingMillis === YEAR_MILLIS) {
      targetStartTime = webChartUtil.startOf('year', timezone, moment(currentTime));
    } else if (samplingMillis === MONTH_MILLIS) {
      targetStartTime = webChartUtil.startOf('month', timezone, moment(currentTime));
    } else if (samplingMillis === DAY_MILLIS) {
      targetStartTime = webChartUtil.startOf('day', timezone, moment(currentTime));
    } else if (DAY_MILLIS % samplingMillis === 0 || HOUR_MILLIS % samplingMillis === 0 || MINUTE_MILLIS % samplingMillis === 0) {
      targetStartTime = moment(currentTime - currentTime % samplingMillis);
    } else {
      targetStartTime = moment(currentTime);
    }

    return targetStartTime;
  };
  /**
   * Based on the currentTime and duration, determine the corresponding endTime to use.
   * @param {BaseModel} model
   * @param {moment} currentTime
   * @param {Number} [duration] If not provided (or zero), uses 'model.samplingMillis()'.
   * @returns {moment}
   */


  modelUtil.getEndTime = function (model, currentTime, duration) {
    var samplingMillis = duration || model.samplingMillis(),
        targetEndMoment,
        timezone = model.timeScale().getTimeZone();

    if (samplingMillis === YEAR_MILLIS) {
      targetEndMoment = webChartUtil.endOf('year', timezone, moment(currentTime));
    } else if (samplingMillis === MONTH_MILLIS) {
      targetEndMoment = webChartUtil.endOf('month', timezone, moment(currentTime));
    } else if (samplingMillis === DAY_MILLIS) {
      targetEndMoment = webChartUtil.endOf('day', timezone, moment(currentTime));
    } else {
      //for other durations, add sampling period
      targetEndMoment = moment(modelUtil.safeMomentAdd(currentTime, samplingMillis));
      return targetEndMoment;
    }

    return targetEndMoment;
  };
  /**
   * Based on the model and given sampling millis, determine whether the sampling period is variable.
   * For example, a sampling period specified as '30 days' will actually equate to 1 month which can be shorter or longer depending
   * on which month it is. Years can sometimes be 1 day longer than 364, and days can be shorter or longer than 24 hours
   * (daylight savings transitions).
   * @param {BaseModel} model
   * @param {Number} [duration] If not provided (or zero), uses 'model.samplingMillis()'.
   * @returns {boolean}
   */


  modelUtil.hasVariableSamplingPeriod = function (model, duration) {
    var samplingMillis = duration || model.samplingMillis();

    if (samplingMillis === YEAR_MILLIS) {
      return true;
    } else if (samplingMillis === MONTH_MILLIS) {
      return true;
    } else if (samplingMillis === DAY_MILLIS) {
      return true;
    }

    return false;
  };
  /**
   * Get the sampling period display.
   * @param {BaseModel} model
   * @param {Number} [duration] If not provided (or zero), uses 'model.samplingMillis()'.
   * @returns {string}
   */


  modelUtil.getSamplingPeriodDisplay = function (model, duration) {
    var samplingMillis = duration || model.samplingMillis();

    if (samplingMillis === YEAR_MILLIS) {
      //year
      return "1 " + webChartUtil.lex.get("year");
    } else if (samplingMillis === MONTH_MILLIS) {
      //month
      return "1 " + webChartUtil.lex.get("month");
    } else if (samplingMillis === DAY_MILLIS) {
      //day
      return "1 " + webChartUtil.lex.get("day");
    }

    return baja.RelTime.make(samplingMillis).toString(); //Note: Due to NCCB-4761, this is NOT localized
  };
  /**
   * Given a calculated duration, provide a "nice" auto-generated sampling Period that just the next common interval that is longer
   * than the needed duration that will ensure that the number of points on the page is reasonable when we are over the limit.
   & For example, if 56 minuts is millis passed in, then round up to 1 hours as the rollup time.
   * These are the levels used Year, Month, Day, Hour, 30 min, 15 min, 1 min, 30 seconds, 15 seconds, 1 second, 500 ms,
   * 100ms, and 1 ms.
   *
   * @param {Number} millis
   * @param {Boolean} [overLimit] If true, increase the nice time increment to lower the amount of points on the page.
   * If we are not over the limit, then keep the calculated increment
   * @returns {Object} Returns the key minutes/days/etc, the increment how many days minutes/days/etc, and millis,
   *                   the duration in milliseconds for that increment.
   */


  modelUtil.getNiceTimeIncrement = function (millis, overLimit) {
    var years, i;

    for (i = 0; i < NICE_LEVELS.length; i++) {
      if (millis > NICE_LEVELS[i].millis) {
        if (i === 0 || overLimit && i === 1) {
          years = Math.ceil(millis / YEAR_MILLIS);

          if (overLimit) {
            years++;
          }

          return {
            key: "year",
            increment: years,
            millis: years * YEAR_MILLIS
          };
        }

        if (overLimit && i === 1) {
          years = Math.ceil(millis / YEAR_MILLIS);
          return {
            key: "year",
            increment: years,
            millis: years * YEAR_MILLIS
          };
        }

        if (overLimit) {
          return NICE_LEVELS[i - 2];
        }

        return NICE_LEVELS[i - 1];
      }
    }

    return FALLBACK_NICE;
  };
  /**
   * Obtain the Bar Layout Object which returns an object containing functions that provide the
   * sizes for the bars.
   * @param {BaseModel} model
   * @returns {Object} an object with numeric function properties for <code>groupWidth<code>, <code>barWidth<code>,  and <code>barOffSet</code>.
   */


  modelUtil.getBarLayout = function (model) {
    var spacing = 4,
        samplingMillis = model.samplingMillis(),
        variableSamplingPeriod = modelUtil.hasVariableSamplingPeriod(model),
        _groupWidth,
        barCount,
        _barWidth;

    if (variableSamplingPeriod) {
      if (samplingMillis === MONTH_MILLIS) {
        //This is required to help with the different between month sizes
        spacing = 14;
      }

      var scale = model.timeScale().scale();
      _groupWidth = modelUtil.getBarWidth(model, samplingMillis);
      barCount = modelUtil.getBars(model.seriesList()).length;
      _barWidth = (_groupWidth - spacing) / barCount;

      var groupWidthFunction = function groupWidthFunction(x) {
        return scale(modelUtil.getEndTime(model, x)) - scale(x);
      };

      var barWidthFunction = function barWidthFunction(x) {
        return (groupWidthFunction(x) - spacing) / barCount;
      };

      return {
        groupWidth: groupWidthFunction,
        barWidth: barWidthFunction,
        barOffset: function barOffset(x) {
          return (groupWidthFunction(x) - barWidthFunction(x) - spacing) / 2;
        }
      };
    }

    _groupWidth = modelUtil.getBarWidth(model, samplingMillis);
    barCount = modelUtil.getBars(model.seriesList()).length;
    _barWidth = (_groupWidth - spacing) / barCount;
    return {
      groupWidth: function groupWidth() {
        return _groupWidth;
      },
      barWidth: function barWidth() {
        return _barWidth;
      },
      barOffset: function barOffset() {
        return (_groupWidth - _barWidth - spacing) / 2;
      }
    };
  };
  /**
   * Get the X position of a bar based on different variables.
   *
   * @param {Number} x
   * @param {Number} barIndex
   * @param {Object} barLayout
   * @param {d3.Scale} scale
   * @returns {Number}
   */


  modelUtil.getBarX = function (x, barIndex, barLayout, scale) {
    var barWidth = barLayout.barWidth(x);
    return scale(x) + barWidth * barIndex;
  };
  /**
   * Determine if a given index in the points array should be considered a gap.
   *
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} points
   * @param {Number} index the index of the points array to consider
   *
   * @returns {boolean} true if the point at the given index is a skip point, or
   * if any of the points next to this point are skip points with the same time
   * as the current point.
   */


  modelUtil.isBarChartDataGap = function (points, index) {
    var point = points[index];

    if (!point || !points.length) {
      return false;
    }

    if (point && point.skip) {
      return true;
    }

    var pointTime = point.x.getTime(),
        lastPoint = index > 0 && points[index - 1],
        nextPoint = index < points.length && points[index + 1],
        isNextMatchingSkip = !!(nextPoint && nextPoint.skip && nextPoint.x.getTime() === pointTime),
        isLastMatchingSkip = !!(lastPoint && lastPoint.skip && lastPoint.x.getTime() === pointTime),
        isNextToSkipWithSameX = isNextMatchingSkip || isLastMatchingSkip;
    return isNextToSkipWithSameX;
  };
  /**
   * This determines whether a given point could overlap with any bars on the
   * graph.
   *
   * @param {Array.<module:nmodule/webChart/rc/model/BaseModel>} model
   * @param {module:nmodule/webChart/rc/model/BaseSeries~Point} point
   * @param {Object} barLayout
   * @param {function} scaleX
   * @returns {boolean} true if the point is before the last bar group
   */


  modelUtil.isPointBeforeLastBarGroup = function (model, point, barLayout, scaleX) {
    if (!point) {
      return false;
    }

    var seriesList = model.seriesList(),
        isBefore = false,
        pointPosition = scaleX(point.x);
    seriesList.forEach(function (series) {
      if (isBefore) {
        return;
      }

      if (!series.isBar()) {
        return;
      }

      var lastPoint = findLastNormalPoint(series.samplingPoints());

      if (!lastPoint) {
        return;
      }

      var previousGroupRightPosition = scaleX(lastPoint.x) + barLayout.groupWidth(lastPoint.x);

      if (previousGroupRightPosition >= pointPosition) {
        isBefore = true;
      }
    });
    return isBefore;
  };
  /**
   * Sort the SeriesList based on given criteria.
   *
   * @param {BaseModel} model
   * @param {Array.<BaseSeries>} seriesList The list to Sort
   * @param {ValueScale} [newPrimaryScale] If present, give preference to this valueScale
   * @param {BaseSeries} [newPrimarySeries] If present, give preference to this series
   */


  modelUtil.sortSeriesList = function (model, seriesList, newPrimaryScale, newPrimarySeries) {
    var i,
        previousIndexes = [],
        maxSeriesListLength = model.maxSeriesListLength(); //since sort changes indexes, indexes must be cached for sort

    for (i = 0; i < seriesList.length; i++) {
      previousIndexes[seriesList[i]] = i;
    } //move the primary series to the front


    seriesList.sort(function (a, b) {
      var aIndex = previousIndexes[a],
          bIndex = previousIndexes[b];

      if (a.valueScale() === newPrimaryScale) {
        aIndex -= maxSeriesListLength;
      }

      if (b.valueScale() === newPrimaryScale) {
        bIndex -= maxSeriesListLength;
      }

      if (a === newPrimarySeries) {
        aIndex -= maxSeriesListLength;
      }

      if (b === newPrimarySeries) {
        bIndex -= maxSeriesListLength;
      }

      return aIndex - bIndex;
    });
  };
  /**
   * Generate a unique scale value name.
   *
   * @param {BaseModel} model
   * @param {ValueScale} valueScale
   * @return {string}
   */


  modelUtil.generateUniqueValueScaleName = function (model, valueScale) {
    var valueScaleList = model.valueScales(),
        test = "#",
        counter = "",
        i;

    while (true) {
      for (i = 0; i < valueScaleList.length; i++) {
        if (valueScaleList[i] === valueScale) {
          continue;
        }

        if (test === valueScaleList[i].uniqueName(true)) {
          if (!counter) {
            counter = 1;
          }

          counter++;
          test = "#" + counter;
          continue;
        }
      }

      return test;
    }
  };
  /**
   * To optimize space on legend and other areas, get the most smallest unique name to the current set of points
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   */


  modelUtil.generateUniqueShortestDisplayNames = function (model) {
    var that = this,
        seriesList = model.seriesList(),
        tree = [],
        displayPaths = [],
        i,
        split;

    for (i = 0; i < seriesList.length; i++) {
      displayPaths[i] = seriesList[i].displayPath();
      split = displayPaths[i].split('/');
      that.populateConflictTree(tree, split, split.length - 1);
    }

    for (i = 0; i < seriesList.length; i++) {
      split = displayPaths[i].split('/');
      var result = that.getShortestUniqueName(tree, split, split.length - 1, "");
      seriesList[i].shortDisplayName(result);
    }
  };
  /**
   * getShortestUniqueName
   * @param {Array.<Array>} tree
   * @param {Array.<String>} split
   * @param {Number} index
   * @param {String} path
   * @return {String}
   */


  modelUtil.getShortestUniqueName = function (tree, split, index, path) {
    var value = split[index],
        newTree = tree[value];

    if (value) {
      if (path.length > 0) {
        path = value + "/" + path;
      } else {
        path = value;
      }
    }

    if (!tree[value]) {
      return path;
    }

    if (!tree[value].conflict) {
      return path;
    }

    index--;
    return this.getShortestUniqueName(newTree, split, index, path);
  };
  /**
   * Populate short name conflict tree
   * @param {Array.<Object>} tree
   * @param {Array.<String>} split
   * @param {Number} index
   */


  modelUtil.populateConflictTree = function (tree, split, index) {
    var value = split[index],
        existingIndex = tree.indexOf(value),
        newTree;

    if (existingIndex > -1) {
      newTree = tree[value];
      tree[value].conflict = true;
    } else {
      tree.push(value);
      newTree = [];
      tree[value] = newTree;
    }

    if (index >= 0) {
      index--;
      this.populateConflictTree(newTree, split, index);
    }
  };

  modelUtil.chunkData = function (model, series, uri) {
    var params = {},
        jq = model.jq();

    params.checkAbort = function () {
      if (model.$stopped) {
        return true;
      }

      return false;
    };

    params.progress = function (chunks, progressIndex) {
      for (var i = 0; i < chunks.length; i++) {
        var chunk = JSON.parse(chunks[i]);

        if (chunk.hasOwnProperty('w')) {
          series.addWarning(chunk.w);
        } else {
          series.preparePoint(chunk);
        }
      }

      series.trimToCapacity();
      jq.trigger(chartEvents.REDRAW_REQUEST_EVENT, 1000);
    };

    return chunkUtil.ajax(uri, params).then(function (chunks) {
      for (var i = 0; i < chunks.length; i++) {
        series.preparePoint(JSON.parse(chunks[i]));
      }

      series.trimToCapacity();
    });
  };
  /**
   * If points are present, stretch 2% for better visibility in barChart and status when domain entry does not equal zero.
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {Array.<Number>} domain
   * @param {boolean } [reverse]
   * @returns {Array.<Number>}
   */


  modelUtil.stretchDomain = function (model, domain, reverse) {
    var stretchDomain = [domain[0], domain[1]]; //copy
    //stretch 2% higher and lower for barChart and status coloring circles when axis does not equal zero

    var diff = domain[1] - domain[0],
        stretch = diff / 50; //2%

    if (reverse) {
      stretch = (stretch - stretch / 26) * -1;
    }

    if (stretchDomain[0]) {
      stretchDomain[0] -= stretch;
    }

    if (stretchDomain[1]) {
      stretchDomain[1] += stretch;
    }

    return stretchDomain;
  };
  /**
   * Finds the last point in an array of points that is not a skip or
   * interpolated point if there is one.
   *
   * @inner
   * @param @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} points
   * @returns {undefined|module:nmodule/webChart/rc/model/BaseSeries~Point}
   */


  function findLastNormalPoint(points) {
    if (!points || !points.length) {
      return;
    }

    for (var i = points.length - 1; i >= 0; i--) {
      var point = points[i];

      if (!point.interpolated && !point.skip) {
        return point;
      }
    }
  }

  return modelUtil;
});
