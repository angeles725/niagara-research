# Niagara N4 — Relations Guide (documentación oficial Tridium)

> Extraído de `organized/docRelations/**` (docRelations-doc, N4.14). Texto plano de los 17 HTML del
> guide oficial. Cada sección conserva el nombre de archivo original para poder citar `[CERT-doc]`.


## About this guide

<!-- source: AboutThisGuide_Relations.html -->

About this guide Index | Prev | Next About this guide This topic contains important information about the purpose, content, context, and intended audience for this document. Product Documentation This document is part of the Niagara technical documentation library. Released versions of Niagara software include a complete collection of technical information that is provided in both online help and PDF format. The information in this document is written primarily for Systems Integrators. In order to make the most of the information in this book, readers should have some training or previous experience with Niagara 4 or NiagaraAX software, as well as experience working with JACE network controllers. Document Content This document provides an introduction to concepts and procedural information about the relations feature in Niagara 4 . Related Links Document change log Related documentation Index | Prev | Next


## Using Relations

<!-- source: CommonRelationsTasks-087C9A61.html -->

Using Relations Index | Prev | Next Using Relations Relations provide metadata used primarily in building hierarchies for logical views of your system based on relationships between components. So that you can organize the display of those components in various meaningful ways. Relations Concepts Regardless of the actual structure of a system, you can define a hierarchy that includes “relation level definitions” which query for certain tags and relationships. Provided components are already tagged and relations already setup, executing the hierarchy definition results in a specific navigation tree hierarchy. For example, the resulting hierarchy groups all the variable air volume (VAV) controllers by the air handler units (AHUs) supplying them. In summary, you add relations between components for purposes of building hierarchies. Optionally, adding one or more tags to a relation provides additional metadata which allows for more specific filtering when building hierarchies. Common Relations Tasks Common relations tasks include adding relations between one or more components. Also, you can edit existing relations. Typical relations tasks are described in the following sections: Related Links Adding a Relation using Relation Mark Adding a Relation using drag and drop Editing a Relation from the Relation Sheet Editing a Relation from the Wire Sheet Index | Prev | Next


## Adding a Relation using drag and drop

<!-- source: CreatingRelationsUsingDragAndDrop-088182B4.html -->

Adding a Relation using drag and drop Index | Prev | Next Adding a Relation using drag and drop You can add a relation between two components within a Wire Sheet simply by dragging a connector from lower end of one component to the lower end of another. Perform the following steps: In a Wire Sheet view, click the Workbench Wire Sheet menu and click Show Relations . On the desired “Relate To” component, click the component’s footer and drag to the component footer for the desired “Relate From” component. A Relation dialog appears. From the pull-down list, click on the desired Relation Id and click OK. NOTE: This pull-down list is populated from the Relation Definitions defined in the selected Tag Dictionary. In the Wire Sheet view, the relation is shown as a dashed line between the two components. Both of the components have the Relation Id for the slot displayed name. Related Links Using Relations (Parent Topic) Index | Prev | Next


## Adding a Relation using Relation Mark

<!-- source: CreatingRelationsUsingRelationMark-08811A8B.html -->

Adding a Relation using Relation Mark Index | Prev | Next Adding a Relation using Relation Mark You can add a relation between components using the Relation Mark menu option and then selecting either Relate From or Relate To from a popup menu. Perform the following steps: Select one or more components to mark for either the “From” relation or the “To” relation component(s). Right-click on the selection and click Relation Mark . Select one or more components to use for the other side (From/To) of the relationship. Perform one of the following, depending on your choice in step 2: If the mark was for the “Relate From” components, right click on the selection and click Relate To . If the mark was for the “Relate To” components, right-click on the selection and click Relate From . A Relation dialog appears. Select the desired Relation Id form the pull-down list. NOTE: This list is populated from RelationInfoLists defined in the Tag Dictionaries installed in the TagDictionaryService . Opening the Relation Sheet view of the components included in the relationship shows this added relation. Related Links Using Relations (Parent Topic) Index | Prev | Next


## Document change log

<!-- source: DocumentChangeLog-1640EE77.html -->

Document change log Index | Prev | Next Document change log Updates (changes and additions) to this document are listed below. April 19, 2016: Document updated to remove several procedures that are not needed. July 25, 2015: Initial release document Related Links About this guide (Parent Topic) Index | Prev | Next


## Editing a Relation from the Wire Sheet

<!-- source: EditingARelationFromTheWireSheet-0C85BDFC.html -->

Editing a Relation from the Wire Sheet Index | Prev | Next Editing a Relation from the Wire Sheet You can modify an existing relation by invoking the Edit dialog from the component Wire Sheet view. Prerequisites: Component with existing relations Perform the following steps: In the Wire Sheet view, right-click on a dashed relation line and click Edit . In the Edit dialog, make any of the following changes: Change either the tag dictionary or tag name referenced in the Relation Id Change the Source Ord NOTE: Although Relation Tags are persisted as BFacets type and can be edited in this dialog, the recommended method for editing relation tags is via the Relation Tags dialog (in the Relation Sheet view, right-click the relation row and click Tags ). Click OK to save your changes. Related Links Using Relations (Parent Topic) Index | Prev | Next


## Editing a Relation from the Relation Sheet

<!-- source: EditingAnExistingRelationFromTheRel-0C85AA2D.html -->

Editing a Relation from the Relation Sheet Index | Prev | Next Editing a Relation from the Relation Sheet You can modify an existing relation by invoking the Edit dialog from the component Relation Sheet view. Prerequisites: Component with existing relations Perform the following steps: Select the desired component and open the Relation Sheet view. Right-click on the desired relation row and click Edit . In the Edit dialog, make any of the following changes: Change either the tag dictionary or tag name referenced in the Relation Id Change the Source Ord NOTE: Although Relation Tags are persisted as BFacets type and can be edited in this dialog, the recommended method for editing relation tags is via the Relation Tags dialog (in the Relation Sheet view, right-click on the relation row and click Tags ). Click OK to save your changes. Related Links Using Relations (Parent Topic) Index | Prev | Next


## Entity-Relationship Modeling

<!-- source: Entity-RelationshipModeling-14BC57B0.html -->

Entity-Relationship Modeling Index | Prev | Next Entity-Relationship Modeling An entity is any identifiable object (point, device, etc.) that exists independently. A relationship captures how one entity relates to another. For example, in your system you might have an AHU device that supplies air to a specific VAV device. Using an English grammar analogy, entities can be thought of as nouns , while relationships can be thought of as verbs connecting two or more nouns (entities). For example: where AHU1 supplies VAV1, both AHU1 and VAV1 are nouns (entities) and “supplies” is the verb (relationship). Taking it further, AHU1 (the subject) supplies (the verb/predicate) VAV1 (the object). You can also determine the relationship direction by examining the structure of the relationship. From the context of the first entity (the subject), the relation is an “outbound” relation. While from the context of the second entity (the object) the relation is an “inbound” relation. In the Component space, a BComponent is an entity, and BRelation is used to declare a relationship from one BComponent to another BComponent. The from component is the subject of the relation and the to component is the object of the relation. A relation is created by adding a dynamic BRelation slot on the object of the relationship. A “BLink” is a specialized type of relation that defines a “data-flow” relationship between a value slot of one component to a value slot of one or more other components. Related Links Relations Reference (Parent Topic) Index | Prev | Next


## Related documentation

<!-- source: RelatedDocumentation-49C4C13A.html -->

Related documentation Index | Prev | Next Related documentation The following documents may relate to the content in this guide and provide additional information. Tagging Guide Templates Guide Hierarchies Guide Related Links About this guide (Parent Topic) Index | Prev | Next


## Relation Definitions

<!-- source: RelationDefinitionsInTagDictionarie-14B9B7F2.html -->

Relation Definitions Index | Prev | Next Relation Definitions Tag Dictionaries often contain a collection of Relation Definitions (shown in the following image) which are standardized Relation Id's with semantic meaning for a given domain or namespace. These relation definitions come into play when adding a relation to a component. In the Relation dialog, your choices are limited to the relations that are defined in the any of the Tag Dictionaries on your system. Figure 2. Relation Definitions in custom Tag Dictionary (left) provide choices seen in the Relation dialog (right) Related Links Relations Reference (Parent Topic) Index | Prev | Next


## Relation Id Structure

<!-- source: RelationIDStructure-13BF3BA7.html -->

Relation Id Structure Index | Prev | Next Relation Id Structure A Relation Id contains different parts that, together, make the ID useful as additional information on objects in a station. The following diagram shows the basic parts of a Relation Id. Figure 1. Parts of a Relation Id Relation Id Tag Dictionary Tag Name Tag Value The following table provides definitions of the different parts of a Relation Id: Relation Element Description Relation Id The Relation Id is comprised of a dictionary and name, generally displayed as two pieces of text separated by a colon (:), as shown in the following example: < dictionaryNamespace >:< name > . Tag Dictionary The dictionary string is used to link or assign a tag to a particular "namespace" (tag dictionary). This is typically a very short string of one or two characters. Tag Name The name string provides the semantic information and is often paired with the Tag Value. Tag Value A string value assigned to the tag for more information, for example: building name, device name, location, or other. Related Links Relations Reference (Parent Topic) Index | Prev | Next


## Relations Reference

<!-- source: RelationsReference-09F8C260.html -->

Relations Reference Index | Prev | Next Relations Reference Related Links Entity-Relationship Modeling Types of Relations Relation Id Structure Relation Definitions Support for Relations Index | Prev | Next


## Types of Relations

<!-- source: TypesOfRelations-14BA1C23.html -->

Types of Relations Index | Prev | Next Types of Relations Relations can be either “direct” or “implied” and are defined as follows: Direct relations are relations that you apply directly to a component. When adding a relation, your choices are limited to relations that are defined in the any of the Tag Dictionaries on your system. Implied relations are determined automatically and applied to a component by the system. Implied relations are defined in a SmartTagDictionary under its Tag Rules folder (BTagRuleList). When an application queries for the relations on a component in the station, the SmartTagDictionary executes code that interprets the Tag Rules against the given component and returns a list of implied relations. Related Links Relations Reference (Parent Topic) Index | Prev | Next


## Wire Sheet view

<!-- source: WorkbenchSupportForRelations-08778BF1.html -->

Wire Sheet view Index | Prev | Next Wire Sheet view Workbench has a Wire Sheet Relation mode when selected component relations are displayed on the wire sheet. This option can be selected by any of the following methods: Workbench Tools > Options > Wire Sheet > Show Relations Figure 4. Workbench Tools option In a Wire Sheet view, select Wire Sheet > Show Relations Figure 5. Wire Sheet menu option On the Workbench Tool Bar, click the Show Relations icon Figure 6. Workbench Tool Bar icon When invoked, the Wire Sheet Relation mode displays relations of the selected component as either of the following: On-sheet relations This situation exists when both components of a relationship are present on the current wire sheet, the relation is shown as a dashed line connecting the two components. The Relation Id is in the name of the colored bar of the component glyph. The relation line exits the right side of the subject (outbound relation component) and enters the left side of the object (inbound relation component). Figure 7. On-sheet relation depicted with dashed line Off-sheet relations This situation exists when the other component of a relationship is NOT present on the current wire sheet. The relation is depicted with a relation “stub” recognizable by the hollow stub appearance (as shown in the following image), distinguishable from a “link” stub which has a solid appearance. Access the main view of the other component in a relationship by right-clicking the relation stub and selecting Goto Relation . Also, double-clicking the relation stub switches to the Wire Sheet view of the other component in the relationship. Figure 8. Off-sheet relation depicted with hollow stub Related Links Support for Relations (Parent Topic) Index | Prev | Next


## Support for Relations

<!-- source: WorkbenchSupportForRelations-0F17F682.html -->

Support for Relations Index | Prev | Next Support for Relations Workbench support for relations is evident in the Relation Sheet and Wire Sheet views. In either view you are able to create, edit or remove relations. Also, the Spy view includes relations information. The workflow for creating and editing Relations in Workbench is very similar to that of creating and editing links in previous versions of Workbench . A BRelation slot is added to the component that is the inbound side of a relation. The outbound Relation component will have a RelationKnob , which is similar to a Link-Knob. Related Links Relation Sheet view Wire Sheet view Relations Reference (Parent Topic) Index | Prev | Next


## Relations Guide

<!-- source: index.html -->

Relations Guide Index | Prev | Next Relations Guide Tridium, Inc. 3951 Westerre Parkway, Suite 350 Richmond, Virginia 23233 U.S.A. Confidentiality The information contained in this document is confidential information of Tridium, Inc., a Delaware corporation (“Tridium”). Such information and the software described herein, is furnished under a license agreement and may be used only in accordance with that agreement. The information contained in this document is provided solely for use by Tridium employees, licensees, and system owners; and, except as permitted under the below copyright notice, is not to be released to, or reproduced for, anyone else. While every effort has been made to assure the accuracy of this document, Tridium is not responsible for damages of any kind, including without limitation consequential damages, arising from the application of the information contained herein. Information and specifications published here are current as of the date of this publication and are subject to change without notice. The latest product specifications can be found by contacting our corporate headquarters, Richmond, Virginia. Trademark notice BACnet and ASHRAE are registered trademarks of American Society of Heating, Refrigerating and Air-Conditioning Engineers. Microsoft, Excel, Internet Explorer, Windows, Windows Vista, Windows Server, and SQL Server are registered trademarks of Microsoft Corporation. Oracle and Java are registered trademarks of Oracle and/or its affiliates. Mozilla and Firefox are trademarks of the Mozilla Foundation. Echelon, LON, LonMark, LonTalk, and LonWorks are registered trademarks of Echelon Corporation. Tridium, JACE, Niagara Framework, NiagaraAX Framework, and Sedona Framework are registered trademarks, and Workbench, WorkPlaceAX, and AXSupervisor, are trademarks of Tridium Inc. All other product names and services mentioned in this publication that are known to be trademarks, registered trademarks, or service marks are the property of their respective owners. Copyright and patent notice This document may be copied by parties who are authorized to distribute Tridium products in connection with distribution of those products, subject to the contracts that authorize such distribution. It may not otherwise, in whole or in part, be copied, photocopied, reproduced, translated, or reduced to any electronic medium or machine-readable form without prior written consent from Tridium, Inc. Copyright © 2017 Tridium, Inc. All rights reserved. The product(s) described herein may be covered by one or more U.S. or foreign patents of Tridium. Index | Prev | Next


## Relation Sheet view

<!-- source: workbench-RelationSheet.html -->

Relation Sheet view Index | Prev | Next Relation Sheet view The Relation Sheet is the main view for managing relations on a component. The view displays the relations of a selected component as well as any links, as shown here. Figure 3. Relation Sheet view shows relations and links NOTE: The Relation Sheet view replaces the Link Sheet view available in NiagaraAX . The Relation Sheet view displays the following information: Name Description Relation Id The relation ID for this relation. Slot The connecting slot of this component for a link. Applies to links only. Dir Indicates the direction of this relation. Options are either an inbound target or an outbound source. Type Indicates the class type of the relation. Other Path The slot path to the other related component. Other Slot The connecting slot of the other component for a link. Enabled (hidden) Indicates whether this link is currently enabled. A “relation” is always enabled. Tags (hidden) Tags can be applied to a relation. Any applied tags are shown here. Although hidden by default, the Enabled and Tags columns can be exposed in the view by clicking on the pull-down list located at the far right of the column heading row, and then clicking to select each of these column heads. Popup menu commands Right-clicking a row in the Relation Sheet view invokes a popup menu with the following options: Option Description Edit Edit the selected relation or link. Tags Edit the tags applied to the selected relation or link. Delete Delete the selected relation or link. Go To Go to the main view of the selected relation. Related Links Support for Relations (Parent Topic) Index | Prev | Next
