function _typeof(obj) { "@babel/helpers - typeof"; if (typeof Symbol === "function" && typeof Symbol.iterator === "symbol") { _typeof = function _typeof(obj) { return typeof obj; }; } else { _typeof = function _typeof(obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; }; } return _typeof(obj); }

/**
 * @copyright 2015 Tridium, Inc. All Rights Reserved.
 * @author Gareth Johnson
 */

/**
 * Configurable Properties for Widgets.
 * 
 * @module bajaux/Properties
 */
define(['Promise', 'underscore', 'bajaux/events', 'nmodule/js/rc/tinyevents/tinyevents', 'lex!'], function (Promise, _, events, tinyevents, lex) {
  "use strict";

  var has = _.has,
      isArray = _.isArray,
      isBoolean = _.isBoolean,
      isEqual = _.isEqual,
      isNumber = _.isNumber,
      isObject = _.isObject,
      isString = _.isString,
      isSymbol = _.isSymbol,
      isUndefined = _.isUndefined,
      pluck = _.pluck;
  var METADATA_CHANGED = events.METADATA_CHANGED,
      PROPERTY_ADDED = events.PROPERTY_ADDED,
      PROPERTY_CHANGED = events.PROPERTY_CHANGED,
      PROPERTY_REMOVED = events.PROPERTY_REMOVED; ////////////////////////////////////////////////////////////////
  // Support functions
  ////////////////////////////////////////////////////////////////

  function createPropertyTriggerName(name) {
    return PROPERTY_CHANGED + ":" + name;
  }

  function createMetadataTriggerName(name) {
    return METADATA_CHANGED + ":" + name;
  }
  /**
   * Normalize metadata to an object literal format where each key maps to an object literal
   * containing value/typeSpec pairs
   * @param {module:bajaux/Properties} properties Parent properties instance
   * @param {module:bajaux/Properties | Object} metadataObj A Properties or Object literal metadata specification
   * @return {Object} Object literal containing meta data keys mapped to {value, typeSpec} pairs
   */


  function toMetadata(properties, metadataObj) {
    var metadata = {},
        typeSpec;

    if (metadataObj instanceof properties.constructor) {
      metadataObj.each(function (index, name, value) {
        metadata[name] = {
          value: value,
          typeSpec: this.get(index).typeSpec
        };
      });
    } else if (isObject(metadataObj)) {
      var names = Object.keys(metadataObj);

      for (var i = 0, len = names.length; i < len; ++i) {
        var name = names[i];
        var value = metadataObj[name]; //check if value is of type {typeSpec: ..., value: ...} (sent down by encodeFacets())

        if (isObject(value) && value.typeSpec && value.value !== null) {
          metadata[name] = {
            value: value.value,
            typeSpec: value.typeSpec
          };
        } else {
          typeSpec = isNumber(value) ? "baja:Double" : isBoolean(value) ? "baja:Boolean" : "baja:String";
          metadata[name] = {
            value: value,
            typeSpec: typeSpec
          };
        }
      }
    }

    return metadata;
  }
  /**
   * Apply new property values to the Properties instance. This will not
   * trigger any modified events.
   * 
   * @inner
   * @param {module:bajaux/Properties} props
   * @param {Array.<Object>} arr array of property configs (same structure
   * as passed to `addAll`)
   * @returns {Object} object with `added` (prop names that were added),
   * `changedNames` (prop names that already existed but had value changed), and
   * `changedValues` (changed values) properties
   */


  function addProperties(props, arr) {
    var array = props.$array,
        map = props.$map,
        added = [],
        changedNames = [],
        changedValues = [];

    for (var i = 0, len = arr.length; i < len; ++i) {
      var prop = arr[i];

      if (!prop || !prop.name) {
        continue;
      }

      var name = prop.name,
          value = prop.value;
      var shouldFireEvents = void 0;

      if (isSymbol(name)) {
        shouldFireEvents = false;
        prop.hidden = true;
        prop["transient"] = true;
      } else {
        shouldFireEvents = true;
      } // Some smart defaults for the Type Spec if it's not specified.


      if (!prop.typeSpec) {
        prop.typeSpec = toBajaTypeSpec(value);
      } //check for metadata property and normalize the meta information


      prop.metadata = prop.metadata && toMetadata(props, prop.metadata);

      if (!map[name]) {
        array.push(prop);
        map[name] = prop;

        if (shouldFireEvents) {
          added.push(name);
          changedNames.push(name);
          changedValues.push(value);
        }
      } else {
        for (var _i = 0, _len = array.length; _i < _len; ++_i) {
          if (array[_i].name === name) {
            if (shouldFireEvents && value !== array[_i].value) {
              changedNames.push(name);
              changedValues.push(value);
            }

            array[_i] = prop;
            break;
          }
        }

        map[name] = prop;
      }
    }

    return {
      added: added,
      changedNames: changedNames,
      changedValues: changedValues
    };
  }

  function applyPropToObjectLiteral(obj, name, prop) {
    var existingProp = obj[name] || (obj[name] = {});

    if (has(prop, 'value') || has(prop, 'defaultValue')) {
      Object.assign(existingProp, prop);
    } else {
      existingProp.value = prop;
    }
  }

  function isIdentifier(val) {
    return isString(val) || isSymbol(val);
  }

  function toBajaTypeSpec(val) {
    if (isBoolean(val)) {
      return 'baja:Boolean';
    }

    if (isNumber(val)) {
      return 'baja:Double';
    }

    return 'baja:String';
  }

  function getAllKeysAndSymbols(obj) {
    return Object.keys(obj).concat(Object.getOwnPropertySymbols(obj));
  }

  function clone(obj) {
    return Object.assign({}, obj);
  } ////////////////////////////////////////////////////////////////
  // Properties
  ////////////////////////////////////////////////////////////////

  /**
   * The properties for a Widget. These Properties have many uses:
   *
   * - When embedding a bajaux Widget on a Px page, they will be shown and editable in the Px
   *   Editor.
   * - When a Widget is a field editor for a Property on a Niagara Component or Struct, that
   *   Property's slot facets will be converted into Widget Properties to customize that field
   *   editor's behavior.
   * - In your bajaux code, you can apply Properties to your own Widgets to customize their
   *   behavior any way you like.
   * 
   * @class
   * @alias module:bajaux/Properties
   * @param {Object|Array.<module:bajaux/Properties~PropertyDefinition>|module:bajaux/Properties} [obj] an initial
   * set of properties with which to initialize this Properties instance. This
   * can be an object literal, an array of object literal Property definitions, or another
   * Properties instance.
   * 
   * @example
   * 
   * <caption>Create a Properties instance with an object literal.</caption>
   * 
   * var props = new Properties({
   *   myProp: 'value',
   *   myHiddenProp: { value: 'hiddenValue', hidden: true }
   * });
   * props.getValue('myProp'); // 'value'
   * props.getValue('myHiddenProp'); // 'hiddenValue'
   * props.get('myHiddenProp').hidden); // true
   * 
   * @example
   * 
   * <caption>Create a Properties instance with an array. Equivalent to the
   * above.</caption>
   * 
   * var props = new Properties([
   *   { name: 'myProp', value: 'value' },
   *   { name: 'myHiddenProp', value: 'hiddenValue', hidden: true }
   * ]);
   *
   * @example
   *
   * <caption>Create a Properties instance with a defaultValue for `myProp`.
   * </caption>
   *
   * var props = new Properties([
   *   { name: 'myProp', value: 'value', defaultValue: 'this is default' },
   *   { name: 'myHiddenProp', value: 'hiddenValue' }
   * ]);
   *
   * props.getValue('myProp'); // 'value'
   * props.getDefaultValue('myProp'); // 'this is default'
   *
   * props.setValue('myProp', undefined); // make the property value undefined
   *
   * props.getValue('myProp'); // 'this is default'
   *
   * props.setValue('myProp', null);
   * props.getValue('myProp'); // null
   *
   * props.getValue('propThatDoesNotExist');  // null
   */


  var Properties = function Properties(obj) {
    var that = this;
    that.$array = [];
    that.$map = {};

    if (obj) {
      if (obj instanceof Properties) {
        return obj.clone();
      } else if (isArray(obj)) {
        addProperties(that, obj);
      } else if (_typeof(obj) === 'object') {
        var names = getAllKeysAndSymbols(obj);
        var arr = [];

        for (var i = 0, len = names.length; i < len; ++i) {
          var name = names[i];
          var mem = obj[name];

          if (has(mem, 'value') || has(mem, 'defaultValue')) {
            arr.push(Object.assign(mem, {
              name: name
            }));
          } else {
            arr.push({
              name: name,
              value: mem
            });
          }
        }

        addProperties(that, arr);
      }
    }

    tinyevents(this);
  };
  /**
   * Create a new Properties instance containing the merged properties of 
   * one or more other Properties instances. Properties of instances later in
   * the argument list will override properties of earlier instances.
   * 
   * Each argument can be of any type acceptable to the Properties constructor
   * (`Object`, `Array`, or `Properties`).
   * 
   * @returns {module:bajaux/Properties}
   * 
   * @example
   * var mergedProps = Properties.extend(
   *   { myProp: 'a' },
   *   new Properties({ myProp: { value: 'a2', hidden: true } })
   * );
   * mergedProps.getValue('myProp'); // 'a2'
   * mergedProps.get('myProp').hidden; // true
   */


  Properties.extend = function () {
    var obj = {};

    for (var i = 0, len = arguments.length; i < len; ++i) {
      var arg = arguments[i];

      if (!arg) {
        continue;
      }

      if (arg instanceof Properties) {
        arg = arg.$array;
      }

      if (isArray(arg)) {
        for (var _i2 = 0, _len2 = arg.length; _i2 < _len2; ++_i2) {
          var prop = arg[_i2];
          applyPropToObjectLiteral(obj, prop.name, prop);
        }
      } else {
        var names = getAllKeysAndSymbols(arg);

        for (var _i3 = 0, _len3 = names.length; _i3 < _len3; ++_i3) {
          var name = names[_i3];
          applyPropToObjectLiteral(obj, name, arg[name]);
        }
      }
    }

    return new Properties(obj);
  };
  /**
   * Add a Property.
   *
   * Please note, if the Property isn't transient, it's value may be saved and loaded
   * elsewhere (for example, in the case of Px, reloaded from a Px file).
   * 
   * If the property does not already exist on this Properties instance, this
   * will emit a `PROPERTY_ADDED` event, with an array (of length 1) of the
   * property names added.
   *
   * As of Niagara 4.14, the property name can also be a Symbol. When the name is a Symbol, the
   * property is automatically made transient and hidden, and it will not fire added, removed, or
   * changed events. It will also not be included in iterative functions like `.each()` and
   * `.get()`.
   * 
   * @param {module:bajaux/Properties~PropertyDefinition|string} prop The Property object to be
   * added, or the name of the Property.
   * @param {*} [value] if passing a string name as the first argument, pass the
   * value here as the second.
   * @returns {module:bajaux/Properties} this Properties instance.
   *
   * @example
   *   <caption>Add a Property</caption>
   *   widget.properties().add("foo", true);
   *
   * @example
   *   <caption>Add a hidden Property</caption>
   *   widget.properties().add({
   *     name: "foo",
   *     value: true,
   *     hidden: true
   *   });
   *
   * @example
   *   <caption>Add a transient, readonly, hidden Property</caption>
   *   widget.properties().add({
   *     name: "foo",
   *     value: true,
   *     hidden: true,
   *     transient: true,
   *     readonly: true
   *   });
   *
   * @example
   *   <caption>Add a Property that maps to the baja:Weekday FrozenEnum in Niagara</caption>
   *   widget.properties().add({
   *     name: "weekday",
   *     value: "tuesday",
   *     typeSpec: "baja:Weekday"
   *   });
   */


  Properties.prototype.add = function add(prop, value) {
    return this.addAll([isIdentifier(prop) ? {
      name: prop,
      value: value
    } : clone(prop)]);
  };
  /**
   * Add a number of properties at once. The object literal configuration 
   * for each property is the same as for `add()`.
   * 
   * @param {Array.<module:bajaux/Properties~PropertyDefinition>|module:bajaux/Properties} arr an
   * array of property definitions, or a `Properties` instance to copy onto this one
   * @returns {module:bajaux/Properties} this Properties instance.
   */


  Properties.prototype.addAll = function (arr) {
    if (arr instanceof Properties) {
      arr = arr.$array;
    }

    if (!Array.isArray(arr)) {
      arr = Array.prototype.slice.call(arguments);
    }

    var that = this,
        results = addProperties(this, arr),
        added = results.added,
        changedNames = results.changedNames,
        changedValues = results.changedValues;

    if (added.length) {
      that.$emit(PROPERTY_ADDED, added);
    }

    if (changedNames.length) {
      that.$emit(PROPERTY_CHANGED, changedNames, changedValues);

      for (var i = 0, len = changedNames.length; i < len; ++i) {
        var name = changedNames[i];
        that.$emit(createPropertyTriggerName(name), name, changedValues[i]);
      }
    }

    return that;
  };
  /**
   * Remove a Property.
   *
   * @param {String} name The name of the Property to remove.
   * @returns {module:bajaux/Properties} this Properties instance.
   */


  Properties.prototype.remove = function remove(name) {
    var that = this,
        array = that.$array,
        map = that.$map,
        prop = map[name],
        i; // If the Property is registered then remove it

    if (prop) {
      delete map[name];

      for (i = 0; i < array.length; ++i) {
        if (array[i].name === name) {
          array.splice(i, 1);
          break;
        }
      }

      if (!isSymbol(name)) {
        that.$emit(PROPERTY_REMOVED, prop.name, prop);
      }
    }

    return that;
  };

  function getPropFromNameOrIndex(properties, name) {
    return isIdentifier(name) ? properties.$map[name] : properties.$array[name];
  }
  /**
   * Return true if the Property can be found via its name or index.
   * 
   * @param  {String|Number|Symbol} name The identifier or index of the Property to look up.
   * @return {Boolean} true if the Property is found.
   */


  Properties.prototype.has = function has(name) {
    return !!getPropFromNameOrIndex(this, name);
  };
  /**
   * Return the total number of Properties.
   * 
   * @return {Number} returns the total number of Properties.
   */


  Properties.prototype.size = function size() {
    return this.$array.length;
  };
  /**
   * If no name is specified then return an object containing all the Property names and values.
   * If a name/index is specified then return a Property's value or null if nothing can be found.
   *
   * Property values will fall back to `defaultValue` if `value` is not set.
   * 
   * @param  {String|Number} [name] If specified, the name of the Property to return or the
   * Property's index. If this parameter is not specified, an object literal containing all the
   * property values will be returned (same as `.toObject()`).
   * @param [defVal] If specified, this will return if a value can't be found providing the first argument is
   * a String. This will override the defaultValue for the given property.
   * @returns {*|null} The Property value, Property default value, or null if nothing is found.
   */


  Properties.prototype.getValue = function getValue(name, defVal) {
    if (isUndefined(name)) {
      return this.toValueMap();
    }

    var prop = this.get(name);

    if (prop === null) {
      return isUndefined(defVal) ? null : defVal;
    }

    var value = prop.value;

    if (!isUndefined(value)) {
      return value;
    }

    if (!isUndefined(defVal)) {
      return defVal;
    }

    var defaultValue = prop.defaultValue;

    if (isUndefined(defaultValue)) {
      return null;
    }

    return defaultValue;
  };
  /**
   * If a name/index is specified then return a Property's default value or null if nothing can be found.
   * If no name is specified then return an object containing all the Property names and default values.
   *
   * @param  {String|Number} [name] If specified, the name of the Property to return or the Property's index.
   * If this parameter is not specified, an object containing all the property default values will be returned.
   *
   * @returns {*|null} The Property's default value or null if nothing is found.
   */


  Properties.prototype.getDefaultValue = function (name) {
    if (isUndefined(name)) {
      var retVal = {};
      var array = this.$array;

      for (var i = 0, len = array.length; i < len; ++i) {
        var _array$i = array[i],
            _name = _array$i.name,
            defaultValue = _array$i.defaultValue;

        if (!isSymbol(_name)) {
          retVal[_name] = defaultValue;
        }
      }

      return retVal;
    }

    return this.get(name, "defaultValue");
  };
  /**
   * Set the value for a given property name 
   * @inner
   * @param {module:bajaux/Properties} properties
   * @param {String} name Property name
   * @param value Property value
   * @param options Additional options
   * @return {boolean|Object} Returns property if it's value was modified or false if it wasn't
   */


  function setPropertyValue(properties, name, value, options) {
    var prop = getPropFromNameOrIndex(properties, name),
        modified = false;

    if (!prop) {
      throw new Error();
    }

    if (!isEqual(prop.value, value)) {
      prop.value = value;
      modified = true;

      if (!isSymbol(name)) {
        properties.emit(createPropertyTriggerName(prop.name), prop.name, value, options);
      }
    }

    return modified && prop;
  }
  /**
   * Set the metadata for a given property name
   * @inner
   * @param {module:bajaux/Properties} properties
   * @param {String} name Property name
   * @param {Object} metadata Object literal containing metadata key/value pairs
   * @param options Additional options
   * @return {boolean|Object} Returns property if it's value was modified or false if it wasn't
   */


  function setPropertyMetadata(properties, name, metadata, options) {
    var prop = getPropFromNameOrIndex(properties, name),
        modified = false;

    if (!prop) {
      throw new Error();
    }

    metadata = toMetadata(properties, metadata);

    if (!isEqual(prop.metadata, metadata)) {
      prop.metadata = metadata;
      modified = true;
      properties.emit(createMetadataTriggerName(prop.name), prop.name, prop.metadata, options);
    }

    return modified && prop;
  }
  /**
   * Set the specified attribute on a property object
   * @private
   * @param {module:bajaux/Properties} properties Properties instance
   * @param {String|Object} propName The name of the Property we're going to set or an object literal
   * containing many property/attribute pairs to be set.
   * @param {String} attrName The name of the property attribute being set (e.g 'value' or 'metadata')
   * @param attrValue Value of the attribute being set
   * @param {Function} handler Function handler responsible for setting attribute value
   * @param {String} event Event name triggered when an attribute is set
   * @param options Additional options
   * @return {boolean} Return false if the property attribute could not be set
   */


  function setPropertyAttribute(properties, propName, attrName, attrValue, handler, event, options) {
    var prop,
        names,
        values,
        p,
        obj,
        res = true;

    if (isIdentifier(propName) || isNumber(propName)) {
      try {
        prop = handler(properties, propName, attrValue, options);

        if (prop && !isSymbol(propName)) {
          properties.emit(event, [prop.name], [prop[attrName]], options);
        }
      } catch (err) {
        res = false;
      }
    } else {
      obj = propName;
      options = attrValue;
      names = [];
      values = [];

      for (p in obj) {
        if (obj.hasOwnProperty(p)) {
          try {
            prop = handler(properties, p, obj[p], options);

            if (prop) {
              names.push(prop.name);
              values.push(prop[attrName]);
            }
          } catch (err) {
            res = false;
          }
        }
      }

      if (names.length) {
        properties.emit(event, names, values, options);
      }
    }

    return res;
  }
  /**
   * If an object is specified as the first argument, it will be iterated
   * through with object's properties being set as values.
   *
   * If a name/index is specified along with a value, the value for the
   * particular value will be set.
   *
   * A Widget can detect Property changes by implemented a method called
   * `changed`. The changed call back will have the Property name and new value
   * passed to it. A developer can then override 'doChanged' to handle any
   * callbacks in their own widget subclasses.
   *
   * @param {String|Object} name The name of the Property we're going to set or a
   * an object containing many values to be set.
   * @param value The value to be set.
   * @param [options] An optional parameter that is passed down into any changed
   * callbacks or event handlers.
   * @returns {Boolean} Return false if at least one of the properties wasn't found.
   */


  Properties.prototype.setValue = function setValue(name, value, options) {
    return setPropertyAttribute(this, name, 'value', value, setPropertyValue, PROPERTY_CHANGED, options);
  };
  /**
   * If an object is specified as the first argument, it will be iterated
   * through with object's properties being set as metadata
   *
   * If a name/index is specified along with a value, the metadata for the
   * particular name/index will be set.
   *
   * @param {String|Object} name The name of the Property we're going to set or a
   * an object containing several metadata values to be set. The value for each property name in the object
   * will be set as corresponding metadata for that property
   * @param {Object} metadata The metadata to be set.
   * @param [options] An optional parameter that is passed down into any changed
   * callbacks or event handlers.
   * @returns {Boolean} Return false if at least one of the properties wasn't found.
   * @since Niagara 4.4
   */


  Properties.prototype.setMetadata = function setMetadata(name, metadata, options) {
    return setPropertyAttribute(this, name, 'metadata', metadata, setPropertyMetadata, METADATA_CHANGED, options);
  };
  /**
   * Called to detach any event handlers from a Property or to
   * stop listening to all Property change events.
   * 
   * @param  {String} name The Property name to remove.
   * @param  {Function} [func] The event handler to remove.
   * @return {module:bajaux/Properties} The Properties instance.
   */


  Properties.prototype.off = function off(name, func) {
    if (func) {
      this.removeListener(name, func);
    } else {
      this.removeAllListeners(name);
    }

    return this;
  };
  /**
   * Return a Property's index via its name or -1 if it can't be found.
   *
   * @param {String} name The name of the Property to look up the index number for.
   * @returns {Number} Returns the index number of the Property.
   */


  Properties.prototype.getIndex = function getIndex(name) {
    var array = this.$array;

    for (var i = 0, len = array.length; i < len; ++i) {
      if (array[i].name === name) {
        return i;
      }
    }

    return -1;
  };
  /**
   * Return a promise that will resolve once the display name of the Property
   * has been resolved.
   *
   * If the Property doesn't have a display name, the Property's name will 
   * be used instead.
   *
   * Please note, a display name can be in the format of a Lexicon format. For instance,
   * `%lexicon(moduleName:keyName)%`.
   *
   * @param {String|Number} name The name or index of the Property.
   * @returns The display name of the Property.
   */


  Properties.prototype.toDisplayName = function toDisplayName(name) {
    var prop = getPropFromNameOrIndex(this, name);

    if (!prop) {
      return Promise.resolve('');
    }

    if (prop.displayName) {
      return lex.format(prop.displayName)["catch"](function () {
        return prop.displayName;
      });
    } else {
      return Promise.resolve(prop.name);
    }
  };
  /**
   * Convert this Properties instance into a new raw object literal. The object
   * keys will be the property names, and the values will be the property
   * values (as returned by `#getValue()`). Note that any metadata about each
   * Property will be lost.
   *
   * This function will be useful for converting Properties into a context
   * object.
   * 
   * @returns {Object}
   * @see module:bajaux/Properties#toObject
   * @since Niagara 4.9 (replaces toValueMap, which still works)
   *
   * @example
   * <caption>Property Sheet converts slot facets into Widget properties. I
   * need to use those facets in my field editor for number formatting
   * purposes.</caption>
   *
   * MyFieldEditor.prototype.numberToString = function (number) {
   *   var cx = this.properties().toObject();
   *   if (typeof cx.precision !== 'number') {
   *     cx.precision = 2;
   *   }
   *   return number.toString(cx);
   * };
   */


  Properties.prototype.toObject = Properties.prototype.toValueMap = function () {
    var obj = {};
    this.each(function (i, key, value) {
      obj[key] = value;
    });
    return obj;
  };
  /**
   * If no arguments are specified, a copy of the internal Properties array will be returned.
   * If only the name is specified, return a copy of the Property for the given name or index. 
   * If a name/index and an attribute name is specified, then return the attribute of a Property.
   * If no particular value can be found then return null;
   * 
   * @param  {String|Number|Symbol} [name] The identifier or index of the Property to look up. If not
   * specified, a copy of the internal Property array will be returned.
   * @param  {String} [attrName] If specified, this will retrieve a specific attribute
   * of the Property. For example, specifying 'value' will get the value of the Property.
   * @param [defAttrValue] If specified, this value will be returned if the attribute name
   * can't be found.
   * @returns {module:bajaux/Properties~PropertyDefinition|*|null|Array.<module:bajaux/Properties~PropertyDefinition>} A copy of the Property definition;
   * the attribute value if requested; or null if the specified Property can't be found; or if no
   * name specified, an array of all property definitions.
   */


  Properties.prototype.get = function get(name, attrName, defAttrValue) {
    if (!isUndefined(name)) {
      var prop = getPropFromNameOrIndex(this, name);
      defAttrValue = isUndefined(defAttrValue) ? null : defAttrValue;

      if (prop) {
        if (attrName) {
          return isUndefined(prop[attrName]) ? defAttrValue : prop[attrName];
        } else {
          return clone(prop);
        }
      } else if (attrName) {
        return defAttrValue;
      }
    } else {
      var returnArray = [];
      var array = this.$array;

      for (var i = 0, len = array.length; i < len; ++i) {
        var _prop = array[i];

        if (!isSymbol(_prop.name)) {
          returnArray.push(clone(_prop));
        }
      }

      return returnArray;
    }

    return null;
  };
  /**
   * Return the corresponding metadata object literal for the Property
   *
   * @param  {String|Number|Symbol} name The identifier or index of the Property to look up.
   * @returns {Object|null} The metadata object literal, or null if the Property could not be found
   */


  Properties.prototype.getMetadata = function (name) {
    return this.get(name, 'metadata', {});
  };
  /**
   * Iterate through each Property.
   * 
   * @param  {function(number, string, *)} func The function to be called for each Property
   * found in the array. This function will have the index, name and value of
   * the Property passed to it. The Context of the function callback will be 
   * the Properties instance.
   * @returns {module:bajaux/Properties} this Properties instance.
   */


  Properties.prototype.each = function each(func) {
    for (var i = 0, arr = this.$array, len = arr.length; i < len; ++i) {
      var name = arr[i].name;

      if (!isSymbol(name)) {
        func.call(this, i, name, this.getValue(name));
      }
    }

    return this;
  };
  /**
   * Build a new Properties instance consisting of a subset of the properties
   * contained within this one. Useful for propagating a specific set of
   * properties down to a child widget.
   * 
   * @param {Array.<string|number|Symbol>} keys which keys to include in the subset. These can also
   * be passed as individual varargs.
   * @returns {module:bajaux/Properties}
   * @example
   * props.subset([ 'prop1', 'prop2', 'prop3' ]);
   * props.subset('prop1', 'prop2', 'prop3');
   */


  Properties.prototype.subset = function (keys) {
    var _this = this;

    if (!Array.isArray(keys)) {
      keys = Array.prototype.slice.call(arguments);
    }

    return new Properties(keys.map(function (key) {
      return _this.get(key);
    }));
  };
  /**
   * Return a clone of this Properties object that can be modified without
   * changing the original.
   * 
   * @returns {module:bajaux/Properties}
   */


  Properties.prototype.clone = function () {
    return this.subset(pluck(this.$array, 'name'));
  };
  /**
   * Sets a modified flag everytime a property is changed.
   * This private API may be removed in the future.
   * @since Niagara 4.10
   * @private
   */


  Properties.prototype.$emit = function () {
    this.$modified = true;
    return this.emit.apply(this, arguments);
  };

  return Properties;
  /**
   * Defines one property in a `Properties` instance.
   *
   * @typedef module:bajaux/Properties~PropertyDefinition
   * @property {string|Symbol} name The name of the property. As of Niagara 4.14, this can also be a
   * Symbol.
   * @property {*} value The property value.
   * @property {string} [displayName] The display name of the property. For translated values,
   * this can reference lexicon values in the same style as baja.Format (other Format behaviors
   * _besides_ lexicons are not supported). For example, `%lexicon(baja:tuesday)%`.
   * @property {boolean} [transient] A hint to an external editor that it doesn't need to save
   * this property.
   * @property {boolean} [hidden] A hint to an external editor to hide this property.
   * @property {boolean} [readonly] A hint to an external editor to make the editor for
   * this property readonly.
   * @property {string} [typeSpec] **Deprecated. As of Niagara 4.14, Workbench and all widget
   * containers support using BajaScript Simples directly as the property value, and will infer the
   * Type from that value directly.** A hint to Niagara on what Simple Niagara Type to use when
   * encoding/decoding the Property. If the Type is a FrozenEnum, the `value` should be the enum's
   * tag.
   * @property {Object<string, module:bajaux/Properties~MetadataDefinition>} [metadata] An optional
   * collection of metadata to be applied to the property.
   */

  /**
   * Defines one entry in a property's metadata.
   *
   * @typedef module:bajaux/Properties~MetadataDefinition
   * @property {*} value the metadata value
   * @property {string} typeSpec the type spec of the metadata value
   *
   */
});
