/**
 * @copyright 2017 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */
define(['d3', 'baja!', 'underscore', 'moment', 'lex!baja,webChart'], function (d3, baja, _, moment, lexs) {
  "use strict";

  var dateTimeUtil = require('bajaScript/baja/obj/dateTimeUtil');
  /**
   * API Status: **Private**
   *
   * A set of utility functions for the webChart module dealing with locale support.
   *
   * @exports nmodule/webChart/rc/localeUtil
   * @see https://github.com/d3/d3-3.x-api-reference/blob/master/Time-Formatting.md#format
   * @see module://docDeveloper/doc/localization.html
   */


  var localeUtil = {};
  var bajaLex = lexs[0];
  localeUtil.bajaLex = bajaLex;
  localeUtil.lex = lexs[1];
  /**
   * Sets up and returns the d3Locale based on the user's language and TimeFomat
   * @returns {Object} d3Locale Object
   */

  localeUtil.d3Locale = function () {
    if (localeUtil.$d3Locale) {
      return localeUtil.$d3Locale;
    } //sets up d3Locale


    var localeParams = {
      "dateTime": localeUtil.getD3DateTimeFormat(),
      "date": localeUtil.getD3DateFormat(),
      "time": localeUtil.getD3TimeFormat(),
      "periods": ["am", "pm"],
      "days": [bajaLex.get("sunday"), bajaLex.get("monday"), bajaLex.get("tuesday"), bajaLex.get("wednesday"), bajaLex.get("thursday"), bajaLex.get("friday"), bajaLex.get("saturday")],
      "shortDays": [bajaLex.get("sunday.short"), bajaLex.get("monday.short"), bajaLex.get("tuesday.short"), bajaLex.get("wednesday.short"), bajaLex.get("thursday.short"), bajaLex.get("friday.short"), bajaLex.get("saturday.short")],
      "months": [bajaLex.get("january"), bajaLex.get("february"), bajaLex.get("march"), bajaLex.get("april"), bajaLex.get("may"), bajaLex.get("june"), bajaLex.get("july"), bajaLex.get("august"), bajaLex.get("september"), bajaLex.get("october"), bajaLex.get("november"), bajaLex.get("december")],
      "shortMonths": [bajaLex.get("january.short"), bajaLex.get("february.short"), bajaLex.get("march.short"), bajaLex.get("april.short"), bajaLex.get("may.short"), bajaLex.get("june.short"), bajaLex.get("july.short"), bajaLex.get("august.short"), bajaLex.get("september.short"), bajaLex.get("october.short"), bajaLex.get("november.short"), bajaLex.get("december.short")]
    };
    localeUtil.$d3Locale = d3.locale(localeParams);
    return localeUtil.$d3Locale;
  };
  /**
   * Get the D3 Date Time Format based on the current baja TimeFormat
   * @returns {string}
   */


  localeUtil.getD3DateTimeFormat = function () {
    var timeDateFormat = baja.getTimeFormatPattern();
    timeDateFormat = timeDateFormat.trim();
    timeDateFormat = localeUtil.getD3DateFormat(timeDateFormat);
    timeDateFormat = localeUtil.getD3TimeFormat(timeDateFormat);
    return timeDateFormat;
  };
  /**
   * Get the D3 date format based on the current Baja TimeFormat
   * @param {string} [dateFormat]
   * @return {string}
   */


  localeUtil.getD3DateFormat = function (dateFormat) {
    if (!dateFormat) {
      dateFormat = localeUtil.getDateFormat();
    } //year conversion


    dateFormat = dateFormat.replace(/YYYY/g, '%Y'); //include century

    dateFormat = dateFormat.replace(/YY/g, '%y'); //do not include century
    //month conversion

    dateFormat = dateFormat.replace(/MMM/g, '%b'); //Abbreviated month name

    dateFormat = dateFormat.replace(/MM/g, '%0m'); //Two digit month

    dateFormat = dateFormat.replace(/M/g, '%-m'); //Two digit month
    //day conversion

    dateFormat = dateFormat.replace(/DD/g, '%0d'); //two digit day of month

    dateFormat = dateFormat.replace(/D/g, '%-e'); //single day of month

    dateFormat = dateFormat.replace(/z/g, '%Z'); //timezone
    //TODO: NCCB-26507 timezone not supported by bajascript

    return dateFormat;
  };
  /**
   * Get the Date Format from the Baja TimeFormat
   * @returns {string}
   */


  localeUtil.getDateFormat = function () {
    var timeFormat = baja.getTimeFormatPattern();
    var dateFormat = timeFormat.replace(/[:Hhmsaz]/g, ''); //remove time format characters

    return dateFormat.trim();
  };
  /**
   *  Get the Time Format from the Baja TimeFormat
   * @param {string} timeFormat
   * @returns {string}
   */


  localeUtil.getD3TimeFormat = function (timeFormat) {
    if (!timeFormat) {
      timeFormat = localeUtil.getTimeFormat();
    }

    timeFormat = timeFormat.replace(/z/g, '%Z'); // timezone
    //am/pm

    timeFormat = timeFormat.replace(/a/g, '%p'); //Convert from baja a to %p
    //hour conversion

    timeFormat = timeFormat.replace(/HH/g, '%0H'); //two digit 12 hour

    timeFormat = timeFormat.replace(/[^0]H/g, '%H'); //one digit converts to digit 12 hour (d3 doesn't support single)

    timeFormat = timeFormat.replace(/hh/g, '%0I'); //two digit 24 hour

    timeFormat = timeFormat.replace(/h/g, '%-I'); //one digit converts to digit 24 hour (d3 doesn't support single)
    //minute

    timeFormat = timeFormat.replace(/mm/g, '%M'); //simple minute conversion
    //second

    timeFormat = timeFormat.replace(/ss/g, '%S'); //simple second conversion

    return timeFormat;
  };
  /**
   * Get the Time from the baja TimeFormat
   * @returns {string}
   */


  localeUtil.getTimeFormat = function () {
    var timeFormat = baja.getTimeFormatPattern();
    var dateFormat = timeFormat.replace(/[-/DMTY]/g, ''); ////remove date format characters

    return dateFormat.replace(/ +(?= )/g, '').trim();
  };
  /**
   * Get the long Date Format that includes the full month name, for example:
   * April 11 2017
   * @param {string} [timeFormat]
   * @returns {string}
   */


  localeUtil.getD3LongDateFormat = function (timeFormat) {
    timeFormat = localeUtil.getD3DateFormat(timeFormat);
    timeFormat = timeFormat.replace(/%b/g, '%B'); //use full month name

    timeFormat = timeFormat.replace(/%y/g, '%Y'); //use full year 2017 instead of 17

    return timeFormat;
  };
  /**
   * Get the long Date Time Format that includes the full month name, full century and hours, minutes, seconds.
   * April 11 2017, 12:20:01 AM
   * @param {string} [timeFormat]
   * @returns {string}
   */


  localeUtil.getD3LongDateTimeFormat = function (timeFormat) {
    timeFormat = localeUtil.getD3DateTimeFormat(timeFormat);
    timeFormat = timeFormat.replace(/%b/g, '%B'); //use full month name

    timeFormat = timeFormat.replace(/%y/g, '%Y'); //use full year 2017 instead of 17

    return timeFormat;
  };
  /**
   * Get the d3 hour format from the baja TimeFormat
   * @param {string} timeFormat
   * @returns {string}
   */


  localeUtil.getD3HourFormat = function (timeFormat) {
    timeFormat = localeUtil.getD3TimeFormat(timeFormat);

    if (timeFormat.indexOf("I") >= 0) {
      return "%-I %p";
    } else {
      return "%0H:%M";
    }
  };
  /**
   * Has period (am or pm) otherwise its 24 hour time
   * @param timeFormat
   */


  localeUtil.hasPeriod = function (timeFormat) {
    timeFormat = localeUtil.getD3TimeFormat(timeFormat);
    return timeFormat.indexOf("%p") >= 0;
  };
  /**
   * Get the d3 hour and minute format.
   * @param {string} timeFormat
   * @returns {string}
   */


  localeUtil.getD3HourMinuteFormat = function (timeFormat) {
    timeFormat = localeUtil.getD3TimeFormat(timeFormat);

    if (timeFormat.indexOf("I") >= 0) {
      return "%-I:%M";
    } else {
      return "%0H:%M";
    }
  };
  /**
   * Get the d3 hour, minute, and period format.
   * @param {string} [timeFormat]
   * @returns {string}
   */


  localeUtil.getD3HourMinutePeriodsFormat = function (timeFormat) {
    timeFormat = localeUtil.getD3TimeFormat(timeFormat);

    if (timeFormat.indexOf("I") >= 0) {
      return "%-I:%M%p";
    } else {
      return "%0H:%M";
    }
  };
  /**
   * A callback that gets the offset to use for the specified tick. This value
   * will be analogous to those generated by Date#getTimezoneOffset
   *
   * @callback module:nmodule/webChart/rc/localeUtil~OffsetCallback
   *
   * @param {Date} the date to base the calculation off of
   * @returns {number} the timezone offset for the ticks
   */

  /**
   * Get the locale specific multi Tick Format for Time and Date
   * @param {module:nmodule/webChart/rc/localeUtil~OffsetCallback} offsetCallback
   * @return {Function}
   */


  localeUtil.getLocaleTickFormat = function (offsetCallback) {
    return function (date) {
      var format = "";
      var offset = offsetCallback(date),
          normalizedDate = localeUtil.normalizeDate(date, offset);

      if (normalizedDate.getMilliseconds()) {
        format = ".%L";
      } else if (normalizedDate.getSeconds()) {
        format = ":%S";
      } else if (normalizedDate.getMinutes()) {
        format = localeUtil.getD3HourMinuteFormat();
      } else if (normalizedDate.getHours()) {
        format = localeUtil.getD3HourFormat();
      } else if (normalizedDate.getDay() && normalizedDate.getDate() !== 1) {
        format = "%a %d";
      } else if (normalizedDate.getDate() !== 1) {
        format = "%b %d";
      } else if (normalizedDate.getMonth()) {
        format = "%b";
      } else {
        format = "%Y";
      }

      return localeUtil.d3Locale().timeFormat(format)(normalizedDate);
    };
  };
  /**
   * Take the given date and convert the date and time making the timezone of
   * the javascript date not matter. The date returned is respective to the passed
   * in offset.
   *
   * @param {Date} date the date we are using with the correct timezone.
   * @param {number} targetUTCOffset the target timezone in minutes (equivalent to
   * momentjs offset)
   * @returns {Date} a date that has correct date / time, but the timezone is incorrect.
   */


  localeUtil.normalizeDate = function (date, targetUTCOffset) {
    var currentOffset = date.getTimezoneOffset();
    return moment(date).add(currentOffset + targetUTCOffset, 'm').toDate();
  };
  /**
   * Format a Date, Moment, or AbsTime based on the given d3 timeFormat with the current locale
   * @param {string} format
   * @param {Date|moment|baja.AbsTime} date
   * @param {module:baja/obj/TimeZone} timezone
   * @returns {string} Returns 'Invalid Date' if date or moment is not valid.
   */


  localeUtil.format = function (format, date, timezone) {
    if (!date) {
      return "Invalid date";
    }

    if (_.isFunction(date.getJsDate)) {
      //conversion from AbsTime
      date = date.getJsDate();
    } else if (_.isFunction(date.toDate)) {
      //conversion from moment
      if (_.isFunction(date.isValid) && !date.isValid()) {
        return "Invalid date";
      }

      date = date.toDate();
    }

    if (!(date instanceof Date)) {
      return "Invalid date";
    }

    var dateMoment = moment(date).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(date, timezone)),
        normalizedDate = localeUtil.normalizeDate(date, dateMoment.utcOffset()),
        timeZoneShortString = timezone.getShortDisplayName(dateTimeUtil.isDstActive(dateMoment.toDate(), timezone));
    return localeUtil.d3Locale().timeFormat(format.replace("%Z", timeZoneShortString))(normalizedDate);
  };

  return localeUtil;
});
