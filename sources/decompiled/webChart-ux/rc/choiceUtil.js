/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author JJ Frankovich
 */
define(['baja!', 'lex!webChart', 'baja!' + 'baja:Boolean,' + 'baja:Enum,' + 'baja:EnumRange,' + 'baja:DynamicEnum'], function (baja, lexs) {
  "use strict";
  /**
   * API Status: **Private**
   *
   * A set of utility functions for the webChart module.
   *
   * @exports nmodule/webChart/rc/choiceUtil
   */

  var choiceUtil = {};
  var webChartLex = lexs[0];

  choiceUtil.getChoice = function (comp, propertyName, defaultChoice) {
    var value;

    if (comp && comp.has(propertyName)) {
      value = comp.get(propertyName);

      if (value.getType().is("baja:Boolean")) {
        return value;
      }

      if (value.getType().is("baja:Enum")) {
        return value.getTag();
      } else {
        return value;
      }
    }

    return defaultChoice;
  };

  choiceUtil.hideChoice = function (comp, propertyName) {
    if (comp.has(propertyName)) {
      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) | baja.Flags.HIDDEN
      }); //comp.remove(propertyName);
    }
  };

  choiceUtil.allowChoice = function (comp, propertyName) {
    if (comp.has(propertyName)) {
      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) & ~baja.Flags.READONLY & ~baja.Flags.HIDDEN
      });
    }
  };

  choiceUtil.addChoice = function (comp, propertyName, value, facets) {
    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: value,
        facets: facets
      });
    }
  };

  choiceUtil.addBooleanChoice = function (comp, propertyName, selected) {
    choiceUtil.addChoice(comp, propertyName, selected);
  };

  choiceUtil.addEnumChoice = function (comp, propertyName, tags, selected) {
    var dynamicEnum = choiceUtil.makeDynamicEnum(tags, selected);
    var facets = baja.Facets.make({
      uxFieldEditor: 'nmodule/webEditors/rc/fe/baja/FrozenEnumEditor',
      enablePopOut: false
    });
    choiceUtil.addChoice(comp, propertyName, dynamicEnum, facets);
  };

  choiceUtil.setReadonly = function (comp, propertyName, readonly) {
    if (readonly) {
      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) | baja.Flags.READONLY | baja.Flags.TRANSIENT
      });
    } else {
      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) & ~baja.Flags.READONLY & ~baja.Flags.TRANSIENT
      });
    }
  };

  choiceUtil.setStat = function (comp, propertyName, value, facets) {
    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: value,
        flags: baja.Flags.READONLY | baja.Flags.TRANSIENT,
        facets: facets
      });
    } else {
      comp.set({
        slot: propertyName,
        value: value
      });
      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) | baja.Flags.READONLY | baja.Flags.TRANSIENT
      });
    }
  };
  /**
   *
   * @param {baja.Component} comp
   * @param {String} propertyName
   * @param {baja.Value | String} choice
   * @param [baja.facets] [facets]
   * @param [baja.Flags] [flags]
   * @returns {boolean}
   */


  choiceUtil.setChoice = function (comp, propertyName, choice, facets, flags) {
    var modified = true;

    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: choice,
        facets: facets
      });
    } else if (choice !== undefined) {
      if (choice !== choiceUtil.getChoice(comp, propertyName)) {
        comp.set({
          slot: propertyName,
          value: choice
        });
      } else {
        modified = false;
      }
    }

    if (!flags) {
      flags = comp.getFlags(propertyName) & ~baja.Flags.HIDDEN;
    }

    comp.setFlags({
      slot: propertyName,
      flags: flags
    });
    return modified;
  };
  /**
   * Use the existing value if it falls within the min and max, otherwise set it the min or max.
   * If its missing, use the defaultChoice.
   * @param {baja.Component} comp
   * @param {String} propertyName
   * @param {baja.Value} defaultChoice
   * @param [baja.facets] [facets]
   * @returns {boolean} return true if modified
   */


  choiceUtil.enforceChoice = function (comp, propertyName, defaultChoice, facets) {
    var modified = false,
        currentChoice;

    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: defaultChoice,
        facets: facets
      });
      modified = true;
    } else {
      currentChoice = choiceUtil.getChoice(comp, propertyName);

      if (facets) {
        comp.setFacets({
          slot: propertyName,
          facets: facets
        }); //if value below min, lower it to min

        try {
          var min = parseInt(comp.getFacets(propertyName).get("min", null));

          if (currentChoice < min) {
            choiceUtil.setChoice(comp, propertyName, min);
            modified = true;
          }
        } catch (ignore) {} //if value above max, lower it to max


        try {
          var max = parseInt(comp.getFacets(propertyName).get("max", null));

          if (currentChoice > max) {
            choiceUtil.setChoice(comp, propertyName, max);
            modified = true;
          }
        } catch (ignore) {}
      } //ensure visible


      comp.setFlags({
        slot: propertyName,
        flags: comp.getFlags(propertyName) & ~baja.Flags.HIDDEN
      });
    }

    return modified;
  };

  choiceUtil.setBooleanChoice = function (comp, propertyName, selected) {
    var modified = true;

    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: !!selected
      });
    } else if (selected !== undefined) {
      if (selected !== choiceUtil.getChoice(comp, propertyName)) {
        comp.set({
          slot: propertyName,
          value: selected
        });
      } else {
        modified = false;
      }
    }

    comp.setFlags({
      slot: propertyName,
      flags: comp.getFlags(propertyName) & ~baja.Flags.HIDDEN
    });
    return modified;
  };
  /**
   *
   * @param comp
   * @param propertyName
   * @param tags
   * @param selected
   */


  choiceUtil.setEnumChoice = function (comp, propertyName, tags, selected) {
    var dynamicEnum = choiceUtil.makeDynamicEnum(tags, selected),
        modified = true;

    if (!comp.has(propertyName)) {
      comp.add({
        slot: propertyName,
        value: dynamicEnum,
        facets: baja.Facets.make({
          uxFieldEditor: 'nmodule/webEditors/rc/fe/baja/FrozenEnumEditor',
          enablePopOut: false
        })
      });
    } else if (selected !== undefined) {
      if (selected !== choiceUtil.getChoice(comp, propertyName)) {
        comp.set({
          slot: propertyName,
          value: dynamicEnum
        });
      } else {
        modified = false;
      }
    }

    comp.setFlags({
      slot: propertyName,
      flags: comp.getFlags(propertyName) & ~baja.Flags.HIDDEN
    });
    return modified;
  };
  /**
   * Make a DynamicEnum based on the tags and possible selection given.
   *
   * @param {Array.<String>} tags
   * @param {Number|String} [selection] Default to 0, can be set to a tag or ordinal
   * @param {String} [lexiconModule="webChart"] Lexicon module name for display name tags
   * @return {baja.DynamicEnum}
   */


  choiceUtil.makeDynamicEnum = function (tags, selected, lexiconModule) {
    var ordinal = 0,
        i;

    if (!lexiconModule) {
      lexiconModule = "webChart";
    }

    for (i = 0; i < tags.length; i++) {
      tags[i] = baja.SlotPath.escape(tags[i]);
    }

    if (selected) {
      if (typeof selected === "string") {
        ordinal = tags.indexOf(selected);
      } else {
        ordinal = selected;
      }
    }

    return baja.DynamicEnum.make({
      ordinal: ordinal,
      range: baja.EnumRange.make({
        ordinals: choiceUtil.getUsualOrdinals(tags.length),
        tags: tags,
        options: baja.Facets.make({
          lexicon: lexiconModule
        })
      })
    });
  };
  /**
   * A utility function for providing the usual ordinal array for an baja.EnumRange.
   @example
   *   <caption>
   *     Get the usual ordinal array for  3 items
   *   </caption>
   *  var choicesEnum = baja.DynamicEnum.make({
   *           ordinal : 0,
   *           range : baja.EnumRange.make({
   *             tags : ["one", "two", "three"],
   *             ordinals : choiceUtil.getUsualOrdinals(3)
   *           })
   *         });
   *
   * @param length
   * @returns {Array}
   */


  choiceUtil.getUsualOrdinals = function (length) {
    var ordinals = [];

    for (var i = 0; i < length; i++) {
      ordinals.push(i);
    }

    return ordinals;
  };
  /**
   * Set the displayNames for a component. By default, will use the lexicon, but additional displayName entries can be provided via name/DisplayName
   * key pairs that have matching indexes.
   *
   * @param {baja.Component} component
   * @param {Array.<String>} [names]
   * @param {Array.<String>} [displayNames]
   */


  choiceUtil.setDisplayNames = function (component, names, displayNames) {
    var nameMap = {};
    component.getSlots().each(function (slot) {
      if (names && names.indexOf(String(slot)) > -1) {
        nameMap[slot] = displayNames[names.indexOf(String(slot))];
      } else {
        var displayName = webChartLex.get(slot);

        if (displayName) {
          nameMap[slot] = displayName;
        } else {
          nameMap[slot] = baja.SlotPath.unescape(String(slot)); //fallback is to al least escape the text
        }
      }
    });

    if (!component.has('displayNames')) {
      component.add({
        slot: "displayNames",
        flags: baja.Flags.HIDDEN | baja.Flags.READONLY | baja.Flags.TRANSIENT,
        value: baja.NameMap.make(nameMap)
      });
    } else {
      component.set({
        slot: "displayNames",
        value: baja.NameMap.make(nameMap),
        flags: baja.Flags.HIDDEN | baja.Flags.READONLY | baja.Flags.TRANSIENT
      });
    }
  };

  return choiceUtil;
});
