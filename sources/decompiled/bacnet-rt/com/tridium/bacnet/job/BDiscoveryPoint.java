package com.tridium.bacnet.job;

import java.io.IOException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "propertyIdentifier",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "index",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "value",
      type = "BValue",
      defaultValue = "BString.DEFAULT"
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.NULL"
   ), @NiagaraProperty(
      name = "pointType",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,3)")}
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   )})
public class BDiscoveryPoint extends BVector {
   public static final Property objectName = newProperty(0, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property propertyIdentifier = newProperty(0, "", null);
   public static final Property index = newProperty(0, "", null);
   public static final Property value = newProperty(0, BString.DEFAULT, null);
   public static final Property facets = newProperty(0, BFacets.NULL, null);
   public static final Property pointType = newProperty(0, 0, BFacets.makeInt(0, 3));
   public static final Property description = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BDiscoveryPoint.class);
   public static final int OBJECT = 0;
   public static final int PROPERTY = 1;
   public static final int ARRAY = 2;
   public static final int ELEMENT = 3;
   public static final BDiscoveryPoint NULL = new BDiscoveryPoint();

   public String getObjectName() {
      return this.getString(objectName);
   }

   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public String getPropertyIdentifier() {
      return this.getString(propertyIdentifier);
   }

   public void setPropertyIdentifier(String v) {
      this.setString(propertyIdentifier, v, null);
   }

   public String getIndex() {
      return this.getString(index);
   }

   public void setIndex(String v) {
      this.setString(index, v, null);
   }

   public BValue getValue() {
      return this.get(value);
   }

   public void setValue(BValue v) {
      this.set(value, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public int getPointType() {
      return this.getInt(pointType);
   }

   public void setPointType(int v) {
      this.setInt(pointType, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDiscoveryPoint() {
   }

   public BDiscoveryPoint(String objectName, BBacnetObjectIdentifier objectId, String propertyId, BValue value, BFacets facets, boolean isObject) {
      this.setObjectName(objectName != null ? objectName : objectId.toShortString());
      this.setObjectId(objectId);
      this.setPropertyIdentifier(propertyId);
      if (value != null) {
         this.setValue(value);
      }

      this.setFacets(facets != null ? facets : BFacets.NULL);
      this.setPointType(isObject ? 0 : 1);
   }

   public BDiscoveryPoint(String objectName, BBacnetObjectIdentifier objectId, String propertyId, BDiscoveryPoint[] kids, BFacets facets) {
      this.setObjectName(objectName != null ? objectName : objectId.toShortString());
      this.setObjectId(objectId);
      this.setPropertyIdentifier(propertyId);
      this.setFacets(facets != null ? facets : BFacets.NULL);
      if (kids != null) {
         for (int i = 0; i < kids.length; i++) {
            this.add(null, kids[i]);
         }
      }

      this.setPointType(2);
   }

   public BDiscoveryPoint(String objectName, BBacnetObjectIdentifier objectId, String propertyId, int index, BValue value, BFacets facets) {
      this.setObjectName(objectName != null ? objectName : objectId.toShortString());
      this.setObjectId(objectId);
      this.setPropertyIdentifier(propertyId != null ? propertyId : "");
      this.setIndex(String.valueOf(index));
      this.setValue(value);
      this.setFacets(facets != null ? facets : BFacets.NULL);
      this.setPointType(3);
   }

   public BDiscoveryPoint(
      String objectName, String objectId, String propertyId, String index, BValue value, BDiscoveryPoint[] kids, BFacets facets, int pointType
   ) {
      this.setObjectName(objectName);

      try {
         this.setObjectId((BBacnetObjectIdentifier)this.getObjectId().decodeFromString(objectId));
      } catch (IOException var10) {
      }

      this.setPropertyIdentifier(propertyId);
      this.setIndex(index);
      this.setValue(value);
      this.setFacets(facets != null ? facets : BFacets.NULL);
      this.setPointType(pointType);
      if (kids != null) {
         for (int i = 0; i < kids.length; i++) {
            this.add("prop" + i, kids[i]);
         }
      }
   }

   public boolean isNull() {
      return this == NULL;
   }

   public int getPropertyId() {
      if (this.getPropertyIdentifier().length() == 0) {
         return -1;
      } else {
         try {
            return BBacnetPropertyIdentifier.ordinal(this.getPropertyIdentifier());
         } catch (Exception var2) {
            return -1;
         }
      }
   }

   public int getPropertyArrayIndex() {
      if (this.getIndex().length() == 0) {
         return -1;
      } else {
         try {
            return Integer.parseInt(this.getIndex());
         } catch (Exception var2) {
            return -1;
         }
      }
   }

   public boolean hasChildren() {
      return this.getPointType() == 0 || this.getPointType() == 2;
   }

   public int getChildCount() {
      return this.getSlotCount(BDiscoveryPoint.class);
   }

   public BDiscoveryPoint[] getPoints() {
      return (BDiscoveryPoint[])this.getChildren(BDiscoveryPoint.class);
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("DiscPt:")
         .append(this.getObjectName())
         .append("[" + this.getObjectId() + "]")
         .append(':')
         .append(this.getPropertyIdentifier())
         .append('{')
         .append(this.getIndex())
         .append('}')
         .append(" =")
         .append(this.getValue())
         .append("; f=" + this.getFacets())
         .append("; t=")
         .append(this.getPointType());
      return sb.toString();
   }
}
