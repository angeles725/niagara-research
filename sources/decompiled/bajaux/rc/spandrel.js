/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */

/**
 * API Status: **Development**
 * @module bajaux/spandrel
 */
define(['log!bajaux.spandrel', 'bajaux/lifecycle/WidgetManager', 'Promise', 'underscore', 'nmodule/js/rc/asyncUtils/asyncUtils', 'bajaux/spandrel/buildConfig', 'bajaux/spandrel/DynamicSpandrelWidget', 'bajaux/spandrel/jsx', 'bajaux/spandrel/logging', 'bajaux/spandrel/SpandrelWidget', 'bajaux/spandrel/symbols'], function (log, WidgetManager, Promise, _, asyncUtils, buildConfig, DynamicSpandrelWidget, jsx, logging, SpandrelWidget, symbols) {
  'use strict';

  var doRequire = asyncUtils.doRequire;
  var defaultManager = new WidgetManager();
  var IS_ELEMENT_SYMBOL = symbols.IS_ELEMENT_SYMBOL;
  /**
   * The purpose of `spandrel` is to provide a reasonably pure-functional,
   * diffable method of defining a nested structure of bajaux Widgets and
   * supporting HTML. Rather than require Widget implementors to manually code
   * calls to `initialize()` or `buildFor()`, `spandrel` allows you to provide
   * your desired structure of HTML elements and their associated Widget
   * instances, and handle the work of updating the document as that structure
   * may change over time.
   *
   * See {@tutorial 50-spandrel} for in-depth information.
   *
   * @alias module:bajaux/spandrel
   * @param {module:bajaux/spandrel~SpandrelData|function(*, module:bajaux/spandrel~WidgetState): module:bajaux/spandrel~SpandrelData} arg
   * @param {object} [params={}] params
   * @param {function(new:module:bajaux/Widget)} [params.extends] optionally specify a Widget superclass to extend
   * @param {string} [params.strategy] optionally specify a known lookup strategy
   * for dynamically building widgets. Currently, the only accepted value is
   * `niagara`, which will instruct `spandrel` to use the Niagara registry to
   * perform widget lookups (introducing a dependency on the `webEditors`
   * module). If included, it overrides the `manager` parameter.
   * @param {module:bajaux/lifecycle/WidgetManager} [params.manager] optionally provide your own WidgetManager to manage Widget lifecycle
   * @returns {Function} a Widget constructor
   * @since Niagara 4.10
   *
   * @example
   * <caption>Generate a static widget</caption>
   * const StaticWidget = spandrel([
   *   '<label>Name: </label>',
   *   '<input type="text" value="{{ props.name }}">',
   *   {
   *     dom: '<span></span>',
   *     value: false,
   *     properties: 'inherit'
   *   }
   * ]);
   * return fe.buildFor({
   *   dom: $('#myStaticWidget'),
   *   type: StaticWidget,
   *   properties: { name: 'Logan', trueText: 'Good', falseText: 'Not So Good' }
   * });
   *
   * @example
   * <caption>Generate a dynamic widget with a field editor for each slot</caption>
   * const DynamicWidget = spandrel(comp => comp.getSlots().toArray().map(slot => ({
   *   dom: '<div class="componentSlot"/>',
   *   kids: [
   *     `<label>${ slot.getName() }: </label>`,
   *     { dom: '<span/>', complex: comp, slot: slot }
   *   ]
   * })));
   *
   * return fe.buildFor({
   *   dom: $('#myDynamicWidget'),
   *   type: DynamicWidget,
   *   value: myComponent
   * });
   *
   * @example
   * <caption>Subclass an existing dynamic spandrel widget, making changes
   * before rendering.</caption>
   *
   * // our superclass will render a <label> element, with a background
   * // determined by a widget property.
   * const LabelWidget = spandrel((value, { properties }) => {
   *   const label = document.createElement('label');
   *   label.innerText = value;
   *   label.style.background = properties.background || '';
   *   return label;
   * });
   *
   * const RedLabelWidget = spandrel((value, { renderSuper }) => {
   *
   *   // renderSuper will call back to the superclass, allowing your subclass
   *   // to edit the data before spandrel renders it to the page.
   *   //
   *   // you can optionally pass a function to renderSuper that will tweak the
   *   // widget state before the superclass renders its data. if no tweaking is
   *   // desired, just renderSuper() is fine.
   *   //
   *   return renderSuper((state) => {
   *     state.properties.background = 'lightpink';
   *
   *     // remember to return the new state.
   *     return state;
   *   })
   *     .then((label) => {
   *       // renderSuper will resolve the data exactly as rendered by the
   *       // superclass.
   *       label.style.color = 'red';
   *       return label;
   *     });
   * }, { extends: LabelWidget });
   */

  function spandrel(arg, params) {
    if (typeof arg === 'function') {
      //dynamically redefine the nested widget structure based on whatever
      //value is being loaded.
      return makeDynamic(arg, params);
    } else {
      // define a static nested widget structure.
      return makeStatic(arg, params);
    }
  }
  /**
   * Given spandrel input (potentially dynamically generated), spit out a build
   * context, where each member may potentially contain more nested data, that
   * will map to one or more fe.buildFor calls.
   *
   * @private
   * @param {module:bajaux/spandrel~SpandrelData|function(*, module:bajaux/spandrel~WidgetState): module:bajaux/spandrel~SpandrelData} arg
   * @param {module:bajaux/spandrel~WidgetState} widgetState configuration data derived
   * from the parent widget to contain all these spandrel-generated widgets
   * @returns {Promise.<module:bajaux/spandrel~BuildContext>}
   */


  spandrel.build = function (arg, widgetState) {
    return buildConfig(arg, widgetState, this);
  };
  /**
   * Use `spandrel.jsx` as your JSX pragma to convert your JSX into spandrel
   * config.
   *
   * @see module:bajaux/spandrel/jsx
   */


  spandrel.jsx = jsx.jsxToSpandrel;
  /**
   * @private
   * @param {module:bajaux/lifecycle/WidgetManager} manager
   */

  spandrel.$installDefaultWidgetManager = function (manager) {
    defaultManager = manager;
  };
  /**
   * @private
   * @returns {module:bajaux/lifecycle/WidgetManager}
   */


  spandrel.$getDefaultWidgetManager = function () {
    return defaultManager;
  };
  /**
   * @private
   */


  spandrel.$trace = function () {
    logging.$trace();
  };
  /**
   * @private
   * @type {Object.<string, Function>}
   */


  spandrel.$KNOWN_MANAGERS = {
    // avoid JsBuild verification failure
    niagara: _.once(function () {
      return doRequire('nmodule'.toLowerCase() + '/webEditors/rc/fe/fe').then(function (fe) {
        return fe.getWidgetManager();
      });
    })
  };
  /**
   * @param {function(*): module:bajaux/spandrel~SpandrelArg} spandrelFunction
   * @param {Object} [params]
   * @param {Function} [params.extends]
   * @param {module:bajaux/lifecycle/WidgetManager} [params.manager]
   * @returns {function(new:module:bajaux/spandrel/SpandrelWidget)}
   */

  function makeDynamic(spandrelFunction) {
    var _ref = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
        Super = _ref["extends"],
        strategy = _ref.strategy,
        manager = _ref.manager;

    return DynamicSpandrelWidget.make({
      "extends": Super,
      manager: manager,
      spandrelFunction: spandrelFunction,
      strategy: strategy
    });
  }

  function makeStatic(spandrelArg) {
    var _ref2 = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {},
        _ref2$manager = _ref2.manager,
        manager = _ref2$manager === void 0 ? defaultManager : _ref2$manager,
        isElement = _ref2[IS_ELEMENT_SYMBOL];

    return SpandrelWidget.make({
      isElement: isElement,
      manager: manager,
      spandrelArg: spandrelArg
    });
  }

  return spandrel;
});
