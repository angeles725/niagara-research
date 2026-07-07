function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _get(target, property, receiver) { if (typeof Reflect !== "undefined" && Reflect.get) { _get = Reflect.get; } else { _get = function _get(target, property, receiver) { var base = _superPropBase(target, property); if (!base) return; var desc = Object.getOwnPropertyDescriptor(base, property); if (desc.get) { return desc.get.call(receiver); } return desc.value; }; } return _get(target, property, receiver || target); }

function _superPropBase(object, property) { while (!Object.prototype.hasOwnProperty.call(object, property)) { object = _getPrototypeOf(object); if (object === null) break; } return object; }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _createSuper(Derived) { var hasNativeReflectConstruct = _isNativeReflectConstruct(); return function _createSuperInternal() { var Super = _getPrototypeOf(Derived), result; if (hasNativeReflectConstruct) { var NewTarget = _getPrototypeOf(this).constructor; result = Reflect.construct(Super, arguments, NewTarget); } else { result = Super.apply(this, arguments); } return _possibleConstructorReturn(this, result); }; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _wrapNativeSuper(Class) { var _cache = typeof Map === "function" ? new Map() : undefined; _wrapNativeSuper = function _wrapNativeSuper(Class) { if (Class === null || !_isNativeFunction(Class)) return Class; if (typeof Class !== "function") { throw new TypeError("Super expression must either be null or a function"); } if (typeof _cache !== "undefined") { if (_cache.has(Class)) return _cache.get(Class); _cache.set(Class, Wrapper); } function Wrapper() { return _construct(Class, arguments, _getPrototypeOf(this).constructor); } Wrapper.prototype = Object.create(Class.prototype, { constructor: { value: Wrapper, enumerable: false, writable: true, configurable: true } }); return _setPrototypeOf(Wrapper, Class); }; return _wrapNativeSuper(Class); }

function _construct(Parent, args, Class) { if (_isNativeReflectConstruct()) { _construct = Reflect.construct; } else { _construct = function _construct(Parent, args, Class) { var a = [null]; a.push.apply(a, args); var Constructor = Function.bind.apply(Parent, a); var instance = new Constructor(); if (Class) _setPrototypeOf(instance, Class.prototype); return instance; }; } return _construct.apply(null, arguments); }

function _isNativeReflectConstruct() { if (typeof Reflect === "undefined" || !Reflect.construct) return false; if (Reflect.construct.sham) return false; if (typeof Proxy === "function") return true; try { Date.prototype.toString.call(Reflect.construct(Date, [], function () {})); return true; } catch (e) { return false; } }

function _isNativeFunction(fn) { return Function.toString.call(fn).indexOf("[native code]") !== -1; }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/**
 * API Status: **Private**
 * @module bajaux/spandrel/SpandrelWidget
 */
define(['log!bajaux.spandrel.SpandrelWidget', 'bajaux/Properties', 'bajaux/Widget', 'bajaux/events', 'bajaux/spandrel/buildConfig', 'bajaux/spandrel/RequestLayoutMixin', 'bajaux/spandrel/logging', 'bajaux/spandrel/symbols', 'bajaux/spandrel/util', 'bajaux/spandrel/workflow', 'Promise', 'underscore', 'nmodule/js/rc/asyncUtils/asyncUtils', 'nmodule/js/rc/switchboard/switchboard'], function (log, Properties, Widget, events, buildConfig, RequestLayoutMixin, logging, symbols, util, workflow, Promise, _, asyncUtils, switchboard) {
  'use strict';

  var logSevere = log.severe.bind(log);
  var areTimingDetailsLoggable = logging.areTimingDetailsLoggable,
      logTimingDetails = logging.logTimingDetails;
  var MODIFY_EVENT = events.MODIFY_EVENT;
  var compact = _.compact,
      filter = _.filter,
      flatten = _.flatten,
      map = _.map,
      negate = _.negate,
      omit = _.omit;
  var INSTANCE_COUNTER_SYMBOL = symbols.INSTANCE_COUNTER_SYMBOL,
      IS_ELEMENT_SYMBOL = symbols.IS_ELEMENT_SYMBOL,
      SPANDREL_HANDLER_PRIORITY = symbols.SPANDREL_HANDLER_PRIORITY,
      OWNER_SYMBOL = symbols.OWNER_SYMBOL,
      RENDER_TICKS_SYMBOL = symbols.RENDER_TICKS_SYMBOL,
      STATE_BINDING_SYMBOL = symbols.STATE_BINDING_SYMBOL,
      TO_VALIDATE_SYMBOL = symbols.TO_VALIDATE_SYMBOL,
      WIDGET_SYMBOL = symbols.WIDGET_SYMBOL;
  var isDynamic = util.isDynamic,
      isDummy = util.isDummy,
      kidsMatching = util.kidsMatching,
      pathMatches = util.pathMatches;
  var $deferred = asyncUtils.$deferred,
      waitInterval = asyncUtils.waitInterval;
  var buildChildWidgetsFromSpandrelData = workflow.buildChildWidgetsFromSpandrelData,
      $moveSpandrelModifyHandlersToFront = workflow.$moveSpandrelModifyHandlersToFront,
      nextTicks = workflow.nextTicks;
  var isReal = negate(isDummy);
  var BIND_ERRORS = Symbol('bindErrors');
  var NON_INHERITABLE_PROPERTIES = ['rootCssClass', 'uxFieldEditor'];
  var STATE_UPDATE_DELAY = 0;

  var nextInstanceCounter = function () {
    var id = 1; // allow for naive falsy checks

    return function () {
      return id++;
    };
  }();

  var BindError = /*#__PURE__*/function (_Error) {
    _inherits(BindError, _Error);

    var _super = _createSuper(BindError);

    function BindError(cause, key) {
      var _this;

      _classCallCheck(this, BindError);

      _this = _super.call(this, 'BindError[' + key + ']: ' + cause);
      _this.$cause = cause;
      _this.stack = cause.stack;
      return _this;
    }

    return BindError;
  }( /*#__PURE__*/_wrapNativeSuper(Error));
  /**
   * @class
   * @alias module:bajaux/spandrel/SpandrelWidget
   * @extends module:bajaux/Widget
   */


  return /*#__PURE__*/function (_Widget) {
    _inherits(SpandrelWidget, _Widget);

    var _super2 = _createSuper(SpandrelWidget);

    _createClass(SpandrelWidget, null, [{
      key: "make",

      /**
       * Defines a new dynamic `spandrel` widget subclass.
       *
       * @param {object} params
       * @param {module:bajaux/spandrel~SpandrelData} [params.spandrelArg] - the spandrel data
       * defining this widget's structure
       * @param {module:bajaux/lifecycle/WidgetManager} [params.manager] the fallback manager for
       * instances of this subclass to use to construct child widgets, if no strategy defined
       * @param {boolean} [params.isElement] true if this widget represents a raw DOM element
       *
       * @returns {Function} a Widget subclass constructor
       */
      value: function make(params) {
        var isElement = params.isElement,
            manager = params.manager,
            spandrelArg = params.spandrelArg;
        return /*#__PURE__*/function (_SpandrelWidget) {
          _inherits(StaticSpandrelWidget, _SpandrelWidget);

          var _super3 = _createSuper(StaticSpandrelWidget);

          function StaticSpandrelWidget() {
            var _this3;

            _classCallCheck(this, StaticSpandrelWidget);

            _this3 = _super3.apply(this, arguments);

            if (isElement) {
              _this3[IS_ELEMENT_SYMBOL] = true;
            }

            return _this3;
          }

          _createClass(StaticSpandrelWidget, [{
            key: "doInitialize",
            value: function doInitialize(dom) {
              var _this4 = this,
                  _arguments = arguments;

              dom[0][WIDGET_SYMBOL] = this;
              return buildConfig(spandrelArg, this.state(), this).then(function (data) {
                return buildChildWidgetsFromSpandrelData(_this4, data, dom, manager);
              }).then(function () {
                return _get(_getPrototypeOf(StaticSpandrelWidget.prototype), "doInitialize", _this4).apply(_this4, _arguments);
              });
            }
            /**
             * No-op in a static widget.
             * @returns {Promise}
             */

          }, {
            key: "rerender",
            value: function rerender() {
              return Promise.resolve();
            }
          }]);

          return StaticSpandrelWidget;
        }(SpandrelWidget);
      }
    }]);

    function SpandrelWidget() {
      var _this2;

      _classCallCheck(this, SpandrelWidget);

      _this2 = _super2.apply(this, arguments);
      switchboard(_assertThisInitialized(_this2), {
        $debounceStateQueue: {
          allow: 'oneAtATime',
          onRepeat: 'returnLast'
        },
        $doFlushStateQueue: {
          allow: 'oneAtATime',
          onRepeat: 'returnLast'
        }
      });
      /** @type {module:bajaux/spandrel/SpandrelWidget~StateUpdateRegistry} */

      _this2.$boundState = {};
      RequestLayoutMixin(_assertThisInitialized(_this2));

      _this2.validators().add(function () {
        var pathsToValidate = _this2[TO_VALIDATE_SYMBOL];

        if (pathsToValidate) {
          return Promise.all(pathsToValidate.map(function (path) {
            return _this2.queryWidget(path).validate();
          }));
        }
      });

      _this2[INSTANCE_COUNTER_SYMBOL] = nextInstanceCounter();
      return _this2;
    }

    _createClass(SpandrelWidget, [{
      key: "doInitialize",
      value: function doInitialize(dom) {
        if (isDynamic(this)) {
          dom.on(MODIFY_EVENT, makeSpandrelHandler(this, statePropagator));
        }

        dom.on(MODIFY_EVENT, makeSpandrelHandler(this, modifyBubbler));
        $moveSpandrelModifyHandlersToFront(dom[0]);
        return _get(_getPrototypeOf(SpandrelWidget.prototype), "doInitialize", this).apply(this, arguments);
      }
    }, {
      key: "doDestroy",
      value: function doDestroy() {
        return Promise.all(map(this.jq().children(), function (e) {
          var widget = Widget["in"](e);

          if (widget) {
            return widget.destroy();
          }
        }));
      }
    }, {
      key: "setModified",
      value: function setModified() {
        // Widget#setModified behavior is to short circuit the modify logic if the
        // widget is in the process of loading. a spandrel widget should always
        // be modifiable, even during a rerender.
        var isLoading = this.isLoading;

        try {
          this.isLoading = never;
          return _get(_getPrototypeOf(SpandrelWidget.prototype), "setModified", this).apply(this, arguments);
        } finally {
          this.isLoading = isLoading;
        }
      }
    }, {
      key: "read",
      value: function read() {
        return this.$read.apply(this, arguments);
      }
      /**
       * This method exists just to inject custom read behavior for spandrel widgets.
       * @private
       * @returns {Promise.<*>}
       */

    }, {
      key: "$read",
      value: function $read() {
        var _arguments2 = arguments,
            _this5 = this;

        if (!this.isInitialized()) {
          return Promise.reject(new Error('Not initialized!'));
        } // TODO: Widget does not pass args from read to doRead.
        // c&p'ed for now


        return this.$flushStateQueue().then(function () {
          return _this5.doRead.apply(_this5, _toConsumableArray(_arguments2));
        });
      }
      /**
       * @param {string} queryString a string, /-separated, to match spandrel keys
       * through the entire nested structure
       * @returns {module:bajaux/Widget|undefined} the first matching widget
       */

    }, {
      key: "queryWidget",
      value: function queryWidget(queryString) {
        return this.queryWidgets(queryString)[0];
      }
      /**
       * @param {string|Array.<string>} queryString a string, /-separated, to match spandrel keys
       * through the entire nested structure (or the same in the form of an array of keys)
       * @returns {Array.<module:bajaux/Widget>} matching widgets
       */

    }, {
      key: "queryWidgets",
      value: function queryWidgets(queryString) {
        var _this6 = this;

        if (Array.isArray(queryString)) {
          return _queryWidgets(this, queryString);
        }

        queryString = String(queryString);

        if (!queryString) {
          return [
          /** @type module:bajaux/Widget */
          this];
        }

        if (typeof queryString === 'string' && queryString.match(/\*\*/)) {
          //slow globbing
          return filter(this.jq().find('.bajaux-initialized'), function (el) {
            return el.spandrelKey;
          }).map(Widget["in"]).filter(function (w) {
            return isReal(w) && pathMatches(util.getPathToKid(_this6, w).join('/'), queryString);
          });
        }

        return _queryWidgets(this, queryString.split('/'));
      }
      /**
       * Gets or sets the spandrel widget's current state.
       *
       * @param {module:bajaux/spandrel~WidgetState} [newState] if setting the
       * state, pass it as an argument. If getting the state, pass no arguments.
       * @returns {module:bajaux/spandrel~WidgetState|Promise} either the widget
       * state directly (if getting), or a Promise to be resolved after all state
       * updates have applied to the widget (if setting)
       */

    }, {
      key: "state",
      value: function state(newState) {
        var _this7 = this;

        switch (arguments.length) {
          case 0:
            var state = this.$stateIgnoringBindErrors();
            var bindErrors = this[BIND_ERRORS];

            if (bindErrors) {
              Object.keys(bindErrors).forEach(function (key) {
                Object.defineProperty(state, key, {
                  get: function get() {
                    throw bindErrors[key];
                  }
                });
              });
            }

            return state;

          case 1:
            if (!newState || _typeof(newState) !== 'object') {
              throw new Error('state map required');
            }

            return this.$applyState(newState, true).then(function () {
              return _this7.rerender();
            })["catch"](logSevere);
        }
      }
      /**
       * Gets the widget's current state, ignoring any current bind errors. This
       * is intended to be called internally by spandrel, *most* of the time.
       *
       * Bind errors *should* be thrown:
       * - in fromState() during a read() call, to ensure the spandrel widget will
       *   not read out an invalid value
       * - when a developer calls this.state(), to ensure their code always works
       *   with valid widget state
       *
       * Bind errors *should not* be thrown during the render process: when
       * rendering, widgets with bind errors will render using their "last known
       * good" value. This isn't perfect, but it allows for the user to enter an
       * invalid value and then make a further change to abandon it, such as
       * typing "asdf" for a height value in a LayoutEditor and then just checking
       * Fill to abandon it. Otherwise, the user would be forced to type a valid
       * number to trigger another rerender.
       *
       * @private
       * @returns {module:bajaux/spandrel~WidgetState}
       */

    }, {
      key: "$stateIgnoringBindErrors",
      value: function $stateIgnoringBindErrors() {
        var destroyed = this.isDestroyed(),
            props = destroyed ? {} : omit(this.properties().toObject(), NON_INHERITABLE_PROPERTIES),
            enabled = this.isEnabled(),
            readonly = this.isReadonly(),
            jq = this.jq();
        return _.extend({}, this.$state, {
          commands: this.getCommandGroup(),
          enabled: enabled,
          formFactor: destroyed ? null : this.getFormFactor(),
          modified: this.isModified(),
          properties: props,
          props: props,
          readonly: readonly,
          rootElement: jq && jq[0],
          self: this,
          writable: enabled && !readonly
        });
      }
      /**
       * @private
       * @param {module:bajaux/spandrel~WidgetState} newState
       * @param {boolean} [updateBindings]
       * @returns {Promise}
       */

    }, {
      key: "$applyState",
      value: function $applyState(newState, updateBindings) {
        var _this8 = this;

        var state = this.$state || (this.$state = {});
        var boundState = this.$boundState;
        var stateUpdates = [];
        var bindErrors = this[BIND_ERRORS];
        Object.keys(newState).forEach(function (key) {
          var val = newState[key];

          if (key === 'enabled') {
            stateUpdates.push(_this8.setEnabled(val));
          } else if (key === 'readonly') {
            stateUpdates.push(_this8.setReadonly(val));
          } else if (key === 'properties' || key === 'props') {
            stateUpdates.push(_this8.properties().addAll(new Properties(val)));
          } else if (key === 'writable' || key === 'commands' || key === 'self' || key === 'modified') {// ignore
          } else {
            state[key] = val;

            if (bindErrors) {
              delete bindErrors[key];
            }

            if (updateBindings && boundState.hasOwnProperty(key)) {
              stateUpdates.push(_this8.$queueStateUpdate(key, function () {
                return val;
              }, true));
            }
          }
        });
        return Promise.all(stateUpdates);
      }
      /**
       * @private
       * @param {string} boundStateKey
       * @param {function} get a function returning/resolving the state value
       * for this key
       * @param {boolean} [dirty] true if this update dirties the state
       * @returns {Promise} to be resolved when the state update has been applied
       * to the widget's state
       */

    }, {
      key: "$queueStateUpdate",
      value: function $queueStateUpdate(boundStateKey, get, dirty) {
        var stateQueue = this.$boundState || (this.$boundState = {});
        stateQueue[boundStateKey] = {
          get: get,
          dirty: dirty
        };
        return this.$flushStateQueue();
      }
      /**
       * Applies a short debounce delay (so multiple MODIFY_EVENTs don't result in
       * duplicate work) and then applies state updates.
       *
       * @private
       * @returns {Promise} to be resolved when debounce delay has elapsed and
       * current queued set of state updates have been applied
       */

    }, {
      key: "$flushStateQueue",
      value: function $flushStateQueue() {
        var _this9 = this;

        return this.$debounceStateQueue().then(function () {
          return _this9.$doFlushStateQueue();
        });
      }
      /**
       * @private
       * @returns {Promise} to be resolved after a short debounce delay
       */

    }, {
      key: "$debounceStateQueue",
      value: function $debounceStateQueue() {
        return waitInterval(STATE_UPDATE_DELAY);
      }
      /**
       * @private
       * @returns {Promise} to be resolved when the work of flushing the state
       * queue is complete
       */

    }, {
      key: "$doFlushStateQueue",
      value: function $doFlushStateQueue() {
        var _this10 = this;

        var stateQueue = this.$boundState;
        var stateUpdates = [];
        var state = {};
        Object.keys(stateQueue).forEach(function (boundStateKey) {
          if (_this10.$isStateKeyDirty(boundStateKey)) {
            return;
          }

          var getState = stateQueue[boundStateKey].get;
          stateQueue[boundStateKey] = {};

          if (getState) {
            stateUpdates.push(Promise["try"](getState).then(function (value) {
              var bindErrors = _this10[BIND_ERRORS];

              if (bindErrors) {
                delete bindErrors[boundStateKey];
              }

              state[boundStateKey] = value;
            })["catch"](function (err) {
              var bindErrors = _this10[BIND_ERRORS] || (_this10[BIND_ERRORS] = {});
              bindErrors[boundStateKey] = new BindError(err, boundStateKey);
            }));
          }
        });

        if (!stateUpdates.length) {
          return Promise.resolve();
        }

        return Promise.all(stateUpdates).then(function () {
          return _this10.$applyState(state);
        });
      }
      /**
       * @private
       * @returns {boolean} true if there are any state updates waiting to be
       * flushed
       */

    }, {
      key: "$hasPendingStateUpdates",
      value: function $hasPendingStateUpdates() {
        var stateQueue = this.$boundState;

        for (var p in stateQueue) {
          if (stateQueue[p].get) {
            return true;
          }
        }
      }
      /**
       * @private
       * @param {string} boundStateKey
       * @param {boolean} [andClear] if true, consumes the dirty flag so it will
       * be marked clean again. so the modified widget won't be rebuilt until it's
       * either marked unmodified or the state key is marked dirty again.
       * @return {boolean}
       */

    }, {
      key: "$isStateKeyDirty",
      value: function $isStateKeyDirty(boundStateKey, andClear) {
        if (boundStateKey) {
          var boundState = this.$boundState;
          var boundObj = boundState && boundState[boundStateKey];

          if (boundObj && boundObj.dirty) {
            if (andClear) {
              boundObj.dirty = false;
            }

            return true;
          }
        }

        return false;
      }
      /**
       * Un-dirty all dirty state keys. This will free them up to start
       * propagating state updates from MODIFY_EVENTs again.
       * @private
       */

    }, {
      key: "$undirtyState",
      value: function $undirtyState() {
        var boundState = this.$boundState;

        if (!boundState) {
          return;
        }

        Object.keys(boundState).forEach(function (key) {
          var boundObj = boundState[key];

          if (boundObj) {
            delete boundObj.dirty;
          }
        });
      }
    }], [{
      key: "mixin",
      value: function mixin(Ctor) {
        var spandrelProt = SpandrelWidget.prototype;
        var ctorProt = Ctor.prototype;
        Object.getOwnPropertyNames(spandrelProt).forEach(function (method) {
          if (method === 'constructor') {
            return;
          }

          if (method.startsWith('do')) {
            var ctorMethod = ctorProt[method];
            var spandrelMethod = spandrelProt[method];

            ctorProt[method] = function () {
              var _this11 = this;

              for (var _len = arguments.length, args = new Array(_len), _key = 0; _key < _len; _key++) {
                args[_key] = arguments[_key];
              }

              return Promise.resolve(ctorMethod && ctorMethod.apply(this, args)).then(function () {
                return spandrelMethod.apply(_this11, args);
              });
            };
          } else {
            ctorProt[method] = spandrelProt[method];
          }
        });
      }
    }]);

    return SpandrelWidget;
  }(Widget);

  function getStateBinding(ed) {
    return ed && ed.jq()[0][STATE_BINDING_SYMBOL];
  }

  function never() {
    return false;
  }

  function _queryWidgets(widget, keys) {
    var widgets = [widget];
    keys.forEach(function (key) {
      widgets = flatten(widgets.map(function (widget) {
        var jq = widget && widget.jq();
        return kidsMatching(jq && jq[0], key).map(Widget["in"]).filter(isReal);
      }));
    });
    return compact(widgets);
  }
  /**
   * This function does the work of intercepting modify events from a spandrel widget's children,
   * setting the spandrel widget itself to modified, and conveying the events upwards as
   * appropriate.
   *
   * Every spandrel widget gets its own reference to this function, so it can be specifically
   * targeted for arming/disarming.
   *
   * The way modify event bubbling works:
   *
   * - The original "OG" widget such as a BooleanEditor is marked modified by the user. A reference
   *   to this widget is stored throughout this process.
   * - That MODIFY_EVENT bubbles up through the DOM until it reaches the owner widget (the spandrel
   *   widget in whose render function that BooleanEditor was created).
   * - Once it reaches the owner widget, its propagation stops - it does not bubble past the owner.
   * - Instead, the owner triggers its own MODIFY_EVENT. This is the only one that an external
   *   MODIFY_EVENT handler will see.
   *
   * @this module:bajaux/Widget the widget initialized in the element the handler is armed on
   * @param {JQuery.Event} e
   * @param {module:bajaux/Widget} modifiedKid
   * @param {object} [params]
   * @param {module:bajaux/Widget} [params.ogKid] the widget that triggered the original MODIFY_EVENT
   * from the user
   * @param {module:bajaux/Widget} [params.owner] a reference to the owner of the OG kid
   */


  function modifyBubbler(e, modifiedKid, params) {
    if (!modifiedKid) {
      // manually triggering an event, not a .setModified() call = you're on your own
      return;
    }

    var owner = params && params.owner || modifiedKid[OWNER_SYMBOL];
    var iAmTheOwner = owner === this;
    var itIsMeGettingModified = e.target === this.jq()[0];

    if (iAmTheOwner && !itIsMeGettingModified) {
      var ogKid = params && params.ogKid || modifiedKid;
      this.setModified(true, {
        ogKid: ogKid,
        owner: this[OWNER_SYMBOL],
        stateKey: e.stateKey
      });
      e.stopImmediatePropagation();
    }
  }
  /**
   * This function handles the work of listening to modify events from child widgets, identifying
   * if they are bound to state, and if so, reading their current values and propagating them into
   * state.
   *
   * @this module:bajaux/Widget the widget initialized in the element the handler is armed on
   * @param {JQuery.Event} e
   * @param {module:bajaux/Widget} modifiedEd the widget that was modified. in a composite widget,
   * this may actually be the owner of the widget that the user actually interacted with.
   * @param {object} [params]
   * @param {module:bajaux/Widget} [params.ogKid] if bubbling up from a composite widget, this will
   * be the widget that the user actually interacted with.
   */


  function statePropagator(e, modifiedEd, params) {
    var _this12 = this;

    if (modifiedEd === this) {
      return;
    }

    var stateBinding = getStateBinding(modifiedEd);

    if (!stateBinding && params && params.ogKid && params.ogKid !== modifiedEd) {
      modifiedEd = params.ogKid;
      stateBinding = getStateBinding(modifiedEd);
    }

    if (!stateBinding) {
      return;
    }

    var _stateBinding = stateBinding,
        boundStateKey = _stateBinding.key,
        target = _stateBinding.target;
    /*
    a DOM element can be propagating state values to multiple ancestor widgets. only
    handle bindings that are actually pointing toward this particular widget.
     this 'dirty' check is checking to see if the bound widget we're getting
    a modify event from, is currently in the process of being re-rendered
    due to a call to this.state({ [bindKey]: newValue }).
     there for sure is a potential race condition here: if the dev says
    "THIS is the value for this state key", and before that rerender
    completes, the user manages to type in "no THIS is the value for this
    state key," the dev wins and the user changes are lost.
     i don't think there's a good resolution for this. the user is typing
    against a value that is now invalidated as per the dev's intentions.
    we can't just "hold on" to the modify event and apply it after the
    rerender because the value will be unreliable.
    */

    if (target !== this || this.$isStateKeyDirty(boundStateKey)) {
      return;
    }

    var timingDetailsLoggable = areTimingDetailsLoggable();
    var ticks = nextTicks();

    if (timingDetailsLoggable) {
      logTimingDetails(this, 'received bound state modification from key {}, new ticks {}', boundStateKey, ticks);
    }

    var stateUpdated = $deferred();
    this.$queueStateUpdate(boundStateKey, function () {
      return modifiedEd.read();
    }).then(stateUpdated.resolve, stateUpdated.reject);

    var updateState = function updateState() {
      return stateUpdated.promise.then(function () {
        if (timingDetailsLoggable) {
          logTimingDetails(_this12, 'finished applying state from bound key {}, ticks {}', boundStateKey, ticks);
        }
      });
    };
    /*
    holding with the "load() wins over rerender()" paradigm: consider state binding, as
    something a developer explicitly enables, to be analogous to load(). since this state
    update will also trigger a parent rerender, we bump the render ticks forward to prevent
    this value from possibly being overwritten by a stale value held by the parent rerender.
    see the render tick comparisons in DynamicSpandrelWidget.
     */


    this[RENDER_TICKS_SYMBOL] = ticks;

    var rerender = function rerender() {
      if (timingDetailsLoggable) {
        logTimingDetails(_this12, 'beginning rerender after applying state from bound key {}, ticks {}', boundStateKey, ticks);
      }

      return _this12.$doRerender().then(function () {
        if (timingDetailsLoggable) {
          logTimingDetails(_this12, 'completed rerender after applying state from bound key {}, ticks {}', boundStateKey, ticks);
        }
      });
    };

    this.$renderQueue.fromStateUpdate(updateState, rerender)["catch"](logSevere);
  }

  function makeSpandrelHandler(widget, func) {
    func = func.bind(widget);
    func[SPANDREL_HANDLER_PRIORITY] = 2;
    return func;
  }
});
/**
 * Keeps track of ongoing state updates.
 *
 * @typedef {Object.<string, module:bajaux/spandrel/SpandrelWidget~StateUpdate>} module:bajaux/spandrel/SpandrelWidget~StateUpdateRegistry
 */

/*
TODO: "dirty" is not a good word for what we're describing here.
 if "dirty" applied it would mean dirty as in a dirty trick: winning at all costs.
 dirty is for when code that a developer wrote calls .load() or .state() and forcefully PUTS
 information into the UI. concurrent information coming from events like "user is typing" will
 LOSE and get wiped out.
 unbeatable? authoritative? indomitable? omnipotent? decisive?
 */

/**
 * When binding widgets to state, updates to the state value will be represented
 * by a `StateUpdate`. The update should propagate through and then be removed
 * from the state queue.
 *
 * Some updates come from a user action, like modifying a field editor widget.
 * The new value will propagate through to the widget state, but are considered
 * "clean" or "polite" and can be cancelled out or overridden. For instance,
 * although state updates can result in a child widget being reloaded or
 * rebuilt, these "clean" updates are typically cancelled out if the target
 * widget is modified by the user.
 *
 * But sometimes we want a state update to take full effect no matter what.
 * These updates can be purposefully initiated by a developer.
 *
 * These developer-driven state updates can be considered "dirty," and they will
 * override any "clean" updates in progress. A prominent example of a "dirty"
 * update is when the developer calls `.state()` on the widget. These updates
 * "win" over updates coming in from user modifications like typing.
 *
 * @typedef {object} module:bajaux/spandrel/SpandrelWidget~StateUpdate
 * @property {function|undefined} get function to get the current bound value.
 * If there is no update in progress, this will be undefined.
 * @property {boolean} [dirty] whether this is a "dirty update"
 */
