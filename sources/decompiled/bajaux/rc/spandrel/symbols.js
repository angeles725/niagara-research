/**
 * @copyright 2021 Tridium, Inc. All Rights Reserved.
 */
define([], function () {
  'use strict';
  /**
   * Stores various symbols that `spandrel` uses to store custom values on Widgets and expandos on
   * DOM nodes, without risking conflicts with existing property names.
   *
   * API Status: **Private**
   * @exports module:bajaux/spandrel/symbols
   */

  var symbols = {
    /**
     * store the spandrel key on the DOM element itself
     */
    KEY_SYMBOL: Symbol('spandrelKey'),

    /**
     * Mark an object as spandrel data, differentiating it from a map from keys to widget
     * definitions.
     *
     * @example
     * // this object would define two widget children with spandrel keys of `fooLabel` and `barLabel`.
     * {
     *   fooLabel: { dom: '<label/>', value: 'foo' },
     *   barLabel: { dom: '<label/>', value: 'bar' }
     * }
     * // without IS_SPANDREL_SYMBOL, spandrel wouldn't know this was a widget definition. it would
     * // think you were trying to create two child widgets named 'dom' and 'value'.
     * { dom: '<div/>', value: 'foo', [IS_SPANDREL_SYMBOL]: true }
     */
    IS_SPANDREL_SYMBOL: Symbol('isSpandrel'),

    /**
     * Provides priority numbering for the ordering of spandrel event handlers. Handlers with lower
     * numbers run first.
     * @since Niagara 4.14
     */
    SPANDREL_HANDLER_PRIORITY: Symbol('isSpandrelHandler'),

    /**
     * Mark a DOM element, widget instance, or widget constructor as a dynamic spandrel widget
     */
    IS_DYNAMIC_SYMBOL: Symbol('isDynamic'),

    /**
     * Store a reference to the root spandrel widget on one of its descendents. Root is different
     * from owner, in that if you build a spandrel widget out of other spandrel widgets, every
     * single child widget in that whole tree will store a reference to the very topmost spandrel
     * widget. Suggestive of `$.parents(isSpandrelWidget).last()` in that it finds the furthest-away
     * spandrel widget.
     *
     * @example
     * class Kid extends spandrel(<div className="kid"><span>Hello</span></div>) {}
     * class Parent extends spandrel(<div className="parent"><Kid className="myKid"/></div>) {}
     *
     * fe.buildFor({ dom: $('#foo'), type: Parent })
     *   .then((parent) => {
     *     const kid = parent.queryWidget('**&#x2f;kid');
     *   });
     *
     * //renders to:
     * <div id="foo">                   Widget.in(el)[ROOT_SYMBOL] === parent
     *   <div class="parent">           Widget.in(el)[ROOT_SYMBOL] === parent
     *     <div class="kidContainer">   Widget.in(el)[ROOT_SYMBOL] === parent
     *       <div class="kid">          Widget.in(el)[ROOT_SYMBOL] === parent
     *         <span>Hello</span>       Widget.in(el)[ROOT_SYMBOL] === parent
     *       </div>
     *     </div>
     *   </div>
     * </div>
     */
    ROOT_SYMBOL: Symbol('spandrelRoot'),

    /**
     * Store a reference to the owner spandrel widget on one of its descendents. Owner is different
     * from root, in that if you build a spandrel widget out of other spandrel widgets, each child
     * widget will store a reference to the spandrel widget that actually rendered it. Suggestive
     * of `$.parent().closest(isSpandrelWidget)` in that it finds the closest spandrel widget (but
     * not itself).
     *
     * @example
     * class Kid extends spandrel(<div className="kid"><span>Hello</span></div>) {}
     * class Parent extends spandrel(<div className="parent"><Kid className="kidContainer" spandrelKey="kid"/></div>) {}
     *
     * fe.buildFor({ dom: $('#foo'), type: Parent })
     *   .then((parent) => {
     *     const kid = parent.queryWidget('**&#x2f;kid');
     *   });
     *
     * //renders to:
     * <div id="foo">                   Widget.in(el)[OWNER_SYMBOL] === undefined (spandrel didn't create this element)
     *   <div class="parent">           Widget.in(el)[OWNER_SYMBOL] === parent
     *     <div class="kidContainer">   Widget.in(el)[OWNER_SYMBOL] === parent (parent created this element)
     *       <div class="kid">          Widget.in(el)[OWNER_SYMBOL] === kid (kid created this element)
     *         <span>Hello</span>       Widget.in(el)[OWNER_SYMBOL] === kid
     *       </div>
     *     </div>
     *   </div>
     * </div>
     */
    OWNER_SYMBOL: Symbol('spandrelOwner'),

    /**
     * Store which child widgets a spandrel widget should validate when it itself is validated.
     * This will also mark a validator function (added to a widget's validators) as being the
     * inline validator inserted by spandrel, of which there can be only one at a time.
     */
    TO_VALIDATE_SYMBOL: Symbol('spandrelKidsToValidate'),

    /**
     * store a spandrel widget's depth within the tree (root === 0)
     */
    DEPTH_SYMBOL: Symbol('spandrelDepth'),

    /**
     * when a widget is bound to state, keep track of which state key on which owner widgets it is
     * passing state values up to. this gets expando-ed onto the actual DOM element in which the
     * bound widget is initialized.
     */
    STATE_BINDING_SYMBOL: Symbol('stateBinding'),

    /**
     * when a spandrel member is defined as lax, allow state changes to propagate to it, even if it is modified by the user
     */
    LAX_SYMBOL: Symbol('lax'),

    /**
     *  when a spandrel member is created as a standin for a DOM element with no type otherwise specified
     */
    IS_ELEMENT_SYMBOL: Symbol('isElement'),

    /**
     * a private state variable that just keeps track of whenever a spandrel
     * widget performs a render, so we can make decisions based on who rendered
     * after whom.
     *
     * this gets used in two ways. during a load(), it gets stored on the widget
     * instance itself, so it can be checked as many times as needed until the next
     * load. during a rerender(), it's ephemeral, only part of the state for that
     * one rerender call. this distinction strengthens the idea that load() calls are
     * top-down and decisive while rerender() calls are bottom-up and subject to
     * overrule.
     */
    RENDER_TICKS_SYMBOL: Symbol('renderTicks'),

    /**
     * Assign each spandrel widget a counter - at this time, solely for use in logging
     * so individual instances can be traced through the logs.
     */
    INSTANCE_COUNTER_SYMBOL: Symbol('instanceCounter'),

    /**
     * A JSX element will have a declared type. This could be a Widget constructor, or for a raw
     * DOM element, a string such as `div`.
     * @see module:bajaux/spandrel/jsx~JsxInstructions
     */
    JSX_TYPE_SYMBOL: Symbol('type'),

    /**
     * A JSX element may have declared props. Do not confuse these with Widget Properties. These
     * include things like `className`, `style`, or in the case of a Widget, `enabled`, `readonly`,
     * `properties` etc.
     * @see module:bajaux/spandrel/jsx~JsxInstructions
     */
    JSX_PROPS_SYMBOL: Symbol('props'),

    /**
     * A JSX element may have declared children. These also take the form of JSX instructions to be
     * converted to spandrel data as needed.
     * @see module:bajaux/spandrel/jsx~JsxInstructions
     */
    JSX_KIDS_SYMBOL: Symbol('kids'),

    /**
     * A piece of spandrel data may have additional work that needs to be performed before it's
     * ready to actually be consumed by spandrel. This would include work that can only be performed
     * with knowledge of child elements (since JSX is built from bottom-up instead of top-down, this
     * can only be done just before rendering, when the full tree of nodes is complete).
     *
     * A spandrel object may place a function under this symbol, and spandrel will call it (passing
     * the `WidgetState` of the rendering/owner widget) immediately before rendering the widget
     * tree.
     * @since Niagara 4.14
     */
    REIFY_SYMBOL: Symbol('reify'),

    /**
     * @since Niagara 4.13
     * Stores a reference to the spandrel widget loaded into a jQuery DOM.
     * @example
     * class StringEditor extends spandrel((str) => <input type="text" value={ str } />) {}
     * const dom = $('<div/>');
     * const editor = new StringEditor();
     * editor.initialize(dom)
     *  .then(() => editor.load('foo'))
     * 
     * //renders to:
     * <div>                                dom[0][WIDGET_SYMBOL] === editor
     *   <input type="text" value="foo" />
     * </div>
     */
    WIDGET_SYMBOL: Symbol('widget')
  };
  return symbols;
});
