function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/**
 * @module bajaux/lifecycle/WidgetManager
 */
define(['bajaux/Widget', 'bajaux/lifecycle/JQueryElementTranslator', 'bajaux/registry/Registry', 'bajaux/widgets/ToStringWidget', 'Promise', 'nmodule/js/rc/asyncUtils/asyncUtils'], function (Widget, JQueryElementTranslator, Registry, ToStringWidget, Promise, asyncUtils) {
  'use strict';

  var doRequire = asyncUtils.doRequire;
  /**
   * WidgetManager's job is to manage the lifecycle of widgets, from the initial
   * "what kind of widget do I need?" question through to the destruction of
   * the unneeded widget.
   *
   * @class
   * @alias module:bajaux/lifecycle/WidgetManager
   * @param {object} params
   * @param {module:bajaux/registry/Registry} params.registry the registry
   * responsible for looking up Widget types
   * @since Niagara 4.10
   */

  var WidgetManager = /*#__PURE__*/function () {
    function WidgetManager() {
      var _ref = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {},
          _ref$registry = _ref.registry,
          registry = _ref$registry === void 0 ? new Registry() : _ref$registry,
          _ref$elementTranslato = _ref.elementTranslator,
          elementTranslator = _ref$elementTranslato === void 0 ? new JQueryElementTranslator() : _ref$elementTranslato;

      _classCallCheck(this, WidgetManager);

      this.$registry = registry;
      this.$elementTranslator = elementTranslator;
      this.$hooks = {};
    }
    /**
     * This method functions as the "starting point" for a Widget build. It
     * receives the parameters as given by the user, and calculates a build
     * context to be used during the rest of the initialize/load/destroy
     * lifecycle.
     *
     * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
     * @returns {Promise.<module:bajaux/lifecycle/WidgetManager~BuildContext>}
     */


    _createClass(WidgetManager, [{
      key: "buildContext",
      value: function buildContext(params) {
        if (!(params && _typeof(params) === 'object')) {
          return Promise.reject(new Error('params required'));
        }

        var data = params.data,
            dom = params.dom,
            enabled = params.enabled,
            formFactor = params.formFactor,
            hooks = params.hooks,
            initializeParams = params.initializeParams,
            keyName = params.keyName,
            layoutParams = params.layoutParams,
            loadParams = params.loadParams,
            moduleName = params.moduleName,
            properties = params.properties,
            readonly = params.readonly,
            value = params.value;
        return this.resolveConstructor(params).then(function (widgetConstructor) {
          var constructorParams = Object.assign({}, params.$constructorParams);

          if (formFactor) {
            constructorParams.formFactor = formFactor;
          }

          if (moduleName) {
            constructorParams.moduleName = moduleName;
          }

          if (keyName) {
            constructorParams.keyName = keyName;
          }

          if (properties) {
            constructorParams.properties = properties;
          }

          if (readonly) {
            constructorParams.readonly = true;
          }

          if (enabled === false) {
            constructorParams.enabled = false;
          }

          return {
            widgetConstructor: widgetConstructor,
            constructorParams: constructorParams,
            dom: dom,
            initializeParams: initializeParams,
            layoutParams: layoutParams,
            loadParams: loadParams,
            value: value,
            hooks: hooks,
            data: data || {}
          };
        });
      }
      /**
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @returns {Promise.<Function|undefined>} resolves the constructor to be
       * used to instantiate the widget, either as configured via params or as
       * looked up from the registry.
       */

    }, {
      key: "resolveConstructor",
      value: function resolveConstructor(params) {
        var _this = this;

        return Promise.resolve(this.deriveConfiguredConstructor(params)).then(function (ctor) {
          return ctor || _this.resolveFromRegistry(params);
        });
      }
      /**
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @returns {Function|Promise.<Function>} the constructor, as configured via
       * the `type` parameter, if present; otherwise undefined. Override to
       * define other methods of examining params to derive a directly-configured
       * constructor.
       */

    }, {
      key: "deriveConfiguredConstructor",
      value: function deriveConfiguredConstructor(params) {
        var type = params.type;

        if (typeof type === 'function') {
          return isAssignableFrom(Widget, type) ? Promise.resolve(type) : Promise.reject(new Error('type as constructor must extend bajaux/Widget'));
        }

        if (typeof type === 'string') {
          return doRequire(type);
        }
      }
      /**
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @returns {Promise<Function>} the constructor resolved from the registry.
       * By default, do a simple lookup by `params.value`; override to define how
       * registry lookups are performed.
       */

    }, {
      key: "resolveFromRegistry",
      value: function resolveFromRegistry(params) {
        return this.$registry.resolveFirst(params.value);
      }
      /**
       * Create a new Widget instance from the build context. If no widget
       * constructor could be determined, default to a
       * {@link module:bajaux/widgets/ToStringWidget|ToStringWidget}.
       *
       * @param {module:bajaux/lifecycle/WidgetManager~BuildContext} buildContext
       * @returns {module:bajaux/Widget|Promise.<module:bajaux/Widget>}
       */

    }, {
      key: "instantiate",
      value: function instantiate(buildContext) {
        var widgetConstructor = buildContext.widgetConstructor,
            constructorParams = buildContext.constructorParams,
            _buildContext$hooks = buildContext.hooks,
            hooks = _buildContext$hooks === void 0 ? {} : _buildContext$hooks;
        var Ctor = widgetConstructor || ToStringWidget;
        var widget = new Ctor(constructorParams);
        var instantiated = this.$hooks.instantiated;
        var inpInstantiated = hooks.instantiated; // For pre-spandrel widgets that try to set defaults after applying the Widget
        // constructor, reapply the params so that the defaults do not overwrite them.

        return Promise.resolve(widget.$properties.$modified && widget.$reapplyParams(constructorParams)).then(function () {
          return instantiated && instantiated(widget);
        }).then(function () {
          return inpInstantiated && inpInstantiated(widget);
        }).then(function () {
          return widget;
        });
      }
      /**
       * Initialize the widget into the DOM element as specified in the build
       * context.
       *
       * @param {module:bajaux/Widget} widget
       * @param {module:bajaux/lifecycle/WidgetManager~BuildContext} buildContext
       * @returns {Promise}
       */

    }, {
      key: "initialize",
      value: function initialize(widget, buildContext) {
        var _this2 = this;

        // here is where the hook would go: does this Widget want a jQuery object
        // to pass to initialize? HTMLElement? virtual DOM? if dom is an
        // HTMLElement and the Widget wants jQuery, can I just wrap it in $() and
        // pass it in? or vice versa?
        // in the future, plug in the appropriate hooks/services to provide a
        // robust way of putting widgets in elements.
        var dom = buildContext.dom,
            initializeParams = buildContext.initializeParams,
            layoutParams = buildContext.layoutParams,
            _buildContext$hooks2 = buildContext.hooks,
            hooks = _buildContext$hooks2 === void 0 ? {} : _buildContext$hooks2;
        var _this$$hooks = this.$hooks,
            preInitialize = _this$$hooks.preInitialize,
            postInitialize = _this$$hooks.postInitialize;
        var inpPreInitialize = hooks.preInitialize,
            inpPostInitialize = hooks.postInitialize;

        if (!dom) {
          return Promise.reject(new Error('dom required'));
        }

        return Promise.resolve(this.$elementTranslator.translateElement(dom)).then(function (dom) {
          var existing = Widget["in"](dom);
          return Promise.resolve(existing && _this2.destroy(existing)).then(function () {
            return preInitialize && preInitialize(widget, buildContext);
          }).then(function () {
            return inpPreInitialize && inpPreInitialize(widget, buildContext);
          }).then(function () {
            return widget.initialize(dom, initializeParams, layoutParams);
          }).then(function () {
            return postInitialize && postInitialize(widget, buildContext);
          }).then(function () {
            return inpPostInitialize && inpPostInitialize(widget, buildContext);
          });
        });
      }
      /**
       * Load the value from the build context into the widget.
       *
       * @param {module:bajaux/Widget} widget
       * @param {module:bajaux/lifecycle/WidgetManager~BuildContext} buildContext
       * @returns {Promise}
       */

    }, {
      key: "load",
      value: function load(widget, buildContext) {
        var loadParams = buildContext.loadParams,
            value = buildContext.value,
            _buildContext$hooks3 = buildContext.hooks,
            hooks = _buildContext$hooks3 === void 0 ? {} : _buildContext$hooks3;
        var _this$$hooks2 = this.$hooks,
            preLoad = _this$$hooks2.preLoad,
            postLoad = _this$$hooks2.postLoad;
        var inpPreLoad = hooks.preLoad,
            inpPostLoad = hooks.postLoad;

        if (value === undefined || widget.isDestroyed()) {
          return Promise.resolve();
        }

        return Promise.resolve(preLoad && preLoad(widget, buildContext)).then(function () {
          return !widget.isDestroyed() && inpPreLoad && inpPreLoad(widget, buildContext);
        }).then(function () {
          return !widget.isDestroyed() && widget.load(buildContext.value, loadParams);
        }).then(function () {
          return !widget.isDestroyed() && postLoad && postLoad(widget, buildContext);
        }).then(function () {
          return !widget.isDestroyed() && inpPostLoad && inpPostLoad(widget, buildContext);
        })["catch"](function (error) {
          if (!widget.isDestroyed()) {
            throw error;
          }
        });
      }
      /**
       * Destroy the widget.
       * @param {module:bajaux/Widget} widget
       * @returns {Promise}
       */

    }, {
      key: "destroy",
      value: function destroy(widget) {
        return widget.destroy();
      }
      /**
       * @private
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @param {module:bajaux/Widget} [widget]
       * @returns {Promise}
       */

    }, {
      key: "$doMakeFor",
      value: function $doMakeFor(params, widget) {
        var _this3 = this;

        return this.buildContext(params).then(function (buildContext) {
          return Promise.resolve(widget || _this3.instantiate(buildContext)).then(function (widget) {
            return {
              buildContext: buildContext,
              widget: widget
            };
          });
        });
      }
      /**
       * Resolves a new Widget instance as defined by the input parameters, but
       * does not initialize or load it anywhere.
       *
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @returns {Promise.<module:bajaux/Widget>}
       */

    }, {
      key: "makeFor",
      value: function makeFor(params) {
        return this.$doMakeFor(params).then(function (_ref2) {
          var widget = _ref2.widget;
          return widget;
        });
      }
      /**
       * Instantiates, initializes, and loads a value into a new Widget as defined
       * by the input parameters.
       * @param {module:bajaux/lifecycle/WidgetManager~BuildParams} params
       * @param {module:bajaux/Widget} [widget] if present, skip the instantiation
       * and just initialize/load the given widget instance.
       * @returns {Promise.<module:bajaux/Widget>} resolves to the widget after it
       * has been initialized and loaded.
       */

    }, {
      key: "buildFor",
      value: function buildFor(params, widget) {
        var _this4 = this;

        return this.$doMakeFor(params, widget).then(function (_ref3) {
          var buildContext = _ref3.buildContext,
              widget = _ref3.widget;
          return _this4.initialize(widget, buildContext).then(function () {
            return _this4.load(widget, buildContext);
          }).then(function () {
            return widget;
          });
        });
      }
      /**
       * Install hooks to be invoked at various stages of a widget lifecycle.
       * @param {module:bajaux/lifecycle/WidgetManager~BuildHooks} hooks
       */

    }, {
      key: "installHooks",
      value: function installHooks(hooks) {
        var myHooks = this.$hooks;
        ['preInitialize', 'postInitialize', 'preLoad', 'postLoad', 'instantiated', 'error'].forEach(function (hookName) {
          var hook = hooks[hookName];

          if (hook) {
            myHooks[hookName] = hook;
          }
        });
      }
      /**
       * This method is called when an error is encountered with a Widget.
       *
       * If there is an installed error hook on this manager, it will be invoked.
       *
       * @param {Error} err the error from the Widget
       * @returns {Promise} If an error hook is installed, this will
       * resolve once the error hook is finished. If no error hook is installed,
       * this will reject with the provided error.
       */

    }, {
      key: "error",
      value: function error(err, widget) {
        var error = this.$hooks.error;

        if (!error) {
          return Promise.reject(err);
        }

        return Promise.resolve(error(err, widget));
      }
    }]);

    return WidgetManager;
  }();
  /**
   * Object describing the parameters that can be passed to `WidgetManager` to
   * define a build context. Subclasses of `WidgetManager` may support
   * additional parameters.
   *
   * @typedef {Object} module:bajaux/lifecycle/WidgetManager~BuildParams
   * @property {String|Function} [type] a `bajaux/Widget` subclass constructor
   * function - if given, an instance of that Widget will *always* be
   * instantiated instead of dynamically looked up from the `value`. You can
   * also use a RequireJS module ID that resolves to a `Widget` subclass
   * constructor.
   * @property {*} [value] the value to be loaded into the new widget, if
   * applicable.
   * @property {string|HTMLElement|JQuery|*} [dom] the DOM element in which the
   * new widget should be initialized, if applicable.
   * @property {Object} [properties] the bajaux Properties the new widget
   * should have.
   * @property {Boolean} [enabled] set to `false` to cause the new widget to be
   * disabled. Not used for lookups.
   * @property {Boolean} [readonly] set to `true` to cause the new widget to be
   * readonly. Not used for lookups.
   * @property {String|Array.<String>} [formFactors] the possible form factors
   * the new widget should have. The created widget could match any of these
   * form factors depending on what is registered in the database. If no widget
   * is found that supports any of these form factors, then no widget will be
   * created (even if one is present that supports a different form factor). If
   * no form factor is given, then the widget created could be of *any* form
   * factor.
   * @property {String} [formFactor] same as a `formFactors` array of length 1.
   * @property {module:bajaux/lifecycle/WidgetManager~BuildHooks} [hooks] any
   * hooks you wish to run at various stages in this widget's lifecycle. They
   * will run immediately after any installed hooks.
   */

  /**
   * Object describing the configuration needed to construct, initialize, and
   * load a Widget in a DOM element.
   *
   * @typedef {Object} module:bajaux/lifecycle/WidgetManager~BuildContext
   * @property {Function} [widgetConstructor] Widget constructor to instantiate.
   * If no constructor could be found it is up to the WidgetManager to decide
   * whether to instantiate a default Widget type or to reject.
   * @property {object} constructorParams params object to pass to the Widget
   * constructor
   * @property {Object} [initializeParams] params object to pass to the
   * initialize() method
   * @property {Object} [layoutParams] params object to pass to the layout()
   * method
   * @property {Object} [loadParams] params object to pass to the load() method
   * @property {string|HTMLElement|JQuery|*} dom DOM element in which to build a
   * Widget. Will be translated by the `WidgetManager` to an appropriate type
   * for the Widget.
   * @property {*} [value] the value to load into the Widget. `null` is an
   * acceptable loadable value. If `undefined`, no loading should be performed.
   * @property {object} data any additional data passed by the caller into the
   * `WidgetManager` as the `data` property. This will be passed through the
   * build lifecycle untouched. Most useful when using lifecycle hooks to add
   * functionality to the `WidgetManager` instance.
   */

  /**
   * Object describing hooks to be invoked at various points in a widget's
   * lifecycle. Each hook will be invoked with `widget` and `buildContext`
   * arguments.
   *
   * @typedef {Object} module:bajaux/lifecycle/WidgetManager~BuildHooks}
   * @property {Function} [instantiated] called immediately after a widget is constructed
   * @property {Function} [preInitialize] called before `initialize()` is called
   * @property {Function} [postInitialize] called after `initialize()` completes
   * @property {Function} [preLoad] called before `load()` is called
   * @property {Function} [postLoad] called after `load()` completes
   * @property {module:bajaux/lifecycle/WidgetManager~error} [error] called when
   * a widget encounters an error
   */

  /**
   * A callback to handle the provided widget's error.
   *
   * @callback {Function} {module:bajaux/lifecycle/WidgetManager~error}
   *
   * @param {Error} the error the widget encountered
   * @param {module:bajaux/Widget} the widget that encountered the error
   * @returns {Promise}
   */

  /**
   * When building out a Widget, there are two DOM-related concerns.
   *
   * First, the `dom` parameter given to `WidgetManager` by the user could be: a
   * string, an `HTMLElement`, a jQuery instance, a React virtual DOM node, or
   * any other "DOM-element-like" object. The user is asking: please put a
   * Widget in _here_.
   *
   * Second, the `dom` parameter given to the Widget's `initialize()` function
   * must be one that the Widget knows how to initialize itself into. Since a
   * vanilla Widget typically wants to initialize itself into a jQuery
   * instance, if you hand it a virtual DOM node it won't know what to do.
   *
   * `ElementTranslator`'s job is to try to convert from the user-supplied `dom`
   * to a `dom` that is usable by a Widget. This should increase the versatility
   * of `WidgetManager` and allow Widgets to be used in different environments.
   *
   * @private
   * @interface ElementTranslator
   * @memberOf module:bajaux/lifecycle/WidgetManager
   */
  //TODO: later, when we need to translate to non-jquery doms, create a composite
  // translator to give multiple translator types a go

  /**
   * Translate a DOM element into one usable by the Widget.
   *
   * @function
   * @name module:bajaux/lifecycle/WidgetManager~ElementTranslator#translateElement
   * @param {*} dom the input `dom`, of unpredictable type, given to `WidgetManager`
   * by the user
   * @param {module:bajaux/Widget} widget the widget to be initialized into this
   * `dom` element
   * @returns {Promise.<*>} to be resolved to a DOM element usable by the
   * Widget, or rejects if there is no way to turn the input `dom` into an
   * element the Widget likes
   */

  /**
   * @param {Function} superCtor
   * @param {Function} subCtor
   * @returns {boolean}
   */


  function isAssignableFrom(superCtor, subCtor) {
    return Object.create(subCtor.prototype) instanceof superCtor;
  }

  return WidgetManager;
});
