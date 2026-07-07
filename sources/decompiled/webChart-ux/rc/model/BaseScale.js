/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/BaseScale
 */
define(['d3', 'Promise', 'baja!', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webEditors/rc/fe/baja/util/numberUtils', 'underscore'], function (d3, Promise, baja, webChartUtil, modelUtil, numberUtils, _) {
  "use strict";
  /**
   * BaseScale represents the base scale for a chart data.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/BaseScale
   * @param {Object} model The model used with this Scale.
   * @param {BaseSeries} [series] The first Series in this scale (if present).
   * @param {Object} [params] The container for the parameters for creating this Scale.
   * @param {String} [params.dataProperty] The dataProperty for the Scale. Defaults to 'y'
   */

  var BaseScale = function BaseScale(model, series, params) {
    var that = this;
    that.$seriesList = series ? [series] : [];
    that.$model = model || null;
    that.$units = series ? series.units() : baja.Unit.DEFAULT;
    that.$enabled = true;
    that.$scale = d3.scale.linear();
    that.$unitQualityName = "";
    that.$dataProperty = params && params.dataProperty ? params.dataProperty : "y"; //default to value scale

    that.$displayUnitSymbol = "";
  };
  /**
   * Initialize any asynchronous data, this includes initializing the unitSymbol based on the current baja.unitConversion()
   * @returns {Promise.<module:nmodule/webChart/rc/model/BaseScale>}
   */


  BaseScale.prototype.initialize = function () {
    var that = this,
        units = that.units();

    if (units && !units.equals(baja.Unit.DEFAULT)) {
      return numberUtils.getDisplayUnits(units, baja.getUnitConversion()).then(function (displayUnits) {
        if (displayUnits) {
          that.$displayUnitSymbol = displayUnits.getSymbol();
        }

        return that;
      });
    }

    return Promise.resolve(that);
  };
  /**
   * Default Display UnitSymbol accessor, unitConversion is only available after BaseSeries.inialize is complete.
   * Before initialize is complete, the BaseScale.unitSymbol will be used.
   *
   * @returns {String}
   */


  BaseScale.prototype.displayUnitSymbol = function () {
    var that = this;
    return that.$displayUnitSymbol || that.unitSymbol();
  };
  /**
   * unit accessor.
   *
   * @returns {baja.Unit}
   */


  BaseScale.prototype.units = function () {
    return this.$units;
  };
  /**
   * data property accessor.
   *
   * @returns {String}
   */


  BaseScale.prototype.dataProperty = function () {
    return this.$dataProperty;
  };
  /**
   * unitDescription accessor.
   *
   * @returns {String}
   */


  BaseScale.prototype.unitDescription = function () {
    var that = this,
        symbol = that.unitSymbol(),
        description = that.units().getUnitName();
    return description && description !== "null" ? description : symbol || "none";
  };
  /**
   * resolve the unitQualityName.
   *
   * @returns {Promise.<String>}
   */


  BaseScale.prototype.unitQualityName = function () {
    var that = this;

    if (that.units().equals(baja.Unit.DEFAULT) || that.$unitQualityName) {
      return Promise.resolve(that.$unitQualityName);
    }

    return webChartUtil.getQuantity(that.units()).then(function (result) {
      that.$unitQualityName = result;
      return result;
    });
  };
  /**
   * Accessor for unitSymbol
   * @returns {String}
   */


  BaseScale.prototype.unitSymbol = function () {
    var symbol = this.$units.getSymbol();

    if (symbol === "null") {
      symbol = "";
    }

    return symbol;
  };
  /**
   * Accessor for uniqueName, the unique name for each BaseScale.
   * @param {boolean} [skipGenerate]
   * @returns {*}
   */


  BaseScale.prototype.uniqueName = function (skipGenerate) {
    var that = this,
        displayUnitSymbol = that.displayUnitSymbol(),
        name = displayUnitSymbol || "null";

    if (name === "null") {
      if (that.$uniqueName) {
        return that.$uniqueName;
      } else if (!skipGenerate) {
        that.$uniqueName = modelUtil.generateUniqueValueScaleName(that.$model, that);
      }

      return that.$uniqueName;
    }

    return name;
  };
  /**
   * Display or set Text for clicking the unit Label.
   * @param {String} [displayName] This will set the displayName attribute of the scale.
   * @returns {String}
   */


  BaseScale.prototype.displayName = function (displayName) {
    return this.$model.settings().scaleDisplayName(this, displayName);
  };

  BaseScale.prototype.requiresTicks = function () {
    var that = this,
        seriesList = that.$seriesList,
        i;

    for (i = 0; i < seriesList.length; i++) {
      if (!seriesList[i].isShade()) {
        return true;
      }
    }

    return false;
  };
  /**
   * Provides an array of numbers to use for ticks on the chart.
   * @returns {Array.<number>}
   */


  BaseScale.prototype.scaleTicks = function () {
    if (this.primarySeries()) {
      var ticks = this.primarySeries().getTicks();

      if (_.isArray(ticks)) {
        return ticks;
      } else {
        return this.scale().ticks(ticks);
      }
    }

    return this.scale().ticks(8);
  };
  /**
   * SeriesList Accessor
   *
   * @returns {Array.<BaseSeries>}
   */


  BaseScale.prototype.seriesList = function () {
    return this.$seriesList;
  };
  /**
   * Primary Series Accessor
   *
   * @returns {module:nmodule/webChart/rc/model/BaseSeries}
   */


  BaseScale.prototype.primarySeries = function () {
    return this.$seriesList[0];
  };
  /**
   * Scale accessor.
   * @returns {d3.Scale}
   */


  BaseScale.prototype.scale = function () {
    return this.$scale;
  };
  /**
   * Is this BaseScale enabled.
   *
   * @returns {boolean}
   */


  BaseScale.prototype.isEnabled = function () {
    return this.$enabled;
  };
  /**
   * Set enabled.
   *
   * @param {boolean} enabled this will set the enabled attribute of the scale.
   */


  BaseScale.prototype.setEnabled = function (enabled) {
    var that = this;

    if (enabled !== that.$enabled) {
      that.$enabled = enabled;
    }
  };
  /**
   * Is this BaseScale locked.
   *
   * @returns {boolean}
   */


  BaseScale.prototype.isLocked = function () {
    return this.$model.settings().isScaleLocked(this);
  };
  /**
   * Set locked.
   *
   * @param {boolean} locked this will set the locked attribute of the scale.
   * @param {Array.<number>} [minMax]
   */


  BaseScale.prototype.setLocked = function (locked, minMax) {
    this.$model.settings().setScaleLocked(this, locked, minMax);
  };
  /**
   * Get the Min/Max for the Scale based on available facets and data.
   *
   * @param {boolean} useSamplingPoints
   * @param {Array.<number>} [fallback]
   * @returns {Array.<number>}
   */


  BaseScale.prototype.getMinMax = function (useSamplingPoints, fallback) {
    var that = this,
        min = Number.POSITIVE_INFINITY,
        max = Number.NEGATIVE_INFINITY,
        i,
        seriesList = this.seriesList();

    for (i = 0; i < seriesList.length; i++) {
      var series = seriesList[i],
          points = useSamplingPoints ? series.samplingPoints() : series.points(),
          j = 0,
          length = points.length;

      for (j = 0; j < length; j++) {
        var point = points[j][that.$dataProperty];

        if (point !== undefined) {
          min = Math.min(min, point);
          max = Math.max(max, point);
        }
      }
    }

    if (min === Number.POSITIVE_INFINITY || min === Number.NEGATIVE_INFINITY) {
      min = fallback[0];
    }

    if (max === Number.POSITIVE_INFINITY || max === Number.NEGATIVE_INFINITY) {
      max = fallback[1];
    }

    return [min, max];
  };
  /**
   * When the axis is locked, provide the min and max if both min and max are set.
   * @return {Array.<number>|undefined}
   */


  BaseScale.prototype.getLockedMinMax = function () {
    var that = this,
        settings = that.$model.settings().valueScaleSettings(that);
    var min = settings.get("min"),
        max = settings.get("max"),
        locked = settings.get("locked");

    if (locked && isFinite(min) && isFinite(max) && min < max) {
      return [min, max];
    }
  };
  /**
   * Set the domain based on the current options for min and max
   */


  BaseScale.prototype.setDomainFromOptions = function () {
    var that = this;
    var lockedMinMax = that.getLockedMinMax();

    if (lockedMinMax) {
      var min = lockedMinMax[0],
          max = lockedMinMax[1],
          domain = that.scale().domain(),
          oldMin = domain[0],
          oldMax = domain[1];

      if (min !== oldMin || max !== oldMax) {
        that.scale().domain(modelUtil.stretchDomain(that.$model, [min, max]));
      }
    }
  };

  return BaseScale;
});
