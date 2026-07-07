function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _slicedToArray(arr, i) { return _arrayWithHoles(arr) || _iterableToArrayLimit(arr, i) || _unsupportedIterableToArray(arr, i) || _nonIterableRest(); }

function _nonIterableRest() { throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _iterableToArrayLimit(arr, i) { if (typeof Symbol === "undefined" || !(Symbol.iterator in Object(arr))) return; var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"] != null) _i["return"](); } finally { if (_d) throw _e; } } return _arr; }

function _arrayWithHoles(arr) { if (Array.isArray(arr)) return arr; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

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
define(['log!bajaux/spandrel', 'bajaux/spandrel/buildConfig', 'bajaux/spandrel/RequestLayoutMixin', 'bajaux/spandrel/logging', 'bajaux/spandrel/symbols', 'bajaux/spandrel/util', 'bajaux/events', 'bajaux/Widget', 'jquery', 'Promise', 'underscore'], function (log, buildConfig, RequestLayoutMixin, logging, symbols, util, events, Widget, $, Promise, _) {
  'use strict';

  var logSevere = log.severe.bind(log);
  var areTimingDetailsLoggable = logging.areTimingDetailsLoggable,
      logTimingDetails = logging.logTimingDetails;
  var MODIFY_EVENT = events.MODIFY_EVENT;
  var any = _.any,
      extend = _.extend,
      isArray = _.isArray,
      once = _.once;
  var DEPTH_SYMBOL = symbols.DEPTH_SYMBOL,
      IS_ELEMENT_SYMBOL = symbols.IS_ELEMENT_SYMBOL,
      LAX_SYMBOL = symbols.LAX_SYMBOL,
      OWNER_SYMBOL = symbols.OWNER_SYMBOL,
      RENDER_TICKS_SYMBOL = symbols.RENDER_TICKS_SYMBOL,
      ROOT_SYMBOL = symbols.ROOT_SYMBOL,
      SPANDREL_HANDLER_PRIORITY = symbols.SPANDREL_HANDLER_PRIORITY,
      STATE_BINDING_SYMBOL = symbols.STATE_BINDING_SYMBOL;
  var cloneNode = util.cloneNode,
      getContainingSpandrelWidget = util.getContainingSpandrelWidget,
      getPathToKid = util.getPathToKid,
      pathMatches = util.pathMatches,
      setValidationOfDescendent = util.setValidationOfDescendent;

  var ElementWidget = /*#__PURE__*/function (_Widget) {
    _inherits(ElementWidget, _Widget);

    var _super = _createSuper(ElementWidget);

    function ElementWidget() {
      _classCallCheck(this, ElementWidget);

      return _super.apply(this, arguments);
    }

    _createClass(ElementWidget, [{
      key: IS_ELEMENT_SYMBOL,
      get: function get() {
        return true;
      }
    }]);

    return ElementWidget;
  }(Widget); // avoid circular dependency


  var spandrel = once(function () {
    return require('bajaux/spandrel');
  });
  var ticks = 0;
  /**
   * This module contains utility methods for managing the construction, rebuilding, and destruction
   * of trees of child widgets.
   *
   * API Status: **Private**
   * @exports module:bajaux/spandrel/workflow
   */

  var workflow = {};
  /**
   * @param {module:bajaux/spandrel/SpandrelWidget} widget
   * @param {JQuery} dom
   * @param {Array} on
   */

  workflow.armHandlers = function (widget, dom, on) {
    var prev = widget.$spandrelHandlers;

    if (prev) {
      prev.forEach(function (_ref) {
        var _ref2 = _slicedToArray(_ref, 3),
            event = _ref2[0],
            handler = _ref2[2];

        return dom.off(event, handler);
      });
    } //allow 1-dimensional array handler


    if (on.length && !isArray(on[0])) {
      on = [on];
    }

    var spandrelHandlers = widget.$spandrelHandlers = [];
    on.forEach(function (arr) {
      var _arr2 = _slicedToArray(arr, 3),
          event = _arr2[0],
          selectorString = _arr2[1],
          handler = _arr2[2];

      if (arr.length === 2) {
        handler = selectorString;
        selectorString = null;
      }

      var selectors = selectorString ? selectorString.split(',').map(function (s) {
        return s.trim();
      }) : null;

      var eventHandler = function eventHandler(e) {
        var target = e.target;
        var spandrelWidget = getContainingSpandrelWidget(target);
        var args = Array.prototype.slice.call(arguments, 1);

        if (spandrelWidget) {
          //Widget#trigger already passes itself as first argument - don't double up
          if (spandrelWidget === args[0]) {
            args = args.slice(1);
          }

          var path = getPathToKid(widget, spandrelWidget);

          if (!selectors || anyPathMatches(selectors, path.join('/'))) {
            // let spandrel log errors for convenience, but don't let it
            // interfere with actual throw/reject behavior.
            try {
              var result = handler.apply(target, [e, spandrelWidget].concat(args));

              if (result === false) {
                return false;
              }

              return Promise.resolve(result)["catch"](logAndRethrow);
            } catch (err) {
              logAndRethrow(err);
            }
          }
        }
      };

      eventHandler[SPANDREL_HANDLER_PRIORITY] = 1;
      spandrelHandlers.push([event, selectorString, eventHandler]);
      dom.on(event, eventHandler);
    });
    workflow.$moveSpandrelModifyHandlersToFront(dom[0]);
  };
  /**
   * What needs to happen: the user-configured spandrel event handlers (e.g. onUxModify) need to run
   * first, then the core spandrel event handlers (modify event capturing and state value
   * propagation) need to run before any other jQuery event handlers (e.g. dom.on(MODIFY_EVENT)
   * armed outside of spandrel).
   *
   * Otherwise, the jQuery event handlers run before spandrel gets a chance to sanitize their
   * behavior (again, mostly modify events - see NCCB-64643).
   *
   * Right now, the only way to do this is to use private jQuery API. jQuery's official behavior is
   * that whatever gets armed first, runs first - so the inline jQuery handlers would always run
   * before the handlers that spandrel arms in doInitialize.
   *
   * @private
   * @param {HTMLElement} el
   */


  workflow.$moveSpandrelModifyHandlersToFront = function (el) {
    var events = $._data(el).events;

    var modifyHandlers = events && events[MODIFY_EVENT];

    if (!modifyHandlers) {
      return;
    } // first, pull the delegates out because it's jQuery behavior that they come first:


    var delegates = modifyHandlers.splice(0, modifyHandlers.delegateCount); // then the spandrel handlers:

    var spandrelHandlers = [];

    for (var i = 0, len = modifyHandlers.length; i < len;) {
      var modifyHandler = modifyHandlers[i];

      if (modifyHandler.handler[SPANDREL_HANDLER_PRIORITY]) {
        spandrelHandlers.push.apply(spandrelHandlers, _toConsumableArray(modifyHandlers.splice(i, 1)));
        len--;
      } else {
        i++;
      }
    }
    /*
    then sort them so handlers from spandrel data go first, then the core handlers from
    SpandrelWidget#doInitialize.
     as part of this commit, you might notice that the statePropagator handler had to change from
    delegated (dom.on(MODIFY_EVENT, '*', statePropagator)) to direct
    (dom.on(MODIFY_EVENT, statePropagator)). this is because jQuery event handling always runs
    delegated handlers first. with statePropagator delegated, sorting it after the direct
    modifyBubbler broke jQuery.
     */


    spandrelHandlers.sort(function (_ref3, _ref4) {
      var handler1 = _ref3.handler;
      var handler2 = _ref4.handler;
      return handler1[SPANDREL_HANDLER_PRIORITY] - handler2[SPANDREL_HANDLER_PRIORITY];
    }); // and put them all back together.

    modifyHandlers.unshift.apply(modifyHandlers, _toConsumableArray(delegates).concat(spandrelHandlers));
  };
  /**
   * Builds out the child widgets of a parent spandrel widget.
   *
   * @param {module:bajaux/Widget} widget the widget whose children we're building out
   * @param {module:bajaux/spandrel~BuildContext} buildContext
   * @param {JQuery} dom
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   * @returns {Promise}
   */


  workflow.buildChildWidgetsFromSpandrelData = function (widget, buildContext, dom, manager) {
    var members = buildContext.members,
        on = buildContext.on;
    workflow.armHandlers(widget, dom, on);
    var root = widget[ROOT_SYMBOL];
    var kidDepth = widget[DEPTH_SYMBOL] + 1;
    var owner = widget[IS_ELEMENT_SYMBOL] ? widget[OWNER_SYMBOL] : widget;
    return Promise.all(members.map(function (member) {
      var config = member.config;
      var kidDom = cloneNode(config.dom).appendTo(dom);
      return workflow.doFeBuild(member, kidDom, manager, root, owner, kidDepth);
    }));
  };
  /**
   * Build a widget in this element, as configured.
   *
   * @param {module:bajaux/spandrel~Member} member
   * @param {JQuery} dom
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   * @param {module:bajaux/spandrel/SpandrelWidget} root the root spandrel widget
   * @param {module:bajaux/spandrel/SpandrelWidget} owner the widget who will own the new widget
   * being created (see {@link module:bajaux/spandrel/symbols.OWNER_SYMBOL})
   * @param {number} depth the depth we're building at in the spandrel widget tree
   * @returns {Promise.<module:bajaux/Widget>} the built widget
   */


  workflow.doFeBuild = function (_ref5, dom, manager, root, owner, depth) {
    var key = _ref5.key,
        stateBinding = _ref5.stateBinding,
        lax = _ref5.lax,
        config = _ref5.config;
    return tryToDetermineType(config, manager, owner).then(function (type) {
      dom[0].spandrelKey = key;

      if (stateBinding) {
        dom[0][STATE_BINDING_SYMBOL] = stateBinding;
      }

      if (lax) {
        dom[0][LAX_SYMBOL] = true;
      }

      var data = extend({
        manager: manager
      }, config.data); // if we're bound to state, we want LOAD_EVENTs to fire when a new
      // widget appears so the bound state picks up the brand-new value

      var $quiet = !(config.properties && config.properties.$quiet === false);
      var params = extend({
        $constructorParams: {
          $quiet: $quiet
        },
        layoutParams: {
          quick: true
        }
      }, config, {
        data: data,
        dom: dom,
        type: type
      });
      return manager.makeFor(params).then(function (ed) {
        workflow.trackRenders(ed);
        ed[OWNER_SYMBOL] = owner;
        RequestLayoutMixin(ed, root || ed, depth, function (err, widget) {
          return manager.error(err, widget);
        });
        return manager.buildFor(params, ed);
      }).then(function (newKid) {
        setValidationOfDescendent(owner, newKid, config.validate);
        return newKid;
      });
    });
  };
  /**
   * @returns {number} incremented tick number
   */


  workflow.nextTicks = function () {
    // if somebody performs 9 quadrillion renders without having to reload
    // the page... good for us!
    if (ticks === Number.MAX_SAFE_INTEGER) {
      ticks = Number.MIN_SAFE_INTEGER;
    }

    return ticks++;
  };
  /**
   * Call this on a widget being constructed into a `spandrel` widget's widget tree.
   *
   * Every time a widget has a value loaded in, it will simply increment the render ticks. This
   * allows `spandrel` to make decisions regarding who loaded a value in before who.
   *
   * @param {module:bajaux/Widget} widget
   */


  workflow.trackRenders = function (widget) {
    if (!widget[RENDER_TICKS_SYMBOL]) {
      widget[RENDER_TICKS_SYMBOL] = -1;
      var load = widget.load;

      widget.load = function (value, loadParams) {
        var timingDetailsLoggable = areTimingDetailsLoggable();
        var ticks = loadParams && loadParams[RENDER_TICKS_SYMBOL];

        if (ticks) {
          if (timingDetailsLoggable) {
            logTimingDetails(this, 'trackRenders: #load called and setting instance ticks {} enforced by loadParams', ticks);
          }
        } else {
          ticks = workflow.nextTicks();

          if (timingDetailsLoggable) {
            logTimingDetails(this, 'trackRenders: #load called with no enforced ticks - setting incremented instance ticks {}', ticks);
          }
        }

        this[RENDER_TICKS_SYMBOL] = ticks;
        return load.call(this, value, loadParams);
      };
    }
  };

  return workflow;

  function spandrelWidget(arg, params) {
    return spandrel()(arg, params);
  }
  /**
   * @param {string[]} selectors array of widget selectors
   * @param {string} path actual path to a queried widget
   * @returns {boolean}
   */


  function anyPathMatches(selectors, path) {
    return any(selectors, function (selector) {
      return pathMatches(path, selector);
    });
  }

  function logAndRethrow(err) {
    logSevere(err);
    throw err;
  }
  /**
   * Given a config from a Spandrel member, determine what kind of widget
   * Spandrel should show for it.
   * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} config the
   * `config` property from a Spandrel member
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   * @param {module:bajaux/spandrel/SpandrelWidget} owner the widget who will own the new widget
   * being created (see {@link module:bajaux/spandrel/symbols.OWNER_SYMBOL})
   * @returns {Promise.<Function>} promise to be resolved with the kind of
   * widget to show. If none is specified or it can't figure it out, default to
   * Widget which will do nothing but show some raw HTML.
   */


  function tryToDetermineType(config, manager, owner) {
    var kids = config.kids;

    if (kids) {
      var _spandrelWidget;

      var members = kids.members,
          on = kids.on;
      return Promise.resolve(spandrelWidget({
        kids: members,
        on: on
      }, (_spandrelWidget = {}, _defineProperty(_spandrelWidget, IS_ELEMENT_SYMBOL, true), _defineProperty(_spandrelWidget, "manager", manager), _spandrelWidget)));
    }

    return manager.buildContext(extend({
      formFactor: 'mini'
    }, config)).then(function (buildContext) {
      return buildContext.widgetConstructor || ElementWidget;
    });
  }
});
