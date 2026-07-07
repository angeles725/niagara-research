/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/* eslint-env browser */
define(['baja!', 'nmodule/webChart/rc/webChartUtil'], function (baja, webChartUtil) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * Utilize Html5 Storage to allow for loading/saving/clearing options
   *
   * @exports nmodule/webChart/rc/optionsManager
   */

  var optionsManager = {};
  var storage;

  try {
    storage = window.localStorage;
  } catch (ignore) {}
  /**
   * Set the Html storage object.
   * @param {Object} newStorage
   */


  optionsManager.setStorage = function (newStorage) {
    storage = newStorage;
  };
  /**
   * Get the Html storage object.
   * @return {Object}
   */


  optionsManager.getStorage = function () {
    return storage;
  };
  /**
   * Is Html5 Storage available?
   * @returns {boolean}
   */


  optionsManager.optionsSupported = function () {
    return storage !== null;
  };
  /**
   * Load options with a given name, usually a baja.Component.
   * @param {String} optionsName
   * @returns {baja.Value}
   */


  optionsManager.loadOptions = function (optionsName) {
    var optionsStorage = null;

    if (storage) {
      // Always access local storage in try/catch...
      try {
        optionsStorage = storage.getItem(optionsName); // If we have data then parse it

        if (optionsStorage) {
          optionsStorage = webChartUtil.unmarshal(optionsStorage);
        }
      } catch (ignore) {
        webChartUtil.trace(ignore);
        optionsManager.clearOptions(optionsName);
      }
    }

    return optionsStorage;
  };
  /**
   * Save baja.Value options with a given name
   * @param {String} optionsName
   * @param {baja.Value} options
   */


  optionsManager.saveOptions = function (optionsName, options) {
    if (storage) {
      // Always access local storage in try/catch...
      try {
        storage.setItem(optionsName, webChartUtil.marshal(options));
      } catch (ignore) {
        webChartUtil.trace(ignore);
        optionsManager.clearOptions(optionsName);
      }
    }
  };
  /**
   * Clear the current options with the given name.
   * @param {String} optionsName
   */


  optionsManager.clearOptions = function (optionsName) {
    if (storage) {
      // Always access local storage in try/catch...
      try {
        storage.removeItem(optionsName);
      } catch (ignore) {
        webChartUtil.trace(ignore);
      }
    }
  };

  return optionsManager;
});
