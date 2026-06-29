package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetActionList;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.COMMAND)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.COMMAND, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "presentValue",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PRESENT_VALUE, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT"
   ), @NiagaraProperty(
      name = "inProcess",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.IN_PROCESS, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "allWritesSuccessful",
      type = "boolean",
      defaultValue = "true",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ALL_WRITES_SUCCESSFUL, ASN_BOOLEAN)")}
   ), @NiagaraProperty(
      name = "action",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetActionList.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.ACTION, ASN_CONSTRUCTED_DATA)")}
   )})
public class BBacnetCommand extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(7), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(7, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property presentValue = newProperty(0, BBacnetUnsigned.DEFAULT, makeFacets(85, 2));
   public static final Property facets = newProperty(0, BFacets.DEFAULT, null);
   public static final Property inProcess = newProperty(0, false, makeFacets(47, 1));
   public static final Property allWritesSuccessful = newProperty(0, true, makeFacets(9, 1));
   public static final Property action = newProperty(0, new BBacnetArray(BBacnetActionList.TYPE), makeFacets(2, -1));
   public static final Type TYPE = Sys.loadType(BBacnetCommand.class);

   public BBacnetUnsigned getPresentValue() {
      return (BBacnetUnsigned)this.get(presentValue);
   }

   public void setPresentValue(BBacnetUnsigned v) {
      this.set(presentValue, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public boolean getInProcess() {
      return this.getBoolean(inProcess);
   }

   public void setInProcess(boolean v) {
      this.setBoolean(inProcess, v, null);
   }

   public boolean getAllWritesSuccessful() {
      return this.getBoolean(allWritesSuccessful);
   }

   public void setAllWritesSuccessful(boolean v) {
      this.setBoolean(allWritesSuccessful, v, null);
   }

   public BBacnetArray getAction() {
      return (BBacnetArray)this.get(action);
   }

   public void setAction(BBacnetArray v) {
      this.set(action, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
