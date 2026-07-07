function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/**
 * @copyright 2020 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @module bajaux/spandrel/RequestLayoutMixin
 */
define(['bajaux/spandrel/symbols', 'Promise', 'underscore', 'nmodule/js/rc/asyncUtils/asyncUtils'], function (symbols, Promise, _, asyncUtils) {
  'use strict';

  var ROOT_SYMBOL = symbols.ROOT_SYMBOL,
      DEPTH_SYMBOL = symbols.DEPTH_SYMBOL;
  var result = _.result;
  var $deferred = asyncUtils.$deferred,
      waitInterval = asyncUtils.waitInterval;
  var LAYOUT_QUEUE_SYMBOL = Symbol('layoutQueue');
  var LAYOUT_DEFERRED_SYMBOL = Symbol('layoutDeferred');
  var ORIGINAL_DOLAYOUT_SYMBOL = Symbol('originalDoLayout');
  var DOLAYOUT_ERROR = Symbol('doLayoutError');
  var CURRENT_FLUSH_SYMBOL = Symbol('currentFlush');
  var NEXT_FLUSH_SYMBOL = Symbol('nextFlush');
  var LAYOUT_DELAY = 16;
  /**
   * This mixin will be applied to a widget in a spandrel widget tree to make
   * layouts as optimized as possible.
   *
   * With this mixin, a layout() call will not immediately cause the widget to
   * layout. Instead, the widget will go into a queue for widgets waiting for
   * layout. After a certain period has elapsed, this queue will empty, and all
   * waiting widgets will be laid out at once. This stops the n^2 behavior where
   * every widget in a spandrel tree would have layout() called once per layer
   * of depth in the tree.
   *
   * As individual widgets lay themselves out, they will be removed from the
   * queue. This means that if an individual widget calls layout() on its own
   * children, the parent's layout() call won't "double up" and re-layout the
   * children a second time.
   *
   * @mixin
   * @alias module:bajaux/spandrel/RequestLayoutMixin
   */

  var RequestLayoutMixin = /*#__PURE__*/function () {
    function RequestLayoutMixin() {
      _classCallCheck(this, RequestLayoutMixin);
    }

    _createClass(RequestLayoutMixin, [{
      key: "doLayout",

      /**
       * @param {object} [params]
       * @param {boolean} [params.quick] if true, this layout call will not wait
       * for the layout to complete to resolve its promise. This should be set to
       * true on recursive layout calls, as child layouts will "coalesce" into the
       * parent's layout call when the queue empties.
       * @returns {Promise|*}
       */
      value: function doLayout(params) {
        var df = this[LAYOUT_DEFERRED_SYMBOL];
        var quick = params && params.quick;

        if (df) {
          return !quick && df.promise;
        }

        var root = this[ROOT_SYMBOL];
        var queue = root[LAYOUT_QUEUE_SYMBOL] || (root[LAYOUT_QUEUE_SYMBOL] = []);
        enqueueAll(queue, this);
        var flush = requestFlush(root);
        return (!quick || isRoot(this)) && flush;
      }
      /**
       * This callback will be called, if present, just before the queued layouts
       * begin.
       */

    }, {
      key: "layoutStarted",
      value: function layoutStarted() {}
      /**
       * This callback will be called, if present, when all the queued layouts
       * have been called.
       */

    }, {
      key: "layoutFinished",
      value: function layoutFinished() {}
    }]);

    return RequestLayoutMixin;
  }();
  /**
   * @param {module:bajaux/Widget} widget
   * @param {module:bajaux/Widget} [root=widget] the root of the spandrel tree,
   * to be held onto
   * @param {number} [depth=0] this widget's depth in the widget tree (the
   * root widget's depth will be 0)
   * @param {module:bajaux/lifecycle/WidgetManager~error} error called if a
   * widget encounters an error during layout
   */


  function addRequestLayoutSupport(widget, root, depth, error) {
    widget[ROOT_SYMBOL] = root || widget;
    widget[DEPTH_SYMBOL] = depth || 0;

    if (!widget[ORIGINAL_DOLAYOUT_SYMBOL]) {
      widget[ORIGINAL_DOLAYOUT_SYMBOL] = widget.doLayout;
      widget[DOLAYOUT_ERROR] = error;
      widget.doLayout = RequestLayoutMixin.prototype.doLayout;
    }
  }

  function isRoot(widget) {
    return widget === widget[ROOT_SYMBOL];
  }

  function hasMixin(widget) {
    return !!widget[ORIGINAL_DOLAYOUT_SYMBOL];
  }
  /**
   * Put the widget and all of its descendents into the layout queue.
   *
   * @param {Array.<module:bajaux/Widget>} layoutQueue
   * @param {module:bajaux/Widget} widget
   */


  function enqueueAll(layoutQueue, widget) {
    if (!hasMixin(widget)) {
      return;
    }

    widget.getChildWidgets().forEach(function (kid) {
      return enqueueAll(layoutQueue, kid);
    });

    if (!layoutQueue.includes(widget)) {
      widget[LAYOUT_DEFERRED_SYMBOL] = $deferred();
      layoutQueue.push(widget);
    }
  }
  /**
   * For flushing the layout queue, we have to use a "double buffer" so children
   * that call layout() while the current layout queue is being flushed don't
   * get lost.
   *
   * @param {module:bajaux/Widget} root
   * @returns {Promise}
   */


  function requestFlush(root) {
    var currentFlush = root[CURRENT_FLUSH_SYMBOL];

    if (currentFlush) {
      var nextFlush = root[NEXT_FLUSH_SYMBOL];

      if (!nextFlush) {
        nextFlush = root[NEXT_FLUSH_SYMBOL] = currentFlush.then(function () {
          delete root[NEXT_FLUSH_SYMBOL];
          return doFlush(root);
        });
      }

      return nextFlush;
    }

    return root[CURRENT_FLUSH_SYMBOL] = doFlush(root).then(function () {
      root[CURRENT_FLUSH_SYMBOL] = root[NEXT_FLUSH_SYMBOL];
    });
  }
  /**
   * Wait the specified timeout, then flush out the layout queue, performing all
   * requested layouts.
   *
   * @param {module:bajaux/Widget} root
   * @returns {Promise}
   */


  function doFlush(root) {
    return waitInterval(LAYOUT_DELAY).then(function () {
      return performQueuedLayouts(root);
    });
  }
  /**
   * - Sort all queued widgets by depth, so the shallowest widgets layout first.
   * - Whenever a widget lays itself out, remove it from the queue, so it won't
   *   get laid out a second time.
   * - Mark the current queue as empty. (Don't empty it before starting the
   *   layouts, because a layout() call from a child should have no effect if
   *   it's already in the process of being laid out.)
   *
   * @param {module:bajaux/Widget} root
   * @returns {Promise}
   */


  function performQueuedLayouts(root) {
    var layoutQueue = root[LAYOUT_QUEUE_SYMBOL];
    var sorted = sortByDepth(layoutQueue.slice());
    var len = sorted.length; //call layoutStarted from bottom to top

    if (root.isDestroyed()) {
      return Promise.resolve();
    }

    for (var i = 0; i < len; ++i) {
      if (!sorted[i].isDestroyed()) {
        result(sorted[i], 'layoutStarted');
      }
    }

    var promise = Promise.all(sorted.map(tryLayoutNow))["finally"](function () {
      layoutQueue.forEach(function (w) {
        return delete w[LAYOUT_DEFERRED_SYMBOL];
      });
      root[LAYOUT_QUEUE_SYMBOL] = [];
    }); //call layoutFinished from top to bottom

    for (var _i = len - 1; _i >= 0; --_i) {
      result(sorted[_i], 'layoutFinished');
    }

    return promise;
  }

  function sortByDepth(widgets) {
    return widgets.sort(function (a, b) {
      return a[DEPTH_SYMBOL] - b[DEPTH_SYMBOL];
    });
  }
  /**
   * Attempts to invoke the widget's original `doLayout()`.
   *
   * If the layout fails for whatever reason, it will provide the WidgetManager
   * with the failed widget and the widget's error.
   *
   * @param {module:bajaux/Widget} widget
   * @returns {Promise}
   */


  function tryLayoutNow(widget) {
    return Promise["try"](function () {
      return layoutNow(widget);
    })["catch"](function (err) {
      var error = widget[DOLAYOUT_ERROR];

      if (!error) {
        throw err;
      }

      return error(err, widget);
    });
  }
  /**
   * Invoke the widget's original `doLayout()`.
   * @param {module:bajaux/Widget} widget
   * @returns {Promise}
   */


  function layoutNow(widget) {
    var df = widget[LAYOUT_DEFERRED_SYMBOL];

    if (!widget.isInitialized()) {
      df.resolve();
      return df.promise;
    }

    var layoutPromise = Promise.resolve(widget[ORIGINAL_DOLAYOUT_SYMBOL]());
    return df ? layoutPromise.then(df.resolve, df.reject) : layoutPromise;
  }

  return addRequestLayoutSupport;
});
