package com.tridium.bacnet.asn;

import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RejectException;
import javax.baja.nre.util.ByteArrayUtil;

public class NBacnetPropertyValue implements BacnetConst, PropertyValue {
   public static final int PROPERTY_ID_TAG = 0;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 1;
   public static final int VALUE_TAG = 2;
   public static final int PRIORITY_TAG = 3;
   private int propertyId;
   private int propertyArrayIndex = -1;
   private byte[] propertyValue;
   private int priority = -1;

   public NBacnetPropertyValue() {
      this(-1, -1, null, -1);
   }

   public NBacnetPropertyValue(int propertyId, byte[] propertyValue) {
      this(propertyId, -1, propertyValue, -1);
   }

   public NBacnetPropertyValue(int propertyId, byte[] propertyValue, int priority) {
      this(propertyId, -1, propertyValue, priority);
   }

   public NBacnetPropertyValue(int propertyId, int propertyArrayIndex, byte[] propertyValue) {
      this(propertyId, propertyArrayIndex, propertyValue, -1);
   }

   public NBacnetPropertyValue(int propertyId, int propertyArrayIndex, byte[] propertyValue, int priority) {
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.propertyValue = propertyValue;
      this.priority = priority;
   }

   public NBacnetPropertyValue(PropertyValue pv) {
      this.propertyId = pv.getPropertyId();
      this.propertyArrayIndex = pv.getPropertyArrayIndex();
      this.propertyValue = pv.getPropertyValue();
   }

   @Override
   public int getPropertyId() {
      return this.propertyId;
   }

   public boolean isPropertyArrayIndexUsed() {
      return this.propertyArrayIndex != -1;
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   @Override
   public byte[] getPropertyValue() {
      return this.propertyValue;
   }

   public boolean isPriorityUsed() {
      return this.priority != -1;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public ErrorType getPropertyAccessError() {
      throw new IllegalStateException();
   }

   @Override
   public boolean isError() {
      return false;
   }

   @Override
   public int getErrorClass() {
      throw new IllegalStateException();
   }

   @Override
   public int getErrorCode() {
      throw new IllegalStateException();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         out.writeUnsignedInteger(1, this.propertyArrayIndex);
      }

      out.writeEncodedValue(2, this.propertyValue);
      if (this.priority != -1) {
         out.writeUnsignedInteger(3, this.priority);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException, RejectException {
      this.propertyId = in.readEnumerated(0);
      in.peekTag();
      if (in.isValueTag(1)) {
         this.propertyArrayIndex = in.readUnsignedInt(1);
      } else {
         this.propertyArrayIndex = -1;
      }

      this.propertyValue = in.readEncodedValue(2);
      in.peekTag();
      if (in.isValueTag(3)) {
         this.priority = in.readUnsignedInt(3);
      } else {
         this.priority = -1;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("\n  " + BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      sb.append(" val=").append(ByteArrayUtil.toHexString(this.propertyValue));
      sb.append(" pri=").append(this.priority);
      return sb.toString();
   }
}
