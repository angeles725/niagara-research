/**
 * @copyright 2020 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */
define(['log!bajaux.spandrel', 'underscore'], function (log, _) {
  'use strict';

  var isArray = _.isArray,
      isEqual = _.isEqual,
      isFunction = _.isFunction;
  var EMPTY_CONTEXT = {
    members: [],
    on: []
  };
  var logFinest = log.finest.bind(log);
  var TRACE = log.isLoggable('FINEST');
  /**
   * API Status: **Private**
   * @exports bajaux/spandrel/diff
   */

  var exports = {};

  var same = function same(l, r) {
    return l === r;
  };

  var sameString = function sameString(l, r) {
    return String(l) === String(r);
  };

  var configEqualityTesters = {
    complex: same,
    data: function data() {
      var l = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
      var r = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
      return deepEquals(l, r);
    },
    dom: same,
    enabled: function enabled(l, r) {
      return l !== false === (r !== false);
    },
    formFactor: same,
    formFactors: function formFactors(l, r) {
      if (typeof l === 'string') {
        l = [l];
      }

      if (typeof r === 'string') {
        r = [r];
      }

      return deepEquals(l, r);
    },
    loadParams: deepEquals,
    properties: function properties() {
      var l = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
      var r = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
      var lKeys = Object.keys(l);
      var rKeys = Object.keys(r);
      var len = lKeys.length;

      if (len !== rKeys.length) {
        return false;
      }

      for (var i = 0; i < len; i++) {
        var lKey = lKeys[i];
        var rKey = rKeys[i];

        if (lKey !== rKey || !deepEquals(l[lKey], r[rKey])) {
          return false;
        }
      }

      return true;
    },
    readonly: function readonly(l, r) {
      return !l === !r;
    },
    slot: sameString,
    type: same,
    validate: same,
    value: function value(l, r) {
      return equalsByMethod(l, r);
    }
  };
  var KNOWN_KEYS = Object.keys(configEqualityTesters);
  /**
   * The purpose of this function is to return results that are completely
   * compatible with the `deep-diff` module spandrel was originally built on.
   * `deep-diff` had the problem of not being able to handle circular references
   * in objects, and basically every BajaScript Complex has a circular reference
   * in it, so diffing a Component as a value to load locked up the browser.
   * Oops!
   *
   * This does not do a true deep-diff of two objects, but a customized method
   * that caters specifically to spandrel build contexts.
   *
   * @param {module:bajaux/spandrel~BuildContext} lhs
   * @param {module:bajaux/spandrel~BuildContext} rhs
   * @returns {object[]} array of diff objects
   */

  exports.diffBuildContexts = function (lhs, rhs) {
    return diffBuildContexts(lhs, rhs, []);
  };
  /**
   * @param {module:bajaux/spandrel~BuildContext} lhs
   * @param {module:bajaux/spandrel~BuildContext} rhs
   * @param {Array.<string|number>} path current path through the build
   * contexts. This will be empty for the root context, and for nested kid
   * contexts will look like `'members', arrayIndex, 'config', 'kids'`, etc.
   * @returns {object[]} array of diff objects
   */


  function diffBuildContexts() {
    var lhs = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : EMPTY_CONTEXT;
    var rhs = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : EMPTY_CONTEXT;
    var path = arguments.length > 2 ? arguments[2] : undefined;
    var lMembers = lhs.members;
    var rMembers = rhs.members;
    var diffs = [];

    for (var i = 0, len = Math.max(lMembers.length, rMembers.length); i < len; ++i) {
      var lMember = lMembers[i];
      var rMember = rMembers[i];
      var memberPath = path.concat('members');

      if (rMember && !lMember) {
        diffs.push(addDiff(rMember, i, memberPath));
      } else if (lMember && !rMember) {
        diffs.push(removeDiff(lMember, i, memberPath));
      } else {
        diffs = diffs.concat(diffMember(lMember, rMember, memberPath.concat(i)));
      }
    }

    diffs = diffs.concat(diffOn(lhs.on, rhs.on, path.concat('on')));
    return diffs;
  }
  /**
   * @param {module:bajaux/spandrel~Member} lhs
   * @param {module:bajaux/spandrel~Member} rhs
   * @param {Array.<string|number>} path
   * @returns {object[]} array of diff objects
   */


  function diffMember(lhs, rhs, path) {
    var lConfig = lhs.config;
    var rConfig = rhs.config;
    var diffs = [];
    path = path.concat('config');

    for (var i = 0, len = KNOWN_KEYS.length; i < len; i++) {
      var key = KNOWN_KEYS[i];
      var lProp = lConfig[key];
      var rProp = rConfig[key];

      if (!configEqualityTesters[key](lProp, rProp)) {
        diffs.push(editDiff(lProp, rProp, path.concat(key)));
      }
    }

    return diffs.concat(diffBuildContexts(lConfig.kids, rConfig.kids, path.concat('kids')));
  }
  /**
   * Diff the `on` property of a build context (the configured event handlers).
   * @param {Array} l
   * @param {Array} r
   * @param {Array.<string|number>} path
   * @returns {object[]} array of diff objects
   */


  function diffOn() {
    var l = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];
    var r = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];
    var path = arguments.length > 2 ? arguments[2] : undefined;

    if (onDiffers(l, r)) {
      return [editDiff(l, r, path)];
    } else {
      return [];
    }
  }
  /**
   * @param {Array} l
   * @param {Array} r
   * @returns {boolean}
   */


  function onDiffers() {
    var l = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];
    var r = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];

    for (var i = 0, len = Math.max(l.length, r.length); i < len; i++) {
      var lEl = l[i];
      var rEl = r[i];

      if (isArray(lEl)) {
        if (!isArray(rEl)) {
          return true;
        }

        return onDiffers(lEl, rEl);
      } else if (lEl !== rEl) {
        return true;
      }
    }
  }
  /**
   * @param {*} l
   * @param {*} r
   * @returns {boolean} true if the lhs has an `equals` method and it returns
   * true when passed the rhs.
   */


  function equalsByMethod(l, r) {
    if (l === r) {
      if (TRACE && Array.isArray(l) && Array.isArray(r)) {
        logFinest("Two arrays were identity-equal across renders. spandrel may fail to show changes." + "Please see spandrel tutorial for details.");
      }

      return true;
    }

    if (Array.isArray(l) && Array.isArray(r)) {
      var llen = l.length;
      var rlen = r.length;

      if (llen !== rlen) {
        return false;
      }

      for (var i = 0; i < llen; ++i) {
        if (!equalsByMethod(l[i], r[i])) {
          return false;
        }
      }

      return true;
    }

    if (isObjectLiteral(l) && isObjectLiteral(r)) {
      var lkeys = Object.keys(l);
      var rkeys = Object.keys(r);
      var _llen = lkeys.length;
      var _rlen = rkeys.length;

      if (_llen !== _rlen) {
        return false;
      }

      for (var _i = 0; _i < _llen; ++_i) {
        var lkey = lkeys[_i];
        var rkey = rkeys[_i];

        if (lkey !== rkey || !equalsByMethod(l[lkey], r[rkey])) {
          return false;
        }
      }

      return true;
    }

    return l && isFunction(l.equals) && l.equals(r);
  }
  /**
   * @param {*} a
   * @param {*} b
   * @returns {boolean} a deep-equals as performed by Underscore
   */


  function deepEquals(a, b) {
    return isEqual(a, b);
  }
  /**
   * @param {*} lhs
   * @param {*} rhs
   * @param {Array.<string|number>} path
   * @returns {Object} a diff object for an edit operation
   */


  function editDiff(lhs, rhs, path) {
    return {
      kind: 'E',
      path: path,
      lhs: lhs,
      rhs: rhs
    };
  }
  /**
   * @param {*} rhs the object added to an array
   * @param {number} index the index at which it was added
   * @param {Array.<string|number>} path the path to the array
   * @returns {Object} a diff object for an add-to-array operation
   */


  function addDiff(rhs, index, path) {
    return {
      kind: 'A',
      path: path,
      index: index,
      item: {
        kind: 'N',
        rhs: rhs
      }
    };
  }
  /**
   * @param {*} lhs the object removed from an array
   * @param {number} index the index from which it was removed
   * @param {Array.<string|number>} path the path to the array
   * @returns {Object} a diff object for a remove-from-array operation
   */


  function removeDiff(lhs, index, path) {
    return {
      kind: 'A',
      path: path,
      index: index,
      item: {
        kind: 'D',
        lhs: lhs
      }
    };
  }

  function isObjectLiteral(obj) {
    return obj && obj.constructor === Object;
  }

  return exports;
});
