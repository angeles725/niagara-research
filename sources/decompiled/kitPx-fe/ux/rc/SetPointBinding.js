function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

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
 * @copyright 2020 Tridium, Inc. All Rights Reserved.
 * @author Vikram Nagulan
 */

/**
* API Status: **Private**
* @module nmodule/kitPx/rc/binding/SetPointBinding
*/
define(['underscore', 'baja!', 'Promise', 'nmodule/webEditors/rc/fe/feDialogs', 'nmodule/bajaui/rc/baja/binding/ValueBinding', 'nmodule/bajaui/rc/ux/bajauiEvents', 'log!nmodule/kitPx/rc/binding/SetPointBinding', 'bajaux/Properties', 'nmodule/webEditors/rc/fe/baja/util/rangeUtils', 'nmodule/bajaui/rc/ux/Slider'], function (_, baja, Promise, feDialogs, ValueBinding, bajauiEvents, log, Properties, rangeUtils, Slider) {
  'use strict';

  var logWarning = log.warning.bind(log);
  var initial = _.initial,
      last = _.last;
  var ACTION_PERFORMED_EVENT = bajauiEvents.ACTION_PERFORMED_EVENT;
  var inRange = rangeUtils.inRange;
  var error = feDialogs.error;
  /**
   * BajaScript implementation of `kitPx:SetPointBinding`.
   * 
   * @class
   * @alias module:nmodule/kitPx/rc/binding/IncrementSetPointBinding
   * @extends module:nmodule/bajaui/rc/baja/binding/ValueBinding
   */

  return /*#__PURE__*/function (_ValueBinding) {
    _inherits(SetPointBinding, _ValueBinding);

    var _super = _createSuper(SetPointBinding);

    function SetPointBinding() {
      _classCallCheck(this, SetPointBinding);

      return _super.apply(this, arguments);
    }

    _createClass(SetPointBinding, [{
      key: "targetChanged",

      /**
       * Updates the bound widget if target has changed
       * 
       * @returns {Promise} 
       */
      value: function targetChanged() {
        var _this = this;

        return _get(_getPrototypeOf(SetPointBinding.prototype), "targetChanged", this).call(this).then(function () {
          // Empty widget for null ord
          if (!_this.getOrdTarget()) {
            return;
          }

          var widget = _this.getWidget(),
              target = _this.getOrdTarget(); // Do not load if the widget is modified locally  


          if (widget.isModified()) {
            return;
          } //Set the widget property and then load


          var targetObj = target.getObject();

          var valueToLoad = getValueToBind(targetObj).newCopy(true),
              widgetProperty = _this.getWidgetProperty();

          if (widgetProperty) {
            widget.properties().setValue(widgetProperty, valueToLoad);
          }

          var facets = getFacetsFromObject(targetObj).toObject(); // Revisit when NCCB-48777 is fixed

          if (widget instanceof Slider) {
            facets = Object.fromEntries(Object.entries(facets).filter(function (entry) {
              return entry[0] !== 'min' && entry[0] !== 'max';
            }));
          }

          widget.properties().addAll(new Properties(facets));
          return widget.setReadonly(!target.canWrite()).then(function () {
            return widget.load(valueToLoad)["catch"](logWarning);
          });
        });
      }
      /**
       * Saves the modified editor value.
       * This is the only route to save a SetPointFieldEditor with this binding
       * 
       * @param {boolean} If this is a widget event don't look for modified
       * @returns {Promise}
       */

    }, {
      key: "save",
      value: function save(isWidgetEvent) {
        var _this2 = this;

        var widget = this.getWidget();
        var target = this.getOrdTarget();

        if (!this.isBound() || !widget.isModified() && !isWidgetEvent) {
          return Promise.resolve();
        }

        return widget.save().then(function () {
          return _this2.$getValueToSave();
        }).then(function (value) {
          if (value === null) {
            return;
          }

          return verifyBounds(value, widget) // min/max validation
          .then(function () {
            var obj = target.getObject();

            if (baja.hasType(obj, 'baja:Component')) {
              var saveResult = saveSetPointAction(obj, value);

              if (!saveResult) {
                error('WARNING: No mechanism found to save');
              } else {
                saveResult["catch"](error);
              }
            } else {
              return saveSetPointProperty(target, value);
            }
          });
        });
      }
      /**
       * Add event listeners for the widget:
       *
       * - on mousedown when not a right click, fire action when widgetEvent is `actionPerformed`
       *
       * @param {module:bajaux/Widget} widget
       */

    }, {
      key: "addListeners",
      value: function addListeners(widget) {
        var _this3 = this;

        this.addWidgetEvents(widget, _defineProperty({}, ACTION_PERFORMED_EVENT, function (event) {
          var widgetEvent = _this3.get('widgetEvent');

          if (widgetEvent === "actionPerformed") {
            // Toggle the property and then call save
            var widgetProperty = _this3.getWidgetProperty(),
                currentValue = widgetProperty ? widget.properties().getValue(widgetProperty) : null;

            if (widgetProperty) {
              widget.properties().setValue(widgetProperty, currentValue);
            }

            if (currentValue === null) {
              // just call save to allow a sub class to pass a value
              return _this3.save(true)["catch"](error);
            }

            return widget.load(currentValue).then(function () {
              return _this3.save(true)["catch"](error);
            });
          }
        }));
      }
      /**
       * 
       * @returns {boolean}
       */

    }, {
      key: "isDegraded",
      value: function isDegraded() {
        var target = this.getOrdTarget();
        return _get(_getPrototypeOf(SetPointBinding.prototype), "isDegraded", this).call(this) || target && !target.canWrite();
      }
      /**
       * Do not set the 'title' attribute and apply summary for this binding
       * 
       * @returns {Promise}
       */

    }, {
      key: "applySummary",
      value: function applySummary() {
        return Promise.resolve();
      }
      /**
       * Gets the value to save. Can be overriden by a sub-class to provide 
       * its own value to call the binding save on.
       * 
       * @private
       * @returns {Promise}
       */

    }, {
      key: "$getValueToSave",
      value: function $getValueToSave() {
        var widget = this.getWidget();
        return widget.read();
      }
    }]);

    return SetPointBinding;
  }(ValueBinding);
  /**
   * 
   * @param {baja.Value} targetValue
   */

  function getValueToBind(targetValue) {
    var type = targetValue.getType();

    if (type.is("baja:IBoolean")) {
      return Boolean.getBooleanFromIBoolean(targetValue);
    } else if (type.is("baja:INumeric")) {
      return Number.getNumberFromINumeric(targetValue);
    } else if (type.is("baja:IEnum")) {
      return baja.Enum.getEnumFromIEnum(targetValue);
    } else if (type.is("baja:IStatusValue")) {
      // assume string point 
      if (type.is('baja:StatusString')) {
        return targetValue.getValue();
      } else {
        var out = targetValue.get('out');
        return out && out.getType().is("baja:StatusString") ? out.getValue() : '';
      }
    } else if (type.is("baja:Component")) {
      //This else if is required for NiagaraVirtuals that happen to be representing kitControl:NumericConst or similar types
      var _out = targetValue.get('out');

      if (_out && _out.getType().is("baja:StatusValue")) {
        return _out.getValue();
      }
    }

    return targetValue;
  }
  /**
   * If target is a component, it invokes the `set` action on it
   * 
   * @param {baja.Component} targetComp 
   * @param {baja.Value} value 
   * @return {Promise<baja.Value|null>|boolean}
   */


  function saveSetPointAction(targetComp, value) {
    var action = targetComp.getSlot("set");

    if (!action || action && !action.isAction()) {
      return false;
    }

    if (!action.getParamType().equals(value.getType())) {
      return false;
    }

    return targetComp.invoke({
      slot: action,
      value: value
    });
  }
  /**
   * Save a property in the target component
   * 
   * @param {module:baja/ord/OrdTarget} target 
   * @param {baja.Object} value 
   */


  function saveSetPointProperty(target, value) {
    var newValue = convertSetPointValue(value, target.getObject());

    if (newValue === null) {
      return Promise.resolve();
    }

    var propertyPath = target.propertyPath;
    var comp = target.getComponent();
    initial(propertyPath).forEach(function (prop) {
      comp = comp.get(prop);
    });
    return comp.set({
      slot: last(propertyPath),
      value: newValue
    });
  }
  /**
   * 
   * @param {baja.Object} from 
   * @param {baja.Object} to 
   */


  function convertSetPointValue(from, to) {
    // If the same then easy as pie
    if (from.getType().equals(to.getType())) {
      return from;
    } // ------- Numeric -------
    // INumeric -> Numeric


    var x;

    if (from.getType().is("baja:INumeric") && to.getType().isNumber()) {
      x = Number.getNumberFromINumeric(from);

      switch (to.getDataTypeSymbol()) {
        case "i":
          return baja.Integer.make(x);

        case "l":
          return baja.Long.make(x);

        case "f":
          return baja.Float.make(x);

        case "d":
          return x;
      }
    } // INumeric -> StatusNumeric


    if (from.getType().is("baja:INumeric") && to.getType().is("baja:StatusNumeric")) {
      var statusNumeric = baja.$("baja:StatusNumeric");
      statusNumeric.setValue(Number.getNumberFromINumeric(from));
      return statusNumeric;
    } // ------- Boolean -------
    // IBoolean -> Boolean


    if (from.getType().is("baja:IBoolean") && to.getType().is("baja:Boolean")) {
      return Boolean.getBooleanFromIBoolean(from);
    } // IBoolean -> StatusBoolean


    if (from.getType().is("baja:IBoolean") && to.getType().is("baja:StatusBoolean")) {
      var statusBoolean = baja.$("baja:StatusBoolean");
      statusBoolean.setValue(Boolean.getBooleanFromIBoolean(from));
      return statusBoolean;
    } // ------- Enum -------
    // IEnum -> DynamicEnum


    if (from.getType().is("baja:IEnum") && to.getType().is("baja:DynamicEnum")) {
      return baja.Enum.getEnumFromIEnum(from);
    } // IEnum -> StatusEnum


    if (from.getType().is("baja:IEnum") && to.getType().is("baja:StatusEnum")) {
      var statusEnum = baja.$("baja:StatusEnum");
      statusEnum.setValue(baja.Enum.getEnumFromIEnum(from));
      return statusEnum;
    } // ------- String -------
    // String -> StatusString


    if (from.getType().is("baja:String") && to.getType().is("baja:StatusString")) {
      var statusString = baja.$("baja:StatusString");
      statusString.setValue(from.toString());
      return statusString;
    }

    baja.error("WARNING: No mechanism found to save");
    return null;
  }
  /**
   * Get facets from the target object
   * 
   * @param {baja.Component} obj 
   */


  function getFacetsFromObject(obj) {
    var type = obj.getType();

    if (type.is("baja:IBoolean")) {
      // Writable Booleans and Boolean consts
      return Boolean.getFacetsFromIBoolean(obj);
    } else if (type.is("baja:INumeric")) {
      // Writable Numerics and Numeric consts
      return Number.getFacetsFromINumeric(obj);
    } else if (type.is("baja:IEnum")) {
      // Writable Enums and Enum consts
      return baja.Enum.getFacetsFromIEnum(obj);
    } else if (type.is("baja:IStatusValue")) {
      // Most likely a StatusString or a StringConst
      return baja.Facets.getFacetsFromObject(obj);
    } else if (type.is("baja:Component")) {
      // Most likely a Niagara Virtual replacement for something like a NumericConst
      return baja.Facets.getFacetsFromObject(obj);
    }

    return baja.Facets.DEFAULT;
  }
  /**
   * Verify min and max range for numeric.
   * For non-numeric it simply returns
   * 
   * @param {baja.Value} value 
   * @param {module:bajaux/Widget} widget 
   */


  function verifyBounds(value, widget) {
    var props = widget.properties();
    return inRange(value, {
      min: props.getValue('min'),
      max: props.getValue('max')
    });
  }
});
