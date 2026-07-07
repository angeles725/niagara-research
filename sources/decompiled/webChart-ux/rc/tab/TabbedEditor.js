/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/tab/TabbedEditor
 */
define(['jquery', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/grid/GridEditor', 'hbs!nmodule/webChart/rc/tab/tabbedEditorStructure'], function ($, webChartUtil, GridEditor, TabbedEditorStructure) {
  "use strict";

  var HIGHLIGHT_CLASS = 'ux-tab-title-highlight ux-highlight';
  /**
   * TabbedEditor is a GridEditor that draws a Tab Label for each tab and shows them one at a time.
   *
   * @class
   * @alias module:nmodule/webChart/rc/tab/TabbedEditor
   * @extends module:nmodule/webChart/rc/grid/GridEditor
   */

  var TabbedEditor = function TabbedEditor() {
    GridEditor.apply(this, arguments);
    this.$selectedTab = null;
  };

  TabbedEditor.prototype = Object.create(GridEditor.prototype);
  TabbedEditor.prototype.constructor = TabbedEditor;
  /**
   * Selects selectedTab if set before initialization
   * @param dom
   * @returns {*}
   */

  TabbedEditor.prototype.doInitialize = function (dom) {
    var that = this;
    return GridEditor.prototype.doInitialize.apply(this, arguments).then(function () {
      if (that.$selectedTab) {
        dom.find('.js-tab-title-' + webChartUtil.safeClassName(that.$selectedTab.title)).click();
      }
    });
  };
  /**
   * Overridden to alter the structure of the tab to include a label and a content area.
   * @returns {function} a template function.
   */


  TabbedEditor.prototype.getStructure = function () {
    return TabbedEditorStructure;
  };
  /**
   * Sets the selected Tab.
   * @returns {module:nmodule/webChart/rc/tab/Tab}
   */


  TabbedEditor.prototype.setSelectedTab = function (tab) {
    var that = this;

    if (that.$selectedTab !== tab) {
      that.$selectedTab = tab;

      if (that.jq() && that.isInitialized()) {
        that.jq().find('.js-tab-title-' + webChartUtil.safeClassName(that.$selectedTab.title)).click();
      }
    }
  };
  /**
   * Return the selected Tab.
   * @returns {module:nmodule/webChart/rc/tab/Tab}
   */


  TabbedEditor.prototype.getSelectedTab = function () {
    var that = this;

    if (that.$selectedTab) {
      return that.$selectedTab;
    } else if (that.$tabs.length > 0) {
      return that.$tabs[0];
    } else {
      return null;
    }
  };
  /**
   * Return the Tab content element.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @returns {jquery}
   */


  TabbedEditor.prototype.getTabContentElement = function (tab) {
    return $('.js-tab-content-' + tab.safeTitle, this.jq());
  };
  /**
   * Return the Tab label element.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @returns {jquery}
   */


  TabbedEditor.prototype.getTabLabelElement = function (tab) {
    return $('.js-tab-title-' + tab.safeTitle, this.jq());
  };
  /**
   * Init content tabs and register onclick handler for tab label.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @param {Number} index
   * @returns {Promise}
   */


  TabbedEditor.prototype.initTab = function (tab, index) {
    var that = this,
        promise = GridEditor.prototype.initTab.apply(this, arguments),
        tabTd;
    that.getTabLabelElement(tab).click(function () {
      for (var i = 0; i < that.$tabs.length; i++) {
        var tab = that.$tabs[i];
        tabTd = that.getTabContentElement(tab).parent();
        var selected = i === index;
        tabTd.toggle(selected);
        that.getTabLabelElement(tab).toggleClass(HIGHLIGHT_CLASS, i === index);

        if (selected) {
          that.$selectedTab = tab;
        }
      }
    });
    return promise.then(function () {
      var dom = that.getTabContentElement(tab).parent();
      dom.attr('colspan', that.$tabs.length + 1);

      if (index > 0) {
        dom.hide();
      } else {
        that.getTabLabelElement(tab).addClass(HIGHLIGHT_CLASS);
      }
    });
  };

  return TabbedEditor;
});
