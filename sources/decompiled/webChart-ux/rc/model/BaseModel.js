/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/model/BaseModel
 */
define(['d3', 'jquery', 'Promise', 'baja!', 'moment', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/chunkUtil', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/model/samplingUtil', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/model/seriesFactory', 'nmodule/webChart/rc/model/ValueScale'], function (d3, $, Promise, baja, moment, webChartUtil, chunkUtil, modelUtil, samplingUtil, chartEvents, seriesFactory, ValueScale) {
  "use strict";
  /**
   * BaseModel represents the base model for a chart model.
   *
   * @class
   * @alias module:nmodule/webChart/rc/model/BaseModel
   * @param jq The jq for the ChartWidget
   * @param {ChartSettings} settings The settings for a ChartWidget
   */

  var BaseModel = function BaseModel(jq, settings) {
    var that = this;
    that.$jq = jq;
    that.$seriesList = [];
    that.$initialized = false;
    that.$loading = false;
    that.$loadingDfs = [];
    that.$valueScales = [new ValueScale(this)];
    that.$primaryValueScale = null; //if null, is first valueScale with at least 1 series

    that.$secondaryValueScale = null; //if null, is second valueScale with at least 1 series

    that.$maxSeriesListLength = 10; //TODO: give user feedback when this is reached

    that.$isRelative = false;
    that.$settings = settings; //stats

    that.$availableDataPoints = 0;
    that.$samplingPeriod = "";
    that.$samplingMillis = 0;
    that.$stopped = false;
    that.$overMaxCapacity = false;
  };
  /**
   * Returns the jQuery DOM element in which the BaseModel's ChartWidget has been initialized. If
   * `initialize()` has not yet completed its work and called its
   * callback, then this will return `null`.
   *
   * @returns {jQuery} the DOM element in which this widget has been initialized,
   * or null if not yet initialized.
   */


  BaseModel.prototype.jq = function () {
    return this.$jq;
  };
  /**
   * Return true if this BaseModel Supports sampling. Return false to disable BaseModel.sampling.
   * Note that Bar Charts depend on the sampling to set the BaseModel.samplingMillis(), so that value will need to be
   * set manually.
   * @returns {boolean}
   */


  BaseModel.prototype.supportsSampling = function () {
    return true;
  };
  /**
   * This method provides the ability to add extra points after sampling is complete. For Shade Charts, this includes
   * adding a 6 pixel area for user to see the history of the last current value.
   */


  BaseModel.prototype.addExtraPoints = function () {
    var seriesList = this.seriesList(),
        timePixels,
        i;

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isShade()) {
        if (timePixels === undefined) {
          timePixels = modelUtil.getTimeForPixels(this, 6);
        }

        var points = seriesList[i].samplingPoints(),
            lastPoint = points[points.length - 1];

        if (lastPoint && !lastPoint.extraPoint) {
          points[points.length] = {
            extraPoint: true,
            x: modelUtil.safeMomentAdd(lastPoint.x, timePixels).toDate(),
            y: lastPoint.y,
            skip: lastPoint.skip,
            status: lastPoint.status,
            interpolated: lastPoint.interpolated
          };
        }
      }
    }
  };
  /**
   * Setup the sampling based on the available focus data. Provide a non-zero bestSliceOverride if
   * a certain rollup interval is preferred.
   * @param {Number} [bestSliceOverride]
   */


  BaseModel.prototype.startSampling = function (bestSliceOverride) {
    var that = this,
        seriesList = that.$seriesList,
        stats = samplingUtil.calculateSeriesSamplingStats(that),
        allFocusPoints = stats.allFocusPoints,
        allMaxFocusLength = stats.allMaxFocusLength,
        bestSlice = stats.bestSlice,
        allMinTime = stats.allMinTime,
        hasBars = modelUtil.hasBar(seriesList),
        niceIncrement,
        startTime,
        samplingPoints,
        supportsSampling = that.supportsSampling(),
        i;

    if (bestSliceOverride) {
      bestSlice = bestSliceOverride;
    }

    that.$availableDataPoints = allMaxFocusLength;

    if (supportsSampling) {
      samplingUtil.configureAutoSample(that, allMaxFocusLength);
    }

    if (!supportsSampling || !that.isSampling() && !hasBars || allMinTime === Number.MAX_VALUE || bestSlice === Number.MAX_VALUE) {
      samplingUtil.clearSamples(that);
      return;
    } //If bars are present, we need to prevent bars from disappearing, so we may need to revert to bestSlice


    if (that.desiredSamplingPeriod() === baja.RelTime.DEFAULT || that.desiredSamplingPeriod().getMillis() < bestSlice) {
      //We currently do not warn the user that their 'desired' sampling period will not be used, but we could if this was common enough.
      niceIncrement = modelUtil.getNiceTimeIncrement(bestSlice, false);
      that.$samplingPeriod = "" + niceIncrement.increment + " " + webChartUtil.lex.get({
        key: niceIncrement.key,
        def: niceIncrement.key
      });
      that.$samplingMillis = niceIncrement.millis;
      startTime = moment(allMinTime).startOf(niceIncrement.key);
    } else {
      that.$samplingMillis = that.desiredSamplingPeriod().getMillis();
      that.$samplingPeriod = modelUtil.getSamplingPeriodDisplay(that, that.$samplingMillis);
      startTime = moment(allMinTime);
    }

    var resampleRequired = false;

    for (i = 0; i < seriesList.length; i++) {
      if (that.isSampling() || seriesList[i].isBar()) {
        samplingPoints = samplingUtil.rollup(seriesList[i], allFocusPoints[i], startTime, that.$samplingMillis, that);

        if (samplingPoints.length > webChartUtil.getMaxSamplingSize()) {
          resampleRequired = true; //abandon the current sampling and sample again at a larger increment

          break;
        }

        seriesList[i].setSamplingPoints(samplingPoints);
      } else {
        seriesList[i].setSamplingPoints(null);
      }
    } //sample at a larger interval


    if (resampleRequired) {
      var largerTimeIncrement = modelUtil.getNiceTimeIncrement(that.$samplingMillis, true);
      that.startSampling(largerTimeIncrement.millis);
    }
  };
  /**
   * Indicates if the model has one or more series with a relative ord.
   * @returns {boolean}
   */


  BaseModel.prototype.isRelative = function () {
    return this.$isRelative;
  };
  /**
   * Set the isRelative state
   * @param {boolean} relative
   */


  BaseModel.prototype.setRelative = function (relative) {
    this.$isRelative = relative;
  };
  /**
   * Returns the Model's sampling state.
   * @returns {Boolean}
   */


  BaseModel.prototype.isSampling = function () {
    return this.$settings.isSampling();
  };
  /**
   * Sets the Model's Sampling state.
   *
   *
   * @param {Boolean} sampling
   * @param {Boolean} [fromAutoCalculations] if true, let settings so its not considered a configuration change
   */


  BaseModel.prototype.setSampling = function (sampling, fromAutoCalculations) {
    this.$settings.setSampling(sampling, fromAutoCalculations);
  };
  /**
   * Returns the Model's auto sampling state.
   * @returns {Boolean}
   */


  BaseModel.prototype.isAutoSampling = function () {
    return this.$settings.isAutoSampling();
  };
  /**
   * Sets the Model's Auto Sampling state.
   *
   * @param {Boolean} autoSampling
   */


  BaseModel.prototype.setAutoSampling = function (autoSampling) {
    this.$settings.setAutoSampling(autoSampling);
  };
  /**
   * Returns the Model's state on whether its overMaxCapacity.
   * @returns {Boolean}
   */


  BaseModel.prototype.isOverMaxCapacity = function () {
    return this.$overMaxCapacity;
  };
  /**
   * Sets the Model's state for whether its overMaxCapacity. fire event if state has changed
   *
   * @param {Boolean} overMaxCapacity
   */


  BaseModel.prototype.setOverMaxCapacity = function (overMaxCapacity) {
    var that = this;

    if (overMaxCapacity === that.$overMaxCapacity) {
      return;
    }

    that.$overMaxCapacity = overMaxCapacity;
    that.jq().trigger(chartEvents.MODEL_OVER_MAX_CAPACITY, overMaxCapacity);
  };
  /**
   * SampleSize accessor.
   *
   * @param {Number} [sampleSize] If specified, this will set the sampleSize.
   * @returns {Number}
   */


  BaseModel.prototype.sampleSize = function (sampleSize) {
    return this.$settings.sampleSize(sampleSize);
  };
  /**
   * desiredSamplingPeriod accessor.
   *
   * @param {baja.RelTime} [desiredSamplingPeriod] If specified, this will set the Desired Sampling Period
   * @returns {baja.RelTime}
   */


  BaseModel.prototype.desiredSamplingPeriod = function (desiredSamplingPeriod) {
    return this.$settings.desiredSamplingPeriod(desiredSamplingPeriod) || baja.RelTime.DEFAULT;
  };
  /**
   * samplingType accessor.
   *
   * @param {String} [samplingType] If specified, this will set the Sampling Type.
   * @returns {String}
   */


  BaseModel.prototype.samplingType = function (samplingType) {
    return this.$settings.samplingType(samplingType);
  };
  /**
   * samplingPeriod accessor.
   *
   * @returns {String} Display String for period used with sampling
   */


  BaseModel.prototype.samplingPeriod = function () {
    return this.$samplingPeriod;
  };
  /**
   * maxSeriesListLength accessor.
   *
   * @returns {Number} The amount amount of series allowed
   */


  BaseModel.prototype.maxSeriesListLength = function () {
    return this.$maxSeriesListLength;
  };
  /**
   * samplingMillis accessor.
   *
   * @returns {Number} millis for period used with sampling
   */


  BaseModel.prototype.samplingMillis = function () {
    return this.$samplingMillis;
  };
  /**
   * Returns the Model's getAvailableDataPoints
   * @returns {Number}
   */


  BaseModel.prototype.getAvailableDataPoints = function () {
    return this.$availableDataPoints;
  };
  /**
   * return the Array of unique ValueScales
   * @returns {Array.<module:nmodule/webChart/rc/model/BaseScale>}
   */


  BaseModel.prototype.valueScales = function () {
    return this.$valueScales;
  };
  /**
   * Accessor for Primary Value scale
   * @param [newPrimary] optionally set the primaryValueScale to an existing scale.
   * @returns {module:nmodule/webChart/rc/model/BaseScale}
   */


  BaseModel.prototype.primaryValueScale = function (newPrimary) {
    var that = this,
        valueScales = that.$valueScales,
        primary = that.$primaryValueScale,
        jq = that.$jq,
        primarySeries = that.primarySeries(),
        seriesList = that.$seriesList;

    if (newPrimary) {
      that.$primaryValueScale = newPrimary;
      modelUtil.sortSeriesList(that, seriesList, newPrimary);
      jq.trigger(chartEvents.VALUE_SCALE_CHANGED, newPrimary);
      return newPrimary;
    }

    if (primary) {
      return primary;
    }

    if (primarySeries) {
      return primarySeries.valueScale();
    }

    if (valueScales.length > 0 && valueScales[0]) {
      return valueScales[0];
    }
  };
  /**
   * Accessor for Second Value scale
   * @param [newSecondary] optionally set the secondValueScale to an existing scale.
   * @returns {module:nmodule/webChart/rc/model/BaseScale}
   */


  BaseModel.prototype.secondaryValueScale = function (newSecondary) {
    var that = this,
        secondary = that.$secondaryValueScale,
        jq = that.$jq,
        secondarySeries;

    if (newSecondary) {
      that.$secondaryValueScale = newSecondary;
      secondary = that.$secondaryValueScale;
      jq.trigger(chartEvents.VALUE_SCALE_CHANGED, newSecondary);

      if (newSecondary) {
        return newSecondary;
      }
    } else if (newSecondary === null) {
      //explicitly passing in null will reset the secondaryValueScale
      that.$secondaryValueScale = null;
      secondary = null;
    }

    if (secondary) {
      return secondary;
    }

    secondarySeries = that.secondarySeries();

    if (secondarySeries) {
      return secondarySeries.valueScale();
    }

    return null; //null is ok for secondarySeries, it is not required
  };
  /**
   * Get the primary data set, the first non-discrete series.
   * @returns {module:nmodule/webChart/rc/model/BaseSeries}
   */


  BaseModel.prototype.primarySeries = function () {
    var that = this,
        seriesList = that.$seriesList,
        i;

    for (i = 0; i < seriesList.length; i++) {
      if (!seriesList[i].isShade()) {
        return seriesList[i];
      }
    }

    for (i = 0; i < seriesList.length; i++) {
      if (seriesList[i].isShade()) {
        return seriesList[i];
      }
    }
  };
  /**
   * Get the secondary data set, the second non-discrete series with a different valueScale than primary.
   * @returns {module:nmodule/webChart/rc/model/BaseSeries}
   */


  BaseModel.prototype.secondarySeries = function () {
    var that = this,
        seriesList = that.$seriesList,
        primaryValueScale = that.primaryValueScale(),
        i;

    for (i = 0; i < seriesList.length; i++) {
      if (!seriesList[i].isShade() && seriesList[i].valueScale() !== primaryValueScale) {
        return seriesList[i];
      }
    }
  };
  /**
   * Return true if the model has the ord.
   * @param {String} ord
   * @returns {boolean}
   */


  BaseModel.prototype.hasOrd = function (ord) {
    var that = this,
        seriesList = that.$seriesList,
        i;

    for (i = 0; i < seriesList.length; ++i) {
      //NCCB-19660: some ordSchemes are coming across as lowercase, some are not, this corrects that during the comparison
      var seriesOrd = seriesList[i].ord();

      if (ord && seriesOrd && seriesOrd.toLowerCase && ord.toLowerCase && seriesOrd.toLowerCase() === ord.toLowerCase()) {
        return true;
      }
    }

    return false;
  };
  /**
   * Accessor for retrieving $seriesList
   * @return {Array.<BaseSeries>}
   */


  BaseModel.prototype.seriesList = function () {
    return this.$seriesList;
  };
  /**
   * Is the Model is considered initialized after completion of the first loadInfo Promise completion.
   * @return {Boolean}
   */


  BaseModel.prototype.isInitialized = function () {
    return this.$initialized;
  };
  /**
   * Is the Model reloading.
   * @return {Boolean}
   */


  BaseModel.prototype.isLoading = function () {
    return this.$loading;
  };
  /**
   * Does this model have any series with any points.
   * @return {Boolean}
   */


  BaseModel.prototype.hasPoints = function () {
    var that = this,
        seriesList = that.$seriesList,
        i;

    for (i = 0; i < seriesList.length; ++i) {
      if (seriesList[i].points().length > 0) {
        return true;
      }
    }

    return false;
  };
  /**
   * Load the info and the data
   * @param {baja.Subscriber} [subscriber]
   * @param {boolean} [completeInfoOnly] Defaults to false, when true, will return promise when just the info is completed.
   * You can know when the data is completed by looking at the promise returned by the data promise.
   * @returns {*}
   */


  BaseModel.prototype.load = function (subscriber, completeInfoOnly) {
    var that = this,
        jq = that.jq(); //give Series a change to load

    that.$stopped = false;
    that.$loading = true;
    var df = webChartUtil.deferred();
    that.$loadingDfs.push(df.promise());
    return that.loadInfo().then(function () {
      var valueScales = that.valueScales(),
          i;

      for (i = 0; i < valueScales.length; i++) {
        if (valueScales[i].isLocked()) {
          valueScales[i].setDomainFromOptions();
        }
      }

      that.$initialized = true;
      var loadDataPromise = that.loadData(subscriber);
      loadDataPromise.then(function () {
        return that.settings().redraw().then(function () {
          //fire event after data is graphed successfully and page is redrawn
          var i,
              seriesList = that.seriesList();

          for (i = 0; i < seriesList.length; ++i) {
            if (!seriesList[i].isLoaded()) {
              return; //don't trigger loaded yet
            }
          }

          that.$loading = false; //loading is complete when data is all loaded and page has been drawn

          that.$loadingDfs = []; //get rid of existing deferred

          jq.trigger(chartEvents.MODEL_DATA_LOADED);
        });
      })["finally"](function () {
        df.resolve(); //loading complete
      });

      if (!completeInfoOnly) {
        return loadDataPromise;
      } else {
        return [loadDataPromise]; //return promise for load, but don't wait on it
      }
    });
  };
  /**
   * Load the model's info. No Point Data is known at this point.
   * @returns {*}
   */


  BaseModel.prototype.loadInfo = function () {
    var that = this,
        seriesList = that.$seriesList,
        primaryLoad,
        secondaryLoad,
        jq = that.jq();
    jq.trigger(chartEvents.MODEL_START_LOAD);
    primaryLoad = $.map(seriesList, function (series) {
      if (series.isPrimaryLoad()) {
        return series.loadInfo();
      }
    });
    return Promise.all(primaryLoad).then(function () {
      //allow schedules to load after everything else is loaded when time is set to 'auto'
      secondaryLoad = $.map(seriesList, function (series) {
        if (!series.isPrimaryLoad()) {
          return series.loadInfo();
        }
      });
      return Promise.all(secondaryLoad);
    }).then(function () {
      modelUtil.generateUniqueShortestDisplayNames(that);
      return that.$settings.graphData();
    });
  };
  /**
   * Load the model's data.
   * @param {baja.Subscriber} [subscriber]
   * @returns {*}
   */


  BaseModel.prototype.loadData = function (subscriber) {
    var that = this,
        seriesList = that.$seriesList,
        primaryLoad,
        secondaryLoad;
    primaryLoad = $.map(seriesList, function (series) {
      if (series.isPrimaryLoad()) {
        return series.loadData(subscriber);
      }
    });
    return Promise.all(primaryLoad).then(function () {
      //allow schedules to load after everything else is loaded when time is set to 'auto'
      secondaryLoad = $.map(seriesList, function (series) {
        if (!series.isPrimaryLoad()) {
          return series.loadData(subscriber);
        }
      });
      return Promise.all(secondaryLoad);
    })["catch"](function (err) {
      if (err !== chunkUtil.ABORT_MESSAGE) {
        throw err;
      }
    });
  };
  /**
   * Subscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   */


  BaseModel.prototype.subscribe = function (subscriber) {
    var seriesList = this.$seriesList,
        i;

    for (i = 0; i < seriesList.length; ++i) {
      seriesList[i].subscribe(subscriber);
    }
  };
  /**
   * Unsubscribe to the series.
   *
   * @param {baja.Subscriber} [subscriber]
   * @return {Promise}
   */


  BaseModel.prototype.unsubscribe = function (subscriber) {
    var seriesList = this.$seriesList,
        promises = [],
        i;

    for (i = 0; i < seriesList.length; ++i) {
      promises[i] = seriesList[i].unsubscribe(subscriber);
    }

    return Promise.all(promises);
  };
  /**
   * If the BaseModel is loading, give a callback for when loading is complete. If done loading, this will resolve
   * right away.
   * @return {Promise}
   */


  BaseModel.prototype.loadingComplete = function () {
    if (this.$loadingDfs.length > 0) {
      return Promise.all(this.$loadingDfs);
    } else {
      return modelUtil.resolvedPromise();
    }
  };
  /**
   * Reload all the data (but keep the current Settings).
   * @param {baja.Subscriber} [subscriber]
   * @return {*}
    */


  BaseModel.prototype.reloadAll = function (subscriber, completeInfoOnly) {
    var that = this; //one reload allowed at a time since two reloads could try to add the same data twice

    return modelUtil.resolvedPromise().then(function () {
      if (that.$loading) {
        return that.stop();
      }

      return modelUtil.resolvedPromise();
    }).then(function () {
      that.unloadSeriesList();
      return that.load(subscriber, completeInfoOnly);
    });
  };
  /**
   * Unload the data from the current seriesList.
   * Triggers a remove data request event on the ChartWidget.
   */


  BaseModel.prototype.unloadSeriesList = function () {
    var that = this,
        jq = that.jq(),
        seriesList = that.$seriesList,
        i;

    for (i = 0; i < seriesList.length; ++i) {
      seriesList[i].unload();
    }

    that.setOverMaxCapacity(false);
    jq.trigger(chartEvents.REMOVE_DATA_REQUEST_EVENT);
  };
  /**
   * Add a number of Series to the Model.
   *
   * @param {baja.Subscriber} [subscriber] The subscriber instance.
   * @param {Array|String|Object} seriesParams An array of
   * Series parameter objects used to create the Series data. Each
   * object must declare an 'ord' property. A value property can also be
   * optionally declared as a 'value' property. This can also be an ORD
   * Strong or a singular Object.
   *
   * @returns {Promise} a promise that's resolved once the new Series has been added.
   * An array of new Series data is passed along the promise.
   */


  BaseModel.prototype.addSeries = function (subscriber, seriesParams) {
    var that = this,
        jq = that.jq();
    return seriesFactory.make(that, subscriber, seriesParams).then(function (series) {
      var limitReached = that.$seriesList.length >= that.$maxSeriesListLength;

      if (!series || series.length === 0 && limitReached) {
        // If the limit was previously reached, then display a proper error message to the user
        if (limitReached) {
          jq.trigger(chartEvents.DISPLAY_ERROR, webChartUtil.lex.get("webChart.ChartAlreadyFullError", that.$maxSeriesListLength));
        }

        return;
      } // Only add the Series if it's not already available.


      var useful = false,
          alreadyAddedList = [],
          tooManyList = [],
          i;

      for (i = 0; i < series.length; ++i) {
        if (that.hasOrd(series[i].ord())) {//NCCB-19660: sometime this provides an unnecessary warning message
          //alreadyAddedList.push(series[i].ord());
        } else if (that.$seriesList.length >= that.$maxSeriesListLength) {
          tooManyList.push(series[i].ord());
        } else {
          that.$seriesList.push(series[i]);
          series[i].added();
          jq.trigger(chartEvents.SERIES_ADDED, [that, series[i]]);
          useful = true;
        }
      }

      if (!useful) {
        var lexText, listText;

        if (alreadyAddedList.length) {
          listText = alreadyAddedList.join('\n');
          lexText = webChartUtil.lex.get("webChart.AlreadyPresentError", listText);
        } else if (tooManyList.length) {
          listText = tooManyList.join('\n');
          lexText = webChartUtil.lex.get("webChart.TooManyChartsError", that.$maxSeriesListLength, listText);
        }

        if (lexText) {
          jq.trigger(chartEvents.DISPLAY_ERROR, lexText);
        }
      }

      return series;
    })["catch"](function (err) {
      //Unresolved Errors are useful to display if they made it this far
      if (err.toString().indexOf("Unresolved")) {
        jq.trigger(chartEvents.DISPLAY_ERROR, err.toString().split("\n")[0]);
      }

      webChartUtil.log(err);
      throw err;
    });
  };
  /**
   * Remove a series. This also ensures the that value scales are up-to-date with the changes.
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series The series to remove.
   * @returns {boolean} true if remove was successful
   */


  BaseModel.prototype.removeSeries = function (series) {
    var that = this,
        seriesList = that.$seriesList,
        settings = that.$settings,
        index = seriesList.indexOf(series),
        valueScales = that.$valueScales,
        valueScale = series.valueScale(),
        valueScalesSeriesList = valueScale.seriesList(),
        seriesIndex = valueScalesSeriesList.indexOf(series),
        scaleIndex = valueScales.indexOf(valueScale); //Return false, if user tries to remove a series that was already removed.

    if (index < 0) {
      return false;
    }

    seriesList.splice(index, 1); //remove series from valueScales

    valueScalesSeriesList.splice(seriesIndex, 1); //see if valueScale needs to be removed

    if (!valueScalesSeriesList.length) {
      valueScales.splice(scaleIndex, 1);

      if (that.$primaryValueScale === valueScale) {
        that.$primaryValueScale = valueScalesSeriesList[0]; //find a good replacement if available

        if (that.$secondaryValueScale === valueScalesSeriesList[0]) {
          that.$secondaryValueScale = null;
        }
      }

      if (that.$secondaryValueScale === valueScale) {
        that.$secondaryValueScale = null;
      } //no more valueScales, so add back default


      if (!valueScales.length) {
        valueScales.push(new ValueScale(this));
      }

      settings.removeValueScaleSettings(valueScale);
    }

    var subscriber = settings.getSubscriber();
    series.unsubscribe(subscriber);
    settings.removeSeriesSettings(series);
    return true;
  };
  /**
   * Remove All Series
   */


  BaseModel.prototype.removeAll = function () {
    this.removeOtherSeries(null);
  };
  /**
   * Remove all other series except for the one passed in.
   * This also ensures the that value scales are up-to-date with the changes.
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series The series to not remove.
   */


  BaseModel.prototype.removeOtherSeries = function (series) {
    var that = this,
        seriesList = that.$seriesList,
        length = seriesList.length - 1,
        i;

    for (i = length; i >= 0; --i) {
      if (series !== seriesList[i]) {
        that.removeSeries(seriesList[i]);
      }
    }
  };
  /**
   * loadFromJson.
   *
   * @param {Object} chartJson
   * @param {module:nmodule/webChart/rc/ChartSettings} chartSettings
   */


  BaseModel.prototype.loadFromJson = function (chartJson, chartSettings) {
    this.$settings = chartSettings;
  };
  /**
   * Settings Accessor.
   *
   * @return {baja.Component} Settings available from chart file
   * is being added too.
   */


  BaseModel.prototype.settings = function () {
    return this.$settings;
  };

  BaseModel.prototype.isStopped = function () {
    return this.$stopped;
  };
  /**
   * Stop the BaseModel from loading; if the Model is loading, return a promise when the stop is completed
   * @return {Promise}
   */


  BaseModel.prototype.stop = function () {
    var that = this,
        i;
    that.$stopped = true;
    that.$loading = false;
    return that.loadingComplete().then(function () {
      that.$loadingDfs = [];

      for (i = 0; i < that.$seriesList.length; ++i) {
        that.$seriesList[i].$loading = false;
      } //that.setLive(false); //stop subscriptions


      that.jq().trigger(chartEvents.MODEL_DATA_STOPPED);
    });
  };
  /**
   * Save the model to some JSON.
   *
   * @param {Object} chartJson The object information
   * is being added too.
   * @param {Object} optionInfo optional
   * @param {baja.DynamicEnum} [optionInfo.ordType=0] defaults to ordinal 0 with tag 'absolute' the other option is 'relative',
   * @param {String} [optionInfo.baseOrd] if ordType is relative, the base used for relativizing the series ords.
   */


  BaseModel.prototype.saveToJson = function (chartJson, optionInfo) {
    var that = this,
        i;
    chartJson.model = {
      series: []
    };

    for (i = 0; i < that.$seriesList.length; ++i) {
      chartJson.model.series.push({});
      that.$seriesList[i].saveToJson(chartJson.model.series[i], optionInfo);
    }
  };
  /**
   * Get the min and max timestamps for all the series data
   * @returns {Array.<Number>}
   */


  BaseModel.prototype.getTimeExtent = function () {
    var that = this,
        seriesList = that.$seriesList,
        min,
        max;
    min = d3.min(seriesList, function (d) {
      return d3.min(d.points(), function (p) {
        return p.x;
      });
    });
    max = d3.max(seriesList, function (d) {
      return d3.max(d.points(), function (p) {
        return p.x;
      });
    });
    return [min, max];
  };
  /**
   * Call the callback on each valueScale
   * @param {Function} callback
   * @returns {*}
   */


  BaseModel.prototype.mapValueScales = function (callback) {
    return $.map(this.$valueScales, callback);
  };
  /**
   * Coordinate a new or existing value scale based on value scales available. Scales with the same
   * units will be reused.
   * @param {module:nmodule/webChart/rc/model/BaseSeries} series
   * @returns {Promise.<module:nmodule/webChart/rc/model/BaseScale>}
   */


  BaseModel.prototype.makeValueScale = function (series) {
    var that = this,
        scales = that.$valueScales,
        newScale,
        i,
        units = series.units(),
        seriesList,
        index,
        j,
        mismatch;

    for (i = 0; i < scales.length; ++i) {
      if (scales[i].units().equals(units)) {
        //TODO: combine convertible scales
        seriesList = scales[i].seriesList();
        index = seriesList.indexOf(series);

        if (index !== -1) {
          return Promise.resolve(scales[i]); //already added
        }

        mismatch = false;

        for (j = 0; j < seriesList.length; ++j) {
          if (seriesList[j].isShade() !== series.isShade() || seriesList[j].isDiscrete() !== series.isDiscrete() || //no longer match discrete with not discrete so that numerics don't show enum ordinal names
          seriesList[j].isDiscrete() && series.isDiscrete() && !modelUtil.sameRanges(series, seriesList[j])) {
            mismatch = true;
          }
        }

        if (mismatch) {
          continue;
        }

        scales[i].seriesList().push(series); //add series to scale

        if (scales[i].seriesList().length === 1) {
          return scales[i].initialize();
        }

        return Promise.resolve(scales[i]);
      }
    }

    newScale = new ValueScale(that, series);

    if (scales[0] && !scales[0].units() && !scales[0].primarySeries()) {
      scales.splice(0, 1); //remove unused fallback scale
    }

    scales.push(newScale);
    return newScale.initialize();
  };
  /**
   * @returns {Promise}
   */


  BaseModel.prototype.destroy = function () {
    return this.unsubscribe(this.settings().getSubscriber());
  };

  return BaseModel;
});
