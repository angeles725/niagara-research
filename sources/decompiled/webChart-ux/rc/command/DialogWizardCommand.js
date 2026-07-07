/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/DialogWizardCommand
 */
define(['Promise', 'baja!', 'log!nmodule.webChart.rc.command.DialogWizardCommand', 'dialogs', 'bajaux/commands/Command'], function (Promise, baja, log, dialogs, Command) {
  "use strict";

  var logSevere = log.severe.bind(log);
  /**
   * DialogWizardCommand helps developers create field editors that allow users to select data
   *
   * @class
   * @alias module:nmodule/webChart/rc/command/DialogWizardCommand
   * @extends module:bajaux/commands/Command
   */

  var DialogWizardCommand = function DialogWizardCommand(chartWidget) {
    var lexParams = this.lexParams(),
        that = this;
    Command.call(this, {
      module: lexParams.module,
      lex: lexParams.lex,
      func: function func() {
        return that.resolveTab().then(function (tab) {
          return that.dialogCycle(tab);
        });
      }
    });
  };

  DialogWizardCommand.prototype = Object.create(Command.prototype);
  DialogWizardCommand.prototype.constructor = DialogWizardCommand;
  /**
   * Override resolveTab to resolve the first tab or tabs. If an array of tabs is returned, then
   * a TabbedEditor will be used to display tabs in a TabbedEditor for the user to complete.
   * @returns {*|Promise} An optional promise that if resolved to a tab will open another dialog when the
   * prior dialog completes.
   */

  DialogWizardCommand.prototype.dialogCycle = function (tab) {
    var that = this;

    if (!tab) {
      return; //end cycle
    }

    var buttons = that.buttons();
    var dialog = dialogs.showOkCancel({
      title: tab.title,
      content: '<div class="DialogWizardCommand-Container"/>',
      buttons: buttons
    }),
        dom = dialog.content().children(".DialogWizardCommand-Container"); //initialize resolved tab

    tab.initialize(dom).then(function () {
      return tab.load();
    })["catch"](logSevere); //handle Ok

    return dialog.ok(function () {
      return Promise.resolve(that.handleRead(tab)).then(function (anotherTab) {
        if (!anotherTab) {
          return; //wizard complete
        }

        return that.dialogCycle(anotherTab);
      });
    }).cancel(function () {
      return tab.destroy();
    }).promise();
  };
  /**
   * Override buttons to provide additional buttons on the dialog
   * @return {Array.<Object>}
   */


  DialogWizardCommand.prototype.buttons = function () {
    return [];
  };
  /**
   * Override resolveTab to resolve the first tab for the content.
   * @return {Promise}
   */


  DialogWizardCommand.prototype.resolveTab = function () {
    return Promise.resolve(null);
  };
  /**
   * Resolve a new tab if you want to continue the wizard, resolve a Falsy value to stop the
   * dialog wizard.
   * @param tab
   * @returns {*}
   */


  DialogWizardCommand.prototype.handleRead = function (tab) {
    return Promise.resolve("");
  };
  /**
   * Subclass Override to set lexicon parameters
   * @returns {Object}
   */


  DialogWizardCommand.prototype.lexParams = function () {
    return {};
  };

  DialogWizardCommand.prototype.options = function () {
    return baja.Facets.make();
  };

  return DialogWizardCommand;
});
