package javax.baja.lonworks.datatypes;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "xmlFile",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      facets = {@Facet("BFacets.make(\"allowLocalAccess\", BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "xifFile",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 4,
      facets = {@Facet("BFacets.make(\"allowLocalAccess\", BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "resFile",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 4,
      facets = {@Facet("BFacets.make(\"allowLocalAccess\", BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "syncNvConfig",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   ), @NiagaraProperty(
      name = "useLonObjects",
      type = "boolean",
      defaultValue = "false"
   )})
public class BImportParameters extends BStruct {
   public static final Property xmlFile = newProperty(0, BOrd.NULL, BFacets.make("allowLocalAccess", BBoolean.TRUE));
   public static final Property xifFile = newProperty(4, BOrd.NULL, BFacets.make("allowLocalAccess", BBoolean.TRUE));
   public static final Property resFile = newProperty(4, BOrd.NULL, BFacets.make("allowLocalAccess", BBoolean.TRUE));
   public static final Property syncNvConfig = newProperty(4, false, null);
   public static final Property useLonObjects = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BImportParameters.class);

   public BOrd getXmlFile() {
      return (BOrd)this.get(xmlFile);
   }

   public void setXmlFile(BOrd v) {
      this.set(xmlFile, v, null);
   }

   public BOrd getXifFile() {
      return (BOrd)this.get(xifFile);
   }

   public void setXifFile(BOrd v) {
      this.set(xifFile, v, null);
   }

   public BOrd getResFile() {
      return (BOrd)this.get(resFile);
   }

   public void setResFile(BOrd v) {
      this.set(resFile, v, null);
   }

   public boolean getSyncNvConfig() {
      return this.getBoolean(syncNvConfig);
   }

   public void setSyncNvConfig(boolean v) {
      this.setBoolean(syncNvConfig, v, null);
   }

   public boolean getUseLonObjects() {
      return this.getBoolean(useLonObjects);
   }

   public void setUseLonObjects(boolean v) {
      this.setBoolean(useLonObjects, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BImportParameters() {
   }

   public BImportParameters(boolean sync) {
      this.setSyncNvConfig(sync);
   }

   public BImportParameters(boolean sync, boolean use) {
      this.setSyncNvConfig(sync);
      this.setUseLonObjects(use);
   }
}
