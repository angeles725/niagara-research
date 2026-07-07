/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/fe/SimplePropertySheet
 */
define(['nmodule/webEditors/rc/wb/PropertySheet', 'nmodule/webEditors/rc/fe/CompositeEditor', 'lex!webEditors', 'underscore'], function (PropertySheet, CompositeEditor, lex, _) {
  "use strict";
  /**
   * A SimplePropertySheet is the editor for any Component.
   *
   * @class
   * @alias module:nmodule/webChart/rc/fe/SimplePropertySheet
   * @extends module:nmodule/webEditors/rc/wb/PropertySheet
   */

  var SimplePropertySheet = function SimplePropertySheet(params) {
    PropertySheet.call(this, {
      properties: {
        showHeader: false,
        showControls: false,
        nested: true
      }
    });
  };

  SimplePropertySheet.prototype = Object.create(PropertySheet.prototype);
  SimplePropertySheet.prototype.constructor = SimplePropertySheet; //workaround for NCCB-26097:   Adding validator to PropertySheet throws wrong error message when validation error occurs

  SimplePropertySheet.prototype.validate = function () {
    var webEditorsLex = lex[0],
        VALIDATE_FAIL_KEY = 'CompositeEditor.validate.failSlot';
    return CompositeEditor.prototype.validate.apply(this, arguments)["catch"](function (compositeError) {
      if (!compositeError.map) {
        throw compositeError;
      }

      var errDetails = _.map(compositeError.map, function (err, key) {
        return webEditorsLex.get(VALIDATE_FAIL_KEY, key, err);
      });

      throw errDetails.join('<br/>\n');
    });
  };

  return SimplePropertySheet;
});
