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

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/ChartWidget
 */
define(['baja!', 'log!nmodule.webChart.rc.ChartWidget', 'bajaux/commands/Command', 'bajaux/commands/ToggleCommand', 'bajaux/events', 'bajaux/mixin/subscriberMixIn', 'bajaux/mixin/responsiveMixIn', 'bajaux/Widget', 'd3', 'jquery', 'moment', 'Promise', 'underscore', 'nmodule/js/rc/asyncUtils/asyncUtils', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/ChartSettings', 'nmodule/webChart/rc/choiceUtil', 'nmodule/webChart/rc/command/addSeriesCommand', 'nmodule/webChart/rc/command/SettingsCommand', 'nmodule/webChart/rc/command/StopCommand', 'nmodule/webChart/rc/dragUtil', 'nmodule/webChart/rc/export/exportUtil', 'nmodule/webChart/rc/fe/StartEndTimeRangeEditor', 'nmodule/webChart/rc/grid/GridEditor', 'nmodule/webChart/rc/line/Line', 'nmodule/webChart/rc/line/TitleLayer', 'nmodule/webChart/rc/line/XAxisLayer', 'nmodule/webChart/rc/model/LineModel', 'nmodule/webChart/rc/model/samplingUtil', 'nmodule/webChart/rc/tab/Tab', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webEditors/rc/fe/baja/FrozenEnumEditor', 'nmodule/webEditors/rc/fe/feDialogs', 'nmodule/webEditors/rc/transform/ExportCommand', 'nmodule/history/rc/command/ShowWarningCommand', 'nmodule/webEditors/rc/wb/util/ICollectionSupport', 'css!nmodule/webChart/rc/ChartWidgetStyle', 'nmodule/webEditors/rc/fe/baseEditorSet', 'baja!' + 'baja:AbsTime,' + 'baja:AbsTimeRange,' + 'baja:Facets,' + 'baja:TimeRange,' + 'baja:Weekday,' + 'webChart:WebChartTimeRange,' + 'webChart:WebChartTimeRangeType,' + 'bql:DynamicTimeRangeType'], function (baja, log, Command, ToggleCommand, events, subscriberMixIn, responsiveMixIn, Widget, d3, $, moment, Promise, _, asyncUtils, chartEvents, ChartSettings, choiceUtil, makeAddSeriesCommand, SettingsCommand, StopCommand, dragUtil, exportUtil, StartEndTimeRangeEditor, //TODO: make BIJavaScript - do not remove until then - has side effects
GridEditor, Line, TitleLayer, XAxisLayer, LineModel, samplingUtil, Tab, webChartUtil, FrozenEnumEditor, feDialogs, ExportCommand, ShowWarningCommand, ICollectionSupport) {
  "use strict";

  var dateTimeUtil = require('bajaScript/baja/obj/dateTimeUtil');

  var logSevere = log.severe.bind(log);
  var doRequire = asyncUtils.doRequire;
  var SELECTION_EVENT = events.command.SELECTION_EVENT,
      samplingCommandTitle = "samplingCommand",
      dashboard = "dashboard",
      dashboardChartData = "dashboardChartData",
      HIDE_HEADER_WIDTH_PROPERTY = 'hideHeaderWidth',
      TITLE_VISIBILITY_PROPERTY = 'titleVisibility',
      LEGEND_VISIBILITY_PROPERTY = 'legendVisibility',
      TIMEZONE_PROPERTY = 'TimeZone',
      DEFAULT_RESPONSIVE_HEIGHT = 300,
      AUTO = 0,
      TIME_RANGE = 1,
      TODAY = 2,
      LAST_24HOURS = 3,
      YESTERDAY = 4,
      WEEK_TO_DATE = 5,
      LAST_WEEK = 6,
      LAST_7DAYS = 7,
      MONTH_TO_DATE = 8,
      LAST_MONTH = 9,
      YEAR_TO_DATE = 10,
      LAST_YEAR = 11,
      PERIODS_WITH_INTERPOLATE_TAIL = [AUTO, TODAY, TIME_RANGE, LAST_24HOURS, WEEK_TO_DATE, LAST_7DAYS, MONTH_TO_DATE, YEAR_TO_DATE];

  function widgetDefaults() {
    return {
      properties: {
        hideCommandBar: {
          value: true,
          hidden: true,
          "transient": true,
          readonly: true
        },
        exporting: {
          value: false,
          hidden: true,
          "transient": true,
          readonly: true
        },
        defaultOptions: {
          value: 'file:^charts/defaultOptions.chart',
          typeSpec: 'baja:Ord'
        },
        titleVisibility: {
          value: 'responsive',
          typeSpec: 'bajaux:DisplayVisibility'
        },
        legendVisibility: {
          value: 'responsive',
          typeSpec: 'bajaux:DisplayVisibility'
        },
        TimeZone: {
          value: baja.TimeZone.NULL.encodeToString(),
          typeSpec: 'baja:TimeZone'
        },
        hideHeaderWidth: 800,
        title: '',
        dashboard: {
          value: false,
          hidden: true,
          "transient": true,
          readonly: true
        },
        dashboardChartData: {
          value: '',
          readonly: true,
          hidden: true,
          dashboard: true
        }
      }
    };
  }
  /**
   * A ChartWidget is the abstract base-class for all bajaux D3 graphs.
   * It outlines the responsibilities of concrete chart types, such as
   * drawing chart elements, formatting data, positioning, etc. This class
   * eliminates the boilerplate for creating a D3 chart for its subclasses.
   *
   * @class
   * @alias module:nmodule/webChart/rc/ChartWidget
   * @extends module:bajaux/Widget
   * @implements module:nmodule/export/rc/TransformOperationProvider
   * @implements module:nmodule/export/rc/TransformOperationOptionsProvider
   * @mixes module:nmodule/webEditors/rc/wb/util/ICollectionSupport
   */


  var ChartWidget = function ChartWidget(params) {
    var that = this,
        options;
    Widget.call(that, {
      params: params,
      defaults: widgetDefaults()
    });
    ICollectionSupport(this);
    that.$initializeResponsiveMixin();
    that.options = options || {};
    that.options.margin = that.options.margin || {
      top: 30,
      right: 60,
      bottom: 45,
      left: 60
    };
    that.$title = null;
    that.margin = that.options.margin;
    that.$dataZoom = true;
    that.$timeZoom = false;
    that.$hasManualZoomed = false; //ChartWidget isn't visible until doLoad completes and the call stack is empty.

    that.$visible = false;
    that.$visibleHeightFix = false;
    that.$firstOrd = null;
    that.$lastOrd = null;
    that.$lastLoadValue = null;
    that.redrawTimeout = null;
    that.$widgetBar = new GridEditor(); //commands

    that.toggleAutoZoomCmd = null;
    that.toggleDeltaCommand = null;
    that.toggleTimeZoom = null;
    that.toggleInterpolateTailCommand = null;
    that.$timeRangeTab = null;
    that.$settingsCommand = null;
    that.$settings = new ChartSettings(this);
    that.$isFile = false;
    that.$isReadonly = false;
    that.$shownOverMaxCapacity = false;
    that.$lineGraph = new Line(that);
    subscriberMixIn(that);
  };

  ChartWidget.prototype = Object.create(Widget.prototype);
  ChartWidget.prototype.constructor = ChartWidget;
  /**
   * Returns the jQuery DOM element wrapped in a d3 object
   * @returns {d3} the DOM element
   */

  ChartWidget.prototype.jq3 = function () {
    return d3.select(this.jq()[0]);
  };
  /**
   * Return true if the widget is loaded from a Chart File
   */


  ChartWidget.prototype.isFile = function () {
    return this.$isFile;
  };
  /**
   * Returns whether or not is zooming on data as it comes in.
   */


  ChartWidget.prototype.isDataZoom = function () {
    return this.$dataZoom;
  };
  /**
   * Returns whether or not is zooming on data as it comes in.
   */


  ChartWidget.prototype.settingsCommand = function () {
    return this.$settingsCommand;
  };
  /**
   * Returns whether or not is zoomed on selected time.
   */


  ChartWidget.prototype.isTimeZoom = function () {
    return this.$timeZoom;
  };
  /**
   * Returns whether or not the user has zoomed
   * @return {boolean}
   */


  ChartWidget.prototype.hasManualZoomed = function () {
    return this.$hasManualZoomed;
  };
  /**
   * Returns whether or not the graph should display the interpolate tail for
   * series that support it.
   *
   * @returns {boolean}
   */


  ChartWidget.prototype.isInterpolateTail = function () {
    var command = this.toggleInterpolateTailCommand;
    return command.isEnabled() && command.isSelected();
  };
  /**
   * @returns {boolean} true if toggleInterpolateTailCommand is Enabled
   */


  ChartWidget.prototype.isInterpolateTailCommandEnabled = function () {
    return this.toggleInterpolateTailCommand.isEnabled();
  };
  /**
   * Accessor for Chart Model
   * @returns {module:nmodule/webChart/rc/model/BaseModel}
   */


  ChartWidget.prototype.model = function () {
    return this.$model;
  };
  /**
   * Accessor for Chart Graph
   * @returns {module:nmodule/webChart/rc/line/Line}
   */


  ChartWidget.prototype.graph = function () {
    return this.$lineGraph;
  };
  /**
   * Accessor for Widget bar
   * @returns {module:nmodule/webChart/rc/grid/GridEditor}
   */


  ChartWidget.prototype.widgetBar = function () {
    return this.$widgetBar;
  };

  ChartWidget.prototype.initCommands = function (element, clearCmd) {
    var that = this,
        widgetBar = that.$widgetBar,
        cmds,
        i,
        saveCmd,
        settings = that.$settings;
    widgetBar.addCommand(makeAddSeriesCommand(that), "addCommand");
    saveCmd = new Command({
      module: "webChart",
      lex: "webChart.SaveCommand",
      func: function func() {
        return that.save();
      }
    }); // A hack but as the chart widget implements its own widget bar, this is what we need to do.

    if (clearCmd) {
      widgetBar.addCommand(clearCmd, "clearCommand");
    }

    saveCmd.setEnabled(false);
    widgetBar.addCommand(saveCmd, "saveCommand");

    function jqSave() {
      return $(".js-tab-saveCommand", that.jq());
    }

    that.jq().on([events.ENABLE_EVENT, events.DISABLE_EVENT, events.READONLY_EVENT, events.WRITABLE_EVENT, events.INITIALIZE_EVENT, events.LOAD_EVENT, events.DESTROY_EVENT].join(' '), '*', false);
    that.jq().on(events.MODIFY_EVENT, function () {
      jqSave().addClass("save-modified"); //allow save to be clicked

      saveCmd.setEnabled(true);
    }).on(events.UNMODIFY_EVENT, function () {
      jqSave().removeClass("save-modified");
      saveCmd.setEnabled(false);
    }).on(events.LOAD_EVENT, function (e) {
      jqSave().parent().toggle(!that.$isReadonly && (that.$isFile || isOnDashboard(that)));
    }); //the new export dialog (work in progress)

    widgetBar.addCommand(new ExportCommand(that), "exportCommand");
    that.$timeRangeTab = new Tab({
      item: new FrozenEnumEditor(),
      title: "webChartTimeRange",
      value: function value() {
        return that.model().timeRange().getPeriod();
      },
      modified: function modified(period) {
        if (!period) {
          //canceled dialog, so reload current value into editor
          return that.$timeRangeTab.widget.load(that.model().timeRange().getPeriod());
        } // eslint-disable-next-line promise/avoid-new


        return new Promise(function (resolve, reject) {
          var working = that.model().timeRange().newCopy(),
              timezone = that.model().timeScale().getTimeZone();
          working.setPeriod(period);

          if (period.getOrdinal() === 1) {
            if (that.$lastStartEndTime) {
              //cache last specific time range entry
              working = that.$lastStartEndTime;
            }

            feDialogs.showFor({
              value: working,
              properties: {
                TimeZone: timezone.encodeToString()
              }
            }).then(function (diff) {
              if (!diff) {
                return that.$timeRangeTab.widget.load(that.model().timeRange().getPeriod());
              }

              working.setPeriod(period);
              that.$lastStartEndTime = working;
              resolve(working);
            })["catch"](function (error) {
              baja.error(error);
              that.jq().trigger(chartEvents.DISPLAY_ERROR, error);
            });
          } else {
            resolve(working);
          }
        }).then(function (timeRange) {
          that.model().timeRange(timeRange);
        }, baja.error);
      }
    });
    that.$widgetBar.addTab(that.$timeRangeTab); //prototype zoom lock toggleCommand

    that.toggleAutoZoomCmd = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleAutoZoomCmd",
      selected: true
    });
    that.toggleAutoZoomCmd.on(SELECTION_EVENT, function () {
      that.$dataZoom = that.toggleAutoZoomCmd.isSelected();

      if (that.isDataZoom()) {
        that.toggleTimeZoomCommand.setSelected(false);
        that.$hasManualZoomed = false;
        that.redrawSoon();
      }
    });
    widgetBar.addCommand(that.toggleAutoZoomCmd, "autoZoomCommand"); //prototype toggleTimeZoomCommand

    that.toggleTimeZoomCommand = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleTimeZoomCommand",
      selected: false
    });
    that.toggleTimeZoomCommand.on(SELECTION_EVENT, function () {
      that.$timeZoom = that.toggleTimeZoomCommand.isSelected();

      if (that.isTimeZoom()) {
        that.$hasManualZoomed = false;
        that.toggleAutoZoomCmd.setSelected(false);
        that.redrawSoon();
      }
    });
    widgetBar.addCommand(that.toggleTimeZoomCommand, "timeZoomCommand"); //toggleDeltaCommand

    that.toggleDeltaCommand = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleDeltaCommand",
      selected: that.model().isDelta()
    });
    that.toggleDeltaCommand.on(SELECTION_EVENT, function () {
      that.model().setDelta(that.toggleDeltaCommand.isSelected());
      that.reloadAll();
    });
    widgetBar.addCommand(that.toggleDeltaCommand, "deltaCommand"); //toggleSamplingCommand

    that.toggleSamplingCommand = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleSamplingCommand",
      selected: that.model().isSampling(),
      func: function func() {
        //toggle autoSampling
        var results = samplingUtil.calculateSeriesSamplingStats(that.model());

        if (results.allMaxFocusLength < webChartUtil.getMaxSamplingSize()) {
          that.model().setAutoSampling(!that.model().isAutoSampling());
          this.toggle();
          that.jq().trigger(chartEvents.SETTINGS_CHANGED);
        } else {
          that.graph().tipLayer().addTip({
            anchor: jqToggleSamplingCommand(that),
            key: "samplingTooHigh",
            args: [results.allMaxFocusLength, webChartUtil.getMaxSamplingSize()]
          });
        }
      }
    });
    that.toggleSamplingCommand.on(SELECTION_EVENT, function () {
      var oldSampling = that.model().isSampling();
      that.model().setSampling(that.toggleSamplingCommand.isSelected());

      if (oldSampling !== that.model().isSampling()) {
        that.redrawSoon();
      }
    });
    widgetBar.addCommand(that.toggleSamplingCommand, samplingCommandTitle); //toggleStatusColoring

    that.toggleStatusColoring = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleStatusColoring",
      selected: settings.getStatusColoring() === "on"
    });
    that.toggleStatusColoring.on(SELECTION_EVENT, function () {
      var value = that.toggleStatusColoring.isSelected() ? "on" : "off";
      settings.setStatusColoring(value);
      that.redrawSoon();
    });
    widgetBar.addCommand(that.toggleStatusColoring, "statusColoringCommand"); //interpolateTail command

    that.toggleInterpolateTailCommand = new ToggleCommand({
      module: "webChart",
      lex: "webChart.toggleInterpolateTailCommand",
      selected: false,
      enabled: false
    });

    function isInterpolateTailEnabled() {
      if (!that.model().isLive()) {
        return false;
      }

      var settings = that.settings();

      if (settings.isSampling() && settings.samplingType() !== "average") {
        return false;
      }

      var timeRange = that.$model.timeRange(),
          periodOrdinal = timeRange.getPeriod().getOrdinal();

      if (!PERIODS_WITH_INTERPOLATE_TAIL.includes(periodOrdinal)) {
        return false;
      }

      if (periodOrdinal === TIME_RANGE && timeRange.getEndFixed()) {
        return false;
      }

      return true;
    }

    function updateInterpolateTailCommand() {
      var isEnabled = isInterpolateTailEnabled();
      that.toggleInterpolateTailCommand.setEnabled(isEnabled);

      if (!isEnabled) {
        that.settings().setShowInterpolateTail(false);
        that.toggleInterpolateTailCommand.setSelected(false);
      } else {
        that.toggleInterpolateTailCommand.setSelected(that.settings().isShowInterpolateTail());
      }
    }

    widgetBar.addCommand(that.toggleInterpolateTailCommand, "interpolateTailCommand");
    element.on(chartEvents.MODEL_LIVE, function () {
      updateInterpolateTailCommand();
      redraw();
    }).on(chartEvents.MODEL_NOT_LIVE, updateInterpolateTailCommand).on(chartEvents.SETTINGS_CHANGED, updateInterpolateTailCommand).on(chartEvents.TIME_RANGE_CHANGED, updateInterpolateTailCommand);
    element.on(chartEvents.SETTINGS_CHANGED, updateInterpolatedTails);

    function updateInterpolatedTails() {
      that.model().seriesList().forEach(function (series) {
        series.updateInterpolatedTail();
      });
    }

    function redraw() {
      updateInterpolatedTails();
      that.redrawSoon();
    }

    that.$redrawIntervalHandle = null;
    var redrawIntervalMs = webChartUtil.getInterpolateTailRefreshInterval();

    function ensureRedrawScheduled() {
      if (!that.$redrawIntervalHandle) {
        that.$redrawIntervalHandle = setInterval(redraw, redrawIntervalMs);
      }
    }

    function cancelRedraw() {
      if (that.$redrawIntervalHandle) {
        clearInterval(that.$redrawIntervalHandle);
        that.$redrawIntervalHandle = null;
      }
    }

    that.toggleInterpolateTailCommand.on(SELECTION_EVENT, function () {
      var isSelected = that.toggleInterpolateTailCommand.isSelected();
      that.settings().setShowInterpolateTail(isSelected);
      that.redrawSoon();

      if (isSelected) {
        ensureRedrawScheduled();
      } else {
        cancelRedraw();
      }
    });
    cmds = that.$lineGraph.getCommands();

    for (i = 0; i < cmds.length; ++i) {
      widgetBar.addCommand(cmds[i], cmds[i].title);
    }

    that.$stopCommand = new StopCommand(that);
    widgetBar.addCommand(that.$stopCommand, "stopCommand");
    that.$showWarningCommand = new ShowWarningCommand(that);
    widgetBar.addCommand(that.$showWarningCommand, "showWarningCommand");

    function jqStop() {
      return $(".js-tab-stopCommand", that.jq());
    }

    function jqShowWarning() {
      return $(".js-tab-showWarningCommand", that.jq());
    } //clear the warnings array


    element.on([chartEvents.TIME_RANGE_CHANGED, chartEvents.REMOVE_DATA_REQUEST_EVENT].join(" "), function () {
      jqShowWarning().hide();
      that.$showWarningCommand.resetWarnings();

      for (var i = 0; i < that.model().seriesList().length; i++) {
        var series = that.model().seriesList()[i];

        if (series.warnings() && series.warnings().length) {
          series.warnings().length = 0;
        }
      }
    });
    element.on(chartEvents.MODEL_DATA_LOADED, function () {
      jqStop().parent().toggle(false); //stop is no longer required when not loading

      if (that.model().isLive()) {
        that.jq().trigger(chartEvents.MODEL_LIVE);
      } // create a map with the warning details
      // if coaleseable delimiter is provided, group the warnings together based
      // on title & msg so as to show the msgArgs grouped together, delimited
      // by the coaleseable delimiter,
      // else, show the warning messages as individual notifications per msgArgs.


      that.$showWarningCommand.resetWarnings();
      jqShowWarning().hide();

      for (var i = 0; i < that.model().seriesList().length; i++) {
        var series = that.model().seriesList()[i],
            warnings = series.warnings();

        if (warnings && warnings.length) {
          for (var w = 0; w < warnings.length; w++) {
            that.$showWarningCommand.setWarning(warnings[w]);
          }

          jqShowWarning().show();
        }
      }
    });
    element.on(chartEvents.MODEL_START_LOAD, function () {
      jqStop().removeClass("webchart-attention");
      jqStop().parent().toggle(true); //stop is no longer required when not loading

      that.$stopCommand.setEnabled(true);
    });
    element.on(chartEvents.MODEL_DATA_STOPPED, function () {
      jqStop().addClass("webchart-attention");
      that.jq().trigger(chartEvents.MODEL_NOT_LIVE);
    });
    element.on(chartEvents.MODEL_OVER_MAX_CAPACITY, function (event, arg) {
      //show user notification of trim even if the tooltip has been set to "never show"
      if (arg && that.model().timeRange().getPeriod().getOrdinal() !== 0) {
        jqToggleSamplingCommand(that).addClass("webchart-attention");
      } else {
        jqToggleSamplingCommand(that).removeClass("webchart-attention");
      }
    });
    var spacerWidget = new Widget();

    spacerWidget.doInitialize = function (jq) {
      jq.parent().width("100%");
    };

    widgetBar.addTab(new Tab({
      item: spacerWidget,
      title: "spacer"
    }));
    that.$settingsCommand = new SettingsCommand(that);
    widgetBar.addCommand(that.$settingsCommand, "settingsCommand");
  };
  /**
   * Make the JSON for the Chart Widget so it can be saved.
   *
   * @param {Object|baja.Component} [optionInfo]
   * @param {baja.DynamicEnum} [optionInfo.ordType=0] defaults to ordinal 0 with tag 'absolute' the other option is 'relative',
   * @param {String} [optionInfo.baseOrd] if ordType is relative, the base used for relativizing the series ords.
    * @returns {Object} The chart's JSON.
   */


  ChartWidget.prototype.makeJson = function (optionInfo) {
    // NOTE TO DEVELOPERS - Starting in Niagara 4.9, there is new special
    // handling for dealing with importing saved web chart files from one
    // Niagara station to another (see niagaraDriver-rt's
    // BChartFileImportHandler class). In particular, since the chart files
    // are saved to JSON below, and since the shorthand history ID form is
    // used when saving history ORDs in the chart file (see the model's save
    // to JSON below), upon importing the saved chart file to another
    // NiagaraStation, we need to convert such history IDs to their absolute
    // form so that they can resolve on the remote station. Therefore
    // BChartFileImportHandler's code is dependent upon the saved JSON chart
    // format implemented below. If changes are ever made to this code to add
    // more shorthand history IDs/ORDs to the saved chart JSON, please also
    // make corresponding updates to BChartFileImportHandler.
    var that = this,
        chartJson = {
      version: "1.0.0.0",
      type: "webChart",
      settings: {}
    };
    optionInfo = optionInfo instanceof baja.Component ? optionInfo.getSlots().toValueMap() : optionInfo;
    that.model().saveToJson(chartJson, optionInfo);
    that.settings().saveToJson(chartJson);

    if (that.title()) {
      chartJson.title = that.title();
    }

    return chartJson;
  };
  /**
   * Make an export of the current Chart Widget so it can be saved.
   *
   * @param {String} type The Chart Widget export Type.
   * @param {Object} [optionInfo]
   * @param {baja.DynamicEnum} [optionInfo.ordType=0] defaults to ordinal 0 with tag 'absolute' the other option is relative.
   * @param {String} [optionInfo.baseOrd=''] if ordType is relative, the base used for relativizing the series ords.
   * @returns {Object} The chart's JSON.
   */


  ChartWidget.prototype.makeExport = function (type, optionInfo) {
    var that = this;

    if (type === "csv") {
      return exportUtil.exportToCsv(this, optionInfo);
    } else if (type === "chart") {
      return JSON.stringify(that.makeJson(optionInfo));
    } else {
      throw new Error("Export type unknown:" + type);
    }
  };
  /**
   * Load the Chart JSON into the Chart Widget.
   *
   * @private
   * @inner
   *
   * @param  {module:nmodule/webChart/rc/ChartWidget} widget The Chart Widget instance.
   * @param  {Object} chartJson The Chart JSON to load.
   */


  function loadSettingsFromJson(widget, chartJson) {
    if (chartJson) {
      var settings = widget.settings();
      settings.loadFromJson(chartJson);
      widget.model().loadFromJson(chartJson, settings);

      if (chartJson.title) {
        widget.title(chartJson.title);
      }
    }
  }

  ChartWidget.prototype.settings = function () {
    return this.$settings;
  };
  /**
   * Ensure that the height is set based on the max available height. This is required in some browsers since 100%
   * height for svg is not the best for displaying and sometimes printing.
   */


  ChartWidget.prototype.heightFix = function () {
    //no needed when not visible
    if (!this.$visible) {
      return;
    }

    var that = this,
        jq = that.jq(),
        svgOuterDiv = jq.children(".svg-outer-div"),
        svg = svgOuterDiv.children("svg"),
        svgHeight,
        svgWidth,
        titleVisibility = that.properties().getValue(TITLE_VISIBILITY_PROPERTY),
        legendVisibility = that.properties().getValue(LEGEND_VISIBILITY_PROPERTY);
    var isPushedDown = titleVisibility === "show" || legendVisibility === "show" || !(jq.hasClass("ux-webchart-short") && jq.hasClass("ux-webchart-thin") && legendVisibility !== "hide" && titleVisibility !== "hide");
    that.options.margin.top = isPushedDown ? 30 : 10;
    jq.find(".Line").attr('transform', 'translate(' + that.margin.left + ',' + that.margin.top + ')');

    if (!svg.length) {
      return;
    }

    svg.hide();
    svgHeight = svgOuterDiv.height();
    svgWidth = svgOuterDiv.width();
    svg.show(); //avoid jquery bug with raw dom manipulation

    svg[0].setAttribute("viewBox", "0 0 " + svgWidth + " " + svgHeight);
  };
  /**
   * Create the DOM elements that your chart will use to represent data.
   *
   * Do not set any values that vary with data here, such as color or size/position.
   *
   * Subclasses should asynchronously override this method.
   *
   * @param element
   * @return {Promise}
   */


  ChartWidget.prototype.doInitialize = function (element) {
    var that = this,
        jq3 = that.jq3(),
        settings = that.$settings,
        widgetBarPromise,
        linePromise,
        graph = that.$lineGraph;
    settings.initialize();
    element.addClass("ChartWidgetOuter");
    element.addClass("ux-fullscreen");
    return Promise.all([baja.TimeZoneDatabase.get(), webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getStationsTimeZoneId")]).then(function (_ref) {
      var _ref2 = _slicedToArray(_ref, 2),
          database = _ref2[0],
          stationTimezoneId = _ref2[1];

      that.$model = new LineModel(element, settings);
      that.$model.timeScale().setTimeZoneDatabase(database);
      that.$model.timeScale().setFallbackTimeZone(database.getTimeZone(stationTimezoneId));
      that.$model.timeScale().setTimeZoneOverride(function () {
        var timezoneString = that.properties().getValue(TIMEZONE_PROPERTY);

        if (timezoneString !== baja.TimeZone.NULL.encodeToString()) {
          return database.getTimeZone(baja.TimeZone.DEFAULT.decodeFromString(timezoneString).getId());
        } else {
          return null;
        }
      });
      var timezone = that.$model.timeScale().getTimeZone(),
          nowOffset = dateTimeUtil.getUtcOffsetInTimeZone(moment(), timezone),
          now = webChartUtil.getAbsTime(moment().utcOffset(nowOffset)),
          yesterdayOffset = dateTimeUtil.getUtcOffsetInTimeZone(moment().subtract(1, 'days'), timezone),
          yesterday = webChartUtil.getAbsTime(moment().utcOffset(yesterdayOffset).subtract(1, 'days')),
          startTime = baja.AbsTime.make({
        date: yesterday.getDate(),
        time: yesterday.getTime(),
        timeZone: timezone
      }),
          endTime = baja.AbsTime.make({
        date: now.getDate(),
        time: now.getTime(),
        timeZone: timezone
      });
      that.$model.timeRange(baja.$('webChart:WebChartTimeRange', {
        period: baja.$('webChart:WebChartTimeRangeType', 2),
        //today for best performance
        startTime: startTime,
        endTime: endTime
      }));
      var widgetBarDiv = $("<div class='widgetBar ux-fixed-header'><div>");
      element.append(widgetBarDiv);
      var svgOuterDiv = jq3.append('div').attr('class', "ux-body svg-outer-div");
      svgOuterDiv.append('svg').attr('top', 0).attr('left', 0).attr('width', '100%').attr('height', '100%').attr('class', 'ChartWidget');
      widgetBarPromise = resolveClearCmd(that).then(function (clearCmd) {
        that.initCommands(element, clearCmd);
        return that.$widgetBar.initialize(widgetBarDiv);
      }).then(function () {
        // TODO: move to view load?
        return that.$widgetBar.load().then(function () {
          //allow re-selection of timeRange option
          var timeRangeSelect = that.jq().find('.js-tab-webChartTimeRange > select');
          timeRangeSelect.focus(function () {
            var index = timeRangeSelect.prop("selectedIndex");

            if (parseInt(index) === 1) {
              timeRangeSelect.attr("originalSelectedIndex", index);
              timeRangeSelect.prop("selectedIndex", "-1");
            }
          }).on("change", function () {
            timeRangeSelect.attr("originalSelectedIndex", null);
          }).blur(function () {
            var original = timeRangeSelect.attr("originalSelectedIndex");

            if (original !== null && original !== undefined) {
              timeRangeSelect.prop("selectedIndex", original);
            }
          });
        });
      });
      that.chart = jq3.select('svg.ChartWidget').append('g').attr('transform', 'translate(' + that.margin.left + ',' + that.margin.top + ')');
      dragUtil.armDrop(element, function () {
        return that.drop.apply(that, arguments);
      });
      linePromise = graph.initialize(that.chart);
      element.on(chartEvents.REMOVE_DATA_REQUEST_EVENT, function () {
        that.removeDomData();
      });
      element.on(chartEvents.REDRAW_REQUEST_EVENT, function (event, timeout) {
        that.redrawSoon(timeout);
      });
      element.on([chartEvents.SERIES_ENABLED, chartEvents.SERIES_DISABLED].join(" "), function () {
        that.redrawSoon();
      });
      element.on(chartEvents.VALUE_SCALE_CHANGED, function (event) {
        that.redrawSoon();
      });
      element.on(chartEvents.GRAPH_DATA_REQUEST_EVENT, function () {
        that.graphData(that.model().seriesList());
      });
      element.on(chartEvents.DISPLAY_ERROR, function (event, error) {
        require(['dialogs'], function (dialogs) {
          var title = webChartUtil.lex.get("webChart.errorTitle"),
              message = webChartUtil.safe(error);
          message = webChartUtil.nl2br(message);
          dialogs.showOk({
            title: title,
            content: message
          });
        });
      });
      element.on(chartEvents.SETTINGS_CHANGED, function () {
        var settings = that.settings(); //update model based on changes

        that.model().loadFromJson(null, settings); //update toggle commands that point to values controlled by settings

        that.toggleStatusColoring.setSelected(settings.getStatusColoring() === "on"); //update toggle commands that point to values controlled by settings

        that.toggleSamplingCommand.setSelected(settings.isSampling()); // Set the toggle delta command state based upon the settings

        if (_.isFunction(that.model().isDelta)) {
          that.toggleDeltaCommand.setSelected(that.model().isDelta());
        }
      });
      element.on(events.PROPERTY_CHANGED, function (event, widget, propName) {
        if (propName === "title") {
          that.redrawSoon();
        } else if (propName === TITLE_VISIBILITY_PROPERTY || propName === LEGEND_VISIBILITY_PROPERTY || propName === HIDE_HEADER_WIDTH_PROPERTY) {
          that.layout();
        } else if (propName === TIMEZONE_PROPERTY) {
          return that.reloadAll();
        }
      });
      element.on(chartEvents.DELTA_CHANGED, function () {
        if (_.isFunction(that.model().isDelta)) {
          that.toggleDeltaCommand.setSelected(that.model().isDelta());
        }
      });
      element.on(chartEvents.TIME_RANGE_CHANGED, function () {
        var reloadPromise;

        if (!that.model().isInitialized()) {
          reloadPromise = Promise.resolve(); //no reloading required since its already in process
        } else {
          reloadPromise = that.reloadAll(true);
        }

        return reloadPromise.then(function () {
          var timeRange = that.model().timeRange(),
              timezone = that.model().timeScale().getTimeZone(),
              switchToTimeZoom = that.model().seriesList().length > 0;

          if (timeRange.getPeriod().getOrdinal() > 0) {
            that.toggleTimeZoomCommand.setSelected(switchToTimeZoom);
            that.toggleAutoZoomCmd.setSelected(!switchToTimeZoom);

            if (timeRange.getPeriod().getOrdinal() > 1) {
              //change the live based on the period selection
              var results = webChartUtil.getStartAndEndDateFromPeriod(timeRange.getPeriod(), timezone);
              that.model().setLive(results.live);
            } else {
              //auto and time range will have live=true for now
              that.model().setLive(!timeRange.getEndFixed());
            }
          } else if (timeRange.getPeriod().getOrdinal() === 0) {
            that.toggleTimeZoomCommand.setSelected(false);
            that.toggleAutoZoomCmd.setSelected(true);
            that.model().setLive(true);
          } //ensure that.$timeRangeTab is up to date


          that.$timeRangeTab.load(timeRange.getPeriod());
        });
      });
      return Promise.all([widgetBarPromise, linePromise]) // required to ensure height calculations are correct during load
      .then(function () {
        element.on(chartEvents.SERIES_ADDED + " " + chartEvents.SETTINGS_CHANGED + " " + chartEvents.TIME_RANGE_CHANGED + " " + chartEvents.FILE_MODIFY_EVENT + " " + chartEvents.TITLE_CHANGED, function (event, eventArgument) {
          if (eventArgument !== chartEvents.IGNORE_MODIFY && that.model().isInitialized() && (that.$isFile || isOnDashboard(that))) {
            // Only set this as modified if the chart was loaded from a file.
            that.setModified(true);
          }
        });
      });
    });
  };
  /**
   * @returns {Promise.<Array.<module:nmodule/export/rc/TransformOperation>>}
   */


  ChartWidget.prototype.getTransformOperations = function () {
    var _this = this;

    return Promise.all([TitleLayer.resolveTitleInfo(this), doRequire('nmodule/webChart/rc/transform/chartWidgetTransformOperationProvider')]).then(function (_ref3) {
      var _ref4 = _slicedToArray(_ref3, 2),
          titleInfo = _ref4[0],
          chartWidgetTransformOperationProvider = _ref4[1];

      return chartWidgetTransformOperationProvider.getTransformOperations(_this, titleInfo.title);
    });
  };
  /**
   * Adjust the `delta` and `timeRange` to match the existing Widget if those properties exist in the config.
   *
   * @param {baja.Component} config
   * @returns {baja.Component}
   * @since Niagara 4.14
   */


  ChartWidget.prototype.updateTransformOptions = function (config) {
    if (baja.hasType(config.get('delta'), 'baja:Boolean')) {
      config.setFlags({
        slot: 'delta',
        flags: config.getFlags('delta') | baja.Flags.TRANSIENT
      });
      config.set({
        slot: 'delta',
        value: this.model().isDelta()
      });
    }

    if (baja.hasType(config.get('timeRange'), 'bql:DynamicTimeRange')) {
      config.setFlags({
        slot: 'timeRange',
        flags: config.getFlags('timeRange') | baja.Flags.TRANSIENT
      });
      var periodStr = this.model().timeRange().getPeriod().encodeToString();
      var timeRange;

      if (periodStr !== "timeRange" && periodStr !== "auto") {
        timeRange = baja.$("bql:DynamicTimeRange", periodStr);
      } else {
        var startTime = this.model().timeRange().getStartTime().encodeToString();
        var endTime = this.model().timeRange().getEndTime().encodeToString();

        if (periodStr === "auto") {
          var _XAxisLayer$domain = XAxisLayer.domain(this.graph(), this.model().seriesList(), this.graph().xAxisLayer().$getFallbackDomain()),
              _XAxisLayer$domain2 = _slicedToArray(_XAxisLayer$domain, 2),
              min = _XAxisLayer$domain2[0],
              max = _XAxisLayer$domain2[1];

          startTime = webChartUtil.getAbsTime(min);
          endTime = webChartUtil.getAbsTime(max);
        }

        timeRange = baja.$("bql:DynamicTimeRange", "timeRange:startTime=" + startTime.encodeToString() + ";endTime=" + endTime.encodeToString());
      }

      config.set({
        slot: 'timeRange',
        value: timeRange
      });
    }

    return config;
  };

  ChartWidget.prototype.doDestroy = function () {
    var that = this,
        redrawTimeout = that.redrawTimeout,
        redrawIntervalHandle = that.$redrawIntervalHandle;
    that.jq().removeClass("ChartWidgetOuter");
    that.jq().removeClass("ux-fullscreen");

    if (redrawTimeout) {
      clearTimeout(redrawTimeout);
    }

    if (redrawIntervalHandle) {
      clearInterval(redrawIntervalHandle);
    }

    that.toggleAutoZoomCmd.off();
    that.toggleTimeZoomCommand.off();
    that.toggleDeltaCommand.off();
    that.toggleSamplingCommand.off();
    that.toggleStatusColoring.off();
    that.toggleInterpolateTailCommand.off();
    return Promise.all([that.$widgetBar.destroy(), that.$lineGraph.destroy(), that.model().destroy()]);
  };

  ChartWidget.prototype.doLayout = function () {
    var that = this;

    if (that.$visible) {
      that.heightFix();
    }

    webChartUtil.trace("layout");
    that.$hideAllPopupsOnPdfExport();
    that.redrawSoon();
  };
  /**
   * Hides all popups when webchart is exported as a pdf from a px-view or as a report from ReportService.
   * @private
   */


  ChartWidget.prototype.$hideAllPopupsOnPdfExport = function () {
    var isExporting = this.properties().getValue('exporting'),
        chartWidgetContainer = this.jq().parent(),
        classToHide = 'chartWidget-exportAsPdf-hidePopup';
    chartWidgetContainer.toggleClass(classToHide, isExporting);
  };

  ChartWidget.prototype.toDisplayName = function () {
    var that = this;
    return TitleLayer.resolveTitleInfo(that).then(function (titleInfo) {
      if (titleInfo.title) {
        return titleInfo.title;
      } //If title is not present, then fallback to default implementation


      return Widget.prototype.toDisplayName.apply(that, arguments);
    });
  };
  /**
   * Produces data formatted for this graph for use in setting up
   * the graph. This data may or may not be immediately replaced by
   * the load method.
   * It can be used to help create the DOM elements require for the graph,
   * or it can be used to give the user something to look at while the
   * real data for the graph loads.
   *
   * @return {Array} Data placed into the graph for setup before the real data loads.
   */


  ChartWidget.prototype.initialData = function () {
    return [];
  };
  /**
   * Resolve the data to something useful.
   *
   * @param {String|baja.Ord} data The data to resolve.
   *
   * @returns {Promise}
   */


  ChartWidget.prototype.resolve = function (data, resolveParams) {
    var that = this,
        dataAsString = String(data);
    that.$lastOrd = dataAsString;

    if (!that.$firstOrd) {
      that.$firstOrd = dataAsString;
    } // Check whether we need to skip ORD resolution in that particular case.


    if (webChartUtil.isFileUri(dataAsString)) {
      return Promise.resolve(dataAsString);
    } else {
      return Widget.prototype.resolve.apply(that, arguments);
    }
  };
  /**
   * Load a WebChart. Note that a ChartWidget will continue to load its data beyond the doLoad promise unless
   * params.fullLoad is true. This gives the user a chance to cancel the load if its taking too long.
   *
   * @param  {baja.Value} value The value for the item we are attempting to load
   * @param {Object} [params] Optional params object passed to `load()`
   * @param {Boolean} [params.fullLoad] if true, delays doLoad promise until all data is available.
   * @param {Boolean} [params.exporting] if true, this notifies the widget that it's being used in an exporting operation.
   * In this case, the widget will ensure all of its data is loaded before it finishes loading.
   * @return {Promise}
   */


  ChartWidget.prototype.doLoad = function (value, params) {
    if (value === null) {
      return Promise.reject(new Error("Value cannot be null"));
    }

    var that = this,
        ord = that.$lastOrd,
        isFile = that.$isFile = webChartUtil.isFileOrd(ord),
        fullLoad = params ? params.fullLoad : that.properties().getValue('exporting');

    try {
      if (value && that.$lastLoadValue && value.equals && value.equals(that.$lastLoadValue)) {
        return Promise.resolve();
      }
    } catch (err) {
      //since values can be different types, equals may through an Error that we usually don't care about
      webChartUtil.trace("Cannot compare previous load value to new load value:", err);
    }

    that.$lastLoadValue = value; // If this is a dashboard then initialize from the dashboard.

    var dashboarded = hasDashboardData(that);

    if (dashboarded) {
      return initializeFromDashboard(that);
    }

    return Promise["try"](function () {
      if (isFile) {
        return Promise.all([webChartUtil.ajax(webChartUtil.getFileUri(ord), {
          type: "GET",
          contentType: "application/json",
          dataType: "json"
        }), webChartUtil.resolvePermissions(ord)]).then(function (_ref5) {
          var _ref6 = _slicedToArray(_ref5, 2),
              chartJson = _ref6[0],
              permissions = _ref6[1];

          loadSettingsFromJson(that, chartJson);

          if (!permissions.hasAdminWrite()) {
            that.$isReadonly = true;
          }

          return chartJson;
        });
      } else {
        return webChartUtil.resolveSettings(that.properties().getValue("defaultOptions")).then(function (chartJson) {
          if (chartJson) {
            //don't use any scale settings in the defaultOptions because we don't use the series from the defaultOptions chart file
            if (chartJson && chartJson.settings && chartJson.settings.scales) {
              chartJson.settings.scales = null;
            }

            loadSettingsFromJson(that, chartJson);
          }

          return value;
        });
      }
    }).then(function addSeries(val) {
      return that.model().addSeries(that.getSubscriber(), {
        ord: ord,
        value: val
      });
    }).then(function load() {
      return that.model().load(that.getSubscriber(), !fullLoad);
    });
  };
  /**
   * Called when the Chart Widget needs to save.
   */


  ChartWidget.prototype.doSave = function () {
    var that = this; // If this chart is being loaded from a Dashboard then just save to the Dashboard
    // and not the file.

    if (isOnDashboard(that)) {
      saveDashboardData(that);
    } else if (that.$firstOrd && that.$isFile) {
      // Make an AJAX call for when the web chart is saved.
      return Promise.resolve(webChartUtil.ajax("/webChart/file/save/" + webChartUtil.getFilePath(that.$firstOrd), {
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify(that.makeJson(this.$getSaveOptions()))
      }));
    }
  };
  /**
   * Remove all the current data for this graph
   */


  ChartWidget.prototype.removeDomData = function () {
    var that = this;
    that.chart.selectAll('.lineContainer .dataSet').data([]);
  };
  /**
   * Update any elements whose values depend on data, such as
   * point positions, colors or text that will not change on redraw.
   *
   * Note: This section should not include updates to any
   * position or size _attributes_. These should be calculated
   * in the redraw() function.
   *
   * Subclasses should override this method.
   *
   * @param formattedData
   * @return {Promise}
   */


  ChartWidget.prototype.graphData = function (formattedData) {
    var that = this;
    return that.$lineGraph.graphData(formattedData).then(function () {
      //ensure that an immediate redraw happens after a graphData
      that.$visible = true;

      if (!that.$visibleHeightFix) {
        that.heightFix();
        that.$visibleHeightFix = true;
      }

      return that.redraw();
    });
  };
  /**
   * Formats the Niagara History datasets into a dataset formatted for a specific type of chart.
   *
   * @param  {Array} value An array containing Niagara History data.
   * @return {Array} An array containing data formatted for this graph.
   */


  ChartWidget.prototype.formatData = function (value) {
    return value;
  };
  /**
   * Respond to a manual zoom event by ensuring auto zoom commands are deselected and the graph is redrawn.
   */


  ChartWidget.prototype.manualZoom = function () {
    var that = this;
    that.$hasManualZoomed = true;

    if (that.isDataZoom()) {
      that.toggleAutoZoomCmd.setSelected(false);
    }

    if (that.isTimeZoom()) {
      that.toggleTimeZoomCommand.setSelected(false);
    }

    that.redrawSoon();
  };
  /**
   * Draw the graph sometime soon. This function coalesces multiple redrawSoon calls into one redraw.
   */


  ChartWidget.prototype.redrawSoon = function (timeout) {
    var that = this;

    if (!timeout) {
      timeout = 100;
    }

    if (that.redrawTimeout === null) {
      that.redrawTimeout = window.setTimeout(function () {
        that.redraw();
      }, timeout); //coalesce  additional redraw requests
    }
  };
  /**
   * Redraws the elements of the graph.
   * Any attribute modifications concerning layout /must/ be
   * described here.
   * This method should be called when the size or position of
   * the chart's DOM container changes, such as on resizing the window.
   * @return {Promise}
   */


  ChartWidget.prototype.redraw = function () {
    var that = this;

    if (!that.$visible || !that.jq()) {
      //content is not visible, don't redraw yet
      return Promise.resolve();
    }

    if (that.redrawTimeout) {
      window.clearTimeout(that.redrawTimeout);
    }

    that.redrawTimeout = null;
    return that.$lineGraph.redraw().then(function () {
      //auto means show me the latest 250K points, so warning will only be displayed when time range is non-auto
      if (that.model().timeRange().getPeriod().getOrdinal() !== 0 && !that.$shownOverMaxCapacity && that.model().isOverMaxCapacity()) {
        that.$shownOverMaxCapacity = true; //only show this message once per refresh

        that.graph().tipLayer().addTip({
          anchor: jqToggleSamplingCommand(that),
          key: "tooManyPoints",
          args: [webChartUtil.getMaxSeriesCapacity()]
        });
      }
    });
  };
  /**
   * Calculates the dimensions of the chart.
   *
   * @param  {boolean} [margins] When true, subtracts the margins to give the dimensions of the plotting area
   *                          When false, gives the dimensions of the chart's containing DOM element.
   * @return {Object} An object containing width and height properties of the chart.
   */


  ChartWidget.prototype.dimensions = function (margins) {
    var that = this;

    if (typeof margins === 'undefined') {
      margins = true;
    }

    var dim = that.jq().children("div.svg-outer-div");
    var results = {
      width: dim.width() - (margins ? that.margin.left + that.margin.right : 0.0) - 1,
      height: dim.height() - (margins ? that.margin.top + that.margin.bottom : 0.0) - 1
    };

    if (results.width < 0) {
      webChartUtil.trace("ChartWidget.dimensions.width() too small:" + results.width);
      results.width = 0;
    }

    if (results.height < 0) {
      webChartUtil.trace("ChartWidget.dimensions.height() too small:" + results.height);
      results.height = 0;
    }

    return results;
  };
  /**
   * Get the current line colors based on the Users stored options and defaults.
   *
   * @returns {Array}
   */


  ChartWidget.prototype.getLineColors = function () {
    var that = this,
        settings = that.$settings,
        i,
        seriesList = that.model().seriesList(),
        colors = settings.getDefaultLineColors(),
        storedColor;

    for (i = 0; i < seriesList.length; i++) {
      storedColor = settings.seriesColor(seriesList[i]);

      if (storedColor) {
        colors[i] = storedColor.encodeToString();
      }
    }

    return colors;
  };
  /**
   * Get the theme colors based on the current theme.
   * @returns {Array.<String>}
   */


  ChartWidget.prototype.getThemeColors = function () {
    try {
      var that = this,
          i,
          value,
          valueElem,
          results = [],
          themeElem = that.chart.insert('span').attr("class", "theme");

      for (i = 0; i < that.model().maxSeriesListLength(); i++) {
        valueElem = themeElem.append("span").attr("class", "themeColor");
        value = valueElem.style("color");
        results.push(value);
      }

      themeElem.remove();
      return results;
    } catch (err) {
      webChartUtil.trace("Cannot obtain default theme colors:", err);
      return webChartUtil.defaultLineColors;
    }
  };
  /**
   * Handle a drop event for new data.
   *
   * @param {event} event
   * @param value for the Drag operation
   * @param type
   * @returns {boolean}
   */


  ChartWidget.prototype.drop = function (event, value, type) {
    var that = this,
        model = that.model(),
        ords = dragUtil.getOrdsFromDrag(value, type);
    model.addSeries(that.getSubscriber(), ords).then(function () {
      return model.load(that.getSubscriber());
    })["catch"](logSevere); //TODO: if no acceptable add, maybe all default behavior

    return ords.length === 0; //return false to stop default behavior
  };
  /**
   * Reload all of the data in the Chart Widget.*
   * @param {boolean} [completeInfoOnly] If true, return promise after BaseModel.loadInfo is complete
   * @return {*}
   */


  ChartWidget.prototype.reloadAll = function (completeInfoOnly) {
    var that = this;
    return that.model().reloadAll(that.getSubscriber(), completeInfoOnly);
  };
  /**
   * Title for a saved Chart Widget.
   *
   * @param title
   * @returns {null|*}
   */


  ChartWidget.prototype.title = function (title) {
    if (title !== undefined && title !== this.$title) {
      this.$title = title;
      this.jq().trigger(chartEvents.TITLE_CHANGED, title);
    }

    return this.$title;
  };

  ChartWidget.prototype.$getSaveOptions = function () {
    if (!this.model().isRelative()) {
      return;
    }

    return {
      ordType: choiceUtil.makeDynamicEnum(["absolute", "relative"], "relative"),
      baseOrd: exportUtil.getBaseOrd()
    };
  };
  /**
   * Handles the display criteria for a part of the ChartWidget that has bajaux properties associated with it.
   * This sets up a function that the responsiveMixin uses when layout is called.
   * @param {string} propertyName the name of the property
   * @returns {module:bajaux/mixin/responsiveMixIn~ResponsiveCallback}
   *
   */


  ChartWidget.prototype.$getPropertyDisplayCondition = function (propertyName) {
    var that = this;
    return function (info) {
      var prop = that.properties().getValue(propertyName),
          responsiveWidth = that.properties().getValue(HIDE_HEADER_WIDTH_PROPERTY),
          responsiveHeight = DEFAULT_RESPONSIVE_HEIGHT;

      if (prop === "show") {
        return false;
      } else if (prop === "hide") {
        return true;
      } else {
        return info.width <= responsiveWidth || info.height <= responsiveHeight;
      }
    };
  };
  /**
   * Configures the responsiveMixin with all needed classes and conditions.
   */


  ChartWidget.prototype.$initializeResponsiveMixin = function () {
    var that = this,
        conditions = {
      'ux-webchart-thin': function uxWebchartThin(info) {
        var hideHeaderWidth = that.properties().getValue(HIDE_HEADER_WIDTH_PROPERTY);
        return info.width <= hideHeaderWidth;
      },
      'ux-webchart-short': {
        maxHeight: DEFAULT_RESPONSIVE_HEIGHT
      }
    };
    conditions["ux-webchart-hide-title"] = that.$getPropertyDisplayCondition(TITLE_VISIBILITY_PROPERTY);
    conditions["ux-webchart-hide-legend"] = that.$getPropertyDisplayCondition(LEGEND_VISIBILITY_PROPERTY);
    responsiveMixIn(that, conditions);
  }; ////////////////////////////////////////////////////////////////
  // Dashboards
  ////////////////////////////////////////////////////////////////

  /**
   * Saves the data away to the dashboard.
   *
   * @inner
   * @private
   *
   * @param widget The chart widget instance to save the data from.
   */


  function saveDashboardData(widget) {
    widget.properties().setValue(dashboardChartData, JSON.stringify(widget.makeJson()));
  }
  /**
   * Return true if the web chart is being loaded from a dashboard.
   *
   * @inner
   * @private
   *
   * @param widget The widget instance.
   * @returns {Boolean} Returns true if the chart widget is on a Dashboard.
   */


  function isOnDashboard(widget) {
    return widget.properties().getValue(dashboard, false);
  }
  /**
   * Return true if the widget is on a dashboard and some data is available to load.
   *
   * @param widget The chart widget instance.
   * @returns {Boolean} Returns true if the chart widget is on a dashboard and there's some
   * data to load.
   */


  function hasDashboardData(widget) {
    return isOnDashboard(widget) && !!widget.properties().getValue(dashboardChartData, "");
  }
  /**
   * Attempt to resolve the dashboard clear command. If the widget
   * isn't on a dashboard, the returned promise will resolve with no
   * value.
   *
   * @inner
   * @private
   *
   * @param widget The widget instance.
   * @returns {Promise.<module:bajaux/commands/Command|undefined>} A
   * promise that will resolve to a clear command.
   */


  function resolveClearCmd(widget) {
    // If being used on a Dashboard then resolve the dashboard
    // resources before initializing the widget bar.
    if (isOnDashboard(widget)) {
      return doRequire('nmodule/dashboard/rc/dashboard').then(function (db) {
        var clearCmd = db.makeClearCmd(widget); // Add to the widget and the widget bar.

        widget.getCommandGroup().add(clearCmd);
        return clearCmd;
      });
    }

    return Promise.resolve();
  }
  /**
   * Obtain the jq for the toggleSamplingCommand
   * @param {ChartWidget} widget
   * @returns {jQuery}
   */


  function jqToggleSamplingCommand(widget) {
    var result = $(".js-tab-" + samplingCommandTitle, widget.jq());
    return result;
  }
  /**
   * Initialize the Widget from any Dashboard data that's available.
   *
   * @inner
   * @private
   *
   * @param widget The widget instance to check for dashboard data.
   * @returns {Promise} A promise that's resolved once the dashboard data has been
   * resolved.
   */


  function initializeFromDashboard(widget) {
    var chartJson = JSON.parse(widget.properties().getValue(dashboardChartData));
    loadSettingsFromJson(widget, chartJson);
    return widget.model().addSeries(widget.getSubscriber(), {
      ord: "file:^dashboard.chart",
      // Fictitious. This never gets saved.
      value: chartJson
    }).then(function load() {
      return widget.model().load(widget.getSubscriber(), true);
    });
  }

  ChartWidget.TIME_PERIODS = {
    AUTO: AUTO,
    TIME_RANGE: TIME_RANGE,
    TODAY: TODAY,
    LAST_24HOURS: LAST_24HOURS,
    YESTERDAY: YESTERDAY,
    WEEK_TO_DATE: YEAR_TO_DATE,
    LAST_WEEK: LAST_WEEK,
    LAST_7DAYS: LAST_7DAYS,
    MONTH_TO_DATE: MONTH_TO_DATE,
    LAST_MONTH: LAST_MONTH,
    YEAR_TO_DATE: YEAR_TO_DATE,
    LAST_YEAR: LAST_YEAR
  };
  return ChartWidget;
});
