/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * @module nmodule/webChart/rc/model/BaseSeries
 */
define(['Promise', 'baja!', 'nmodule/webChart/rc/export/exportUtil', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/model/samplingUtil', 'nmodule/webChart/rc/model/ValueScale', 'nmodule/webChart/rc/chartEvents'], function (Promise, baja, exportUtil, webChartUtil, modelUtil, samplingUtil, ValueScale, events) {
  "use strict";
  /**
   * Returns points with the interpolated tail for this series if it is found
   * that an interpolated tail is needed.
   *
   * @inner
   *
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} points
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>}
   */

  function pointsWithInterpolatedTail(series, points) {
    if (!series.hasInterpolatedPoints()) {
      return points;
    }

    if (points.length === 0) {
      return points;
    }

    var lastPoint = points[points.length - 1],
        lastPointTime = lastPoint.x;

    if (!series.$interpolatedPointDate) {
      series.updateInterpolatedTail();
    }

    if (lastPointTime.getTime() >= series.$interpolatedPointDate.getTime()) {
      return points;
    }

    return points.concat(series.$tailPoints);
  }
  /**
   * API Status: **Development**
   *
   * BaseSeries represents the Base Series for a chart data representation.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/BaseSeries
   * @param {module:nmodule/webChart/rc/model/BaseModel} model The model used with this Series.
   * @param {Object} params The container for the parameters for creating this Series.
   * @param {String} params.ord The ord for the Series.
   * @param {String} params.originalOrd The original ord for the Series (prior
   * to traversing to history space via a history extension or n:history tag)
   * @param {baja.Value} [params.value] Optional resolved value for the Series.
   */


  var BaseSeries = function BaseSeries(model, params) {
    var that = this;

    if (!params) {
      params = {};
    }

    that.$params = params;
    that.$ord = webChartUtil.getRelativeOrd(params.ord);
    that.$originalOrd = webChartUtil.getRelativeOrd(params.originalOrd) || that.$ord;
    that.$value = params.value || null;
    that.$model = model || null;
    that.$displayName = null;
    that.$displayPath = null;
    that.$shortDisplayName = null;
    that.$units = baja.Unit.DEFAULT;
    that.$points = [];
    that.$enabled = true;
    that.$subscribeContinuous = false;
    that.$capacity = webChartUtil.getMaxSeriesCapacity();
    that.$lastY = null;
    that.$facets = baja.Facets.DEFAULT;
    that.$loaded = false;
    that.$loading = false;
    that.$recordType = null;
    that.$valueScale = new ValueScale(model, that); //fallback scale

    that.$cachedPrecision = null;
    that.$focusPoints = null; //subset of points in focus, no sampling yet

    that.$samplingPoints = null; //subset of points in focus, then sample algorithm applied

    that.$error = null; //for user error reporting

    that.$warnings = []; // warnings associated with the series
  };
  /**
   * Color accessor. Get or set Series color as an HTML string '#rrggbb'
   *
   * @param {String} [color] If specified, this will set color.
   * @returns {String}
   */


  BaseSeries.prototype.color = function (color) {
    return this.$model.settings().seriesColor(this, color);
  };
  /**
   * ChartType accessor.
   *
   * @param {String} [chartType] If specified, this will set chartType.
   * @returns {String}
   */


  BaseSeries.prototype.chartType = function (chartType) {
    return this.$model.settings().seriesChartType(this, chartType);
  };
  /**
   * Return true when the Series is considered a Line Chart.
   * @returns {boolean}
   */


  BaseSeries.prototype.isLine = function () {
    var chartType = this.$model.settings().seriesChartType(this);
    return chartType === "line" || chartType === "discreteLine";
  };
  /**
   * Return true when the Series is considered a Discrete Line.
   * @returns {boolean}
   */


  BaseSeries.prototype.isDiscreteLine = function () {
    var chartType = this.$model.settings().seriesChartType(this);
    return chartType === "discreteLine";
  };
  /**
   * Return true when the Series is considered a BarChart.
   * @returns {boolean}
   */


  BaseSeries.prototype.isBar = function () {
    var chartType = this.$model.settings().seriesChartType(this);
    return chartType === "bar";
  };
  /**
   * Return true when the Series is considered a Shade Chart.
   * @returns {boolean}
   */


  BaseSeries.prototype.isShade = function () {
    var chartType = this.$model.settings().seriesChartType(this);
    return chartType === "shade";
  };
  /**
   * Determines whether or not the series point methods should include
   * interpolated data. This may be overridden by child classes to turn off
   * interpolated point data.
   *
   * @return {boolean} true if interpolate tail is turned on, false if not.
   */


  BaseSeries.prototype.hasInterpolatedPoints = function () {
    return this.$model.settings().isShowInterpolateTail();
  };
  /**
   * Updates the timestamp of the interpolated point for the data points.
   */


  BaseSeries.prototype.updateInterpolatedTail = function () {
    this.$interpolatedPointDate = new Date();
    var points = (this.$model.isSampling() || this.isBar()) && this.$samplingPoints ? this.$samplingPoints : this.$points,
        tailPoints = [];

    if (!points.length) {
      this.$tailPoints = tailPoints;
      return;
    }

    var lastPoint = points[points.length - 1];
    tailPoints.push({
      x: this.$interpolatedPointDate,
      y: points[points.length - 1].y,
      interpolated: true,
      status: lastPoint.status
    });
    this.$tailPoints = tailPoints;
  };
  /**
   * History capacity accessor. Note that you should not set the capacity greater than the Max Series Capacity that defaults
   * to 250K records.
   *
   * @param {Number} [capacity] If specified, this will set capacity.
   * @returns {Number}
   */


  BaseSeries.prototype.capacity = function (capacity) {
    var that = this;

    if (capacity !== undefined) {
      //only change capacity if its less than max and greater than zero
      if (capacity > 0 && capacity < webChartUtil.getMaxSeriesCapacity()) {
        that.$capacity = capacity;
      }
    }

    return that.$capacity;
  };
  /**
   * Trim the Points the allotted capacity.
   */


  BaseSeries.prototype.trimToCapacity = function () {
    var that = this,
        cap = that.$capacity,
        length = that.$points ? that.$points.length : -1;

    if (cap > -1 && length > cap) {
      that.$points = that.$points.slice(length - cap);

      if (!that.$model.isOverMaxCapacity() && length > webChartUtil.getMaxSeriesCapacity()) {
        that.$model.setOverMaxCapacity(true);
      }
    }
  };
  /**
   * Error accessor.
   *
   * @param {String} [error] If specified, this will set the error for the series.
   * @returns {String}
   */


  BaseSeries.prototype.error = function (error) {
    if (error !== undefined) {
      this.$error = error;
    }

    return this.$error;
  };
  /**
   * Get the facets for this series.
   *
   * @returns {baja.Facets}
   */


  BaseSeries.prototype.facets = function () {
    return this.$facets;
  };
  /**
   * Return true if concurrent load is allowed, otherwise load is complete after others are loaded
   *
   * @returns {Boolean}
   */


  BaseSeries.prototype.isPrimaryLoad = function () {
    return true;
  };
  /**
   * This type describes the Object properties expected for a Point used by the Chart Model.
   *
   * @typedef {Object} module:nmodule/webChart/rc/model/BaseSeries~Point
   * @property {Data|Object} x The value for the x axis, usually a Date.
   * @property {Number|Object} y The value for the y axis, usually a Number.
   * @property {boolean} [skip] if given, this tells the ChartModel that there is break in the data.
   * @property {Number} [status] if given, this is the 'status bits' of a baja.Status.
    /**
   * Data Points accessor.
   *
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} [points] If specified, this will set the data points Array for the series.
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>}
   */


  BaseSeries.prototype.points = function (points) {
    var that = this;

    if (points !== undefined) {
      that.$points = points;
      that.trimToCapacity();
    }

    return pointsWithInterpolatedTail(that, that.$points);
  };
  /**
   * Focus Points accessor returns a subset of the total points if user is zoomed in on a subset of domain.
   * @param {boolean} store If true, store the results for other calls to focusPoints that are not for storage.
   *                  If false, return the existing stored focusPoints if the points are already available.
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>}
   */


  BaseSeries.prototype.focusPoints = function (store) {
    var that = this;

    if (that.$focusPoints && !store) {
      return that.$focusPoints;
    }

    var points = that.points(),
        model = that.$model,
        timeScale = model.timeScale(),
        scale = timeScale.scale(),
        timeRange = model.timeRange(),
        domain = scale.domain(),
        timezone = timeScale.getTimeZone(),
        getX = function getX(point) {
      return point.x;
    },
        index = samplingUtil.getEdgeIndex(points, domain[0], getX),
        endIndex = samplingUtil.getEdgeIndex(points, domain[1], getX); // if has prerecord, include it


    if (index > 0) {
      index--;
    }

    endIndex += 2; //always go at least 2

    var zoomedPoints = points.slice(index, endIndex); // ensure pre and post records don't scew data if sampling is on and not average

    if (model.isSampling() && model.samplingType() !== "average") {
      zoomedPoints = trimPointsToTimeRange(zoomedPoints, getX, timeRange, timezone);
    }

    if (store) {
      that.$focusPoints = zoomedPoints;
    }

    return zoomedPoints;
  };
  /**
   * Set the samplingPoints for this series
   * @param {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>} samplingPoints
   */


  BaseSeries.prototype.setSamplingPoints = function (samplingPoints) {
    this.$samplingPoints = samplingPoints;
  };
  /**
   * Sampling Points accessor returns a subset of the total points if user is zoomed in on a subset of domain
   *
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseSeries~Point>}
   */


  BaseSeries.prototype.samplingPoints = function () {
    var that = this,
        sampling = that.$model.isSampling();

    if ((sampling || that.isBar()) && that.$samplingPoints) {
      return pointsWithInterpolatedTail(that, that.$samplingPoints);
    }

    return that.focusPoints();
  };
  /**
   * Units accessor.
   *
   * @param {baja.Unit} [units] If specified, this will set the units.
   * @returns {baja.Unit}
   */


  BaseSeries.prototype.units = function (units) {
    if (units !== undefined) {
      this.$units = units;
    }

    return this.$units;
  };
  /**
   * Value accessor.
   *
   * @param {baja.Value} [value] If specified, this will set the value.
   * @returns {baja.Value}
   */


  BaseSeries.prototype.value = function (value) {
    if (value !== undefined) {
      this.$value = value;
    }

    return this.$value;
  };
  /**
   * ValueScale accessor.
   *
   * @returns {module:nmodule/webChart/rc/model/ValueScale}
   */


  BaseSeries.prototype.valueScale = function () {
    return this.$valueScale;
  };
  /**
   * Display Name accessor.
   *
   * @param {String} [displayName] If specified, this will set the displayName.
   * @returns {String}
   */


  BaseSeries.prototype.displayName = function (displayName) {
    if (displayName !== undefined) {
      this.$displayName = displayName;
    }

    return this.$displayName;
  };
  /**
   * Display Path accessor.
   * @param {String} [displayPath] If specified, this will set the displayPath.
   *
   * @returns {String}
   */


  BaseSeries.prototype.displayPath = function (displayPath) {
    if (displayPath !== undefined) {
      this.$displayPath = displayPath;
    }

    if (!this.$displayPath) {
      return this.$ord;
    }

    return this.$displayPath;
  };
  /**
   * Short Display Name accessor.
   * @param {String} [shortDisplayName] If specified, this will set the shortDisplayName.
   *
   * @returns {String}
   */


  BaseSeries.prototype.shortDisplayName = function (shortDisplayName) {
    if (shortDisplayName !== undefined) {
      this.$shortDisplayName = shortDisplayName;
    }

    if (!this.$shortDisplayName) {
      return webChartUtil.getShortDisplayName(this.displayName());
    }

    return this.$shortDisplayName;
  };
  /**
   * Returns whether or not this series is enabled.
   *
   * @returns {boolean}
   */


  BaseSeries.prototype.isEnabled = function () {
    return this.$model.settings().seriesEnabled(this);
  };
  /**
   * Sets whether or not this series is enabled.
   *
   * @param {boolean} enabled
   */


  BaseSeries.prototype.setEnabled = function (enabled) {
    var that = this;

    if (that.isEnabled() === enabled) {
      return;
    }

    this.$model.settings().seriesEnabled(that, enabled);

    if (that.$model) {
      that.$model.jq().trigger(enabled ? events.SERIES_ENABLED : events.SERIES_DISABLED, [that]);
    }
  };
  /**
   * ord accessor.
   *
   * @returns {baja.Ord}
   */


  BaseSeries.prototype.ord = function () {
    return this.$ord;
  };
  /**
   * Original Ord accessor.
   *
   * @returns {baja.Ord}
   */


  BaseSeries.prototype.originalOrd = function () {
    return this.$originalOrd;
  };
  /**
   * Get the recordType.
   *
   * @returns {baja.TypeSpec}
   */


  BaseSeries.prototype.recordType = function () {
    return this.$recordType;
  };
  /**
   * loaded accessor.
   *
   * @returns {boolean}
   */


  BaseSeries.prototype.isLoaded = function () {
    return this.$loaded;
  };
  /**
   * Called when a Series is added to the model.
   * @since Niagara 4.9
   */


  BaseSeries.prototype.added = function () {
    //initialize settings based on params
    this.$model.settings().seriesSettings(this, this.$params);
  };
  /**
   * Override this method to load your series info and resolve a Promise when complete.
   *
   * @returns {Promise}
   */


  BaseSeries.prototype.loadInfo = function () {
    this.$loaded = true;
    return modelUtil.resolvedPromise();
  };
  /**
   * Override this method to load your series data and resolve a promise when complete.
   *
   * @returns {Promise}
   */


  BaseSeries.prototype.loadData = function () {
    this.$loaded = true;
    return modelUtil.resolvedPromise();
  };
  /**
   * A Series is Discrete if its record type is a Boolean or DynamicEnum.
   * @returns {boolean}
   */


  BaseSeries.prototype.isDiscrete = function () {
    var recordType = this.$recordType;
    return recordType === "baja:Boolean" || recordType === "baja:DynamicEnum";
  };
  /**
   * A Series is Boolean if its record type is a Boolean.
   * @returns {boolean}
   */


  BaseSeries.prototype.isBoolean = function () {
    var recordType = this.$recordType;
    return recordType === "baja:Boolean";
  };
  /**
   *
   * @returns {module:baja/obj/TimeZone|null}
   */


  BaseSeries.prototype.getPreferredTimeZone = function () {
    return this.$preferredTimeZone;
  };
  /**
   * Sets the preferred timezone for display.
   *
   * @param {module:baja/obj/TimeZone} timeZone
   */


  BaseSeries.prototype.setPreferredTimeZone = function (timeZone) {
    this.$preferredTimeZone = timeZone;
  };
  /**
   *What is the default d3 Interpolation for the line? Some options are the following:
   *
   * linear - piecewise linear segments, as in a polyline.
   *
   * step-after - alternate between horizontal and vertical segments, as in a step function.
   *
   * step - alternate between horizontal and vertical segments, as in a step function.
   *
   * @returns {String}
   */


  BaseSeries.prototype.getDefaultInterpolation = function () {
    return "linear";
  };
  /**
   * What is the current Interpolation for the line?
   * @returns {String}
   */


  BaseSeries.prototype.getLineInterpolation = function () {
    if (this.isDiscreteLine()) {
      return "step-after";
    } else {
      return "linear"; //return "monotone"; //TODO: although this looks much better, not safe due to NCCB-7988
    }
  };
  /**
   * Get the baja Value for y.
   * @param {Number} y the raw y data
   * @returns {baja.Value}
   */


  BaseSeries.prototype.getYAsValue = function (y) {
    var that = this,
        facets = that.$facets;

    if (that.isBoolean()) {
      return !!y;
    } else if (that.isDiscrete()) {
      return baja.DynamicEnum.make({
        ordinal: y,
        range: facets.get('range')
      });
    } else {
      return y;
    }
  };
  /**
   * Get the Display Value for y. Override for custom values in data.
   * @param y the raw y data
   * @returns {Promise.<String>}
   */


  BaseSeries.prototype.resolveYDisplay = function (y) {
    var that = this,
        facets = that.$facets;

    if (that.isDiscrete()) {
      return modelUtil.resolveEnumDisplay(that, y, facets);
    }

    var params = facets.toObject();
    params.precision = that.getPrecision();
    return modelUtil.resolveNumericDisplay(y, params);
  };
  /**
   * Return the precision for this Series based on the available facets
   * @returns {Number} min 0, max 20
   */


  BaseSeries.prototype.getPrecision = function () {
    var that = this,
        facets = that.$facets,
        cached = that.$cachedPrecision,
        precision;

    if (cached !== null) {
      return cached;
    }

    if (!facets) {
      return 2; //default for number until facets is available
    } //TODO: why do we allow entering a negative precision?


    precision = Math.max(0, Math.min(facets.get("precision", 2), 20));
    that.$cachedPrecision = precision;
    return precision;
  };
  /**
   * Provide either the number of ticks desired, hardcoded tick values, of function
   * to pass to d3.scale.ticks.
   * @returns {number|Array.<number>|*}
   */


  BaseSeries.prototype.getTicks = function () {
    var that = this,
        newTicks = [];

    if (that.isShade()) {
      return 0;
    } else if (that.isBoolean()) {
      var domain = that.valueScale().scale().domain(),
          min = domain[0],
          max = domain[1];

      if (min <= 0 && max >= 0) {
        newTicks.push(0);
      }

      if (min <= 1 && max >= 1) {
        newTicks.push(1);
      }

      return newTicks;
    } else if (that.isDiscrete()) {
      var ticks = that.valueScale().scale().ticks(8);

      for (var i = 0; i < ticks.length; i++) {
        var tick = Math.round(ticks[i]);

        if (!newTicks.contains(tick)) {
          newTicks.push(tick);
        }
      }

      return newTicks;
    }

    return 8; //TODO: maybe a bajaux property?
  };
  /**
   * Return true if the subscription since the last update is continuous
   *
   * @returns {boolean}
   */


  BaseSeries.prototype.isSubscribeContinuous = function () {
    return this.$subscribeContinuous;
  };
  /**
   * Set Subscribe Continuous.
   *
   * @param {boolean} [subscribeContinuous] If specified, this will set subscribeContinuous.
   */


  BaseSeries.prototype.setSubscribeContinuous = function (subscribeContinuous) {
    if (subscribeContinuous !== undefined) {
      this.$subscribeContinuous = subscribeContinuous;
    }
  };
  /**
   * Unload the current series by emptying the points and set loaded status back to false.
   */


  BaseSeries.prototype.unload = function () {
    var that = this;
    that.$points.length = 0;
    that.$loaded = false;
    that.$loading = false;
  };
  /**
   * Using the raw data, convert to a {@link module:nmodule/webChart/rc/model/BaseSeries~Point|BaseSeries~Point} and add to the
   * BaseSeries.points array
   * @param {Object} raw The object passed over from the WebChartQueryServlet
   */


  BaseSeries.prototype.preparePoint = function (raw) {};
  /**
   * Using raw data from a subscription, convert this to a  {@link module:nmodule/webChart/rc/model/BaseSeries~Point|BaseSeries~Point} and
   * add to the BaseSeries.points array. If this is the same as BaseSeries.preparePoint then its doesn't need to be
   * overridden.
   * @param {Object} raw The object passed from subscription
   */


  BaseSeries.prototype.prepareLivePoint = function (raw) {
    this.preparePoint(raw);
  };
  /**
   * Subscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   */


  BaseSeries.prototype.subscribe = function (subscriber) {};
  /**
   * Unsubscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   */


  BaseSeries.prototype.unsubscribe = function (subscriber) {};
  /**
   * Save the Series to the JSON.
   *
   * @param  {Object} seriesJson The object used to hold the details
   * of the Series data.
   * @param {Object} [optionInfo] has entries of ordType and optionally
   * @param {baja.DynamicEnum} [optionInfo.ordType=0] defaults to ordinal 0 with tag 'absolute' the other option is 'relative',
   * @param {String} [optionInfo.baseOrd] if ordType is relative, the base used for relativizing the series ords.
   *  baseOrd.
   */


  BaseSeries.prototype.saveToJson = function (seriesJson, optionInfo) {
    var that = this,
        color = that.color(),
        chartType = that.chartType(),
        enabled = that.isEnabled();

    if (optionInfo) {
      var ordType = optionInfo.ordType,
          baseOrd = optionInfo.baseOrd;

      if (!ordType || ordType.getTag() === "absolute") {
        seriesJson.ord = that.ord();
      } else {
        baseOrd = baja.Ord.make(baseOrd);
        seriesJson.ord = exportUtil.relativizeOrd(baseOrd, baja.Ord.make(that.originalOrd())).toString();
      }
    } else {
      seriesJson.ord = that.ord();
    }

    if (chartType) {
      seriesJson.chartType = baja.bson.encodeValue(chartType);
    }

    if (color) {
      seriesJson.color = baja.bson.encodeValue(color);
    }

    if (enabled !== undefined) {
      seriesJson.enabled = baja.bson.encodeValue(enabled);
    }
  };

  function trimPointsToTimeRange(points, getX, timeRange, timezone) {
    if (points.length === 0) {
      return points;
    }

    var index = 0,
        maxIndex = points.length - 1,
        startAndEnd = webChartUtil.getStartAndEndDateFromTimeRange(timeRange, timezone);

    while (index < maxIndex && getX(points[index]).valueOf() < startAndEnd.start.valueOf()) {
      index++;
    }

    while (maxIndex > index && getX(points[maxIndex]).valueOf() > startAndEnd.end.valueOf()) {
      maxIndex--;
    }

    return points.slice(index, maxIndex + 1);
  }
  /**
   * Accessor for retrieving warnings
   *
   * warnings contain warning JSONObject
   * [warning] warning message details
   * @property {string} [warning.module] contains the name of the module to use for the lexicon lookup
   * @property {string} [warning.title] optional and contains the lexicon key for the warning title String
   * @property {string} [warning.titleArgs] optional and contains the lexicon args for the warning title String
   * @property {string} [warning.msg] optional and contains the lexicon key for the warning message
   * @property {string} [warning.msgArgs] optional and contains the lexicon args for the warning message
   * @property {string} [warning.coaleseable] optional, but when present with a String value that is a delimiter String,
   * this warning message can be coalesced with other warnings of the same type (same type means same title and msg)
   * by checking if the msgArgs is a String and then concatenating the msgArgs together using the supplied delimiter.
   *
   * @return {Array.<Object>}
   */


  BaseSeries.prototype.warnings = function () {
    return this.$warnings;
  };
  /**
   * Add a JSONObject containing warning details to the warnings array
   * @param warning
   */


  BaseSeries.prototype.addWarning = function (warning) {
    this.$warnings.push(warning);
  };

  return BaseSeries;
});
