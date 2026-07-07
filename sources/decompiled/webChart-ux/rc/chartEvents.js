/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */
define([], function () {
  'use strict';
  /**
   * A list of all chart related events.
   * API Status: **Private**
   * @exports {module:nmodule/webChart/rc/chartEvents}
   */

  var exports = {
    /** Triggers when a redraw request is made. */
    REDRAW_REQUEST_EVENT: 'webchart:redrawrequest',

    /** Triggers when a graphData request is made. */
    GRAPH_DATA_REQUEST_EVENT: 'webchart:graphdatarequest',

    /** Triggers when a graph data is removed. */
    REMOVE_DATA_REQUEST_EVENT: 'webchart:removedatarequest',

    /** Triggers when a series is enabled. */
    SERIES_ENABLED: 'webchart:series:enabled',

    /** Triggers when a series is disabled. */
    SERIES_DISABLED: 'webchart:series:disabled',

    /** Triggers when a series has been given focus.
     Only one series may be given focus at a time.
     The series is passed as a parameter when this event is fired.
     For convenance, the second parameter is the number of enabled series.*/
    SERIES_FOCUS: 'webchart:series:focus',

    /** Triggers when none of the series are focused. */
    SERIES_BLUR: 'webchart:series:blur',

    /** Triggers whenever a Series has been added to the Web Chart
     In the event, the first parameter is the model instance.
     The second parameter is the newly added Series instance. */
    SERIES_ADDED: 'webchart:series:added',

    /** Triggers when the model is set to live. */
    MODEL_LIVE: 'webchart:model:live',

    /** Triggers when the model is no longer live. */
    MODEL_NOT_LIVE: 'webchart:model:notlive',

    /** Triggers when the web chart's settings change. */
    SETTINGS_CHANGED: 'webchart:settings:changed',

    /** Triggers when the chart's title is changed.
     The new title is passed as a parameter. */
    TITLE_CHANGED: 'webchart:title:changed',

    /** Triggers when the chart's time range is changed. */
    TIME_RANGE_CHANGED: 'webchart:timerange:changed',

    /** Triggers when the chart's delta is changed. */
    DELTA_CHANGED: 'webchart:delta:changed',

    /** Triggers when one of the visible chart's value changes units.
     The new value scale is passed as a parameter. */
    VALUE_SCALE_CHANGED: 'webchart:valuescale:changed',

    /** Triggers modified only if the webChart is a chartFile */
    FILE_MODIFY_EVENT: 'webchart:file:modify',

    /** Triggers when an error should be shown to the user. */
    DISPLAY_ERROR: 'webchart:error',

    /** Triggers when a the ChartModel start the loading process. */
    MODEL_START_LOAD: 'webchart:model:startLoad',

    /** Triggers when a the ChartModel is done loading data. */
    MODEL_DATA_LOADED: 'webchart:model:dataloaded',

    /** Triggers when a the ChartModel is stopped. */
    MODEL_DATA_STOPPED: 'webchart:model:datastopped',

    /** Pass this as the event argument with a chartEvent _Change event if we should not consider the Chart File modified */
    IGNORE_MODIFY: 'webchart:ignoremodify',

    /** The Model is over max capacity (or no longer over max) capacity so allow event to be fired when this changes */
    MODEL_OVER_MAX_CAPACITY: 'webchart:model:overmaxcapacity'
  };
  return exports;
});
