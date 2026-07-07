function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

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
 * @module nmodule/kitPx/rc/fe/GenericFieldEditor
 */
define(['baja!', 'jquery', 'Promise', 'bajaux/spandrel', 'bajaux/Widget', 'bajaux/commands/CommandGroup', 'bajaux/util/CommandButtonGroup', 'nmodule/bajaui/rc/model/UxModelSupport', 'nmodule/webEditors/rc/fe/fe', 'nmodule/webEditors/rc/fe/baja/util/ComplexDiff', 'nmodule/webEditors/rc/wb/PropertySheet', 'nmodule/webEditors/rc/wb/commands/PopOutCommand', 'css!nmodule/kitPx/rc/kitPx'], function (baja, $, Promise, spandrel, Widget, CommandGroup, CommandButtonGroup, UxModelSupport, fe, ComplexDiff, PropertySheet, PopOutCommand) {
  'use strict';

  var widgetDefaults = function widgetDefaults() {
    return {
      properties: {
        rootCssClass: 'ux-GenericFieldEditor',
        foreground: baja.$('gx:Brush', 'null'),
        background: baja.$('gx:Brush', 'null')
      }
    };
  };
  /**
   * @class
   * @alias module:nmodule/kitPx/rc/fe/GenericFieldEditor
   * @extends {module:bajaux/spandrel/SpandrelWidget}
   */


  return /*#__PURE__*/function (_spandrel) {
    _inherits(GenericFieldEditor, _spandrel);

    var _super = _createSuper(GenericFieldEditor);

    function GenericFieldEditor(params) {
      var _this;

      _classCallCheck(this, GenericFieldEditor);

      _this = _super.call(this, {
        params: params,
        defaults: widgetDefaults()
      });
      UxModelSupport(_assertThisInitialized(_this));
      return _this;
    }
    /**
     * Returns the editor wrapped by this editor
     * 
     * @returns {module:bajaux/Widget}
     */


    _createClass(GenericFieldEditor, [{
      key: "getContent",
      value: function getContent() {
        return this.queryWidget('editor');
      }
    }, {
      key: "doLoad",
      value: function doLoad() {
        var _this2 = this;

        return _get(_getPrototypeOf(GenericFieldEditor.prototype), "doLoad", this).apply(this, arguments).then(function () {
          var ed = _this2.getContent(),
              buttonGroup = _this2.queryWidget('commands'); //no double popup required for when the PropertySheet is used


          if (!ed || !buttonGroup || ed instanceof PropertySheet) {
            return;
          }

          var cmdGroup = new CommandGroup({
            commands: [new PopOutCommand(ed)]
          });
          return buttonGroup.load(cmdGroup);
        });
      }
    }, {
      key: "doRead",
      value: function doRead() {
        var content = this.getContent();
        return content && content.read().then(function (value) {
          if (value instanceof ComplexDiff) {
            return value.apply(content.value().newCopy(true));
          }

          return value;
        });
      }
    }, {
      key: "doSave",
      value: function doSave() {
        var content = this.getContent();
        return content && content.save();
      }
    }, {
      key: "doLayout",
      value: function doLayout() {
        this.$applyStyles();
      }
    }, {
      key: "doDestroy",
      value: function doDestroy() {
        this.$lastKnownValue = undefined;
        return _get(_getPrototypeOf(GenericFieldEditor.prototype), "doDestroy", this).apply(this, arguments);
      }
      /**
       * Get only the text input inside the field editor content.
       * 
       * @returns {Element|null}
       */

    }, {
      key: "$getContentTextElement",
      value: function $getContentTextElement() {
        var content = this.getContent();
        return content && content.jq().children('input')[0];
      }
      /**
       * Apply only to text input for now matching wb 
       */

    }, {
      key: "$applyStyles",
      value: function $applyStyles() {
        var contentTextEl = this.$getContentTextElement();

        if (contentTextEl) {
          var background = this.properties().getValue('background');
          var foreground = this.properties().getValue('foreground');
          background.applyBackgroundToElement(contentTextEl);
          foreground.applyForegroundToElement(contentTextEl);
        }
      }
    }]);

    return GenericFieldEditor;
  }(spandrel(function (model, _ref) {
    var properties = _ref.properties,
        rootElement = _ref.rootElement,
        self = _ref.self;

    if (rootElement) {
      rootElement.classList.add('hx-bajaux-fe');
      rootElement.classList.add('ux-fg');
    }

    var value = model.getValue();

    if (!baja.hasType(value)) {
      //TODO: NCCB-48876: caching `$lastKnownValue` is used to workaround the value disappearing
      if (this.$lastKnownValue === undefined) {
        return '<div/>'; // no value ready to load yet
      } else {
        value = this.$lastKnownValue;
      }
    }

    this.$lastKnownValue = value; // special case: we actually do want the uxFieldEditor property, and it is omitted from widget state

    var myProperties = self.properties().toObject(); // If an appropriate mini editor is not found for the value then fallback to PropertySheet

    return fe.getDefaultConstructor(value.getType(), {
      formFactors: ['mini'],
      properties: myProperties
    }).then(function (ctor) {
      return ctor || value.getType().isComplex() && PropertySheet;
    }).then(function (Editor) {
      return {
        editor: {
          dom: '<div class="hx-bajaux-editor" />',
          properties: properties,
          value: value,
          type: Editor
        },
        commands: {
          dom: '<div/>',
          properties: {
            toolbar: true,
            onDisabled: 'hide'
          },
          type: CommandButtonGroup
        }
      };
    });
  }, {
    strategy: 'niagara'
  }));
});
