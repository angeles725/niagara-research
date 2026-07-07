/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/menu/MenuWidget
 */
define(['jquery', 'nmodule/webChart/rc/grid/GridEditor', 'hbs!nmodule/webChart/rc/menu/menuWidgetStructure'], function ($, GridEditor, MenuWidgetStructure) {
  "use strict";
  /**
   * MenuWidget is a GridEditor that draws a right click menu of commands or widgets
   *
   * @class
   * @alias module:nmodule/webChart/rc/menu/MenuWidget
   * @extends module:nmodule/webChart/rc/grid/GridEditor
   */

  var MenuWidget = function MenuWidget() {
    var that = this;
    GridEditor.apply(that, arguments);
    /**
     * Destroy MenuWidget if the click is not on context menu. If item is clicked, return true to allow
     * for the CommandButton's mousedown handler to invoke the command.
     * @param {Object} event
     * @returns {boolean}
     */

    that.$mouseDownHandler = function (event) {
      if ($(event.target).closest(".MenuWidget-container")[0]) {
        return true;
      } else {
        that.destroy();
        event.preventDefault();
      }

      return false;
    };
  };

  MenuWidget.prototype = Object.create(GridEditor.prototype);
  MenuWidget.prototype.constructor = MenuWidget;
  /**
   * @returns {function} a template function.
   */

  MenuWidget.prototype.getStructure = function () {
    return MenuWidgetStructure;
  };
  /**
   * Init and add event listener on the body. Note that this listener uses true for 'use capture' so that
   * dom elements that prevent event propagation don't prevent the menu from closing (like hxpx).
   * @param {jQuery} dom
   * @returns {Promise}
   */


  MenuWidget.prototype.doInitialize = function (dom) {
    var that = this;
    $('body')[0].addEventListener("mousedown", that.$mouseDownHandler, true);
    return GridEditor.prototype.doInitialize.apply(this, arguments);
  };
  /**
   * Init content tabs and register onclick handler.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @param {Number} index
   * @returns {Promise}
   */


  MenuWidget.prototype.initTab = function (tab, index) {
    var that = this;
    return GridEditor.prototype.initTab.apply(this, arguments).then(function () {
      that.getTabContentElement(tab).click(function (event) {
        //TODO: keep menu open for nested menu
        that.destroy();
        event.preventDefault();
      });
    });
  };
  /**
   * Destroy and Remove the event listener on the body
   * @returns {Promise}
   */


  MenuWidget.prototype.doDestroy = function () {
    var that = this,
        jq = that.$jq;

    if (that.$mouseDownHandler) {
      $('body')[0].removeEventListener("mousedown", that.$mouseDownHandler, true);
    }

    return GridEditor.prototype.doDestroy.apply(this, arguments).then(function () {
      jq.remove();
    });
  };

  return MenuWidget;
});
