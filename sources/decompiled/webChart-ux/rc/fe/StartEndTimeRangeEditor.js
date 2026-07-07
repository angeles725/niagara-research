function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/fe/StartEndTimeRangeEditor
 */
define(['baja!', 'jquery', 'Promise', 'nmodule/webEditors/rc/fe/baja/ComplexCompositeEditor', 'nmodule/webEditors/rc/fe/fe', 'bajaux/events', 'bajaux/Properties', 'nmodule/webEditors/rc/fe/config/ComplexCompositeBuilder', 'hbs!nmodule/webChart/rc/fe/startEndTimeRangeEditorStructure', 'nmodule/webEditors/rc/fe/baja/AbsTimeRangeEditor', 'nmodule/webEditors/rc/fe/baseEditorSet', 'css!nmodule/webChart/rc/ChartWidgetStyle'], function (baja, $, Promise, ComplexCompositeEditor, fe, events, Properties, ComplexCompositeBuilder, startEndTimeRangeEditorStructure) {
  "use strict";

  var TIMEZONE_PROPERTY = "TimeZone";
  /**
   * A StartEndTimeRangeEditor is the editor for webChart:WebChartTimeRangEditor.
   *
   * @class
   * @alias module:nmodule/webChart/rc/fe/StartEndTimeRangeEditor
   * @extends module:nmodule/webEditors/rc/fe/baja/ComplexCompositeEditor
   */

  var StartEndTimeRangeEditor = function StartEndTimeRangeEditor(params) {
    var that = this;
    ComplexCompositeEditor.apply(this, arguments);
    that.validators().add(function (diff) {
      var value = that.value().newCopy();
      return diff.apply(value).then(startBeforeEnd);
    });
  };

  StartEndTimeRangeEditor.prototype = Object.create(ComplexCompositeEditor.prototype);
  StartEndTimeRangeEditor.prototype.constructor = StartEndTimeRangeEditor;
  /**
   * Return all the properties of the WebChartTimeRange
   * @returns {string[]}
   */

  StartEndTimeRangeEditor.prototype.getSlotFilter = function () {
    return ['startFixed', 'startTime', 'endFixed', 'endTime'];
  };

  StartEndTimeRangeEditor.prototype.$buildSubEditorDom = function (key) {
    var jq = this.jq(),
        dom = $('.js-' + key, jq);
    return dom;
  };

  var MODIFY_EVENT = events.MODIFY_EVENT;
  /**
   * initialize the dom and register all event handlers
   * @param {jQuery} dom
   */

  StartEndTimeRangeEditor.prototype.doInitialize = function (dom) {
    var that = this;
    dom.on(MODIFY_EVENT, '.slot-startFixed, .slot-endFixed', function () {
      that.checkTimeEditors();
      that.setModified(true); //ensure that re-validation occurs when these slots change
    });
    var div = $("<div></div>");
    div.html(startEndTimeRangeEditorStructure());
    that.jq().append(div); //doing this last allows squelching of the premature modify event

    return ComplexCompositeEditor.prototype.doInitialize.apply(this, arguments);
  };
  /**
   * Load the current value and setup any dependant slot interactions
   * @returns {Promise|*}
   * @param value {baja.Value}
   */


  StartEndTimeRangeEditor.prototype.doLoad = function (value) {
    var that = this;
    return Promise.resolve(ComplexCompositeEditor.prototype.doLoad.apply(this, arguments)).then(function () {
      return that.checkTimeEditors();
    });
  };
  /**
   * Ensure that the value is valid time range
   * @param {webChart:WebChartTimeRange} value
   * @inner
   */


  function startBeforeEnd(value) {
    var startTime = value.get('startTime'),
        endTime = value.get('endTime'),
        startFixed = value.get('startFixed'),
        endFixed = value.get('endFixed');

    if (startFixed && endFixed && startTime.getJsDate().getTime() > endTime.getJsDate().getTime()) {
      //TODO: if we ever start showing the validation errors to the user, we should lexicon this
      throw new Error('start time not before end time');
    }
  }
  /**
   * Return the Start Time element.
   * @private
   * @returns {jquery}
   */


  StartEndTimeRangeEditor.prototype.$getStartTimeEditor = function () {
    return this.jq().find('.slot-startTime').data('widget');
  };
  /**
   * Return the End Time  element.
   * @private
   * @returns {jquery}
   */


  StartEndTimeRangeEditor.prototype.$getEndTimeEditor = function () {
    return this.jq().find('.slot-endTime').data('widget');
  };
  /**
   * Return the Start Fixed element.
   * @private
   * @returns {jquery}
   */


  StartEndTimeRangeEditor.prototype.$getStartFixedEditor = function () {
    return this.jq().find('.slot-startFixed').data('widget');
  };
  /**
   * Return the End Fixed element.
   * @private
   * @returns {jquery}
   */


  StartEndTimeRangeEditor.prototype.$getEndFixedEditor = function () {
    return this.jq().find('.slot-endFixed').data('widget');
  };
  /**
   * Setup any interdependency between the checkboxes and the other editors
   * @returns {Promise}
   */


  StartEndTimeRangeEditor.prototype.checkTimeEditors = function () {
    var that = this,
        startTimeEditor = that.$getStartTimeEditor(),
        endTimeEditor = that.$getEndTimeEditor(),
        startFixedEditor = that.$getStartFixedEditor(),
        endFixedEditor = that.$getEndFixedEditor();
    return Promise.all([startFixedEditor.read(), endFixedEditor.read()]).then(function (_ref) {
      var _ref2 = _slicedToArray(_ref, 2),
          startEnabled = _ref2[0],
          endEnabled = _ref2[1];

      return Promise.all([startTimeEditor.setEnabled(startEnabled), endTimeEditor.setEnabled(endEnabled)]);
    });
  };

  StartEndTimeRangeEditor.prototype.makeBuilder = function () {
    var that = this,
        builder = ComplexCompositeEditor.prototype.makeBuilder.call(that);

    builder.getConfigFor = function (key) {
      return ComplexCompositeBuilder.prototype.getConfigFor.call(builder, key).then(function (config) {
        if (key === "startTime" || key === "endTime") {
          config.properties = Properties.extend(config.properties, {
            TimeZone: that.properties().getValue(TIMEZONE_PROPERTY)
          });
        }

        return config;
      });
    };

    return builder;
  };
  /**
   * register this editor automatically if the file is included
   * @inner
   */


  function register() {
    fe.register('webChart:WebChartTimeRange', 'nmodule/webChart/rc/fe/StartEndTimeRangeEditor');
  }

  register();
  return StartEndTimeRangeEditor;
});
