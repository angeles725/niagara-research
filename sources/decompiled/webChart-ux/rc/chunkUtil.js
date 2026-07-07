/**
 * @copyright 2015, Tridium, Inc. All Rights Reserved.
 */
define(['d3', 'Promise'], function (d3, Promise) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A set of utility functions for the chunking
   *
   * @exports nmodule/webChart/rc/chunkUtil
   */

  var chunkUtil = {};
  chunkUtil.ABORT_MESSAGE = "abort";
  /**
   * Like a regular ajax request, but provides caller the ability to have a progress and check handler for chunked responses
   * @param {String} uri the uri of the request
   * @param {Object} params  the object literal for the method's optional arguments.
   * @param {Function} [params.progress] function to be called with an array of Strings that are fully available to process
   * @param {Function} [params.checkAbort] return true from this function to abort the ajax request, will get called on each progress call
   * @returns {Promise} resolves to the array of Strings which have yet to be processed though the progress callback.
   */

  chunkUtil.ajax = function (uri, params) {
    // eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve, reject) {
      if (!params) {
        params = {};
      }

      var progress = params.progress,
          checkAbort = params.checkAbort,
          lastDelimiterIndex = 0,
          progressIndex = 0;
      var request = d3.text(uri, null, function (err, data) {
        if (err) {
          //window.console.log("err:" + err);
          reject(err);
        } else {
          if (lastDelimiterIndex > 0) {
            var lastUnprocessedChunks = data.substring(lastDelimiterIndex + 1).split("\n");
            resolve(lastUnprocessedChunks);
          } else {
            if (!data || !data.length) {
              resolve([]);
            } else {
              resolve(data.split("\n"));
            }
          }
        }
      });

      if (progress) {
        request.on("progress", function (responseObject) {
          var text = responseObject.responseText,
              startString = lastDelimiterIndex === 0 ? 0 : 1;

          if (checkAbort && checkAbort()) {
            request.abort();
            reject(chunkUtil.ABORT_MESSAGE);
            return;
          }

          var lastDelmiter = text.lastIndexOf("\n");

          if (lastDelimiterIndex !== lastDelmiter && lastDelmiter !== -1) {
            var newResponse = text.substring(lastDelimiterIndex + startString, lastDelmiter);
            lastDelimiterIndex = lastDelmiter;
            progress(newResponse.split('\n'), progressIndex);
            progressIndex++;
          } else {//not enough progress made between chunks, or delimeter not found, wait for completion
          }
        });
      }
    });
  };

  return chunkUtil;
});
