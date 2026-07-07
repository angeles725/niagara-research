/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 *
 * @deprecated since Niagara 4.12
 * @module nmodule/webChart/rc/fe/color/ColorEditor
 */
define(['baja!', 'log!nmodule.webChart.rc.fe.color.ColorEditor', 'jquery', 'Promise', 'nmodule/webChart/rc/fe/color/colorChooser', 'nmodule/webEditors/rc/fe/baja/BaseEditor', 'nmodule/webEditors/rc/fe/fe', 'bajaux/Widget', 'baja!gx:Color'], function (baja, log, $, Promise, colorChooser, BaseEditor, fe, Widget) {
  "use strict";

  var logSevere = log.severe.bind(log);
  var COLOR_EDITOR_CLASS = 'ColorEditor';
  var PREVIEW_CLASS = 'ColorEditor-preview';
  /**
   * A ColorEditor is a simple editor for gx:Color. It's not part of the
   * webEditors baseEditors so if you have a gx:color on a PropertySheet,
   * make sure to include this module.
   *
   * @class
   * @alias module:nmodule/webChart/rc/fe/color/ColorEditor
   * @extends module:bajaux/Widget
   */

  var ColorEditor = function ColorEditor() {
    BaseEditor.apply(this, arguments);
  };

  ColorEditor.prototype = Object.create(BaseEditor.prototype);
  ColorEditor.prototype.constructor = ColorEditor;
  /**
   * Get the JQuery Dom for the Preview
   * @returns {jquery}
   * @private
   */

  ColorEditor.prototype.$getPreviewDom = function () {
    return $("." + PREVIEW_CLASS, this.jq());
  };
  /**
   * initialize the dom
   * @param {jquery} dom
   */


  ColorEditor.prototype.doInitialize = function (dom) {
    var that = this;
    dom.on('click', function () {
      colorChooser.show(that.value()).then(function (result) {
        var dom = that.$getPreviewDom();
        dom.css({
          backgroundColor: result
        });
        dom.data({
          value: result
        });
        that.setModified(true);
      })["catch"](logSevere);
    });
    dom.addClass(COLOR_EDITOR_CLASS);
  };
  /**
   * Load a gx:Color into the editor
   * @param {gx:Color} val
   */


  ColorEditor.prototype.doLoad = function (val) {
    var that = this;
    var div = $('<div/>').addClass(PREVIEW_CLASS).css('background-color', String(val));
    that.jq().html(div);
    that.$getPreviewDom().data({
      value: val.encodeToString()
    });
  };
  /**
   * Reads the color value from the preview.
   *
   * @returns {gx:Color}
   */


  ColorEditor.prototype.doRead = function () {
    var colorValue = this.$getPreviewDom().data("value"),
        color = baja.$('gx:Color', colorValue);
    return color;
  };
  /**
   * After destroying child editors, removes CSS classes added in
   * `initialize()`.
   *
   * @returns {Promise} promise to be resolved after CSS classes removed
   * and child editors destroyed
   */


  ColorEditor.prototype.doDestroy = function () {
    var that = this;
    return Promise.resolve(BaseEditor.prototype.doDestroy.apply(this, arguments)).then(function () {
      that.jq().removeClass(COLOR_EDITOR_CLASS);
    });
  };

  return ColorEditor;
});
