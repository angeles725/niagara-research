/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/ValueScale
 */
define(['baja!', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/model/BaseScale'], function (baja, modelUtil, BaseScale) {
  "use strict";
  /**
   * ValueScale represents the value scale for a chart data.
   *
   * @class
   * @extends module:nmodule/webChart/rc/model/BaseScale
   * @alias module:nmodule/webChart/rc/model/ValueScale
   * @param {Object} model The model used with this Scale.
   * @param {module:nmodule/webChart/rc/model/BaseSeries} [series] The first Series in this scale (if present).
   */

  var ValueScale = function ValueScale(model, series) {
    BaseScale.apply(this, arguments);
  };

  ValueScale.prototype = Object.create(BaseScale.prototype);
  ValueScale.prototype.constructor = ValueScale;
  /**
   * @returns {Promise.<module:nmodule/webChart/rc/model/BaseScale>}
   */

  ValueScale.prototype.initialize = function () {
    var that = this;
    return BaseScale.prototype.initialize.apply(that, arguments).then(function () {
      var series = that.primarySeries(),
          minMax = that.getLockedMinMax(series);

      if (minMax) {
        that.setLocked(true, minMax);
      }

      return that;
    });
  };
  /**
   *
   * @param {string} key
   * @param {baja.Facets} facets
   * @param {number} min
   * @returns {number}
   */


  function getFacetMin(key, facets, min) {
    var seriesMin = facets.get(key, baja.Double.POSITIVE_INFINITY).valueOf();

    if (isFinite(seriesMin)) {
      min = Math.min(seriesMin, min);
    }

    return min;
  }
  /**
   *
   * @param {string} key
   * @param {baja.Facets} facets
   * @param {number} max
   * @returns {number}
   */


  function getFacetMax(key, facets, max) {
    var seriesMax = facets.get(key, baja.Double.NEGATIVE_INFINITY).valueOf();

    if (isFinite(seriesMax)) {
      max = Math.max(seriesMax, max);
    }

    return max;
  }
  /**
   * When the axis is locked, provide the locked min and max if they are available.
   * If both 'min' and 'chartMin' facets are set, prefer the 'chartMin'; likewise prefer 'chartMax' over 'max'.
   * If both options and facets contain locked min and max, prefer those set on the options.
   * @returns {Array.<number>|undefined}
   */


  ValueScale.prototype.getLockedMinMax = function () {
    var that = this,
        min = Number.POSITIVE_INFINITY,
        max = Number.NEGATIVE_INFINITY,
        i,
        seriesList = this.seriesList();
    var optionMinMax = BaseScale.prototype.getLockedMinMax.apply(this, arguments);

    if (optionMinMax) {
      return optionMinMax;
    }

    for (i = 0; i < seriesList.length; i++) {
      var facets = seriesList[i].facets();

      if (facets && (that.$model.settings().getFacetsLimitMode() === 'locked' || facets.get("chartLimitMode") === 'locked')) {
        min = getFacetMin("chartMin", facets, min);
        max = getFacetMax("chartMax", facets, max);

        if (isFinite(min) && isFinite(max) && min < max) {
          return [min, max];
        }

        min = getFacetMin("min", facets, min);
        max = getFacetMax("max", facets, max);

        if (isFinite(min) && isFinite(max) && min < max) {
          return [min, max];
        }
      }
    }
  };
  /**
   * Based on the data and facets, provide the best min and max for setting up the domain.
   * @param {boolean} useSamplingPoints
   * @param {Array.<number>} [fallback]
   * @returns {Array.<number>}
   */


  ValueScale.prototype.getMinMax = function (useSamplingPoints, fallback) {
    var that = this,
        min = Number.POSITIVE_INFINITY,
        max = Number.NEGATIVE_INFINITY,
        i,
        seriesList = this.seriesList(),
        facetsLimitMode = that.$model.settings().getFacetsLimitMode();

    for (i = 0; i < seriesList.length; i++) {
      var facets = seriesList[i].facets();

      if (facets) {
        min = getFacetMin("chartMin", facets, min);
        max = getFacetMax("chartMax", facets, max);

        if (facetsLimitMode === 'inclusive' || facets.get("chartLimitMode") === 'inclusive') {
          min = getFacetMin("min", facets, min);
          max = getFacetMax("max", facets, max);
        }
      }
    } //this repeats BaseScale.getMinMax, even if min or max is set so we show any outliers beyond min or max.


    for (i = 0; i < seriesList.length; i++) {
      var series = seriesList[i];

      if (series.isBoolean()) {
        min = Math.min(min, 0);
        max = Math.max(max, 1);
        continue;
      }

      var points = useSamplingPoints ? series.samplingPoints() : series.points(),
          j = 0,
          length = points.length;

      if (series.isDiscrete()) {
        min = Math.min(min, 0); //include the range for min and max

        var range = modelUtil.getEnumRange(series),
            rangeOrdinals = range ? range.getOrdinals() : [];

        if (rangeOrdinals.length > 0) {
          min = Math.min(min, rangeOrdinals[0]);
          max = Math.max(max, rangeOrdinals[rangeOrdinals.length - 1]);
        }
      }

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

  return ValueScale;
});
