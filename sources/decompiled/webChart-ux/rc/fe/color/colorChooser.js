/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */
define(['jquery', 'Promise', 'dialogs', 'nmodule/webChart/rc/webChartUtil', 'hbs!nmodule/webChart/rc/fe/color/colorChooserStructure', 'css!nmodule/webChart/rc/fe/color/colorChooserStyle'], function ($, Promise, dialogs, webChartUtil, colorChooserStructure) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * @exports nmodule/webChart/rc/fe/color/colorChooser
   */

  var colorChooser = {};
  var selected = "#ff0000",
      def = [];
  /**
   * Show the color chooser starting with the startingValue.
   * @param startingValue
   * @return {Promise} that resolves the the newValue
   */

  colorChooser.show = function (startingValue) {
    //eslint-disable-next-line promise/avoid-new
    return new Promise(function (resolve) {
      // Reset default color
      selected = startingValue.encodeToString();
      var title = webChartUtil.lex.get("colorPicker.dialogTitle"),
          dom,
          swatchDom,
          dialog = dialogs.showOkCancel({
        title: title,
        content: '<div class="ColorChooser-content"></div>'
      }).ok(function () {
        resolve(selected);
      });
      dom = dialog.content().children('.ColorChooser-content');
      dom.html(colorChooserStructure());
      colorChooser.getPaletteDom().html(colorChooser.makeDefaultPalette());
      swatchDom = colorChooser.getSwatchDom();
      swatchDom.css({
        backgroundColor: startingValue
      });
      swatchDom.data({
        value: startingValue
      });
    });
  };
  /**
   * Return the swatch element.
   * @returns {jquery}
   */


  colorChooser.getSwatchDom = function () {
    return $(".ColorChooser-swatch", webChartUtil.getCurrentDialogDom());
  };
  /**
   * Return the palette element.
   * @returns {jquery}
   */


  colorChooser.getPaletteDom = function () {
    return $(".ColorChooser-palette", webChartUtil.getCurrentDialogDom());
  };
  /**
   * Return the palette elements.
   * @returns {jquery}
   */


  colorChooser.getPaletteEntryDom = function () {
    return $(".ColorChooser-palette-entry", colorChooser.getPaletteDom());
  }; ////////////////////////////////////////////////////////////////
  // Methods
  ////////////////////////////////////////////////////////////////

  /**
   * Create default palette.
   * @return {jquery}
   */


  colorChooser.makeDefaultPalette = function () {
    var outer = $("<div>");
    var cols = 8;
    var rows = 8;

    for (var r = 0; r < rows; r++) {
      for (var c = 0; c < cols; c++) {
        var i = r * 8 + c;
        var x = c * 17 + 1;
        var y = r * 17 + 1;
        var val = def[i];
        var elem = $("<div class='ColorChooser-palette-entry'></div>");
        elem.data({
          value: val
        });
        elem.css({
          top: y,
          left: x,
          backgroundColor: val
        });
        outer.append(elem);
      }
    }

    outer.on('click', '.ColorChooser-palette-entry', function () {
      colorChooser.update($(this).data('value'));
    });
    return outer;
  };
  /**
   * Update selected color.
   * @param {gx:Color} val
   */


  colorChooser.update = function (val) {
    selected = val;
    webChartUtil.trace("value from update:" + val);
    var swatchDom = colorChooser.getSwatchDom();
    swatchDom.css({
      backgroundColor: val
    });
    swatchDom.data({
      value: val
    });
  }; ////////////////////////////////////////////////////////////////
  // Attributes
  ////////////////////////////////////////////////////////////////
  // Red


  def = ["#ff0000", "#400000", "#800000", "#c00000", "#ff4040", "#ff8080", "#ffc0c0", "#000000", // Orange
  "#ff8000", "#402000", "#804000", "#bf6000", "#ffa040", "#ffc080", "#ffdfbf", "#202020", // Yellow
  "#ffff00", "#404000", "#808000", "#c0c000", "#ffff40", "#ffff80", "#ffffc0", "#404040", // Green
  "#00ff00", "#004000", "#008000", "#00c000", "#40ff40", "#80ff80", "#c0ffc0", "#606060", // Cyan
  "#00ffff", "#004040", "#008080", "#00c0c0", "#40ffff", "#80ffff", "#c0ffff", "#808080", // Aqua ?

  /*
   "#0080ff",
   "#002040",
   "#004080",
   "#0060bf",
   "#40a0ff",
   "#80c0ff",
   "#bfdfff",
   "#b0b0b0",
   */
  // Blue
  "#0000ff", "#000040", "#000080", "#0000c0", "#4040ff", "#8080ff", "#c0c0ff", "#c0c0c0", // Purple
  "#8000ff", "#1e0040", "#3c0080", "#5a00c0", "#9a41ff", "#bb80ff", "#ddbfff", "#e0e0e0", // Magenta
  "#ff00ff", "#400040", "#800080", "#c000c0", "#ff40ff", "#ff80ff", "#ffc0ff", "#ffffff"];
  return colorChooser;
});
