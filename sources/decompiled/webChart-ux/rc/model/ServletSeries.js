/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/ServletSeries
 */
define(['Promise', 'baja!', 'nmodule/history/rc/servlet/queryServletUtil', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/model/BaseModel', 'nmodule/webChart/rc/model/BaseSeries', 'nmodule/webChart/rc/chartEvents'], function (Promise, baja, queryServletUtil, webChartUtil, modelUtil, BaseModel, BaseSeries, chartEvents) {
  "use strict";
  /**
   * ServletSeries represents the series coming rom the WebChartQueryRpc for a chart.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/ServletSeries
   * @extends module:nmodule/webChart/rc/model/BaseSeries
   */

  var ServletSeries = function ServletSeries() {
    BaseSeries.apply(this, arguments);
  };

  ServletSeries.prototype = Object.create(BaseSeries.prototype);
  ServletSeries.prototype.constructor = ServletSeries;
  /**
   * Load the info for the series.
   *
   * @returns {Promise}
   */

  ServletSeries.prototype.loadInfo = function () {
    var that = this,
        model = that.$model,
        ord = that.$ord;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise();
    } //ensure we start with a refresh set of points


    that.$points.length = 0;
    that.$focusPoints = null;
    that.$samplingPoints = null;
    var infoPromise = webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getInfo", [String(ord)]).then(function (response) {
      if (response.recordTypes && response.recordTypes.length > 0) {
        that.$recordType = response.recordTypes[0];
      }

      if (response.displayPaths && response.displayPaths.length > 0) {
        that.$displayPath = response.displayPaths[0];
      }

      if (response.timezoneIDs && response.timezoneIDs.length > 0) {
        that.setPreferredTimeZone(model.timeScale().getTimeZoneDatabase().getTimeZone(response.timezoneIDs[0]));
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

      if (that.$value && that.$value.toConfig) {
        return that.$value.toConfig().then(function (config) {
          that.$displayName = config.get("navDisplayName");
        });
      }
    }).then(function () {
      //fallbacks for missing displayName
      if (!that.$displayName) {
        if (that.$displayPath) {
          var split = that.$displayPath.split('/');
          that.$displayName = split[split.length - 1];
        } else {
          that.$displayName = webChartUtil.getDisplayName(String(that.$ord));
        }
      }
    });
    return infoPromise;
  };
  /**
   * Load the series data.
   *
   * @returns {Promise}
   */


  ServletSeries.prototype.loadData = function () {
    var that = this,
        model = that.$model,
        delta = model.isDelta ? model.isDelta() : false,
        timeRange = model.timeRange(),
        timezone = model.timeScale().getTimeZone(),
        startAndEnd = webChartUtil.getStartAndEndDateFromTimeRange(timeRange, timezone),
        start = startAndEnd.start,
        end = startAndEnd.end,
        ord = that.$ord,
        fullOrd;

    if (that.$loaded || that.$loading) {
      return modelUtil.resolvedPromise();
    }

    that.$loading = true; //ensure we start with a refresh set of points

    that.$points.length = 0;
    that.$focusPoints = null;
    that.$samplingPoints = null;
    fullOrd = modelUtil.getFullOrd({
      ord: ord,
      start: baja.AbsTime.make({
        jsDate: start.toDate()
      }),
      end: baja.AbsTime.make({
        jsDate: end.toDate()
      }),
      delta: delta
    });
    var dataUri = "/webChart/query/data/" + baja.SlotPath.escape(fullOrd);
    return modelUtil.chunkData(model, that, dataUri).then(function () {
      that.$loaded = true;
      that.$loading = false;
    });
  };
  /**
   * Properly format a Niagara history entry passed over from the WebChartQueryServlet and append it to the current data.
   * @param {Object} raw the object passed over from the WebChartQueryServlet
   */


  ServletSeries.prototype.preparePoint = function (raw) {
    modelUtil.prepareServletPoint(raw, this.$points, this.$model);
  };
  /**
   * Properly format a Niagara history entry passed over from box and append it to the current data.
   * @param {Object} raw the object passed over from box
   */


  ServletSeries.prototype.prepareLivePoint = function (raw) {
    modelUtil.prepareLivePoint(raw, this.$points, this.$model);
  };
  /**
   * Run a function against the data once its retrieved.
   *
   * @param {ServletSeries} series
   * @param {BaseModel} model
   * @param {Function} completeFunction
   * @param {baja.Subscriber} subscriber
   * @param {boolean} subscribing
   * @return {Promise}
   */


  function applySourceOrds(series, model, completeFunction, subscriber, subscribing) {
    var ord = series.ord();
    return queryServletUtil.resolveHistorySourceOrd(ord, subscribing).then(function (sourceOrd) {
      if (sourceOrd) {
        return completeFunction(series, model, subscriber, sourceOrd);
      }
    });
  }

  function subscribe(series, model, subscriber, ord) {
    var jq = model.jq();

    if (!ord) {
      return;
    }

    series.$changed = series.$changed || function (prop, cx) {
      var that = this,
          lastRecord,
          delta = model.isDelta();

      if (model.isLoading() || model.isStopped()) {
        return;
      }

      if (prop.getName() !== "lastRecord" && prop.getName() !== "lastSuccess") {
        return;
      }

      if (that !== series.$comp) {
        return;
      }

      if (typeof that.getLastRecord === 'function') {
        lastRecord = that.getLastRecord();

        if (lastRecord.getTimestamp().getJsDate().getFullYear() < 1971) {
          return;
        }
      }

      if (series.isSubscribeContinuous()) {
        if (delta || !lastRecord) {
          //Both for delta and NiagaraHistoryImport, we must do bql queries to get the new data.
          startSubscription(series, model);
        } else {
          series.prepareLivePoint(lastRecord, model);
          series.trimToCapacity();
        }
      } else {//TODO: skipped for now, but potentially a race condition here where the timeRange misses an item
        //webChartUtil.trace("skipped for now:" + lastRecord.getTimestamp().encodeToString());
      }

      jq.trigger(chartEvents.REDRAW_REQUEST_EVENT);
    };

    ord.get({
      ok: function ok() {
        series.$comp = this;
        subscriber.attach("changed", series.$changed);
      },
      subscriber: subscriber,
      fail: baja.error
    });
    startSubscription(series, model);
  }
  /**
   * Starts the live subscription up and appends all the data not yet on the chart.
   * @param {module:nmodule/webChart/rc/model/ServletSeries} series
   * @param {LineModel} model
   * @inner
   */


  function startSubscription(series, model) {
    //generate time range based history ord
    //history:/historyTest/Random?start=2013-12-06T00:00:00.000-05:00;end=2013-12-06T08:51:57.001-05:00
    var points = series.servletPoints(),
        ord = series.ord(),
        delta = model.isDelta(),
        timeRange = model.timeRange(),
        jsDate,
        startAbsTime,
        results,
        start,
        timeScale = model.timeScale(); //TODO, no data, so where does subscription start?

    if (points.length > 0) {
      jsDate = points[points.length - 1].x;
      jsDate = modelUtil.safeMomentAdd(jsDate, 1).toDate(); //don't retrieve the last record

      startAbsTime = baja.AbsTime.make({
        jsDate: jsDate
      });
    } else if (timeRange.getPeriod().getOrdinal() > 1) {
      results = webChartUtil.getStartAndEndDateFromPeriod(timeRange.getPeriod(), timeScale.getTimeZone());
      start = results.start;

      if (start) {
        startAbsTime = webChartUtil.getAbsTime(start);
      }
    } else if (timeRange.getPeriod().getOrdinal() === 1) {
      if (timeRange.getStartFixed()) {
        startAbsTime = timeRange.getStartTime();
      }
    }

    ord = modelUtil.getFullOrd({
      ord: ord,
      start: startAbsTime,
      delta: delta
    });
    var dataUri = "/webChart/query/data/" + baja.SlotPath.escape(ord);
    modelUtil.chunkData(model, series, dataUri).then(function () {
      series.setSubscribeContinuous(true);
    })["catch"](function (err) {
      baja.error(err);
    });
  }
  /**
   * Subscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   * @return {Promise}
   */


  ServletSeries.prototype.subscribe = function (subscriber) {
    if (!this.isSubscribeContinuous()) {
      //only need to do this if not already subscribed
      return applySourceOrds(this, this.$model, subscribe, subscriber, true);
    }

    return Promise.resolve();
  };
  /**
   * Unsubscribe utility function.
   *
   * @param {ServletSeries} series
   * @param {BaseModel} model
   * @inner
   * @param subscriber
   * @return {Promise}
   */


  function unsubscribe(series, model, subscriber) {
    if (series.$comp) {
      return subscriber.unsubscribe(series.$comp).then(function () {
        series.setSubscribeContinuous(false); //time query required for next tim
      });
    }

    series.setSubscribeContinuous(false); //time query required for next time

    return Promise.resolve();
  }
  /**
   * Unsubscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   * @return {Promise}
   */


  ServletSeries.prototype.unsubscribe = function (subscriber) {
    if (this.isSubscribeContinuous()) {
      //only need to do this if not already unsubscribed
      return applySourceOrds(this, this.$model, unsubscribe, subscriber, false);
    }

    return Promise.resolve();
  };
  /**
   * A method for accessing the raw servlet points only. This is the method to use to get points
   * without the interpolated point on the end.
   *
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>}
   */


  ServletSeries.prototype.servletPoints = function () {
    return this.$points || [];
  };

  return ServletSeries;
});
