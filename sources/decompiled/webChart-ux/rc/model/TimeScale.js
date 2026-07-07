/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/TimeScale
 */
define(['d3', 'baja!', 'moment', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/localeUtil', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/model/BaseScale'], function (d3, baja, moment, webChartUtil, localeUtil, modelUtil, BaseScale) {
  "use strict";

  var dateTimeUtil = require('bajaScript/baja/obj/dateTimeUtil');

  var SECOND_MILLIS = 1000,
      MINUTE_MILLIS = 60 * SECOND_MILLIS;
  /**
   * TimeScale represents the time scale for a chart data.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/TimeScale
   * @extends module:nmodule/webChart/rc/model/BaseScale
   * @param {Object} model The model used with this Scale.
   * @param {BaseSeries} [series] The first Series in this scale (if present).
   */

  var TimeScale = function TimeScale(model) {
    BaseScale.apply(this, arguments);
    var that = this;
    that.$units = baja.Unit.DEFAULT;
    that.$dataProperty = "x";
    that.$scale = d3.time.scale(); //default d3 multi-scale except that months use abbreviations

    that.$scale.tickFormat = function () {
      return localeUtil.getLocaleTickFormat(that.getDisplayedTimezoneOffset.bind(that));
    };

    that.$seriesList = model.seriesList();
  };

  TimeScale.prototype = Object.create(BaseScale.prototype);
  TimeScale.prototype.constructor = TimeScale;

  TimeScale.prototype.requiresTicks = function () {
    return true; //time always requires ticks
  };
  /**
   * SeriesList Accessor
   *
   * @returns {Array.<BaseSeries>}
   */


  TimeScale.prototype.seriesList = function () {
    return this.$seriesList;
  };
  /**
   * Primary Series Accessor
   *
   * @returns {module:nmodule/webChart/rc/model/BaseSeries}
   */


  TimeScale.prototype.primarySeries = function () {
    return this.$seriesList[0];
  };
  /**
   * Scale accessor.
   * @returns {d3.Scale}
   */


  TimeScale.prototype.scale = function () {
    return this.$scale;
  };
  /**
   * Get Time Display and ensure day is printed
   * @param {Date} y
   * @returns {string}
   */


  TimeScale.prototype.getFullTimeDisplay = function (y) {
    return this.getTimeDisplay(y, true);
  };
  /**
   * Add day if needed
   * @param {String} day
   * @param {String} time
   * @param {boolean} ensureDayOn
   * @returns {*}
   */


  function ensureDay(day, time, ensureDayOn) {
    if (ensureDayOn) {
      return day + ", " + time;
    }

    return time;
  }
  /**
   * This returns the number of minutes the times on the graph are offset from
   * the utc timestamp.
   * @param {Date} targetDate the date to consider.
   * @returns {number}
   */


  TimeScale.prototype.getDisplayedTimezoneOffset = function (targetDate) {
    var timezone = this.getTimeZone();

    if (timezone) {
      return dateTimeUtil.getUtcOffsetInTimeZone(targetDate, timezone);
    } else {
      return new Date().getTimezoneOffset() * -1;
    }
  };
  /**
   * Gets the timezone to display which is the timezone of the primary series.
   * @returns {module:baja/obj/TimeZone|null}
   */


  TimeScale.prototype.getTimeZone = function () {
    var preferredTimeZone = this.primarySeries() && this.primarySeries().getPreferredTimeZone(),
        overrideTimeZone = this.$timezoneOverride && this.$timezoneOverride();

    if (overrideTimeZone) {
      return overrideTimeZone;
    } else if (preferredTimeZone) {
      return preferredTimeZone;
    } else if (this.$fallbackTimeZone) {
      return this.$fallbackTimeZone;
    } else {
      return this.getTimeZoneDatabase().getTimeZone(dateTimeUtil.getEnvTimeZoneId());
    }
  };
  /**
   * Gets the timezone database used for timezone lookup
   * @returns {baja.TimeZoneDatabase}
   */


  TimeScale.prototype.getTimeZoneDatabase = function () {
    return this.$timezoneDB;
  };
  /**
   * Sets the timezone database used for timezone lookup
   * @param {baja.TimeZoneDatabase} timezoneDB
   */


  TimeScale.prototype.setTimeZoneDatabase = function (timezoneDB) {
    this.$timezoneDB = timezoneDB;
  };
  /**
   * A callback that gets the timezone to be displayed instead of the preferred/fallback
   * timezones.
   *
   * @callback module:nmodule/webChart/rc/model/TimeScale~TimeZoneOverrideCallback
   *
   * @returns {module:baja/obj/TimeZone}
   */

  /**
   * Sets the timezone override callback to get the timezone override.
   * @param {module:nmodule/webChart/rc/model/TimeScale~TimeZoneOverrideCallback} timezoneOverride
   */


  TimeScale.prototype.setTimeZoneOverride = function (timezoneOverride) {
    this.$timezoneOverride = timezoneOverride;
  };
  /**
   * Sets the timezone to fall back to if no other timezone is set.
   * @param {module:baja/obj/TimeZone} timeZone
   */


  TimeScale.prototype.setFallbackTimeZone = function (timeZone) {
    this.$fallbackTimeZone = timeZone;
  };
  /**
   * In the context of the domain, provide an accurate time.
   * @param {Date} y
   * @param {boolean} [ensureDayOn]
   * @param {boolean} [estimateOk]
   * @returns {String}
   */


  TimeScale.prototype.getTimeDisplay = function (y, ensureDayOn, estimateOk) {
    var that = this,
        format = ensureDayOn ? localeUtil.getD3LongDateFormat() : localeUtil.getD3DateFormat(),
        //full month text (use more space) or just mm/dd/yyy
    xdomain = that.$scale.domain(),
        timezone = that.getTimeZone(),
        start = moment(xdomain[0]).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(xdomain[0], timezone)),
        end = moment(xdomain[1]).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(xdomain[1], timezone)),
        startDisplay = localeUtil.format(format, start, timezone),
        endDisplay = localeUtil.format(format, end, timezone),
        value = moment(y).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(y, timezone)),
        valueDisplay = localeUtil.format(format, value, timezone),
        model = that.$model,
        samplingOn = model.isSampling() || modelUtil.hasBar(model.seriesList()),
        samplingMillis = samplingOn ? model.samplingMillis() : "",
        startValue,
        endValue,
        startValueDisplay,
        endValueDisplay;

    if (startDisplay === "Invalid date" || endDisplay === "Invalid date") {
      return "";
    }

    if (samplingMillis) {
      startValue = moment(value).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(value, timezone));
      endValue = modelUtil.getEndTime(model, startValue);
      startValueDisplay = localeUtil.format(format, startValue, timezone);
      endValueDisplay = localeUtil.format(format, endValue, timezone);
    } //same day?


    if (startDisplay === endDisplay) {
      //is time required?
      if (samplingMillis) {
        startValueDisplay = localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), startValue, timezone);
        endValueDisplay = localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), endValue, timezone);
      } //are seconds required?


      if (moment(value).startOf('minute').isSame(value)) {
        if (startValueDisplay !== endValueDisplay) {
          return ensureDay(valueDisplay, webChartUtil.lex.get("dateTo", startValueDisplay, endValueDisplay), ensureDayOn);
        }

        return ensureDay(valueDisplay, localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), value, timezone), ensureDayOn);
      } else {
        if (startValueDisplay !== endValueDisplay) {
          //same minute rollup doesn't need end time
          if (modelUtil.safeMomentAdd(startValue, 60000).isSame(endValue)) {
            return ensureDay(valueDisplay, startValueDisplay, ensureDayOn);
          }

          return ensureDay(valueDisplay, webChartUtil.lex.get("dateTo", startValueDisplay, endValueDisplay), ensureDayOn);
        }

        return ensureDay(valueDisplay, localeUtil.format(localeUtil.getD3TimeFormat(), value, timezone), ensureDayOn);
      }
    } else if (moment(value).startOf('day').isSame(value)) {
      //everything is same day
      if (samplingMillis) {
        if (startValueDisplay !== endValueDisplay) {
          return webChartUtil.lex.get("dateTo", startValueDisplay, endValueDisplay);
        }
      }

      return valueDisplay;
    } else {
      if (estimateOk) {
        //not the same day. if estimate ok, then no seconds required
        return valueDisplay + " " + localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), value, timezone);
      }

      if (samplingMillis) {
        if (samplingMillis >= MINUTE_MILLIS) {
          startValueDisplay = localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), startValue, timezone);
          endValueDisplay = localeUtil.format(localeUtil.getD3HourMinutePeriodsFormat(), endValue, timezone);
        } else if (samplingMillis >= SECOND_MILLIS) {
          startValueDisplay = localeUtil.format(localeUtil.getD3TimeFormat(), startValue, timezone);
          endValueDisplay = localeUtil.format(localeUtil.getD3TimeFormat(), endValue, timezone);
        }

        if (startValueDisplay !== endValueDisplay) {
          return ensureDay(valueDisplay, valueDisplay + ' ' + webChartUtil.lex.get("dateTo", startValueDisplay, endValueDisplay), ensureDayOn);
        }
      }

      return valueDisplay + " " + localeUtil.format(localeUtil.getD3TimeFormat(), value, timezone);
    }
  };

  return TimeScale;
});
