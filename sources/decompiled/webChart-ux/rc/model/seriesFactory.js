function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson and JJ Frankovich
 */

/* eslint-env browser */
define(['jquery', 'Promise', 'underscore', 'baja!', 'nmodule/webChart/rc/dragUtil', 'nmodule/webChart/rc/model/ServletSeries', 'nmodule/webChart/rc/model/PointSeries', 'nmodule/webChart/rc/model/ScheduleSeries', 'nmodule/webChart/rc/model/ExternalSeries', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webEditors/rc/fe/registry/StationRegistry', 'baja!history:HistoryExt,' + 'control:ControlPoint,' + 'control:StringPoint,' + 'schedule:ControlSchedule,' + 'schedule:StringSchedule,' + 'baja:OrdList,' + 'history:TrendRecord,' + 'history:StringTrendRecord,' + 'history:HistoryDevice,' + 'history:HistoryFolder'], function ($, Promise, _, baja, dragUtil, ServletSeries, PointSeries, ScheduleSeries, ExternalSeries, webChartUtil, StationRegistry) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A Factory for creating Series data.
   *
   * The Creation of the Series data is separate from the concrete Series implementations.
   * That's because one part of the factory might produce an array of Series data to
   * add to a Chart.
   *
   * Each factory method can return a promise if the creation of any data needs to
   * happen asynchronously. For instance, if creation requires any user input.
   *
   * Additional factory methods can be added via registration of new BIChartFactory.
   *
   * @exports nmodule/webChart/rc/model/seriesFactory
   */

  var exports = {};
  var factories,
      pointTypeSpec = "control:ControlPoint",
      excludePointTypeSpec = "control:StringPoint",
      virtualPointTypeSpec = "niagaraVirtual:NiagaraVirtualControlPoint",
      excludeVirtualPointTypeSpec = "niagaraVirtual:NiagaraVirtualStringPoint",
      scheduleTypeSpec = "schedule:ControlSchedule",
      excludeScheduleTypeSpec = "schedule:StringSchedule",
      ordListTypeSpec = "baja:OrdList",
      trendTypeSpec = "history:TrendRecord",
      excludeTrendTypeSpec = "history:StringTrendRecord",
      historyDeviceTypeSpec = "history:HistoryDevice",
      historyDatabaseTypeSpec = "history:LocalHistoryDatabase",
      historyFolderTypeSpec = "history:HistoryFolder",
      historyTimeQueryTypeSpec = "history:HistoryTimeQuery",
      historyDeltaTimeQueryTypeSpec = "history:HistoryDeltaQuery",
      chartFactoryTypeSpec = 'webChart:IChartFactory'; ////////////////////////////////////////////////////////////////
  // Util
  ////////////////////////////////////////////////////////////////

  /**
   * Adds a series if it has not already been added and return whether or
   * not a seires was added.
   * @param {Array.<module:nmodule/webChart/rc/model/ServletSeries>} newSeries the series that have been added
   * @param {Array.<module:nmodule/webChart/rc/model/ServletSeries>} res the series that can be added
   * @param {module:nmodule/webChart/rc/model/BaseModel} model the webChart model
   * @param {Object} params the object literal that contains the method's arguments
   * @param {string} params.stationName the local station name
   */

  function addSeries(newSeries, res, model, params) {
    var i,
        count = 0,
        maxCount = model.maxSeriesListLength();

    if (res) {
      res = $.isArray(res) ? res : [res];
      Object.keys(newSeries).forEach(function (series) {
        count++;
      });

      for (i = 0; i < res.length && count < maxCount; ++i) {
        var lookUpOrd = res[i].ord();

        if (lookUpOrd.indexOf("history:") !== -1) {
          lookUpOrd = webChartUtil.getPersistentHistoryOrd(lookUpOrd, params.stationName);

          if (lookUpOrd.indexOf("?") !== -1) {
            lookUpOrd = lookUpOrd.substring(0, lookUpOrd.indexOf("?"));
          }
        }

        if (!newSeries[lookUpOrd]) {
          count++;
          newSeries[lookUpOrd] = res[i]; //prevent double adds for same ord
        }
      }
    }
  }

  function runFactories(model, subscriber, seriesParam, newSeries, params) {
    return webChartUtil.resolveStationName().then(function (stationName) {
      params.stationName = stationName;
      return factories.reduce(function (prom, factory) {
        return prom.then(function () {
          return factory(model, subscriber, seriesParam, params).then(function (res) {
            addSeries(newSeries, res, model, params);
          });
        });
      }, Promise.resolve());
    });
  }

  function processChartJson(model, subscriber, chartJson) {
    var seriesJson,
        i,
        seriesParams = [],
        series,
        root; // eslint-disable-next-line promise/avoid-new

    return new Promise(function (resolve, reject) {
      // If there's series data then resolve all of the ORDs
      // to their values and create them using the
      // Series factory.
      if (chartJson && chartJson.model && chartJson.model.series && chartJson.model.series.length) {
        seriesJson = chartJson.model.series;

        for (i = 0; i < seriesJson.length; ++i) {
          // Handle relative ord
          // TODO NCCB-31381 may require changes to this code if we support more than just slot relativity.
          if (seriesJson[i].ord && seriesJson[i].ord.indexOf("slot:") > -1 && seriesJson[i].ord.indexOf("slot:/") < 0) {
            model.setRelative(true);

            if (!root) {
              if (typeof window.niagara !== 'undefined' && window.niagara.env && typeof window.niagara.env.getBaseOrd === 'function') {
                root = window.niagara.env.getBaseOrd();
              }

              if (!root) {
                var ords = dragUtil.getOrdsFromDrag(window.location.href, "text");
                root = ords[0];
              }
            } //don't attempt to make use relative on file base ord


            if (root && root.indexOf("file:") < 0) {
              seriesJson[i].ord = root + '|' + seriesJson[i].ord;
            }
          }

          seriesParams.push(seriesJson[i]);
        }

        exports.make(model, subscriber, seriesParams).then(function (newSeries) {
          series = newSeries;
          resolve(series);
        })["catch"](function (err) {
          webChartUtil.log(err);
          resolve(series);
        });
      } else {
        resolve();
      }
    });
  }
  /**
   *
   * @returns {Promise.<Array.<module:nmodule/webChart/rc/model/ExternalSeries>>}
   */


  function processCsvData(model, subscriber, csvData) {
    return ExternalSeries.factory(model, subscriber, {
      ord: "Test",
      value: csvData
    });
  }
  /**
   * Add a series based on the History Config.
   * @param {baja.Component} config the `history:HistoryConfig`
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {Array|String|Object} seriesParams
   */


  function addHistoryConfig(config, model, seriesParams) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      if (!config) {
        resolve();
        return;
      }

      var typeSpec = config.get("recordType"),
          ord = seriesParams.ord;

      if (!typeSpec) {
        typeSpec = config.get("typeSpec");
      }

      if (!typeSpec || typeSpec === historyDatabaseTypeSpec) {
        //TODO: history space goes through here to provide a blank chart
        resolve();
        return;
      }

      var params = webChartUtil.getParametersFromOrd(seriesParams.ord);

      if (params.timeRange && model.timeRange) {
        model.timeRange(params.timeRange);
      }

      if (params.delta && model.setDelta) {
        model.setDelta(params.delta);
      }

      baja.importTypes({
        typeSpecs: [String(typeSpec)],
        ok: function ok() {
          var type = baja.lt(String(typeSpec));

          if (type.is(trendTypeSpec) && !type.is(excludeTrendTypeSpec)) {
            baja.Ord.make(ord).resolve({
              ok: function ok() {
                return webChartUtil.resolvePersistentHistoryOrd(ord).then(function (persistentOrd) {
                  seriesParams.ord = persistentOrd;
                })["finally"](function () {
                  //this in in a finally in case the stationName lookup somehow fails
                  resolve(new ServletSeries(model, seriesParams));
                });
              },
              fail: function fail() {
                resolve(); //no permissions, just exclude
              }
            });
          } else if (type.is(historyDeviceTypeSpec)) {
            var deviceOrd = config.get("ord"),
                fullOrd = deviceOrd + "|bql:select deviceName, historyName",
                rows = [];
            baja.Ord.make(fullOrd).get({
              cursor: {
                each: function each() {
                  var historyOrd = baja.Ord.make("history:/" + this.get("deviceName") + "/" + this.get("historyName")),
                      addHistoryParams = {
                    ord: historyOrd
                  };
                  rows.push(addHistoryParams);
                },
                limit: -1
              }
            }).then(function () {
              return Promise.all(_.map(rows, function (row) {
                return addHistoryOrd(row, model);
              })).then(function (results) {
                results = webChartUtil.getCleanCopy(results);
                resolve(results);
              });
            })["catch"](reject);
          } else if (type.is(historyFolderTypeSpec)) {
            //TODO: how do we get this list from bajaScript?
            return webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getSourceList", ord).then(function (response) {
              var sources = response.sources;
              return sources.reduce(function (prom, source) {
                return prom.then(function (results) {
                  return addHistoryOrd({
                    ord: baja.Ord.make("history:" + source)
                  }, model).then(function (result) {
                    return results.concat(result);
                  });
                });
              }, Promise.resolve([])).then(function (allResults) {
                allResults = webChartUtil.getCleanCopy(allResults); //TODO: provide limit to number to add at a time with 'results.length = 10;'

                resolve(allResults);
              });
            });
          } else if (type.is(historyTimeQueryTypeSpec) || type.is(historyDeltaTimeQueryTypeSpec)) {
            var newSeriesParams = {
              ord: baja.Ord.make(seriesParams.ord.substring(0, seriesParams.ord.indexOf("?")))
            };
            resolve(addHistoryOrd(newSeriesParams, model));
          } else {
            resolve();
          }
        },
        fail: reject
      });
    });
  }

  function addHistoryOrd(seriesParams, model) {
    return seriesParams.ord.get().then(function (history) {
      return history.toConfig();
    }).then(function (config) {
      seriesParams.value = history;
      return addHistoryConfig(config, model, seriesParams);
    })["catch"](function (ignore) {//could be no permissions, just let promise resolve
    });
  }
  /* This function is similar to addHistoryOrd except that this function constructs the
   * seriesParams object and does not swallow exceptions.  Exceptions are not swallowed
   * because, when checking the component itself and not its children, the exception should
   * lead to checking the children for history configs and tags.
   * @param {String} id historyId
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {String} originalOrd
   * @param {Object} seriesParams
   * @return {Promise}
   */


  function addSeriesForHistoryId(id, model, originalOrd, seriesParams) {
    var ord = baja.Ord.make("history:" + id);
    var newSeriesParams = makeSeriesParams(seriesParams, ord, originalOrd);
    return newSeriesParams.ord.get().then(function (history) {
      newSeriesParams.value = history;
      return history.toConfig();
    }).then(function (config) {
      return addHistoryConfig(config, model, newSeriesParams);
    });
  }
  /**
   * For a ControlPoint, check for a local historyId tag, if none is found then fallback to Points for non-String points.
   * @param {module:nmodule/webChart/rc/model/BaseModel} model
   * @param {Object} seriesParams
   * @return {Promise}
   */


  function resolvePointSeries(model, seriesParams) {
    var historyIdFound = false;
    var originalOrd = seriesParams.ord;
    var value = seriesParams.value;
    return Promise.resolve().then(function () {
      if (value.isMounted()) {
        return value.lease().then(function () {
          return value.tags();
        }).then(function (tags) {
          var historyId = tags.get("n:history");

          if (historyId) {
            // #note1
            // if the history ID fails to be added, the error will send to the catch below that
            // will search the child components
            historyIdFound = true;
            return addSeriesForHistoryId(historyId, model, originalOrd, seriesParams);
          }
        });
      }
    }).then(function (results) {
      if (results) {
        return results;
      }

      if (!historyIdFound && value && value.getType && !value.getType().is(excludePointTypeSpec) && !value.getType().is(excludeVirtualPointTypeSpec)) {
        return [new PointSeries(model, seriesParams)];
      }
    });
  } ////////////////////////////////////////////////////////////////
  // Factory Implementation
  ////////////////////////////////////////////////////////////////


  factories = [// Handle a basic history ORD.
  function historyFactory(model, subscriber, seriesParam) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      var ord = seriesParam.ord,
          value = seriesParam.value;

      if (ord && ord.indexOf("history:") > -1 && value && value.toConfig) {
        value.toConfig().then(function (config) {
          resolve(addHistoryConfig(config, model, seriesParam));
        })["catch"](function (e) {
          reject(e);
        });
      } else {
        resolve();
      }
    });
  }, // Handle a point with history extensions directly under it, or a Niagara proxy point.
  function pointFactory(model, subscriber, seriesParam) {
    var originalOrd = seriesParam.ord; // eslint-disable-next-line promise/avoid-new

    return new Promise(function (resolve, reject) {
      var value = seriesParam.value,
          historyExts;

      function checkNiagaraProxyExt(proxyExt) {
        function loadFromNiagaraProxyExt() {
          if (proxyExt.getType().is("niagaraDriver:NiagaraProxyExt")) {
            return proxyExt.rpc("fetchRemoteTags", ["n:history"]).then(function (tagMap) {
              if (tagMap && tagMap["n:history"]) {
                var historyId = tagMap["n:history"],
                    ord = baja.Ord.make("history:" + historyId),
                    sourceParams = makeSeriesParams(seriesParam, ord, originalOrd);
                return sourceParams.ord.get().then(function (history) {
                  return history.toConfig();
                }).then(function (config) {
                  sourceParams.value = history;
                  resolve(addHistoryConfig(config, model, sourceParams));
                });
              } else {
                // If no remote history id found for the Niagara proxy point, then use the resolvePointSeries
                resolve(resolvePointSeries(model, seriesParam));
              }
            })["catch"](function (ignore) {
              // If any errors trying to resolve the history for the Niagara proxy point,
              // then revert to resolvePointSeries
              resolve(resolvePointSeries(model, seriesParam));
            });
          } else {
            resolve(resolvePointSeries(model, seriesParam));
          }
        }

        if (!baja.lt("niagaraDriver:NiagaraProxyExt")) {
          // Need to lazily load this type
          return baja.importTypes(["niagaraDriver:NiagaraProxyExt"]).then(function () {
            return loadFromNiagaraProxyExt();
          })["catch"](function (ignore) {
            // If any errors trying to load the type,
            // then revert to using the live point.
            resolve(resolvePointSeries(model, seriesParam));
          });
        } else {
          return loadFromNiagaraProxyExt();
        }
      }

      if (value && typeof value.getType === "function" && value.getType().is(pointTypeSpec)) {
        historyExts = value.getSlots().is("history:HistoryExt").toValueArray(); // If the point has some history extensions then query them for the
        // history config.

        if (historyExts.length) {
          return baja.Component.lease(historyExts).then(function () {
            // Find all of the history configs for the enabled extensions
            // and lease them.
            var configs = [],
                i;

            for (i = 0; i < historyExts.length; ++i) {
              if (historyExts[i].getEnabled()) {
                configs.push(historyExts[i].getHistoryConfig());
              }
            }

            return baja.Component.lease(configs).then(function () {
              return configs;
            });
          }).then(function (configs) {
            return Promise.all(_.map(configs, function (config) {
              var addParams = makeSeriesParams(seriesParam, "history:" + config.getId(), originalOrd);
              return addHistoryConfig(config, model, addParams);
            })).then(function (results) {
              results = webChartUtil.getCleanCopy(results);

              if (results.length) {
                resolve(results);
              } else {
                return checkNiagaraProxyExt(value.getProxyExt());
              }
            });
          });
        } else {
          return checkNiagaraProxyExt(value.getProxyExt());
        }
      } else {
        resolve();
      }
    });
  }, // Handle a Niagara virtual point
  function virtualPointFactory(model, subscriber, seriesParam) {
    var originalOrd = seriesParam.ord; // eslint-disable-next-line promise/avoid-new

    return new Promise(function (resolve, reject) {
      var value = seriesParam.value;

      function checkForNiagaraVirtualPoint() {
        if (value && typeof value.getType === "function" && value.getType().is(virtualPointTypeSpec)) {
          return value.rpc("fetchRemoteTags", ["n:history"]).then(function (tagMap) {
            if (tagMap && tagMap["n:history"]) {
              var historyId = tagMap["n:history"],
                  ord = baja.Ord.make("history:" + historyId),
                  sourceParams = makeSeriesParams(seriesParam, ord, originalOrd);
              return sourceParams.ord.get().then(function (history) {
                return history.toConfig();
              }).then(function (config) {
                sourceParams.value = history;
                resolve(addHistoryConfig(config, model, sourceParams));
              });
            } else {
              resolve(resolvePointSeries(model, seriesParam));
            }
          })["catch"](function (ignore) {
            resolve(resolvePointSeries(model, seriesParam));
          });
        } else {
          resolve();
        }
      }

      if (!baja.lt(virtualPointTypeSpec) || !baja.lt(excludeVirtualPointTypeSpec)) {
        // Need to lazily load this type
        return baja.importTypes([virtualPointTypeSpec, excludeVirtualPointTypeSpec]).then(function () {
          return checkForNiagaraVirtualPoint();
        })["catch"](function (ignore) {
          resolve();
        });
      } else {
        return checkForNiagaraVirtualPoint();
      }
    });
  }, // Handle a non-point component.
  // First it will look to see if the component has explicitly set a history id via the
  // "n:history" tag. If that fails, then we can query all children for histories and add them
  // directly to the chart.
  function queryFactory(model, subscriber, seriesParam) {
    return Promise["try"](function () {
      var value = seriesParam.value;
      var originalOrd = seriesParam.ord; // Uses BQL to search child components for history configs and n:history tags

      function addChildrenHistories() {
        return Promise["try"](function () {
          // query approach will now attempt to find all children of the component with a
          // HistoryConfig or an n:history tag, there can be up to 10 histories found this way and
          // they could be at any depth
          // NOTE: In order for Niagara Virtual components (ie. Niagara virtual folders) to also
          // be supported, we added an RPC call for that specific use case (see
          // BNiagaraVirtualComponent#findDescendantHistories in the niagaraVirtual-rt module).
          // If these BQL queries ever need to be changed in the future, please also update the
          // corresponding BQL queries in that RPC method to reflect the changes.
          var bqlHistoryConfig = baja.Ord.make({
            base: value.getNavOrd(),
            child: "bql:select id, parent.parent.slotPath, recordType, typeSpec, ord from history:HistoryConfig stop where parent.enabled"
          }),
              bqlHistoryTags = baja.Ord.make({
            base: value.getNavOrd(),
            child: "bql:select n$243ahistory, slotPath from baja:Component where n$243ahistory != null"
          }),
              maxSeries = model.maxSeriesListLength(),
              configRows = [],
              configResults = [],
              tagRows = []; // Check for the Niagara virtual case and use a special RPC call in that case

          if (value.getType().is("niagaraVirtual:NiagaraVirtualComponent")) {
            return webChartUtil.rpc("type:niagaraVirtual:NiagaraVirtualComponent", "findDescendantHistories", value.getNavOrd().toString(), maxSeries)["catch"](function (err) {
              // If any errors occur when trying to run the RPC call to find
              // virtual descendants, then suppress the error and continue.
              // This is expected to happen when the remote station is 4.6
              // or earlier.
              webChartUtil.trace("findDescendantHistories RPC call failed on virtual (expected if remote station 4.6 or earlier): " + err);
            }).then(function (historyIds) {
              if (!historyIds) {
                // This can happen if an error occurred (see above), so
                // propogate an empty array to skip this factory gracefully.
                historyIds = [];
              }

              return Promise.all(historyIds.map(function (historyId) {
                return addHistoryOrd({
                  ord: baja.Ord.make("history:" + historyId)
                }, model);
              }));
            });
          } else {
            // Non-virtual case
            return bqlHistoryConfig.get({
              cursor: {
                each: function each(row) {
                  configRows.push(row);
                }
              },
              limit: maxSeries,
              offset: 0
            }).then(function (table) {
              return Promise.all(configRows.map(function (row) {
                var ord = "history:" + row.get("id");
                var originalOrd = row.get("parent$2eparent$2eslotPath");
                var historySeriesParams = makeSeriesParams(seriesParam, ord, originalOrd);
                return addHistoryConfig(row, model, historySeriesParams);
              }));
            }).then(function (results) {
              configResults = webChartUtil.getCleanCopy(results);

              if (configResults.length >= maxSeries) {
                // already found the maximum number of additional series that we can accept
                return configResults;
              } else {
                // check for history tags on child components to add to the chart
                return bqlHistoryTags.get({
                  cursor: {
                    each: function each(row) {
                      tagRows.push(row);
                    }
                  },
                  // the number of history tags to retrieve is limited by the history configs
                  // already found
                  limit: maxSeries - configResults.length,
                  offset: 0
                }).then(function (table) {
                  return Promise.all(tagRows.map(function (row) {
                    var ord = baja.Ord.make("history:" + row.get("n$243ahistory"));
                    var originalOrd = baja.Ord.make(row.get("slotPath"));
                    return addHistoryOrd({
                      ord: ord,
                      originalOrd: originalOrd
                    }, model);
                  }));
                }).then(function (results) {
                  var tagResults = webChartUtil.getCleanCopy(results);
                  return configResults.concat(tagResults);
                });
              }
            });
          }
        });
      }

      if (value && typeof value.getType === "function" && value.getType().isComponent() && !value.getType().is(pointTypeSpec) && !value.getType().is(scheduleTypeSpec)) {
        // First see if the component has a n:history tag and use it if a history can be found.
        if (value.isMounted()) {
          return value.lease().then(function () {
            return value.tags();
          }).then(function (tags) {
            var historyId = tags.get("n:history");

            if (historyId) {
              // #note1
              // if the history ID fails to be added, the error will send to the catch below that
              // will search the child components
              return addSeriesForHistoryId(historyId, model, originalOrd, seriesParam);
            } else {
              return addChildrenHistories();
            }
          })["catch"](function (err) {
            // This catch cannot be removed because of #note1 above.
            webChartUtil.log(err);
            return addChildrenHistories();
          });
        } else {
          return addChildrenHistories();
        }
      }
    });
  }, // Handle loading a chart from a file.
  function chartFileAjaxFactory(model, subscriber, seriesParams) {
    var uri,
        ord = seriesParams.ord,
        value = seriesParams.value;

    if (!webChartUtil.isFileUri(ord) && ord !== "") {
      return Promise.resolve();
    }

    if (ord && !ord.match(/chart$/i)) {
      return Promise.resolve();
    } // If we've already got the JSON then process it.


    if (_typeof(value) === "object") {
      return processChartJson(model, subscriber, value);
    }

    uri = webChartUtil.isFileOrd(ord) ? webChartUtil.getFileUri(ord) : ord;
    return Promise.resolve($.ajax(uri, {
      type: "GET",
      contentType: "application/json",
      dataType: "json"
    })).then(function (chartJson) {
      return processChartJson(model, subscriber, chartJson);
    });
  }, // Handle loading a chart from a file.
  function csvFileAjaxFactory(model, subscriber, seriesParams) {
    var uri,
        ord = seriesParams.fileName,
        //TODO
    value = seriesParams.value;

    if (!ord || !ord.match(/csv$/i)) {
      return Promise.resolve();
    } // If we've already got the JSON then process it.


    if (_typeof(value) === "object") {
      return processCsvData(model, subscriber, value);
    }

    uri = webChartUtil.isFileOrd(ord) ? webChartUtil.getFileUri(ord) : ord;
    return Promise.resolve($.ajax(uri, {
      type: "GET",
      contentType: "text/csv",
      dataType: "csv"
    })).then(function (csvData) {
      return processCsvData(model, subscriber, csvData);
    });
  }, // Handle schedules
  function scheduleFactory(model, subscriber, seriesParams) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve) {
      var value = seriesParams.value;

      if (value && typeof value.getType === "function" && value.getType().is(scheduleTypeSpec) && !value.getType().is(excludeScheduleTypeSpec)) {
        resolve([new ScheduleSeries(model, seriesParams)]);
      } else {
        resolve();
      }
    });
  }, // Handle OrdList
  function ordListFactory(model, subscriber, seriesParams) {
    var originalOrd = seriesParams.ord; // eslint-disable-next-line promise/avoid-new

    return new Promise(function (resolve) {
      var value = seriesParams.value;

      if (value && typeof value.getType === "function" && value.getType().is(ordListTypeSpec)) {
        var ords = seriesParams.value.getOrds(),
            promise = webChartUtil.fallbackBatchResolve(ords).then(function (values) {
          var newSeriesParams = [];

          for (var i = 0; i < ords.length; i++) {
            if (values[i] !== null) {
              var newParam = makeSeriesParams(seriesParams, ords[i], originalOrd);
              newParam.value = values[i];

              if (i > 0 && newParam.color) {
                delete newParam.color;
              }

              newSeriesParams.push(newParam);
            }
          }

          return exports.make(model, subscriber, newSeriesParams);
        });
        resolve(promise);
      } else {
        resolve();
      }
    });
  }, //look for agents on IChartFactory
  function registryFactory(model, subscriber, seriesParams, params) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve) {
      var value = seriesParams.value,
          reg,
          type;

      if (value && typeof value.getType === "function") {
        type = value.getType();
        reg = StationRegistry.getInstance();
        reg.resolveFirst(type, {
          tags: [chartFactoryTypeSpec]
        }).then(function (ChartFactoryConstructor) {
          if (ChartFactoryConstructor) {
            var factory = new ChartFactoryConstructor();
            resolve(factory.factory(model, subscriber, seriesParams, params));
          } else {
            resolve();
          }
        })["catch"](function (err) {
          webChartUtil.trace(err);
          resolve();
        });
      } else {
        resolve();
      }
    });
  }];
  /**
   * If any seriesParams contains an ordLists, expand them into individual ords
   * @param {Array.<Object>}seriesParams
   * @return {Array.<Object>}
   */

  function expandOrdLists(seriesParams) {
    var newSeriesParams = [],
        i,
        j;

    for (i = 0; i < seriesParams.length; ++i) {
      if (seriesParams[i].value && seriesParams[i].value.getType && seriesParams[i].value.getType().is(ordListTypeSpec)) {
        var ords = seriesParams[i].value.getOrds();

        for (j = 0; j < ords.length; ++j) {
          newSeriesParams.push({
            ord: ords[j]
          });
        }
      } else {
        newSeriesParams.push(seriesParams[i]);
      }
    }

    return newSeriesParams;
  }
  /**
   * Creates a new series param object by copying over key values from the old
   * seriesParams and including the ord and originalOrd on the new object.
   *
   * @param {Array|String|Object} seriesParams
   * @param {String} ord
   * @param {String} originalOrd
   * @returns {Object}
   */


  function makeSeriesParams(seriesParams, ord, originalOrd) {
    var color = seriesParams.color,
        chartType = seriesParams.chartType;
    return {
      ord: ord,
      originalOrd: originalOrd,
      color: color,
      chartType: chartType
    };
  }
  /**
   * Returns a promise that resolves with a number of newly created
   * Series objects.
   *
   * Please note, this method may invoke some User Interface.
   *
   * @param model The Chart Model.
   * @param {baja.Subscriber} subscriber The subscriber instance to use
   * when resolving data.
   * @param {Array|String|Object} seriesParams An array of
   * Series parameter objects used to create the Series data. Each
   * object must declare an 'ord' property. A value property can also be
   * optionally declared as a 'value' property. This can also be an ORD
   * Strong or a singular Object.
   * @param {Object} [params] This optional argument is a container for additional factory
   * resolution parameters.
   * @param {Array.<String>} params.currentSeriesOrds Collects existing and pending ORD strings
   * for series that are (or will be) added to the chart.  This allows for limit checking to
   * skip looking for additional series when the chart's series limit is reached.
   * @returns {Promise}
   */


  exports.make = function make(model, subscriber, seriesParams, params) {
    var newSeries = [],
        ordsToResolve,
        i,
        x,
        param,
        queries;

    if (!model || !seriesParams) {
      return Promise.reject(new Error("No series factory data"));
    }

    seriesParams = $.isArray(seriesParams) ? seriesParams : [seriesParams]; // if seriesParam is string, place string as ord parameter

    for (i = 0; i < seriesParams.length; ++i) {
      seriesParams[i] = typeof seriesParams[i] === "string" ? {
        ord: seriesParams[i]
      } : seriesParams[i];
    }

    seriesParams = expandOrdLists(seriesParams); // Sanitize the series parameters.

    for (i = 0; i < seriesParams.length; ++i) {
      param = seriesParams[i];

      if (!param.ord && param.ord !== "") {
        return Promise.reject(new Error("No ORD"));
      }

      queries = baja.Ord.make(param.ord).parse(); // Remove any view ORD schemes.

      for (x = 0; x < queries.size(); ++x) {
        if (queries.get(x).getSchemeName() === "view") {
          queries.remove(x);
          break;
        }
      }

      param.ord = baja.Ord.make(queries).toString(); // Don't resolve file ORDs or if we already have the value or it
      // uses the http

      if (!webChartUtil.isFileUri(param.ord) && !param.value) {
        ordsToResolve = ordsToResolve || [];
        ordsToResolve.push(param.ord);
      }
    } // If unitialized, initialize the params container and its currentSeriesOrds based on any
    // Series already added to the model prior to this call


    function seriesToOrdString(series) {
      return series.ord().toString();
    }

    if (!params) {
      params = {
        currentSeriesOrds: model.seriesList().map(seriesToOrdString)
      };
    } else if (!params.currentSeriesOrds) {
      params.currentSeriesOrds = model.seriesList().map(seriesToOrdString);
    } // Before further processing, check to see if the max series limit has already been reached


    if (params.currentSeriesOrds.length >= model.maxSeriesListLength()) {
      return Promise.resolve([]); // Limit reached, no need to run the expensive factories
    } else {
      return Promise["try"](function () {
        // If there are any ORDs that don't have a value then resolve them.
        if (ordsToResolve) {
          return webChartUtil.fallbackBatchResolve(ordsToResolve, subscriber).then(function (objs) {
            var i, x; // Fill in any missing values in the series parameters.

            for (i = 0; i < ordsToResolve.length; ++i) {
              for (x = 0; x < seriesParams.length; ++x) {
                if (ordsToResolve[i] === seriesParams[x].ord) {
                  seriesParams[x].value = objs[i];
                  break;
                }
              }
            }
          });
        }
      }).then(function () {
        // Now we need to run the factories on the seriesParams to determine the Series to plot on
        // the web chart. Since running the factories is expensive, we'll stop doing it as soon as
        // the web chart Series limit (model.maxSeriesListLength()) is reached.
        function doProcess(seriesParams) {
          if (!seriesParams.length) {
            return;
          }

          return runFactories(model, subscriber, seriesParams[0], newSeries, params).then(function () {
            Object.keys(newSeries).forEach(function (seriesOrd) {
              if (!params.currentSeriesOrds.contains(seriesOrd)) {
                params.currentSeriesOrds.push(seriesOrd);
              }
            });
            return params.currentSeriesOrds.length < model.maxSeriesListLength() && doProcess(seriesParams.slice(1));
          });
        }

        return doProcess(seriesParams).then(function () {
          var results = [];
          Object.keys(newSeries).forEach(function (seriesOrd) {
            if (results.length >= model.maxSeriesListLength()) {
              return;
            }

            results.push(newSeries[seriesOrd]);
          });
          return results;
        });
      })["catch"](function (err) {
        webChartUtil.log("Cannot add series:", Array.prototype.slice.call(arguments));
        webChartUtil.trace(err); //throw err;//promise.reject(err);
      });
    }
  };

  return exports;
});
