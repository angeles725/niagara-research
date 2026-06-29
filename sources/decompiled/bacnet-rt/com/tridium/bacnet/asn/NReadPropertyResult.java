package com.tridium.bacnet.asn;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.nre.util.ByteArrayUtil;

public class NReadPropertyResult implements AsnConst, PropertyValue {
   public static final int PROPERTY_ID_TAG = 2;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 3;
   public static final int PROPERTY_VALUE_TAG = 4;
   public static final int PROPERTY_ACCESS_ERROR_TAG = 5;
   private int propertyId;
   private int propertyArrayIndex = -1;
   private byte[] propertyValue;
   private ErrorType propertyAccessError;
   private NReadAccessResult rar;

   public NReadPropertyResult(NReadAccessResult rar) {
      this.rar = rar;
   }

   public NReadPropertyResult(int propertyId, byte[] propertyValue) {
      this(propertyId, -1, propertyValue, null);
   }

   public NReadPropertyResult(int propertyId, int propertyArrayIndex, byte[] propertyValue) {
      this(propertyId, propertyArrayIndex, propertyValue, null);
   }

   public NReadPropertyResult(int propertyId, ErrorType propertyAccessError) {
      this(propertyId, -1, null, propertyAccessError);
   }

   public NReadPropertyResult(int propertyId, int propertyArrayIndex, ErrorType propertyAccessError) {
      this(propertyId, propertyArrayIndex, null, propertyAccessError);
   }

   public NReadPropertyResult(int propertyId, int propertyArrayIndex, byte[] propertyValue, ErrorType propertyAccessError) {
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.propertyValue = propertyValue;
      this.propertyAccessError = propertyAccessError;
   }

   @Override
   public int getPropertyId() {
      return this.propertyId;
   }

   @Override
   public int getPropertyArrayIndex() {
      return this.propertyArrayIndex;
   }

   @Override
   public byte[] getPropertyValue() {
      return this.propertyValue;
   }

   @Override
   public int getPriority() {
      throw new IllegalStateException();
   }

   @Override
   public ErrorType getPropertyAccessError() {
      return this.propertyAccessError;
   }

   @Override
   public boolean isError() {
      return this.propertyAccessError != null;
   }

   @Override
   public int getErrorClass() {
      return this.propertyAccessError.getErrorClass();
   }

   @Override
   public int getErrorCode() {
      return this.propertyAccessError.getErrorCode();
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.rar != null ? this.rar.getObjectId() : null;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(2, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         out.writeUnsignedInteger(3, this.propertyArrayIndex);
      }

      if (this.propertyAccessError == null) {
         out.writeEncodedValue(4, this.propertyValue);
      } else {
         out.writeOpeningTag(5);
         out.writeEnumerated(this.getErrorClass());
         out.writeEnumerated(this.getErrorCode());
         out.writeClosingTag(5);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.propertyId = in.readEnumerated(2);
      int tag = in.peekTag();
      if (in.isValueTag(3)) {
         this.propertyArrayIndex = in.readUnsignedInt(3);
         tag = in.peekTag();
      } else {
         this.propertyArrayIndex = -1;
      }

      if (in.isOpeningTag(4)) {
         this.propertyValue = in.readEncodedValue(4);
      } else {
         if (!in.isOpeningTag(5)) {
            throw new AsnException("Invalid tag: " + tag);
         }

         in.skipTag();
         this.propertyAccessError = new NErrorType();
         this.propertyAccessError.readEncoded(in);
         in.skipTag();
      }
   }

   @Override
   public String toString() {
      return this.string(false);
   }

   public String debug() {
      return this.string(true);
   }

   private String string(boolean debug) {
      StringBuilder sb = new StringBuilder();
      if (debug) {
         sb.append("\n");
      }

      sb.append("  ").append(BBacnetPropertyIdentifier.tag(this.propertyId));
      if (this.propertyArrayIndex != -1) {
         sb.append('[').append(this.propertyArrayIndex).append(']');
      }

      if (this.isError()) {
         sb.append(" err=").append(this.propertyAccessError.toString());
      } else {
         sb.append(" val=");
         if (this.propertyValue == null) {
            sb.append("null");
         } else if (this.propertyValue.length == 0) {
            sb.append("-");
         } else {
            sb.append(ByteArrayUtil.toHexString(this.propertyValue));
         }
      }

      return sb.toString();
   }
}
