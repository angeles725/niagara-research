/**
 * @copyright 2019 Tridium, Inc. All Rights Reserved.
 * @author Logan Byam
 */

/**
 * Defines the valid overall data structures that `spandrel` can accept as input for creating widget
 * structures. These valid inputs are:
 *
 * - a string of raw HTML
 * - an array of widget definitions (with properties `dom`, `value`, `type`, etc.) that a
 * `WidgetManager` can use to construct zero or more widgets
 * - an object literal, mapping identification keys to widget definitions
 *
 * This data may be passed to `spandrel()` directly, or a `spandrel` function may dynamically
 * generate it.
 *
 * @typedef {(
 *   string|
 *   module:bajaux/spandrel~WidgetDefinition|
 *   Array.<string|module:bajaux/spandrel~WidgetDefinition>|
 *   Object<string,string|module:bajaux/spandrel~WidgetDefinition>
 * )} module:bajaux/spandrel~SpandrelData
 */

/**
 * The data structure that `spandrel` accepts as input to define one widget.
 *
 * @typedef {module:bajaux/lifecycle/WidgetManager~BuildParams} module:bajaux/spandrel~WidgetDefinition
 * @property {(
 *   Array.<string|module:bajaux/spandrel~WidgetDefinition>|
 *   Object<string,module:bajaux/spandrel~WidgetDefinition>)} [kids]
 * @property {Array} [on] optional event handler definitions
 * @property {string} [key] the spandrel key to use to identify this widget. If omitted, spandrel
 * will generate one internally.
 * @property {boolean} [bind] if true, enables state binding. Defaults `bindKey` to the spandrel
 * key.
 * @property {string} [bindKey] if present, enables state binding. Defines the state key in the
 * owner widget to which modified values from this widget will propagate.
 * @property {module:bajaux/spandrel~StateBinding} [stateBinding] an explicitly defined state
 * binding can also be declared here. Consider this property **private** - `bindKey`
 * is the publicly supported binding mechanism, and `stateBinding` is only passed around
 * internally.
 */

/**
 * Defines the _internal_ data structure that defines one complete Spandrel widget. A Spandrel widget
 * consists of a set of zero or more child widgets (`members`) and a set of zero
 * or more event handlers (`on`) that apply at its own level.
 *
 * @typedef {object} module:bajaux/spandrel~BuildContext
 * @property {Array.<module:bajaux/spandrel~Member>} members
 * @property {Array} on
 */

/**
 * The _internal_ data structure defining one member of a composite Spandrel widget. Each one has
 * a unique identifying key (these are numbers increasing if configured as an array;
 * object keys if configured as an object literal). The config property is
 * what will be passed to `manager.buildFor()` to construct the widget. These
 * can also be nested using the `kids` property, in which case the nested
 * widgets will also be Spandrel composites.
 *
 * @typedef {object} module:bajaux/spandrel~Member
 * @property {string} key
 * @property {module:bajaux/spandrel~SpandrelBuildParams} config
 * @property {module:bajaux/spandrel~StateBinding} [stateBinding]
 */

/**
 * Represents the binding of a widget to a state key of its owner widget. When a child widget is
 * bound to its owner's state, any MODIFY_EVENTs from that child widget will cause the current value
 * to be read and applied to its owner widget's state.
 *
 * This has a few benefits:
 *
 * 1. It makes it easier to read out the owner widget's current value. It doesn't have to call
 *    read() on all its child widgets - it already has the current values of everything in its own
 *    state.
 * 2. Child widgets can be hidden (like if I check "null" then a text input might disappear). If
 *    that removed widget is bound to state, then its value sticks around, so it can be brought back
 *    if the user then unchecked "null."
 *
 * @typedef {object} module:bajaux/spandrel~StateBinding
 * @property {string} key the state key in the target widget to which we're bound
 * @property {module:bajaux/spandrel/SpandrelWidget} target the target widget
 */

/**
 * When building out a spandrel widget, the data structure used to define the child widgets looks
 * very similar to what WidgetManager uses. It adds support for a few spandrel-specific properties.
 *
 * @typedef {module:bajaux/lifecycle/WidgetManager~BuildParams} module:bajaux/spandrel~SpandrelBuildParams
 * @property {Array.<module:bajaux/spandrel~BuildContext>} [kids] defines what child widgets this
 * spandrel widget will build out. (These child definitions can include their own child definitions
 * and so on.)
 * @property {object} [data] additional spandrel-specific data
 * @property {boolean|function|undefined} [validate] true if this child widget should use its
 * built-in validation; a function if additional validation should be added; falsy if this child no
 * longer needs to be validated
 */
//TODO: this probably goes to Widget/BaseWidget

/**
 * Describes the current state of a widget.
 *
 * @typedef {object} module:bajaux/spandrel~WidgetState
 * @property {module:bajaux/commands/CommandGroup} commands the widget's Commands
 * @property {boolean} enabled true if the widget is enabled (but you probably
 * want `writable` instead)
 * @property {string} formFactor the widget's form factor
 * @property {object} props the widget's Properties, as a key-value map
 * @property {object} properties (same as props)
 * @property {boolean} readonly true if the widget is readonly (but you probably
 * want `writable` instead)
 * @property {function} renderSuper if your widget extends a `spandrel` superclass, this function
 * will render the superclass' `spandrel` data for you to change as needed. See the `spandrel`
 * tutorial for more details.
 * @property {HTMLElement|null} rootElement a copy of the element in which the
 * widget is currently initialized; or `null` if the widget has not yet been
 * initialized. Please note the word "copy!" You can alter the `rootElement`'s
 * `classList` and `style` attributes and they will be applied to the real
 * element; however things like event listeners must be applied in
 * `doInitialize` or use the built-in spandrel event handling functionality.
 * @property {module:bajaux/spandrel/SpandrelWidget} self a reference to this widget
 * @property {boolean} writable true if the widget is enabled and not readonly
 */

/**
 * Describes valid properties to assign to a DOM element when creating spandrel
 * data from JSX.
 *
 * @typedef {object} module:bajaux/spandrel/jsx~Props
 * @property {object|string} [style] the style attribute may be a map of CSS
 * properties to their string values. Any properties with a falsy value will not
 * be applied.
 * @property {string} [className] use `className` instead of `class` to specify
 * CSS classes
 * @property {string} [tagName='div'] when constructing a Widget directly, a DOM
 * element will be dynamically generated in which to instantiate it. The tag
 * name of the element may be specified. `style`, `className` and other
 * attributes will be applied to it as normal.
 * @property {string} [spandrelKey] for ease of use when using `queryWidget`,
 * the `spandrelKey` may be applied to a DOM element or a Widget.
 * @property {object} [spandrelSrc] when using a data type that provides its own
 * `spandrel` data, such as `UxModel`, you may apply it directly to a DOM
 * element as a `spandrelSrc` attribute. The object must have a `toSpandrel`
 * function, that will be called using the DOM element you're generating:
 * `spandrelSrc.toSpandrel({ dom })`.
 */

/**
 * This stores the "outcome" of one spandrel render call - the determination of
 * what data the widget wants to render, and what classes and styles it wants to
 * add to the root element. No actual changes to the DOM are made to create this
 * data - it should be applied to the DOM in a second phase.
 *
 * This data will be cached on the widget after the first render and applied
 * directly to the DOM. On the second and following renders, a new set of this
 * data will be generated and diffed against the previous data, so that only the
 * differences can be applied to the DOM.
 *
 * @typedef {object} module:bajaux/spandrel~SpandrelRenderResults
 * @property {module:bajaux/spandrel~BuildContext} spandrelData
 * @property {string[]} addedClasses
 * @property {Object.<string, string>} addedStyles
 */

/**
 * Represents raw JSX data, before being converted by `spandrel.jsx` into spandrel data. Consider
 * these "instructions" for creating actual spandrel data, and as such they can be modified before
 * the actual spandrel data is created (reified).
 *
 * The documented properties on an instance of `JsxInstructions` are only accessible via Symbol.
 * This is to prevent conflicts with regular DOM element properties, notably `type`.
 *
 * @private
 * @typedef {object} module:bajaux/spandrel/jsx~JsxInstructions
 * @implements {Thenable.<module:bajaux/spandrel~SpandrelData>}
 * @property {string|function} {@link module:bajaux/spandrel/symbols.JSX_TYPE_SYMBOL} the type of
 * the element or widget to create e.g. "div" or `Widget`
 * @property {object} [{@link module:bajaux/spandrel/symbols.JSX_PROPS_SYMBOL}] any additional
 * properties to apply to the element or widget e.g. "style" or "properties"
 * @property {Array.<module:bajaux/spandrel/jsx~JsxInstructions|string|null>} [{@link module:bajaux/spandrel/symbols.JSX_KIDS_SYMBOL}] any
 * child JSX nodes
 * @property {function} then call `.then()`, pass to `Promise.resolve()` etc to perform the
 * conversion from JSX configuration to usable spandrel data.
 */
