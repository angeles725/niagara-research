# Extract — docGraphics.txt (PX Editor / Graphics user guide)

> Preserved excerpts (§5) from the official Tridium/Honeywell Graphics guide, N4.14.0.162 install.
> Source install path: niagara-help/docs-text/docGraphics.txt (9051 lines). Cited by focus px-menu B180+.
> Line anchors below map to the ORIGINAL file line numbers for [CERT-doc] citation.

## docGraphics.txt:450-476 — Px workflow overview
```
The general process of creating presentation views for control logic can follow many different paths. The
major steps generally go as follows:

1. Create your view

    When you create a view, you are creating a relationship between a Px file and a component. The Px file
    defines the view to associate with one or more components of various types, such as folders and points.

2. Add widgets

    After creating a view, you add graphic visualizations (called widgets) to the canvas.

3. Bind your data to the widgets

    To pass data to the widgets, you use data binding. The bound data from the control objects animates
    and updates the widgets.

4. Create a nav file

    To easily find and navigate among views, you can create a customizable navigation tree using a special
    file type: .nav file. You edit the .nav file using the Nav File Editor and assign a particular nav file to a user
    in the user's profile (using the User Manager view).

5. Create and distribute a report

    The reporting function helps you design, display, and deliver data to online views, printed pages, and to
    distribute via email.
```

## docGraphics.txt:714-756 — About Px files + shared-file caution
```
About Px files

A Px file defines the content and presentation of a Px view.

The Px file is a special XML file that describes the components in a database and can be any collection of
components, up to a complete database. All views (wire sheet view, property sheet view, category sheet
view, and so on) can be used on components in a Px file. This means that a Px view can provide a complete
variety of options in the development of dynamic user interfaces.

Figure 6 Basic default Px file in Text Editor View

16                                                                                           March 15, 2023
Niagara Graphics Guide  Chapter 1 About Presentation XML (Px)

This is how a very basic Px file appears in the Text Editor. This file contains an empty canvas pane nested in a
scroll pane.

As you add more objects to the file, ( using the Px Editor) the file looks more like the snippet shown here.
The graphic components, value bindings, and their container elements and attributes are all referenced in
the Px file.

Figure 7 Px file in Text Editor View and in Px Editor View

The elements in the Px file are also graphically represented in the Widget Tree side bar pane.

NOTE: Two types of Px files are available. The standard Px file (includes a scroll pane at the root with a can-
vas pane) or the ReportPxFile (contains a report pane at the root).

Shared Px files

A Px file may be used as part of one or more Px views. Editing a shared Px file affects all views that use it.

Since the bindings within a Px file are always resolved relative to the current ORD, you can reuse the same
Px file across multiple components by specifying bindings with relative ORDs. This allows you to create a sin-
gle presentation and use it in views that are assigned to components that can use an identical graphic layout.
The obvious advantage of file sharing is that you only need to create and edit one Px file for many views,
thus saving time and ensuring consistency among similar applications. However, it requires that you under-
stand the following caution.

CAUTION: Editing a Px file affects all views that use that particular Px file. When you view a component in
the Px Editor, you have its Px file open for editing. If the Px file is shared with other Px views, all of those Px
Views are changed.

```

## docGraphics.txt:793-822 — New Px View wizard
```
You may create and delete Px views using the Slot Sheet view. However, the easiest way to create a new
canvas for a Px view on a component is to use the New Px View wizard.
When you create a view, you are creating a relationship between a Px file and a component. The Px file
defines the view, which may be associated with one or more components of various types, such as folders
and points.
You can assign an existing Px file to the view instead of creating a new one.
Step 1 In the Nav tree, right-click a component and click ViewsNew View.

            The New Px View wizard opens.

Step 2  Enter a View Name.
Step 3
        Unless you specify a different name, the wizard assigns a default Px File name.
Step 4
        Carefully select the Target Media.

        Choosing a type of media has specific benefits when using the Px Editor to create a new view on a
        component for the first time.

        In Niagara, choosing HxPx Media takes full advantage of the HTML5 functionality, which provides
        a significantly improved mobile user experience.

        NOTE: Media values affect the initial contents of a Px file when you create it. Changing a view's
        Media type (on a component Property Sheet) after the Px file is created does not actually change
        the Px file.

        To finish, click the OK button.

        The wizard creates a new Px file in the station's Files folder and names it based on the View
        Name property. The wizard then opens the canvas for the new Px view in the Px Editor.
```

## docGraphics.txt:942-970 — Make Widget wizard (drag-drop -> binding)
```
The Make Widget wizard provides properties for creating widget bindings.

You can add graphic visualizations to your Px page by dragging pre-made widgets from a palette or by
making new widgets. When you make a widget using the Make Widget wizard, the window provides
properties for creating the widget bindings.

Step 1 From the Nav side bar, drag a widget onto the Px Editor canvas.

            The Make Widget wizard opens, with the ord for the selected component displaying in the ord
            area.

March 15, 2023                                                                                           23
Chapter 3 Adding widgets using the Make Widget wizard  Niagara Graphics Guide

Step 2  To change the ord value, double-click it.
Step 3  From the Source options list, select the type of binding (choose from: Bound Label, Properties,
        Actions, and others).
Step 4  NOTE: Binding options that are dimmed indicate invalid options for the selected component.
Step 5  The Secondary view area displays properties and options that are related to the selected Source
Step 6  option.
Step 7  In the Secondary view area, choose a widget template, formatting options for display labels, or
        views, as appropriate for your Source option.
        In the right-hand list of properties, edit the properties or actions, as desired.
        To complete the configuration, click OK.
        The wizard closes and your new widget displays on the Px Editor canvas.

        To toggle the view, click the view/edit mode tool bar icon ( ).
        This icon alternates between displaying the widget in the Px Viewer and Px Editor.

```

## docGraphics.txt:1087-1111 — Popup Binding hyperlink example
```
Hyperlinks with Popup Bindings (an example)

You add a Popup Binding to an object that serves as a hyperlink. After adding the Popup Binding, you con-
figure Px and Popup Window properties. This topic describes an added hyperlink using a Popup Binding on
a Px Button widget.

Figure 12 Example of the options to configure a popup hyperlink

In the Px Editor view, a Popup Binding is added to a Px button widget (Popup Weather). This is the default
view of the component.

The Popup Binding properties (displayed in the bottom left corner) specify the following:

Property         Description

ord              Binds the component to: slot:/Services/WeatherService/Edinburgh.

degradebeharior  No degrade behavior is assigned.
title            The title Pop up appears in the top left corner of the Popup Window.

icon             The workbench.png icon is assigned to the top left corner of the Popup Window.

position         Window X and Y coordinate screen position is x=100 and y=100 pixels.

size             The Popup Window size is 800 pixels wide and 600 pixels high.
```

## docGraphics.txt:1548-1587 — About data binding + Add a binding
```
About data binding

Bindings are established between a widget and an object. Binding provides real-time information for
presentation.

All widgets may be bound to data sources using data binding. An ord links a bindings to a widget. A single
binding consists of a single widget�object relationship. A binding's ord property identifies the location of
the object that updates and animates the widget.

For example, the most common type of binding, the value binding, provides some of the typical functions
that are associated with building real-time information for presentation as both text and graphics. This in-
cludes support for mouse-over status and right-click actions. Additionally it provides a mechanism to ani-
mate any property of its parent widget using converters that convert the target object into property values.
The following figure shows a value binding.

The following figure illustrates the object-to-widget property binding concept. In this example, a widget has
three separate data bindings. This means that each binding is coming from a different object and therefore
each binding has a different ord that defines its binding. Each binding provides access to an object's values
so that they may be used, as required, to animate the widget properties.

March 15, 2023  51
Chapter 4 Animating graphics (data binding)  Niagara Graphics Guide
Figure 16 Widget with three bindings

Add a data binding to a widget

Add a binding to a widget that is already on the Px Editor canvas by editing properties.
There are different ways to add a binding to a widget. You can add a binding to a widget using the Make
Widget wizard or you can edit the widget properties as described here.
Step 1 In the PxEditor canvas, select the desired widget.

Step 2 In the Properties side bar, click the Add Binding button ( ).
Step 3 In the Add Binding window, select a binding type from the options list and click the OK button.

            The binding type properties are added to the binding area at the bottom of the widget property
            sheet.
Step 4 Under the binding type properties, click the ord property. The ord window opens.
Step 5 In the ord window, type or browse to the value to bind to the widget.
Step 6 Click the OK button.
The ord is added to the binding area of the widget property sheet.
```

## docGraphics.txt:1741-1799 — Types of bindings + properties (degradeBehavior)
```
Types of data bindings

There are different types of bindings that may be used with widgets.
Some bindings work with only a certain type of widget (for example, a bound label binding) and other bind-
ing types may be used with several types of widgets (for example, a value binding). Shown here are bindings
as they appear in an options list.
The following list describes each binding type.
� Action binding

    Invokes an action on the binding target component when an event is fired by the parent widget.
� Bound Label binding

    Connects a value to a bound label widget.
� Field Editor binding

    Used to bind field editor components to an object.
� Popup binding

    Used to display a Px view in an additional popup window that you can specify and configure.
� Setpoint binding

    Used to display the current value of a setpoint and also to provide the ability to modify it.
� Spectrum binding

    Used to animate a widget's brush (color) property.

March 15, 2023          55
Chapter 4 Animating graphics (data binding)  Niagara Graphics Guide

� Spectrum setpoint binding

    Used in conjunction with a spectrum binding.

� Table binding

    Uused to bind table data in a bound table.

� Value binding

    Used to bind to values that are typically under a component.

Types of binding properties

One or more of the following properties may be included with various binding types.

� actionArgument

    This property works with certain widgetEvents to specify the action to take when the widgetEvent is trig-
    gered. For example, a mouseEvent, such as a click on a widget can change a Boolean status from true to
    false or prompt the user to select a setting. Argument options, like the widgetEvents, themselves, are
    context sensitive, depending on widget and binding type.

� degradeBehavior

    This property specifies how the object behaves when binding communications are not available. If a bind-
    ing is not usable, this property allows the designer to choose how to degrade the UI gracefully. For exam-
    ple, if the user does not have permission to invoke a specific action, a button that is bound to that action
    can be dimmed or hidden entirely. To preserve backward compatibility, the default degradeBehavior is
    none.
```

## docGraphics.txt:4252-4334 — Relative vs absolute ORD portability
```
About developing for portability

Develop Px Views that you can easily use in multiple scenarios by specifying relative ORDs in bindings and
hyperlinks.

For purposes of this discussion, the term portability means reusability. In other words, being able to use the
same Px View in different stations without having to edit the ORD properties for bindings and hyperlinks.
Another example of portability, is being able to use the same Px view in a controller station and a Supervisor
station without having to edit ORD properties. You can develop portable Px views by avoiding the use of ab-
solute ORDs in bindings and hyperlinks. The reason to avoid using absolute ORD properties in Px views is
that they are not portable between stations.

Bound label using absolute ORD

The floor plan graphic shown below has a bound label configured with a hyperlink to a detailed graphic for a
specific device. You can see that both the ord binding and hyperlink properties use an absolute ord, meaning
the value specifies the entire station slot path.

Figure 57 Floor plan graphic using components configured with absolute ORDs

If you wanted to use the same floor plan graphic in the Supervisor station it you would have to create Niagar-
aNetwork proxy points and assign the same Px view (or you could use Px view export tags). In either case,
the absolute slot path used in the Supervisor must be different than that used in the JACE.

March 15, 2023          123
Chapter 9 Px graphics reference  Niagara Graphics Guide

� Supervisor absolute ORD
    station:|slot:/Drivers/NiagaraNetwork/VA/Richmond/myController/points/First-
    Floor/AHU1

� JACE absolute ORD
    station:|slot:/Drivers/LonNetwork/FirstFloor/points/FirstFloor/SpaceTemp

Bound label using relative ORD
Here, you have the same scenario but in this case using relative ORDs instead of absolute ORDs.

Figure 58 Floor plan graphic using components configured with relative ORDs

The same relative ORDs can be used in both the JACE and the Supervisor station since it is being applied
against different base ORDs

Hyperlink using absolute ORD

The following example, the user has hyperlinked from the floor plan graphic to a more detailed graphic of
the air handling unit (AHU). This detailed AHU graphic includes a button that is a hyperlink back to the floor
plan graphic (FirstFloor). The button hyperlink is configured with an absolute ORD.

124                              March 15, 2023
Niagara Graphics Guide                                                                     Chapter 9 Px graphics reference

Figure 59 Graphic that includes a button with a hyperlink configured with an absolute ORD

In order to use the above graphic in the Supervisor station, you must use NiagaraNetwork proxy points and
assign a Px view or by using a Px view export tag.
The absolute ORD to the First Floor graphic in the JACE is different from that in the Supervisor.
� Absolute ORD in JACE

    station:|slot:/Drivers/LonNetwork/FirstFloor

� Absolute ORD in Supervisor

    station:|slot:/Drivers/NiagaraNetwork/VA/Richmond/myController/points/
    FirstFloor

Hyperlink using relative ORD
Using relative ORD properties, it is possible to navigate back up the tree by entering two periods in the
path, similar to how directories can be navigated in a DOS command window.
NOTE: You can back up multiple levels using this syntax: "..".

March 15, 2023                                                                             125
Chapter 9 Px graphics reference                                                      Niagara Graphics Guide
Figure 60 Graphic includes a button with a hyperlink configured with a relative ORD

� Relative ORD in Px page
    slot:..

� Base ORD in JACE
    station:|slot:/Drivers/LonNetwork/FirstFloor/AHU1

� Base ORD in Supervisor
    station:|slot:/Drivers/NiagaraNetwork/VA/Richmond/myController/points/First-
    Floor/AHU1
```

## docGraphics.txt:1748-1780 — Types of data bindings (enumerated)
```
� Action binding

    Invokes an action on the binding target component when an event is fired by the parent widget.
� Bound Label binding

    Connects a value to a bound label widget.
� Field Editor binding

    Used to bind field editor components to an object.
� Popup binding

    Used to display a Px view in an additional popup window that you can specify and configure.
� Setpoint binding

    Used to display the current value of a setpoint and also to provide the ability to modify it.
� Spectrum binding

    Used to animate a widget's brush (color) property.

March 15, 2023          55
Chapter 4 Animating graphics (data binding)  Niagara Graphics Guide

� Spectrum setpoint binding

    Used in conjunction with a spectrum binding.

� Table binding

    Uused to bind table data in a bound table.

� Value binding

    Used to bind to values that are typically under a component.
```

## docGraphics.txt:1813-1816 — hyperlink binding property
```
� hyperlink

    This property provides a link to another object. When used, the hyperlink is active in the browser or in
    the Px viewer.
```

## docGraphics.txt:5237-5240 — HyperlinkLabel component
```
The HyperlinkLabel, in the bajaui palette, has the same properties as the Label (see bajaui-Label), plus an
ORD property that allows you to assign an ORD to the label. When an ORD path is supplied for this property,
the HyperlinkLabel causes a mouse cursor to change to a standard link cursor and the component performs
a hyperlink when clicked.
```

