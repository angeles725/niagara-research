function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

/**
 * @copyright 2022 Tridium, Inc. All Rights Reserved.
 */
define(['log!bajaux.spandrel', 'log!bajaux.spandrel.dom', 'bajaux/spandrel/symbols', 'nmodule/js/rc/log/Log'], function (spandrelLog, domLog, symbols, Log) {
  'use strict';

  var _Log$Level = Log.Level,
      FINE = _Log$Level.FINE,
      FINER = _Log$Level.FINER,
      FINEST = _Log$Level.FINEST;
  var INSTANCE_COUNTER_SYMBOL = symbols.INSTANCE_COUNTER_SYMBOL;
  var LIFECYCLE_LEVEL = FINE;
  var LIFECYCLE_DETAILS_LEVEL = FINER;
  var TIMING_DETAILS_LEVEL = FINEST;
  var DOM_DETAILS_LEVEL = FINEST;
  /**
   * API Status: **Private**
   * @exports bajaux/spandrel/logging
   */

  var exports = {};
  /**
   * @returns {boolean} whether we should log lifecycle events (coarse-grained diffing information)
   */

  exports.isLifecycleLoggable = function () {
    return spandrelLog.isLoggable(LIFECYCLE_LEVEL);
  };
  /**
   * @returns {boolean} whether we should log lifecycle details (finer info about exactly what is being diffed)
   */


  exports.areLifecycleDetailsLoggable = function () {
    return spandrelLog.isLoggable(LIFECYCLE_DETAILS_LEVEL);
  };
  /**
   * @returns {boolean} whether we should log very fine-grained timing info - "starting rerender",
   * "finished rerender", etc.
   */


  exports.areTimingDetailsLoggable = function () {
    return spandrelLog.isLoggable(TIMING_DETAILS_LEVEL);
  };
  /**
   * @returns {boolean} whether we want to see details about DOM changes (very chatty)
   */


  exports.areDomDetailsLoggable = function () {
    return domLog.isLoggable(DOM_DETAILS_LEVEL);
  };
  /**
   * @param {module:bajaux/Widget} owner
   * @param {string} msg
   * @param {...*} args
   * @returns {Promise}
   */


  exports.logLifecycle = function (owner, msg) {
    for (var _len = arguments.length, args = new Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
      args[_key - 2] = arguments[_key];
    }

    return ownerLog(LIFECYCLE_LEVEL, owner, msg, args);
  };
  /**
   * @param {module:bajaux/Widget} owner
   * @param {string} msg
   * @param {...*} args
   * @returns {Promise}
   */


  exports.logLifecycleDetails = function (owner, msg) {
    for (var _len2 = arguments.length, args = new Array(_len2 > 2 ? _len2 - 2 : 0), _key2 = 2; _key2 < _len2; _key2++) {
      args[_key2 - 2] = arguments[_key2];
    }

    return ownerLog(LIFECYCLE_DETAILS_LEVEL, owner, msg, args);
  };
  /**
   * @param {module:bajaux/Widget} owner
   * @param {string} msg
   * @param {...*} args
   * @returns {Promise}
   */


  exports.logTimingDetails = function (owner, msg) {
    for (var _len3 = arguments.length, args = new Array(_len3 > 2 ? _len3 - 2 : 0), _key3 = 2; _key3 < _len3; _key3++) {
      args[_key3 - 2] = arguments[_key3];
    }

    return ownerLog(TIMING_DETAILS_LEVEL, owner, msg, args);
  };
  /**
   * @param {module:bajaux/Widget} owner
   * @param {string} msg
   * @param {...*} args
   * @returns {Promise}
   */


  exports.logDomDetails = function (owner, msg) {
    for (var _len4 = arguments.length, args = new Array(_len4 > 2 ? _len4 - 2 : 0), _key4 = 2; _key4 < _len4; _key4++) {
      args[_key4 - 2] = arguments[_key4];
    }

    return ownerLog(DOM_DETAILS_LEVEL, owner, msg, args);
  };
  /**
   * @param {module:bajaux/Widget} widget
   * @returns {string}
   */


  exports.widgetName = function (widget) {
    if (!widget) {
      return '<no widget>';
    }

    var name = widget.constructor.name;
    var counter = widget[INSTANCE_COUNTER_SYMBOL];
    return name + (counter ? '#' + counter : '');
  };
  /**
   * Turns on finest-level logging even if not configured in the station
   * @private
   */


  exports.$trace = function () {
    spandrelLog.setLevel(FINEST);
  };

  function ownerLog(level, owner, msg, args) {
    return spandrelLog.log.apply(spandrelLog, [level, exports.widgetName(owner) + ': ' + msg].concat(_toConsumableArray(args)));
  }

  return exports;
});
