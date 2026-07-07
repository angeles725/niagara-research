/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/ChartSettings
 */
define(['Promise', 'baja!', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/choiceUtil', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/chartEvents', 'baja!' + 'gx:Color,' + 'baja:Status'], function (Promise, baja, modelUtil, choiceUtil, webChartUtil, chartEvents) {
  "use strict";

  var DATA_GAP_TAGS = ["dotted", "gap", "solid"]; //Obsolete properties of the chart live here and will be excluded from the settings.

  var OBSOLETE_PROPERTIES = ['backgroundAreaColor'];

  function mapOldDataGapTagToNew(tag) {
    if (tag === "yes") {
      return "gap";
    }

    if (tag === "no") {
      return "solid";
    }

    return tag;
  }
  /**
   * Utility method to filter out obsolete properties.
   * This will enable to support older web charts saved as files and in dashboard.
   *
   * @private
   * @param {Object} chartSettings
   * @param {Array<Object>} chartSettings.s
   * @returns {Object}
   */


  function filterObsoleteChartSettings(chartSettings) {
    //Filter out obsolete properties from chart settings.
    if (chartSettings && chartSettings.s) {
      chartSettings.s = chartSettings.s.filter(function (property) {
        return !OBSOLETE_PROPERTIES.includes(property.n);
      });
    }

    return chartSettings;
  }
  /**
   * Contain all the persistable settings for a chart
   * @class
   * @alias module:nmodule/webChart/rc/ChartSettings
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   */


  var ChartSettings = function ChartSettings(widget) {
    var that = this;
    that.$widget = widget;
    that.$seriesListSettings = new baja.Component();
    that.$valueScaleListSettings = new baja.Component();
    that.$chartSettings = new baja.Component();
    that.$layersSettings = new baja.Component();
    that.$samplingSettings = new baja.Component();
    that.$defaultLineColors = null;
  };

  ChartSettings.prototype.reset = function () {
    var that = this,
        widget = that.$widget;
    that.$seriesListSettings = new baja.Component();
    that.$valueScaleListSettings = new baja.Component();
    that.$chartSettings = new baja.Component();
    that.$layersSettings = new baja.Component();
    that.$samplingSettings = new baja.Component();
    that.initialize();
    that.load(); //recreate series settings as the defaults

    var seriesList = widget.model().seriesList();

    for (var i = 0; i < seriesList.length; i++) {
      that.seriesSettings(seriesList[i], {});
    } //recreate series settings as the defaults


    var valueScales = widget.model().valueScales();

    for (i = 0; i < valueScales.length; i++) {
      that.valueScaleSettings(valueScales[i], {});
    }
  };

  ChartSettings.prototype.initialize = function () {
    var that = this,
        comp = that.$layersSettings; //initialize layer Settings

    choiceUtil.addEnumChoice(comp, "dataPopup", ["off", "on"], "on");
    choiceUtil.addEnumChoice(comp, "dataMouseover", ["off", "on"], "on");
    choiceUtil.addEnumChoice(comp, "statusColoring", ["off", "on"], "off");
    choiceUtil.setDisplayNames(comp); //initialize chartSettings

    comp = that.$chartSettings;
    choiceUtil.addEnumChoice(comp, "yAxisOrient", ["left", "right"], "right");
    choiceUtil.addEnumChoice(comp, "dataZoom", ["primary", "all"], "primary");
    choiceUtil.addBooleanChoice(comp, "showGrid", true);
    choiceUtil.addEnumChoice(comp, "chartCursor", ["crosshair", "none"], "crosshair");
    choiceUtil.addEnumChoice(comp, "facetsLimitMode", ["off", "inclusive", "locked"], "off");
    choiceUtil.addEnumChoice(comp, "showStartTrendGaps", DATA_GAP_TAGS, "dotted");
    choiceUtil.addEnumChoice(comp, "showDataGaps", DATA_GAP_TAGS, "solid");
    choiceUtil.addBooleanChoice(comp, "showInterpolateTail", false);
    choiceUtil.addBooleanChoice(comp, "delta", false);
    choiceUtil.setDisplayNames(comp); //initialize samplingSettings

    comp = that.$samplingSettings;
    choiceUtil.addBooleanChoice(comp, "autoSampling", true);
    choiceUtil.addBooleanChoice(comp, "sampling", true);
    choiceUtil.addEnumChoice(comp, "samplingType", ["average", "min", "max", "sum"], "average");
    choiceUtil.addChoice(comp, "desiredSamplingPeriod", baja.RelTime.DEFAULT, baja.Facets.make({
      uxFieldEditor: 'nmodule/webChart/rc/fe/SamplingPeriodEditor',
      min: baja.RelTime.DEFAULT,
      showDay: true,
      enablePopOut: false
    }));
    choiceUtil.enforceChoice(comp, "sampleSize", baja.$("baja:Integer", webChartUtil.getDefaultAutoSamplingSize()), baja.Facets.make(['precision', 'min', 'max'], [0, 1, webChartUtil.getMaxSamplingSize()]));
    choiceUtil.setDisplayNames(comp); //do not initialize seriesListSettings or valueScaleSettings until load
  };

  ChartSettings.prototype.load = function () {
    var that = this,
        widget = that.$widget,
        comp = that.$seriesListSettings,
        chartSettings = that.$chartSettings,
        reorderArray = [],
        displayArray = [],
        seriesList = widget.model().seriesList(); //seriesListSettings initialized

    for (var i = 0; i < seriesList.length; i++) {
      that.seriesSettings(seriesList[i]);
      reorderArray.push(that.getSeriesSettingName(seriesList[i]));
      displayArray.push(that.getSeriesSettingDisplayName(seriesList[i]));
    }

    choiceUtil.setDisplayNames(comp, reorderArray, displayArray); //ensure ordering of series

    webChartUtil.safeReorder(reorderArray, comp); //valueScaleSettings initialized

    reorderArray = [];
    displayArray = [];
    comp = that.$valueScaleListSettings;
    var valueScales = widget.model().valueScales();

    for (i = 0; i < valueScales.length; i++) {
      that.valueScaleSettings(valueScales[i]);
      reorderArray.push(that.getValueScaleName(valueScales[i]));
      displayArray.push(that.getValueScaleDisplayName(valueScales[i]));
    }

    choiceUtil.setDisplayNames(comp, reorderArray, displayArray); //ensure ordering of series

    webChartUtil.safeReorder(reorderArray, comp);
    that.loadSamplingSettings(); //NCCB-7562: JavaFx Browser missing cursor: none

    if (webChartUtil.isJavaFx()) {
      choiceUtil.hideChoice(chartSettings, "chartCursor");
    } else {
      choiceUtil.allowChoice(chartSettings, "chartCursor");
    } //ShowInterpolateTail choice is only enabled when the widget's ShowInterpolateTail command is enabled.


    choiceUtil.setReadonly(chartSettings, "showInterpolateTail", !widget.isInterpolateTailCommandEnabled());
  };

  ChartSettings.prototype.getDefaultLineColors = function () {
    var that = this,
        widget = that.$widget;

    if (!that.$defaultLineColors) {
      that.$defaultLineColors = widget.getThemeColors();
    }

    return that.$defaultLineColors.slice(0);
  };

  ChartSettings.prototype.saveToJson = function (chartJson) {
    var that = this;

    if (that.$chartSettings) {
      chartJson.settings.chart = webChartUtil.encodeValueForStorage(that.$chartSettings);
    }

    if (that.$layersSettings) {
      chartJson.settings.layers = webChartUtil.encodeValueForStorage(that.$layersSettings);
    }

    if (that.$samplingSettings) {
      chartJson.settings.sampling = webChartUtil.encodeValueForStorage(that.$samplingSettings);
    }

    if (that.$valueScaleListSettings) {
      chartJson.settings.scales = webChartUtil.encodeValueForStorage(that.$valueScaleListSettings);
    }
  };

  ChartSettings.prototype.loadFromJson = function (chartJson) {
    var that = this,
        widget = that.$widget;

    if (chartJson.settings.chart) {
      //Filter out the 'backgroundAreaColor' and any other obsolete chart settings.
      var chartSettings = filterObsoleteChartSettings(chartJson.settings.chart);
      that.$chartSettings = baja.bson.decodeValue(chartSettings);
    } // This supports old chart files that have an old enum with only yes / no as options for
    // showDataGaps and showStartTrendGaps


    var showDataGaps = mapOldDataGapTagToNew(that.getShowDataGaps());
    choiceUtil.setEnumChoice(that.$chartSettings, "showDataGaps", DATA_GAP_TAGS, showDataGaps);
    var showStartTrendGaps = mapOldDataGapTagToNew(that.getShowStartTrendGaps());
    choiceUtil.setEnumChoice(that.$chartSettings, "showStartTrendGaps", DATA_GAP_TAGS, showStartTrendGaps);

    if (chartJson.settings.layers) {
      that.$layersSettings = baja.bson.decodeValue(chartJson.settings.layers);
    }

    if (chartJson.settings.sampling) {
      that.$samplingSettings = baja.bson.decodeValue(chartJson.settings.sampling);
    }

    if (chartJson.settings.scales) {
      that.$valueScaleListSettings = baja.bson.decodeValue(chartJson.settings.scales);
    }

    that.initialize();
    that.load();
    widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
  };

  ChartSettings.prototype.settingsChanged = function () {
    this.$widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
  };
  /**
   * Accessor for seriesListSettings
   * @returns {baja.Component}
   */


  ChartSettings.prototype.seriesListSettings = function () {
    return this.$seriesListSettings;
  };
  /**
   * Accessor for the Component that contains valueScaleSettings
   * @returns {baja.Component}
   */


  ChartSettings.prototype.valueScaleListSettings = function () {
    return this.$valueScaleListSettings;
  };
  /**
   * Accessor for ChartSettings
   * @returns {baja.Component}
   */


  ChartSettings.prototype.chartSettings = function () {
    return this.$chartSettings;
  };
  /**
   * Accessor for Layer Settings
   * @returns {baja.Component}
   */


  ChartSettings.prototype.layersSettings = function () {
    return this.$layersSettings;
  };
  /**
   * Accessor for Sampling Settings
   * @returns {baja.Component}
   */


  ChartSettings.prototype.samplingSettings = function () {
    return this.$samplingSettings;
  };
  /**
   * Update Sampling options
   * @returns {baja.Component}
   */


  ChartSettings.prototype.loadSamplingSettings = function (comp) {
    var that = this,
        widget = that.$widget,
        model = widget.model(),
        samplingPeriod = model.samplingPeriod();

    if (!comp) {
      comp = that.samplingSettings();
    }

    choiceUtil.setReadonly(comp, "sampling", choiceUtil.getChoice(comp, "autoSampling"));
    choiceUtil.setStat(comp, "availableDataPoints", model.getAvailableDataPoints(), baja.Facets.make(['precision'], [0]));

    if (samplingPeriod) {
      choiceUtil.setStat(comp, "samplingPeriod", samplingPeriod);
    } else {
      choiceUtil.hideChoice(comp, "samplingPeriod");
    }

    choiceUtil.setDisplayNames(comp);
    return comp;
  };
  /**
   * Should this ChartWidget show the grid or not? Default is true
   * @returns {boolean}
   */


  ChartSettings.prototype.hasShowGrid = function () {
    return choiceUtil.getChoice(this.$chartSettings, "showGrid");
  };
  /**
   *  Given the current chart settings, should we orient the Y axis on the left or right (default=right)
   * @returns {String}
   */


  ChartSettings.prototype.getYOrient = function () {
    return choiceUtil.getChoice(this.$chartSettings, "yAxisOrient");
  };
  /**
   * Given the current chart settings, should we provide a data popup when the chart is clicked.
   * @returns {String}
   */


  ChartSettings.prototype.getDataPopupLayout = function () {
    return choiceUtil.getChoice(this.$layersSettings, "dataPopup");
  };
  /**
   * Given the current chart settings, should we provide a data popup when the chart is clicked.
   * @returns {String}
   */


  ChartSettings.prototype.getDataMouseOver = function () {
    return choiceUtil.getChoice(this.$layersSettings, "dataMouseover");
  };
  /**
   * Given the current chart settings, should we provide the status coloring
   * @returns {String}
   */


  ChartSettings.prototype.getStatusColoring = function () {
    return choiceUtil.getChoice(this.$layersSettings, "statusColoring");
  };
  /**
   * We can set the status coloring
   * @param {String} tag
   */


  ChartSettings.prototype.setStatusColoring = function (tag) {
    var that = this,
        widget = that.$widget,
        modified = choiceUtil.setEnumChoice(this.$layersSettings, "statusColoring", ["off", "on"], tag);

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
    }
  };
  /**
   * Access chartCursor
   * @returns {String}
   */


  ChartSettings.prototype.getChartCursor = function () {
    if (webChartUtil.isJavaFx()) {
      return 'crosshair'; //none does not work in JavaFX
    }

    return choiceUtil.getChoice(this.$chartSettings, "chartCursor");
  };
  /**
   * Access dataZoom Scope
   * @returns {String}
   */


  ChartSettings.prototype.getDataZoomScope = function () {
    return choiceUtil.getChoice(this.$chartSettings, "dataZoom");
  };
  /**
   * Is Sampling
   * @param {Number} index
   * @returns {boolean}
   */


  ChartSettings.prototype.isSampling = function () {
    var that = this,
        saved = that.$samplingSettings.get("sampling");

    if (saved !== null) {
      return saved;
    }

    return true; //TODO: is this the best default?
  };
  /**
   * Set Sampling
   * @param {Boolean} sampling
   * @param {String} [eventArgument] Optional Event argument for passing to the SETTINGS_CHANGED event
   */


  ChartSettings.prototype.setSampling = function (sampling, eventArgument) {
    var that = this,
        widget = that.$widget,
        modified = choiceUtil.setBooleanChoice(that.$samplingSettings, "sampling", sampling);

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED, eventArgument);
    }
  };
  /**
   * Get the current color for a line index.
   * @param {Number} index
   * @returns {boolean}
   */


  ChartSettings.prototype.isAutoSampling = function () {
    var that = this,
        saved = that.$samplingSettings.get("autoSampling");

    if (saved !== null) {
      return saved;
    }

    return true; //TODO: is this the best default?
  };
  /**
   * We can set the status coloring
   * @param {String} tag
   */


  ChartSettings.prototype.setAutoSampling = function (autoSampling) {
    var that = this,
        widget = that.$widget,
        modified = choiceUtil.setBooleanChoice(that.$samplingSettings, "autoSampling", autoSampling);

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
    }
  };
  /**
   * Determines whether or not interpolate tail is on.
   *
   * @returns {boolean}
   */


  ChartSettings.prototype.isShowInterpolateTail = function () {
    var that = this,
        saved = that.$chartSettings.get("showInterpolateTail");

    if (saved !== null) {
      return saved;
    }

    return false;
  };
  /**
   * Sets whether the interpolate tail for the graph is on.
   *
   * @param {boolean} isInterpolateTail
   */


  ChartSettings.prototype.setShowInterpolateTail = function (isInterpolateTail) {
    var that = this,
        widget = that.$widget,
        modified = choiceUtil.setBooleanChoice(that.$chartSettings, "showInterpolateTail", isInterpolateTail);

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
    }
  };
  /**
   * Determines whether or not the charts delta setting is on.
   *
   * @returns {boolean}
   */


  ChartSettings.prototype.isDelta = function () {
    var that = this,
        saved = that.$chartSettings.get("delta");

    if (saved !== null) {
      return saved;
    }

    return false;
  };
  /**
   * Sets whether or not the chars delta setting is on.
   *
   * @param {boolean} isDelta
   */


  ChartSettings.prototype.setDelta = function (isDelta) {
    var that = this,
        widget = that.$widget,
        modified = choiceUtil.setBooleanChoice(that.$chartSettings, "delta", isDelta);

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
    }
  };
  /**
   * Access sampleSize
   * @param {String} tag
   */


  ChartSettings.prototype.sampleSize = function (sampleSize) {
    var that = this,
        widget = that.$widget,
        modified = false;

    if (sampleSize !== undefined) {
      modified = choiceUtil.setChoice(that.$samplingSettings, "sampleSize", sampleSize);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }

      return sampleSize;
    } else {
      var result = choiceUtil.getChoice(that.$samplingSettings, "sampleSize");
      return result;
    }
  };
  /**
   * Access samplingType
   * @param {String} tag
   */


  ChartSettings.prototype.samplingType = function (samplingType) {
    var that = this,
        widget = that.$widget,
        modified = false;

    if (samplingType !== undefined) {
      modified = choiceUtil.setChoice(that.$samplingSettings, "samplingType", samplingType);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }

      return samplingType;
    } else {
      return choiceUtil.getChoice(that.$samplingSettings, "samplingType");
    }
  };
  /**
   * Access desiredSamplingPeriod
   * @param {baja.RelTime} desiredSamplingPeriod
   */


  ChartSettings.prototype.desiredSamplingPeriod = function (desiredSamplingPeriod) {
    var that = this,
        widget = that.$widget,
        modified = false;

    if (desiredSamplingPeriod !== undefined) {
      modified = choiceUtil.setChoice(that.$samplingSettings, "desiredSamplingPeriod", desiredSamplingPeriod);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }

      return desiredSamplingPeriod;
    } else {
      return choiceUtil.getChoice(that.$samplingSettings, "desiredSamplingPeriod");
    }
  };
  /**
   * Series Color Accessor.
   *
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @param {baja.DefaultSimple} [color]
   * @returns {baja.DefaultSimple}
   */


  ChartSettings.prototype.seriesColor = function (series, color) {
    var that = this,
        modified = false,
        widget = that.$widget,
        seriesSettings = this.seriesSettings(series);

    if (color !== undefined) {
      modified = choiceUtil.setChoice(seriesSettings, "color", color);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }

      return color;
    }

    return choiceUtil.getChoice(seriesSettings, "color", baja.$('gx:Color', 'null'));
  };
  /**
   * Determine if a color is modified from the original set of colors.
   * @param {Number} index
   * @returns {String}
   */


  ChartSettings.prototype.seriesChartType = function (series, chartType) {
    var that = this,
        modified = false,
        widget = that.$widget,
        seriesSettings = this.seriesSettings(series);

    if (chartType !== undefined) {
      modified = choiceUtil.setChoice(seriesSettings, "chartType", chartType);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }
    }

    return choiceUtil.getChoice(seriesSettings, "chartType");
  };
  /**
   * Determines whether or not the given series is enabled.
   *
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @param {boolean} [isEnabled] If specified, the series state will be
   * modified to reflect this enabled state.
   * @returns {boolean}
   */


  ChartSettings.prototype.seriesEnabled = function (series, isEnabled) {
    var that = this,
        modified = false,
        widget = that.$widget,
        seriesSettings = this.seriesSettings(series);

    if (isEnabled !== undefined) {
      modified = choiceUtil.setChoice(seriesSettings, "enabled", isEnabled);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }

      return isEnabled;
    }

    return choiceUtil.getChoice(seriesSettings, "enabled");
  };
  /**
   * Access to the current value of the facetsLimitMode. The choices consist of the following:
   *  off - do not look at 'min' and 'max' facets for determining min and max of a `ValueScale`.
   *  This is the default option. Even when set to off, 'chartMin' and 'chartMax' facets can be used to determine the 'inclusive'
   *  min and max of the 'Value Scale'.
   *
   *  inclusive - include the 'min' and 'max' facets with the data to determine the min and max of the `ValueScale`.
   *
   *  locked - ignore the data and use the 'min', 'max' as specified by the facets.
   *
   *  In all options, 'chartMin', 'chartMax' facet keys can be used as a higher priority substitute to min and max.
   *  Even if the facetLimitMode is 'off' this can be overridden for specific series if a facet of 'chartLimitMode' is
   *  supplied with the corresponding values of 'inclusive' or 'locked'.
   *
   * @returns {String}
   * @since Niagara 4.6U1
   */


  ChartSettings.prototype.getFacetsLimitMode = function () {
    return choiceUtil.getChoice(this.$chartSettings, "facetsLimitMode");
  };
  /**
   * Access to the current value of the showStartTrendGaps. The choices consist of the following:
   * gap - if there is a start trend flag, then a gap will be shown when there is previous data on the chart.
   * solid - no gaps will be shown due to the start flag. A solid line is drawn instead.
   * dotted -  gaps will be displayed using a dotted line. (This is the default)
   *
   * @returns {String}
   * @since Niagara 4.6U1
   */


  ChartSettings.prototype.getShowStartTrendGaps = function () {
    return choiceUtil.getChoice(this.$chartSettings, "showStartTrendGaps");
  };
  /**
   * Access to the current value of the showDataGaps. A value is considered a data gap
   * when one of the following conditions is met:
   *   1) value is non-finite; this includes +inf, -inf, null, and nan.
   *   2) trend flags have the hidden flag set
   *   3) status flags have that the null flag set
   *
   * The display will adjust based upon the following choices:
   * gap - if there is a skip point, then a gap will be shown when there is previous data on the chart.
   * solid - no gaps will be shown. A solid line is drawn instead. (This is the default)
   * dotted -  gaps will be displayed using a dotted line.
   *
   * @returns {String}
   * @since Niagara 4.6U1
   */


  ChartSettings.prototype.getShowDataGaps = function () {
    return choiceUtil.getChoice(this.$chartSettings, "showDataGaps");
  };
  /**
   * Determine if a scale is locked.
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @returns {boolean}
   */


  ChartSettings.prototype.isScaleLocked = function (baseScale) {
    var that = this,
        settings = that.valueScaleSettings(baseScale);
    var result = choiceUtil.getChoice(settings, "locked", false);
    return result;
  };
  /**
   * set Scale Locked
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @param {Boolean} locked
   * @param {Array.<number>} [minMax]
   */


  ChartSettings.prototype.setScaleLocked = function (baseScale, locked, minMax) {
    var that = this,
        widget = that.$widget,
        settings = that.valueScaleSettings(baseScale),
        modified = choiceUtil.setBooleanChoice(settings, "locked", locked);

    if (minMax) {
      if (settings.get("min") !== minMax[0] || settings.get("max") !== minMax[1]) {
        settings.set({
          slot: "min",
          value: minMax[0]
        });
        settings.set({
          slot: "max",
          value: minMax[1]
        });
        modified = true;
      }
    }

    if (modified) {
      widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
    }
  };
  /**
   * scale Display Accessor
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @param {String} [scaleDisplayName]
   */


  ChartSettings.prototype.scaleDisplayName = function (baseScale, scaleDisplayName) {
    var that = this,
        modified = false,
        widget = that.$widget,
        settings = that.valueScaleSettings(baseScale);

    if (scaleDisplayName !== undefined) {
      modified = choiceUtil.setChoice(settings, "displayName", scaleDisplayName);

      if (modified) {
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
      }
    }

    return choiceUtil.getChoice(settings, "displayName");
  };
  /**
   * Remove a series that no longer requires settings
   * @param series
   */


  ChartSettings.prototype.removeSeriesSettings = function (series) {
    this.$seriesListSettings.remove(this.getSeriesSettingName(series));
  };
  /**
   * Remove a valueScale that no longer requires settings
   * @param {module:nmodule/webChart/rc/model/BaseScale} valueScale
   */


  ChartSettings.prototype.resetValueScaleSettings = function () {
    this.$valueScaleListSettings = new baja.Component();
  };
  /**
   * Remove a valueScale that no longer requires settings
   * @param {module:nmodule/webChart/rc/model/BaseScale} valueScale
   */


  ChartSettings.prototype.removeValueScaleSettings = function (valueScale) {
    this.$valueScaleListSettings.remove(this.getValueScaleName(valueScale));
  };
  /**
   * Get the SeriesName
   * @param series
   * @returns {*|String}
   */


  ChartSettings.prototype.getSeriesSettingName = function (series) {
    return baja.SlotPath.escape(series.ord());
  };

  ChartSettings.prototype.getSeriesSettingDisplayName = function (series) {
    return series.shortDisplayName();
  };

  ChartSettings.prototype.seriesSettings = function (series, seriesParams) {
    var that = this,
        slotName = that.getSeriesSettingName(series, seriesParams),
        comp = that.$seriesListSettings,
        seriesComp = choiceUtil.getChoice(comp, slotName),
        defaultColor = seriesParams ? seriesParams.color : null,
        defaultChartType = seriesParams ? seriesParams.chartType : null,
        defaultEnabled = seriesParams ? seriesParams.enabled : null;

    if (!seriesComp) {
      seriesComp = new baja.Component();
    }

    if (!choiceUtil.getChoice(seriesComp, "color") || defaultColor) {
      if (!defaultColor) {
        defaultColor = that.getUniqueColor();
      } else {
        defaultColor = baja.bson.decodeValue(defaultColor);
      }

      if (defaultColor && defaultColor.encodeToString() !== 'null') {
        //default color based on color index if its not set
        choiceUtil.setChoice(seriesComp, "color", defaultColor);
      }
    }

    if (!choiceUtil.getChoice(seriesComp, "chartType")) {
      if (defaultChartType) {
        defaultChartType = baja.bson.decodeValue(defaultChartType);
      } else if (series.recordType()) {
        //the earliest we know the default Chart Type
        defaultChartType = that.getDefaultChartType(series);
      } else {
        defaultChartType = ""; //still unknown until loaded
      }

      choiceUtil.setChoice(seriesComp, "chartType", defaultChartType, baja.Facets.make({
        uxFieldEditor: 'nmodule/webChart/rc/fe/series/ChartTypeEditor',
        enablePopOut: false
      }));
    }

    if (choiceUtil.getChoice(seriesComp, "enabled") === undefined) {
      if (defaultEnabled !== null && defaultEnabled !== undefined) {
        defaultEnabled = baja.bson.decodeValue(defaultEnabled);
      } else {
        defaultEnabled = true;
      }

      choiceUtil.setChoice(seriesComp, "enabled", defaultEnabled);
    }

    if (seriesParams) {
      //only add the setting back in when seriesParams is present
      choiceUtil.setChoice(comp, slotName, seriesComp, baja.Facets.make({
        uxFieldEditor: 'nmodule/webChart/rc/fe/series/SeriesEditor',
        enablePopOut: false
      }));
    }

    return seriesComp;
  };
  /**
   * Get the ScaleName
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @returns {*|String}
   */


  ChartSettings.prototype.getValueScaleName = function (baseScale) {
    return baja.SlotPath.escape(baseScale.uniqueName());
  };
  /**
   * Get the Scale Display Name
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @returns {*|String}
   */


  ChartSettings.prototype.getValueScaleDisplayName = function (baseScale) {
    return baseScale.displayName();
  };
  /**
   * Get the Component representing the value Scale Settings
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @returns {baja.Component}
   *
   */


  ChartSettings.prototype.valueScaleSettings = function (baseScale) {
    var that = this,
        slotName = that.getValueScaleName(baseScale),
        comp = that.$valueScaleListSettings,
        valueScaleComp = choiceUtil.getChoice(comp, slotName),
        setDefaults = !valueScaleComp,
        defaultDisplayName = baseScale.uniqueName(),
        defaultLocked = false,
        discrete = modelUtil.hasDiscrete(baseScale.seriesList()),
        facets = baja.Facets.make(['units'], [baseScale.units()]);

    if (!valueScaleComp) {
      valueScaleComp = new baja.Component();
      choiceUtil.setChoice(comp, slotName, valueScaleComp);
    }

    if (setDefaults) {
      choiceUtil.setChoice(valueScaleComp, "displayName", defaultDisplayName);
      choiceUtil.setChoice(valueScaleComp, "locked", defaultLocked);
      choiceUtil.setChoice(valueScaleComp, "max", 0, facets, discrete ? baja.Flags.HIDDEN : 0);
      choiceUtil.setChoice(valueScaleComp, "min", 0, facets, discrete ? baja.Flags.HIDDEN : 0);
    }

    return valueScaleComp;
  };
  /**
   * Prepare the Scale Settings for display to the User with additional information. This resolves to a promise.
   * @param {module:nmodule/webChart/rc/model/BaseScale} baseScale
   * @return {Promise.<baja.Component>}
   */


  ChartSettings.prototype.valueScaleSettingsDisplay = function (baseScale) {
    var that = this;
    return baseScale.unitQualityName().then(function (unitQualityName) {
      var valueScaleComp = that.valueScaleSettings(baseScale),
          discrete = modelUtil.hasDiscrete(baseScale.seriesList());
      var domain = baseScale.scale().domain(),
          stretchDomain = modelUtil.stretchDomain(that.$widget.model(), domain, true),
          min = stretchDomain[0],
          max = stretchDomain[1],
          facets = baja.Facets.make(['units'], [baseScale.units()]); //TODO: maybe transient if locked?

      choiceUtil.setChoice(valueScaleComp, "max", max, facets, discrete ? baja.Flags.HIDDEN : 0);
      choiceUtil.setChoice(valueScaleComp, "min", min, facets, discrete ? baja.Flags.HIDDEN : 0);
      choiceUtil.setChoice(valueScaleComp, "locked", baseScale.isLocked(), null, discrete ? baja.Flags.HIDDEN : 0); //display Information
      //for information purposes, display both displayUnit and unit

      if (baseScale.unitSymbol() !== baseScale.displayUnitSymbol()) {
        choiceUtil.setChoice(valueScaleComp, "displayUnit", baseScale.displayUnitSymbol(), undefined, baja.Flags.READONLY | baja.Flags.TRANSIENT);
      }

      choiceUtil.setChoice(valueScaleComp, "unit", baseScale.unitSymbol(), undefined, baja.Flags.READONLY | baja.Flags.TRANSIENT); //This requires async resolution of qualityName and its not that important

      choiceUtil.setChoice(valueScaleComp, "unitQuality", unitQualityName, undefined, baja.Flags.READONLY | baja.Flags.TRANSIENT); //show all the associated series with this unit

      var seriesList = baseScale.seriesList(),
          i,
          displayList = "";

      for (i = 0; i < seriesList.length; i++) {
        if (i > 0) {
          displayList += ", ";
        }

        displayList += seriesList[i].displayName();
      }

      choiceUtil.setChoice(valueScaleComp, "seriesList", displayList, undefined, baja.Flags.READONLY | baja.Flags.TRANSIENT);
      choiceUtil.setDisplayNames(valueScaleComp);
      return valueScaleComp;
    });
  };

  ChartSettings.prototype.getUniqueColor = function () {
    var that = this,
        settings = that.$seriesListSettings,
        colors = that.getDefaultLineColors(),
        storedColor,
        index; // Call function for each Property that's a ControlPoint

    settings.getSlots().is("baja:Component").each(function (slot) {
      var seriesSettings = this.get(slot);
      storedColor = seriesSettings.get("color");

      if (storedColor) {
        index = colors.indexOf(storedColor.toCssString());

        while (index > -1) {
          colors.splice(index, 1);
          index = colors.indexOf(storedColor.toCssString());
        }
      }
    });

    if (colors.length) {
      if (colors[0]) {
        return baja.$('gx:Color', colors[0]);
      }
    }

    return baja.$('gx:Color', "null");
  };

  ChartSettings.prototype.getDefaultChartType = function (series) {
    var that = this,
        settings = that.$seriesListSettings,
        barFound = false,
        notBarFound = false; //if you have all bars, adding a series will default to bar

    settings.getSlots().is("baja:Component").each(function (slot) {
      var seriesSettings = this.get(slot),
          configuredChartType = seriesSettings.get("chartType"),
          encoding;

      if (configuredChartType) {
        encoding = configuredChartType.encodeToString();

        if (encoding === "bar") {
          barFound = true;
        } else if (encoding) {
          //non empty and not bar
          notBarFound = true;
        }
      }
    }); //all others are configured bars, so keep the bars going

    if (barFound && !notBarFound) {
      return "bar";
    }

    if (series.isDiscrete()) {
      return "shade";
    } else if (series.getDefaultInterpolation() === "linear") {
      return "line";
    } else {
      return "discreteLine";
    }
  };
  /**
   * Graph the data and return a promise when its complete.
   * @returns {Promise}
   */


  ChartSettings.prototype.graphData = function () {
    var widget = this.$widget;
    return widget.graphData(widget.model().seriesList());
  };
  /**
   * Redraw Widget and a promise when redrawing is complete.
   * @returns {Promise}
   */


  ChartSettings.prototype.redraw = function () {
    var widget = this.$widget;
    return widget.redraw();
  };
  /**
   * Return true if the widget is loaded from a Chart File
   * @returns {boolean}
   */


  ChartSettings.prototype.isFile = function () {
    var widget = this.$widget;
    return widget.isFile();
  };
  /**
   * Retrieve the subscriber
   */


  ChartSettings.prototype.getSubscriber = function () {
    var that = this,
        widget = that.$widget;

    if (widget && widget.getSubscriber) {
      return widget.getSubscriber();
    }
  };

  return ChartSettings;
});
