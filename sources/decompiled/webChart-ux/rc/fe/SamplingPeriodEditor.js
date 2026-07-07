/**
 * @copyright 2017 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */

/**
 * API Status: **Private**
 * @module nmodule/webChart/rc/fe/SamplingPeriodEditor
 */
define(['baja!', 'nmodule/webEditors/rc/fe/baja/OverrideRelTimeEditor'], function (baja, OverrideRelTimeEditor) {
  "use strict"; //these display values get processed through the `control` lexicon

  var CUSTOM_DURATION = -1,
      DURATION_KEY = 'durationSelect',
      RELTIME_KEY = 'relTime',
      DURATIONS = [['sampling.bestFit', 0], //TODO: should we add seconds?
  ['sampling.min1', 60000], ['sampling.min15', 15 * 60000], ['sampling.min30', 30 * 60000], ['sampling.hour', 60 * 60000], ['sampling.day', 24 * 60 * 60000], ['sampling.week', 7 * 24 * 60 * 60000], ['sampling.month', 30 * 24 * 60 * 60000], ['sampling.year', 364 * 24 * 60 * 60000], ['sampling.custom', CUSTOM_DURATION]];
  /**
   * Editor for giving a few options for the desired Sampling Period as well as a way to enter a custom time.
   *
   * @private
   * @class
   * @extends module:nmodule/webEditors/rc/fe/baja/OverrideRelTimeEditor
   * @alias module:nmodule/webChart/rc/fe/SamplingPeriodEditor
   */

  var SamplingPeriodEditor = function SamplingPeriodEditor() {
    var that = this;
    OverrideRelTimeEditor.apply(that, arguments);
  };

  SamplingPeriodEditor.prototype = Object.create(OverrideRelTimeEditor.prototype);
  SamplingPeriodEditor.prototype.constructor = SamplingPeriodEditor;

  SamplingPeriodEditor.prototype.doInitialize = function (dom) {
    //TODO: remove pop-up for Complex Editors until its done right in webEditors
    dom.closest(".PropertySheetRow").children("td.col-commands").children("div.command-group").toggle(false);
    return OverrideRelTimeEditor.prototype.doInitialize.apply(this, arguments);
  };
  /**
  * When a new duration is selected, update the RelTime editor. It should
  * show itself if a custom duration is selected, otherwise be loaded
  * with the selected duration and hidden.
  *
  * @private
  * @returns {Promise} promise to be resolved after the RelTime editor has
  * shown/hidden as appropriate
  */


  SamplingPeriodEditor.prototype.$updateFromDuration = function () {
    var builder = this.getBuilder(),
        durationEd = builder.getEditorFor(DURATION_KEY),
        relTimeEd = builder.getEditorFor(RELTIME_KEY);
    return durationEd.read().then(function (duration) {
      if (duration === CUSTOM_DURATION) {
        return relTimeEd.jq().toggle(true);
      } else {
        return relTimeEd.load(baja.RelTime.make(duration)).then(function () {
          return relTimeEd.jq().toggle(false);
        });
      }
    });
  };
  /**
   * Return the durations to use for this editor.
   * @returns {Array} the durations to use for this editor
   */


  SamplingPeriodEditor.prototype.$getDurations = function () {
    return DURATIONS;
  };
  /**
   * Return the lexicon module name.
   * @returns {string}
   */


  SamplingPeriodEditor.prototype.$getLexiconName = function () {
    return "webChart";
  };

  return SamplingPeriodEditor;
});
