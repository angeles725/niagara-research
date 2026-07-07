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
define(['d3', 'jquery', 'underscore', 'Promise', 'baja!', 'bajaux/Widget', 'dialogs', 'moment', 'nmodule/webEditors/rc/fe/baja/util/compUtils', 'nmodule/webEditors/rc/fe/baja/util/statusUtils', 'nmodule/webEditors/rc/fe/baja/util/ComplexDiff', 'niagaraSystemProperties', 'lex!baja,webChart', 'nmodule/js/rc/csrf/csrfUtil', 'nmodule/js/rc/asyncUtils/asyncUtils', 'baja!' + 'webChart:WebChartTimeRange,' + 'webChart:WebChartTimeRangeType,' + 'gx:Color,' + 'baja:Status'], function (d3, $, _, Promise, baja, Widget, dialogs, moment, compUtils, statusUtils, ComplexDiff, niagaraSystemProperties, lexs, csrfUtil, asyncUtils) {
  "use strict";

  var doRequire = asyncUtils.doRequire;

  var dateTimeUtil = require('bajaScript/baja/obj/dateTimeUtil');
  /**
   * API Status: **Private**
   *
   * A set of utility functions for the webChart module.
   *
   * @exports nmodule/webChart/rc/webChartUtil
   */


  var webChartUtil = {},
      logOn = true,
      traceOn = false;
  webChartUtil.bajaLex = lexs[0];
  webChartUtil.lex = lexs[1]; //cache of any calls to get the stationName

  webChartUtil.$stationName = null;
  webChartUtil.defaultLineColors = ['#00A0B0', '#06BF00', '#B300DB', '#0050FF', '#D9BB96', '#8C0000', '#387038', '#703870', '#25255A', '#1F6F70'];
  /**
   * Turn trace on or off.
   * @param {Boolean} value
   */

  webChartUtil.setTrace = function (value) {
    traceOn = value;
  };
  /**
   * Turn logs on or off.
   * @param {Boolean} value
   */


  webChartUtil.setLog = function (value) {
    logOn = value;
  };
  /**
   * Dump the current stack to log.
   * @param {Object|string} message
   */


  webChartUtil.dumpStack = function (message) {
    try {
      throw new Error(message);
    } catch (err) {
      baja.error(err);
    }
  };
  /**
   * Log a message.
   * @param {String|Error} message
   */


  webChartUtil.log = function () {
    var message, i;

    if (logOn) {
      for (i = 0; i < arguments.length; i++) {
        message = arguments[i];

        if (message instanceof Error) {
          baja.error(message);
        } else {
          baja.outln(message);
        }
      }
    }
  };
  /**
   * Log a message if trace is on.
   * @param {String|Error} message
   */


  webChartUtil.trace = function () {
    var message, i;

    if (traceOn) {
      for (i = 0; i < arguments.length; i++) {
        message = arguments[i];

        if (message instanceof Error) {
          baja.error(message);
        } else {
          baja.outln(message);
        }
      }
    }
  };
  /**
   * Return true if this is a JavaFX Browser (certain things should be avoided in JavaFX that would normally be allowed).
   * @returns {boolean}
   */


  webChartUtil.isJavaFx = function () {
    //only check once
    if (webChartUtil.$isJavaFx === undefined) {
      webChartUtil.$isJavaFx = navigator && navigator.userAgent && navigator.userAgent.indexOf('JavaFX') > -1;
    }

    return webChartUtil.$isJavaFx;
  };
  /**
   * Return true if this is a WebStart Browser (certain things should be avoided in WebStart that would normally be allowed).
   * @returns {boolean}
   */


  webChartUtil.isWebStart = function () {
    //only check once
    if (webChartUtil.$isWebStart === undefined) {
      webChartUtil.$isWebStart = navigator && navigator.userAgent && navigator.userAgent.indexOf('WebStart') > -1;
    }

    return webChartUtil.$isWebStart;
  };
  /**
   * Return true if current browser is Safari.
   * @returns {boolean}
   */


  webChartUtil.isSafari = function () {
    return /Safari/.test(navigator.userAgent) && /Apple Computer/.test(navigator.vendor);
  };
  /**
   * Return true if current browser is Firefox.
   * @returns {boolean}
   */


  webChartUtil.isFirefox = function () {
    return navigator && navigator.userAgent && navigator.userAgent.indexOf("Firefox") > -1;
  };
  /**
   * Gets the position of d3 mouse event based upon the browser being used.
   * @param {dom} element
   * @returns {{x: number, y: number}}
   */


  webChartUtil.getD3MouseEventPosition = function (element) {
    var isSafari = webChartUtil.isSafari(); //avoid svg bug with firefox
    //https://bugzilla.mozilla.org/show_bug.cgi?id=1610093

    var isFirefox = webChartUtil.isFirefox();

    if (isSafari || isFirefox) {
      var targetElement = $(element).closest(".Line")[0],
          translateInfo = targetElement && d3.transform(d3.select(targetElement).attr("transform")).translate,
          transformedOffsetX = translateInfo ? translateInfo[0] : 0,
          transformedOffsetY = translateInfo ? translateInfo[1] : 0;
      return {
        x: d3.event.offsetX - transformedOffsetX,
        y: d3.event.offsetY - transformedOffsetY
      };
    } else {
      return {
        x: d3.mouse(element)[0],
        y: d3.mouse(element)[1]
      };
    }
  };

  webChartUtil.getRelativeOrd = function (ord) {
    if (!ord) {
      return ord;
    }

    ord = String(ord);

    if (ord.indexOf("local:|") === 0) {
      return ord.substring(7);
    } else {
      return ord;
    }
  };
  /**
   * Get the history display name for an ord.
   * @param {String} ord
   * @returns {String}
   */


  webChartUtil.getDisplayName = function (ord) {
    ord = ord.replace(/.*history:/, ''); //history:|history:/webChart/BigRamp

    if (ord.indexOf("/") === 0 || ord.indexOf("^") === 0) {
      ord = ord.substring(1);
    }

    return baja.SlotPath.unescape(ord);
  };
  /**
   * Replace all invalid characters that shouldn't be used for a css name. Things like spaces and dots are encoded into unique name.
   *
   * @param {String} desiredClassName
   * @returns {String}
   */


  webChartUtil.safeClassName = function (desiredClassName) {
    if (!desiredClassName) {
      return desiredClassName;
    }

    return desiredClassName.replace(/[^a-zA-Z0-9]/g, function (s) {
      var c = s.charCodeAt(0);

      if (c === 32) {
        return '-';
      }

      return '__' + ('000' + c.toString(16)).slice(-4);
    });
  };
  /**
   * Get the short display name for a displayName.
   * @param {String} displayName
   * @returns {String}
   */


  webChartUtil.getShortDisplayName = function (displayName) {
    if (!displayName || !displayName.length) {
      return displayName;
    }

    var slashIndex = displayName.indexOf("/");

    if (slashIndex > -1) {
      displayName = displayName.substring(slashIndex + 1);
    }

    return displayName;
  };
  /**
   * Print out all methods of an object for debug purposes.
   * @param obj
   */


  webChartUtil.printMethods = function (obj) {
    for (var m in obj) {
      if (typeof obj[m] === "function") {
        webChartUtil.log("method:" + obj[m]);
      }
    }
  };
  /**
   * Request an webChartUtil.rpc and provide additional debug, including when an rpc takes too long
   *
   * @returns {Promise}
   */


  webChartUtil.rpc = function () {
    if (webChartUtil.traceOn) {
      return timeout(baja.rpc.apply(this, arguments), 10000, "Rpc is taking more than 10 seconds"); //help diagnose problems by adding timeout
    } else {
      return baja.rpc.apply(this, arguments);
    }
  };

  function timeout(prom, timeout, msg) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      var ticket = setTimeout(function () {
        return reject(new Error(msg));
      }, timeout);
      prom.then(function (result) {
        clearTimeout(ticket);
        resolve(result);
      })["catch"](reject);
    });
  }
  /**
   * Request an ajax call to the WebChart servlet and use proper error handling.
   * @param {String} uri
   * @param Object [params]
   * @returns {Promise}
   */


  webChartUtil.ajax = function (uri, params) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      if (!params) {
        params = {};
      }

      if (!params.type) {
        params.type = "GET";
      }

      if (!params.accepts) {
        params.accepts = {
          json: "application/vnd.tridium.webChart-v1+json"
        };
      }

      params.success = function (result, status, jqXHR) {
        resolve(result);
      };

      params.error = function (jqXHR, textStatus, errorThrown) {
        //window.console.log("error1:" + textStatus);
        var errorResult = {};

        errorResult.toString = function () {
          return errorThrown;
        };

        errorResult.textStatus = textStatus;
        errorResult.jqXHR = jqXHR;
        reject(errorResult); //make sure the toString is not just Object
      }; //Add the CSRF token


      var token = csrfUtil.getCsrfToken();

      if (!params.headers) {
        params.headers = {
          'x-niagara-csrfToken': token
        };
      } else {
        params.headers['x-niagara-csrfToken'] = token;
      }

      $.ajax(uri, params);
    });
  };
  /**
   * Get quantity from database
   * @param db
   * @param unit
   * @returns {baja.UnitDatabase.Quantity}
   */


  function getContainingQuantity(db, unit) {
    var quantities = db.getQuantities(),
        quantity,
        units,
        i,
        j;

    for (i = 0; i < quantities.length; i++) {
      quantity = quantities[i];
      units = quantity.getUnits();

      for (j = 0; j < units.length; j++) {
        if (units[j].equivalent(unit)) {
          return quantity;
        }
      }
    }
  }
  /**
   * Resolve the Quantity associated with a unitEncoding, utilize the WebChartQueryServlet for this
   * @param {baja.Unit} unit
   * @returns {Promise.<String>}
   */


  webChartUtil.getQuantity = function (unit) {
    if (!unit || unit.equals(baja.Unit.DEFAULT)) {
      //default unit has no quantity
      return Promise.resolve("");
    }

    return baja.UnitDatabase.get().then(function (db) {
      var quantity = getContainingQuantity(db, unit),
          quantityName;

      if (!quantity) {
        return "";
      }

      quantityName = quantity.getName();
      return quantityName;
    });
  };
  /**
   * A utility function for providing the usual ordinal array for an baja.EnumRange.
   @example
   *   <caption>
   *     Get the usual ordinal array for  3 items
   *   </caption>
   *  var choicesEnum = baja.DynamicEnum.make({
   *           ordinal : 0,
   *           range : baja.EnumRange.make({
   *             tags : ["one", "two", "three"],
   *             ordinals : webChartUtil.getUsualOrdinals(3)
   *           })
   *         });
   *
   * @param length
   * @returns {Array}
   */


  webChartUtil.getUsualOrdinals = function (length) {
    var ordinals = [];

    for (var i = 0; i < length; i++) {
      ordinals.push(i);
    }

    return ordinals;
  };
  /**
   * A utility function for only moving the non-null results to a new array
   * @param {Array} original
   * @returns {Array}
   */


  webChartUtil.getCleanCopy = function (original) {
    var results = [],
        i;

    for (i = 0; i < original.length; i++) {
      if (original[i]) {
        results.push(original[i]);
      }
    }

    return results;
  };
  /**
   * Return the top dialog Object
   * @returns {js.dialogs.Dialog}
   */


  webChartUtil.getCurrentDialogObject = function () {
    var dialog;
    dialogs.each(function (i, dlg) {
      dialog = dlg;
    });
    return dialog;
  };
  /**
   * transfer a list of events to different name so that they don't get overridden.
   * @param {d3} selection
   * @param {Array.<String>} names
   * @param {String} suffix for new name
   */


  webChartUtil.transferEvents = function (selection, names, suffix) {
    var originalEvent, i;

    for (i = 0; i < names.length; ++i) {
      originalEvent = selection.on(names[i]);

      if (originalEvent) {
        selection.on(names[i] + suffix, originalEvent);
      }
    }
  };
  /**
   * Given a hex color code from the baja lexicon, convert it to an
   * appropriate CSS hex color code
   *
   * @inner
   * @param hexString The {String} form of the hex color code retrieved from the baja lexicon
   * @returns A {String} containing a hex color code appropriate for use in CSS
   */


  function convertColorCode(hexString) {
    if (hexString && hexString.length > 7) {
      hexString = hexString.replace('FF', '');
    }

    return hexString;
  }
  /**
   * Converts a `baja.Status` to a string`.
   *
   * @param {baja.Status|Number} status the status to encode (a bit string in
   * the form of a number will be converted to a `baja.Status`)
   * @returns {Promise} promise to be resolved with the toString-ed
   * status, or rejected if the input was invalid
   */


  webChartUtil.statusToString = function statusToString(status) {
    if (!status) {
      //handle undefined and null
      status = 0;
    }

    return statusUtils.statusToString(status);
  };
  /**
   * Does the status have a designated status coloring?
   * @param status
   * @returns {Boolean}
   */


  webChartUtil.hasStatusColor = function statusToColor(status) {
    if (!status) {
      //handle undefined and null
      status = 0;
    }

    if (typeof status === 'number') {
      status = baja.Status.make(status);
    }

    return status.isDisabled() || status.isFault() || status.isDown() || status.isAlarm() || status.isStale() || status.isOverridden();
  };
  /**
   * Converts a `baja.Status` to a color `BStatus`.
   *
   * @param {baja.Status|Number} status the status to encode (a bit string in
   * the form of a number will be converted to a `baja.Status`)
   * @returns {{fgColor: String, bgColor: String}} Object that contains foreground and background color
   */


  webChartUtil.statusToColor = function statusToColor(status) {
    var fgColor, bgColor;

    if (!status) {
      //handle undefined and null
      status = 0;
    }

    if (typeof status === 'number') {
      status = baja.Status.make(status);
    }

    if (!baja.hasType(status, 'baja:Status')) {
      throw new Error('baja:Status or number required');
    }

    if (!webChartUtil.$statusFgColors) {
      // Lazy load the status foreground colors
      // Array index 0 = alarmFg
      // Array index 1 = disabledFg
      // Array index 2 = faultFg
      // Array index 3 = downFg
      // Array index 4 = overriddenFg
      // Array index 5 = staleFg
      webChartUtil.$statusFgColors = [convertColorCode(webChartUtil.bajaLex.get('Status.alarm.fg')), convertColorCode(webChartUtil.bajaLex.get('Status.disabled.fg')), convertColorCode(webChartUtil.bajaLex.get('Status.fault.fg')), convertColorCode(webChartUtil.bajaLex.get('Status.down.fg')), convertColorCode(webChartUtil.bajaLex.get('Status.overridden.fg')), convertColorCode(webChartUtil.bajaLex.get('Status.stale.fg'))];
    }

    if (!webChartUtil.$statusBgColors) {
      webChartUtil.$statusBgColors = [convertColorCode(webChartUtil.bajaLex.get('Status.alarm.bg')), convertColorCode(webChartUtil.bajaLex.get('Status.disabled.bg')), convertColorCode(webChartUtil.bajaLex.get('Status.fault.bg')), convertColorCode(webChartUtil.bajaLex.get('Status.down.bg')), convertColorCode(webChartUtil.bajaLex.get('Status.overridden.bg')), convertColorCode(webChartUtil.bajaLex.get('Status.stale.bg'))];
    }

    if (status.isDisabled()) {
      fgColor = webChartUtil.$statusFgColors[1];
      bgColor = webChartUtil.$statusBgColors[1];
    } else if (status.isFault()) {
      fgColor = webChartUtil.$statusFgColors[2];
      bgColor = webChartUtil.$statusBgColors[2];
    } else if (status.isDown()) {
      fgColor = webChartUtil.$statusFgColors[3];
      bgColor = webChartUtil.$statusBgColors[3];
    } else if (status.isAlarm()) {
      fgColor = webChartUtil.$statusFgColors[0];
      bgColor = webChartUtil.$statusBgColors[0];
    } else if (status.isStale()) {
      fgColor = webChartUtil.$statusFgColors[5];
      bgColor = webChartUtil.$statusBgColors[5];
    } else if (status.isOverridden()) {
      fgColor = webChartUtil.$statusFgColors[4];
      bgColor = webChartUtil.$statusBgColors[4];
    }

    return {
      fgColor: fgColor,
      bgColor: bgColor
    };
  };
  /**
   * Returns a sequentially different color for visual testing
   * @returns {{fgColor: String, bgColor: String}} Object that contains foreground and background color
   */


  webChartUtil.statusToColorDebug = function () {
    var status = 0;

    if (!window.colorCounter) {
      window.colorCounter = 1;
    }

    window.colorCounter++;

    if (window.colorCounter % 7 === 0) {} else if (window.colorCounter % 7 === 1) {
      status = baja.Status.fault;
    } else if (window.colorCounter % 7 === 2) {
      status = baja.Status.down;
    } else if (window.colorCounter % 7 === 3) {
      status = baja.Status.alarm;
    } else if (window.colorCounter % 7 === 4) {
      status = baja.Status.stale;
    } else if (window.colorCounter % 7 === 5) {
      status = baja.Status.overridden;
    } else if (window.colorCounter % 7 === 6) {
      status = baja.Status.ok;
    }

    return webChartUtil.statusToColor(status);
  };
  /**
   * Return the top dialog Object jq
   * @returns {jQuery}
   */


  webChartUtil.getCurrentDialogDom = function () {
    return webChartUtil.getCurrentDialogObject().jq();
  };
  /**
   * Marshal a baja.Value into s JSON string, any dynamic transient properties will be removed from baja.Component.
   *
   * @param {baja.Value} object
   * @returns {String}
   */


  webChartUtil.marshal = function (object) {
    return JSON.stringify(webChartUtil.encodeValueForStorage(object));
  };
  /**
   * Remove any dynamic and transient properties.
   * @param comp
   */


  function removeTransients(comp) {
    comp.getSlots().dynamic().properties().flags(baja.Flags.TRANSIENT).each(function (slot) {
      comp.remove(slot);
    });
    comp.getSlots().isComponent().each(function (slot) {
      removeTransients(comp.get(slot));
    });
  }
  /**
   * Encode a BValue to a JsonObject used storage. Any dynamic transient properties will be removed from baja.Component.
   * @param {baja.Value} object
   * @returns {Object}
   */


  webChartUtil.encodeValueForStorage = function (object) {
    var copy = object;

    if (object.getType().isComponent()) {
      copy = copy.newCopy(true);
      removeTransients(copy);
    }

    return baja.bson.encodeValue(copy);
  };
  /**
   * Unmarshals a JSON string into a baja.value
   *
   * @param {String} encodedString
   * @returns {baja.Value}
   */


  webChartUtil.unmarshal = function (encodedString) {
    return baja.bson.decodeValue(JSON.parse(encodedString));
  };
  /**
   * Get a svg element  width
   * @param {dom} svg, g, etc
   * @returns {Number}
   */


  webChartUtil.getWidth = function (g) {
    if (g === null) {
      return 0;
    }

    try {
      var width = g.getBBox().width;
      return width;
    } catch (err) {
      webChartUtil.trace("avoiding FF bug https://bugzilla.mozilla.org/show_bug.cgi?id=612118:" + err);
      return 0;
    }
  };
  /**
   * Get a svg element  width
   * @param {dom} svg, g, etc
   * @returns {Number}
   */


  webChartUtil.getHeight = function (g) {
    if (g === null) {
      return 0;
    }

    try {
      var height = g.getBBox().height;
      return height;
    } catch (err) {
      webChartUtil.trace("avoiding FF bug https://bugzilla.mozilla.org/show_bug.cgi?id=612118:" + err);
      return 0;
    }
  };
  /**
   * Returns true is there is a smaller form factor available
   *
   * @returns {boolean}
   * @param widget
   */


  webChartUtil.hasSmallerFormFactor = function (widget) {
    var smaller = webChartUtil.getSmallerFormFactor(widget);
    return !!smaller;
  };
  /**
   * Returns true is there is a larger form factor available
   *
   * @returns {boolean}
   * @param widget
   */


  webChartUtil.hasLargerFormFactor = function (widget) {
    var larger = webChartUtil.getLargerFormFactor(widget);
    return !!larger;
  };
  /**
   * Look through a Widget's prototype chain for any constuctors with a defined formFactor.
   * @param {Widget} widget
   * @param widget
   */


  webChartUtil.getAvailableFormFactors = function (widget) {
    var currentFormFactor = widget.constructor.formFactor;

    if (currentFormFactor === undefined && widget.prototype) {
      return webChartUtil.getAvailableFormFactors(widget.prototype);
    }

    return currentFormFactor;
  };
  /**
   * getSmallerFormFactor gets the next smaller formfactor
   * @returns {*} smaller form factor if one is available.
   * @param widget
   */


  webChartUtil.getSmallerFormFactor = function (widget) {
    var currentFormFactor = widget.getFormFactor();
    var availableFormFactors = webChartUtil.getAvailableFormFactors(widget);

    if (currentFormFactor === Widget.formfactor.max && availableFormFactors & Widget.formfactor.compact) {
      return Widget.formfactor.compact;
    } else if (currentFormFactor === Widget.formfactor.max && availableFormFactors & Widget.formfactor.mini) {
      return Widget.formfactor.mini;
    } else if (currentFormFactor === Widget.formfactor.compact && availableFormFactors & Widget.formfactor.mini) {
      return Widget.formfactor.mini;
    }

    return null;
  };
  /**
   * getLargerFormFactor gets the next larger formfactor
   * @param {Widget.formfactor} formFactor
   * @returns {Widget.formfactor} larger form factor if one is available.
   */


  webChartUtil.getLargerFormFactor = function (widget) {
    var currentFormFactor = widget.getFormFactor();
    var availableFormFactors = webChartUtil.getAvailableFormFactors(widget);

    if (currentFormFactor === Widget.formfactor.mini && availableFormFactors & Widget.formfactor.compact) {
      return Widget.formfactor.compact;
    } else if (currentFormFactor === Widget.formfactor.mini && availableFormFactors & Widget.formfactor.max) {
      return Widget.formfactor.max;
    } else if (currentFormFactor === Widget.formfactor.compact && availableFormFactors & Widget.formfactor.max) {
      return Widget.formfactor.max;
    }

    return null;
  };
  /**
   * Temporarily sets a CSS rule for a dom element. This allows dynamic properties, such as color scales, to be
   * set by static stylesheets.
   *
   * For example, to get the color for a HeatMap stored in the following rule:
   *     .HeatMap > .cold { color: #B06; }
   * You could write
   *     var coldColor = hm.getStyleProperty('cold','color');
   *
   * These properties can be overwritten by loading new stylesheets.
   *
   *
   * @param  {String} property The property we want to read from the rule
   * @param jq3 d3 based dom element
   * @param [newCssClass] optional different cssClass we want to test
   * @param [fallback] for IE and PhantomJs
   * @return {String} The value of the property.
   */


  webChartUtil.getStyleProperty = function (jq3, property, newCssClass, fallback) {
    var result = fallback,
        classes = {},
        inlineStyle,
        jq = $(jq3[0][0]);

    try {
      inlineStyle = jq.attr('style'); //window.console.log("inline:" + inlineStyle);

      if (inlineStyle) {
        jq.removeAttr('style');
      }

      if (newCssClass) {
        newCssClass = newCssClass.replace('.', '');
        classes[newCssClass] = true;
        jq3.classed(classes); //remove current styling
      }

      var computedStyle = jq3.style(property); //window.console.log("computed:" + computedStyle);

      if (computedStyle !== "normal") {
        result = computedStyle;
      }

      if (newCssClass) {
        classes[newCssClass] = false;
        jq3.classed(classes);
      }
    } catch (err) {
      webChartUtil.trace("TODO: Some Browsers (like PhantomJS do not supported getStyleProperty()");
    } //put inline style back if present


    if (inlineStyle) {
      jq.attr('style', inlineStyle);
    }

    return result;
  };
  /**
   * Convert moment to Date
   * @param {moment} m
   * @returns {baja.AbsTime}
   */


  webChartUtil.getAbsTime = function (m) {
    var date = baja.Date.make({
      year: m.year(),
      month: m.month(),
      day: m.date()
    }),
        time = baja.Time.make({
      hour: m.hour(),
      min: m.minute(),
      sec: m.second(),
      ms: m.millisecond()
    });
    return baja.AbsTime.make({
      date: date,
      time: time,
      offset: m.utcOffset() * 60000
    });
  };
  /**
   * Convert baja.AbsTime to moment
   * @param {baja.AbsTime} absTime
   * @returns {moment}
   */


  webChartUtil.getMoment = function (absTime) {
    return moment(absTime.getJsDate());
  };
  /**
   * Return true if the resolution of the specified ORD should be skipped.
   *
   * @param  {String} ord The ORD to be resolved.
   * @returns {Boolean} Returns true if resolution of the ORD should be skipped.
   */


  webChartUtil.isFileUri = function (ord) {
    var queries = baja.Ord.make(ord).parse(),
        isFileUri = false,
        schemeName,
        i; // Remove any view ORD schemes.

    for (i = 0; i < queries.size(); ++i) {
      schemeName = queries.get(i).getSchemeName();

      if (schemeName === "http" || schemeName === "https" || schemeName === "file") {
        isFileUri = true;
      }
    }

    return isFileUri;
  };
  /**
   * Return true if the ORD maps to a file.
   *
   * @param  {String} ord The ORD used to work out whether
   * this maps to a file or not.
   * @returns {Boolean} Returns true if the ORD maps to a file.
   */


  webChartUtil.isFileOrd = function (ord) {
    var queries = baja.Ord.make(ord).parse(),
        i;

    for (i = 0; i < queries.size(); ++i) {
      if (queries.get(i).getSchemeName() === "file") {
        return true;
      }
    }

    return false;
  };
  /**
   * Return the file path from the ORD string. If the ORD contains no file
   * ORD then an empty string is returned. File Path will include extension.
   * extension stripped from it.
   *
   * @param  {String} ord The ORD used to work out whether
   * this maps to a file or not.
   * @returns {String} the File Path.
   */


  webChartUtil.getFilePath = function (ord) {
    var queries = baja.Ord.make(ord).parse(),
        i,
        res; //,
    //split;

    for (i = 0; i < queries.size(); ++i) {
      if (queries.get(i).getSchemeName() === "file") {
        res = /\^?(.+)$/.exec(queries.get(i).getBody()); //window.console.log("res[0]" + res)

        return res && res[1] ? res[1] : "";
      }
    }

    return "";
  };
  /**
   * Returns an URI to the file.
   *
   * @param  {String} ord The ORD used to work out whether
   * this maps to a file or not.
   * @returns {String} the File URI.
   */


  webChartUtil.getFileUri = function (ord) {
    var path = webChartUtil.getFilePath(ord);
    return path ? "/file/" + encodeURIComponent(path) : "";
  };
  /**
   * Resolves to the default chart settings content. If there are no default or
   * the rpc is not available then resolve to null. This call does not spam the console with any
   * log message if the file does not exist.
   * @returns {Promise.<Object|null>}
   */


  webChartUtil.resolveSettings = function (settingOrd) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve) {
      webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getChartSettings", String(settingOrd)).then(function (results) {
        resolve(results);
      })["catch"](function () {
        resolve(null);
      });
    });
  };
  /**
   * Zoom Utility for determining the translateVector from the scale and center point on the graph
   * @param {Array.<Number>} point Point relative to the svg.line-container
   * @param {Number} scaleFactor
   * @return Array.<Number> translateVector
   */


  webChartUtil.zoomCenter = function (point, scaleFactor) {
    var loc,
        view = {
      k: scaleFactor,
      x: 0,
      y: 0
    }; //replicate mouse wheel zoom based on point location

    function location(p) {
      return [(p[0] - view.x) / view.k, (p[1] - view.y) / view.k];
    }

    function translateTo(p, l) {
      view.x += l[0] - p[0];
      view.y += l[1] - p[1];
    }

    loc = location(point);
    translateTo(point, loc);
    return [view.x, view.y];
  };
  /**
   * Return the center of a dom element relative to the browser
   * @param {d3} selection
   * @return {Array.<Number>} Point
   */


  webChartUtil.getCenter = function (selection) {
    var node = selection.node(),
        jSelection = $(node),
        offset = jSelection.offset(),
        middleX = (offset.left + webChartUtil.getWidth(node)) / 2,
        middleY = (offset.top + webChartUtil.getHeight(node)) / 2;
    return [middleX, middleY];
  };
  /**
   * Return the center of a dom element relative to the d3 selection.
   * @param {d3} selection
   * @return {Array.<Number>} Point
   */


  webChartUtil.getRelativeCenter = function (selection) {
    var node = selection.node(),
        middleX = webChartUtil.getWidth(node) / 2,
        middleY = webChartUtil.getHeight(node) / 2;
    return [middleX, middleY];
  }; ////////////////////////////////////////////////////////////////
  // Names
  ////////////////////////////////////////////////////////////////

  /**
   * Translate a programatic name to a friendly
   * name.  This is done based on standard identifier
   * capitalization.  So the string "fooBar" would
   * be translated as "Foo Bar".
   * @param {String} s
   * @return {String}
   */


  webChartUtil.toFriendly = function (s) {
    var buf = "";

    if (!s.length) {
      return "";
    }

    buf += s[0].toUpperCase();
    var len = s.length;

    for (var i = 1; i < len; ++i) {
      var c = s[i];

      if (c.toUpperCase() === c && i > 0) {
        buf += ' ' + c;
      } else {
        buf += c;
      }
    }

    return buf;
  };
  /**
   * Translate a friendly string back into its
   * programatic name.
   * @param {String} s
   * @return {String}
   */


  webChartUtil.fromFriendly = function (s) {
    var buf = "",
        c;
    buf += s[0].toLowerCase();
    var len = s.length;
    var i = 1;

    for (; i < len; ++i) {
      c = s[i];

      if (c === ' ') {
        break;
      }

      buf += c;
    }

    for (; i < len; ++i) {
      c = s[i];

      if (c !== ' ') {
        buf += c;
      }
    }

    return buf;
  };
  /**
   * Convert newlines to breaks
   * @param {String} s
   * @returns {String}
   */


  webChartUtil.nl2br = function (s) {
    return s.replace(/\n/g, "<br/>");
  };
  /**
   * Return the elements bounds for an object
   * @param {Element} obj The raw dom element
   * @returns {Array.<int>} left, top
   */


  webChartUtil.getElementBounds = function (obj) {
    var left = 0,
        top = 0;

    if (obj.offsetParent) {
      do {
        left += obj.offsetLeft;
        top += obj.offsetTop;
        obj = obj.offsetParent;
      } while (obj);

      return [left, top];
    } else {
      return [0, 0];
    }
  };
  /**
   * carbon copy of java logic in baja:javax.baja.file.FilePath.isName() except for additional
   * check needed for NCCB-11874: % not supported by FileServlet.
   * @param {String} c
   * @returns {boolean}
   * @private
   */


  function isName(c) {
    var code = c.charCodeAt(0);
    return !(code <= 31 || // control characters
    code === 127 || // del
    c === '"' || c === '\\' || c === '<' || c === '>' || c === '?' || c === '*' || c === '/' || c === '%' || // TODO: NCCB-11874: % not supported by FileServlet
    c === '|');
  }
  /**
   * IsValidPath is the same as isValidName, but it allows "/" to allow the user to enter sub directories.
   * @param name
   * @returns {boolean}
   */


  webChartUtil.isValidPath = function (path) {
    return webChartUtil.isValidName(path, true);
  };
  /**
   * IsValidName is a carbon copy of java logic in baja:javax.baja.file.FilePath.isValidName()
   * @param {String} name
   * @param {boolean} [isPath]
   * @returns {boolean}
   */


  webChartUtil.isValidName = function (name, isPath) {
    try {
      var len = name.length,
          i,
          ch;

      if (len === 0) {
        return false;
      }

      for (i = 0; i < len; ++i) {
        ch = name[i];

        if (isPath && ch === '/') {
          continue;
        }

        if (!isName(ch) || ch === ':') {
          return false;
        }
      }

      return true;
    } catch (err) {
      webChartUtil.log(err);
      return false; // not ascii?
    }
  };
  /**
   * Safely show text to a user
   * @param {String} unsafe text
   * @return {String} safe text
   */


  webChartUtil.safe = function (unsafeText) {
    return $('<span>').text(unsafeText).html();
  };
  /**
   * Ensure an ord is safe for a hyperlink by double encoding for now (See double decode in OrdTargetFilter.toOrd()
   * @param {String} unsafe hyperlink url
   * @return {String} safe hyperlink for OrdTargetFilter's double decode.
   */


  webChartUtil.safeOrd = function (unsafeText) {
    return encodeURIComponent(unsafeText);
  };
  /**
   * Component.reorder requires that we know all the properties in order to re-order,
   * so if I only know some of the names, ensure the other names are included in the list.
   * @param {Array.<String>} someNames
   * @param {baja.Component} comp
   */


  webChartUtil.safeReorder = function (someNames, comp) {
    var currentDynProps = comp.getSlots().dynamic().properties(),
        currentDynProp,
        i,
        slot;

    for (i = someNames.length - 1; i >= 0; i--) {
      slot = comp.getSlot(someNames[i]);

      if (!slot || slot.isFrozen()) {
        someNames.splice(i, 1);
      }
    } //account for other dynamic props which we don't care the order


    while (currentDynProps.next()) {
      currentDynProp = currentDynProps.get().getName();

      if (someNames.indexOf(currentDynProp) < 0) {
        someNames.push(currentDynProp);
      }
    }

    comp.reorder(someNames);
  };
  /**
   * Get the permission for the desired ord. If the file does not exist yet,
   * get the permissions that would be inherited to it if we were to create it.
   * @return {Promise.<baja.Permissions>} Resolves to baja.Permissions
   * @param {baja.Ord} ord
   */


  webChartUtil.resolvePermissions = function (ord) {
    //TODO: waiting on bajascript permissions for file/history
    // NCCB-10993: NavNodes have no easy way to check permission in Bajascript
    return webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getPermissions", ord).then(function (result) {
      return baja.Permissions.make(result.permissions);
    });
  };

  webChartUtil.checkPermissions = function (ord, requiredPermissionsMask) {
    var requiredPermissions = baja.Permissions.make(requiredPermissionsMask);
    return webChartUtil.rpc("type:webChart:WebChartQueryRpc", "getPermissions", ord).then(function (result) {
      var neededPermissions = baja.Permissions.make(result.permissions);

      if (!neededPermissions.has(requiredPermissions)) {
        throw new Error(webChartUtil.lex.get("webChart.PermissionsException", neededPermissions, requiredPermissions));
      }
    });
  };
  /*
  * getParametersFromOrd looks in an ord for artifacts that can be used to retrieve a TimeRange and/or delta setting
  * for the chart.
  * @param {baja.Ord} ord
  * @returns {Object} with timeRange and delta if found
  */


  webChartUtil.getParametersFromOrd = function (ord) {
    var result = {};

    try {
      var body = new baja.ViewQuery(ord),
          params = body.getParameters(),
          startTime = params.start,
          endTime = params.end,
          period = params.period,
          timeRangeType;

      if (period) {
        timeRangeType = baja.$('webChart:WebChartTimeRangeType').make(period);
        result.timeRange = baja.$('webChart:WebChartTimeRange', {
          period: timeRangeType
        });
      } else if (startTime || endTime) {
        timeRangeType = baja.$('webChart:WebChartTimeRangeType').make("timeRange");
        var obj = {
          period: timeRangeType,
          startFixed: !!startTime,
          endFixed: !!endTime
        };

        if (startTime) {
          obj.startTime = baja.AbsTime.DEFAULT.decodeFromString(startTime);
        }

        if (endTime) {
          obj.endTime = baja.AbsTime.DEFAULT.decodeFromString(endTime);
        }

        result.timeRange = baja.$('webChart:WebChartTimeRange', obj);
      }

      if (params.delta) {
        result.delta = true;
      }

      return result;
    } catch (err) {
      webChartUtil.trace("Cannot getParametersFromOrd", err);
      return result;
    }
  };
  /**
   * Translate a dynamic period into a start date, end date. If date is not fixed, set to Date(0).
   * @param {webChart:WebChartTimeRange} timeRange
   * @param {module:baja/obj/TimeZone} timezone
   * @returns {{start: moment, end: moment}}
   */


  webChartUtil.getStartAndEndDateFromTimeRange = function (timeRange, timezone) {
    var result = {
      start: moment(new Date(0)),
      end: moment(new Date(0))
    };

    if (timeRange.getPeriod().getOrdinal() > 1) {
      return webChartUtil.getStartAndEndDateFromPeriod(timeRange.getPeriod(), timezone);
    } else if (timeRange.getPeriod().getOrdinal() === 1) {
      if (timeRange.getStartFixed()) {
        result.start = webChartUtil.getMoment(timeRange.getStartTime());
      }

      if (timeRange.getEndFixed()) {
        result.end = webChartUtil.getMoment(timeRange.getEndTime());
      }
    }

    return result;
  };
  /**
   * Translate a dynamic period into a start date, end date, and provide some meta data on whether the results
   * are fixed an whether the range supports live subscription.
   * @param {webChart:WebChartTimeRangePeriod} period
   * @param {baja.TimeZone} timezone
   * @returns {{start: moment, end: moment, live: boolean, startFixed: boolean, endFixed: boolean}}
   */


  webChartUtil.getStartAndEndDateFromPeriod = function (period, timezone) {
    var start = moment(),
        end = moment(),
        startFixed = true,
        endFixed = true,
        live = true;

    switch (period.getTag()) {
      case 'today':
        start = webChartUtil.startOf('day', timezone, start);
        end = webChartUtil.endOf('day', timezone, end);
        break;

      case 'last24Hours':
        start = webChartUtil.subtractFromMoment(1, 'days', timezone, start);
        start = webChartUtil.startOf('hour', timezone, start);
        endFixed = false;
        break;

      case 'yesterday':
        start = webChartUtil.startOf('day', timezone, start);
        end = webChartUtil.endOf('day', timezone, end);
        start = webChartUtil.subtractFromMoment(1, 'days', timezone, start);
        end = webChartUtil.subtractFromMoment(1, 'days', timezone, end);
        live = false;
        break;

      case 'weekToDate':
        start = webChartUtil.startOf('week', timezone, start);
        endFixed = false;
        break;

      case 'lastWeek':
        start = webChartUtil.subtractFromMoment(1, 'week', timezone, start);
        end = webChartUtil.subtractFromMoment(1, 'week', timezone, end);
        start = webChartUtil.startOf('week', timezone, start);
        end = webChartUtil.endOf('week', timezone, end);
        live = false;
        break;

      case 'last7Days':
        start = webChartUtil.subtractFromMoment(1, 'week', timezone, start);
        start = webChartUtil.startOf('day', timezone, start);
        live = true;
        break;

      case 'monthToDate':
        start = webChartUtil.startOf('month', timezone, start);
        endFixed = false;
        break;

      case 'lastMonth':
        start = webChartUtil.subtractFromMoment(1, 'month', timezone, start);
        end = webChartUtil.subtractFromMoment(1, 'month', timezone, end);
        start = webChartUtil.startOf('month', timezone, start);
        end = webChartUtil.endOf('month', timezone, end);
        live = false;
        break;

      case 'yearToDate':
        start = webChartUtil.startOf('year', timezone, start);
        endFixed = false;
        break;

      case 'lastYear':
        start = webChartUtil.startOf('year', timezone, start);
        end = webChartUtil.endOf('year', timezone, end);
        start = webChartUtil.subtractFromMoment(1, 'year', timezone, start);
        end = webChartUtil.subtractFromMoment(1, 'year', timezone, end);
        live = false;
        break;

      default:
        throw new Error("unknown period");
    }

    return {
      start: start,
      end: end,
      live: live,
      startFixed: startFixed,
      endFixed: endFixed
    };
  };
  /**
   * Gets the start of the specified period from the time provided based upon the
   * timezone.
   *
   * The main difference between this method and using moment directly is this
   * will account for daylight savings in the timezone.
   *
   * @param {string} period
   * @param {baja.TimeZone} timezone
   * @param {moment} fromMoment
   * @returns {moment}
   */


  webChartUtil.startOf = function (period, timezone, fromMoment) {
    var start = moment(fromMoment).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(fromMoment.toDate(), timezone));
    start.startOf(period); // Ensure that the offset is based on the offset of the new start date when crossing daylight savings boundary

    start.utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(start.toDate(), timezone), true);
    return start;
  };
  /**
   * Gets the end of the specified period from the time provided based upon the
   * timezone.
   *
   * The main difference between this method and using moment directly is this
   * will account for daylight savings in the timezone.
   *
   * @param {string} period
   * @param {baja.TimeZone} timezone
   * @param {moment} fromMoment
   * @returns {moment}
   */


  webChartUtil.endOf = function (period, timezone, fromMoment) {
    var end;

    if (period === 'week') {
      end = webChartUtil.startOf(period, timezone, fromMoment).add(6, 'd').endOf('day');
    } else {
      end = moment(fromMoment).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(fromMoment.toDate(), timezone));
      end.endOf(period);
    } // Ensure that the offset is based on the offset of the new end date when crossing daylight savings boundary


    end.utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(end.toDate(), timezone), true);
    return end;
  };
  /**
   * Subtracts from the provided moment in the given timezone.
   *
   * The main difference between this method and using moment directly is this
   * will account for daylight savings in the timezone.
   *
   * @param {string} period
   * @param {baja.TimeZone} timezone
   * @param {moment} fromMoment
   * @returns {moment}
   */


  webChartUtil.subtractFromMoment = function (amount, period, timezone, fromMoment) {
    var result = moment(fromMoment).utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(fromMoment.toDate(), timezone));
    result.subtract(amount, period);
    return result.utcOffset(dateTimeUtil.getUtcOffsetInTimeZone(result.toDate(), timezone), true);
  };
  /**
   * Determines if a date falls within two specified dates
   * @param {moment} start
   * @param {moment} end
   * @param {moment} targetDate
   * @return {boolean} true if date is in range
   */


  webChartUtil.isInTimeRange = function (start, end, targetDate) {
    var isStartSet = !start.isSame(moment(new Date(0)));
    var isEndSet = !end.isSame(moment(new Date(0)));

    if (isStartSet && isEndSet) {
      return targetDate.isSameOrAfter(start) && targetDate.isSameOrBefore(end);
    } else if (isEndSet) {
      return targetDate.isSameOrBefore(end);
    } else if (isStartSet) {
      return targetDate.isSameOrAfter(start);
    } else {
      return true;
    }
  };
  /**
   * Separate the hierarchy ords out into the supplied array and return a new array of the ords that
   * are not hierarchy ords.
   *
   * @param {Array.<String>} allOrds
   * @param {Array.<String>} hierarchyOrds
   * @param {Function} HierarchySpace
   * @returns {Array.<String>} ords that are not hierarchy ords
   */


  function separateHierarchyOrds(allOrds, hierarchyOrds, HierarchySpace) {
    var remainingOrds = [],
        i,
        ord;

    for (i = 0; i < allOrds.length; ++i) {
      ord = allOrds[i];

      if (HierarchySpace.isHierarchyOrd(ord)) {
        hierarchyOrds.push(ord);
      } else {
        remainingOrds.push(ord);
      }
    }

    return remainingOrds;
  }
  /**
   * Find the target objects to which the ords resolve.
   *
   * @param {Array.<String>} ordsToResolve
   * @param [subscriber]
   * @returns {Array.<Object>} the resolved Targets Array; an index will be Null if its
   * unresolvable. If nothing is resolvable, then the BatchResolve is rejected.
   */


  function batchResolve(ordsToResolve, subscriber) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      // resolve immediately if there is nothing to do
      if (ordsToResolve.length <= 0) {
        resolve([]);
        return;
      }

      var batchResolve = new baja.BatchResolve(ordsToResolve);
      batchResolve.resolve({
        subscriber: subscriber
      }).then(function (result) {
        resolve(result.getTargetObjects());
      })["catch"](function (err) {
        webChartUtil.log(err);
        return Promise.all(_.map(ordsToResolve, function (ord, i) {
          return batchResolve.getFail(i) ? null : batchResolve.get(i);
        })).then(function (targetObjects) {
          resolve(targetObjects);
        });
      });
    });
  }
  /**
   * Find the target LevelElems to which the hierarchy ords resolve.  LevelElems are preferred to
   * target objects because series for the hierarchy children should be added to the chart instead
   * of the children of an actual target object.
   *
   * @param {Array.<String>} ords
   * @param {Function} HierarchySpace
   * @returns {Promise.<Array.<baja.Component|null>>} the resolved `LevelElem`s; an index will be null if it is
   * unresolvable.
   */


  function hierarchyResolve(ords, HierarchySpace) {
    return Promise.all(ords.map(function (ord) {
      return HierarchySpace.resolveHierarchyLevelElem(ord)["catch"](function (err) {
        // log the error but do not reject for a single failure- allow as much to be charted as
        // possible
        webChartUtil.log(err);
        return null;
      });
    }));
  }
  /**
   * Resolve the target objects of the specified ords. Hierarchy ords are resolved to a level
   * element rather than the component to which it refers so the hierarchy children are added to the
   * chart and not the children of the component itself. The remaining ords are resolved through
   * the usual batch resolve.
   *
   * @param ordsToResolve
   * @param [subscriber]
   * @returns {Array.<Object>} the resolved Targets Array, An index will be null if its
   * unresolvable. If nothing if resolvable, then an unresolved exception is thrown.
   */


  webChartUtil.fallbackBatchResolve = function (ordsToResolve, subscriber) {
    var hierarchyOrds = [];
    var otherOrds;
    return requireHierarchySpace().then(function (HierarchySpace) {
      otherOrds = separateHierarchyOrds(ordsToResolve, hierarchyOrds, HierarchySpace);
      return Promise.all([batchResolve(otherOrds, subscriber), hierarchyResolve(hierarchyOrds, HierarchySpace)]);
    }).then(function (_ref) {
      var _ref2 = _slicedToArray(_ref, 2),
          otherObjs = _ref2[0],
          hierarchyObjs = _ref2[1];

      var i,
          ord,
          hierarchyIndex,
          otherIndex,
          obj,
          allObjs = [],
          someResolved = false;

      if (otherObjs.length <= 0 && hierarchyObjs.length <= 0) {
        throw new Error("Unresolved ORD");
      } // ensure that objects are returned in the same order as their corresponding ord to resolve


      for (i = 0; i < ordsToResolve.length; ++i) {
        ord = ordsToResolve[i];

        if ((hierarchyIndex = hierarchyOrds.indexOf(ord)) > -1) {
          obj = hierarchyObjs[hierarchyIndex];
        } else if ((otherIndex = otherOrds.indexOf(ord)) > -1) {
          obj = otherObjs[otherIndex];
        } else {
          throw new Error("Ord not found in hierarchy or other ord arrays.");
        }

        if (obj) {
          someResolved = true;
        }

        allObjs.push(obj);
      }

      if (!someResolved) {
        throw new Error("Unresolved ORD (all null)");
      }

      return allObjs;
    });
  };
  /**
   * Validate a widget and get the complex in case a ComplexDiff is given
   * @param {Widget} widget
   * @return {Promise}
   */


  webChartUtil.fullValidate = function (widget) {
    return widget.validate().then(function (diff) {
      if (diff instanceof ComplexDiff) {
        var value = widget.value().newCopy();
        return diff.apply(value);
      }

      return diff;
    });
  };
  /**
   * Read a widget and get the complex in case a ComplexDiff is given
   * @param {Widget} widget
   * @return {Promise}
   */


  webChartUtil.fullRead = function (widget) {
    return widget.read().then(function (diff) {
      if (diff instanceof ComplexDiff) {
        var value = widget.value().newCopy(true);
        return diff.apply(value);
      }

      return diff;
    });
  };
  /**
   * Resolve a promise with the StationName
   * @returns {Promise}
   */


  webChartUtil.resolveStationName = function () {
    // @since Niagara 4.14 calls the new compUtils.getStationName function
    return compUtils.getStationName();
  };
  /**
   * If the local station is used in a historyOrd, utilize the history Scheme shortcut "^" instead incase
   * the ord ends up being used in a persistable chart file. Don't modify the ord if the localStationName is
   * not used in the ord.
   * @returns {Promise}
   * @param {String} ord
   */


  webChartUtil.resolvePersistentHistoryOrd = function (ord) {
    return webChartUtil.resolveStationName().then(function (stationName) {
      var o = ord.toString(); //ensure we are using a string

      if (stationName && o) {
        return webChartUtil.getPersistentHistoryOrd(o, stationName);
      }

      return ord;
    });
  };
  /**
   * Replace the station name with the history schema shortcut "^" with the given parameters.
   * @param {string} ord
   * @param {string} stationName
   * @returns {string}
   */


  webChartUtil.getPersistentHistoryOrd = function (ord, stationName) {
    return ord.replace("history:/" + stationName + "/", "history:^");
  };
  /**
   * The number of points on a Series when we should start auto-sampling. Defaults to 2500.
   * This item can also be reconfigured in the ChartSettings on a per chart file basis.
   * @returns {Number}
   */


  webChartUtil.getDefaultAutoSamplingSize = function () {
    var that = this;

    if (that.$autoSamplingSize === undefined) {
      that.$autoSamplingSize = webChartUtil.getIntSystemProperty("niagara.webChart.autoSamplingSize", 2500);
    }

    return that.$autoSamplingSize;
  };
  /**
   * The scheduled time for auto refresh when interpolate tail is turned
   * on for the Chart in milliseconds. This will default to 100000 ms.
   *
   * @returns {Number}
   */


  webChartUtil.getInterpolateTailRefreshInterval = function () {
    return webChartUtil.getIntSystemProperty("niagara.history.interpolateRefreshInterval", 10000) || 10000;
  };
  /**
   * The max number of points on screen for a series without sampling forced on. Defaults to 50K.
   * @returns {Number}
   */


  webChartUtil.getMaxSamplingSize = function () {
    var that = this;

    if (that.$maxSamplingSize === undefined) {
      that.$maxSamplingSize = Math.max(webChartUtil.getDefaultAutoSamplingSize(), webChartUtil.getIntSystemProperty("niagara.webChart.maxSamplingSize", 50000));
    }

    return that.$maxSamplingSize;
  };
  /**
   * The max number of points available in the series, Defaults to 250K.
   * @returns {Number}
   */


  webChartUtil.getMaxSeriesCapacity = function () {
    var that = this;

    if (that.$maxSeriesCapacity === undefined) {
      that.$maxSeriesCapacity = webChartUtil.getIntSystemProperty("niagara.webChart.maxSeriesCapacity", 250000);
    }

    return that.$maxSeriesCapacity;
  };
  /**
   * Get a System.property if it is properly exposed. If its not set or not available, return the fallback Number
   * @param {String} key
   * @param {Number} fallback
   * @returns {Number}
   */


  webChartUtil.getIntSystemProperty = function (key, fallback) {
    try {
      var result = niagaraSystemProperties[key];

      if (result !== undefined) {
        return parseInt(result);
      }
    } catch (ignore) {}

    return fallback;
  };
  /**
   * Return a Deferred object. It should have three methods:
   * `resolve` that resolves the promise, `reject` that rejects the promise,
   * and `promise` that returns the Promise itself.
   * @returns {Object}
   */


  webChartUtil.deferred = function () {
    var res,
        rej,
        // eslint-disable-next-line promise/avoid-new
    _promise = new Promise(function (resolve, reject) {
      res = resolve;
      rej = reject;
    });

    return {
      resolve: res,
      reject: rej,
      promise: function promise() {
        return _promise;
      }
    };
  };
  /**
   * Simulate a user click on the desired jQuery element.
   * @param {jQuery} jq
   */


  webChartUtil.click = function (jq) {
    var clickEvent = window.document.createEvent("MouseEvent");
    clickEvent.initEvent("click", true, true);

    if (jq.get(0)) {
      jq.get(0).dispatchEvent(clickEvent);
    }
  };
  /**
   * Return array of string values
   */


  webChartUtil.csvToArray = function (text) {
    var lines = text.split(/\n/); //TODO: newlines in quotes

    var output = [];

    for (var i = 0; i < lines.length; i++) {
      // only push this line if it contains a non whitespace character.
      if (/\S/.test(lines[i])) {
        output.push($.trim(lines[i]).split(",")); //TODO: commas in quotes
      }
    }

    return output;
  };

  function requireHierarchySpace() {
    // avoid JsBuildVerificationTest failure.
    // cannot add webChart -> BHierarchyJsBuild because circular dependency.
    return doRequire('nmodule'.toLowerCase() + '/hierarchy/rc/bs/HierarchySpace');
  }

  return webChartUtil;
});
