/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/transform/chartWidgetTransformOperationProvider
 */
define(['Promise', 'nmodule/export/rc/TransformOperation', 'nmodule/webChart/rc/transform/ChartWidgetToChartFile', 'nmodule/webChart/rc/transform/ChartWidgetToCsv', 'nmodule/webChart/rc/transform/ChartWidgetToPrint'], function (Promise, TransformOperation, ChartWidgetToChartFile, ChartWidgetToCsv, ChartWidgetToPrint) {
  'use strict';
  /**
   * @alias module:nmodule/webChart/rc/transform/ChartWidgetTransformOperationProvider
   * @implements module:nmodule/export/rc/TransformOperationProvider
   */

  var chartWidgetTransformOperationProvider = function chartWidgetTransformOperationProvider() {};
  /**
   * @param {*} subject
   * @param {String} title
   * @returns {Promise.<Array.<module:nmodule/export/rc/TransformOperation>>}
   */


  chartWidgetTransformOperationProvider.getTransformOperations = function (subject, title) {
    var isWb = window.niagara && window.niagara.env && window.niagara.env.type === "wb";
    var transformOps = [new TransformOperation(new ChartWidgetToChartFile(title), subject), new TransformOperation(new ChartWidgetToCsv(title), subject)];

    if (!isWb) {
      transformOps.push(new TransformOperation(new ChartWidgetToPrint(), subject));
    }

    return Promise.resolve(transformOps);
  };

  return chartWidgetTransformOperationProvider;
});
