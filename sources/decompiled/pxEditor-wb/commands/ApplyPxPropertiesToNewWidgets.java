package com.tridium.px.editor.commands;

import com.tridium.px.editor.BPxEditorPane;
import com.tridium.ui.px.PxPropertyComponent;
import com.tridium.ui.px.PxPropertyComponentArray;
import com.tridium.ui.px.Target;
import com.tridium.ui.px.TargetArray;
import com.tridium.util.CompUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.baja.px.editor.BPxEditor;
import javax.baja.px.editor.event.PxPropertyEvent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.ui.BWidget;
import javax.baja.ui.Command;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.px.PxProperty;

public final class ApplyPxPropertiesToNewWidgets extends Command {
   public static final String PX_PROPERTY_INFO_NAME = "propertiesToPxProperties";
   private List<BWidget> widgets;
   private List<BWidget> newWidgets;

   public ApplyPxPropertiesToNewWidgets(BPxEditor editor, List<BWidget> widgets, List<BWidget> newWidgets) {
      super(editor, "commands.applyPxProperties");
      if (widgets.size() != newWidgets.size()) {
         throw new IllegalArgumentException();
      } else {
         this.widgets = widgets;
         this.newWidgets = newWidgets;
      }
   }

   public static void storePxPropertyInfoOnNewWidget(BPxEditor editor, BWidget widget, BWidget newWidget) {
      Map<String, String> propertyNamesToPxPropertyNames = findPxPropertiesForWidget(editor, widget);
      if (!propertyNamesToPxPropertyNames.isEmpty()) {
         String pxPropertyInfo = encodePxPropertyInfo(propertyNamesToPxPropertyNames);
         CompUtil.setOrAdd(newWidget, "propertiesToPxProperties", BString.make(pxPropertyInfo), 8199, BFacets.DEFAULT, null);
      }

      for (BWidget child : widget.getChildWidgets()) {
         BValue value = newWidget.get(child.getName());
         if (value instanceof BWidget) {
            BWidget newChild = (BWidget)value;
            storePxPropertyInfoOnNewWidget(editor, child, newChild);
         }
      }
   }

   private static Map<String, String> findPxPropertiesForWidget(BPxEditor editor, BWidget widget) {
      BPxEditorPane editorPane = (BPxEditorPane)editor.getContent();
      Map<String, String> propertyNamesToPxPropertyNames = new HashMap<>();

      for (PxPropertyComponent pxPropertyComponent : editorPane.getPxPropertyComponents().forWidgets(new BWidget[]{widget})) {
         TargetArray targetArray = pxPropertyComponent.getTargets();

         for (int i = 0; i < targetArray.size(); i++) {
            Target target = targetArray.get(i);
            propertyNamesToPxPropertyNames.put(target.getPropertyName(), target.getPxProperty().getName());
         }
      }

      return propertyNamesToPxPropertyNames;
   }

   private static String encodePxPropertyInfo(Map<String, String> propertyNamesToPxPropertyNames) {
      return propertyNamesToPxPropertyNames.keySet()
         .stream()
         .map(propName -> propName + "=" + propertyNamesToPxPropertyNames.get(propName))
         .collect(Collectors.joining(";"));
   }

   private static Map<String, String> decodePxPropertyInfo(String encodedPxPropertyInfo) {
      String[] entries = encodedPxPropertyInfo.split(";");
      Map<String, String> propertyNamesToPxPropertyNames = new HashMap<>(entries.length * 2);

      for (String entry : entries) {
         String[] names = entry.split("=");
         propertyNamesToPxPropertyNames.put(names[0], names[1]);
      }

      return propertyNamesToPxPropertyNames;
   }

   public CommandArtifact doInvoke() {
      ApplyPxPropertiesToNewWidgets.Artifact artifact = new ApplyPxPropertiesToNewWidgets.Artifact();
      artifact.redo();
      return artifact;
   }

   private static Map<String, PxProperty> makeNameToPxPropertyMap(PxProperty[] pxProperties) {
      Map<String, PxProperty> namesToPxProperties = new HashMap<>(pxProperties.length);

      for (PxProperty pxProperty : pxProperties) {
         namesToPxProperties.put(pxProperty.getName(), pxProperty);
      }

      return namesToPxProperties;
   }

   private static void applyPxPropertiesToWidgets(BPxEditor editor, Collection<PxProperty> pxProperties) {
      BWidget root = editor.getWidget();
      pxProperties.forEach(pxProperty -> pxProperty.apply(root));
   }

   private static void fireChangedPxPropertyEventsInPxEditor(BPxEditor editor, Collection<PxProperty> pxProperties) {
      pxProperties.forEach(pxProperty -> editor.firePxEvent(new PxPropertyEvent(2, pxProperty)));
   }

   private static void updatePxPropertiesInPxEditor(BPxEditor editor, PxPropertyComponentArray pxPropertyComponentArray) {
      pxPropertyComponentArray.updateTargets(editor.getWidget(), editor.getPxProperties());
   }

   private BPxEditor getPxEditor() {
      return (BPxEditor)this.getOwner();
   }

   private BPxEditorPane getPxEditorPane() {
      return (BPxEditorPane)this.getPxEditor().getContent();
   }

   final class Artifact implements CommandArtifact {
      public void redo() {
         BPxEditor editor = ApplyPxPropertiesToNewWidgets.this.getPxEditor();
         Collection<PxProperty> changedPxProperties = new ApplyPxPropertiesToNewWidgets.PxPropertyApplicator(
               editor, ApplyPxPropertiesToNewWidgets.this.widgets, ApplyPxPropertiesToNewWidgets.this.newWidgets
            )
            .applyPxPropertiesToNewWidgets()
            .getChangedPxProperties();
         if (!changedPxProperties.isEmpty()) {
            ApplyPxPropertiesToNewWidgets.updatePxPropertiesInPxEditor(editor, ApplyPxPropertiesToNewWidgets.this.getPxEditorPane().getPxPropertyComponents());
            ApplyPxPropertiesToNewWidgets.applyPxPropertiesToWidgets(editor, changedPxProperties);
            ApplyPxPropertiesToNewWidgets.fireChangedPxPropertyEventsInPxEditor(editor, changedPxProperties);
         }
      }

      public void undo() {
      }
   }

   private static final class PxPropertyApplicator {
      private PxPropertyComponentArray pxPropertyComponents;
      private Map<String, PxProperty> namesToPxProperties;
      private Collection<PxProperty> changedPxProperties = Collections.emptySet();
      private List<BWidget> widgets;
      private List<BWidget> newWidgets;

      private PxPropertyApplicator(BPxEditor editor, List<BWidget> widgets, List<BWidget> newWidgets) {
         this.widgets = widgets;
         this.newWidgets = newWidgets;
         this.namesToPxProperties = ApplyPxPropertiesToNewWidgets.makeNameToPxPropertyMap(editor.getPxProperties());
         BPxEditorPane editorPane = (BPxEditorPane)editor.getContent();
         this.pxPropertyComponents = editorPane.getPxPropertyComponents();
      }

      public ApplyPxPropertiesToNewWidgets.PxPropertyApplicator applyPxPropertiesToNewWidgets() {
         this.changedPxProperties = new HashSet<>();

         for (int i = 0; i < this.widgets.size(); i++) {
            this.apply(this.widgets.get(i), this.newWidgets.get(i));
         }

         return this;
      }

      private void apply(BWidget widget, BWidget newWidget) {
         BValue pxPropertyInfoValue = widget.get("propertiesToPxProperties");
         if (pxPropertyInfoValue instanceof BString) {
            PxPropertyComponent newPxPropComp = new PxPropertyComponent(newWidget);
            ApplyPxPropertiesToNewWidgets.decodePxPropertyInfo(pxPropertyInfoValue.toString())
               .forEach((propertyName, pxPropertyName) -> this.addPxPropertyToPxPropertyComponent(newPxPropComp, propertyName, pxPropertyName));
            if (newPxPropComp.getTargets().size() > 0) {
               this.pxPropertyComponents.add(newPxPropComp);
            }
         }

         for (BWidget child : widget.getChildWidgets()) {
            BValue value = newWidget.get(child.getName());
            if (value instanceof BWidget) {
               BWidget newChild = (BWidget)value;
               this.apply(child, newChild);
            }
         }
      }

      private void addPxPropertyToPxPropertyComponent(PxPropertyComponent pxPropertyComponent, String propertyName, String pxPropertyName) {
         PxProperty pxProperty = this.namesToPxProperties.get(pxPropertyName);
         if (pxProperty != null) {
            pxPropertyComponent.addTarget(pxProperty, propertyName);
            this.changedPxProperties.add(pxProperty);
         }
      }

      public Collection<PxProperty> getChangedPxProperties() {
         return this.changedPxProperties;
      }
   }
}
