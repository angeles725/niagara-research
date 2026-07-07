/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/transform/ChartWidgetToPrint
 */
define(['baja!', 'lex!webChart', 'Promise'], function (baja, lexs, Promise) {
  'use strict';

  var webChartLex = lexs[0];
  /**
   * Provides the native print dialog
   *
   * @class
   * @alias module:nmodule/webChart/rc/transform/transformer/ChartWidgetToPrint
   * @implements module:nmodule/export/rc/Transformer
   */

  var ChartWidgetToPrint = function ChartWidgetToPrint() {};
  /** @returns {string} */


  ChartWidgetToPrint.prototype.getIcon = function () {
    return webChartLex.get('converters.ChartWidgetToPrint.icon');
  };
  /** @returns {string} `` */


  ChartWidgetToPrint.prototype.getMimeType = function () {
    return '';
  };
  /** @returns {string} */


  ChartWidgetToPrint.prototype.getDisplayName = function () {
    return webChartLex.get('converters.ChartWidgetToPrint.displayName');
  };
  /** @returns {string} '' */


  ChartWidgetToPrint.prototype.getFileExtension = function () {
    return '';
  };
  /**
   * add showDestinations=false to indicate that destination picker is required.
   * @returns {baja.Component}
   */


  ChartWidgetToPrint.prototype.getDefaultConfig = function () {
    return new baja.Component();
  };
  /**
   * @param {baja.Component} config
   * @returns {object}
   */


  ChartWidgetToPrint.prototype.getExportContextObject = function (config) {
    return config.getSlots().toValueMap();
  };
  /**
   * This launches the print preview dialog.
   * @returns {Promise}
   */


  ChartWidgetToPrint.prototype.transform = function () {
    setTimeout(function () {
      window.print();
    }, 100);
    return Promise.resolve();
  };
  /**
   * No ExportDestination is rquired for running the print command.
   * @returns {boolean}
   */


  ChartWidgetToPrint.prototype.isSupplier = function () {
    return false;
  };

  return ChartWidgetToPrint;
});
