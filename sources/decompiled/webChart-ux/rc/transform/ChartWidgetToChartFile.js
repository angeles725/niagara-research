/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/transform/ChartWidgetToChartFile
 */
define(['baja!', 'lex!webChart', 'nmodule/webChart/rc/export/exportUtil', 'nmodule/webChart/rc/choiceUtil'], function (baja, lexs, exportUtil, choiceUtil) {
  'use strict';

  var webChartLex = lexs[0];
  /**
   * Converts a ChartWidget to a Chart File.
   * @param {String} fileName
   *
   * @class
   * @alias module:nmodule/webChart/rc/transform/transformer/ChartWidgetToChartFile
   * @implements module:nmodule/export/rc/Transformer
   */

  var ChartWidgetToChartFile = function ChartWidgetToChartFile(fileName) {
    this.fileName = fileName;
  };
  /** @returns {string} */


  ChartWidgetToChartFile.prototype.getIcon = function () {
    return webChartLex.get('converters.ChartWidgetToChartFile.icon');
  };
  /** @returns {string} `application/json` */


  ChartWidgetToChartFile.prototype.getMimeType = function () {
    return 'application/json';
  };
  /** @returns {string} `chart` */


  ChartWidgetToChartFile.prototype.getFileExtension = function () {
    return 'chart';
  };
  /** @returns {string} */


  ChartWidgetToChartFile.prototype.getFileName = function () {
    return this.fileName;
  };
  /** @returns {string} `charts` */


  ChartWidgetToChartFile.prototype.getFilePath = function () {
    return 'charts';
  };
  /** @returns {string} */


  ChartWidgetToChartFile.prototype.getDisplayName = function () {
    return webChartLex.get('converters.ChartWidgetToChartFile.displayName');
  };
  /**
   * @returns {baja.Component}
   */


  ChartWidgetToChartFile.prototype.getDefaultConfig = function () {
    var comp = new baja.Component();
    comp.add({
      slot: 'ordType',
      value: choiceUtil.makeDynamicEnum(["absolute", "relative"]),
      facets: baja.Facets.make({
        uxFieldEditor: 'webEditors:FrozenEnumEditor'
      }),
      cx: {
        displayName: webChartLex.get('ordType')
      }
    });
    comp.add({
      slot: 'baseOrd',
      value: exportUtil.getBaseOrd(),
      //flags: baja.Flags.HIDDEN, //TODO: How to hide this to start, then show it when ordType=relative
      flags: baja.Flags.TRANSIENT,
      cx: {
        displayName: webChartLex.get('baseOrd')
      }
    });
    return comp;
  };
  /**
   * @param {baja.Component} config
   * @returns {object}
   */


  ChartWidgetToChartFile.prototype.getExportContextObject = function (config) {
    return config.getSlots().toValueMap();
  };
  /**
   * Transform the contents of the ChartWidget to a json formatted Chart File.
   *
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   * @param {object} [cx] context; also used for toString()ing values
   * @param {baja.DynamicEnum} [cx.ordType=0] defaults to ordinal 0 with tag 'absolute' the other option is relative
   * @param {String} [cx.baseOrd=''] if ordType is relative, the base used for relativizing the series ords.
   * @returns {string}
   */


  ChartWidgetToChartFile.prototype.transform = function (widget, cx) {
    return JSON.stringify(widget.makeJson(cx));
  };

  return ChartWidgetToChartFile;
});
