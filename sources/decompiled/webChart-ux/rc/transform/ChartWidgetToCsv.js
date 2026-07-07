/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/transform/ChartWidgetToCsv
 */
define(['baja!', 'lex!webChart,webEditors', 'underscore', 'nmodule/webChart/rc/export/exportUtil', 'baja!file:ITableToCsv'], function (baja, lexs, _, exportUtil) {
  'use strict';

  var webChartLex = lexs[0],
      webEditorsLex = lexs[1];
  /**
   * Converts a ChartWidget to CSV format.
   *
   * @class
   * @alias module:nmodule/webChart/rc/transform/transformer/ChartWidgetToCsv
   * @implements module:nmodule/export/rc/Transformer
   */

  var ChartWidgetToCsv = function ChartWidgetToCsv(fileName) {
    this.fileName = fileName;
  };
  /** @returns {string} */


  ChartWidgetToCsv.prototype.getIcon = function () {
    return webChartLex.get('converters.ChartWidgetToCsv.icon');
  };
  /** @returns {string} `text/csv` */


  ChartWidgetToCsv.prototype.getMimeType = function () {
    return 'text/csv';
  };
  /** @returns {string} */


  ChartWidgetToCsv.prototype.getDisplayName = function () {
    return webChartLex.get('converters.ChartWidgetToCsv.displayName');
  };
  /** @returns {string} `csv` */


  ChartWidgetToCsv.prototype.getFileExtension = function () {
    return 'csv';
  };
  /** @returns {string}  */


  ChartWidgetToCsv.prototype.getFileName = function () {
    return this.fileName;
  };
  /**
   * @returns {baja.Component}
   */


  ChartWidgetToCsv.prototype.getDefaultConfig = function () {
    var comp = baja.$('file:ITableToCsv');
    comp.add({
      slot: 'statusColumn',
      value: false,
      cx: {
        displayName: webChartLex.get('statusColumn')
      }
    });
    comp.add({
      slot: 'maxRecords',
      value: baja.Integer.MAX_VALUE,
      cx: {
        displayName: webEditorsLex.get('export.maxRecords')
      }
    });
    return comp;
  };
  /**
   * @param {baja.Component} config
   * @returns {object}
   */


  ChartWidgetToCsv.prototype.getExportContextObject = function (config) {
    var obj = config.getSlots().toValueMap(),
        lineEnding = obj.useCRLF ? '\r\n' : '\n'; //since this is history, default to showing seconds by default, but allow facets of 'showSeconds=false' to override

    if (!obj.facets || obj.facets.get('showSeconds') === null) {
      obj.showSeconds = true;
    }

    return _.extend(_.omit(obj, 'facets', 'useCRLF'), obj.facets ? obj.facets.toObject() : {}, {
      lineEnding: lineEnding
    });
  };
  /**
   * Transform the contents of the TableModel or PaginationModel into CSV.
   *
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   * @param {object} [cx] context; also used for toString()ing values
   * @param {string} [cx.delimiter=','] custom delimiter
   * @param {boolean} [cx.includeBOM=true] set to false to omit the UTF-8
   * byte order mark from the CSV (be aware that omitting the BOM does not
   * play nice with Excel)
   * @param {boolean} [cx.includeHeaders=true] set to false to not include
   * header columns in the CSV
   * @param {string} [cx.lineEnding='\n'] choose a custom line ending like \r\n
   * @param {boolean} [cx.statusColumn=true] set to true to include the status column
   * @returns {Promise.<string>}
   */


  ChartWidgetToCsv.prototype.transform = function (widget, cx) {
    return exportUtil.exportToCsv(widget, cx);
  };

  return ChartWidgetToCsv;
});
