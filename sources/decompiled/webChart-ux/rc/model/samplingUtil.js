/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */
define(['moment', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/webChartUtil'], function (moment, chartEvents, modelUtil, webChartUtil) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A set of utility functions for common things with sampling within a model.
   *
   * @exports nmodule/webChart/rc/model/samplingUtil
   */

  var samplingUtil = {};
  /**
   * Determine information needed for sampling
   * @param {BaseModel} model
   * @return {{allMinTime: (Number), bestSlice: (Number), allFocusPoints: Array, allMaxFocusLength: (Number)}}
   */

  samplingUtil.calculateSeriesSamplingStats = function (model) {
    var seriesList = model.seriesList(),
        i,
        focusPoints,
        slice,
        minTime,
        maxTime,
        duration,
        bucketSize,
        minSlice = Number.MAX_VALUE,
        allMinTimeDiff = Number.MAX_VALUE,
        bestSlice,
        maxSliceNeeded = false,
        maxSliceNeededFromSamplingSize = false,
        maxSlice = 0,
        allMinTime = Number.MAX_VALUE,
        allFocusPoints = [],
        allMaxFocusLength = 0,
        sampleSize = model.sampleSize(),
        bars = modelUtil.getBars(seriesList).length,
        j; //configure sample size, minimums, and maxes

    for (i = 0; i < seriesList.length; i++) {
      focusPoints = seriesList[i].focusPoints(true).filter(function (point) {
        return !point.interpolated;
      });
      allFocusPoints[i] = focusPoints;
      allMaxFocusLength = Math.max(allMaxFocusLength, focusPoints.length);

      if (focusPoints.length > 1) {
        minTime = focusPoints[0].x.getTime();
        maxTime = focusPoints[focusPoints.length - 1].x.getTime();
        duration = maxTime - minTime;
        bucketSize = Math.min(sampleSize, focusPoints.length - 1); //maximize slice when focus points are of a large amount

        slice = duration / bucketSize;

        if (sampleSize < focusPoints.length) {
          maxSlice = Math.max(maxSlice, slice);
          maxSliceNeeded = true;
          maxSliceNeededFromSamplingSize = true;
        } else {
          //minimize slice when focus points are low
          minSlice = Math.min(minSlice, slice);
        }

        allMinTime = Math.min(allMinTime, minTime);

        if (seriesList[i].isBar()) {
          //more expensive, so limit to bars
          for (j = 1; j < focusPoints.length; j++) {
            allMinTimeDiff = Math.min(allMinTimeDiff, focusPoints[j].x.getTime() - focusPoints[j - 1].x.getTime());
          }
        }
      } else if (focusPoints.length === 1) {
        //even if we cant rollup up, ensure minTime is checked
        minTime = focusPoints[0].x.getTime();
        allMinTime = Math.max(allMinTime, minTime);
      }
    } //get the minimal bar width


    if (bars > 0) {
      var timeScale = model.timeScale().scale(),
          width = timeScale.range()[1] - timeScale.range()[0];

      if (width) {
        bucketSize = parseInt(width / (bars * 10 + 4));
        minSlice = (timeScale.domain()[1] - timeScale.domain()[0]) / bucketSize;

        if (maxSliceNeededFromSamplingSize) {
          maxSlice = Math.max(minSlice, maxSlice);
        } else if (minSlice < allMinTimeDiff) {
          maxSliceNeeded = true;
          maxSlice = allMinTimeDiff;
        }
      }
    }

    if (maxSliceNeeded) {
      bestSlice = maxSlice;
    } else {
      bestSlice = minSlice;
    }

    if (bestSlice === 0) {
      webChartUtil.trace("slice is zero");
    }

    return {
      allMinTime: allMinTime,
      bestSlice: bestSlice,
      allFocusPoints: allFocusPoints,
      allMaxFocusLength: allMaxFocusLength
    };
  };
  /**
   * Get the max point of points for all series
   * @param {BaseModel} model
   * @returns {number}
   */


  samplingUtil.getMaxPoints = function (model) {
    var seriesList = model.seriesList(),
        max = 0,
        i;

    for (i = 0; i < seriesList.length; i++) {
      max = Math.max(max, seriesList[i].points().length);
    }

    return max;
  };
  /**
   * Clear the cache for all sampling
   * @param model
   */


  samplingUtil.clearSamples = function (model) {
    var seriesList = model.seriesList(),
        i;

    for (i = 0; i < seriesList.length; i++) {
      seriesList[i].setSamplingPoints(null); //clear out sampling points
    }
  };
  /**
   * Determine if sampling should be changed based on the maxFocusPointsLength
   * @param {BaseModel} model
   * @param {Number} maxFocusPointsLength
   */


  samplingUtil.configureAutoSample = function (model, maxFocusPointsLength) {
    var sampling = model.isSampling(),
        autoSampling = model.isAutoSampling(),
        sampleSize = model.sampleSize();
    var turnAutoSamplingOn = maxFocusPointsLength > sampleSize || webChartUtil.getMaxSamplingSize() < maxFocusPointsLength;

    if (autoSampling && sampling && maxFocusPointsLength < sampleSize) {
      model.setSampling(false, chartEvents.IGNORE_MODIFY);
    } else if (autoSampling && !sampling && turnAutoSamplingOn) {
      model.setSampling(true, chartEvents.IGNORE_MODIFY);
    }
  };
  /**
   * Provide a simple Rollup algorithm for choosing a bucket size and applying it the array.
   * @param {BaseSeries} series
   * @param {Array.<Object>} array
   * @param {moment} startTime
   * @param {Number} duration (in milliseconds)
   * @param {BaseModel} model
   * @returns {Array}
   */


  samplingUtil.rollup = function (series, array, startTime, duration, model) {
    var index = 0,
        newArray = [],
        startSkip = false,
        endSkip = false,
        discrete = series.isDiscrete(),
        newValueSum,
        newRecordCount,
        newStatus,
        newPoint,
        point,
        jumpNeeded,
        endTime,
        samplingType = model.samplingType(),
        addY = samplingType === "average" || samplingType === "sum",
        variableSamplingPeriod = modelUtil.hasVariableSamplingPeriod(model, duration);
    startTime = modelUtil.getStartTime(model, startTime, duration);
    endTime = modelUtil.getEndTime(model, startTime, duration);

    while (index < array.length) {
      newValueSum = 0;
      newRecordCount = 0;
      newStatus = 0;
      point = array[index]; // eslint-disable-next-line no-unmodified-loop-condition

      while (index < array.length && (point.x.getTime() < endTime.toDate().getTime() || variableSamplingPeriod && point.x.getTime() <= endTime.toDate().getTime())) {
        if (point.interpolated) {
          index++;
          point = array[index];
          continue;
        }

        if (!newRecordCount && point.skip) {
          startSkip = true;
        }

        endSkip = point.skip;

        if (!point.skip) {
          //ignore skips for rollup computations
          if (addY || !newRecordCount) {
            newValueSum += point.y;
          } else if (samplingType === "max") {
            newValueSum = Math.max(point.y, newValueSum);
          } else if (samplingType === "min") {
            newValueSum = Math.min(point.y, newValueSum);
          } else {
            throw new Error("unknown sampling type");
          }

          newRecordCount++;
          newStatus |= point.status; //combine the status flags for rollup
        }

        index++;
        point = array[index];
      }

      if (newRecordCount > 0) {
        newPoint = []; //show rollup time at the start of the rollup time

        newPoint.x = new Date(startTime);

        if (samplingType === "average") {
          newPoint.y = newValueSum / newRecordCount; //average for now
        } else {
          newPoint.y = newValueSum;
        }

        if (discrete) {
          newPoint.y = newPoint.y + 0.5 | 0; //round to closest ordinal for discrete
        }

        newPoint.status = newStatus;

        if (startSkip) {
          newArray.push(modelUtil.getSkipPoint(newPoint));
          startSkip = false;
        }

        newArray.push(newPoint);

        if (endSkip) {
          newArray.push(modelUtil.getSkipPoint(newPoint));
          endSkip = false;
        }

        if (!variableSamplingPeriod) {
          startTime = endTime;
        }
      } else if (point) {
        //nothing found so jump to next bucket to save some iterations in the while loop
        jumpNeeded = point.x.getTime() - startTime.toDate().getTime(); //jump in specific time

        jumpNeeded = jumpNeeded / duration | 0; //jump in bucket iterations

        jumpNeeded = jumpNeeded * duration; //jump that follows nice interval duration

        if (!variableSamplingPeriod) {
          startTime = modelUtil.safeMomentAdd(startTime, jumpNeeded);
        }
      }

      if (variableSamplingPeriod) {
        startTime = modelUtil.safeMomentAdd(modelUtil.getEndTime(model, startTime, duration), 1);
        endTime = modelUtil.getEndTime(model, startTime, duration);
      } else {
        endTime = modelUtil.safeMomentAdd(startTime, duration);
      }
    }

    if (newArray.length && array.length) {
      var lastNewPoint = newArray[newArray.length - 1],
          lastOldPoint = array[array.length - 1];

      if (lastOldPoint.interpolated) {
        newArray.push({
          x: lastOldPoint.x,
          y: lastNewPoint.y,
          status: lastNewPoint.status,
          interpolated: true
        });
      }
    }

    return newArray;
  };
  /**
   * Given that an array is already ordered and a searchValue, find the index where the value should reside
   */


  samplingUtil.getEdgeIndex = function (array, searchValue, mapFunction) {
    var minIndex = 0,
        maxIndex = array.length - 1,
        currentIndex,
        currentElement,
        resultIndex;

    while (minIndex <= maxIndex) {
      resultIndex = currentIndex = (minIndex + maxIndex) / 2 | 0;
      currentElement = array[currentIndex];

      if (mapFunction(currentElement) < searchValue) {
        minIndex = currentIndex + 1;
      } else if (mapFunction(currentElement) > searchValue) {
        maxIndex = currentIndex - 1;
      } else {
        return currentIndex;
      }
    }

    if (minIndex === maxIndex) {
      return minIndex;
    }

    return resultIndex;
  };
  /**
   * Keep averaging adjacent indexes until the array length is small enough
   */


  samplingUtil.averageSplit = function (series, array, maxIndex) {
    if (array.length <= maxIndex) {
      return array;
    } //split size of array by 2 until it fits within maxIndex


    var i,
        newIndex,
        newArray = [];

    for (i = 0; i < array.length; i += 2) {
      newIndex = i / 2 | 0; //half of array length (floor)

      if (array[i + 1]) {
        newArray[newIndex] = samplingUtil.combineRecords(array[i], array[i + 1]);
      } else {
        newArray[newIndex] = array[i];
      }
    }

    return samplingUtil.averageSplit(series, newArray, maxIndex);
  };

  samplingUtil.combineRecords = function (p1, p2) {
    var newPoint = [];
    newPoint.x = new Date((p1.x.getTime() + p2.x.getTime()) / 2);
    newPoint.y = (p1.y + p2.y) / 2;
    newPoint.skip = p1.skip | p2.skip; //TODO: may want to avoid double count

    newPoint.status = p1.status | p2.status;
    return newPoint;
  };

  return samplingUtil;
});
