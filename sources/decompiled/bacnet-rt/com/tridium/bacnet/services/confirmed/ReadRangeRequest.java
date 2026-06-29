package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.sys.BEnum;

public class ReadRangeRequest extends BacnetConfirmedRequest implements RangeReference {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int BY_POSITION_TAG = 3;
   public static final int BY_TIME_TAG_DEPRECATED = 4;
   public static final int TIME_RANGE_TAG_DEPRECATED = 5;
   public static final int BY_SEQUENCE_NUMBER_TAG = 6;
   public static final int BY_TIME_TAG = 7;
   public static final int INTEGER16_MIN = -32768;
   public static final int INTEGER16_MAX = 32767;
   private BBacnetObjectIdentifier objectId = null;
   private int propertyId = -1;
   private int propertyArrayIndex = -1;
   private int rangeType = -1;
   private long referenceIndex = -1L;
   private BBacnetDateTime referenceTime = null;
   private int count = 0;
   private BBacnetDateTime beginningTime = null;
   private BBacnetDateTime endingTime = null;

   public ReadRangeRequest() {
      this(null, -1, -1, -1, -1L, null, 0);
   }

   public ReadRangeRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, int rangeType, long referenceIndex, int count) {
      this(objectId, propertyId, propertyArrayIndex, rangeType, referenceIndex, null, count);
   }

   public ReadRangeRequest(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, BBacnetDateTime referenceTime, int count) {
      this(objectId, propertyId, propertyArrayIndex, 7, -1L, referenceTime, count);
   }

   public ReadRangeRequest(
      BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, int rangeType, long referenceIndex, BBacnetDateTime referenceTime, int count
   ) {
      super(26);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.rangeType = rangeType;
      this.referenceIndex = referenceIndex;
      this.referenceTime = referenceTime;
      this.count = count;
   }

   public BBacnetObjectIdentifier getObjectId() {
      return this.objectId;
   }

   public void setObjectId(BBacnetObjectIdentifier objectId) {
      this.objectId = objectId;
   }

   @Override
   public int getPropertyId() {
      return this.propertyId;
   }

   public void setPropertyId(BEnum propertyId) {
      this.propertyId = propertyId.getOrdinal();
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

   public boolean isRangeUsed() {
      return this.rangeType != -1;
   }

   @Override
   public int getRangeType() {
      return this.rangeType;
   }

   @Override
   public long getReferenceIndex() {
      return this.referenceIndex;
   }

   public void setReferenceIndex(long referenceIndex) {
      this.referenceIndex = referenceIndex;
   }

   @Override
   public BBacnetDateTime getReferenceTime() {
      return this.referenceTime;
   }

   public void setReferenceTime(BBacnetDateTime referenceTime) {
      this.referenceTime = referenceTime;
   }

   @Override
   public int getCount() {
      return this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      this.writeEncoded((AsnOutputStream)out);
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      this.readEncoded((AsnInputStream)in);
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeObjectIdentifier(0, this.objectId);
      out.writeEnumerated(1, this.propertyId);
      if (this.propertyArrayIndex != -1) {
         out.writeUnsignedInteger(2, this.propertyArrayIndex);
      }

      switch (this.rangeType) {
         case 3:
            out.writeOpeningTag(this.rangeType);
            out.writeUnsignedInteger(this.referenceIndex);
            out.writeSignedInteger(count(this.count));
            out.writeClosingTag(this.rangeType);
            break;
         case 4:
            out.writeOpeningTag(this.rangeType);
            out.writeDate(this.referenceTime.getDate());
            out.writeTime(this.referenceTime.getTime());
            out.writeSignedInteger(count(this.count));
            out.writeClosingTag(this.rangeType);
            break;
         case 5:
            out.writeOpeningTag(this.rangeType);
            out.writeDate(this.beginningTime.getDate());
            out.writeTime(this.beginningTime.getTime());
            out.writeDate(this.endingTime.getDate());
            out.writeTime(this.endingTime.getTime());
            out.writeClosingTag(this.rangeType);
            break;
         case 6:
            out.writeOpeningTag(this.rangeType);
            out.writeUnsignedInteger(this.referenceIndex);
            out.writeSignedInteger(count(this.count));
            out.writeClosingTag(this.rangeType);
            break;
         case 7:
            out.writeOpeningTag(this.rangeType);
            this.referenceTime.writeAsn(out);
            out.writeSignedInteger(count(this.count));
            out.writeClosingTag(this.rangeType);
            break;
         default:
            throw new IllegalStateException("Invalid range type in ReadRangeRequest:" + this.rangeType);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.objectId = in.readObjectIdentifier(0);
      this.propertyId = in.readEnumerated(1);
      in.peekTag();
      if (in.isValueTag(2)) {
         this.propertyArrayIndex = in.readUnsignedInt(2);
         in.peekTag();
      } else {
         this.propertyArrayIndex = -1;
      }

      if (in.isOpeningTag(3)) {
         this.rangeType = 3;
         in.skipTag();
         this.referenceIndex = in.readUnsignedInteger();
         this.count = in.readSignedInteger();
         in.skipTag();
      } else if (in.isOpeningTag(4)) {
         this.rangeType = 4;
         in.skipTag();
         this.referenceTime = new BBacnetDateTime();
         this.referenceTime.readAsn(in);
         this.count = in.readSignedInteger();
         in.skipTag();
      } else if (in.isOpeningTag(5)) {
         this.rangeType = 5;
         in.skipTag();
         this.beginningTime = new BBacnetDateTime();
         this.beginningTime.readAsn(in);
         this.endingTime = new BBacnetDateTime();
         this.endingTime.readAsn(in);
         in.skipTag();
      } else if (in.isOpeningTag(6)) {
         this.rangeType = 6;
         in.skipTag();
         this.referenceIndex = in.readUnsignedInteger();
         this.count = in.readSignedInteger();
         in.skipTag();
      } else if (in.isOpeningTag(7)) {
         this.rangeType = 7;
         in.skipTag();
         this.referenceTime = new BBacnetDateTime();
         this.referenceTime.readAsn(in);
         this.count = in.readSignedInteger();
         in.skipTag();
      } else {
         this.rangeType = -1;
      }
   }

   public static int count(int count) {
      if (count > 32767) {
         return 32767;
      } else {
         return count < -32768 ? -32768 : count;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadRangeRequest: ");
      sb.append("\n  objId  " + this.objectId);
      sb.append("\n  propId " + this.propertyId);
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      sb.append("\n  rangeType " + this.rangeType);
      switch (this.rangeType) {
         case -1:
            sb.append("\n  RANGE NOT USED");
            break;
         case 0:
         case 1:
         case 2:
         default:
            sb.append("INVALID RANGE TYPE TAG! ");
            break;
         case 3:
            sb.append("\n  refNdx(pos) " + this.referenceIndex);
            sb.append("\n  count       " + this.count);
            break;
         case 4:
            sb.append("\n  refTim " + this.referenceTime);
            sb.append("\n  count  " + this.count);
            break;
         case 5:
            sb.append("\n  begTim " + this.beginningTime);
            sb.append("\n  endTim " + this.endingTime);
            break;
         case 6:
            sb.append("\n  refNdx(sqN) " + this.referenceIndex);
            sb.append("\n  count       " + this.count);
            break;
         case 7:
            sb.append("\n  refTim " + this.referenceTime);
            sb.append("\n  count  " + this.count);
      }

      return sb.toString();
   }

   @Override
   public BacnetServicePrimitive doParseAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      return new ReadRangeAck(serviceChoice, inputStream);
   }
}
