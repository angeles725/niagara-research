/**
 * @copyright 2015, Tridium, Inc. All Rights Reserved.
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/LockAxisCommand
 */
define(['bajaux/commands/Command', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/model/modelUtil'], function (Command, chartEvents, modelUtil) {
  "use strict";
  /**
   * LockAxisCommand is a Command that provides easy access to lock the axis
   *
   * @class
   * @alias module:nmodule/webChart/rc/command/LockAxisCommand
   * @extends module:bajaux/commands/Command
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   */

  var LockAxisCommand = function LockAxisCommand(widget, baseScale) {
    var that = this,
        key = baseScale.isLocked() ? "webChart.UnlockAxisCommand" : "webChart.LockAxisCommand";
    Command.call(that, {
      module: "webChart",
      lex: key,
      func: function func() {
        baseScale.setLocked(!baseScale.isLocked());
        return widget.settings().valueScaleSettingsDisplay(baseScale).then(function (scaleInfo) {
          var min = scaleInfo.get("min"),
              max = scaleInfo.get("max"),
              domain = baseScale.scale().domain(),
              oldMin = domain[0],
              oldMax = domain[1];

          if (min !== oldMin || max !== oldMax) {
            baseScale.scale().domain(modelUtil.stretchDomain(widget.model(), [min, max]));
          }

          widget.jq().trigger(chartEvents.VALUE_SCALE_CHANGED, baseScale);
          widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
        });
      }
    });
  };

  LockAxisCommand.prototype = Object.create(Command.prototype);
  LockAxisCommand.prototype.constructor = LockAxisCommand;
  return LockAxisCommand;
});
