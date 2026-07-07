function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

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

function _defineEnumerableProperties(obj, descs) { for (var key in descs) { var desc = descs[key]; desc.configurable = desc.enumerable = true; if ("value" in desc) desc.writable = true; Object.defineProperty(obj, key, desc); } if (Object.getOwnPropertySymbols) { var objectSymbols = Object.getOwnPropertySymbols(descs); for (var i = 0; i < objectSymbols.length; i++) { var sym = objectSymbols[i]; var desc = descs[sym]; desc.configurable = desc.enumerable = true; if ("value" in desc) desc.writable = true; Object.defineProperty(obj, sym, desc); } } return obj; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

/**
 * @copyright 2020 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */
define(['bajaux/events', 'bajaux/spandrel/symbols', 'bajaux/spandrel/util', 'Promise', 'underscore'], function (events, symbols, util, Promise, _) {
  'use strict';

  var flatten = _.flatten,
      isFunction = _.isFunction,
      once = _.once;
  var IS_SPANDREL_SYMBOL = symbols.IS_SPANDREL_SYMBOL,
      KEY_SYMBOL = symbols.KEY_SYMBOL,
      JSX_TYPE_SYMBOL = symbols.JSX_TYPE_SYMBOL,
      JSX_PROPS_SYMBOL = symbols.JSX_PROPS_SYMBOL,
      JSX_KIDS_SYMBOL = symbols.JSX_KIDS_SYMBOL,
      REIFY_SYMBOL = symbols.REIFY_SYMBOL;
  var isDynamic = util.isDynamic,
      reify = util.reify;
  var JSX_NODE_SYMBOL = Symbol('jsxNode');
  /*
  a namespaces map will tag along as part of the "props" attribute of each jsx node. this keeps
  track of what namespaces are known by each JSX element so the corresponding *NS method can be
  called.
   */

  var NAMESPACES_SYMBOL = Symbol('namespaces');
  var ELEMENT_NAMESPACE_SYMBOL = Symbol('elementNamespace');
  var BAJAUX_NOT_DOM_ATTRIBUTES = {
    bind: true,
    bindKey: true,
    complex: true,
    enabled: true,
    formFactor: true,
    lax: true,
    properties: true,
    readonly: true,
    slot: true,
    stateBinding: true,
    tagName: true,
    validate: true,
    value: true
  };
  var BAJAUX_NOT_DOM_ATTRIBUTE_NAMES = Object.keys(BAJAUX_NOT_DOM_ATTRIBUTES);

  var isQuieted = function () {
    var quiet = {};
    ['INITIALIZE_EVENT', 'LOAD_EVENT', 'SAVE_EVENT', 'ENABLE_EVENT', 'DISABLE_EVENT', 'READONLY_EVENT', 'WRITABLE_EVENT', 'LAYOUT_EVENT', 'DESTROY_EVENT'].forEach(function (eventKey) {
      quiet[events[eventKey]] = true;
    });
    return function (eventName) {
      return quiet[eventName];
    };
  }();
  /**
   * API Status: **Development**
   * @exports bajaux/spandrel/jsx
   */


  var exports = {};
  /**
   * Return JSX instructions for creating one node (DOM element of Widget) in a tree of spandrel
   * data.
   *
   * These represent _instructions_ only. The spandrel data will not be created until it is
   * "reified" by calling `.then()` on the object returned by this function (or passing it to
   * `Promise.resolve()` etc.). spandrel itself will perform this reification as part of the
   * process of building out the widget.
   *
   * @param {string|Function} type HTML tag name, or a Widget constructor to instantiate
   * @param {object|null} [props]
   * @param {...module:bajaux/spandrel/jsx~JsxInstructions} kids
   * @returns {module:bajaux/spandrel/jsx~JsxInstructions}
   *
   * @example
   * <caption>Basic JSX->spandrel example</caption>
   * &#37;** @jsx spandrel.jsx *&#37;
   * class ComponentToHTML extends spandrel((comp) => {
   *   return (
   *     <table>
   *     {
   *       comp.getSlots().properties().toArray().map((prop) => {
   *         return <tr>
   *           <td>{ prop.getName() }</td>
   *           <td>{ prop.getType() }</td>
   *         </tr>;
   *       })
   *     }
   *     </table>
   *   );
   * }) {}
   *
   * @example
   * <caption>Continued configuration after creation</caption>
   *
   * // these two widgets are equivalent.
   *
   * spandrel((string) => <label className="hello">{ string }</label>);
   * spandrel((string) => {
   *   const label = <label>{string}</label>;
   *   label.className = 'hello';
   *   return label;
   * });
   *
   * // at the moment, spandrel JSX nodes are *write only* from your javascript code. this means you
   * // cannot do this:
   *
   * spandrel((string) => {
   *   const label = <label className="foo">{string}</label>;
   *   label.className += ' bar'; // can't read it!
   *   return label;
   * });
   */

  exports.jsxToSpandrel = function (type, props) {
    for (var _len = arguments.length, kids = new Array(_len > 2 ? _len - 2 : 0), _key = 2; _key < _len; _key++) {
      kids[_key - 2] = arguments[_key];
    }

    return toJsxInstructions(type, props || {}, flatten(kids));
  };
  /**
   * Looks for event handlers on the properties of a JSX node, such as `onUxModify`, and normalizes
   * them into an `on` array as expected to be a member of `spandrel` data. This will **mutate** the
   * input properties object: `onUx*` members will be *removed* and placed in the resulting array.
   * If any of the events require unquieting (e.g. `bajaux:load`) in order to trigger event
   * handlers, the `$quiet` property will be set to false in `props.properties`.
   *
   * @private
   * @param {object} props the properties of a JSX node
   * @returns {Array} an array of event handlers as expected to be a member of
   * {@link module:bajaux/spandrel~WidgetDefinition}
   */


  exports.$normalizeJsxEventHandlers = function (props) {
    return normalizeEventHandlers(props);
  };
  /**
   * Applies DOM attributes like style and className from the JSX instructions to the given DOM
   * element. `className` and `style` will be merged together; atomic attributes like "readonly"
   * will be overwritten.
   *
   * @private
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsx
   * @param {Element|JQuery} dom
   */


  exports.$applyJsxToDom = function (jsx, dom) {
    applyJsxToDom(jsx, dom);
  };
  /**
   * When defining a `spandrelSrc` in JSX, the implementor will often want to apply additional
   * configuration in the JSX that would override the generated spandrel data.
   *
   * Given actual spandrel build params (enabled/readonly/properties/formFactor/etc) as generated by
   * the spandrelSrc, this will take the configuration declared in the JSX and apply it to those
   * build params. `properties` will be merged; all other properties will simply overwrite if
   * present.
   *
   * @private
   * @param {module:bajaux/spandrel~SpandrelBuildParams} buildParams
   * @param {object} jsxProps
   * @returns {object}
   * @example
   * <caption>
   *   The "visible" property of the BorderPane's content widget will always be set to true,
   *   regardless of what the UxModel says. All other properties generated by the UxModel will still
   *   be respected.
   * </caption>
   * return (
   *   <BorderPane>
   *     <widget name="content" src={uxModel.get('content')} properties={{ visible: true }} />
   *   </BorderPane>
   * );
   */


  exports.$clobber = function (buildParams, jsxProps) {
    var newBuildParams = Object.assign({}, buildParams);
    BAJAUX_NOT_DOM_ATTRIBUTE_NAMES.forEach(function (bajauxAttributeName) {
      if (bajauxAttributeName in jsxProps) {
        var bajauxAttributeValue = jsxProps[bajauxAttributeName];

        if (bajauxAttributeName === 'properties') {
          var properties = Object.assign({}, newBuildParams[bajauxAttributeName]);
          newBuildParams[bajauxAttributeName] = Object.assign(properties, bajauxAttributeValue);
        } else {
          newBuildParams[bajauxAttributeName] = bajauxAttributeValue;
        }
      }
    });
    return newBuildParams;
  };
  /**
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsx
   * @returns {module:bajaux/spandrel~SpandrelData}
   */


  function jsxToSpandrel(jsx) {
    var _dom, _on, _on2, _properties, _config, _mutatorMap;

    var type = jsx[JSX_TYPE_SYMBOL],
        props = jsx[JSX_PROPS_SYMBOL],
        kids = jsx[JSX_KIDS_SYMBOL];
    var textContent = '';
    var widgetType;
    props = props || {};

    if (typeof type !== 'string') {
      widgetType = type;
    }

    if (!kids.length) {
      kids = undefined;
    } else {
      kids.forEach(function (kid, i) {
        if (!kid) {
          return;
        }

        if (typeof kid === 'string') {
          textContent += kid;
          kids = undefined;
        } else {
          kid.key = kid.key || String(i);
        }
      });
    }

    var _props = props,
        spandrelKey = _props.spandrelKey;
    var lazyReifier = makeDomReifier(jsx, textContent);
    var _props2 = props,
        bind = _props2.bind,
        bindKey = _props2.bindKey,
        complex = _props2.complex,
        enabled = _props2.enabled,
        formFactor = _props2.formFactor,
        lax = _props2.lax,
        readonly = _props2.readonly,
        slot = _props2.slot,
        validate = _props2.validate,
        value = _props2.value;

    if (typeof enabled === 'string') {
      enabled = enabled !== 'false';
    }

    if (typeof readonly === 'string') {
      readonly = readonly !== 'false';
    }

    if (typeof value === 'undefined' && isDynamic(type)) {
      // NiagaraWidgetManager specifically checks for undefined instead of falsy.
      // complex + slot + value: null will cause it to fail: "null is not compatible with this slot type."
      if (!(complex && slot)) {
        value = null;
      }
    }

    var on;
    var config = (_config = {}, _defineProperty(_config, IS_SPANDREL_SYMBOL, true), _defineProperty(_config, JSX_NODE_SYMBOL, jsx), _defineProperty(_config, KEY_SYMBOL, spandrelKey), _dom = "dom", _mutatorMap = {}, _mutatorMap[_dom] = _mutatorMap[_dom] || {}, _mutatorMap[_dom].get = function () {
      return lazyReifier.getDom();
    }, _defineProperty(_config, "enabled", enabled), _defineProperty(_config, "formFactor", formFactor), _defineProperty(_config, "kids", kids), _on = "on", _mutatorMap[_on] = _mutatorMap[_on] || {}, _mutatorMap[_on].get = function () {
      return on || lazyReifier.getOn();
    }, _on2 = "on", _mutatorMap[_on2] = _mutatorMap[_on2] || {}, _mutatorMap[_on2].set = function (o) {
      on = o;
    }, _properties = "properties", _mutatorMap[_properties] = _mutatorMap[_properties] || {}, _mutatorMap[_properties].get = function () {
      return lazyReifier.getProperties();
    }, _defineProperty(_config, "readonly", readonly), _defineProperty(_config, "type", widgetType), _defineProperty(_config, "validate", validate), _defineProperty(_config, "value", value), _defineEnumerableProperties(_config, _mutatorMap), _config); // TODO: there may be additional extra parameters that could be passed to
    // fe.buildFor than these. it doesn't feel safe to just do an
    // Object.assign() due to the possibility of collisions between DOM element
    // properties and fe.buildFor arguments. revisit if we need more.

    if (complex) {
      config.complex = complex;
    }

    if (slot) {
      config.slot = slot;
    }

    if (bind) {
      config.bind = bind;
    }

    if (bindKey) {
      config.bindKey = bindKey;
    }

    if (lax) {
      config.lax = true;
    }

    return config;
  }
  /**
   * JSX nodes are built from the bottom up. If the jsx reads:
   *
   * `<div><span></span></div>`
   *
   * then that inner span is constructed *before* the div is.
   *
   * But the construction of nodes is sometimes informed by their parents. If building an SVG:
   *
   * `<svg xmlns="http://www.w3.org/2000/svg"><circle/></svg>`
   *
   * then the construction of that `circle` *requires* knowledge of the namespace it inherits from
   * its parent `svg`. We *can't* correctly create it until we've got that information from the
   * parent node.
   *
   * `spandrel` addresses this by holding on to that tree of JSX nodes until it's actually
   * requested. The nodes sit in a data structure until a `spandrel` widget actually requests the
   * `dom` property to put a child widget into. At that moment, the tree will be "reified" into an
   * actual tree of Elements (e.g. HTMLElements and SVGElements) into which to build its structure.
   *
   * This also reifies the widget Properties and event handlers (because these are also constructed
   * with information from the JSX structure).
   *
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsx
   * @param {string} [textContent] if there is any text content to assign to the created Element
   * @returns {{ getDom: (function(): Element), getProperties(): object, getOn(): object[] }}
   */


  function makeDomReifier(jsx, textContent) {
    var type = jsx[JSX_TYPE_SYMBOL],
        props = jsx[JSX_PROPS_SYMBOL];
    var makeDom;
    props = props || {};
    var on;

    if (typeof type === 'string') {
      if (type === 'any') {
        makeDom = function makeDom() {
          return document.createElement(props.tagName || 'div');
        };
      } else {
        makeDom = function makeDom() {
          return createElement(type, jsx[NAMESPACES_SYMBOL]);
        };
      }
    } else {
      makeDom = function makeDom() {
        return document.createElement(props.tagName || 'div');
      };
    }
    /**
     * Before actually constructing the DOM element for this spandrel widget, perform reification:
     * that is, give spandrel.jsx calls higher up in the DOM hierarchy a chance to propagate their
     * namespaces down to descendant elements, so the correct (namespaced) element constructors are
     * called.
     */


    var reifyDom = once(function () {
      propagateNamespaces(jsx, jsx[NAMESPACES_SYMBOL]);
      var dom = makeDom();
      dom.textContent = textContent;
      on = normalizeEventHandlers(props);
      applyJsxToDom(jsx, dom);
      return dom;
    });
    return {
      getDom: function getDom() {
        return reifyDom();
      },
      getProperties: function getProperties() {
        // configured DOM event handlers can affect widget properties, so reify first.
        reifyDom();
        return props.properties;
      },
      getOn: function getOn() {
        reifyDom();
        return on;
      }
    };
  }

  function applyJsxToDom(jsx, dom) {
    dom = dom[0] || dom;
    var type = jsx[JSX_TYPE_SYMBOL],
        props = jsx[JSX_PROPS_SYMBOL];
    var isWidget = isFunction(type) || type === 'any';
    Object.keys(props).forEach(function (name) {
      var prop = props[name];
      setDomElementProp(dom, name, prop, jsx[NAMESPACES_SYMBOL], isWidget);
    });
  }

  function normalizeEventHandlers(props) {
    var on = props.on || [];

    if (Array.isArray(on)) {
      if (on[0] && !Array.isArray(on[0])) {
        on = [on];
      }
    } else if (on.constructor === Object) {
      on = Object.keys(on).map(function (eventName) {
        return [eventName, on[eventName]];
      });
    }

    var needsUnquiet;
    Object.keys(props).forEach(function (name) {
      var prop = props[name];

      if (name.startsWith('onUx') && prop) {
        var func = prop;
        var handler;
        var eventName;
        var eventSubstring = name.substring(4).toLowerCase();

        if (eventSubstring === 'modifiedvalue') {
          handler = function handler(e, ed) {
            var _this = this;

            return ed.read().then(function (newValue) {
              return func.call(_this, newValue, e, ed);
            });
          };

          eventName = 'bajaux:modify';
        } else {
          handler = func;
          eventName = 'bajaux:' + eventSubstring;

          if (isQuieted(eventName)) {
            needsUnquiet = true;
          }
        }

        on.push([eventName, handler]);
        delete props[name];
      } else if (name.startsWith('on') && prop) {
        var _eventName = name.substring(2).toLowerCase();

        if (_eventName) {
          on.push([_eventName, prop]);
          delete props[name];
        }
      }
    });

    if (needsUnquiet) {
      var properties = props.properties || (props.properties = {});
      properties.$quiet = false;
    }

    return on;
  }
  /**
   * @param {string} type
   * @param {object} namespaces
   * @returns {Element}
   */


  function createElement(type, namespaces) {
    if (namespaces) {
      var elementNamespace = namespaces[ELEMENT_NAMESPACE_SYMBOL];

      if (elementNamespace) {
        return document.createElementNS(elementNamespace, type);
      }
    }

    return document.createElement(type);
  }
  /**
   * @param {Element} el
   * @param {string} name
   * @param {string} value
   * @param {object} namespaces
   */


  function setDomElementAttribute(el, name, value, namespaces) {
    var _name$split = name.split(':'),
        _name$split2 = _slicedToArray(_name$split, 2),
        nsName = _name$split2[0],
        nsProp = _name$split2[1];

    var ns = nsProp && namespaces[nsName];

    if (ns) {
      el.setAttributeNS(ns, name, value);
    } else {
      el.setAttribute(name, value);
    }
  }

  function setDomElementProp(el, name, value, namespaces, isWidget) {
    if (isWidget && BAJAUX_NOT_DOM_ATTRIBUTES[name]) {
      return;
    }

    if (name === '$init') {
      return value(el);
    }

    if (name === 'spandrelKey' || name === 'spandrelSrc' || name === 'on') {
      return;
    }

    if (name === 'className' || name === 'class') {
      var _el$classList;

      return (_el$classList = el.classList).add.apply(_el$classList, _toConsumableArray(value.trim().split(/\s+/)));
    } // if needed, come back and add ability to parse a `style` attribute as string and merge it in.


    if (name === 'style' && _typeof(value) === 'object') {
      var style = el.style;
      Object.keys(value).forEach(function (prop) {
        var propValue = value[prop];

        if (propValue || typeof propValue === 'number') {
          style[prop] = propValue;
        }
      });
    } else if (typeof value === 'boolean') {
      if (value) {
        setDomElementAttribute(el, name, 'true', namespaces);
      }

      el[name] = value;
    } else {
      setDomElementAttribute(el, name, value, namespaces);
    }
  }
  /**
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsx
   * @param {object} namespaces
   */


  function propagateNamespaces(jsx) {
    var namespaces = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};
    namespaces = Object.assign({}, namespaces);
    var props = jsx[JSX_PROPS_SYMBOL],
        kids = jsx[JSX_KIDS_SYMBOL];

    if (props) {
      Object.keys(props).forEach(function (prop) {
        if (prop.startsWith('xmlns')) {
          var ns = props[prop];

          var _prop$split = prop.split(':'),
              _prop$split2 = _slicedToArray(_prop$split, 2),
              name = _prop$split2[1];

          if (name) {
            namespaces[name] = ns;
          } else {
            namespaces[ELEMENT_NAMESPACE_SYMBOL] = ns;
          }
        }
      });
    }

    jsx[NAMESPACES_SYMBOL] = namespaces;

    if (kids) {
      kids.forEach(function (kid) {
        return kid && kid[JSX_NODE_SYMBOL] && propagateNamespaces(kid[JSX_NODE_SYMBOL], namespaces);
      });
    }
  }
  /**
   * @param {string|Function} type
   * @param {object} props
   * @param {Array.<module:bajaux/spandrel/jsx~JsxInstructions|string>} kidInstructions
   * @returns {module:bajaux/spandrel/jsx~JsxInstructions}
   */


  function toJsxInstructions(type, props, kidInstructions) {
    var _instructions;

    var spandrelSrc = props.spandrelSrc;
    var instructions = (_instructions = {}, _defineProperty(_instructions, JSX_TYPE_SYMBOL, type), _defineProperty(_instructions, JSX_PROPS_SYMBOL, props), _defineProperty(_instructions, JSX_KIDS_SYMBOL, kidInstructions), _defineProperty(_instructions, NAMESPACES_SYMBOL, {}), _defineProperty(_instructions, IS_SPANDREL_SYMBOL, true), _instructions);

    if (spandrelSrc) {
      if (kidInstructions.length) {
        throw new Error('spandrelSrc cannot be combined with children');
      }

      if (typeof type !== 'string') {
        throw new Error('spandrelSrc can only be applied to a DOM element');
      }

      return spandrelSrcReifier(instructions);
    } else {
      return makeThenable(instructions, function () {
        return reifyJsxTree(instructions);
      });
    }
  }
  /**
   * @param {object} object
   * @param {function} func
   * @returns {Thenable} a Thenable that will run the given function exactly once and then resolve
   */


  function makeThenable(object, func) {
    var runOnce = once(func);
    Object.defineProperty(object, 'then', {
      enumerable: false,
      writable: true,
      value: function value(resolve, reject) {
        return Promise.resolve(runOnce()).then(resolve, reject);
      }
    });
    return object;
  }
  /**
   * This JSX node specified `spandrelSrc`, so reifying this node consists of giving that
   * `spandrelSrc` the opportunity to programmatically generate its contents.
   *
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsx
   * @returns {module:bajaux/spandrel/jsx~JsxInstructions}
   */


  function spandrelSrcReifier(jsx) {
    var props = jsx[JSX_PROPS_SYMBOL];
    var spandrelKey = props.spandrelKey,
        spandrelSrc = props.spandrelSrc;
    var lazyReifier = makeDomReifier(jsx, '');
    jsx[IS_SPANDREL_SYMBOL] = true;

    jsx[REIFY_SYMBOL] = function (ownerState) {
      return reify(spandrelSrc.toSpandrel({
        dom: lazyReifier.getDom()
      }), ownerState).then(function (sp) {
        if (_typeof(sp) === 'object' && !Array.isArray(sp)) {
          var spOn = normalizeEventHandlers(sp);
          sp = exports.$clobber(sp, props);

          if (spandrelKey) {
            sp[KEY_SYMBOL] = spandrelKey;
          } // mark it as spandrel data, and not a key->widget map


          sp[IS_SPANDREL_SYMBOL] = true;
          sp.on = spOn.concat(lazyReifier.getOn());
        }

        return sp;
      });
    };

    return jsx;
  }
  /**
   * @param {module:bajaux/spandrel/jsx~JsxInstructions} jsxInstructions
   * @returns {Promise.<module:bajaux/spandrel~SpandrelData>}
   */


  function reifyJsxTree(jsxInstructions) {
    if (!jsxInstructions || typeof jsxInstructions === 'string') {
      return Promise.resolve(jsxInstructions);
    }

    var props = jsxInstructions[JSX_PROPS_SYMBOL],
        kids = jsxInstructions[JSX_KIDS_SYMBOL];
    Object.keys(jsxInstructions).forEach(function (name) {
      props[name] = jsxInstructions[name];
    });
    return Promise.all(kids).then(function (reifiedKids) {
      jsxInstructions[JSX_KIDS_SYMBOL] = reifiedKids;
      return jsxToSpandrel(jsxInstructions);
    });
  }

  return exports;
});
