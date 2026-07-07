/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson
 */

/**
 * bajaux Validators are used to validate data.
 * 
 * @module bajaux/Validators
 * @requires Promise
 * @requires bajaux/events
 */
define(['Promise', 'bajaux/events'], function (Promise, events) {
  "use strict";
  /**
   * Manages a collection of Validator functions.
   * @class
   * @alias module:bajaux/Validators
   * 
   * @param eventHandler The associated eventHandler that has a 'trigger' method.
   */

  var Validators = function Validators(eventHandler) {
    var that = this;
    /**
     * The associated Event Handler. This object has a method
     * named 'trigger' used for firing events.
     * @private
     */

    that.$eventHandler = eventHandler;
    /**
     * The internal array of Validators to use.
     * @private
     */

    that.$validators = [];
  };
  /**
   * Called to validate some data. The read value run against each registered
   * Validator.
   *
   * To validate a widget, please call {@link module:bajaux/Widget#validate}
   * instead. This method will extract the read value and then call this method.
   * 
   * This method should not be overridden. Instead Validators functions
   * should be added using {@link module:bajaux/Validators#add}.
   *
   * When saving a modified widget, the widget will be read, validated
   * and then saved. The read data is the current representation of
   * the widget and is passed to the validators. The read data will
   * not be the same as the loaded current value of the widget that
   * is used in the save process.
   * 
   * @see module:bajaux/Widget#read
   * @see module:bajaux/Widget#save
   * 
   * @see module:bajaux/Validators#add
   * @see module:bajaux/Validators#remove
   * @see module:bajaux/Validators#get
   *
   * @param readValue The read value that needs to be validated.
   * @returns {Promise} A promise to be resolved with the validated value
   */


  Validators.prototype.validate = function validate(readValue) {
    var eventHandler = this.$eventHandler,
        validators = this.$validators;

    function isValid() {
      //run all the validators in sequence
      return validators.reduce(function (prom, validator) {
        return prom.then(function () {
          return validator.call(eventHandler, readValue);
        });
      }, Promise.resolve()).then(function () {
        return readValue;
      });
    }

    return isValid().then(function (validValue) {
      eventHandler.trigger(events.VALID_EVENT);
      return validValue;
    }, function (err) {
      eventHandler.trigger(events.INVALID_EVENT, err);
      throw err;
    });
  };
  /**
   * Add a Validator function. This is used to validate
   * a read value before it can be saved.
   *
   * When a Validator function is invoked, the first argument will be the
   * value. If the function throws or returns a promise that rejects, the value
   * is considered to be invalid. If the function returns anything else
   * (including a promise that resolves), the value is considered to be valid.
   *
   * Please note, when saving a modified widget, the value will be read
   * from the widget, then validated and finally saved. Therefore, the 
   * data passed into the validator for validation will the read data.
   *
   * @see module:bajaux/Widget#read
   * @see module:bajaux/Widget#save
   *
   * @see module:bajaux/Validators#validate
   * @see module:bajaux/Validators#remove
   * @see module:bajaux/Validators#get
   * 
   * @param {Function} validator
   * @returns {module:bajaux/Validators}
   * 
   * @example
   * validators.add(function (value) {
   *   if (!isAcceptable(value)) {
   *     return Promise.reject(new Error('value not acceptable'));
   *   }
   * });
   */


  Validators.prototype.add = function add(validator) {
    var that = this,
        eventHandler = that.$eventHandler,
        vs = that.$validators;

    if (vs.indexOf(validator) === -1) {
      vs.push(validator);
      eventHandler.trigger(events.VALIDATORS_MODIFIED);
    }

    return that;
  };
  /**
   * Remove a Validator function.
   *
   * @see module:bajaux/Validators#validate
   * @see module:bajaux/Validators#remove
   * @see module:bajaux/Validators#get
   * 
   * @param {Function} validator
   * @returns {module:bajaux/Validators}
   */


  Validators.prototype.remove = function remove(validator) {
    var i = 0,
        that = this,
        eventHandler = that.$eventHandler,
        vs = that.$validators;

    for (i = 0; i < vs.length; ++i) {
      if (vs[i] === validator) {
        vs.splice(i, 1);
        eventHandler.trigger(events.VALIDATORS_MODIFIED);
        break;
      }
    }

    return that;
  };
  /**
   * Return an array copy of the validators.
   *
   * @see module:bajaux/Validators#validate
   * @see module:bajaux/Validators#add
   * @see module:bajaux/Validators#remove
   * 
   * @returns {Array} array of Validator functions.
   */


  Validators.prototype.get = function get() {
    // Return a copy of the validators array.
    return this.$validators.slice(0);
  };

  return Validators;
});
