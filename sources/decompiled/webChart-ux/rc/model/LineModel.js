/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/LineModel
 */
define(['baja!', 'baja!webChart:WebChartTimeRange', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/model/BaseModel', 'nmodule/webChart/rc/model/TimeScale', 'nmodule/webChart/rc/chartEvents', 'moment'], function (baja, types, webChartUtil, BaseModel, TimeScale, chartEvents, moment) {
  "use strict";
  /**
   * LineModel represents the servlet data for a line chart.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/LineModel
   * @param {jQuery} jq Chart Widget's jq
   */

  var LineModel = function LineModel(jq) {
    var that = this;
    BaseModel.apply(that, arguments); //line model options

    that.$delta = false;
    that.$live = true;
    that.$timeScale = new TimeScale(that); //TODO: is 1 day a good "Custom Time Period" default? Should we move to options?

    that.$timeRange = baja.$('webChart:WebChartTimeRange', {
      //period: baja.$('webChart:WebChartTimeRangeType', 0), //auto is slow on JACE6
      period: baja.$('webChart:WebChartTimeRangeType', 2),
      //today for best performance
      startTime: webChartUtil.getAbsTime(moment().subtract(1, 'days')),
      endTime: baja.AbsTime.now()
    });
  };

  LineModel.prototype = Object.create(BaseModel.prototype);
  LineModel.prototype.constructor = LineModel;
  /**
   * Returns the Model's live state.
   * @returns {Boolean}
   */

  LineModel.prototype.isLive = function () {
    return this.$live;
  }; //LineModel.prototype.stop = function() {
  //  var that = this;
  //  BaseModel.prototype.stop.apply(that, arguments);
  //};

  /**
   * Sets the Model's live state.
   *
   * Fires off a MODEL_LIVE or MODEL_NOT_LIVE event if it changes.
   *
   * @param {Boolean} live
   */


  LineModel.prototype.setLive = function (live) {
    var that = this,
        jq = that.jq(),
        oldLive = that.$live;

    if (oldLive !== live) {
      that.$live = live;

      if (jq) {
        jq.trigger(live ? chartEvents.MODEL_LIVE : chartEvents.MODEL_NOT_LIVE, [that]);
      }
    }

    return that.$live;
  };
  /**
   * Returns the Model's delta state.
   * @returns {Boolean}
   */


  LineModel.prototype.isDelta = function () {
    return this.$settings.isDelta();
  };
  /**
   * Sets the Model's delta state.
   * @param {Boolean} delta
   */


  LineModel.prototype.setDelta = function (delta) {
    var that = this,
        jq = that.jq();

    if (delta !== that.isDelta()) {
      this.$settings.setDelta(delta);

      if (jq) {
        jq.trigger(chartEvents.DELTA_CHANGED, [that]);
      }
    }
  };
  /**
   * Time Range accessor
   *
   * @param {webChart.WebChartTimeRange} [timeRange] If specified, this will set the jQuery DOM.
   * @returns {webChart.WebChartTimeRange}
   */


  LineModel.prototype.timeRange = function (timeRange) {
    var that = this,
        jq = that.jq();

    if (timeRange !== undefined) {
      that.$timeRange = timeRange;

      if (jq) {
        jq.trigger(chartEvents.TIME_RANGE_CHANGED, [that]);
      }
    }

    return that.$timeRange;
  };
  /**
   * TimeScale accessor
   *
   * @returns {webChart.TimeScale}
   */


  LineModel.prototype.timeScale = function () {
    return this.$timeScale;
  };

  LineModel.prototype.loadFromJson = function (chartJson, chartSettings) {
    var that = this;
    BaseModel.prototype.loadFromJson.apply(that, arguments);

    if (chartJson && chartJson.model && chartJson.model.timeRange) {
      that.timeRange(baja.bson.decodeValue(chartJson.model.timeRange));
    }
  };

  LineModel.prototype.saveToJson = function (chartJson) {
    var that = this;
    BaseModel.prototype.saveToJson.apply(that, arguments);
    chartJson.model.timeRange = baja.bson.encodeValue(that.$timeRange);
  };

  return LineModel;
});
