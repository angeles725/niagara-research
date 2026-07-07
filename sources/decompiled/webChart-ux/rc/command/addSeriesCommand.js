/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/command/addSeriesCommand
 */
define(["baja!", 'log!nmodule.webChart.rc.command.addSeriesCommand', "bajaux/commands/Command"], function (baja, log, Command) {
  "use strict";

  var logSevere = log.severe.bind(log);
  /**
   * Check to see if this object is valid to be loaded as a series.
   *
   * @inner
   * @param {*} obj
   * @returns {Boolean} true if the object is a valid baja value, but not a
   * hierarchy or actual history instance.
   */

  function isValidSeries(obj) {
    return baja.hasType(obj) && !baja.hasType(obj, 'hierarchy:LevelElem') && !baja.hasType(obj, 'history:IHistory');
  }
  /**
   * Ask the user to select something to add to the chart.
   *
   * This method will lazily require all the information to
   * load a Nav tree into the Web Chart application.
   *
   * @internal
   * @private
   *
   * @param chartWidget The Chart Widget instance.
   */


  function addSeries(chartWidget) {
    // Do all of this in a lazy way so we can keep the web chart
    // foot print low.
    require(["jquery", "Promise", "lex!webChart", "dialogs", "nmodule/webEditors/rc/fe/fe", "nmodule/webEditors/rc/wb/tree/NavTree", "nmodule/webEditors/rc/wb/tree/stationTree"], function ($, Promise, lexicons, dialogs, fe, NavTree, stationTree) {
      var lex = lexicons[0],
          dlg,
          content,
          ok = "ok",
          treeOptions;
      dlg = dialogs.showOkCancel({
        title: lex.get("webChart.addSeriesCommand.displayName"),
        content: "<div class='webChart-add-series-content ux-border'>" + lex.getSafe("webChart.addSeriesCommand.loading") + "</div>"
      });
      content = dlg.content().children(".webChart-add-series-content"); //TODO: obtain tree configuration from profile

      treeOptions = {
        configTree: true,
        navFileTree: true,
        filesTree: true,
        historiesTree: true,
        hierarchiesTree: true
      };
      stationTree.makeRootNode(treeOptions).then(function (rootNode) {
        return stationTree.createNavTree({
          dom: content,
          value: rootNode
        });
      }).then(function (tree) {
        dlg.disableButton(ok); // Ensure we're expanded out to the history space by default.

        tree.setSelectedPath(["local", "history"]);

        function resolveValidSeriesData() {
          var nodes = tree.getSelectedNodes(),
              i,
              value,
              navOrd,
              batchResolveOrds = [];

          for (i = 0; i < nodes.length; ++i) {
            value = nodes[i].value();

            if (value && value.getNavOrd) {
              navOrd = value.getNavOrd();

              if (navOrd) {
                batchResolveOrds.push(navOrd);
              }
            }
          }

          return new baja.BatchResolve(batchResolveOrds).resolve().then(function (batchResolve) {
            var objs = batchResolve.getTargetObjects(),
                j,
                seriesData = [];

            for (j = 0; j < objs.length; ++j) {
              seriesData.push({
                ord: batchResolve.getOrd(j),
                value: isValidSeries(objs[i]) ? objs[i] : undefined
              });
            }

            return seriesData;
          })["catch"](function (err) {
            baja.error(err);
            throw err;
          });
        }

        function hasValidSelection() {
          return resolveValidSeriesData().then(function (results) {
            return results.length > 0;
          });
        }

        tree.jq().on(NavTree.SELECTED_EVENT, function (e, ed) {
          hasValidSelection().then(function (valid) {
            if (valid) {
              dlg.enableButton(ok);
            }
          })["catch"](logSevere);
        }).on(NavTree.ACTIVATED_EVENT, function () {
          hasValidSelection().then(function (valid) {
            if (valid) {
              dlg.enableButton(ok);
              dlg.click(ok);
            }
          })["catch"](logSevere);
        });
        dlg.ok(function () {
          var model = chartWidget.model(),
              subscriber = chartWidget.getSubscriber();
          resolveValidSeriesData().then(function (seriesData) {
            model.addSeries(subscriber, seriesData).then(function () {
              return model.load(subscriber);
            }) // eslint-disable-next-line promise/no-return-in-finally
            ["finally"](function () {
              return tree.destroy();
            });
          })["catch"](logSevere);
        });
      })["catch"](logSevere);
    });
  }

  return function make(chartWidget) {
    return new Command({
      module: "webChart",
      lex: "webChart.addSeriesCommand",
      func: function func() {
        addSeries(chartWidget);
      }
    });
  };
});
