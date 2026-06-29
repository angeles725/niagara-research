package com.tridium.bacnet.asn;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;

public class NBacnetPropertyReference implements PropertyReference, BacnetConst {
   public static final int PROPERTY_ID_TAG = 0;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 1;
   private int propertyId;
   private int propertyArrayIndex;

   public NBacnetPropertyReference() {
      this(-1, -1);
   }

   public NBacnetPropertyReference(int propertyId) {
      this(propertyId, -1);
   }

   public NBacnetPropertyReference(int propertyId, int propertyArrayIndex) {
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
   }

   @Override
   public int getPropertyId() {
      return this.propertyId;
   }

   public void setPropertyId(int propertyId) {
      this.propertyId = propertyId;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.propertyArrayIndex != -1;
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   public void setPropertyArrayIndex(int propertyArrayIndex) {
      this.propertyArrayIndex = propertyArrayIndex;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         out.writeUnsignedInteger(1, this.propertyArrayIndex);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.propertyId = in.readEnumerated(0);
      in.peekTag();
      if (in.isValueTag(1)) {
         this.propertyArrayIndex = in.readUnsignedInt(1);
      } else {
         this.propertyArrayIndex = -1;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(32);
      sb.append("  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      return sb.toString();
   }

   @Override
   public boolean equals(Object o) {
      return !(o instanceof NBacnetPropertyReference)
         ? false
         : this.propertyId == ((NBacnetPropertyReference)o).propertyId && this.propertyArrayIndex == ((NBacnetPropertyReference)o).propertyArrayIndex;
   }

   @Override
   public int hashCode() {
      return 1;
   }
}
