/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * ExternalSeries Series for a chart. The data is provided outside of Niagara
 *
 * @module nmodule/webChart/rc/model/ExternalSeries
 * @private
 */
define(['d3', 'Promise', 'baja!', 'nmodule/webChart/rc/model/BaseSeries', 'nmodule/webChart/rc/model/modelUtil'], function (d3, Promise, baja, BaseSeries, modelUtil) {
  "use strict";
  /**
   * A Series for external data like that from a csv file
   *
   * @private
   * @class
   * @alias module:nmodule/webChart/rc/model/ExternalSeries
   */

  var ExternalSeries = function ExternalSeries() {
    BaseSeries.apply(this, arguments);
  };

  ExternalSeries.prototype = Object.create(BaseSeries.prototype);
  ExternalSeries.prototype.constructor = ExternalSeries;

  ExternalSeries.prototype.isPrimaryLoad = function () {
    var that = this,
        model = that.$model,
        timeRange = model.timeRange();
    return timeRange.getPeriod().getOrdinal() !== 0;
  };
  /**
   * Load the data for the Schedule Series.
   */


  ExternalSeries.prototype.loadInfo = function () {
    var that = this,
        model = that.$model,
        ord = that.$ord;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise();
    } //ensure we start with a refresh set of points


    that.$points.length = 0;
    that.$focusPoints = null;
    that.$samplingPoints = null;
    that.$displayName = ord; //TODO: fix

    that.$recordType = "baja:Double";
    that.$units = that.$facets.get("units", baja.Unit.DEFAULT); //setup valueScale now that units are available

    return model.makeValueScale(that).then(function (valueScale) {
      that.$valueScale = valueScale;
    });
  };
  /**
   * Load the series data.
   *
   * @returns {Promise}
   */


  ExternalSeries.prototype.loadData = function () {
    var that = this;
    this.$points = this.$value.slice(0);
    that.$focusPoints = null;
    that.$samplingPoints = null;
    that.$loaded = true;
    that.$loading = false;
    return Promise.resolve();
  };

  function prepareToString(string, acceptingFormat) {
    if (!acceptingFormat) {
      acceptingFormat = '%e-%b-%y/%H:%M:%S/%p';
    } //2-Aug-18 11:18:57 am
    //2-Aug-18 13:43:22 PM
    //var format = d3.time.format('%Y-%m-%dT%H:%M:%S.%LZ').parse;


    string = string.replace("am", "AM");
    string = string.replace("pm", "PM");
    string = string.replace(/ /g, "/");
    var format = d3.time.format(acceptingFormat).parse;
    return format(string);
  }
  /**
   * @returns {Promise.<Array.<module:nmodule/webChart/rc/model/ExternalSeries>>}
   */


  ExternalSeries.factory = function (model, subscriber, seriesParams) {
    var value = seriesParams.value; //Example Header Timestamp, Point1 (Optional Units), Option Point1 (Status), Point2 (Optional Units), Option Point2 (Status)

    var timeStampIndex = -1,
        headerRow = value[0],
        seriesInfos = [];

    for (var i = 0; i < headerRow.length; i++) {
      if (headerRow[i] === "Timestamp") {
        timeStampIndex = i;
      } else if (headerRow[i] !== null && headerRow[i].indexOf("(Status)") < 0) {
        var header = headerRow[i],
            statusHeader = headerRow[i + 1],
            unitIndex = header.indexOf("("),
            name = header,
            unit = "";

        if (unitIndex > -1) {
          name = header.substring(0, unitIndex);
          unit = header.substring(unitIndex + 1, header.length - 1);
        }

        var statusIndex = -1;

        if (statusHeader === name + "(Status)") {
          statusIndex = i + 1;
        }

        seriesInfos.push({
          ord: name,
          name: name,
          unit: unit,
          valueIndex: i,
          statusIndex: statusIndex,
          points: []
        });
      }
    }

    for (i = 1; i < value.length; i++) {
      var timestampEncoding = value[i][timeStampIndex];
      var date = prepareToString(timestampEncoding);

      if (i === 1) {// window.console.log("encoding:" + timestampEncoding);
        // window.console.log("date:" + date);
      }

      if (!date) {
        //try again 3/31/2018 14:00
        // window.console.log("test:" + d3.time.format('%m/%d/%Y/%H:%M')(new Date()));
        date = prepareToString(timestampEncoding, '%m/%d/%Y/%H:%M'); // window.console.log("date2:" + date);
      }

      if (!date) {
        throw Error("Cannot except format yet:" + timestampEncoding);
      }

      for (var j = 0; j < seriesInfos.length; j++) {
        var seriesInfo = seriesInfos[j];
        var y = value[i][seriesInfo.valueIndex];

        if (y === undefined || y === "") {
          continue;
        }

        y = parseFloat(y); // window.console.log("y:" + y);

        var point = {
          x: date,
          y: y,
          skip: false
        };

        if (seriesInfo.statusIndex > -1) {
          point.status = value[i][seriesInfo.statusIndex];
        }

        seriesInfo.points.push(point);
      }
    }

    var seriesList = [];

    for (var k = 0; k < seriesInfos.length; k++) {
      //ensure all timestamp are in the proper order
      var sorted = seriesInfos[k].points.sort(function (x, y) {
        return x.x - y.x;
      });
      seriesList.push(new ExternalSeries(model, {
        ord: seriesInfos[k].ord,
        value: sorted
      }));
    }

    return Promise.resolve(seriesList);
  };

  return ExternalSeries;
});
