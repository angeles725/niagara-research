/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/ScheduleSeries
 */
define(['Promise', 'baja!', 'baja!schedule:BooleanSchedule', 'nmodule/webChart/rc/model/BaseSeries', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/webChartUtil', 'moment'], function (Promise, baja, types, BaseSeries, modelUtil, webChartUtil, moment) {
  "use strict";
  /**
   * Schedule Series for a chart. This Series subscribes to a live Schedule.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/ScheduleSeries
   * @extends module:nmodule/webChart/rc/model/BaseSeries
   */

  var ScheduleSeries = function ScheduleSeries() {
    BaseSeries.apply(this, arguments);
  };

  ScheduleSeries.prototype = Object.create(BaseSeries.prototype);
  ScheduleSeries.prototype.constructor = ScheduleSeries;

  ScheduleSeries.prototype.isPrimaryLoad = function () {
    var that = this,
        model = that.$model,
        timeRange = model.timeRange();
    return timeRange.getPeriod().getOrdinal() !== 0;
  };
  /**
   * Load the data for the Schedule Series.
   */


  ScheduleSeries.prototype.loadInfo = function () {
    var that = this,
        model = that.$model,
        timeRange = model.timeRange(),
        ord = that.$ord,
        start,
        end;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise();
    } //ensure we start with a refresh set of points


    that.$points.length = 0;
    that.$focusPoints = null;
    that.$samplingPoints = null;

    if (timeRange.getPeriod().getOrdinal() === 0) {
      //auto is bad for schedules, so lets retrieve the start and end time of all the data
      //TODO: if schedules are loaded concurrently with points, the answer will be wrong
      var extent = model.getTimeExtent();

      if (extent[0] && extent[1]) {
        start = webChartUtil.getAbsTime(moment(extent[0]));
        end = webChartUtil.getAbsTime(moment(extent[1]));

        if (!start && !end) {//TODO: re-organize promises
        }
      }
    }

    var infoPromise = webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getInfo", [ord]).then(function (response) {
      if (response.capacities && response.capacities.length > 0) {
        that.capacity(parseInt(response.capacities[0]));
      }

      if (response.recordTypes && response.recordTypes.length > 0) {
        that.$recordType = response.recordTypes[0];
      }

      if (response.displayPaths && response.displayPaths.length > 0) {
        that.$displayPath = response.displayPaths[0];
        var split = that.$displayPath.split('/');
        that.$displayName = split[split.length - 1];
      }

      var valueFacets = response.valueFacets;
      return valueFacets && valueFacets.length && baja.Facets.DEFAULT.decodeAsync(valueFacets[0]);
    }).then(function (facets) {
      if (facets) {
        that.$facets = facets;
        that.$units = that.$facets.get("units", baja.Unit.DEFAULT);
      } //setup valueScale now that units are available


      return model.makeValueScale(that);
    }).then(function (valueScale) {
      that.$valueScale = valueScale;
    });
    return infoPromise;
  };
  /**
   * Load the series data.
   *
   * @returns {Promise}
   */


  ScheduleSeries.prototype.loadData = function () {
    var that = this,
        model = that.$model,
        delta = model.isDelta ? model.isDelta() : false,
        timeRange = model.timeRange(),
        fullOrd,
        ord = that.$ord,
        start,
        end;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise(); //nothing to do
    }

    that.$loading = true; //ensure we start with a refresh set of points

    that.$points.length = 0;
    that.$focusPoints = null;
    that.$samplingPoints = null;

    if (timeRange.getPeriod().getOrdinal() === 0) {
      //auto is bad for schedules, so lets retrieve the start and end time of all the data
      //TODO: if schedules are loaded concurrently with points, the answer will be wrong
      var extent = model.getTimeExtent();

      if (extent[0] && extent[1]) {
        start = webChartUtil.getAbsTime(moment(extent[0]));
        end = webChartUtil.getAbsTime(moment(extent[1]));

        if (!start && !end) {//TODO: re-organize promises
        }
      }
    }

    fullOrd = modelUtil.getFullScheduleOrd({
      ord: ord,
      timeRange: timeRange,
      delta: delta,
      start: start,
      end: end
    });
    var dataUri = "/webChart/query/schedule/" + baja.SlotPath.escape(fullOrd);
    return modelUtil.chunkData(model, that, dataUri).then(function () {
      that.$loaded = true;
      that.$loading = false;
    });
  };
  /**
   * Properly format a results from WebChartQueryServlet for schedules and append it to the current data.
   * @param {Object} raw the object passed over from the WebChartQueryServlet
   */


  ScheduleSeries.prototype.preparePoint = function (raw) {
    return modelUtil.prepareServletPoint(raw, this.$points, this.$model);
  };

  ScheduleSeries.prototype.getDefaultInterpolation = function () {
    return "step-after";
  };
  /**
   * Return the Series display name that will appear in the Legend.
   *
   * @returns {String}  The display name.
   */


  ScheduleSeries.prototype.displayName = function () {
    return this.$value.getDisplayName();
  };

  return ScheduleSeries;
});
