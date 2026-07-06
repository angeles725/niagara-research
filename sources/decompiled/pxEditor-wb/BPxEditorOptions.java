package com.tridium.px.editor;

import com.tridium.tagdictionary.neqlize.BNeqlizeMode;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.baja.gx.BColor;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.ui.options.BUserOptions;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "showGrid",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "gridSize",
      type = "int",
      defaultValue = "10",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "0"
      )}
   ), @NiagaraProperty(
      name = "gridColor",
      type = "BColor",
      defaultValue = "BColor.make(64, 64, 64, 64)"
   ), @NiagaraProperty(
      name = "useSnap",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "snapSize",
      type = "int",
      defaultValue = "10",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "0"
      )}
   ), @NiagaraProperty(
      name = "showHatch",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "hatchColor",
      type = "BColor",
      defaultValue = "BColor.make(64, 64, 64, 64)"
   ), @NiagaraProperty(
      name = "preserveIdentities",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "animateBindings",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "neqlizeMode",
      type = "BNeqlizeMode",
      defaultValue = "BNeqlizeMode.traverseIfPossible"
   ), @NiagaraProperty(
      name = "neqlizeExcludedRelations",
      type = "String",
      defaultValue = "",
      facets = {@Facet(
         name = "BFacets.MULTI_LINE",
         value = "BBoolean.TRUE"
      )}
   ), @NiagaraProperty(
      name = "useServiceExcludedRelations",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet(
         name = "BFacets.TRUE_TEXT",
         value = "BString.make(\"%lexicon(pxEditor:PxEditorOptions.useServiceExcludedRelations.true)%\")"
      ), @Facet(
         name = "BFacets.FALSE_TEXT",
         value = "BString.make(\"%lexicon(pxEditor:PxEditorOptions.useServiceExcludedRelations.false)%\")"
      )}
   ), @NiagaraProperty(
      name = "neqlizeExcludedTags",
      type = "String",
      defaultValue = "",
      facets = {@Facet(
         name = "BFacets.MULTI_LINE",
         value = "BBoolean.TRUE"
      )}
   ), @NiagaraProperty(
      name = "useServiceExcludedTags",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet(
         name = "BFacets.TRUE_TEXT",
         value = "BString.make(\"%lexicon(pxEditor:PxEditorOptions.useServiceExcludedTags.true)%\")"
      ), @Facet(
         name = "BFacets.FALSE_TEXT",
         value = "BString.make(\"%lexicon(pxEditor:PxEditorOptions.useServiceExcludedTags.false)%\")"
      )}
   ), @NiagaraProperty(
      name = "pxMediaTypeName",
      type = "String",
      defaultValue = "BPxEditorOptions.getDefaultMediaType()",
      flags = 4
   )})
@NiagaraTopic(
   name = "propertyChanged",
   eventType = "BString"
)
public class BPxEditorOptions extends BUserOptions {
   public static final Property showGrid = newProperty(0, true, null);
   public static final Property gridSize = newProperty(0, 10, BFacets.make("min", 0));
   public static final Property gridColor = newProperty(0, BColor.make(64, 64, 64, 64), null);
   public static final Property useSnap = newProperty(0, true, null);
   public static final Property snapSize = newProperty(0, 10, BFacets.make("min", 0));
   public static final Property showHatch = newProperty(0, true, null);
   public static final Property hatchColor = newProperty(0, BColor.make(64, 64, 64, 64), null);
   public static final Property preserveIdentities = newProperty(0, false, null);
   public static final Property animateBindings = newProperty(0, true, null);
   public static final Property neqlizeMode = newProperty(0, BNeqlizeMode.traverseIfPossible, null);
   public static final Property neqlizeExcludedRelations = newProperty(0, "", BFacets.make("multiLine", BBoolean.TRUE));
   public static final Property useServiceExcludedRelations = newProperty(
      0,
      true,
      BFacets.make(
         BFacets.make("trueText", BString.make("%lexicon(pxEditor:PxEditorOptions.useServiceExcludedRelations.true)%")),
         BFacets.make("falseText", BString.make("%lexicon(pxEditor:PxEditorOptions.useServiceExcludedRelations.false)%"))
      )
   );
   public static final Property neqlizeExcludedTags = newProperty(0, "", BFacets.make("multiLine", BBoolean.TRUE));
   public static final Property useServiceExcludedTags = newProperty(
      0,
      true,
      BFacets.make(
         BFacets.make("trueText", BString.make("%lexicon(pxEditor:PxEditorOptions.useServiceExcludedTags.true)%")),
         BFacets.make("falseText", BString.make("%lexicon(pxEditor:PxEditorOptions.useServiceExcludedTags.false)%"))
      )
   );
   public static final Property pxMediaTypeName = newProperty(4, getDefaultMediaType(), null);
   public static final Topic propertyChanged = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BPxEditorOptions.class);
   private static final String DEFAULT_PXMEDIA_TYPE = AccessController.doPrivileged(
      (PrivilegedAction<String>)(() -> System.getProperty("niagara.default.pxMediaType"))
   );

   public boolean getShowGrid() {
      return this.getBoolean(showGrid);
   }

   public void setShowGrid(boolean v) {
      this.setBoolean(showGrid, v, null);
   }

   public int getGridSize() {
      return this.getInt(gridSize);
   }

   public void setGridSize(int v) {
      this.setInt(gridSize, v, null);
   }

   public BColor getGridColor() {
      return (BColor)this.get(gridColor);
   }

   public void setGridColor(BColor v) {
      this.set(gridColor, v, null);
   }

   public boolean getUseSnap() {
      return this.getBoolean(useSnap);
   }

   public void setUseSnap(boolean v) {
      this.setBoolean(useSnap, v, null);
   }

   public int getSnapSize() {
      return this.getInt(snapSize);
   }

   public void setSnapSize(int v) {
      this.setInt(snapSize, v, null);
   }

   public boolean getShowHatch() {
      return this.getBoolean(showHatch);
   }

   public void setShowHatch(boolean v) {
      this.setBoolean(showHatch, v, null);
   }

   public BColor getHatchColor() {
      return (BColor)this.get(hatchColor);
   }

   public void setHatchColor(BColor v) {
      this.set(hatchColor, v, null);
   }

   public boolean getPreserveIdentities() {
      return this.getBoolean(preserveIdentities);
   }

   public void setPreserveIdentities(boolean v) {
      this.setBoolean(preserveIdentities, v, null);
   }

   public boolean getAnimateBindings() {
      return this.getBoolean(animateBindings);
   }

   public void setAnimateBindings(boolean v) {
      this.setBoolean(animateBindings, v, null);
   }

   public BNeqlizeMode getNeqlizeMode() {
      return (BNeqlizeMode)this.get(neqlizeMode);
   }

   public void setNeqlizeMode(BNeqlizeMode v) {
      this.set(neqlizeMode, v, null);
   }

   public String getNeqlizeExcludedRelations() {
      return this.getString(neqlizeExcludedRelations);
   }

   public void setNeqlizeExcludedRelations(String v) {
      this.setString(neqlizeExcludedRelations, v, null);
   }

   public boolean getUseServiceExcludedRelations() {
      return this.getBoolean(useServiceExcludedRelations);
   }

   public void setUseServiceExcludedRelations(boolean v) {
      this.setBoolean(useServiceExcludedRelations, v, null);
   }

   public String getNeqlizeExcludedTags() {
      return this.getString(neqlizeExcludedTags);
   }

   public void setNeqlizeExcludedTags(String v) {
      this.setString(neqlizeExcludedTags, v, null);
   }

   public boolean getUseServiceExcludedTags() {
      return this.getBoolean(useServiceExcludedTags);
   }

   public void setUseServiceExcludedTags(boolean v) {
      this.setBoolean(useServiceExcludedTags, v, null);
   }

   public String getPxMediaTypeName() {
      return this.getString(pxMediaTypeName);
   }

   public void setPxMediaTypeName(String v) {
      this.setString(pxMediaTypeName, v, null);
   }

   public void firePropertyChanged(BString event) {
      this.fire(propertyChanged, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BPxEditorOptions make() {
      return (BPxEditorOptions)load(TYPE);
   }

   public void changed(Property prop, Context cx) {
      super.changed(prop, cx);
      this.firePropertyChanged(BString.make(prop.getName()));
   }

   private static String getDefaultMediaType() {
      return DEFAULT_PXMEDIA_TYPE == null ? "WbPxMedia" : DEFAULT_PXMEDIA_TYPE;
   }
}
