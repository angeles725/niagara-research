/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/SettingsCommand
 */
define(['d3', 'jquery', 'baja!', 'lex!', 'dialogs', 'nmodule/webChart/rc/webChartUtil', 'nmodule/webChart/rc/choiceUtil', 'bajaux/commands/Command', 'nmodule/webChart/rc/tab/TabbedEditor', 'nmodule/webChart/rc/tab/Tab', 'nmodule/webChart/rc/chartEvents', 'nmodule/webChart/rc/fe/SimplePropertySheet', 'nmodule/webChart/rc/grid/GridEditor', 'nmodule/webChart/rc/menu/contextMenuUtil'], function (d3, $, baja, lex, dialogs, webChartUtil, choiceUtil, Command, TabbedEditor, Tab, chartEvents, SimplePropertySheet, GridEditor, contextMenuUtil) {
  "use strict";
  /**
   * SettingsCommand is a Command that provides access to the WebChart Settings
   *
   * @private
   * @class
   * @alias module:nmodule/webChart/rc/command/SettingsCommand
   * @extends module:bajaux/commands/Command
   * @param {module:nmodule/webChart/rc/ChartWidget} widget
   */

  var SettingsCommand = function SettingsCommand(widget) {
    Command.call(this, {
      module: "webChart",
      lex: "webChart.settingsCommand",
      func: function func() {
        var tabbedEditor = new TabbedEditor(),
            removeHandler,
            settings = widget.settings(),
            title = webChartUtil.lex.get("webChart.settingsCommand.dialogTitle");

        function destroy() {
          if (removeHandler) {
            widget.jq().off(chartEvents.FILE_MODIFY_EVENT, removeHandler);
          }

          return tabbedEditor.destroy();
        }

        var dialog = dialogs.showOkCancel({
          title: title,
          content: '<div class="SettingsCommand-commands"/><div class="SettingsCommand-Container"/>',
          buttons: [{
            name: "reset",
            displayName: webChartUtil.lex.get("reset"),
            handler: function handler() {
              widget.settings().reset();
              widget.reloadAll();
              widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
              return destroy();
            }
          }]
        }).ok(function () {
          return webChartUtil.fullValidate(tabbedEditor).then(function () {
            return tabbedEditor.save().then(function () {
              widget.reloadAll();
              widget.jq().trigger(chartEvents.SETTINGS_CHANGED);
            });
          })["catch"](function (err) {
            baja.error(err);
            widget.jq().trigger(chartEvents.DISPLAY_ERROR, err);
            throw err;
          })["finally"](destroy);
        }).cancel(destroy);
        var dom = dialog.content().children(".SettingsCommand-Container");
        widget.settings().load();
        return lex.module("webChart").then(function (lex) {
          var seriesTabTitle = lex.get("webChart.settingsCommand.seriesTabTitle"),
              axisTabTitle = lex.get("webChart.settingsCommand.axisTabTitle"),
              layersTabTitle = lex.get("webChart.settingsCommand.layersTabTitle"),
              samplingTabTitle = lex.get("webChart.settingsCommand.samplingTabTitle");
          var seriesTab = new Tab({
            item: new SimplePropertySheet(),
            value: settings.seriesListSettings(),
            title: seriesTabTitle
          });
          tabbedEditor.addTab(seriesTab);
          tabbedEditor.addTab(new Tab({
            item: new SimplePropertySheet(),
            value: settings.chartSettings(),
            title: axisTabTitle
          }));
          tabbedEditor.addTab(new Tab({
            item: new SimplePropertySheet(),
            value: settings.layersSettings(),
            title: layersTabTitle
          }));
          var samplingTab = new Tab({
            item: new SimplePropertySheet(),
            value: settings.samplingSettings(),
            title: samplingTabTitle
          });

          samplingTab.modified = function (samplingSettings) {
            //TODO: PropertySheet currently cannot handle changing the hidden flags without calling load.
            //We can't call load because fields like the sample size might be in the middle of being
            //edited and you can't type
            //settings.loadSamplingSettings(samplingSettings);
            //samplingTab.load(samplingSettings);
            //workaround: duplicate enable/disable logic for component on the widget
            var sheet = samplingTab.widget,
                autoSamplingOn = choiceUtil.getChoice(samplingSettings, "autoSampling"),
                editor = sheet.$getRowForKey('sampling').$getValueEditor();
            editor.setEnabled(!autoSamplingOn);
            editor.setReadonly(autoSamplingOn);
          };

          tabbedEditor.addTab(samplingTab);
          return tabbedEditor.initialize(dom).then(function () {
            return tabbedEditor.load();
          }).then(function () {
            seriesTab.widget.jq().find(".PropertySheetRow").each(function (i, row) {
              $(row).data("ord", widget.model().seriesList()[i].ord());
            });
            var d3Group = d3.select(seriesTab.widget.jq()[0]).selectAll(".PropertySheetRow");
            d3Group.data(widget.model().seriesList()); //remove rows when they have been removed via context menu

            removeHandler = function removeHandler() {
              seriesTab.widget.jq().find(".PropertySheetRow").each(function (i, row) {
                var rowJq = $(row);
                var ord = rowJq.data("ord");

                if (!widget.model().hasOrd(ord)) {
                  var rowWidget = rowJq.data("widget");

                  if (rowWidget) {
                    rowWidget.destroy()["catch"](baja.error);
                  }

                  rowJq.remove();
                }
              });
            };

            widget.jq().on(chartEvents.FILE_MODIFY_EVENT, removeHandler);

            var handleContextMenu = function handleContextMenu(d) {
              return contextMenuUtil.seriesContextMenu(widget.graph(), d3.select(this), d);
            };

            contextMenuUtil.registerContextMenu(d3Group, handleContextMenu);

            if (widget.jq().hasClass("ux-webchart-thin")) {
              var commandBar = new GridEditor(),
                  commandBarDom = dialog.content().children(".SettingsCommand-commands");
              var tabs = widget.widgetBar().getTabs(),
                  i;

              for (i = 0; i < tabs.length; ++i) {
                if (tabs[i].value instanceof Command && tabs[i].value.isEnabled() && (tabs[i].title === "exportCommand" || tabs[i].title === "deltaCommand" || tabs[i].title === "statusColoringCommand" || tabs[i].title === "liveCommand" || tabs[i].title === "samplingCommand" || tabs[i].title === "interpolateTailCommand")) {
                  commandBar.addCommand(tabs[i].value);
                }
              }

              return commandBar.initialize(commandBarDom).then(function () {
                return commandBar.load();
              });
            }
          });
        });
      }
    });
  };

  SettingsCommand.prototype = Object.create(Command.prototype);
  SettingsCommand.prototype.constructor = SettingsCommand;
  return SettingsCommand;
});
