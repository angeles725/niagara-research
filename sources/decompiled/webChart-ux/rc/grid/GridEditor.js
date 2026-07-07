/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/grid/GridEditor
 */
define(['jquery', 'Promise', 'nmodule/webChart/rc/tab/Tab', 'bajaux/commands/Command', 'bajaux/Widget', 'hbs!nmodule/webChart/rc/grid/gridEditorStructure'], function ($, Promise, Tab, Command, Widget, gridEditorStructure) {
  "use strict";
  /**
   * GridEditor is a Widget that can group other Widgets and commands that are organized in tabs.
   *
   * @class
   * @alias module:nmodule/webChart/rc/grid/GridEditor
   * @extends module:bajaux/Widget
   */

  var GridEditor = function GridEditor() {
    Widget.apply(this, arguments);
    this.$tabs = [];
  };

  GridEditor.prototype = Object.create(Widget.prototype);
  GridEditor.prototype.constructor = GridEditor;
  /**
   * Use the structure provided to initialize the dom the current tabs.
   * @param {jquery} dom
   * @returns {Promise}
   */

  GridEditor.prototype.doInitialize = function (dom) {
    var that = this,
        inits;
    dom.html(that.getStructure()({
      tabs: that.$tabs
    }));
    inits = $.map(that.$tabs, function (tab, index) {
      var tabDom = that.getTabContentElement(tab);

      if (tabDom.length === 0) {
        return reject("init cannot find dom for tab: " + tab.title + ", " + index);
      }

      if (tab.widget instanceof Widget) {
        tab.dom = tabDom;
        return that.initTab(tab, index);
      } else {
        return reject("init needs a widget for tab: " + tab.title + ", " + index);
      }
    });
    return Promise.all(inits);
  };
  /**
   * Callback for subclasses to addOn to the initialization of a tab.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @returns {Promise}
   */


  GridEditor.prototype.initTab = function (tab) {
    return tab.initialize();
  };
  /**
   * Callback for subclasses to addOn to the loading of a tab.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @returns {Promise}
   */


  GridEditor.prototype.loadTab = function (tab) {
    return tab.load();
  };
  /**
   * Return the element for the Content.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @returns {jquery}
   */


  GridEditor.prototype.getTabContentElement = function (tab) {
    return $('.js-tab-' + tab.safeTitle, this.jq());
  };
  /**
   * Subclasses should override this method to alter the structure of the tabs
   * @returns {function} a template function.
   */


  GridEditor.prototype.getStructure = function () {
    return gridEditorStructure;
  };
  /**
   * Add a Command, uses the commandID at the title.
   * @param {module:bajaux/commands/Command} command
   * @param {Object} [title] alternate tab title if provided
   * @return {module:nmodule/webChart/rc/tab/Tab}
   */


  GridEditor.prototype.addCommand = function (command, title) {
    var that = this;

    if (command instanceof Command) {
      var tab = new Tab({
        item: command,
        title: title ? String(title) : String(command.$id)
      });
      that.$tabs.push(tab);
      return tab;
    } else {
      throw new Error("Must be a Command");
    }
  };
  /**
   * Get the Command based on the tab number.
   * @param {Number} tabNumber
   * @returns {module:nmodule/webChart/rc/tab/Tab}
   */


  GridEditor.prototype.getTabByIndex = function (tabNumber) {
    var that = this;
    return that.$tabs[tabNumber];
  };
  /**
   * Get the Command based on the tab number.
   * @param {Number} tabNumber
   * @returns {module:bajaux/commands/Command}
   */


  GridEditor.prototype.getCommand = function (tabNumber) {
    var that = this,
        tab = that.getTabByIndex(tabNumber),
        value;

    if (tab && tab.widget && tab.widget.value) {
      value = tab.widget.value();

      if (value instanceof Command) {
        return value;
      }
    }

    return null;
  };
  /**
   * Get the Command based on the title.
   * @param {String} tabTitle
   * @returns {module:nmodule/webChart/rc/tab/Tab}
   */


  GridEditor.prototype.getTab = function (tabTitle) {
    var that = this,
        i,
        tab;

    for (i = 0; i < that.$tabs.length; i++) {
      tab = that.$tabs[i];

      if (tab.title === tabTitle) {
        return tab;
      }
    }
  };
  /**
   * Add a Tab.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @return {module:nmodule/webChart/rc/tab/Tab}
   */


  GridEditor.prototype.addTab = function (tab) {
    this.$tabs.push(tab);
    return tab;
  };
  /**
   * Replace a Tab.
   * @param {module:nmodule/webChart/rc/tab/Tab} tab
   * @param {Number} index
   */


  GridEditor.prototype.replaceTab = function (tab, index) {
    this.$tabs[index] = tab;
  };
  /**
   * Return the array of tabs.
   * @returns {Array.<module:nmodule/webChart/rc/tab/Tab>}
   */


  GridEditor.prototype.getTabs = function () {
    return this.$tabs;
  };
  /**
   * Read all the values for all the tabs.
   * @returns {Promise} All resolutions are organized in a single resolve Array.
   */


  GridEditor.prototype.doRead = function () {
    var that = this,
        reads = [],
        i;

    for (i = 0; i < that.$tabs.length; i++) {
      reads.push(that.$tabs[i].read());
    }

    return Promise.all(reads);
  };
  /**
   * Save all the values for all the tabs.
   * @returns {Promise} All resolutions are organized in a single resolve Array.
   */


  GridEditor.prototype.doSave = function () {
    var that = this,
        saves = [],
        i;

    for (i = 0; i < that.$tabs.length; i++) {
      saves.push(that.$tabs[i].save());
    }

    return Promise.all(saves);
  };
  /**
   * Load all the tabs values.
   * @returns {Promise}
   */


  GridEditor.prototype.doLoad = function () {
    var that = this,
        loads = $.map(that.$tabs, function (tab, index) {
      var tabDom = that.getTabContentElement(tab);

      if (tabDom.length === 0) {
        return reject("load cannot find dom for tab: " + tab.title + ", " + index);
      }

      if (tab.widget instanceof Widget) {
        tab.dom = tabDom;
        return that.loadTab(tab, index);
      } else {
        return reject("load needs a widget for tab: " + tab.title + ", " + index);
      }
    });
    return Promise.all(loads);
  };

  GridEditor.prototype.doDestroy = function (params) {
    var that = this,
        destroys = $.map(that.$tabs, function (tab, index) {
      var tabDom = that.getTabContentElement(tab);

      if (tabDom.length === 0) {
        return reject("destroy cannot find dom for tab: " + tab.title + ", " + index);
      }

      if (tab.widget instanceof Widget) {
        tab.dom = tabDom;
        return tab.destroy();
      } else {
        return reject("destroy needs a widget for tab: " + tab.title + ", " + index);
      }
    });
    that.$tabs = [];
    return Promise.all(destroys);
  };
  /** @returns {Promise} */


  function reject(msg) {
    return Promise.reject(new Error(msg));
  }

  return GridEditor;
});
