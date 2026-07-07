function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _get(target, property, receiver) { if (typeof Reflect !== "undefined" && Reflect.get) { _get = Reflect.get; } else { _get = function _get(target, property, receiver) { var base = _superPropBase(target, property); if (!base) return; var desc = Object.getOwnPropertyDescriptor(base, property); if (desc.get) { return desc.get.call(receiver); } return desc.value; }; } return _get(target, property, receiver || target); }

function _superPropBase(object, property) { while (!Object.prototype.hasOwnProperty.call(object, property)) { object = _getPrototypeOf(object); if (object === null) break; } return object; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _createSuper(Derived) { var hasNativeReflectConstruct = _isNativeReflectConstruct(); return function _createSuperInternal() { var Super = _getPrototypeOf(Derived), result; if (hasNativeReflectConstruct) { var NewTarget = _getPrototypeOf(this).constructor; result = Reflect.construct(Super, arguments, NewTarget); } else { result = Super.apply(this, arguments); } return _possibleConstructorReturn(this, result); }; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _isNativeReflectConstruct() { if (typeof Reflect === "undefined" || !Reflect.construct) return false; if (Reflect.construct.sham) return false; if (typeof Proxy === "function") return true; try { Date.prototype.toString.call(Reflect.construct(Date, [], function () {})); return true; } catch (e) { return false; } }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

/**
 * @copyright 2021 Tridium, Inc. All Rights Reserved.
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module bajaux/spandrel/DynamicSpandrelWidget
 */
define(['log!bajaux.spandrel', 'bajaux/spandrel/buildConfig', 'bajaux/spandrel/diff', 'bajaux/spandrel/DiffQueue', 'bajaux/spandrel/logging', 'bajaux/spandrel/SpandrelRenderQueue', 'bajaux/spandrel/SpandrelWidget', 'bajaux/spandrel/symbols', 'bajaux/spandrel/util', 'bajaux/spandrel/workflow', 'bajaux/Widget', 'jquery', 'Promise', 'underscore', 'nmodule/js/rc/asyncUtils/asyncUtils', 'nmodule/js/rc/log/Log'], function (log, buildConfig, diff, DiffQueue, logging, SpandrelRenderQueue, SpandrelWidget, symbols, util, workflow, Widget, $, Promise, _, asyncUtils, Log) {
  'use strict';

  var logWarning = log.warning.bind(log);
  var isLifecycleLoggable = logging.isLifecycleLoggable,
      areLifecycleDetailsLoggable = logging.areLifecycleDetailsLoggable,
      areTimingDetailsLoggable = logging.areTimingDetailsLoggable,
      areDomDetailsLoggable = logging.areDomDetailsLoggable,
      logLifecycle = logging.logLifecycle,
      logLifecycleDetails = logging.logLifecycleDetails,
      logTimingDetails = logging.logTimingDetails,
      logDomDetails = logging.logDomDetails,
      widgetName = logging.widgetName;
  var $deferred = asyncUtils.$deferred;
  var DEPTH_SYMBOL = symbols.DEPTH_SYMBOL,
      IS_DYNAMIC_SYMBOL = symbols.IS_DYNAMIC_SYMBOL,
      LAX_SYMBOL = symbols.LAX_SYMBOL,
      RENDER_TICKS_SYMBOL = symbols.RENDER_TICKS_SYMBOL,
      ROOT_SYMBOL = symbols.ROOT_SYMBOL,
      STATE_BINDING_SYMBOL = symbols.STATE_BINDING_SYMBOL,
      WIDGET_SYMBOL = symbols.WIDGET_SYMBOL;
  var diffBuildContexts = diff.diffBuildContexts;
  var difference = _.difference,
      extend = _.extend,
      isFunction = _.isFunction,
      last = _.last,
      lastIndexOf = _.lastIndexOf,
      noop = _.noop,
      once = _.once;
  var cloneNode = util.cloneNode,
      compareTicks = util.compareTicks,
      getInlineStyles = util.getInlineStyles,
      hasFocus = util.hasFocus,
      isDynamic = util.isDynamic,
      kidsMatching = util.kidsMatching,
      requiresRebuild = util.requiresRebuild,
      setEnabledSilently = util.setEnabledSilently,
      setPropertiesSilently = util.setPropertiesSilently,
      setReadonlySilently = util.setReadonlySilently,
      setValidationOfDescendent = util.setValidationOfDescendent,
      toEl = util.toEl,
      updateElement = util.updateElement;
  var armHandlers = workflow.armHandlers,
      buildChildWidgetsFromSpandrelData = workflow.buildChildWidgetsFromSpandrelData,
      doFeBuild = workflow.doFeBuild,
      nextTicks = workflow.nextTicks,
      trackRenders = workflow.trackRenders;
  var PATH_TO_KID_CONTEXTS = ['config', 'kids', 'members']; // a private state variable indicating that we're willing to abandon changes to
  // modified widgets during a rerender.

  var WIPE_MODIFIED_SYMBOL = Symbol('wipeModified'); // avoid circular dependency

  var spandrel = once(function () {
    return require('bajaux/spandrel');
  });
  /**
   * @class
   * @alias module:bajaux/spandrel/DynamicSpandrelWidget
   * @extends module:bajaux/spandrel/SpandrelWidget
   */

  var DynamicSpandrelWidget = /*#__PURE__*/function (_SpandrelWidget) {
    _inherits(DynamicSpandrelWidget, _SpandrelWidget);

    var _super = _createSuper(DynamicSpandrelWidget);

    function DynamicSpandrelWidget() {
      _classCallCheck(this, DynamicSpandrelWidget);

      return _super.apply(this, arguments);
    }

    _createClass(DynamicSpandrelWidget, [{
      key: "initialize",

      /**
       * @param {JQuery} dom
       * @param {object} [params]
       * @param {object} [layoutParams]
       * @returns {Promise}
       */
      value: function initialize(dom, params, layoutParams) {
        var _this = this;

        dom[0][WIDGET_SYMBOL] = this;
        return Promise.resolve(this.$resolveManager()).then(function (manager) {
          _this.$manager = manager;
          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "initialize", _this).call(_this, dom, params, Object.assign({
            quick: true
          }, layoutParams));
        });
      }
      /**
       * When loading a new value into a dynamic `spandrel` widget, the `spandrel` data will be
       * generated/regenerated by the `spandrel` function used to define the class, and used by
       * `spandrel` to build out the widget tree into the DOM.
       *
       * @param {*} value
       * @param {object} [loadParams]
       * @returns {Promise}
       */

    }, {
      key: "doLoad",
      value: function doLoad(value, loadParams) {
        var _this2 = this;

        var timingDetailsLoggable = areTimingDetailsLoggable();
        var wipeModifications = (loadParams && loadParams[WIPE_MODIFIED_SYMBOL]) !== false;
        var myRenderTicks = this[RENDER_TICKS_SYMBOL]; // if there are outstanding updates to state, we don't have to rerender
        // at this time. but we do have to write them through so they count as a
        // previous render, so the render from this doLoad can correctly diff
        // against them.

        if (timingDetailsLoggable) {
          logTimingDetails(this, '#doLoad called with {}, ticks {}, wipeModified {}', value, myRenderTicks, wipeModifications);
        }

        return Promise.resolve(this.$hasPendingStateUpdates() && this.$flushStateQueue()).then(function () {
          var modified = _this2.isModified();

          if (timingDetailsLoggable) {
            if (modified) {
              logTimingDetails(_this2, '#doLoad: rerendering because it was modified, ticks {}', myRenderTicks);
            } else {
              logTimingDetails(_this2, '#doLoad: skipping rerender because it was not modified, ticks {}', myRenderTicks);
            }
          }

          return Promise.all([_this2.toState(value), modified && _this2.$doRerender(loadingState(_this2, wipeModifications, myRenderTicks))["catch"](noop)]);
        }).then(function (_ref) {
          var _ref2 = _slicedToArray(_ref, 1),
              state = _ref2[0];

          if (state) {
            _this2.$applyState(state, false);
          }

          if (timingDetailsLoggable) {
            logTimingDetails(_this2, '#doLoad performing render work, ticks {}', myRenderTicks);
          }

          return _this2.render(value, loadingState(_this2, wipeModifications, myRenderTicks));
        });
      }
    }, {
      key: "doRead",
      value: function doRead(params) {
        if (isFunction(this.fromState)) {
          var state = params && params.ignoreBindErrors ? this.$stateIgnoringBindErrors() : this.state();
          return this.fromState(state);
        } else {
          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "doRead", this).apply(this, arguments);
        }
      }
      /**
       * Optionally override to set up the widget's initial state based on the
       * value being loaded in.
       * @param {*} value
       * @returns {object|Promise.<object>} an object literal representing the
       * widget's initial state
       */

    }, {
      key: "toState",
      value: function toState(value) {}
      /**
       * Cleanup any added classes and/or styles
       * @override
       * @param {object} [params] optional parameters to be passed to `doDestroy`
       * @returns {Promise}
       */

    }, {
      key: "destroy",
      value: function destroy(params) {
        var _this$state = this.state(),
            rootElement = _this$state.rootElement;

        if (rootElement) {
          var classList = rootElement.classList,
              style = rootElement.style;

          var _ref3 = this.$rootElModifications || {},
              _ref3$addedClasses = _ref3.addedClasses,
              addedClasses = _ref3$addedClasses === void 0 ? [] : _ref3$addedClasses,
              _ref3$addedStyles = _ref3.addedStyles,
              addedStyles = _ref3$addedStyles === void 0 ? {} : _ref3$addedStyles;

          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "destroy", this).apply(this, arguments).then(function () {
            addedClasses.forEach(function (c) {
              return classList.remove(c);
            });
            Object.keys(addedStyles).forEach(function (k) {
              style[k] = '';
            });
          });
        }

        return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "destroy", this).apply(this, arguments);
      }
      /**
       * Sometimes, a `validate` keyword on a child widget might be determined by a state key. If
       * there is a state update pending, we want to let that flush through before performing a
       * validation, so the validation behavior of child widgets is fully up-to-date before we start.
       *
       * @returns {Promise}
       */

    }, {
      key: "validate",
      value: function validate() {
        var _arguments = arguments,
            _this3 = this;

        return this.$renderQueue.$poke().then(function () {
          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "validate", _this3).apply(_this3, _arguments);
        });
      }
      /**
       * Optionally override to reconstitute a value based on the widget's
       * current state. The opposite of `toState()`.
       *
       * Do not call `read()` or `doRead()` from this method, as it takes the
       * place of `doRead()` if implemented.
       *
       * @function fromState
       * @abstract
       * @memberOf DynamicSpandrelWidget
       * @param {module:bajaux/spandrel~WidgetState} state current widget state
       * @returns {object|Promise.<object>} an object that is of the same type
       * passed to `toState`, and will be used to resolve `doRead()`
       */

      /**
       * Generates the `spandrel` data needed for the next render cycle, and tracks any desired changes
       * to `rootElement` that occur during the `spandrel` function.
       *
       * @private
       * @param {*} value the value being loaded
       * @param {module:bajaux/spandrel~WidgetState} state the widget state
       * @returns {Promise.<module:bajaux/spandrel~SpandrelRenderResults>} data
       * produced by the widget, ready to be applied to the DOM
       */

    }, {
      key: "$prepRenderData",
      value: function $prepRenderData(value, state) {
        var _this4 = this;

        var rootElement = state.rootElement;
        var oldClass = rootElement.getAttribute('class');
        var oldStyle = rootElement.getAttribute('style');
        var oldStyles = getInlineStyles(rootElement); // here, we make a copy because:
        // - we need to keep track of precisely what classes the widget adds to
        //   its classList, therefore we have to start with a blank classList.
        // - if we just set the classList to empty on the real live element,
        //   class-based selectors will stop working if the selection happens
        //   while the element is in the 'in-between' state while
        //   $generateSpandrelData is being called.

        var copyClassList = document.createElement(rootElement.tagName).classList;
        var origClassList = rootElement.classList;
        setClassList(rootElement, copyClassList);
        return this.$generateSpandrelData(value, state).then(function (spandrelArg) {
          var addedClasses = Array.prototype.slice.call(copyClassList);
          var addedStyles = getInlineStyles(rootElement); // record styles explicitly removed

          var oldProps = Object.keys(oldStyles);

          for (var i = 0, len = oldProps.length; i < len; i++) {
            var prop = oldProps[i];

            if (oldStyles[prop] && !addedStyles[prop]) {
              addedStyles[prop] = '';
            }
          }

          setClassList(rootElement, origClassList);
          attr(rootElement, 'class', oldClass);
          attr(rootElement, 'style', oldStyle);
          rootElement[IS_DYNAMIC_SYMBOL] = true;
          _this4.$rootElModifications = {
            addedClasses: addedClasses,
            addedStyles: addedStyles
          };
          return {
            spandrelArg: spandrelArg,
            addedClasses: addedClasses,
            addedStyles: addedStyles
          };
        });
      }
      /**
       * Pass the given value back through the spandrel processing function,
       * regenerating the widget's structure as appropriate. This differs from
       * `load()` in that it changes the DOM structure only - it does not fire
       * load events or change the result of `this.value()`.
       *
       * @param {*} value
       * @param {module:bajaux/spandrel~WidgetState} [state] widget
       * state to use; if omitted the widget's current state will be used
       * @returns {Promise}
       */

    }, {
      key: "render",
      value: function render(value, state) {
        var _this5 = this;

        var firstRenderDf = this.$firstRenderDf || (this.$firstRenderDf = $deferred());
        state = state || this.state();
        return Promise["try"](function () {
          return _this5.isInitialized() && _this5.$prepareForNextRender(value, state).then(function (_ref4) {
            var prev = _ref4.prev,
                curr = _ref4.curr;
            return _this5.$applyDiffs(prev, curr, state);
          }).then(function () {
            firstRenderDf.resolve();

            _this5.$undirtyState();

            return _this5.layout({
              quick: _this5 !== _this5[ROOT_SYMBOL]
            });
          });
        })["catch"](function (err) {
          firstRenderDf.resolve();

          if (!_this5.$manager) {
            throw err;
          }

          return _this5.$manager.error(err, _this5);
        });
      } // TODO: rerender() needs to handle when read() resolves a type that is
      // not compatible with load().

      /**
       * Triggers a rerender of this widget.
       *
       * Multiple rerender requests that occur at the same time (perhaps in response to changes in
       * more than one data field) may not always result in multiple individual rerender cycles.
       * Rerender requests that occur close together will coalesce for better performance. If your
       * call to `rerender()` coalesces into another concurrent call, it will resolve after that
       * other call completes.
       *
       * In short, `rerender()` will either trigger its own rerender, or fold into another, and
       * resolve its promise after a full rerender cycle completes.
       *
       * If this is called before the widget has completed even one load/render cycle, it will be a
       * no-op. (`rerender()` has to have something to "re" render against.)
       *
       * @param {module:bajaux/spandrel~WidgetState} [state]
       * @returns {Promise}
       */

    }, {
      key: "rerender",
      value: function rerender(state) {
        var _this6 = this;

        if (areTimingDetailsLoggable()) {
          logTimingDetails(this, 'rerender() called, waiting on render queue (may be cancelled by another rerender)');
        }

        return this.$renderQueue.fromRerender(function () {
          return _this6.$doRerender(state);
        });
      }
    }, {
      key: "load",
      value: function load() {
        var _arguments2 = arguments,
            _this7 = this;

        if (areTimingDetailsLoggable()) {
          logTimingDetails(this, 'load() called, waiting on render queue');
        }

        return this.$renderQueue.fromLoad(function () {
          return _this7.isInitialized() && _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "load", _this7).apply(_this7, _arguments2);
        });
      }
    }, {
      key: "read",
      value: function read() {
        var _arguments3 = arguments,
            _this8 = this;

        return this.$renderQueue.fromRead(function () {
          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "read", _this8).apply(_this8, _arguments3);
        });
      }
      /**
       * Performs the actual rerendering work. This must be done direct from a
       * rerender() call, of course, but a doLoad() must also call this to flush
       * pending changes before the new value can load cleanly.
       *
       * @private
       * @param {module:bajaux/spandrel~WidgetState} state
       * @returns {Promise}
       */

    }, {
      key: "$doRerender",
      value: function $doRerender(state) {
        var _this9 = this;

        var timingDetailsLoggable = areTimingDetailsLoggable();

        if (!this.$firstRenderDf) {
          if (timingDetailsLoggable) {
            logTimingDetails(this, '$doRerender performing no work because nothing rendered yet');
          }

          return Promise.resolve();
        }

        if (timingDetailsLoggable) {
          logTimingDetails(this, '$doRerender called - waiting on first render promise to complete');
        }

        return this.$firstRenderDf.promise.then(function () {
          state = state || _this9.$stateIgnoringBindErrors();
          var ticks = state[RENDER_TICKS_SYMBOL] || (state[RENDER_TICKS_SYMBOL] = nextTicks());

          if (timingDetailsLoggable) {
            logTimingDetails(_this9, 'beginning $doRerender with new ticks {}', ticks);
          } // we still want to be able to rerender even if a bound widget has failed
          // to read.


          return _this9.$read({
            ignoreBindErrors: true
          }).then(function (v) {
            if (timingDetailsLoggable) {
              logTimingDetails(_this9, '$doRerender read out value to rerender {} corresponding to ticks {}', v, ticks);
            }

            return _this9.render(v, state);
          }, function (err) {
            if (_this9.isDestroyed()) {
              if (timingDetailsLoggable) {
                logTimingDetails(_this9, 'Abandoning rerender because it was destroyed, ticks {}', ticks);
              }
            } else {
              if (timingDetailsLoggable) {
                logTimingDetails(_this9, '$doRerender failed with error {}', err);
              }

              throw err;
            }
          }).then(function () {
            if (timingDetailsLoggable) {
              logTimingDetails(_this9, '$doRerender completed for ticks {}', ticks);
            }
          });
        });
      }
      /**
       * @private
       * @returns {boolean} true if the widget has rendered at least once
       */

    }, {
      key: "$hasCompletedFirstRender",
      value: function $hasCompletedFirstRender() {
        var df = this.$firstRenderDf;
        return df && df.state === 'resolved';
      }
      /**
       * This gets called as part of a diffing phase when a child is rerendered. The ticks passed in
       * should be the same ticks the parent is currently rerendering with. This allows the "most
       * recently rendered" checks on children to be done in the context of the parent rerender -
       * if the child gets rerendered as part of the parent rerender, it's still fair game for new
       * values to get loaded into it, even though it might otherwise have more recent ticks.
       *
       * @private
       * @param {number} ticks
       * @returns {Promise}
       */

    }, {
      key: "$rerenderFromTicks",
      value: function $rerenderFromTicks(ticks) {
        var state = this.$stateIgnoringBindErrors();
        state[RENDER_TICKS_SYMBOL] = ticks;
        return this.$doRerender(state);
      }
    }, {
      key: "layout",
      value: function layout() {
        var _arguments4 = arguments,
            _this10 = this;

        if (!this.$firstRenderDf) {
          return Promise.resolve();
        }

        return this.$firstRenderDf.promise.then(function () {
          return _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "layout", _this10).apply(_this10, _arguments4);
        });
      }
    }, {
      key: "doReadonly",
      value: function doReadonly(readonly) {
        if (areTimingDetailsLoggable()) {
          logTimingDetails(this, 'triggering a rerender from readonly set to {}', readonly);
        }

        return this.rerender();
      }
    }, {
      key: "doEnabled",
      value: function doEnabled(enabled) {
        if (areTimingDetailsLoggable()) {
          logTimingDetails(this, 'triggering a rerender from enabled set to {}', enabled);
        }

        return this.rerender();
      }
    }, {
      key: "changed",
      value: function changed() {
        return Promise.resolve(this.$hasCompletedFirstRender() && _get(_getPrototypeOf(DynamicSpandrelWidget.prototype), "changed", this).apply(this, arguments));
      }
    }, {
      key: "doChanged",
      value: function doChanged() {
        if (areTimingDetailsLoggable()) {
          logTimingDetails(this, 'triggering a rerender from a property change');
        }

        return this.rerender();
      }
      /**
       * This renders the spandrel data out as the widget wishes for it to be
       * rendered. It will be cached on the widget, so that it can be compared
       * against the next render for diffing purposes.
       *
       * @private
       * @param {*} value the value being loaded into the dynamic spandrel widget
       * @param {module:bajaux/spandrel~WidgetState} [state]
       * @returns {Promise.<{ prev: module:bajaux/spandrel~SpandrelRenderResults|undefined, curr: module:bajaux/spandrel~SpandrelRenderResults }>} to
       * be resolved with the results of the previous and current renders, ready
       * to be diffed and applied to the DOM
       */

    }, {
      key: "$prepareForNextRender",
      value: function $prepareForNextRender(value, state) {
        var _this11 = this;

        return this.$prepRenderData(value, state).then(function (_ref5) {
          var spandrelArg = _ref5.spandrelArg,
              addedClasses = _ref5.addedClasses,
              addedStyles = _ref5.addedStyles;

          var widgetState = _this11.$stateIgnoringBindErrors();

          return buildConfig(spandrelArg, widgetState, _this11).then(function (spandrelData) {
            if (isDynamic(_this11)) {
              var oldBoundState = _this11.$boundState;
              var newBoundState = {};
              findBindKeys(spandrelData, _this11).forEach(function (bindKey) {
                newBoundState[bindKey] = oldBoundState[bindKey] || {};
              });
              _this11.$boundState = newBoundState;
            }

            var prev = _this11.$spandrel;
            var curr = {
              spandrelData: spandrelData,
              addedClasses: addedClasses,
              addedStyles: addedStyles
            };
            _this11.$spandrel = curr;
            return {
              prev: prev,
              curr: curr
            };
          });
        });
      } //TODO: add lex support

      /**
       * @private
       * @param {module:bajaux/spandrel~SpandrelRenderResults} prev
       * @param {module:bajaux/spandrel~SpandrelRenderResults} curr
       * @param {module:bajaux/spandrel~WidgetState} state the state of the widget being diffed
       * @returns {Promise}
       */

    }, {
      key: "$applyDiffs",
      value: function $applyDiffs(prev, curr, state) {
        var _this12 = this;

        var dom = this.jq();
        var rootElement = dom[0];
        var mgr = this.$manager || spandrel().$getDefaultWidgetManager();
        var prevClasses;
        var prevStyles;
        var spandrelData = curr.spandrelData,
            addedClasses = curr.addedClasses,
            addedStyles = curr.addedStyles;
        var lifecycleDetailsLoggable = areLifecycleDetailsLoggable();
        var timingDetailsLoggable = areTimingDetailsLoggable();
        var domDetailsLoggable = areDomDetailsLoggable();

        if (timingDetailsLoggable) {
          logTimingDetails(this, 'starting $applyDiffs');
        }

        return Promise.resolve().then(function () {
          var differences;

          if (isDynamic(_this12)) {
            var oldBoundState = _this12.$boundState;
            var newBoundState = {};
            findBindKeys(spandrelData, _this12).forEach(function (bindKey) {
              newBoundState[bindKey] = oldBoundState[bindKey] || {};
            });
            _this12.$boundState = newBoundState;
          }

          if (prev) {
            differences = diffBuildContexts(prev.spandrelData, spandrelData);
            prevClasses = prev.addedClasses;
            prevStyles = prev.addedStyles;
          }

          if (!prev) {
            if (lifecycleDetailsLoggable) {
              logLifecycleDetails(_this12, 'not applying diffs on first render');
            }

            return buildChildWidgetsFromSpandrelData(_this12, spandrelData, dom, mgr);
          }

          if (lifecycleDetailsLoggable) {
            var prevData = prev.spandrelData;
            var currData = curr.spandrelData;

            if (domDetailsLoggable) {
              if (differences.length) {
                logDomDetails(_this12, 'diffs: [\n  {}\n]', differences.map(function (diff) {
                  return diffToDisplay(diff, prevData, currData);
                }).join('\n  '));
              } else if (timingDetailsLoggable) {
                logLifecycleDetails(_this12, 'had no diffs to display');
              }
            } else {
              var diffs = nonDomDiffs(differences);

              if (diffs.length) {
                logLifecycleDetails(_this12, 'diffs (excluding DOM): [\n  {}\n]', diffs.map(function (diff) {
                  return diffToDisplay(diff, prevData, currData);
                }).join('\n  '));
              } else if (timingDetailsLoggable) {
                logLifecycleDetails(_this12, 'had no diffs to apply');
              }
            }
          }

          return differences && differences.length && applyDiffs(_this12, spandrelData, prev.spandrelData, differences, state, mgr);
        }).then(function () {
          var classList = rootElement.classList,
              style = rootElement.style,
              children = rootElement.children,
              members = spandrelData.members; // Ensure spandrelKeys are updated correctly after diffing

          members.forEach(function (member, i) {
            var child = children[i];

            if (child) {
              child.spandrelKey = member.key;
            } else {
              logWarning('DOM element was removed from spandrel widget by an external actor');
            }
          });

          if (prevClasses && prevClasses.length) {
            classList.remove.apply(classList, difference(prevClasses, addedClasses));
          }

          if (prevStyles) {
            var removedStyles = difference(Object.keys(prevStyles), Object.keys(addedStyles));

            for (var i = 0, len = removedStyles.length; i < len; ++i) {
              style.removeProperty(removedStyles[i]);
            }
          }

          if (addedClasses.length) {
            classList.add.apply(classList, addedClasses);
          }

          Object.assign(style, addedStyles);
        });
      }
    }], [{
      key: "make",

      /**
       * Defines a new dynamic `spandrel` widget subclass.
       *
       * @param {object} params
       * @param {Function} [params.extends=module:bajaux/spandrel/DynamicSpandrelWidget] - the
       * superclass you wish to extend from
       * @param {function(*, module:bajaux/spandrel~WidgetState): module:bajaux/spandrel~SpandrelData} [params.spandrelFunction] - the
       * function to be called when loading a value into an instance of your widget, and that returns
       * spandrel data to be rendered
       * @param {module:bajaux/lifecycle/WidgetManager} [params.manager] the fallback manager for
       * instances of this subclass to use to construct child widgets, if no strategy defined
       * @param {string} [params.strategy] a known strategy ID for constructing child widgets
       *
       * @returns {Function} a Widget subclass constructor
       */
      value: function make(params) {
        var Super = params["extends"],
            spandrelFunction = params.spandrelFunction,
            manager = params.manager,
            strategy = params.strategy;
        /**
         * @private
         */

        var DerivedSpandrelWidget = /*#__PURE__*/function (_ref6) {
          _inherits(DerivedSpandrelWidget, _ref6);

          var _super2 = _createSuper(DerivedSpandrelWidget);

          /**
           * Widget constructor.
           * @param {object} params
           */
          function DerivedSpandrelWidget(params) {
            var _this13;

            _classCallCheck(this, DerivedSpandrelWidget);

            _this13 = _super2.apply(this, arguments);
            _this13[IS_DYNAMIC_SYMBOL] = true;
            _this13.$renderQueue = new SpandrelRenderQueue(_assertThisInitialized(_this13));
            trackRenders(_assertThisInitialized(_this13));

            _this13.$resolveManager = function () {
              return resolveManager(params, strategy, manager);
            };

            return _this13;
          }
          /**
           * Does the work of rendering the `spandrel` data as defined by the developer.
           * This data will be consumed in a `spandrel` render cycle to build out the widget tree into
           * the DOM.
           *
           * @private
           * @param {*} value
           * @param {module:bajaux/spandrel~WidgetState} state
           * @returns {Promise.<module:bajaux/spandrel~SpandrelData>}
           */


          _createClass(DerivedSpandrelWidget, [{
            key: "$generateSpandrelData",
            value: function $generateSpandrelData(value, state) {
              var _this14 = this;

              var widgetState = extend({}, state, {
                renderSuper: function renderSuper(func) {
                  if (typeof func === 'function') {
                    return Promise.resolve(func(state)).then(function (newState) {
                      return _get(_getPrototypeOf(DerivedSpandrelWidget.prototype), "$generateSpandrelData", _this14).call(_this14, value, newState);
                    });
                  } else {
                    return _get(_getPrototypeOf(DerivedSpandrelWidget.prototype), "$generateSpandrelData", _this14).call(_this14, value, state);
                  }
                }
              });
              return Promise.resolve(spandrelFunction.call(this, value, widgetState));
            }
          }]);

          return DerivedSpandrelWidget;
        }(Super || DynamicSpandrelWidget); //we need SpandrelWidget functionality even if we didn't inherit from it


        if (Super) {
          SpandrelWidget.mixin(Super);
        }

        DerivedSpandrelWidget[IS_DYNAMIC_SYMBOL] = true;
        return DerivedSpandrelWidget;
      }
    }]);

    return DynamicSpandrelWidget;
  }(SpandrelWidget);
  /**
   * @param {JQuery} dom
   * @param {module:bajaux/spandrel~Member} member
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   * @param {module:bajaux/Widget} root
   * @param {number} depth
   * @param {module:bajaux/Widget} owner
   * @param {boolean} wipeModified
   * @returns {Promise}
   */


  function rebuild(dom, member, manager, root, depth, owner, wipeModified) {
    //TODO: support a RebuildStrategy
    var oldWidget = Widget["in"](dom);

    if (oldWidget && !(oldWidget instanceof SpandrelWidget) && oldWidget.isModified()) {
      // do not wipe changes if state key is not dirty (i.e. dev did not say "i really mean it")
      if (!wipeModified && !isLax(oldWidget) && !isStateKeyDirty(owner, oldWidget)) {
        return Promise.resolve();
      }
    }

    return Promise.resolve(oldWidget && oldWidget.destroy()).then(function () {
      var newDom = cloneNode(member.config.dom);
      dom.replaceWith(newDom);
      return doFeBuild(member, newDom, manager, root, owner, depth);
    });
  }
  /**
   * During a rerender, the current `spandrel` data will be diffed against the `spandrel` data from
   * the previous render. This function does the work of taking those differences and actually
   * applying them to the DOM.
   *
   * @param {module:bajaux/Widget} owner
   * @param {module:bajaux/spandrel~BuildContext} curr current spandrel build context
   * @param {module:bajaux/spandrel~BuildContext} prev previous spandrel build context
   * @param {Array.<object>} differences deep-diff differences between the two
   * @param {module:bajaux/spandrel~WidgetState} state the state of the widget being diffed
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   * @returns {Promise} to be resolved when differences are applied
   */


  function applyDiffs(owner, curr, prev, differences, state, manager) {
    var rootElement = state.rootElement;
    var lifecycleLoggable = isLifecycleLoggable();
    var domDetailsLoggable = areDomDetailsLoggable();

    var findNodePath = function findNodePath(path) {
      var _getSpandrelKeysAndCu = getSpandrelKeysAndCurrentMemberFromPath(path, prev, curr),
          keys = _getSpandrelKeysAndCu.keys,
          member = _getSpandrelKeysAndCu.member;

      var node = keys.reduce(function (node, key) {
        return kidsMatching(node, key)[0];
      }, rootElement);
      return {
        node: node,
        keys: keys,
        member: member
      };
    };

    var root = owner[ROOT_SYMBOL];
    var depth = owner[DEPTH_SYMBOL] + 1;
    var wipeModified = state[WIPE_MODIFIED_SYMBOL];
    var myRenderTicks = state[RENDER_TICKS_SYMBOL];
    var diffQueue = new DiffQueue(owner, myRenderTicks);
    /*
    TODO: do we need to optimize this?
    if we got a diff for the top-level widget and the event handler on the
    kid widget at the same time, spandrel was destroying the parent (which
    also destroys the kid) and re-arming the event handler on the (destroyed)
    kid at the same time.
    for now, we'll run the promises in serial since deep-diff should return the
    diffs in top-to-bottom order. but maybe we could eke out some more
    performance if we ran promises in distinct branches (never same branch) in
    parallel.
     */

    var queueEditFromDiff = function queueEditFromDiff(diff) {
      var kind = diff.kind,
          index = diff.index,
          item = diff.item,
          path = diff.path; //additions/deletions only count if they're to lists of child widgets.
      //e.g. i might have added a new member to an array as in an OrdList.

      if (path && path.length && last(path) !== 'members') {
        kind = 'E';
      }

      switch (kind) {
        case 'E':
          {
            var _findNodePath = findNodePath(path),
                node = _findNodePath.node,
                keys = _findNodePath.keys,
                member = _findNodePath.member;

            var editPath = keys.join('/');
            var changedProp = last(path);

            if (changedProp === 'on') {
              return diffQueue.queuePostBajaux(editPath, function () {
                var widget = owner.queryWidget(keys.join('/'));

                if (!widget) {
                  // it was deleted
                  return;
                } // TODO: optimize this
                // the differing value could be just one member of `on` but we have
                // to wipe and re-arm all events at once


                var on = getValueFromBuildContext(curr, path, 'on');
                return armHandlers(widget, widget.jq(), on);
              });
            } else if (changedProp === 'dom') {
              //special case for dom: don't do a full wipe and rebuild if dom is
              //reusable
              var oldElement = toEl(diff.lhs);
              var newElement = toEl(diff.rhs);
              var realLiveElement = node;

              if (realLiveElement && !requiresRebuild(oldElement, newElement)) {
                if (domDetailsLoggable && !oldElement.isEqualNode(newElement)) {
                  logDomDetails(owner, 'updating dom in-place at path {}', editPath);
                }

                return diffQueue.queue(editPath, function () {
                  realLiveElement[STATE_BINDING_SYMBOL] = member.stateBinding; // NCCB-53679: previously, this diffed the dom as returned by
                  // the current render against the real live element.
                  //
                  // instead:
                  // diff the dom as returned by the previous render, against the
                  // dom as returned by the current render. apply the diff to the
                  // real live element. this ensures that if a child, non-spandrel
                  // widget adds its own classes in doInitialize/doLoad (such as a
                  // BaseEditor), those won't get wiped out by the diff process.

                  return updateElement(oldElement, newElement, realLiveElement);
                });
              } else if (lifecycleLoggable) {
                logLifecycle(owner, 'rebuilding dom at path {} because it could not be diffed', editPath);
              }
            } else if (changedProp === 'validate') {
              var validate = diff.rhs;
              var existing = Widget["in"](node);
              return setValidationOfDescendent(owner, existing, validate);
            } else if (changedProp === 'value') {
              var _existing = Widget["in"](node);

              var config = getValueFromBuildContext(curr, path, 'config');
              return Promise.resolve(manager.resolveConstructor(config)).then(function (Ctor) {
                // if the state key for this child widget is dirty because someone
                // called state({}), that state key get consumed on this rerender
                // whether we reload the widget or not.
                var stateKeyDirty = _existing && isStateKeyDirty(owner, _existing);

                if (Ctor && _existing instanceof Ctor) {
                  var hisTicks = _existing[RENDER_TICKS_SYMBOL];
                  var heWasRenderedAfterMe = compareTicks(myRenderTicks, hisTicks) < 0;

                  if (heWasRenderedAfterMe && !wipeModified) {
                    if (lifecycleLoggable) {
                      logLifecycle(owner, 'declining to load new value "{}" into ' + 'widget {} at path {} because another value was ' + 'loaded into it during rerender. my ticks {}, more recent load ticks {}', member.config.value, widgetName(_existing), editPath, myRenderTicks, hisTicks);
                    }

                    return;
                  }

                  if (_existing.isModified() && !wipeModified) {
                    // never wipe changes while a user is typing
                    if (hasFocus(_existing)) {
                      if (lifecycleLoggable) {
                        logLifecycle(owner, 'declining to load new value "{}" into ' + 'widget {} at path {} because it has focus', member.config.value, widgetName(_existing), editPath);
                      }

                      return;
                    } // do not wipe changes if state key is not dirty (i.e. dev did not say "i really mean it")


                    if (!isLax(_existing) && !stateKeyDirty) {
                      if (lifecycleLoggable) {
                        logLifecycle(owner, 'declining to load new value "{}" into ' + 'widget {} at path {} because it is modified and state key is not dirty', member.config.value, widgetName(_existing), editPath);
                      }

                      return;
                    }
                  }

                  if (lifecycleLoggable) {
                    logLifecycle(owner, 'loading new value "{}" into widget {} at path {} ' + 'and enforcing ticks {}, wipeModified {}', member.config.value, widgetName(_existing), editPath, myRenderTicks, !!wipeModified);
                  }

                  return diffQueue.queueForLoad(editPath, function () {
                    var _Object$assign;

                    var loadParams = member.config.loadParams || {};
                    loadParams = Object.assign((_Object$assign = {}, _defineProperty(_Object$assign, WIPE_MODIFIED_SYMBOL, !!wipeModified), _defineProperty(_Object$assign, RENDER_TICKS_SYMBOL, myRenderTicks), _Object$assign), loadParams);
                    return manager.load(_existing, Object.assign({
                      loadParams: loadParams
                    }, member.config));
                  });
                } else {
                  if (lifecycleLoggable) {
                    logLifecycle(owner, 'rebuilding widget at {} because new value to load ' + '"{}" is not compatible with existing widget {}', editPath, member.config.value, widgetName(_existing));
                  }

                  return diffQueue.queueForRebuild(editPath, function () {
                    return rebuild($(node), member, manager, root, depth, owner, wipeModified);
                  });
                }
              });
            } else if (changedProp === 'enabled') {
              var _existing2 = Widget["in"](node);

              var enabled = diff.rhs;

              if (lifecycleLoggable) {
                logLifecycle(owner, 'setting widget {}\'s enabled to {} at path {}', widgetName(_existing2), enabled, editPath);
              }

              return diffQueue.queueForRerender(editPath, function () {
                return setEnabledSilently(_existing2, enabled);
              }, function () {
                return rerenderFromTicks(_existing2, myRenderTicks);
              });
            } else if (changedProp === 'readonly') {
              var _existing3 = Widget["in"](node);

              var readonly = diff.rhs;

              if (lifecycleLoggable) {
                logLifecycle(owner, 'setting widget {}\'s readonly to {} at path {}', widgetName(_existing3), readonly, editPath);
              }

              return diffQueue.queueForRerender(editPath, function () {
                return setReadonlySilently(_existing3, readonly);
              }, function () {
                return rerenderFromTicks(_existing3, myRenderTicks);
              });
            } else if (changedProp === 'properties') {
              var _existing4 = Widget["in"](node);

              var properties = diff.rhs;

              if (canUpdateProperties(_existing4)) {
                if (lifecycleLoggable) {
                  logLifecycle(owner, 'updating widget {}\'s properties at path {}', widgetName(_existing4), editPath);
                }

                return diffQueue.queueForRerender(editPath, function () {
                  return setPropertiesSilently(_existing4, properties);
                }, function () {
                  return rerenderFromTicks(_existing4, myRenderTicks);
                });
              }
            }

            if (isUpdatableDiff(path)) {
              var _existing5 = Widget["in"]($(node));

              if (_existing5 instanceof SpandrelWidget && typeof _existing5.rerender === 'function') {
                if (lifecycleLoggable) {
                  logLifecycle(owner, 'updating "{}" widget property at path {}', changedProp, editPath);
                }

                return diffQueue.queueForRerender(editPath, function () {
                  return update(_existing5, member);
                }, function () {
                  return rerenderFromTicks(_existing5, myRenderTicks);
                });
              } else if (lifecycleLoggable) {
                logLifecycle(owner, 'could not update "{}" widget property on {} at path {} ' + 'because it was not a dynamic spandrel widget', changedProp, widgetName(_existing5), editPath);
              }
            }

            if (lifecycleLoggable) {
              logLifecycle(owner, 'falling back to a full rebuild at path {} because ' + 'widget property "{}" could not be updated in-place', editPath, changedProp);
            } // since we're doing a rebuild, any kid widgets will also get rebuilt. this means that
            // any newly added kids in the spandrel data need to become no-ops to avoid having them
            // double-up.


            var kids = member.config.kids;

            if (kids) {
              kids.members.forEach(function (_ref7) {
                var key = _ref7.key;
                return diffQueue.nullify(editPath + '/' + key);
              });
            }

            return diffQueue.queueForRebuild(editPath, function () {
              return rebuild($(node), member, manager, root, depth, owner, wipeModified);
            });
          }

        case 'A':
          switch (item.kind) {
            case 'N':
              {
                var _findNodePath2 = findNodePath(path),
                    _keys = _findNodePath2.keys,
                    _node = _findNodePath2.node;
                /** @type module:bajaux/spandrel~Member */


                var addedMember = item.rhs;
                var addedKey = addedMember.key;

                var _editPath = _keys.concat(addedKey).join('/');

                if (lifecycleLoggable) {
                  logLifecycle(owner, 'creating new widget at {}', _editPath);
                }

                return diffQueue.queueAdd(_editPath, function () {
                  return doFeBuild(addedMember, cloneNode(addedMember.config.dom).appendTo(_node), manager, root, owner, depth);
                });
              }

            case 'D':
              {
                var _findNodePath3 = findNodePath((path || []).concat(index)),
                    _keys2 = _findNodePath3.keys,
                    _node2 = _findNodePath3.node;

                var widget = _node2 && Widget["in"](_node2);

                var _editPath2 = _keys2.join('/');

                if (lifecycleLoggable) {
                  if (widget) {
                    logLifecycle(owner, 'destroying {} at path {}', widgetName(widget), _editPath2);
                  } else {
                    logLifecycle(owner, 'removing DOM element at path {}', _editPath2);
                  }
                }

                setValidationOfDescendent(owner, widget, false);
                return diffQueue.queueDelete(_editPath2, function () {
                  return Promise.resolve(widget && widget.destroy()).then(function () {
                    return $(_node2).remove();
                  });
                });
              }
          }

      }
    };

    return Promise.all(differences.map(queueEditFromDiff)).then(function () {
      return diffQueue.doDiffs();
    });
  }

  function isLax(widget) {
    return widget.jq()[0][LAX_SYMBOL];
  }
  /**
   * @param {module:bajaux/Widget} owner
   * @param {module:bajaux/Widget} widget
   * @return {boolean}
   */


  function isStateKeyDirty(owner, widget) {
    var stateBinding = widget.jq()[0][STATE_BINDING_SYMBOL];

    if (stateBinding && owner.$isStateKeyDirty) {
      return stateBinding.target === owner && owner.$isStateKeyDirty(stateBinding.key, true);
    }
  }

  function isUpdatableDiff(path) {
    var lastInPath = last(path);
    return lastInPath === 'properties' || lastInPath === 'writable';
  }
  /**
   * @param {module:bajaux/spandrel/SpandrelWidget} widget
   * @param {module:bajaux/spandrel~Member} member
   * @returns {Promise}
   */


  function update(widget, member) {
    //TODO: support an UpdateStrategy
    return widget.applyParams(member.config).then(function () {
      return widget.rerender();
    });
  }

  function attr(el, name, attr) {
    if (attr) {
      el.setAttribute(name, attr);
    } else {
      el.removeAttribute(name);
    }
  }

  function canUpdateProperties(widget) {
    return widget && widget.doChanged !== Widget.prototype.doChanged;
  }
  /**
   * @param {string[]} path a property path through a nested spandrel config
   * @returns {string[]} the widget keys extracted from the path
   */


  function extractKeys(path) {
    // in the spandrel config, the path to the diff if nested is going
    // to follow the pattern:
    // members, key, config, kids, members, key, config, kids, members, key...
    return path.filter(function (p, i) {
      return path[i - 1] === 'members';
    });
  }
  /**
   * @param {object} params
   * @returns {module:bajaux/lifecycle/WidgetManager} the manager passed either
   * as `params.data.manager` or `params.params.data.manager`
   */


  function extractManager() {
    var params = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};
    //TODO: does Widget itself need an API for this?
    var data = params.data;
    var manager = data && data.manager;

    if (!manager) {
      params = params.params;
      data = params && params.data;
      manager = data && data.manager;
    }

    return manager;
  }

  function findBindKeys(_ref8, owner) {
    var members = _ref8.members;
    var bindKeys = [];
    members.forEach(function (_ref9) {
      var stateBinding = _ref9.stateBinding,
          config = _ref9.config;
      var kids = config.kids;

      if (stateBinding) {
        if (stateBinding.target === owner) {
          bindKeys.push(stateBinding.key);
        }
      }

      if (kids) {
        bindKeys.push.apply(bindKeys, _toConsumableArray(findBindKeys(kids, owner)));
      }
    });
    return bindKeys;
  }

  function getValueFromBuildContext(ctx, path, name) {
    return resolveObjectPath(ctx, path.slice(0, lastIndexOf(path, name) + 1));
  }
  /**
   * Does a deep-get of a property path from an object. Works like the array
   * form of `_.property`.
   * @param {object} obj
   * @param {string[]} path
   * @returns {*}
   */


  function resolveObjectPath(obj, path) {
    return path.reduce(function (curr, prop) {
      return curr[prop] || {};
    }, obj);
  }
  /**
   * @param {module:bajaux/spandrel/SpandrelWidget} widget
   * @param {boolean} wipeModifications
   * @param {number} renderTicks
   * @returns {module:bajaux/spandrel~WidgetState} the widget's state to pass to
   * a render call, but marked as being willing to wipe user modifications
   * (should only be called when `load()` is directly called, not when child
   * widgets are reloaded during a rerender).
   */


  function loadingState(widget, wipeModifications, renderTicks) {
    var state = widget.state();
    state[WIPE_MODIFIED_SYMBOL] = !!wipeModifications;
    state[RENDER_TICKS_SYMBOL] = renderTicks;
    return state;
  }

  function setClassList(el, classList) {
    Object.defineProperty(el, 'classList', {
      configurable: true,
      enumerable: true,
      writable: true,
      value: classList
    });
  }

  function resolveManager(params, strategy, defaultManager) {
    var knownManager = strategy && spandrel().$KNOWN_MANAGERS[strategy];

    if (knownManager) {
      return Promise.resolve(knownManager());
    } else {
      return extractManager(params) || defaultManager;
    }
  }

  function nonDomDiffs(differences) {
    return differences.filter(function (diff) {
      return !(diff.rhs instanceof Element);
    });
  }

  function formatArg(arg) {
    if (typeof arg === 'function') {
      return 'function ' + arg.name;
    }

    return Log.formatArg(arg);
  }

  function getSpandrelKeysAndCurrentMemberFromPath() {
    var path = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];
    var prev = arguments.length > 1 ? arguments[1] : undefined;
    var curr = arguments.length > 2 ? arguments[2] : undefined;
    var currMembers = curr && curr.members;
    var prevMembers = prev && prev.members;
    var currMember;
    var prevMember;
    var keys = extractKeys(path || []).map(function (key) {
      currMember = currMembers[key] || {};
      prevMember = prevMembers[key] || {};
      currMembers = resolveObjectPath(currMember, PATH_TO_KID_CONTEXTS);
      prevMembers = resolveObjectPath(prevMember, PATH_TO_KID_CONTEXTS);
      return prevMember.key || key; //if the key changed, the old key is still in the dom
    });
    return {
      keys: keys,
      member: currMember
    };
  }

  function diffToDisplay(diff, prev, curr) {
    var item = diff.item,
        kind = diff.kind,
        path = diff.path,
        lhs = diff.lhs,
        rhs = diff.rhs;
    var displayPath = getSpandrelKeysAndCurrentMemberFromPath(path, prev, curr).keys.join() || '(self)';

    switch (kind) {
      case 'E':
        var changed = last(path);

        if (changed === 'on') {
          return "{Edit event handlers at ".concat(displayPath, "}");
        }

        return "{Edit ".concat(changed, " at ").concat(displayPath, " from ").concat(formatArg(lhs, changed), " to ").concat(formatArg(rhs, changed), "}");

      case 'A':
        switch (item.kind) {
          case 'N':
            return "{Add ".concat(formatArg(rhs), " at ").concat(displayPath, "}");

          case 'D':
            return "{Delete ".concat(displayPath, "}");
        }

    }
  }

  function rerenderFromTicks(widget, ticks) {
    return Promise.resolve(widget.$rerenderFromTicks && widget.$rerenderFromTicks(ticks));
  }

  return DynamicSpandrelWidget;
});
