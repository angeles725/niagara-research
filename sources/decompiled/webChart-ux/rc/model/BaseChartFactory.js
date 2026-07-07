/**
 * @copyright 2016 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * @module nmodule/webChart/rc/model/BaseChartFactory
 */
define(['Promise'], function (Promise) {
  "use strict";
  /**
   * API Status: **Development**
   *
   * BaseChartFactory provides a Base class for deciding what to do when a value of the registered type
   * is added to a ChartWidget.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/BaseChartFactory
   */

  var BaseChartFactory = function BaseChartFactory() {};
  /**
   * Given the model, subscriber, and seriesParams, provide additional BaseSeries to the model
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {baja.Subscriber} subscriber Utilize This for live updates
   * @param {Object} seriesParams The container for series Parameters known so far.
   * @param {baja.Value} seriesParams.value The baja.Value dropped onto the chart
   * @param {baja.Ord} seriesParams.ord The baja.Ord for the item dropped onto the chart
   * @param {Object} [params] This optional argument is a container for additional factory
   * resolution parameters. Typically this params Object should just be passed through to any
   * recursive factory calls, including seriesFactory.make() calls. This parameter was added
   * starting in Niagara 4.7.
   * @param {Array.<String>} params.currentSeriesOrds Collects existing and pending ORD strings
   * for series that are (or will be) added to the chart.  This allows for limit checking to
   * skip looking for additional series when the chart's series limit is reached.
   * @returns {Promise.<Array.<module:nmodule/webChart/rc/model/BaseSeries>>} Resolves the promise to an array of
   * series that should be add to the chart.
   */


  BaseChartFactory.prototype.factory = function (model, subscriber, seriesParams, params) {
    return Promise.resolve();
  };

  return BaseChartFactory;
});
