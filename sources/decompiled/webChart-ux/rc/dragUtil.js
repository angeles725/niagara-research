function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich and Gareth Johnson
 */

/* eslint-env browser */
define(['log!nmodule.webChart.rc.dragUtil', "bajaux/dragdrop/dragDropUtils", "jquery", "Promise"], function (log, dragDropUtils, $, Promise) {
  "use strict";

  var logSevere = log.severe.bind(log);
  var TEXT = /^text/i;

  function isText(mime) {
    return !!TEXT.exec(mime);
  }
  /**
   * Attempt to retrieve `niagara/navnodes` data added to the clipboard in a
   * dragstart.
   *
   * @inner
   * @param {DataTransfer} dataTransfer
   * @returns {Promise} promise to be resolved with an array of
   * nav node JSON, or with `null` if none is on the clipboard
   */


  function getNavNodes(dataTransfer) {
    return dragDropUtils.fromClipboard(dataTransfer).then(function (envelope) {
      switch (envelope.getMimeType()) {
        case 'niagara/navnodes':
          return envelope.toJson();
      }
    })["catch"](function () {
      return null; //ignore
    });
  }
  /**
   * Attempt to read the contents of a chart file being dragged onto the chart.
   *
   * @inner
   * @param {DataTransfer} dataTransfer
   * @returns {Promise} promise to be resolved with an array of series
   * data read from the dragged file, or with `null` if no file data is present
   */


  function getChartFileSeries(dataTransfer) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      if (typeof FileReader !== 'undefined' && dataTransfer.files && dataTransfer.files.length && /\.chart$/i.test(dataTransfer.files[0].name)) {
        // Handle when a user just drags and drops a chart file onto a chart
        // in the browser.
        var reader = new FileReader();

        reader.onloadend = function () {
          // Nullify the function reference. Just in case of memory leaks.
          reader.onloadend = null;

          try {
            var chartJson = JSON.parse(reader.result),
                series = chartJson && chartJson.model && chartJson.model.series;
            resolve(series && series.slice());
          } catch (e) {
            reject(e);
          }
        };

        reader.readAsText(dataTransfer.files[0]);
      } else {
        resolve(null);
      }
    });
  }
  /**
   * API Status: **Private**
   *
   * Utility for handling drag and drop.
   *
   * @exports nmodule/webChart/rc/dragUtils
   */


  var exports = {};
  /**
   * Process the data from a drag so that an Array of ords are returned.
   *
   * @param {String} type
   * @param {Object|String} value data from drag and drop.
   * @returns {Array.<String>}
   */

  exports.getOrdsFromDrag = function (value, type) {
    var array = [],
        ord,
        i;

    if (type === 'text/csv') {
      array.push(value);
    } else if (isText(type)) {
      ord = decodeURIComponent(value); // Skip over the http(s):// protocol

      var skipProtocol = ord.indexOf("://") + 3; // skip over the server / user-name / port, etc

      var skipServer = ord.indexOf("/", skipProtocol); // Skip over the first scheme name

      var skipScheme = ord.indexOf(":", skipServer); // Back up to find either / or ? (which should be /ord/ or /ord?)

      var start = ord.lastIndexOf("?", skipScheme);

      if (start === -1) {
        start = ord.lastIndexOf("/", skipScheme);
      }

      ord = ord.substring(start + 1);
      ord = ord.replace(/\|view:.*/, ''); // Remove view ord

      array.push(ord);
    } else if (type === "niagara/navnodes" || type === "application/json") {
      value = $.isArray(value) ? value : [value];

      for (i = 0; i < value.length; i++) {
        ord = value[i].ord;
        ord = ord.replace(/^.*fox:\d*\|/, ''); //ip:localhost|fox:1912|history:

        ord = ord.replace(/^.*fox:*\|/, ''); //ip:localhost|fox:|history:

        ord = ord.replace(/^.*fox:$/, ''); //ip:localhost|fox:
        // history:|history:/webChart/BigRamp

        ord = ord.replace(/^.*history:\|history:/, 'history:');
        array.push(ord);
      }
    }

    return array;
  };
  /**
   * Arm the drop handlers for the DOM element and invoke the callback when a
   * drop happens.
   *
   * @param {jQuery} jq The jQuery for the DOM element.
   * @param {Function} callback Called when a valid drop occurs.
   */


  exports.armDrop = function (jq, callback) {
    jq.on("drop", function (event) {
      var transfer = event.originalEvent.dataTransfer,
          type,
          value;
      Promise.all([getNavNodes(transfer), getChartFileSeries(transfer)]).then(function (_ref) {
        var _ref2 = _slicedToArray(_ref, 2),
            navNodes = _ref2[0],
            series = _ref2[1];

        if (navNodes) {
          value = navNodes;
          type = 'niagara/navnodes';
        } else if (series) {
          value = series;
          type = 'application/json';
        } else {
          value = transfer.getData('Text');
          type = 'Text';
        }

        if (value && type) {
          callback(event, value, type);
        }
      })["catch"](logSevere);
      event.preventDefault();
    });
    jq.on("dragover dragenter", function (event) {
      event.preventDefault();
    });
  };

  return exports;
});
