/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam and Gareth Johnson
 */
define([], function () {
  "use strict";
  /**
   * A list of all bajaux related events.
   * @exports bajaux/events
   */

  var exports = {
    //
    // Properties
    //

    /** Triggers when a Property is added **/
    PROPERTY_ADDED: 'bajaux:propertyadded',

    /** Triggers when a Property is removed **/
    PROPERTY_REMOVED: 'bajaux:propertyremoved',

    /** Triggers when a Property is changed **/
    PROPERTY_CHANGED: "bajaux:propertychanged",

    /** Triggers when a Property's metadata is changed **/
    METADATA_CHANGED: "bajaux:metadatachanged",
    //    
    // Widget
    //

    /** Triggers when Widget is enabled. */
    ENABLE_EVENT: 'bajaux:enable',

    /** Triggers when Widget is enabled and its callbacks fail. */
    ENABLE_FAIL_EVENT: 'bajaux:enablefail',

    /** Triggers when Widget is disabled. */
    DISABLE_EVENT: 'bajaux:disable',

    /** Triggers when Widget is disabled and its callbacks fail. */
    DISABLE_FAIL_EVENT: 'bajaux:disablefail',

    /** Triggers when a Widget is destroyed. */
    DESTROY_EVENT: 'bajaux:destroy',

    /** Triggers when Widget is destroyed and its callbacks fail. */
    DESTROY_FAIL_EVENT: 'bajaux:destroyfail',

    /** Triggers when Widget initializes. */
    INITIALIZE_EVENT: 'bajaux:initialize',

    /** Triggers when Widget tries to initialize, but fails. */
    INITIALIZE_FAIL_EVENT: 'bajaux:initializefail',

    /** Triggers when the layout has changed. **/
    LAYOUT_EVENT: 'bajaux:layout',
    //
    // Widget - Editor
    //

    /** Triggers when an Widget is modified. */
    MODIFY_EVENT: 'bajaux:modify',

    /** Triggers when an Widget is unmodified. */
    UNMODIFY_EVENT: 'bajaux:unmodify',

    /** Triggers when an Widget loads a value. */
    LOAD_EVENT: 'bajaux:load',

    /** Triggers when an Widget tries to load a value, but fails. */
    LOAD_FAIL_EVENT: 'bajaux:loadfail',

    /** Triggers when an Widget saves. */
    SAVE_EVENT: 'bajaux:save',

    /** Triggers when an Widget tries to save, but fails. */
    SAVE_FAIL_EVENT: 'bajaux:savefail',

    /** Triggers when an Widget validates and finds a valid value. */
    VALID_EVENT: 'bajaux:valid',

    /** Triggers when an Widget validates and finds an invalid value. */
    INVALID_EVENT: 'bajaux:invalid',

    /** Triggers when an Widget's validators are modified. */
    VALIDATORS_MODIFIED: 'bajaux:validatorsmodified',

    /** Triggers when an Widget is made readonly. */
    READONLY_EVENT: 'bajaux:readonly',

    /** Triggers when an Widget is made readonly and its callbacks fail. */
    READONLY_FAIL_EVENT: 'bajaux:readonlyfail',

    /** Triggers when an Widget is made writable. */
    WRITABLE_EVENT: 'bajaux:writable',

    /** Triggers when an Widget is made writable and its callbacks fail. */
    WRITABLE_FAIL_EVENT: 'bajaux:writablefail',
    //
    // Commands
    //
    command: {
      /** Triggered when the command changes. */
      CHANGE_EVENT: 'bajaux:changecommand',

      /** Triggered when a toggle command's selection state changes */
      SELECTION_EVENT: 'bajaux:selectioncommand',

      /** Triggered when the command is invoked. */
      INVOKE_EVENT: 'bajaux:invokecommand',

      /** Triggered when the command tries to invoke, but fails. */
      FAIL_EVENT: 'bajaux:failcommand',

      /** Triggered when a command group changes. */
      GROUP_CHANGE_EVENT: 'bajaux:changecommandgroup'
    }
  };
  /** All of the failure events for bajaux. These events are
      fired whenever something has gone wrong. */

  exports.FAILURE_EVENTS = [exports.ENABLE_FAIL_EVENT, exports.DISABLE_FAIL_EVENT, exports.INITIALIZE_FAIL_EVENT, exports.DESTROY_FAIL_EVENT, exports.LOAD_FAIL_EVENT, exports.SAVE_FAIL_EVENT, exports.READONLY_FAIL_EVENT, exports.WRITABLE_FAIL_EVENT, exports.command.FAIL_EVENT];
  return exports;
});
