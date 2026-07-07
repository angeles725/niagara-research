/**
 * @copyright 2015, Tridium, Inc. All Rights Reserved.
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/StopCommand
 */
define(['bajaux/commands/Command'], function (Command) {
  "use strict";
  /**
   * StopCommand is a Command that provides access to Stop the Chart Model from loading
   *
   * @private
   * @class
   * @alias module:nmodule/webChart/rc/command/StopCommand
   * @extends module:bajaux/commands/Command
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   */

  var StopCommand = function StopCommand(widget) {
    var that = this;
    Command.call(that, {
      module: "webChart",
      lex: "webChart.stopCommand",
      selected: false,
      func: function func() {
        var model = widget.model();

        if (!model.isStopped()) {
          model.stop();
        } else {
          model.reloadAll();
        }
      }
    });
  };

  StopCommand.prototype = Object.create(Command.prototype);
  StopCommand.prototype.constructor = StopCommand;
  return StopCommand;
});
