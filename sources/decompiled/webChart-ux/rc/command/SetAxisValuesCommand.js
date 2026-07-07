/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/SetAxisValuesCommand
 */
define(['Promise', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/command/DialogWizardCommand', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/tab/Tab', 'nmodule/webChart/rc/tab/TabbedEditor', 'nmodule/webChart/rc/model/modelUtil', 'nmodule/webChart/rc/fe/SimplePropertySheet'], function (Promise, webChartUtil, DialogWizardCommand, chartEvents, Tab, TabbedEditor, modelUtil, SimplePropertySheet) {
  "use strict";
  /**
   * SetAxisValuesCommand helps users add additional histories to a view.
   *
   * @class
   * @alias module:nmodule/webChart/rc/command/SetAxisValuesCommand
   * @extends module:bajaux/commands/Command
   */

  var SetAxisValuesCommand = function SetAxisValuesCommand(widget, selectedBaseScale) {
    var that = this;
    that.$widget = widget;
    that.$selectedBaseScale = selectedBaseScale;
    DialogWizardCommand.apply(this, arguments);
  };

  SetAxisValuesCommand.prototype = Object.create(DialogWizardCommand.prototype);
  SetAxisValuesCommand.prototype.constructor = SetAxisValuesCommand;

  SetAxisValuesCommand.prototype.buttons = function () {
    var that = this,
        widget = that.$widget;
    return [{
      name: "reset",
      displayName: webChartUtil.lex.get("reset"),
      handler: function handler() {
        widget.settings().resetValueScaleSettings();
        widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
        widget.jq().trigger(chartEvents.VALUE_SCALE_CHANGED);
      }
    }];
  };

  function minBeforeMax(value) {
    var min = value.get('min'),
        max = value.get('max');

    if (min >= max) {
      throw new Error(webChartUtil.lex.get("webChart.minMaxFailure", min, max));
    } else if (max - min < 0.01) {
      throw new Error(webChartUtil.lex.get("webChart.minMaxFailure.tooClose", min, max));
    }
  }
  /**
   * Setup the axisListTab.
   *
   * @param {bajaux.Widget} widget
   * @param {module:nmodule/webChart/rc/model/BaseScale} [selectedBaseScale]
   * @returns {module:nmodule/webChart/rc/tab/Tab}
   */


  SetAxisValuesCommand.axisListTab = function (widget, selectedBaseScale) {
    var axisTabbedEditor = new TabbedEditor(),
        settings = widget.settings(),
        i;

    axisTabbedEditor.save = function () {
      return TabbedEditor.prototype.save.apply(this, arguments).then(function () {
        for (i = 0; i < widget.model().valueScales().length; i++) {
          var baseScale = widget.model().valueScales()[i];

          if (baseScale.seriesList().length === 0) {
            continue;
          }

          var scaleInfo = widget.settings().valueScaleSettings(baseScale),
              min = scaleInfo.get("min"),
              max = scaleInfo.get("max"),
              domain = baseScale.scale().domain(),
              oldMin = domain[0],
              oldMax = domain[1];

          if (min !== oldMin || max !== oldMax) {
            if (!baseScale.isLocked()) {
              widget.manualZoom();
            }

            baseScale.scale().domain(modelUtil.stretchDomain(widget.model(), [min, max]));
          }

          widget.jq().trigger(chartEvents.VALUE_SCALE_CHANGED, baseScale);
          widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
        }
      });
    };

    widget.settings().load();

    function addValidator(sheet, valueScale) {
      sheet.validators().add(function (diff) {
        var scaleInfo = settings.valueScaleSettings(valueScale);
        var value = scaleInfo.newCopy();
        return diff.apply(value).then(function (result) {
          minBeforeMax(result); //ensure unique display names

          for (i = 0; i < widget.model().valueScales().length; i++) {
            var displayName = result.get("displayName");

            if (!displayName) {
              throw new Error(webChartUtil.lex.get("webChart.blankDisplayName"));
            }

            if (valueScale !== widget.model().valueScales()[i] && displayName === widget.model().valueScales()[i].displayName()) {
              throw new Error(webChartUtil.lex.get("webChart.sameDisplayName", displayName, widget.model().valueScales()[i].displayName()));
            }
          }

          return axisTabbedEditor.read().then(function (reads) {
            var j,
                k,
                displayNames = [];

            for (j = 0; j < reads.length; j++) {
              var displayName = reads[j].get("displayName");
              displayNames[j] = displayName;

              for (k = 0; k < reads.length; k++) {
                if (j !== k && displayName === reads[k].get("displayName")) {
                  throw new Error(webChartUtil.lex.get("webChart.sameDisplayName", displayName, reads[k].get("displayName")));
                }
              }
            }
          });
        });
      });
    }

    for (i = 0; i < widget.model().valueScales().length; i++) {
      var valueScale = widget.model().valueScales()[i];

      if (valueScale.seriesList().length > 0) {
        var sheet = new SimplePropertySheet(),
            tab = new Tab({
          item: sheet,
          value: settings.valueScaleSettingsDisplay(valueScale),
          //promise
          title: valueScale.displayName() ? valueScale.displayName() : "#"
        });
        axisTabbedEditor.addTab(tab);

        if (valueScale === selectedBaseScale) {
          axisTabbedEditor.setSelectedTab(tab);
        }

        addValidator(sheet, valueScale);
      }
    }

    return new Tab({
      item: axisTabbedEditor,
      title: webChartUtil.lex.get("webChart.SetAxisValuesCommand.displayName")
    });
  };
  /**
   * Override resolveTab to resolve to a tab for the content.
   * @return {Promise.<module:nmodule/webChart/rc/tab/Tab>}
   */


  SetAxisValuesCommand.prototype.resolveTab = function () {
    var that = this,
        selectedBaseScale = that.$selectedBaseScale,
        widget = that.$widget;
    return Promise.resolve(SetAxisValuesCommand.axisListTab(widget, selectedBaseScale));
  };

  SetAxisValuesCommand.prototype.handleRead = function (tab) {
    var that = this,
        widget = that.$widget;
    return webChartUtil.fullValidate(tab.widget).then(function () {
      return tab.save();
    })["catch"](function (err) {
      if (err) {
        widget.jq().trigger(chartEvents.DISPLAY_ERROR, err);
        throw err;
      }
    });
  };
  /**
   * Override to set lexicon parameters
   * @param obj
   * @returns {{}}
   */


  SetAxisValuesCommand.prototype.lexParams = function (obj) {
    return {
      module: "webChart",
      lex: "webChart.SetAxisValuesCommand"
    };
  };

  return SetAxisValuesCommand;
});
