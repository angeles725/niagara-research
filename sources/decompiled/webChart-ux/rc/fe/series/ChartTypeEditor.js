/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/fe/series/ChartTypeEditor
 */
define(['jquery', 'Promise', 'bajaux/commands/ToggleCommand', 'bajaux/util/CommandButton', 'nmodule/webEditors/rc/fe/baja/BaseEditor'], function ($, Promise, ToggleCommand, CommandButton, BaseEditor) {
  'use strict';
  /**
   * A field editor for working with Chart Types.
   *
   * @class
   * @extends module:nmodule/webEditors/rc/fe/baja/BaseEditor
   * @alias module:nmodule/webChart/rc/series/ChartTypeEditor
   */

  var ChartTypeEditor = function ChartTypeEditor(params) {
    BaseEditor.call(this, $.extend({
      keyName: 'ChartTypeEditor'
    }, params));
  };

  ChartTypeEditor.prototype = Object.create(BaseEditor.prototype);
  ChartTypeEditor.prototype.constructor = ChartTypeEditor;
  /**
   * Creates the dom for the editor
   * @private
   * @param {jQuery} dom
   */

  ChartTypeEditor.prototype.doInitialize = function (dom) {
    var that = this,
        getPromise = function getPromise(value) {
      var widget = new CommandButton(),
          div = $("<span value='" + value + "'>").appendTo(dom),
          command = new ToggleCommand({
        module: "webChart",
        lex: "chartType." + value,
        func: function func() {
          that.setModified(true);
          return that.doLoad(div.attr("value"));
        }
      });
      return widget.initialize(div).then(function () {
        return widget.load(command);
      });
    },
        linePromise = getPromise("line"),
        discretePromise = getPromise("discreteLine"),
        shadePromise = getPromise("shade"),
        barPromise = getPromise("bar");

    return Promise.all([linePromise, discretePromise, shadePromise, barPromise]);
  };
  /**
   * Checks or unchecks the checkbox to reflect the loaded value.
   * @private
   * @param {Boolean} val
   */


  ChartTypeEditor.prototype.doLoad = function (val) {
    var jq = this.jq(),
        selected = jq.find("span[value='" + val + "']"),
        selectedWidget = selected.data("widget");

    if (selectedWidget) {
      selectedWidget.value().setSelected(true);
    }

    jq.find("span.CommandButton").not("[value='" + val + "']").each(function () {
      var notSelected = $(this).data("widget");

      if (notSelected) {
        notSelected.value().setSelected(false);
      }
    });
  };
  /**
   * Returns the selected chartType
   * @private
   * @returns {String}
   */


  ChartTypeEditor.prototype.doRead = function () {
    return this.jq().find(".ux-toggled").attr("value");
  };

  return ChartTypeEditor;
});
