/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/tab/Tab
 */
define(['baja!', 'Promise', 'bajaux/Widget', 'bajaux/commands/Command', 'bajaux/util/CommandButton', 'nmodule/webChart/rc/webChartUtil', 'bajaux/events'], function (baja, Promise, Widget, Command, CommandButton, webChartUtil, events) {
  "use strict";
  /**
   * A Tab encapsulates the data required for rendering a widget in an object.
   *
   * @class
   * @alias module:nmodule/webChart/rc/tab/Tab
   * @param {Object} obj the object literal for the method's arguments.
   * @param {Widget|Command} obj.item the Widget or Command
   * @param {Function|baja.Value} [obj.value] For Widget items, either a Function that returns or resolves to a value, or just a value.
   * @param {String} obj.title Unique title compared to other sibling tabs
   * @param {Function} [obj.modified] Optional Function for providing a modification callback of Widget or completion of invocation.
   * @param {Function} [obj.loaded] Optional Function for providing a loaded callback of Widget or completion of invocation.
   * @param {jquery} [obj.dom] Optional dom for rendering the Widget, this is usually not required since initialization
   *                       passes in its own down to override this one. If initialization is called without dom,
   *                       then this dom can be preset.
   * @param {Tab} [obj.cloneTab] Optional Tab for populating item, dom, title, and value parameters if params are
   *                             not already present.
   */

  var Tab = function Tab(obj) {
    var that = this,
        item = obj.item,
        dom = obj.dom,
        title = obj.title,
        value = obj.value,
        modified = obj.modified,
        loaded = obj.loaded,
        cloneTab = obj.cloneTab;

    if (cloneTab) {
      if (cloneTab.item) {
        item = obj.cloneTab.item;
      }

      if (cloneTab.dom) {
        dom = obj.cloneTab.dom;
      }

      if (cloneTab.title && !title) {
        title = obj.cloneTab.title;
      }

      if (cloneTab.value && !value) {
        value = obj.cloneTab.value;
      }
    }

    if (!item) {
      throw new Error("tab must have item:" + title);
    }

    if (!title || !title.length) {
      throw new Error("tab must have title:" + item.constructor);
    }

    if (item instanceof Widget) {
      that.widget = item;
      that.dom = dom;
      that.value = value;
      that.title = title;
    } else if (item instanceof Command) {
      that.widget = new CommandButton();
      that.dom = dom;
      that.value = item;
      that.title = title;
    } else {
      throw new Error("must be Widget or Command:" + title);
    }

    that.safeTitle = webChartUtil.safeClassName(title);
    that.modified = modified;
    that.loaded = loaded;
  };
  /**
   * Initialize the dom, parameters are optional because the dom may already be stored in the Tab. Passing a new dom
   * into this function will change the dom.
   * @param {JQuery} newDom
   * @returns {Promise}
   */


  Tab.prototype.initialize = function (newDom) {
    var that = this;

    if (newDom !== undefined && newDom !== null) {
      that.dom = newDom;
    }

    return that.widget.initialize(that.dom).then(function () {
      that.widget.jq().on(events.MODIFY_EVENT, function (e) {
        if (that.modified) {
          var promise = webChartUtil.fullValidate(that.widget);
          promise.then(function (currentRead) {
            that.modified(currentRead, e);
          })["catch"](function (err) {
            baja.error(err); //TODO: css for showing error in red?

            throw err;
          });
        }

        return false;
      });
      that.widget.jq().on(events.LOAD_EVENT, function (e) {
        if (that.loaded) {
          that.loaded(arguments);
        }
      });
    });
  };
  /**
   * Load a value on the underlying widget, parameters are optional because the value may already be stored in the tab. Passing a new value
   * into this function will change the value,
   * @param {Object|Function} [newValue]
   * @returns {Promise}
   */


  Tab.prototype.load = function (newValue) {
    var that = this;

    if (newValue !== undefined && newValue !== null) {
      that.value = newValue;
    }

    if (that.value instanceof Function) {
      var resultFromFunction = that.value();
      return Promise.resolve(resultFromFunction).then(function (resultFromPromise) {
        return that.widget.load(resultFromPromise);
      });
    } else if (typeof (that.value && that.value.then) === 'function') {
      //is promise
      return Promise.resolve(that.value).then(function (resultFromPromise) {
        return that.widget.load(resultFromPromise);
      });
    }

    return that.widget.load(that.value);
  };
  /**
   * Accessor for calling read() on the underlying widget. If the read is for a complex, a newCopy of the Complex
   * will be returned insted of the ComplexDiff.
   * @returns {Promise}
   */


  Tab.prototype.read = function () {
    return webChartUtil.fullRead(this.widget);
  };
  /**
   * Accessor for calling save() on the underlying widget.
   * @returns {Promise}
   */


  Tab.prototype.save = function () {
    return this.widget.save();
  };
  /**
   * Get the title identifier.
   * @returns {String}
   */


  Tab.prototype.getTitle = function () {
    return this.title;
  };
  /**
   * Get the title that is safe for using as a css className.
   * @returns {String}
   */


  Tab.prototype.getSafeTitle = function () {
    return webChartUtil.safeClassName(this.title);
  };
  /**
   * Get the escaped title (safe for appending to a className).
   * @returns {String}
   */


  Tab.prototype.getTitleDisplay = function () {
    return baja.SlotPath.unescape(this.title);
  };

  Tab.prototype.destroy = function (params) {
    return this.widget.destroy(params);
  };

  return Tab;
});
