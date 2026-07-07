/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/fe/series/SeriesEditor
 */
define(['jquery', 'nmodule/webEditors/rc/fe/baja/ComplexCompositeEditor', 'hbs!nmodule/webChart/rc/fe/series/seriesEditorStructure', 'nmodule/webEditors/rc/fe/baseEditorSet'], function ($, ComplexCompositeEditor, seriesEditorStructure) {
  "use strict";
  /**
   * A SeriesEditor is the editor for configurable properties on BaseSeries.
   *
   * @class
   * @alias module:nmodule/webChart/rc/fe/series/SeriesEditor
   * @extends module:nmodule/webEditors/rc/fe/baja/ComplexCompositeEditor
   */

  var SeriesEditor = function SeriesEditor(params) {
    ComplexCompositeEditor.apply(this, arguments);
  };

  SeriesEditor.prototype = Object.create(ComplexCompositeEditor.prototype);
  SeriesEditor.prototype.constructor = SeriesEditor;
  /**
   * Return all configurable properties of the SeriesEditor
   * @returns {String[]}
   */

  SeriesEditor.prototype.getSlotFilter = function () {
    return ['color', 'chartType'];
  };

  SeriesEditor.prototype.$buildSubEditorDom = function (key) {
    var jq = this.jq(),
        dom = $('.js-' + key, jq);
    return dom;
  };
  /**
   * initialize the dom and register all event handlers
   * @param {jQuery} dom
   */


  SeriesEditor.prototype.doInitialize = function (dom) {
    var div = $("<div></div>");
    dom.addClass('SeriesEditor');
    div.html(seriesEditorStructure());
    dom.append(div);
    return ComplexCompositeEditor.prototype.doInitialize.apply(this, arguments);
  };
  /**
   * Remove `SeriesEditor` class and call super.
   */


  SeriesEditor.prototype.doDestroy = function () {
    this.jq().removeClass('SeriesEditor');
    return ComplexCompositeEditor.prototype.doDestroy.apply(this, arguments);
  };

  return SeriesEditor;
});
