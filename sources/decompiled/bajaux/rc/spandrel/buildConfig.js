function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */

/**
 * @private
 * @module bajaux/spandrel/buildConfig
 */
define(['bajaux/Widget', 'bajaux/spandrel/symbols', 'bajaux/spandrel/util', 'Promise', 'underscore'], function (Widget, symbols, util, Promise, _) {
  'use strict';

  var extend = _.extend,
      map = _.map,
      omit = _.omit;
  var IS_SPANDREL_SYMBOL = symbols.IS_SPANDREL_SYMBOL,
      KEY_SYMBOL = symbols.KEY_SYMBOL;
  var $DummyWidget = util.$DummyWidget,
      reify = util.reify;
  /**
   * Given spandrel input (potentially dynamically generated), spit out a JSON
   * array, where each member may potentially contain more nested data, that
   * will map to one or more fe.buildFor calls.
   *
   * @private
   * @alias module:bajaux/spandrel/buildConfig
   * @param {module:bajaux/spandrel~SpandrelData} arg
   * @param {module:bajaux/spandrel~WidgetState} [widgetState] configuration data derived
   * from the parent widget to contain all these spandrel-generated widgets
   * @param {module:bajaux/Widget} owner the owner, or "top-level", widget. When generating a tree
   * of spandrel data (as most widgets do), buildConfig will be called recursively, but the
   * top-level owner widget must remain known at each step.
   * @returns {Promise.<module:bajaux/spandrel~BuildContext>}
   */

  function buildConfig(arg) {
    var widgetState = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {
      properties: {}
    };
    var owner = arguments.length > 2 ? arguments[2] : undefined;
    var ownerState = arguments.length > 3 && arguments[3] !== undefined ? arguments[3] : widgetState;

    if (!owner) {
      throw new Error('owner widget required');
    }

    widgetState = extend({
      props: widgetState.properties
    }, widgetState);
    var normalizedOn = [];
    /**
     * @param {module:bajaux/spandrel~SpandrelData|Function} spandrelArg
     * @param {string} key spandrel key for this member
     * @returns {Promise.<module:bajaux/spandrel~Member>}
     */

    var toSpandrelMember = function toSpandrelMember(spandrelArg, key) {
      return reify(spandrelArg, ownerState).then(function (spandrelArg) {
        key = String(spandrelArg && spandrelArg[KEY_SYMBOL] || key || '0');
        return Promise.resolve(handleSpandrelArg(spandrelArg, key, widgetState, owner, ownerState)).then(function (member) {
          normalizeStateBinding(member, owner);

          if (!spandrelArg) {
            return member;
          }

          normalizedOn = normalizedOn.concat(normalizeOn(spandrelArg.on, key));
          return member;
        });
      });
    };

    if (isHtml(arg) || !arg) {
      return Promise.all([toSpandrelMember(arg, 0)]).then(function (members) {
        return {
          members: members,
          on: []
        };
      });
    }

    if (Array.isArray(arg)) {
      if (!arg.length) {
        arg = [undefined];
      }

      return Promise.all(arg.map(toSpandrelMember)).then(function (members) {
        return {
          members: members,
          on: normalizedOn
        };
      });
    }

    if (_typeof(arg) === 'object') {
      if (arg[IS_SPANDREL_SYMBOL]) {
        return buildConfig([arg], widgetState, owner);
      }

      var _arg = arg,
          on = _arg.on,
          kids = _arg.kids;

      if (kids || on) {
        if (arg.dom) {
          // if dom is specified, we want one kid widget, with kids built under that.
          return Promise.all([toSpandrelMember(arg, '0')]).then(function (members) {
            return {
              members: members,
              on: normalizedOn
            };
          });
        } else {
          normalizedOn = normalizeOn(on); // if no dom is specified, we want kids built directly.

          return Promise.all(map(kids, function (kid, key) {
            return toSpandrelMember(kid, String(key));
          })).then(function (members) {
            return {
              members: members,
              on: normalizedOn
            };
          });
        }
      }

      return Promise.all(Object.keys(arg).map(function (k) {
        return toSpandrelMember(arg[k], k);
      })).then(function (members) {
        return {
          members: members,
          on: normalizedOn || []
        };
      });
    }

    return Promise.reject(new Error('cannot configure spandrel widget from ' + arg));
  }
  /**
   * @param {Array|Object} on the `on` event handlers as provided by the user. The
   * members may be of the form `[ event, handler ]` or `[ event, selector, handler ]`.
   * It may also be an object literal mapping event names to handlers (no selectors
   * allowed in this form).
   * @param {string} [key] since the result handlers will be applied one level
   * up, if the input handlers should apply only to one nested widget key, the
   * key will be prepended to the selectors.
   * @returns {Array} the normalized `on` event handler arrays to be included
   * in the final BuildContext.
   */


  function normalizeOn(on, key) {
    if (!on) {
      return [];
    }

    if (on.constructor === Object) {
      on = Object.keys(on).map(function (eventName) {
        return [eventName, on[eventName]];
      });
    }

    if (!on.length) {
      return [];
    }

    if (!Array.isArray(on[0])) {
      on = [on];
    }

    return on.map(function (on) {
      var _on = _slicedToArray(on, 3),
          event = _on[0],
          selector = _on[1],
          handler = _on[2];

      if (on.length < 3) {
        handler = selector;

        if (key) {
          selector = key + '/**/*';
        } else {
          selector = '**/*';
        }
      } else {
        if (key) {
          selector = key + '/' + selector;
        }
      }

      return [event, selector, handler];
    });
  }
  /**
   * Spandrel can take string values, arrays of values, or key -> value maps.
   * This function will handle one value in a potential collection of
   * arguments.
   *
   * @param {module:bajaux/spandrel~SpandrelData} spandrelArg
   * @param {string} key
   * @param {object} widgetState
   * @param {module:bajaux/Widget} owner
   * @param {module:bajaux/spandrel~WidgetState} ownerState
   * @returns {object|Promise.<object>} object with key and config properties
   */


  function handleSpandrelArg(spandrelArg, key, widgetState, owner, ownerState) {
    if (!spandrelArg) {
      return {
        key: key,
        config: {
          dom: '<wbr style="display: none;">',
          type: $DummyWidget
        },
        on: []
      };
    }

    if (isHtml(spandrelArg)) {
      return {
        key: key,
        config: rawStringToConfig(spandrelArg)
      };
    } else {
      if (spandrelArg.key && spandrelArg.config) {
        //already a spandrel object
        //TODO: use a non-enumerable property instead of guessing
        return spandrelArg;
      }

      return Promise.resolve(objectToConfig(spandrelArg, widgetState, owner, ownerState)).then(function (config) {
        var member = {
          key: key,
          config: config
        };
        var bindKey = config.bindKey,
            stateBinding = config.stateBinding,
            lax = config.lax;

        if (bindKey) {
          member.bindKey = bindKey;
          delete config.bindKey;
        } else if (config.bind) {
          member.bindKey = key;
          delete config.bind;
        }

        if (stateBinding) {
          member.stateBinding = stateBinding;
          delete config.stateBinding;
        }

        if (lax) {
          member.lax = true;
          delete config.lax;
        }

        return member;
      });
    }
  }

  function isHtml(dom) {
    return typeof dom === 'string' || dom instanceof Element;
  }
  /**
   * @param str raw html
   * @returns {object} config object that can be passed to fe.buildFor
   */


  function rawStringToConfig(str) {
    return {
      dom: str,
      formFactor: 'mini'
    };
  }
  /**
   * @param {module:bajaux/spandrel~SpandrelData} spandrelArg arg as given to spandrel
   * @param {module:bajaux/spandrel~WidgetState} widgetState parent widget config
   * @param {module:bajaux/Widget} owner
   * @param {module:bajaux/spandrel~WidgetState} ownerState
   * @returns {object|Promise.<object>} config object that can be passed to
   * fe.buildFor
   */


  function objectToConfig(spandrelArg, widgetState, owner, ownerState) {
    var kids = spandrelArg.kids;
    var widgetProps = widgetState.properties;
    var argProps = spandrelArg.properties;

    if (typeof argProps === 'function') {
      argProps = argProps(extend({}, widgetProps));
    } else if (argProps === 'inherit') {
      argProps = ownerState.properties;
    }

    var config = extend({}, spandrelArg, {
      formFactor: spandrelArg.formFactor || 'mini'
    });

    if (argProps) {
      config.properties = argProps;
    }

    if (config.readonly === 'inherit' || config.readonly === undefined) {
      config.readonly = ownerState.readonly;
    }

    if (config.enabled === 'inherit' || config.enabled === undefined) {
      config.enabled = ownerState.enabled;
    }

    config.writable = config.enabled && !config.readonly;

    if (kids) {
      return buildConfig(kids, config, owner, ownerState).then(function (kids) {
        return extend(omit(config, 'on'), {
          kids: kids
        });
      });
    }

    return omit(config, 'on');
  }
  /**
   * The "source of truth" for state bindings is `stateBinding` but the public API is to
   * specify a `bindKey`. If `bindKey` is present, convert it to `stateBinding`. This modifies the
   * member passed in.
   *
   * @param {module:bajaux/spandrel~Member} member
   * @param {bajaux/Widget} owner
   */


  function normalizeStateBinding(member, owner) {
    var bindKey = member.bindKey;

    if (bindKey) {
      member.stateBinding = {
        key: bindKey,
        target: owner
      };
      delete member.bindKey;
    }
  }

  return buildConfig;
});
