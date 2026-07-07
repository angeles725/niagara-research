function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson
 */

/**
 * The base Widget class, used for loading Web based Niagara Widgets.
 * 
 * @module bajaux/Widget
 * @requires lex
 * @requires jquery
 * @requires bajaux/events
 * @requires bajaux/Properties
 * @requires bajaux/Validators
 */
define(['lex!', 'jquery', 'Promise', 'bajaux/events', 'bajaux/Properties', 'bajaux/commands/CommandGroup', 'bajaux/commands/Command', 'bajaux/Validators', 'nmodule/js/rc/tinyevents/tinyevents', 'log!bajaux.Widget'], function (lex, $, Promise, events, Properties, CommandGroup, Command, Validators, tinyevents, log) {
  "use strict";

  var logWarning = log.warning.bind(log);
  var notInitialized = "Not initialized!";
  var alreadyDestroyed = "Already destroyed!";
  var ROOT_CSS_CLASS_SYMBOL = Symbol('rootCssClass');
  var idCount = 0;
  /**
   * A Widget contains the most basic mechanisms necessary for displaying user
   * interface upon a DOM element. It should not be instantiated directly,
   * but rather subclassed with the specific functionality you need.
   *
   * At a bare minimum, either the `doInitialize` or `doLoad` function will need
   * to be implemented by your subclass.
    * @class
   * @alias module:bajaux/Widget
   *
   * @param {Object} [params] a parameters object.
   * @param {Object} [params.properties] properties to add to this editor's
   * underlying `bajaux/Properties` instance.
   * @param {Boolean} [params.enabled] false to disable this editor
   * @param {Boolean} [params.readonly] true to readonly this editor
   * @param {String} [params.formFactor] form factor this editor should use
   * (c.f. `Widget.formfactor`)
   * @param {String} [params.keyName] the key name that bajaux should use to
   * look up lexicon entries for this editor
   * @param {String} [params.moduleName='bajaux'] the module name that
   * bajaux should use to look up lexicon entries for this editor
   * @param {Object} [params.data] optional additional configuration data that
   * may be used on a per-widget basis. This will often be used in conjunction
   * with `fe`.
   *
   * @param {Object} [params.params] in conjunction with `defaults`, allows a
   * Widget subclass to accept parameters passed to the constructor as well as
   * an initial set of default values. See example.
   * @param {Object} [params.defaults] in conjunction with `params`, allows a
   * Widget subclass to define an initial set of default values. See example.
   *
   * @example
   * <caption>Construct a Widget, defining its initial configuration with an
   * object literal.</caption>
   * const w = new Widget({
   *   readonly: true,
   *   properties: { foo: 'bar' },
   *   formFactor: 'compact'
   * });
   *
   * @example
   * <caption>Define a Widget subclass with an initial set of default values,
   * while still allowing parameters to be passed to the constructor. This is
   * the preferred method of calling a Widget super-constructor.</caption>
   *
   * class MyButton extends Widget {
   *   constructor(params) {
   *     super({
   *       params,
   *       defaults: {
   *         moduleName: 'myModule',
   *         keyName: 'MyButton',
   *         properties: { borderRadius: 15, padding: 10 }
   *       }
   *     });
   *   }
   * }
   * const button = new MyButton({ properties: { padding: 20 } });
   * button.properties().getValue('borderRadius'); // 15, from defaults
   * button.properties().getValue('padding'); // 20, from constructor
   *
   * @example
   * <caption>params and defaults will be correctly processed even when
   * subclassing.</caption>
   * class AWidget extends Widget {
   *   constructor(params) {
   *     super({ params, defaults: { properties: { name: 'A' } } });
   *   }
   * }
   * class BWidget extends AWidget {
   *   constructor(params) {
   *     // defaults will be merged in with superclass defaults.
   *     super({ params, defaults: { properties: { name: 'B' } } });
   *   }
   * }
   * new BWidget().properties().getValue('name'); // 'B'
   * new BWidget({ properties: { name: 'Bee' } }).properties().getValue('name'); // 'Bee'
   */

  var Widget = function Widget(params) {
    var _this = this;

    // note: DO NOT convert to ES6 class - this will break "Widget.apply(this)" pattern everyone uses
    var args = arguments;

    if (!params || _typeof(params) !== 'object') {
      params = {
        moduleName: args[0],
        keyName: args[1],
        formFactor: args[2]
      };
    }

    params = processParams(params);
    var props = new Properties();
    var moduleName = params.moduleName || 'bajaux';
    var keyName = params.keyName || 'widget';
    var formFactor = params.formFactor || Widget.formfactor.max;
    /**
     * Whether this widget is currently in the process of initializing - set to
     * `true` when `initialize` is called, and set to
     * `false` after its callback completes.
     * @private
     * @type {Boolean}
     */

    this.$initializing = false;
    var initResolved = false;
    /**
     * A promise to be resolved after the Widget finishes initializing.
     * @private
     * @type {Promise}
     */
    // eslint-disable-next-line promise/avoid-new

    this.$initPromise = new Promise(function (resolve) {
      _this.$resolveInit = resolve;
    }).then(function () {
      initResolved = true;
    });

    this.$initPromise.isResolved = function () {
      return initResolved;
    };
    /**
     * Indicates whether this widget is enabled or not.
     * @private
     * @type {Boolean}
     */


    this.$enabled = true;
    /**
     * An internal array of MixIns registered on this widget.
     * @private
     */

    this.$mixins = [];
    /**
     * Name of the Niagara Module associated with this widget. This is
     * used for looking up values from a lexicon.
     * @private
     * @type {String}
     */

    this.$moduleName = moduleName;
    /**
     * Name of type associated with this widget. This is
     * used for looking up values from a lexicon.
     * @private
     * @type {String}
     */

    this.$keyName = keyName;
    /**
     * The widget's form-factor.
     * @type {String}
     * @private
     */

    this.$formFactor = formFactor;
    /**
     * The Properties for a widget.
     * @private
     * @type {module:bajaux/Properties}
     */

    this.$properties = props; // By default, use the editor's display name.      

    this.setCommandGroup(new CommandGroup("%lexicon(".concat(moduleName, ":").concat(keyName, ".displayName)%")));
    /**
     * Whether any widgets in this view have been changed.
     * @private
     * @type {Boolean}
     */

    this.$modified = false;
    /**
     * Whether this widget is currently in the process of loading - set to
     * `true` when `load` is called, and set to
     * `false` after its callback completes.
     * @private
     * @type {Boolean}
     */

    this.$loading = false;
    /**
     * The widget's current value.
     * @private
     */

    this.$value = null;
    /**
     * The widget's collection of Validators.
     * @private
     */

    this.$validators = new Validators(this);
    /**
     * The widget's readonly state.
     * @private
     */

    this.$readonly = false;
    /**
     * This flag is not yet exposed as public API. It is intended to be used
     * only by Tridium developers.
     *
     * When in "quiet mode" the widget will not be as chatty when firing events.
     * Firing a jQuery event isn't free, so in situations when you have huge
     * numbers of Widgets this can help performance. The following events are
     * squelched in quiet mode:
     *
     * - `INITIALIZE_EVENT`
     * - `LOAD_EVENT`
     * - `SAVE_EVENT`
     * - `ENABLE_EVENT`
     * - `DISABLE_EVENT`
     * - `READONLY_EVENT`
     * - `WRITABLE_EVENT`
     * - `LAYOUT_EVENT`
     * - `DESTROY_EVENT`
     *
     * Note that error events like `INITIALIZE_FAIL_EVENT`, and events that are
     * critical for operation like `MODIFY_EVENT`, will still be fired.
     *
     * @private
     * @type {boolean}
     */

    this.$quiet = !!params.$quiet;
    /**
     * The widget's form-factor.
     * @type {String}
     * @private
     */

    this.applyParams(params);
    this.$properties.$modified = false;
    props.on(events.PROPERTY_ADDED, function (name) {
      _this.trigger(events.PROPERTY_ADDED, name);
    });
    props.on(events.PROPERTY_CHANGED, function (names, values, options) {
      var initialized = _this.isInitialized();

      names.forEach(function (name, i) {
        var value = values[i];

        _this.trigger(events.PROPERTY_CHANGED, name, value, options);

        if (initialized) {
          _this.changed(name, value, options);
        }
      });
    });
    props.on(events.PROPERTY_REMOVED, function (name, prop) {
      _this.trigger(events.PROPERTY_REMOVED, name, prop);
    });
    props.on(events.METADATA_CHANGED, function (name, value, options) {
      _this.trigger(events.METADATA_CHANGED, name, value, options);
    });
    tinyevents(this);
  };
  /**
   * Can re-apply certain params that can also be passed to the constructor.
   * @param {Object} [params]
   * @param {Boolean} [params.readonly]
   * @param {Boolean} [params.enabled=true] must explicitly set to false to
   * disable
   * @param {Object} [params.properties]
   * @returns {Promise} promise to be resolved after any
   * `setEnabled`/`setReadonly` work is done. Note that these functions will not
   * be called if the value of `enabled`/`readonly` is not actually changing.
   * @since Niagara 4.10
   */


  Widget.prototype.applyParams = function (params) {
    params = params || {};
    var readonly = !!params.readonly,
        properties = params.properties,
        enabled = getEnabledFromParams(params);
    return Promise.all([readonly !== this.isReadonly() && this.setReadonly(readonly), enabled !== this.isEnabled() && this.setEnabled(enabled), this.properties().addAll(new Properties(properties))]);
  };
  /**
   * Convenience to process and apply params.
   * The intention of this method is to support older style Widgets that
   * add property defaults in the constructor after applying the Widget constructor.
   * That was causing any incoming property value to be reset by the default.
   * This method maybe removed in the future.
   * 
   * @since Niagara 4.10
   * @private
   * @returns {Promise}
   */


  Widget.prototype.$reapplyParams = function (params) {
    logWarning(this.constructor + ' -> Modifying Properties in Widget constructor after super call,' + ' may not be supported in the future.' + ' Pass "defaults" to the constructor instead.');
    return this.applyParams(processParams(params));
  };
  /**
   * Allows for Widget-specific extension of parameters objects, granting more
   * control over which params are invariant for your Widget subclass and which
   * can be overridden by the constructor. Default behavior is to do a deep
   * extend on any `properties` argument, and shallow extend for other
   * properties.
   *
   * @private
   * @since Niagara 4.10
   * @deprecated since Niagara 4.14 - use `{ params, defaults }` Widget constructor signature instead
   * @param {...object} [obj] zero or more params objects. Properties later in the
   * arguments list will overwrite earlier properties.
   *
   * @returns {object} extended params object
   *
   * @example
   * <caption>When implementing a Widget subclass, I may want certain params to
   * always be applied, while other params may be overridden by the constructor.
   * </caption>
   *
   * class MyWidget extends Widget {
   *   constructor(params) {
   *     super(Widget.$extendParams(
   *     {
   *       properties: { myProp: 'myProp' } // define my default set of properties here...
   *     },
   *     params, // allow the constructor to override them with own values...
   *     {
   *       moduleName: 'MyModule', // and enforce a constant Widget display name.
   *       keyName: 'MyWidget'
   *     }));
   *   }
   * }
   */


  Widget.$extendParams = function () {
    var args = Array.prototype.slice.apply(arguments);
    var result = {};
    var propsObjects = [];
    var defaults;
    args.forEach(function (arg) {
      if (!arg || _typeof(arg) !== 'object') {
        return;
      }

      Object.keys(arg).forEach(function (key) {
        if (key === '__proto__') {
          return;
        }

        if (key === 'properties') {
          var props = arg[key];

          if (props) {
            propsObjects.push(props);
          }
        } else if (key === 'defaults') {
          defaults = arg[key];
        } else {
          result[key] = arg[key];
        }
      });
    });

    if (propsObjects.length) {
      var defaultProps = defaults && defaults.properties;
      var propObjectsWithDefaults = propsObjects.map(function (propObjs) {
        var propsWithDefault = {};
        var propsWithoutDefault = propObjs instanceof Properties ? propObjs.toObject() : propObjs;
        var allPropNamesAndSymbols = Object.keys(propsWithoutDefault).concat(Object.getOwnPropertySymbols(propsWithoutDefault));
        allPropNamesAndSymbols.forEach(function (propName) {
          var originalProp = propsWithoutDefault[propName]; // Make sure we have a JSON object with a { value: '' } structure

          if (_typeof(originalProp) !== 'object' || originalProp === null || !originalProp.hasOwnProperty('value')) {
            propsWithDefault[propName] = {
              value: originalProp
            };
          } else {
            propsWithDefault[propName] = originalProp;
          } // Provide this prop with a default value.


          if (defaultProps && defaultProps.hasOwnProperty(propName)) {
            if (defaultProps[propName].hasOwnProperty('value')) {
              propsWithDefault[propName].defaultValue = defaultProps[propName].value;
            } else {
              propsWithDefault[propName].defaultValue = defaultProps[propName];
            }
          } else {
            propsWithDefault[propName].defaultValue = propsWithDefault[propName].value;
          }
        });
        return propsWithDefault;
      });
      result.properties = Properties.extend.apply(Properties, _toConsumableArray(propObjectsWithDefaults));
    }

    return result;
  };

  Widget.$ROOT_CSS_CLASS_SYMBOL = ROOT_CSS_CLASS_SYMBOL;
  /**
   * Widget form-factors.
   *
   * @see module:bajaux/Widget#getFormFactor
   *
   * @property {String} max A large Widget (e.g. a View).
   * @property {String} compact A medium sized Widget (e.g. a dashboard Widget).
   * @property {String} mini A small Widget (e.g. a Field Editor).
   */

  Widget.formFactor = Widget.formfactor = {
    max: "max",
    compact: "compact",
    mini: "mini"
  };
  /**
   * Widget CSS class names.
   *
   * @property {String} initialized `bajaux-initialized`: applied after the
   * widget is initialized.
   * @property {String} disabled `bajaux-disabled`: applied when the widget is
   * disabled.
   * @property {String} designTime `bajaux-design-time`: Applied to an ancestor
   * DOM element to indicate the widget is running in a graphic design editor.
   * You can use this to style your widget differently between design time and
   * production.
   * @property {String} readonly `bajaux-readonly`: Applied when the widget is
   * marked readonly.b
   * @property {String} max `bajaux-max`: A large Widget (e.g. a View).
   * @property {String} compact `bajaux-compact`: A medium sized Widget (e.g. a
   * dashboard Widget).
   * @property {String} mini `bajaux-mini`: A small Widget (e.g. a Field
   * Editor).
   */

  Widget.css = {
    initialized: "bajaux-initialized",
    disabled: "bajaux-disabled",
    designTime: "bajaux-design-time",
    readonly: "bajaux-readonly",
    max: "bajaux-" + Widget.formfactor.max,
    compact: "bajaux-" + Widget.formfactor.compact,
    mini: "bajaux-" + Widget.formfactor.mini
  };
  var css = Widget.css;
  var unknownErr = Widget.unknownErr = "unknown error";
  var allFormFactorCss = [css.max, css.compact, css.mini].join(' ');
  var classesToRemoveOnDestroy = Object.keys(css).map(function (c) {
    return css[c];
  });
  /**
   * Generate a unique DOM ID. The ID will include the name of this widget's
   * constructor just for tracing/debugging purposes.
   *
   * @returns {String}
   */

  Widget.prototype.generateId = function () {
    var prefix = this.constructor.name || 'Widget';
    return prefix + '__id__' + idCount++;
  };
  /**
   * Return the widget's form-factor. The form-factor 
   * is normally passed in from the Widget's constructor. However,
   * it can be set from a 'formFactor' property if required. 
   *
   * A widget's form-factor typically doesn't change during a widget's
   * life-cycle.
   *
   * @see module:bajaux/Widget.formfactor
   * 
   * @return {String} The form-factor.
   */


  Widget.prototype.getFormFactor = function () {
    return this.properties().getValue("formFactor", this.$formFactor);
  };
  /**
   * A widget may need to do its own layout calculation. It might need
   * to statically position elements, or show/hide them based
   * on the shape of its container.
   * 
   * This function gives a widget an opportunity to do that. It's called once
   * the Widget has been initialized and once the form factor has changed.
   * Also it may be called when the widget's container changes shape
   * or size, or is shown/hidden.
   * 
   * This method should not typically be overridden. Override
   * {@link module:bajaux/Widget#doLayout|doLayout()} instead.
   *
   * @param {*} [params] as of Niagara 4.10, any parameters passed to
   * `layout()` will also be passed to `doLayout()`.
   * @returns {Promise} A promise that's resolved once the layout has
   * completed.
   */


  Widget.prototype.layout = function (params) {
    var _this2 = this;

    if (!this.isInitialized()) {
      return Promise.resolve();
    }

    return Promise.resolve(this.doLayout(params)).then(function () {
      // Trigger the layout event once the Widget has been laid out.
      if (!_this2.$quiet) {
        _this2.trigger(events.LAYOUT_EVENT);
      }
    });
  };
  /**
   * Called when the layout of the Widget changes. This method is designed
   * to be overridden.
   *
   * @param {*} [params] as of Niagara 4.10, any parameters passed to
   * `layout()` will also be passed to `doLayout()`.
   * @returns {*|Promise} This method may optionally return a promise once the
   * Widget has been laid out.
   */


  Widget.prototype.doLayout = function (params) {};
  /**
   * Access the widget's display name asynchronously.
   * 
   * By default, this will attempt to access the widget's display name from
   * the originating Lexicon. The Lexicon key should be in the format of
   * `keyName.displayName`. If an entry can't be found then the Type's 
   * name will be used.
   *
   * @returns {Promise} A promise to be resolved with the widget's display name
   */


  Widget.prototype.toDisplayName = function toDisplayName() {
    var $moduleName = this.$moduleName,
        $keyName = this.$keyName; // Attempt to access the Widget's displayName from the originating module's 
    // Lexicon as 'keyName.displayName'.

    return lex.module($moduleName).then(function (moduleLex) {
      return moduleLex.get($keyName + ".displayName") || $keyName;
    })["catch"](function () {
      return $keyName;
    });
  };
  /**
   * Access the widget's icon asynchronously.
   * 
   * By default this will attempt to access the widget's icon from
   * the originating Lexicon. The Lexicon key should be in the format of
   * `keyName.description`. If an entry can't be found then a blank string
   * will be used.
   * 
   * @returns {Promise} A promise to be resolved with the widget's description
   */


  Widget.prototype.toDescription = function toDescription() {
    var $moduleName = this.$moduleName,
        $keyName = this.$keyName; // Attempt to access the Widget's description from the originating module's 
    // Lexicon as 'keyName.description'.

    return lex.module($moduleName).then(function (moduleLex) {
      return moduleLex.get($keyName + ".description") || "";
    })["catch"](function () {
      return '';
    });
  };
  /**
   * Access the widget's icon asynchronously.
   * 
   * By default, this will attempt to access the widget's description from
   * the originating Lexicon. The Lexicon key should be in the format of
   * `keyName.icon`. If an entry can't be found then a blank String will be 
   * returned.
   *
   * @returns {Promise} A promise to be resolved with the widget's icon URI.
   */


  Widget.prototype.toIcon = function toIcon() {
    var $moduleName = this.$moduleName,
        $keyName = this.$keyName; // Attempt to access the widget's icon ORD from the originating module's 
    // Lexicon as 'keyName.icon'.

    return lex.module($moduleName).then(function (moduleLex) {
      var s = moduleLex.get($keyName + ".icon");
      return s ? s.replace(/^module:\/\//, "/module/") : "";
    })["catch"](function () {
      return '';
    });
  };
  /**
   * Return the widget's command group.
   *
   * @returns {module:bajaux/commands/CommandGroup}
   */


  Widget.prototype.getCommandGroup = function getCommandGroup() {
    return this.$commandGroup;
  };
  /**
   * Set this widget's command group. Triggers a `bajaux:changecommandgroup`
   * event.
   *
   * @param {module:bajaux/commands/CommandGroup} commandGroup
   */


  Widget.prototype.setCommandGroup = function setCommandGroup(commandGroup) {
    var _this3 = this;

    commandGroup.visit(function (c) {
      return c.jq(_this3.$jq);
    });
    this.$commandGroup = commandGroup;
    this.trigger(events.command.GROUP_CHANGE_EVENT);
  };
  /**
   * Private callback for when the DOM element is changed.
   *
   * @private
   */


  Widget.prototype.domChanged = function domChanged() {
    var _this4 = this;

    this.$commandGroup.visit(function (c) {
      return c.jq(_this4.$jq);
    });
  };
  /**
   * Return a promise that will only resolve after `initialize` has resolved.
   * Will reject if the widget is destroyed.
   * 
   * @private
   * @returns {Promise}
   */


  Widget.prototype.$initialized = function () {
    if (this.$destroyed) {
      return Promise.reject(new Error(alreadyDestroyed));
    }

    return this.$initPromise;
  };
  /**
   * Return true if this Widget is initialized.
   *
   * @returns {Boolean}
   */


  Widget.prototype.isInitialized = function isInitialized() {
    return this.$initPromise.isResolved() && !this.$destroyed;
  };
  /**
   * Return true if this Widget has already been destroyed. After destruction,
   * `initialize()` will always reject: the widget cannot be reused.
   * 
   * @returns {Boolean}
   */


  Widget.prototype.isDestroyed = function isDestroyed() {
    return !!this.$destroyed;
  };
  /**
   * Find the Widget instance living in this DOM element.
   *
   * @param {JQuery|HTMLElement} el
   * @returns {module:bajaux/Widget|undefined}
   * @since Niagara 4.6
   */


  Widget["in"] = function (el) {
    // noinspection JSValidateTypes
    return $(el).data('widget');
  };
  /**
   * Initializes the DOM element to be bound to this Widget.
   * 
   * In a nutshell, `initialize` defines the following contract:
   * 
   * * After `initialize` completes and resolves its Promise, the target element will be fully
   *   initialized, structured, and ready to load in a value. It will be accessible by calling
   *   `this.jq()`.
   * * If this is an editor, `load` may not be called until 
   *   `initialize`'s promise is resolved. Attempting 
   *   to load a value prior to initialization will result in failure.
   * * This widget will be set as a jQuery data value on the initialized
   *   DOM element. It can be retrieved by calling `Widget.in(element)`.
   * 
   * `initialize` delegates the actual work of building the
   * HTML structure (if any) to the `doInitialize` function. When
   * subclassing Widget, you should not override `initialize`.
   * `doInitialize` should be overridden.
   * 
   * After `initialize` completes, an  `bajaux:initialize` or
   * `bajaux:initializefail` event will be triggered, as appropriate.
   * 
   * `initialize` is a one-time operation. It will always reject if the widget has already been
   * initialized once, or if it has been destroyed.
   *
   * @see module:bajaux/Widget#doInitialize
   * 
   * @param {JQuery} dom The jQuery DOM element in which this widget should
   * build its HTML (will be passed directly to `doInitialize`)
   * @param {*} [params] optional parameters object to be passed through to
   * `doInitialize`
   * @param {*} [layoutParams] as of Niagara 4.10, optional parameters object to
   * be passed through to `layout`
   * @returns {Promise} A promise to be resolved once the widget has
   * initialized
   */


  Widget.prototype.initialize = function initialize(dom, params, layoutParams) {
    var _this5 = this;

    var prom = this.$initialized();

    if (prom.isResolved()) {
      return prom;
    }

    if (this.$initializing) {
      return Promise.reject(new Error('already initialized!'));
    }

    if (Widget["in"](dom)) {
      return Promise.reject(new Error("DOM element already has 'widget' data!"));
    }

    this.$initializing = true;

    var initializeFail = function initializeFail(err) {
      err = err || new Error(unknownErr);

      try {
        _this5.trigger(events.INITIALIZE_FAIL_EVENT, err);
      } catch (ignore) {} //roll back the binding if initialization fails.


      delete _this5.$jq;
      throw err;
    }; // Once the Command Group has finished loading, initialize the Widget.


    return this.$commandGroup.loading().then(function () {
      // Set this before doInitialize is called so methods like #isDesignTime will
      // work during initialization callbacks.
      _this5.$jq = dom;
      return _this5.doInitialize(dom, params);
    }).then(function () {
      try {
        dom.data('widget', _this5).addClass(Widget.css.initialized);

        _this5.domChanged();

        if (!_this5.$quiet) {
          _this5.trigger(events.INITIALIZE_EVENT);
        }
      } catch (eventHandlerErr) {
        dom.removeData('widget').removeClass(Widget.css.initialized);
        throw eventHandlerErr;
      }
    }).then(function () {
      // Remove any of the old form factor classes and add the new one.
      _this5.$jq.removeClass(allFormFactorCss).addClass(Widget.css[_this5.getFormFactor()]).addClass(getRootCssClass(_this5));

      _this5.$resolveInit(dom, params); // Ensure the widget is laid out once initialized. init promise must resolve, since
      // layout() is a no-op until isInitialized() returns true.
      // we also want to ensure that any listeners for the "initialized" tinyevent get notified
      // *after* isInitialized() returns true. this is to protect the pattern:
      // if (widget.isInitialized()) { doIt(); } else { widget.on('initialized', doIt); }


      return prom.then(function () {
        _this5.emit('initialized');

        return _this5.layout(layoutParams);
      });
    }, initializeFail)["finally"](function () {
      _this5.$initializing = false;
    });
  };
  /**
   * Performs the actual work of initializing the DOM element in which this
   * widget will live. This function should be overridden by subclasses - the 
   * subclass function should append elements to `element` as 
   * necessary and then optionally return a promise.
   * 
   * Most commonly, this will involve building up the HTML structure 
   * necessary to load in a value. If this widget will display/edit a String,
   * for example, `doInitialize` might append a text input element to 
   * the target element. A `DynamicEnum` might include a 
   * `<select>` dropdown. 
   * 
   * In some cases, no initialization may be required at all. This 
   * might be the case if you are binding the widget to an HTML element that is
   * already pre-populated with all the necessary structure to load a value,
   * or maybe `doLoad` will empty out the element completely
   * and rebuild it from scratch every time a new value is loaded. In this
   * case, you do not need to override this method. (However, a widget that
   * overrides neither `doInitialize` nor `doLoad` will not be very useful!)
   * 
   * Tip: the promises returned by `setEnabled` and `setReadonly` can
   * only ever resolve after `initialize` itself resolves. So
   * `return this.setEnabled(enabled)` or `return this.setReadonly(readonly)`
   * from `doInitialize` will result in a deadlock that will never resolve.
   * They may be called, but not returned.
   * 
   * @see module:bajaux/Widget#initialize
   * 
   * @param {JQuery} element The element in which this Widget should build its
   * HTML.
   * @param {Object} [params] Optional params object passed into `initialize()`.
   * @returns {*|Promise} An optional promise to be resolved once the Widget
   * has initialized.
   */


  Widget.prototype.doInitialize = function doInitialize(element, params) {};
  /**
   * Called to clean up the DOM when the widget is being destroyed.
   *
   * This method can be overridden if DOM clean up needs to be 
   * handled in a different way.
   */


  Widget.prototype.cleanupDom = function cleanupDom() {
    this.jq().empty().off();
  };
  /**
   * Indicates that a widget is no longer needed and is in the process of being
   * removed. In this function, subclasses can deallocate any resources, event 
   * handlers, etc. that they may be holding. Delegates the actual work to 
   * `doDestroy`.
   *
   * This method will not typically be overridden. `doDestroy()` should be
   * overridden instead.
   * 
   * Triggers a `bajaux:destroy` or `bajaux:destroyfail` event, as appropriate.
   *
   * Please note, after `doDestroy` has resolved, the DOM will be emptied, 
   * all event handlers will be removed and the 'widget' data stored on the
   * DOM element will be deleted.
   * 
   * @see module:bajaux/Widget#doDestroy
   *
   * @param {object} [params] optional parameters to be passed to `doDestroy`
   * @returns {Promise} A promise to be resolved when the widget has been
   * destroyed
   */


  Widget.prototype.destroy = function destroy(params) {
    var _this6 = this;

    var jq = this.$jq;

    var cleanup = function cleanup() {
      if (_this6.$jq) {
        // Strips, removes and cleans up everything after
        // necessary events have fired.
        jq.removeData("widget");
        jq.removeClass(classesToRemoveOnDestroy);
        jq.removeClass(getRootCssClass(_this6));

        _this6.cleanupDom();

        _this6.$jq = null;
      }

      if (_this6.$properties) {
        _this6.$properties.removeAllListeners(events.PROPERTY_ADDED).removeAllListeners(events.PROPERTY_CHANGED).removeAllListeners(events.PROPERTY_REMOVED);
      }
    }; // If the Widget isn't initialized then just clean up listeners and mark
    // the widget as destroyed.


    if (!this.isInitialized()) {
      this.$destroyed = true;
      return Promise["try"](cleanup);
    }

    var cmds = [];
    this.getCommandGroup().visit(function (cmd) {
      if (cmd instanceof Command) {
        cmds.push(cmd);
      }
    });
    this.$destroyed = true;
    return Promise.all([Promise["try"](function () {
      return _this6.doDestroy(params);
    }), Promise.all(cmds.map(function (cmd) {
      return Promise["try"](function () {
        return cmd.setEnabled(false);
      });
    }))]).then(function () {
      if (!_this6.$quiet) {
        _this6.trigger(events.DESTROY_EVENT);
      }

      _this6.emit('destroyed');
    }, function (err) {
      err = err || new Error(unknownErr);

      _this6.trigger(events.DESTROY_FAIL_EVENT, err);

      throw err;
    })["finally"](cleanup);
  };
  /**
   * Called by `destroy` so this widget has a chance to clean up after itself
   * and release any resources it is holding.
   * 
   * Notably, any jQuery event handlers registered on child elements of the
   * widget's DOM element should be unregistered here. Also, you may want to
   * remove any CSS classes you've added to the widget's DOM element.
   *  
   * @see module:bajaux/Widget#destroy
   * 
   * @param {Object} [params] Optional params object passed to `destroy()`
   * @returns {*|Promise} An optional promise that's resolved once the widget
   * has been destroyed.
   */


  Widget.prototype.doDestroy = function doDestroy(params) {};
  /**
   * Returns the jQuery DOM element in which this widget has been initialized.
   * If `initialize()` has not yet been called, then this will return `null`.
   *
   * @returns {JQuery|null} the DOM element in which this widget has been
   * initialized, or `null` if not yet initialized.
   */


  Widget.prototype.jq = function jq() {
    return this.$jq || null;
  };
  /**
   * Returns this widget's enabled state.
   *
   * @see module:bajaux/Widget#setEnabled
   *
   * @returns {Boolean}
   */


  Widget.prototype.isEnabled = function isEnabled() {
    return this.$enabled;
  };
  /**
   * Set this widget's enabled state.
   *
   * Setting of the internal flag will be synchronous, so `isEnabled` will
   * return the expected value immediately after calling this function. However,
   * the actual work of updating the DOM cannot be performed until after the
   * widget has finished initializing, so this method will return a promise.
   *
   * This method will not typically be overridden. `doEnabled()` should be
   * overridden instead.
   *
   * @see module:bajaux/Widget#isEnabled
   * 
   * @param {Boolean} enabled the new enabled state
   * @returns {Promise} A promise to resolve immediately if `initialize` has
   * not yet been called, that will resolve once the work of `initialize`
   * followed by `doEnabled` have both been completed. It will reject if
   * `initialize` or `doEnabled` fail.
   */


  Widget.prototype.setEnabled = function setEnabled(enabled) {
    var _this7 = this;

    enabled = !!enabled; // Set state change immediately! This is always set regardless of
    // any errors that may be thrown by any callbacks.

    this.$enabled = enabled; // Only trigger callbacks and events if the Widget has been initialized.

    var prom = this.$initialized().then(function () {
      var enabled = _this7.$enabled;

      _this7.jq().toggleClass(Widget.css.disabled, !enabled);

      return Promise["try"](function () {
        return _this7.doEnabled(enabled);
      }).then(function () {
        if (!_this7.$quiet) {
          _this7.trigger(enabled ? events.ENABLE_EVENT : events.DISABLE_EVENT);
        }

        return enabled;
      }, function (err) {
        err = err || new Error(unknownErr);

        _this7.trigger(enabled ? events.ENABLE_FAIL_EVENT : events.DISABLE_FAIL_EVENT, err);

        throw err;
      });
    }); // If the Widget isn't initialized yet then we don't know when it will be
    // resolved. Therefore, we need to resolve this right now.

    if (!this.isInitialized()) {
      return Promise.resolve(enabled);
    } else {
      return prom;
    }
  };
  /**
   * Called when the widget is enabled/disabled.
   *
   * @param {Boolean} enabled the new enabled state.
   * @returns {*|Promise} An optional Promise that can be returned if
   * the state change is asynchronous.
   */


  Widget.prototype.doEnabled = function doEnabled(enabled) {};
  /**
   * Trigger a widget event. By default, this fires a DOM event on the associated
   * widget's DOM element.
   */


  Widget.prototype.trigger = function trigger(name) {
    if (this.$jq) {
      var passedArgs = Array.prototype.slice.call(arguments, 1);
      var args = [this].concat(passedArgs);
      this.$jq.trigger(name, args);
    }
  };
  /**
   * Return true if the widget implements the specified MixIn.
   *
   * @param {String} mixin the name of the mixin to test for.
   * @returns {Boolean}
   */


  Widget.prototype.hasMixIn = function hasMixIn(mixin) {
    var mixins = this.$mixins;

    for (var i = 0; i < mixins.length; ++i) {
      if (mixins[i] === mixin) {
        return true;
      }
    }

    return false;
  };
  /**
   * Return the Properties for a widget.
   * 
   * @returns {module:bajaux/Properties} The Properties for a widget.
   */


  Widget.prototype.properties = function properties() {
    return this.$properties;
  };
  /**
   * Called whenever a Widget's Property is changed.
   *
   * If this Widget is not yet initialized, this is a no-op.
   * 
   * This function should not typically be overridden.
   * {@link module:bajaux/Widget#doChanged|doChanged()} should be overridden
   * instead.
   *
   * @see module:bajaux/Widget#properties
   * 
   * @param  {String} name The name of the Property that's changed.
   * @param  {*} value The new Property value.
   * @returns {Promise}
   */


  Widget.prototype.changed = function changed(name, value) {
    var _this8 = this;

    if (name === 'enabled') {
      return this.setEnabled(value).then(function () {
        return _this8.doChanged(name, value);
      });
    } else {
      return Promise.resolve(this.doChanged(name, value));
    }
  };
  /**
   * Called by {@link module:bajaux/Widget#changed|changed()} when a Property
   * is changed.
   *
   * This method is designed to be overridden by any subclasses.
   * 
   * @param  {String} name The name of the Property that's changed.
   * @param  {*} value The new Property value.
   * @returns {Promise|*}
   */


  Widget.prototype.doChanged = function doChanged(name, value) {};
  /**
   * Returns true if the Widget is in a graphic design editor.
   *
   * @returns {Boolean}
   */


  Widget.prototype.isDesignTime = function isDesignTime() {
    return this.$jq ? this.$jq.closest("." + Widget.css.designTime).length > 0 : false;
  };
  /**
   * Returns this widget's modified state. 
   *
   * @returns {Boolean}
   */


  Widget.prototype.isModified = function isModified() {
    return this.$modified;
  };
  /**
   * Sets this widget's modified or "dirty" status, to indicate that the user
   * has made changes to this widget that may need to be saved.
   *
   * The modification status will only be set if the widget is initialized
   * and the widget is not loading a new value.
   * 
   * Triggers `bajaux:modify` or `bajaux:unmodify` depending on the input value.
   * Any arguments passed to this function after the first will be passed
   * through to the triggered event.
   * 
   * This method should not typically be overridden. 
   * {@link module:bajaux/Widget#doModified|doModified()} should be overridden
   * instead.
   * 
   * @see module:bajaux/Widget#doModified
   * 
   * @param {Boolean|*} modified (a non-Boolean will be checked for truthiness)
   * 
   * @example
   *   <caption>Say I have collection of nested widgets in my DOM element.
   *   Whenever one of those widgets is modified, I want to mark myself
   *   modified but also provide the originally modified editor. For example,
   *   when a Property Sheet is modified, I want to know which row caused the
   *   modification.</caption>
   *     
   *   var that = this;
   *   dom.on(events.MODIFY_EVENT, function (e, modifiedEd) {
   *     that.setModified(true, modifiedEd);
   *     return false;
   *   });
   */


  Widget.prototype.setModified = function setModified(modified) {
    var args = Array.prototype.slice.call(arguments);

    if (this.isLoading() || !this.isInitialized()) {
      return;
    }

    modified = !!modified;
    this.$modified = modified;
    this.doModified(modified);
    args[0] = modified ? events.MODIFY_EVENT : events.UNMODIFY_EVENT;
    this.trigger.apply(this, args);
  };
  /**
   * The actual implementation for `setModified`. This function
   * should do any work necessary when the widget is set to a modified or 
   * "dirty" state - typically enabling a save button, arming a
   * `window.onbeforeunload` handler, etc. Likewise, it should do
   * the opposite when setting modified to false.
   * 
   * Note that this is *synchronous*, as is `setModified`. Async work can be
   * performed, but `setModified` will not wait for it.
   * 
   * @see module:bajaux/Widget#setModified
   * 
   * @param {Boolean} modified
   */


  Widget.prototype.doModified = function doModified(modified) {};
  /**
   * Resolve a value from some data. Please note, this will not load the value
   * but will resolve some data that could then be loaded by the widget.
   *
   * By default, this will treat the data as an ORD so it can be resolved via
   * BajaScript.
   *
   * @param {*|String|baja.Ord} data Specifies some data used to resolve a
   * load value so `load(value)` can be called on the widget.
   * @param {Object} [resolveParams] An Object Literal used for ORD resolution.
   * This parameter is designed to be used internally by bajaux and
   * shouldn't be used by developers.
   * @returns {Promise} a promise to be resolved with the value resolved from
   * the given data object
   */


  Widget.prototype.resolve = function resolve(data, resolveParams) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      resolveParams = resolveParams || {}; // Lazily require BajaScript so we don't create a direct dependency on it
      // since this method can be overridden.

      require(["baja!"], function (baja) {
        var ordUtil = require('bajaScript/baja/ord/ordUtil');

        var quickResolvePromise = ordUtil.quickResolve(data, resolveParams);

        if (quickResolvePromise) {
          return quickResolvePromise.then(function (ordTarget) {
            resolve(ordTarget.getObject());
          });
        }

        resolveParams.ok = resolveParams.ok || resolve;
        resolveParams.fail = resolveParams.fail || reject; // Resolve the ORD
        //TODO: if resolveParams.ok is truthy, Widget#resolve() will never actually resolve its promise

        baja.Ord.make(String(data)).get(resolveParams);
      });
    });
  };
  /**
   * Updates the widget's HTML with the given value. An widget for editing a
   * string, for example, might load the string into a text input. A view for
   * editing a `DynamicEnum` might programmatically set a `<select>`
   * dropdown's value.
   * 
   * `load()` may not be called until `initialize()` has completed its work.
   * If `initialize()` is not finished, `load()` will reject.
   * 
   * After `load()` completes its work, the value loaded will be accessible
   * via `this.value()`.
   * 
   * `load()` delegates the work of loading the HTML values to `doLoad()`. 
   * Subclasses will typically not override `load`, but more commonly will
   * override `doLoad`.
   * 
   * After `load()` completes, a `bajaux:load` or `bajaux:loadfail` event will
   * be triggered, as appropriate.
   * 
   * While this method is performing its work, `this.isLoading()` will return 
   * `true`.
   *
   * @see module:bajaux/Widget#doLoad
   * @see module:bajaux/Widget#value
   * @see module:bajaux/mixin/batchLoadMixin
   * 
   * @param {*} value The value to be loaded
   * @param {Object} [params] additional parameters to be passed to `doLoad()`
   * @returns {Promise} A promise to be resolved with the loaded value after
   * the widget has been loaded, or rejected if the widget fails to load the
   * value.
   */


  Widget.prototype.load = function load(value, params) {
    var _this9 = this;

    var oldValue = this.$value;

    if (!this.isInitialized()) {
      //TODO: trigger LOAD_FAIL?
      return Promise.reject(new Error(notInitialized));
    }

    this.$value = value;
    this.$loading = true;
    return Promise["try"](function () {
      return _this9.doLoad(value, params);
    }).then(function () {
      if (!_this9.$quiet) {
        _this9.trigger(events.LOAD_EVENT);
      }

      _this9.emit('loaded', value);

      return value;
    }, function (err) {
      err = err || new Error(unknownErr);
      _this9.$value = oldValue;

      _this9.trigger(events.LOAD_FAIL_EVENT, err);

      throw err;
    })["finally"](function () {
      _this9.$loading = false;
    });
  };
  /**
   * Loads in a new value, and sets the widget modified as well. Use this
   * convenience method when you wish to load in a new value as if a user had
   * done it, thereby triggering the necessary modify event handlers.
   *
   * @param {*} value
   * @param {*} [params]
   * @returns {Promise.<*>}
   * @since Niagara 4.12
   */


  Widget.prototype.loadAndModify = function (value, params) {
    var _this10 = this;

    return this.load(value, params).then(function (result) {
      _this10.setModified(true);

      return result;
    });
  };
  /**
   * Performs the actual work of populating the widget's HTML to reflect the
   * input value.
   * 
   * This function should be overridden by subclasses. The subclass function
   * should manipulate the DOM {@link module:bajaux/Widget#jq|jq()} and,
   * optionally,return a promise to indicate that the work of loading the value
   * has completed.
   * 
   * @see module:bajaux/Widget#load
   * @see module:bajaux/Widget#value
   * 
   * @param {*} value The value to be loaded.
   * @param {Object} [params] Optional params object passed to `load()`
   * @returns {Promise} An optional promise that's resolved once the widget has
   * loaded.
   */


  Widget.prototype.doLoad = function doLoad(value, params) {};
  /**
   * Check if this widget is currently in the process of loading. This will
   * return `true` immediately after `load` is called, and return `false`
   * after the `load` promise resolves.
   *
   * @returns {Boolean}
   */


  Widget.prototype.isLoading = function isLoading() {
    return this.$loading;
  };
  /**
   * Returns the widget's current loaded value. This the value that was last
   * loaded via `load()`. To read a widget's current representation, reflecting
   * any user-entered changes, call `read()`. If no value has been loaded yet,
   * `null` is returned.
   * 
   * @see module:bajaux/Widget#load
   * @see module:bajaux/Widget#doLoad
   * @see module:bajaux/Widget#read
   * 
   * @returns {*|null} the loaded value, or `null` if a value hasn't been
   * loaded yet.
   */


  Widget.prototype.value = function () {
    return this.$value;
  };
  /**
   * Saves any outstanding user-entered changes to this widget. Triggers a
   * `bajaux:save` or `bajaux:savefail` event, as appropriate.
   * 
   * In order to save the widget, its current value will be validated using
   * `validate()`, then the validated value will be passed to `doSave()`.
   * 
   * This method will not typically be overridden. 
   * {@link module:bajaux/Widget#doSave|doSave()} should be overridden instead.
   *
   * @see module:bajaux/Widget#doSave
   * @see module:bajaux/mixin/batchSaveMixin
   * 
   * @param {Object} [params] Additional parameters to be passed to `doSave()`
   * 
   * @returns {Promise} A promise to be resolved once the widget has been saved,
   * or rejected if the save fails.
   */


  Widget.prototype.save = function save(params) {
    var _this11 = this;

    if (!this.isInitialized()) {
      //TODO: trigger SAVE_FAIL
      return Promise.reject(new Error(notInitialized));
    }

    return Promise.resolve(this.validate()).then(function (validValue) {
      //TODO: always pass an object to avoid if (params) checks?
      return _this11.doSave(validValue, params);
    }).then(function () {
      // Now the widget is saved, unmodify the widget
      _this11.setModified(false);

      if (!_this11.$quiet) {
        _this11.trigger(events.SAVE_EVENT);
      } //squelch "promise not returned" warnings - MODIFY_EVENT/SAVE_EVENT
      //triggers may kick off promises


      return null;
    }, function (err) {
      err = err || new Error(unknownErr);

      _this11.trigger(events.SAVE_FAIL_EVENT, err);

      throw err;
    });
  };
  /**
   * Performs the actual work of saving the widget. This function should
   * be overridden by subclasses to save the value. The subclass function
   * should save the value and then, optionally, return a promise to indicate
   * that the work of saving the widget has completed.
   *
   * @param {*} validValue The value to be used for saving. This value will have
   * been read from the widget using `read()` and validated using `validate()`.
   * @param {Object} [params] Optional params object passed to `save()`
   * @returns {*|Promise} An optional promise that's resolved once
   * the widget has saved.
   */


  Widget.prototype.doSave = function doSave(validValue, params) {};
  /**
   * Read the current representation of the widget. For instance, if the widget
   * is made up from two text input boxes, this might resolve an object with
   * two strings from those text boxes.
   * 
   * Note the word "representation" - this function does not necessarily
   * return the widget's actual value, but might assemble a different
   * object, or array, or number, based on current user-entered values.
   * 
   * `read` will not typically be overridden.
   * {@link module:bajaux/Widget#doRead|doRead()} should be overridden instead.
   *
   * @see module:bajaux/Widget#doRead
   * 
   * @returns {Promise} A promise that will be resolved with a value read from
   * the widget as specified by `doRead`, or rejected if the read fails.
   */


  Widget.prototype.read = function read() {
    var _this12 = this;

    if (!this.isInitialized()) {
      return Promise.reject(new Error(notInitialized));
    }

    return Promise["try"](function () {
      return _this12.doRead();
    });
  };
  /**
   * Does the work of reading the widget's current representation.
   * 
   * This might mean reading a series of text inputs and assembling their
   * values into an array. It might mean instantiating a copy of the backing
   * `baja.Component` and setting slot values on the new copy.
   * It might mean simply returning the boolean value of a checkbox. If your
   * widget is composed of pure text/HTML and is not actually backed by an
   * external value, it might mean returning <i>nothing</i>.
   * 
   * When saving a modified widget, the output of this function will be passed
   * directly into this widget's validation process, so all your validation
   * steps should be expecting to receive this. It will also be passed to
   * `doSave`, so your `doSave` implementation should also expect this value.
   * 
   * The default behavior of `doRead` is simply to use the widget's current
   * value.
   *
   * @see module:bajaux/Widget#read
   * @returns {*|Promise} The read value, or a promise to be resolved with the
   * read value
   */


  Widget.prototype.doRead = function doRead() {
    return this.value();
  };
  /**
   * Return the widget's Validators.
   *
   * @see module:bajaux/Validators
   * 
   * @return {module:bajaux/Validators}
   */


  Widget.prototype.validators = function validators() {
    return this.$validators;
  };
  /**
   * Read the current value from the widget and validate it.
   *
   * @see module:bajaux/Validators#validate
   * 
   * @return {Promise} A promise to be resolved with the value read from the
   * widget and passed through all validators, or rejected if the value could
   * not be read or validated.
   */


  Widget.prototype.validate = function validate() {
    var _this13 = this;

    return this.read().then(function (value) {
      return _this13.validators().validate(value);
    });
  };
  /**
   * Returns this widget's readonly state.
   *
   * @see module:bajaux/Widget#setReadonly
   *
   * @returns {Boolean}
   */


  Widget.prototype.isReadonly = function isReadonly() {
    return this.$readonly;
  };
  /**
   * Set this widget's readonly state.
   * 
   * Setting of the internal flag will be synchronous, so `isReadonly` will
   * return the expected value immediately after calling this function. However,
   * the actual work of updating the DOM cannot be performed until after the
   * widget has finished initializing, so this method will return a promise.
   * 
   * This method will not typically be overridden. `doReadonly()` should be
   * overridden instead.
   *
   * @see module:bajaux/Widget#isReadonly
   * 
   * @param {Boolean} readonly the new readonly state.
   * @returns {Promise} A promise to resolve immediately if `initialize` has
   * not yet been called, that will resolve once the work of `initialize`
   * followed by `doReadonly` have both been completed. It will reject if
   * `initialize` or `doReadonly` fail.
   */


  Widget.prototype.setReadonly = function setReadonly(readonly) {
    var _this14 = this;

    readonly = !!readonly; // Set state change immediately! This is always set regardless of
    // any errors that may be thrown by any callbacks.

    this.$readonly = readonly; // Only trigger callbacks and events if the widget has been initialized.

    var prom = this.$initialized().then(function () {
      var readonly = _this14.$readonly;

      _this14.jq().toggleClass(Widget.css.readonly, readonly);

      return Promise["try"](function () {
        return _this14.doReadonly(readonly);
      }).then(function () {
        if (!_this14.$quiet) {
          _this14.trigger(readonly ? events.READONLY_EVENT : events.WRITABLE_EVENT);
        }

        return readonly;
      }, function (err) {
        err = err || new Error(unknownErr);

        _this14.trigger(readonly ? events.READONLY_FAIL_EVENT : events.WRITABLE_FAIL_EVENT, err);

        throw err;
      });
    }); // If the widget isn't initialized yet then we don't know when it will be
    // resolved. Therefore, we need to resolve this right now.

    if (!this.isInitialized()) {
      return Promise.resolve(readonly);
    } else {
      return prom;
    }
  };
  /**
   * Called when the widget is set to readonly or made writable.
   *
   * @param {Boolean} readonly the new readonly state.
   * @returns {*|Promise} An optional Promise that can be returned if
   * the state change is asynchronous.
   */


  Widget.prototype.doReadonly = function doReadonly(readonly) {};
  /**
   * Returns an array of child widgets living inside this editor's DOM.
   * This method will specifically *not* return child widgets of child widgets
   * - for instance, if this widget has one child editor for a `baja.Facets`,
   * you will only get a single widget back - it won't recurse down and give
   * you all the tag editors, type editors etc.
   *
   * This is safer and easier than using `$.find()`, which recurses down,
   * or carefully managing strings of `$.children()` calls.
   *
   * Pass in a `jQuery` instance to limit the child editor search to
   * a particular set of elements. Otherwise, will search all child elements
   * of this editor's DOM.
   *
   * If this editor has not initialized yet, you'll just get an empty array
   * back.
   *
   * The returned array will have some utility functions attached that return
   * promises. See example for details.
   *
   * @param {Object|jQuery} [params]
   * @param {JQuery} [params.dom=this.jq().children()] the dom element to search
   * @param {Function} [params.type] the widget type to search for - pass in the
   * actual constructor, for `instanceof` checks
   * @returns {Array.<module:bajaux/Widget>} an array of child widgets
   * @example
   *
   *   var kids = ed.getChildWidgets();
   *   kids.setAllEnabled(false).then(function () {});
   *   kids.setAllModified(false).then(function () {});
   *   kids.setAllReadonly(false).then(function () {});
   *   kids.readAll().then(function (valuesArray) {});
   *   kids.validateAll().then(function (valuesArray) {});
   *   kids.saveAll().then(function () {});
   *   kids.destroyAll().then(function () {});
   *
   * @example
   *
   *   var stringEditors = ed.getChildWidgets({ type: StringEditor });
   *
   * @since Niagara 4.10
   */


  Widget.prototype.getChildWidgets = function (params) {
    params = !params ? {} : params instanceof $ ? {
      dom: params
    } : params;
    var dom = params.dom || this.jq() && this.jq().children();
    return getChildWidgets(dom, params);
  };
  /**
   * Attempts to place the cursor focus on this editor. For instance, if
   * showing a simple string editor in a dialog, it should request focus so
   * that the user can simply begin typing without having to move the mouse
   * over to it and click.
   *
   * Override this as necessary; by default, will place focus on the first
   * `input` or `textarea` element in this editor's element.
   *
   * @since Niagara 4.10
   */


  Widget.prototype.requestFocus = function () {
    if (!this.isReadonly()) {
      $('input:not([readonly]):not(.nofocus), textarea:not([readonly]):not(.nofocus)', this.jq()).first().focus();
    }
  };

  function getChildWidgets(dom, params) {
    var type = params.type,
        filter = params.filter; //this looks overcomplicated but it's actually a fast way of doing it,
    //and it's tough to do safely with pure jQuery selectors.

    function edsOf(dom) {
      return dom.map(function () {
        var $dom = $(this);

        var typeofFilter = _typeof(filter);

        var widget = (typeofFilter !== 'string' || $dom.is(filter)) && Widget["in"]($dom);

        if (widget) {
          if (type) {
            return widget instanceof type ? widget : [];
          }

          if (typeofFilter === 'function') {
            return filter(widget) ? widget : [];
          }

          return widget;
        } else {
          return edsOf($dom.children());
        }
      }).get();
    }

    var eds = dom ? edsOf(dom) : [];
    addUtilityFunctions(eds);
    return eds;
  }

  function callOnAll(arr, functionName, args) {
    return Promise.all(arr.map(function (ed) {
      return ed[functionName].apply(ed, args);
    }));
  }

  function mapWhen(fName) {
    return function () {
      var args = Array.prototype.slice.call(arguments);
      return callOnAll(this, fName, args);
    };
  }

  var utilityFunctions = {
    readAll: mapWhen('read'),
    saveAll: mapWhen('save'),
    validateAll: mapWhen('validate'),
    setAllEnabled: mapWhen('setEnabled'),
    setAllModified: mapWhen('setModified'),
    setAllReadonly: mapWhen('setReadonly'),
    destroyAll: mapWhen('destroy'),
    layoutAll: mapWhen('layout')
  };

  function addUtilityFunctions(editorsArray) {
    Object.keys(utilityFunctions).forEach(function (key) {
      editorsArray[key] = utilityFunctions[key];
    });
  }

  function getRootCssClass(widget) {
    var props = widget.properties();
    return props.getValue('rootCssClass') || props.getValue(ROOT_CSS_CLASS_SYMBOL) || '';
  }

  function getEnabledFromParams(params) {
    var properties = params.properties,
        enabledProperty = properties instanceof Properties ? properties.getValue("enabled") : null;

    if (enabledProperty !== null) {
      return enabledProperty !== false;
    } else if (properties && properties.enabled !== undefined) {
      return properties.enabled !== false;
    }

    return params.enabled !== false;
  }

  function processParams(paramsObj) {
    if (!paramsObj) {
      return {};
    }

    var objects = [];

    while (paramsObj) {
      var _paramsObj = paramsObj,
          params = _paramsObj.params,
          defaults = _paramsObj.defaults;

      if (defaults) {
        replaceDefaultRootCssClass(defaults.properties);
      }

      if (paramsObj) {
        objects.push(paramsObj);
      }

      if (defaults) {
        objects.push(defaults);
      }

      paramsObj = params;
    }

    return Widget.$extendParams.apply(Widget, objects);
  }
  /**
   * If rootCssClass is specified using `defaults`, replace it with a Symbol to make it invisible
   * to the Px Editor.
   * @param {object} properties default properties object
   */


  function replaceDefaultRootCssClass(properties) {
    if (!properties) {
      return;
    }

    var rootCssClass = properties.rootCssClass;

    if (rootCssClass) {
      delete properties.rootCssClass;
      properties[ROOT_CSS_CLASS_SYMBOL] = rootCssClass;
    }
  }

  return Widget;
});
