function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson and JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/PointSeries
 */
define(['jquery', 'Promise', 'baja!', 'baja!control:ControlPoint', 'log!nmodule.webChart.rc.model.PointSeries', 'nmodule/webChart/rc/model/BaseSeries', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/webChartUtil'], function ($, Promise, baja, types, log, BaseSeries, modelUtil, chartEvents, webChartUtil) {
  "use strict";

  var logSevere = log.severe.bind(log);
  /**
   * Point Series for a chart. This Series subscribes to a live Point.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/PointSeries
   * @extends module:nmodule/webChart/rc/model/BaseSeries
   */

  var PointSeries = function PointSeries() {
    BaseSeries.apply(this, arguments);
    this.$pendingPoints = []; //used for storing updates while chart is paused so that new data can come in when play is used
  };

  PointSeries.prototype = Object.create(BaseSeries.prototype);
  PointSeries.prototype.constructor = PointSeries;
  /**
   * Load the info for the Point Series.
   */

  PointSeries.prototype.loadInfo = function () {
    var that = this,
        model = that.$model;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise(); //nothing to do
    }

    that.setSubscribeContinuous(false); //auto enable live on a new point

    that.$model.setLive(true);
    that.$facets = that.$value.get("facets");

    if (that.$facets) {
      that.$units = that.$facets.get("units", baja.Unit.DEFAULT);
    } //setup valueScale now that units are available


    that.$recordType = that.$value.get("out").get("value").getType().getTypeSpec();
    return Promise.all([model.makeValueScale(that), webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getDisplayPath", String(that.$ord))]).then(function (_ref) {
      var _ref2 = _slicedToArray(_ref, 2),
          valueScale = _ref2[0],
          response = _ref2[1];

      that.$valueScale = valueScale;
      that.$displayPath = response.displayPath;
      that.$model.jq().trigger(chartEvents.REDRAW_REQUEST_EVENT);
      that.$loaded = true;
    });
  };
  /**
   * Unload the data for the Point Series.
   */


  PointSeries.prototype.unload = function () {};
  /**
   * Return the Series display name that will appear in the Legend.
   *
   * @returns {String}  The display name.
   */


  PointSeries.prototype.displayName = function () {
    return this.$value.getDisplayName();
  };
  /**
   * If the Delta array is available and we have a valid last value
   * then create the Delta value.
   *
   * @private
   * @inner
   *
   * @param {module:nmodule/webChart/rc/model/PointSeries} series
   * The Point Series instance.
   * @param obj  The new record used in calculating the Delta.
   * @param last The last record used in calculating the Delta.
   */


  function addToDelta(series, obj, last) {
    var p;

    if (last && series.$deltaPoints) {
      p = $.extend({}, obj);
      p.y = p.y - last.y;
      series.$deltaPoints.push(p);
    }
  }
  /**
   * Create the delta points array for the Series based on the existing
   * non-delta points array.
   *
   * @private
   * @inner
   *
   * @param  {module:nmodule/webChart/rc/model/PointSeries} Point Series.
   * @return {Array] The new Delta Array.
   */


  function createDeltaPoints(series) {
    var points = series.$points,
        last,
        i;
    series.$deltaPoints = [];
    series.$pendingDeltaPoints = []; //start storing pendingDeltaPoints

    for (i = 0; i < points.length; ++i) {
      addToDelta(series, points[i], last);
      last = points[i];
    }

    return series.$deltaPoints;
  }
  /**
   * Update the chart with the current value. If timestamp is not provided, the function
   * will be async and it will retrieve the currentTime from the WebChartQueryServlet.
   * @param {String} [timestamp]
   * @returns {Promise}
   */


  PointSeries.prototype.$update = function (timestamp) {
    var that = this;

    if (timestamp) {
      that.$updateWithTime(timestamp);
    } else {
      return webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getCurrentTime").then(function (results) {
        return that.$updateWithTime(results.currentTime);
      });
    }
  };
  /**
   * Update the chart with the current value of the subscribed point.
   * @param {String} timestamp Timestamp must be provided
   */


  PointSeries.prototype.$updateWithTime = function (timestamp) {
    if (!timestamp) {
      throw new Error("timestamp required!");
    }

    var that = this,
        model = that.$model,
        jq = model.jq(),
        value = that.$value,
        subscribed = that.$subscribed,
        points = subscribed ? that.$points : that.$pendingPoints,
        //when "unsubscribed, store in pending
    deltaPoints = subscribed ? that.$deltaPoints : that.$pendingDeltaPoints,
        dataPoint = {
      x: modelUtil.prepareDate(timestamp),
      y: modelUtil.getValue(value.get("out").getValue()),
      skip: false
    },
        status = value.get("out").getStatus().getBits(),
        skipInfo = modelUtil.getSkipInfo(dataPoint.y, status, undefined, model),
        lastPoint,
        lastDeltaPoint;

    if (status) {
      dataPoint.status = status;
    } // Ensure the new data point is after the last recorded timestamp. This is needed
    // due to the fact the '$lastSubTimestamp' may be out of date.


    if (!points.length || +dataPoint.x > +points[points.length - 1].x) {
      if (!skipInfo.skip) {
        addToDelta(that, dataPoint, points[points.length - 1]);
        points.push(dataPoint);
      } else if (model.settings().getShowDataGaps() === "yes") {
        //go to last point and make that a that a skip to ensure a break in data
        lastPoint = points[points.length - 1];

        if (!lastPoint && !subscribed) {
          //edge case for first point in unsubscribed state
          lastPoint = that.$points[that.$points.length - 1];
        }

        if (deltaPoints) {
          lastDeltaPoint = deltaPoints[deltaPoints.length - 1];

          if (!lastDeltaPoint && !subscribed) {
            //edge case for first point in unsubscribed state
            lastDeltaPoint = that.$deltaPoints[that.$deltaPoints.length - 1];
          }

          if (lastDeltaPoint) {
            deltaPoints.push(modelUtil.getSkipPoint(lastDeltaPoint));
          }
        }

        if (lastPoint) {
          points.push(modelUtil.getSkipPoint(lastPoint));
        }
      }

      that.trimToCapacity();
    }

    if (subscribed) {
      that.setSubscribeContinuous(true);
      jq.trigger(chartEvents.REDRAW_REQUEST_EVENT);
    }
  };
  /**
   * Subscribe to the PointSeries. In this case, this will
   * subscribe to the point so it receives live values.
   *
   * @param  {baja.Subscriber} subscriber The BajaScript Subscriber
   * that should be used to subscribe to a point.
   */


  PointSeries.prototype.subscribe = function (subscriber) {
    var that = this,
        value = that.$value,
        points = that.$points,
        pendingPoints = that.$pendingPoints,
        deltaPoints = that.$deltaPoints,
        pendingDeltaPoints = that.$pendingDeltaPoints,
        jq = that.$model.jq(),
        i;
    that.$subscribed = true;

    that.$changed = that.$changed || function (prop, cx) {
      if (prop.getName() === "out" && value.getHandle() === this.getHandle() && cx) {
        that.$update(cx.timestamp);
      }
    };

    subscriber.attach("changed", that.$changed);
    subscriber.subscribe(value).then(function () {
      return that.$update(value.$lastSubTimestamp);
    })["catch"](logSevere); //move any pending data into the main point array

    if (pendingPoints.length) {
      for (i = 0; i < pendingPoints.length; i++) {
        points.push(pendingPoints[i]);
      }

      pendingPoints.length = 0;

      if (pendingDeltaPoints) {
        for (i = 0; i < pendingDeltaPoints.length; i++) {
          deltaPoints.push(pendingDeltaPoints[i]);
        }
      }

      that.trimToCapacity();
      jq.trigger(chartEvents.REDRAW_REQUEST_EVENT);
    }
  };
  /**
   * Called when the Point Series is unsubscribed.
   *
   * In this case, we're not going to unsubscribe to the point. For something like a
   * point we want to keep getting and storing the data.
   */


  PointSeries.prototype.unsubscribe = function (subscriber) {
    var that = this;
    that.$subscribed = false;
    that.setSubscribeContinuous(false);
  };
  /**
   * Return the data array for the Series.
   * @param {Array} [points] Optional array to set the points
   * @returns {*} The data array for the Series.
    */


  PointSeries.prototype.points = function (points) {
    var that = this;

    if (that.$model.isDelta()) {
      if (points !== undefined) {
        that.$deltaPoints = points;
        that.trimToCapacity();
      }

      return that.$deltaPoints || createDeltaPoints(that);
    } else {
      return BaseSeries.prototype.points.apply(that, arguments);
    }
  };

  return PointSeries;
});
