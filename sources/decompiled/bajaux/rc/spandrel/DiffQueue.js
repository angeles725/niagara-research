function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/*
 * @copyright 2022 Tridium, Inc. All Rights Reserved.
 */

/**
 * API Status: **Private**
 * @module bajaux/spandrel/DiffQueue
 */
define(['Promise'], function (Promise) {
  'use strict';

  var resolved = Promise.resolve();

  var noop = function noop() {
    return resolved;
  };

  var FINALIZED = Symbol('finalized');
  /**
   * Represents all the work to be done in one diffing phase.
   *
   * There are five possible "buckets" for diffing work to go into:
   *
   * - "before" - deleting child widgets happens here because they have to go first for the widget
   *   structure to be consistent.
   * - "prework" - work that happens regardless of what else happens to the child widgets. dom
   *   updates happen here.
   * - "work" - updates to child widgets such as properties, enabled, readonly, and formFactor.
   *   these changes will define "postwork" as a rerender because the widget must rerender to
   *   reflect these changes.
   * - "postwork" - see "work" - this will be a rerender or load, depending on whether a load() was
   *   queued or not. a rebuild will go here, and will also cancel all "work" - if you're tearing
   *   down the child widget and rebuilding it, there is no reason to do any other work on it.
   * - "after" - adding child widgets happens here because when keys change you can get both a
   *   rebuild and an add for the same key.
   *
   * @private
   * @alias module:bajaux/spandrel/DiffQueue
   */

  var DiffQueue = /*#__PURE__*/function () {
    function DiffQueue() {
      _classCallCheck(this, DiffQueue);

      this.$ops = {};
    }
    /**
     * @private
     * @param {string} key
     * @returns {DiffOp}
     */


    _createClass(DiffQueue, [{
      key: "$get",
      value: function $get(key) {
        var ops = this.$ops;
        return ops[key] || (ops[key] = new DiffOp());
      }
      /**
       * Queues up some work. All work will be performed and cannot be cancelled.
       *
       * Call this for DOM updates (except when the tag itself changes, as this requires a rebuild.)
       *
       * @param {string} key
       * @param {function} doWork
       */

    }, {
      key: "queue",
      value: function queue(key, doWork) {
        this.$get(key).queuePrework(doWork);
      }
      /**
       * Queues up some work. The work will be performed, and the diffs will wrap up with a rerender -
       * unless cancelled by a load or rebuild.
       *
       * Call this for updates to Widget state attributes that can wrap up into a single rerender,
       * like properties, readonly, enabled, and formFactor.
       *
       * @param {string} key
       * @param {function} doWork
       * @param {function} doRerender
       */

    }, {
      key: "queueForRerender",
      value: function queueForRerender(key, doWork, doRerender) {
        var op = this.$get(key);
        op.queueWork(doWork);
        op.$postwork = [doRerender];
      }
      /**
       * Queues up some work. The work will be performed, and any queued rerenders will be cancelled
       * (because a load() supersedes a rerender()). This work can only be cancelled by a rebuild.
       *
       * Call this for changes to Widget value.
       *
       * @param {string} key
       * @param {function} doWork
       */

    }, {
      key: "queueForLoad",
      value: function queueForLoad(key, doWork) {
        var op = this.$get(key);

        if (!op[FINALIZED]) {
          op.doPostwork = doWork;
        }
      }
      /**
       * Queues up some work. This work will always be performed, and will cancel out any queued
       * rerender or load work.
       *
       * Call this when the widget's type changes, or when the existing widget is otherwise not
       * reusable and must be destroyed and rebuilt.
       *
       * @param {string} key
       * @param {function} doWork
       */

    }, {
      key: "queueForRebuild",
      value: function queueForRebuild(key, doWork) {
        var op = this.$get(key);
        op.doWork = noop;
        op.doPostwork = doWork;
        op[FINALIZED] = true;
      }
      /**
       * Queues up an add. This happens last because it can happen after a rebuild operation for the
       * same key.
       * @param {string} key
       * @param {function} doAdd
       */

    }, {
      key: "queueAdd",
      value: function queueAdd(key, doAdd) {
        this.$get(key).after = doAdd;
      }
      /**
       * Queues up a delete. This happens first because it could erroneously wipe out legitimate DOM
       * changes after an edit for the same key.
       * @param {string} key
       * @param {function} doDelete
       */

    }, {
      key: "queueDelete",
      value: function queueDelete(key, doDelete) {
        this.$get(key).before = doDelete;
      }
      /**
       * Queues up work to be done after _all_ other `bajaux` work is done. This is for any work that
       * spandrel would perform on a DOM element _outside_ of the bajaux workflow, because it's the
       * bajaux workflow itself that would give us the DOM element to work with in the first place.
       *
       * @param {string} key
       * @param {function} doWork
       */

    }, {
      key: "queuePostBajaux",
      value: function queuePostBajaux(key, doWork) {
        // ATM the only post-bajaux work is updating event handlers. if we ever need to do more than
        // that, push onto an async queue instead of just wiping finalize.
        this.$get(key).finalize = doWork;
      }
      /**
       * Cancels all diffing for the given key.
       * @param {string} key
       */

    }, {
      key: "nullify",
      value: function nullify(key) {
        this.$get(key).doDiff = noop;
      }
      /**
       * Call after queuing up all necessary work to perform all the work for this diffing phase.
       * @returns {Promise}
       */

    }, {
      key: "doDiffs",
      value: function doDiffs() {
        var ops = this.$ops;
        return Object.keys(ops).reduce(function (prom, key) {
          return prom.then(function () {
            return ops[key].doDiff();
          });
        }, Promise.resolve());
      }
    }]);

    return DiffQueue;
  }();
  /**
   * Represents the diffing of exactly one widget.
   * @private
   */


  var DiffOp = /*#__PURE__*/function () {
    function DiffOp(ticks) {
      _classCallCheck(this, DiffOp);

      this.$ticks = ticks;
      this.$prework = [];
      this.$work = [];
      this.$postwork = [];
    }
    /**
     * @returns {Promise} work to do first
     */


    _createClass(DiffOp, [{
      key: "before",
      value: function before() {
        return Promise.resolve();
      }
      /**
       * @returns {Promise} work to do after
       */

    }, {
      key: "after",
      value: function after() {
        return Promise.resolve();
      }
      /**
       * @returns {Promise} work that can only be done after all other delete/update/add work is done.
       * non-bajaux work on DOM elements goes here.
       */

    }, {
      key: "finalize",
      value: function finalize() {
        return Promise.resolve();
      }
    }, {
      key: "queuePrework",
      value: function queuePrework(doWork) {
        this.$prework.push(doWork);
      }
      /**
       * Queue up a bit of work. Any updates that don't require a complete teardown and rebuild of the
       * widget (such as setReadonly() or load()) should go in here.
       * @param {function} doWork
       */

    }, {
      key: "queueWork",
      value: function queueWork(doWork) {
        this.$work.push(doWork);
      }
    }, {
      key: "doPrework",
      value: function doPrework() {
        return doInSerial(this.$prework);
      }
    }, {
      key: "doWork",
      value: function doWork() {
        return doInSerial(this.$work);
      }
    }, {
      key: "doPostwork",
      value: function doPostwork() {
        return doInSerial(this.$postwork);
      }
      /**
       * @returns {Promise} the whole diffing process for this widget, including the before and after
       */

    }, {
      key: "doDiff",
      value: function doDiff() {
        var _this = this;

        return this.before().then(function () {
          return _this.doPrework();
        }).then(function () {
          return _this.doWork();
        }).then(function () {
          return _this.doPostwork();
        }).then(function () {
          return _this.after();
        }).then(function () {
          return _this.finalize();
        });
      }
    }]);

    return DiffOp;
  }();

  function doInSerial(funcs) {
    return funcs.reduce(function (prom, func) {
      return prom.then(func);
    }, Promise.resolve());
  }

  return DiffQueue;
});
