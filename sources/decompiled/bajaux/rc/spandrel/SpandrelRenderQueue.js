function _toConsumableArray(arr) { return _arrayWithoutHoles(arr) || _iterableToArray(arr) || _unsupportedIterableToArray(arr) || _nonIterableSpread(); }

function _nonIterableSpread() { throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method."); }

function _unsupportedIterableToArray(o, minLen) { if (!o) return; if (typeof o === "string") return _arrayLikeToArray(o, minLen); var n = Object.prototype.toString.call(o).slice(8, -1); if (n === "Object" && o.constructor) n = o.constructor.name; if (n === "Map" || n === "Set") return Array.from(o); if (n === "Arguments" || /^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)) return _arrayLikeToArray(o, minLen); }

function _iterableToArray(iter) { if (typeof Symbol !== "undefined" && Symbol.iterator in Object(iter)) return Array.from(iter); }

function _arrayWithoutHoles(arr) { if (Array.isArray(arr)) return _arrayLikeToArray(arr); }

function _arrayLikeToArray(arr, len) { if (len == null || len > arr.length) len = arr.length; for (var i = 0, arr2 = new Array(len); i < len; i++) { arr2[i] = arr[i]; } return arr2; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/**
 * @copyright 2021 Tridium, Inc. All Rights Reserved.
 */

/**
 * API Status: **Private**
 * @module bajaux/spandrel/SpandrelRenderQueue
 */
define(['Promise', 'nmodule/js/rc/switchboard/switchboard'], function (Promise, switchboard) {
  'use strict';

  var resolved = Promise.resolve();

  var resolveInstantly = function resolveInstantly() {
    return resolved;
  };

  var ignore = function ignore() {};
  /**
   * Let's enumerate the concurrency rules for a dynamic spandrel widget.
   *
   * There are two lowercase-e events that trigger a render cycle: a rerender()
   * call in response to user changes *within* the widget, and a load() call
   * in response to programmatic changes from *outside* the widget. If these
   * happen at the same time, they must not step on each other (NCCB-54825). In
   * addition, load() is authoritative: if a load() comes in, it *must* run a
   * cycle by itself, regardless of whether a render is currently happening.
   *
   * rerender() calls can coalesce: if 10 people call rerender() at once then
   * only one render cycle need be done. This cycle must wait for any ongoing
   * cycles triggered by load() to complete before it itself begins.
   *
   * load() calls *cannot* coalesce. If 10 people call load() with different
   * values, then they each need to queue up and execute sequentially. Each
   * one must wait for any ongoing cycles to complete before the next one
   * starts.
   *
   * Essentially, load() and rerender() calls should go into the same queue.
   * load() should queue, and rerender() should preempt. Because switchboard does
   * not have enough granularity to do queue-sometimes/preempt-sometimes, we
   * implement this by hand.
   *
   * @class
   * @alias module:bajaux/spandrel/SpandrelRenderQueue
  */


  return /*#__PURE__*/function () {
    function SpandrelRenderQueue(owner) {
      _classCallCheck(this, SpandrelRenderQueue);

      switchboard(this, {
        '$poke': {
          allow: 'oneAtATime',
          onRepeat: 'queue'
        }
      });
      this.$owner = owner;
      /** @type {Array.<module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest>} */

      this.$q = [];
    }
    /**
     * @param {function(): Promise} doWork
     * @returns {Promise}
     */


    _createClass(SpandrelRenderQueue, [{
      key: "fromLoad",
      value: function fromLoad(doWork) {
        var renderRequest = this.$makeRequest('load', doWork);
        this.$preemptTrailingRerenders(renderRequest);
        return this.$queueAndWaitForWork(renderRequest);
      }
      /**
       * @param {function(): Promise} doWork
       * @returns {Promise}
       */

    }, {
      key: "fromRerender",
      value: function fromRerender(doWork) {
        var renderRequest = this.$makeRequest('rerender', doWork);
        this.$preemptTrailingRerenders(renderRequest);
        return this.$queueAndWaitForWork(renderRequest);
      }
      /**
       * @param {function(): Promise} doWork
       * @returns {Promise}
       */

    }, {
      key: "fromRead",
      value: function fromRead(doWork) {
        var renderRequest = this.$makeRequest('read', doWork);
        return this.$queueAndWaitForWork(renderRequest);
      }
      /**
       * When a rerender request comes from an update to bound state, the actual
       * work of doing the state updates must always occur before the rerender.
       * Call this to ensure that the state update always happens, even if the
       * rerender itself may be preempted by another rerender.
       *
       * @param {function(): Promise} doStateUpdate
       * @param {function(): Promise} doRerender
       * @returns {Promise}
       */

    }, {
      key: "fromStateUpdate",
      value: function fromStateUpdate(doStateUpdate, doRerender) {
        var renderRequest = this.$makeRequest('stateUpdate', doRerender);
        renderRequest.$nonPreemptible = doStateUpdate;
        this.$preemptTrailingRerenders(renderRequest);
        return this.$queueAndWaitForWork(renderRequest);
      }
      /**
       * Cancel all rerender work at the end of the queue and "fold" the deferred
       * promises into the most recent request. This way, all queued rerender
       * requests "fold" into a single rerender cycle.
       * @private
       * @param {module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest} newRequest
       */

    }, {
      key: "$preemptTrailingRerenders",
      value: function $preemptTrailingRerenders(newRequest) {
        var queue = this.$q;
        var queuedRequest;
        var i = queue.length;

        while ((queuedRequest = queue[--i]) && isPreemptible(queuedRequest)) {
          var _newRequest$resolves, _newRequest$rejects;

          (_newRequest$resolves = newRequest.resolves).unshift.apply(_newRequest$resolves, _toConsumableArray(queuedRequest.resolves));

          (_newRequest$rejects = newRequest.rejects).unshift.apply(_newRequest$rejects, _toConsumableArray(queuedRequest.rejects));

          queuedRequest.doWork = resolveInstantly;
          queuedRequest.resolves = [];
          queuedRequest.rejects = [];
        }
      }
      /**
       * @private
       * @param {string} reason
       * @param {function(): Promise} doWork
       * @returns {module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest}
       */

    }, {
      key: "$makeRequest",
      value: function $makeRequest(reason, doWork) {
        var request = {
          reason: reason,
          doWork: doWork
        }; // eslint-disable-next-line promise/avoid-new

        request.promise = new Promise(function (resolve, reject) {
          request.resolves = [resolve];
          request.rejects = [reject];
        }); // noinspection JSValidateTypes

        return request;
      }
      /**
       * @private
       * @param {module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest} request
       * @returns {Promise} to be resolved whenever the queue gets around to doing
       * this work
       */

    }, {
      key: "$queueAndWaitForWork",
      value: function $queueAndWaitForWork(request) {
        this.$q.push(request);
        this.$poke()["catch"](ignore);
        return request.promise;
      }
      /**
       * This function exists only to be switchboarded - just call it to ensure
       * that we're actually working through the render queue. Calling it multiple
       * times won't trigger any actual extra work.
       *
       * @private
       * @returns {Promise}
       */

    }, {
      key: "$poke",
      value: function $poke() {
        return this.$drainQueue();
      }
      /**
       * Do the actual work of performing all the requested work in the queue.
       *
       * @private
       * @returns {Promise}
       */

    }, {
      key: "$drainQueue",
      value: function $drainQueue() {
        var _this = this;

        var nextRequest = this.$getNext();

        if (!nextRequest) {
          return resolved;
        }

        return this.$doWork(nextRequest).then(function () {
          return _this.$drainQueue();
        });
      }
      /**
       * Perform the requested work. This will resolve the deferred promise
       * associated with that request.
       *
       * @private
       * @param {module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest} request
       * @returns {Promise}
       */

    }, {
      key: "$doWork",
      value: function $doWork(request) {
        var doWork = request.doWork,
            resolves = request.resolves,
            rejects = request.rejects,
            $nonPreemptible = request.$nonPreemptible;
        return Promise.resolve($nonPreemptible && $nonPreemptible()).then(function () {
          return doWork();
        }).then(function (result) {
          resolves.forEach(function (resolve) {
            return resolve(result);
          });
        }, function (err) {
          rejects.forEach(function (reject) {
            return reject(err);
          });
        });
      }
      /**
       * @private
       * @returns {module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest|undefined}
       */

    }, {
      key: "$getNext",
      value: function $getNext() {
        return this.$q.shift();
      }
    }]);

    return SpandrelRenderQueue;
  }();

  function isPreemptible(renderRequest) {
    switch (renderRequest.reason) {
      case 'rerender':
      case 'stateUpdate':
        return true;

      default:
        return false;
    }
  }
  /**
   * A request from a spandrel widget to perform rendering work. This is
   * essentially a Deferred, to be resolved when the queue gets around to doing
   * the rendering work.
   *
   * @typedef module:bajaux/spandrel/SpandrelRenderQueue~RenderRequest
   * @property {string} reason why are we doing this work? `load` or `rerender`
   * @property {function(): Promise} doWork
   * @property {Array.<function()>} resolves
   * @property {Array.<function()>} rejects
   * @property {Promise} promise
   */

});
