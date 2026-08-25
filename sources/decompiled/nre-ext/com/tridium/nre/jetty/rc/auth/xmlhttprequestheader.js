/**
 * @copyright 2018 Tridium, Inc. All Rights Reserved.
 */

/*exported sendHttpHeader */
/*global ActiveXObject, XMLHttpRequest */

/**
 * Make an HTTP connection using XMLHttpRequest or its MS equivalents.
 *
 * @function
 * @private
 * @inner
 *
 * @param {String} method  the method to use (i.e. 'post' or 'get').
 * @param {String} uri  the URI used in the connection.
 * @param {String} [body]  the body of the HTTP POST.
 * @param {Object} callback  this object must have 'ok' and 'fail' functions.
 */
function sendHttpHeader(uri, header, callback) {
   'use strict';

   var x = null,
       handler;

   // HTTP Callback Handler
   handler = function () {
     var st;  // Status

     if (x.readyState === 4) {
       try {
         st = parseInt(x.status, 10);

         if (st !== 200 && st !== 401) {
           callback.fail("auth failure");
         }
         else {
           // HTTP 200 ok or 401
           callback.ok(x.getResponseHeader("WWW-Authenticate"));
         }
       }
       catch (error) {
         callback.fail(error);
       }
     }
   };

   // Make AJAX network call
   try {
     // Create XMLHttpRequest
     try {
       x = new XMLHttpRequest();
     }
     catch (e) {
       try {
         x = new ActiveXObject("Msxml2.XMLHTTP");
       }
       catch (e2) {
         try {
           x = new ActiveXObject("Microsoft.XMLHTTP");
         }
         catch (e3) {
           // No XMLHttpAvailable?
           throw new Error("Failed to create XMLHttpRequest: " + e3);
         }
       }
     }

     x.onreadystatechange = handler;
     x.open("GET", uri, true);
     x.setRequestHeader("Authorization", header);

     // Send body
     x.send("");
   }
   catch (error) {
     callback.fail(error);
     return;
   }
 }
