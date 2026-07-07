function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function"); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, writable: true, configurable: true } }); if (superClass) _setPrototypeOf(subClass, superClass); }

function _setPrototypeOf(o, p) { _setPrototypeOf = Object.setPrototypeOf || function _setPrototypeOf(o, p) { o.__proto__ = p; return o; }; return _setPrototypeOf(o, p); }

function _createSuper(Derived) { var hasNativeReflectConstruct = _isNativeReflectConstruct(); return function _createSuperInternal() { var Super = _getPrototypeOf(Derived), result; if (hasNativeReflectConstruct) { var NewTarget = _getPrototypeOf(this).constructor; result = Reflect.construct(Super, arguments, NewTarget); } else { result = Super.apply(this, arguments); } return _possibleConstructorReturn(this, result); }; }

function _possibleConstructorReturn(self, call) { if (call && (_typeof(call) === "object" || typeof call === "function")) { return call; } return _assertThisInitialized(self); }

function _assertThisInitialized(self) { if (self === void 0) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return self; }

function _isNativeReflectConstruct() { if (typeof Reflect === "undefined" || !Reflect.construct) return false; if (Reflect.construct.sham) return false; if (typeof Proxy === "function") return true; try { Date.prototype.toString.call(Reflect.construct(Date, [], function () {})); return true; } catch (e) { return false; } }

function _getPrototypeOf(o) { _getPrototypeOf = Object.setPrototypeOf ? Object.getPrototypeOf : function _getPrototypeOf(o) { return o.__proto__ || Object.getPrototypeOf(o); }; return _getPrototypeOf(o); }

/**
 * @copyright 2020 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */
define(['bajaux/Properties', 'bajaux/Widget', 'bajaux/spandrel/symbols', 'jquery', 'Promise', 'underscore'], function (Properties, Widget, symbols, $, Promise, _) {
  'use strict';

  var IS_DYNAMIC_SYMBOL = symbols.IS_DYNAMIC_SYMBOL,
      IS_ELEMENT_SYMBOL = symbols.IS_ELEMENT_SYMBOL,
      REIFY_SYMBOL = symbols.REIFY_SYMBOL,
      ROOT_SYMBOL = symbols.ROOT_SYMBOL,
      TO_VALIDATE_SYMBOL = symbols.TO_VALIDATE_SYMBOL,
      WIDGET_SYMBOL = symbols.WIDGET_SYMBOL;
  var difference = _.difference,
      find = _.find,
      isFunction = _.isFunction,
      once = _.once;
  var BOOLEAN_ATTRIBUTES = ['checked', 'disabled', 'draggable', 'indeterminate', 'multiple', 'readonly', 'selected']; // avoid circular dependencies

  var getDynamicSpandrelWidget = once(function () {
    return require('bajaux/spandrel/DynamicSpandrelWidget');
  });

  var DummyWidget = /*#__PURE__*/function (_Widget) {
    _inherits(DummyWidget, _Widget);

    var _super = _createSuper(DummyWidget);

    function DummyWidget() {
      _classCallCheck(this, DummyWidget);

      return _super.apply(this, arguments);
    }

    return DummyWidget;
  }(Widget);
  /**
   * @private
   * @exports bajaux/spandrel/util
   */


  var exports = {};
  /**
   * @param {string} kebabCase a-string-in-kebab-case
   * @returns {string} aStringInCamelCase
   */

  exports.camelCase = function (kebabCase) {
    return kebabCase.replace(/^-*/, '').replace(/-./g, function (s) {
      return s[1].toUpperCase();
    });
  };
  /**
   * @param {JQuery|HTMLElement} node
   * @returns {JQuery}
   */


  exports.cloneNode = function (node) {
    node = exports.toEl(node);

    if (!node) {
      throw new Error('spandrel member did not declare a "dom" property (did you set an element\'s ' + 'contents to an Object instead of a string or JSX element?)');
    }

    var el = node.cloneNode(true);
    Object.assign(el, node);
    return $(el);
  };
  /**
   * @param {number} a
   * @param {number} b
   * @returns {number}
   */


  exports.compareTicks = function (a, b) {
    if (b > 0 && a < 0) {
      return 1; // b rolled over
    }

    if (a > 0 && b < 0) {
      return -1;
    }

    return a - b;
  };
  /**
   * Get the Spandrel widget that contains or is mounted in this DOM element.
   *
   * @param {JQuery|HTMLElement} dom
   * @returns {module:bajaux/spandrel/SpandrelWidget|undefined}
   */


  exports.getContainingSpandrelWidget = function (dom) {
    dom = $(dom).closest('.bajaux-initialized');
    var w = Widget["in"](dom);

    while (dom.length && !(w.queryWidgets || dom[0].spandrelKey)) {
      dom = dom.parent().closest('.bajaux-initialized');
      w = Widget["in"](dom);
    } // noinspection JSValidateTypes


    return w;
  };

  exports.getInlineStyles = function (el) {
    var style = el.style;
    var result = {};

    for (var i = 0, len = style.length; i < len; ++i) {
      var prop = exports.camelCase(style[i]);
      result[prop] = style[prop];
    }

    return result;
  };
  /**
   * @param {module:bajaux/spandrel/SpandrelWidget} parentWidget
   * @param {module:bajaux/spandrel/SpandrelWidget} childWidget
   * @returns {string[]} the path of keys from the parent to the child, that is,
   * `parentWidget.queryWidget(path.join('/'))` would return this child.
   */


  exports.getPathToKid = function (parentWidget, childWidget) {
    var path = [],
        w = childWidget,
        dom = childWidget.jq(),
        spandrelKey;

    while (dom && dom.length && w !== parentWidget && (spandrelKey = dom[0].spandrelKey)) {
      path.push(spandrelKey);
      dom = dom.parent();
      w = dom.length && dom[0][WIDGET_SYMBOL];
    }

    return path.reverse();
  };
  /**
   * @param {module:bajaux/Widget} widget
   * @returns {boolean} if the widget currently has direct focus - i.e. _this_
   * widget has focus and not one of its children.
   */


  exports.hasFocus = function (widget) {
    var jq = widget.jq();

    if (!jq) {
      return false;
    }

    var el = $(exports.$getActiveElement()).parent();

    while ((el = el.closest('.bajaux-initialized')) && el.length) {
      var w = Widget["in"](el);

      if (w && !exports.isElement(w)) {
        return w === widget;
      }

      el = el.parent();
    }

    return false;
  };
  /**
   * @private
   * @returns {Element}
   */


  exports.$getActiveElement = function () {
    return document.activeElement;
  };
  /**
   * @param {module:bajaux/Widget|function} w a Widget instance or constructor
   * @returns {boolean} true if the widget is a dynamic spandrel widget
   */


  exports.isDynamic = function (w) {
    return !!(w && w[IS_DYNAMIC_SYMBOL]);
  };
  /**
   * Is this element just a "stand-in" for a DOM element - one of the widgets
   * that gets dynamically slotted in when your JSX just returns <div>?
   *
   * @param {module:bajaux/Widget} w
   * @returns {boolean} if the widget is a DOM element stand-in
   *
   * @example
   * spandrel((value) => {
   *   return <div spandrelKey="myDiv">
   *     <StringEditor spandrelKey="myString" value={ value }/>
   *   </div>;
   * });
   * //...
   * isElement(widget.queryWidget('myDiv')); // true
   * isElement(widget.queryWidget('myDiv/myString')); // false
   */


  exports.isElement = function (w) {
    return !!(w && w[IS_ELEMENT_SYMBOL]);
  };
  /**
   * @param {module:bajaux/Widget} w
   * @returns {boolean} true if this widget is the spandrel root
   */


  exports.isRoot = function (w) {
    return !!(w && w[ROOT_SYMBOL] === w);
  };
  /**
   * @param {module:bajaux/Widget} w
   * @returns {boolean} true if this is a dummy widget (inserted in place of a
   * falsy value in the spandrel data and intended to be ignored)
   */


  exports.isDummy = function (w) {
    return w instanceof DummyWidget;
  };

  exports.$DummyWidget = DummyWidget;
  /**
   * @param {HTMLElement} node
   * @param {string} key desired {@link module:bajaux/spandrel~Member member} key, or '*'
   * to match any
   * @returns {Array.<HTMLElement>}
   */

  exports.kidsMatching = function (node, key) {
    if (!node) {
      return [];
    }

    key = String(key);
    return Array.prototype.slice.call(node.childNodes).filter(function (n) {
      var nodeKey = n.spandrelKey;
      return key === '*' || key === nodeKey;
    });
  };
  /**
   * @param {string} path path of keys e.g. `a/b/c`
   * @param {string} pattern query pattern, e.g. `a/&#42;/c`
   * @returns {boolean} true if the path matches the pattern
   */


  exports.pathMatches = function (path, pattern) {
    if (path === pattern) {
      return true;
    }

    var pathMembers = path.split('/'),
        patternMembers = pattern.split('/');
    var pathIndex = 0;

    for (var i = 0, len = patternMembers.length; i < len; ++i, ++pathIndex) {
      var patternMember = patternMembers[i],
          pathMember = pathMembers[pathIndex];

      switch (patternMember) {
        case '*':
          break;
        //match any

        case '**':
          var nextPatternMember = patternMembers[++i];

          if (!nextPatternMember) {
            return true; //ending in globstar is a match
          } //advance to next match


          if (nextPatternMember !== '*') {
            while (pathMember && pathMember !== nextPatternMember) {
              pathMember = pathMembers[++pathIndex];
            }

            if (!pathMember) {
              return false; //no match found
            }
          }

          if (nextPatternMember === '*' && i === patternMembers.length - 1) {
            return true;
          } //???


          break;

        default:
          if (patternMember !== pathMember) {
            return false;
          }

      }
    } //true if we hit every member in the path without failures


    return pathIndex >= pathMembers.length;
  };
  /**
   * Returns true if the element has changed in a way that would require a
   * Widget inside it to be torn down and rebuilt. Currently, a change in tag
   * name or addition/removal of the element itself would require a rebuild.
   * Changes in properties such as `class` would not require a rebuild.
   *
   * @param {HTMLElement} newElement
   * @param {HTMLElement} oldElement
   * @returns {boolean}
   */


  exports.requiresRebuild = function (oldElement, newElement) {
    return !(newElement && oldElement) || newElement.tagName !== oldElement.tagName;
  };
  /**
   * @param {JQuery|HTMLElement} el
   * @returns {HTMLElement}
   */


  exports.toEl = function (el) {
    return $(el)[0];
  };
  /**
   * Considers the difference between two different DOM elements, and applies
   * that difference to a third element. For example, if the old element has the
   * class list "foo bar", and the new element has the class list "foo bar baz",
   * then this will add the class "baz" to the target element.
   *
   * If the structures of the two elements are appreciably different, this will
   * throw an error, so ensure `requiresRebuild` does not return true before
   * calling this. This will recurse down into child nodes, but may not work
   * in all situations - it's mainly for updating `<span>oldText</span>` to
   * `<span>newText</span>`.
   *
   * @param {HTMLElement|Node} oldElement
   * @param {HTMLElement|Node} newElement
   * @param {HTMLElement|Node} targetElement
   */


  exports.updateElement = function (oldElement, newElement, targetElement) {
    if (newElement.nodeType === Node.TEXT_NODE) {
      targetElement.textContent = newElement.textContent;
      return;
    } // for a dynamic spandrel element, do not update class/style as these are
    // accounted for by spandrel itself.


    var isSpandrelElement = !!oldElement[IS_DYNAMIC_SYMBOL];
    var oldAttrs = oldElement.attributes || [];

    for (var i = 0, len = oldAttrs.length; i < len; i++) {
      var name = oldAttrs[i].name;

      if (isSpandrelElement && (name === 'class' || name === 'style')) {
        continue;
      }

      if (!newElement.hasAttribute(name)) {
        targetElement.removeAttribute(name);
      }
    }

    var newAttrs = newElement.attributes || [];

    for (var _i = 0, _len = newAttrs.length; _i < _len; _i++) {
      var _newAttrs$_i = newAttrs[_i],
          _name = _newAttrs$_i.name,
          value = _newAttrs$_i.value;

      if (isSpandrelElement && (_name === 'class' || _name === 'style')) {
        continue;
      }

      if (_name === 'class') {
        updateClasses(oldElement, newElement, targetElement);
      } else if (_name === 'style') {
        updateStyle(oldElement, newElement, targetElement);
      } else {
        targetElement.setAttribute(_name, value); // update the underlying DOM value of an INPUT tag

        if (_name === 'value' && oldElement.tagName.toLowerCase() === 'input') {
          targetElement.value = value;
        }
      }
    } //pick up expandos


    Object.assign(targetElement, newElement);

    for (var _i2 = 0, _len2 = BOOLEAN_ATTRIBUTES.length; _i2 < _len2; ++_i2) {
      var attr = BOOLEAN_ATTRIBUTES[_i2];
      targetElement[attr] = newElement[attr];
    }

    var newKids = newElement.childNodes;
    var oldKids = oldElement.childNodes;
    var targetKids = targetElement.childNodes;
    var newLen = newKids.length;
    var oldLen = oldKids.length;
    var targetLen = targetKids.length;

    for (var _i3 = 0, _len3 = Math.max(newLen, oldLen, targetLen); _i3 < _len3; _i3++) {
      var oldKid = oldKids[_i3];
      var newKid = newKids[_i3];
      var targetKid = targetKids[_i3]; // for the purposes of diffing, we will treat text content as an
      // 'attribute' and update it. child elements will not be added or removed.

      if (oldKid && newKid) {
        exports.updateElement(oldKids[_i3], newKids[_i3], targetKid);
      } else if (!oldKid && isTextNode(newKid)) {
        targetElement.textContent = newKid.textContent;
      } else if (!newKid && isTextNode(oldKid) && isTextNode(targetKid)) {
        targetElement.removeChild(targetKid);
      }
    }
  };
  /**
   * Entry point for updating whether a descendent widget of an owner needs to be validated when
   * the owner is.
   *
   * @param {module:bajaux/spandrel/SpandrelWidget} owner
   * @param {module:bajaux/Widget} descendent
   * @param {boolean|function|undefined} validate
   */


  exports.setValidationOfDescendent = function (owner, descendent, validate) {
    var isFunc = typeof validate === 'function';
    var pathToDescendent = exports.getPathToKid(owner, descendent).join('/');

    if (validate) {
      exports.startRequiringValidation(owner, pathToDescendent);

      if (isFunc) {
        exports.setInlineValidator(descendent, validate);
      }
    } else {
      exports.stopRequiringValidation(owner, pathToDescendent);
      exports.removeInlineValidator(descendent);
    }
  };
  /**
   * @param {module:bajaux/spandrel~SpandrelData} spandrelArg
   * @param {*} inputArg the argument to pass to the reify function, if present
   * @returns {Promise.<module:bajaux/spandrel~SpandrelData>} a promise that resolves to the reified
   * spandrel data, or the input spandrel data itself if it is not reifiable
   */


  exports.reify = function (spandrelArg, inputArg) {
    var reify = spandrelArg && spandrelArg[REIFY_SYMBOL];
    return Promise.resolve(isFunction(reify) ? reify.call(spandrelArg, inputArg) : spandrelArg);
  };
  /**
   * Removes any previously set inline validator function from this widget. When the spandrel data
   * provides a function as the `validate` attribute, this is that function.
   *
   * @param {module:bajaux/Widget} widget
   */


  exports.removeInlineValidator = function (widget) {
    var validators = widget.validators();
    var existing = find(validators.get(), function (f) {
      return f[TO_VALIDATE_SYMBOL];
    });
    validators.remove(existing);
  };
  /**
   * Add a validator function to a widget. When the spandrel data provides a function as the
   * `validate` attribute, this is that function. If one was previously present, it will be removed.
   *
   * @param {module:bajaux/Widget} widget
   * @param {function} validate
   */


  exports.setInlineValidator = function (widget, validate) {
    exports.removeInlineValidator(widget);

    var func = function func() {
      return validate.apply(this, arguments);
    };

    func[TO_VALIDATE_SYMBOL] = true;
    widget.validators().add(func);
  };
  /**
   * Sets enabled silently, without allowing the doChanged callback to call rerender(). This is
   * required when diffing, because we want to defer the rerender until well after enabled is set.
   *
   * @param {module:bajaux/Widget} widget
   * @param {boolean} enabled
   * @returns {Promise}
   */


  exports.setEnabledSilently = function (widget, enabled) {
    return withoutRerender(widget, function () {
      return widget.setEnabled(enabled);
    });
  };
  /**
   * Sets properties silently, without allowing the doChanged callback to call rerender(). This is
   * required when diffing, because we want to defer the rerender until well after properties are
   * set.
   *
   * @param {module:bajaux/Widget} widget
   * @param {object|module:bajaux/Properties} properties
   * @returns {Promise}
   */


  exports.setPropertiesSilently = function (widget, properties) {
    return withoutRerender(widget, function () {
      return widget.properties().addAll(new Properties(properties));
    });
  };
  /**
   * Sets readonly silently, without allowing the doRerender callback to call rerender(). This is
   * required when diffing, because we want to defer the rerender until well after readonly is set.
   *
   * @param {module:bajaux/Widget} widget
   * @param {boolean} readonly
   * @returns {Promise}
   */


  exports.setReadonlySilently = function (widget, readonly) {
    return withoutRerender(widget, function () {
      return widget.setReadonly(readonly);
    });
  };
  /**
   * Mark a path to a descendent widget in a spandrel widget as needing validation (when the
   * spandrel data provides a `validate` attribute).
   *
   * @param {module:bajaux/spandrel/SpandrelWidget} owner
   * @param {string} pathToDescendent the path from the owner to the descendent widget that requires
   * validation
   */


  exports.startRequiringValidation = function (owner, pathToDescendent) {
    var paths = owner[TO_VALIDATE_SYMBOL] || (owner[TO_VALIDATE_SYMBOL] = []);

    if (!paths.includes(pathToDescendent)) {
      paths.push(pathToDescendent);
    }
  };
  /**
   * Mark a path to a descendent widget in a spandrel widget as no longer needing validation (when
   * spandrel data stops providing a `validate` attribute).
   *
   * @param {module:bajaux/spandrel/SpandrelWidget} owner
   * @param {string} pathToDescendent the path from the owner to the descendent widget that no
   * longer requires validation
   */


  exports.stopRequiringValidation = function (owner, pathToDescendent) {
    var paths = owner[TO_VALIDATE_SYMBOL];

    if (paths) {
      var i = paths.indexOf(pathToDescendent);

      if (i >= 0) {
        paths.splice(i, 1);
      }
    }
  };

  function updateClasses(oldElement, newElement, targetElement) {
    var oldClassList = _toConsumableArray(oldElement.classList);

    var newClassList = _toConsumableArray(newElement.classList);

    var targetClassList = targetElement.classList;
    var removed = difference(oldClassList, newClassList);
    var added = difference(newClassList, oldClassList);
    removed.forEach(function (c) {
      return targetClassList.remove(c);
    });
    added.forEach(function (c) {
      return targetClassList.add(c);
    });
  }

  function updateStyle(oldElement, newElement, targetElement) {
    var oldStyle = oldElement.style;
    var newStyle = newElement.style;
    var targetStyle = targetElement.style;

    _toConsumableArray(oldStyle).forEach(function (name) {
      var oldValue = oldStyle[name];
      var newValue = newStyle[name];

      if (oldValue !== newValue) {
        targetStyle[name] = newValue;
      }
    });

    _toConsumableArray(newStyle).forEach(function (name) {
      var oldValue = oldStyle[name];

      if (!oldValue) {
        targetStyle[name] = newStyle[name];
      }
    });
  }

  function isTextNode(el) {
    return el && el.nodeType === Node.TEXT_NODE;
  }

  function isDynamicSpandrelWidget(widget) {
    return widget instanceof getDynamicSpandrelWidget();
  }

  function withoutRerender(widget, doIt) {
    if (isDynamicSpandrelWidget(widget)) {
      var rerender = widget.rerender;
      widget.rerender = resolve;
      return Promise.resolve(doIt())["finally"](function () {
        widget.rerender = rerender;
      });
    } else {
      return doIt();
    }
  }

  function resolve() {
    return Promise.resolve();
  }

  return exports;
});
