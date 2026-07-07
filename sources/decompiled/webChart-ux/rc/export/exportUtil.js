/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */
define(['Promise', 'baja!', 'underscore', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/dragUtil', 'nmodule/webEditors/rc/wb/table/model/TableModel', 'nmodule/webEditors/rc/transform/transformer/TableModelToCsv', 'nmodule/webEditors/rc/wb/table/model/columns/JsonObjectPropertyColumn', 'moment'], function (Promise, baja, _, webChartUtil, dragUtil, TableModel, TableModelToCsv, JsonObjectPropertyColumn, moment) {
  "use strict";

  var isInTimeRange = webChartUtil.isInTimeRange;
  /**
   * API Status: **Private**
   *
   * A set of utility functions for exporting
   *
   * @exports nmodule/webChart/rc/export/exportUtil
   */

  var exportUtil = {};
  var getOrdsFromDrag = dragUtil.getOrdsFromDrag;
  /**
   * Export a Widget to csv
   * @param widget Widget to export
   * @param {Object} optionInfo Any Options for the export
   * @returns {Promise}
   */

  exportUtil.exportToCsv = function (widget, optionInfo) {
    var model = widget.model(),
        exportObject = [],
        rowHeader = [],
        i,
        series,
        dataRow,
        minTimestamp = Number.MAX_VALUE,
        pointsIndex = [],
        seriesList = [],
        allPoints = [],
        points,
        fullSeriesList = model.seriesList(),
        statusColumn = optionInfo.statusColumn,
        columnHeader,
        displayUnitSymbol,
        displayName,
        timeScale = model.timeScale(),
        timezone = timeScale.getTimeZone();
    exportObject[0] = rowHeader;
    rowHeader.push(webChartUtil.lex.get("timestamp")); //setup header like HistoryChart->export data

    for (i = 0; i < fullSeriesList.length; i++) {
      series = fullSeriesList[i];

      if (!series.isEnabled()) {
        continue; //disabled series not included
      }

      seriesList.push(series);
      displayName = series.shortDisplayName();
      columnHeader = displayName;
      displayUnitSymbol = series.valueScale().displayUnitSymbol();

      if (displayUnitSymbol) {
        columnHeader += "(" + displayUnitSymbol + ")"; //TODO: excel fix
      }

      rowHeader.push({
        name: columnHeader,
        context: series.facets().toObject()
      });

      if (statusColumn) {
        rowHeader.push(displayName + " (" + webChartUtil.lex.get("status") + ")");
      }

      points = series.samplingPoints();
      allPoints.push(points);
      pointsIndex.push(0);
    } //gets points ready for comparison


    var startAndEnd = webChartUtil.getStartAndEndDateFromTimeRange(model.timeRange(), timezone);
    minTimestamp = exportUtil.nextTimestamp(minTimestamp, allPoints, pointsIndex, startAndEnd.start, startAndEnd.end);

    while (true) {
      dataRow = exportUtil.dataRow(minTimestamp, allPoints, pointsIndex, seriesList, statusColumn, startAndEnd.start, startAndEnd.end, timezone, optionInfo);

      if (dataRow) {
        exportObject.push(dataRow);
      } else {
        break;
      }

      minTimestamp = exportUtil.nextTimestamp(minTimestamp, allPoints, pointsIndex, startAndEnd.start, startAndEnd.end);
    }

    return new TableModelToCsv().transform(exportUtil.arrayToTableModel(exportObject), optionInfo);
  };
  /**
   *
   * @param {Array} dataRow
   * @param {Number} index
   * @param {Number} y
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @param {baja.Status} status
   * @param {boolean} statusColumn
   */


  exportUtil.setData = function (dataRow, index, y, series, status, statusColumn) {
    dataRow[index] = series.getYAsValue(y);

    if (statusColumn) {
      dataRow[index + 1] = baja.Status.make(status || 0);
    }
  };
  /**
   *
   * @param {Number} timestamp
   * @param {Array} allPoints
   * @param {Number} pointsIndex
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries>} seriesList
   * @param {boolean} statusColumn
   * @param {moment} start
   * @param {moment} end
   * @param {module:baja/obj/TimeZone} displayedTimezone
   * @param {Object} optionInfo The options selected by the user for the report
   * @return {Array}
   */


  exportUtil.dataRow = function (timestamp, allPoints, pointsIndex, seriesList, statusColumn, start, end, displayedTimezone) {
    var optionInfo = arguments.length > 8 && arguments[8] !== undefined ? arguments[8] : {};
    var encodeToString = optionInfo.encodeToString,
        showDate = optionInfo.showDate,
        showMilliseconds = optionInfo.showMilliseconds,
        showSeconds = optionInfo.showSeconds,
        showTime = optionInfo.showTime,
        showTimeZone = optionInfo.showTimeZone,
        textPattern = optionInfo.textPattern,
        timeFormat = optionInfo.timeFormat;
    var dataRow = [],
        i,
        pointIndex,
        points,
        point,
        useful = false,
        dataRowIndex = 0;

    for (i = 0; i < allPoints.length; i++) {
      points = allPoints[i];
      pointIndex = pointsIndex[i];
      point = points[pointIndex];
      var isValidDate = point && start && end && isInTimeRange(start, end, moment(point.x.getTime()));

      if (point && !point.skip && !point.interpolated && timestamp === point.x.getTime() && isValidDate) {
        if (statusColumn) {
          dataRowIndex = i * 2 + 1;
        } else {
          dataRowIndex = i + 1;
        }

        exportUtil.setData(dataRow, dataRowIndex, point.y, seriesList[i], point.status, statusColumn);
        useful = true;
        pointsIndex[i]++; //pointIndex for series is used, so increment by 1
      } else {//leave blank for now
          //dataRow[i+1] = "nan";
        }
    }

    if (useful) {
      if (encodeToString) {
        dataRow[0] = baja.AbsTime.make({
          jsDate: new Date(timestamp)
        });
      } else {
        dataRow[0] = baja.AbsTime.make({
          jsDate: new Date(timestamp)
        }).toDateTimeStringSync({
          timezone: displayedTimezone,
          showDate: showDate,
          showMilliseconds: showMilliseconds,
          showSeconds: showSeconds,
          showTime: showTime,
          showTimeZone: showTimeZone,
          textPattern: timeFormat || textPattern
        });
      }

      return dataRow;
    } else {
      return null;
    }
  };
  /**
   *
   * @param {Number} timestamp
   * @param {Array} allPoints
   * @param {Number} pointsIndex
   * @param {moment} start
   * @param {moment} end
   * @returns {number}
   */


  exportUtil.nextTimestamp = function (timestamp, allPoints, pointsIndex, start, end) {
    var i,
        pointIndex,
        points,
        point,
        nextTimestamp = Number.MAX_VALUE;

    for (i = 0; i < allPoints.length; i++) {
      points = allPoints[i];
      pointIndex = pointsIndex[i];
      point = points[pointIndex];

      while (point && (point.skip || !isInTimeRange(start, end, moment(point.x.getTime())))) {
        pointsIndex[i]++;
        pointIndex = pointsIndex[i];
        point = points[pointIndex];
      }

      if (point && !point.skip && isInTimeRange(start, end, moment(point.x.getTime()))) {
        nextTimestamp = Math.min(nextTimestamp, point.x.getTime());
      }
    }

    return nextTimestamp;
  };
  /**
   * Relativize a path
   *
   * @param {Array<string>} from base path
   * @param {Array<string>} to path being relativized
   * @returns {string}
   */


  exportUtil.relative = function (from, to) {
    var names = [],
        i,
        a = 0,
        b; // find matching section

    while (a < to.length && a < from.length && from[a].equals(to[a])) {
      a++;
    } // back up


    b = from.length - a;

    for (i = 0; i < b; i++) {
      if (names.length > 0) {
        names.push("/");
      }

      names.push("..");
    } // go forwards


    for (i = a; i < to.length; i++) {
      if (names.length > 0) {
        names.push("/");
      }

      names.push(to[i]);
    }

    return names.join('');
  };
  /**
   * Convert an ord string to an ord string with the view query removed.
   *
   * @param ordString
   * @returns {string}
   */


  exportUtil.getOrdWithoutView = function (ordString) {
    return ordString.replace(/\|view:.*/, '');
  };
  /**
   * Get the index of the first relevant item in the OrdQueryList.
   *
   * Skips station, memory, and bog.
   *
   * @param {baja.OrdQueryList} q
   * @returns {Number}
   */


  exportUtil.getPathIndex = function (q) {
    var stationFound, n; // Skip past station: scheme if there is one.

    for (n = 0; n < q.size(); n++) {
      var schemeName = q.get(n).getSchemeName();

      if (schemeName === "memory") {
        stationFound = true;
        n = n + 2;
        break;
      }

      if (schemeName === "station" || schemeName === "bog") {
        stationFound = true;
        n = n + 1;
        break;
      }
    }

    if (!stationFound) {
      n = 0;
    }

    return n;
  };
  /**
   * Relativize an Ord
   *
   * This logic is duplicate to pxEditor:RelativizeOrds
   *
   * @param {baja.Ord} baseOrd
   * @param {baja.Ord} oldOrd
   * @returns {baja.Ord}
   */


  exportUtil.relativizeOrd = function (baseOrd, oldOrd) {
    var q = oldOrd.normalize().parse();
    var n = exportUtil.getPathIndex(q); // If the first query is not a slot path, just return the original ord.

    var path = q.get(n);

    if (path.getSchemeName() !== "slot") {
      return oldOrd;
    }

    var names = path.getNames();
    var baseOrdQueryList = baseOrd.normalize().parse();
    var basePath = baseOrdQueryList.get(exportUtil.getPathIndex(baseOrdQueryList));

    if (basePath.getSchemeName() !== "slot") {
      return oldOrd;
    }

    var baseNames = basePath.getNames();
    var rel = exportUtil.relative(baseNames, names);
    var queries = [];
    queries.push(new baja.SlotPath(rel));

    for (var i = n + 1; i < q.length; i++) {
      queries.push(q[i]);
    }

    return baja.Ord.make(queries.join(''));
  };
  /**
   * Convert an Array of Arrays of baja.Value to a TableModel.
   * @param {Array.<Array.<baja.Value>>} array
   * @returns {module:nmodule/webEditors/rc/wb/table/model/TableModel}
   */


  exportUtil.arrayToTableModel = function (array) {
    var columnNames = array.shift(); //send first row to column names

    var rows = array;

    var columns = _.map(columnNames, function (name) {
      var obj = typeof name === 'string' ? {
        name: name
      } : name;
      var column = new JsonObjectPropertyColumn(obj.name);
      column.setUnseen(!!obj.unseen);
      column.setExportable(obj.exportable !== false);
      column.data('context', obj.context);
      return column;
    });

    return new TableModel({
      columns: columns,
      rows: _.map(rows, function (row) {
        var obj = {};

        for (var i = 0; i < row.length; i++) {
          obj[columns[i].getName()] = row[i];
        }

        return obj;
      })
    });
  };
  /**
   * Get the current base ORD; e.g. "local:|foxs:|station:|slot:/Folder".
   *
   * @returns {string}
   */


  exportUtil.getBaseOrd = function () {
    // Get base folder / ord
    var baseOrd;

    if (typeof window.niagara !== 'undefined' && window.niagara.env && typeof window.niagara.env.getBaseOrd === 'function') {
      baseOrd = window.niagara.env.getBaseOrd();
    }

    if (!baseOrd) {
      var ords = getOrdsFromDrag(window.location.href, "text");
      baseOrd = ords[0];
    }

    baseOrd = exportUtil.getOrdWithoutView(baseOrd);
    return baseOrd;
  };

  return exportUtil;
});
