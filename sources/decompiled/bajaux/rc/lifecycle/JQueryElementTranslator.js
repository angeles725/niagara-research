function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } }

function _createClass(Constructor, protoProps, staticProps) { if (protoProps) _defineProperties(Constructor.prototype, protoProps); if (staticProps) _defineProperties(Constructor, staticProps); return Constructor; }

/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/* eslint-env browser */

/**
 * API Status: **Private**
 * @private
 * @module bajaux/lifecycle/JQueryElementTranslator
 */
define(['jquery', 'Promise'], function ($, Promise) {
  'use strict';
  /**
   * Translates DOM elements to jQuery instances to provide to vanilla Widgets.
   *
   * @class
   * @implements module:bajaux/lifecycle/WidgetManager~ElementTranslator
   */

  return /*#__PURE__*/function () {
    function JQueryElementTranslator() {
      _classCallCheck(this, JQueryElementTranslator);
    }

    _createClass(JQueryElementTranslator, [{
      key: "translateElement",
      value: function translateElement(dom) {
        if (typeof dom === 'string' || dom instanceof HTMLElement || dom instanceof $) {
          return Promise.resolve($(dom));
        }

        return Promise.reject(new Error('dom cannot be translated'));
      }
    }]);

    return JQueryElementTranslator;
  }();
});
