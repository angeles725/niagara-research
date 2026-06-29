package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.services.BacnetComplexAck;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BEnum;

public class ReadRangeAck extends BacnetComplexAck implements RangeData {
   public static final int OBJECT_ID_TAG = 0;
   public static final int PROPERTY_ID_TAG = 1;
   public static final int PROPERTY_ARRAY_INDEX_TAG = 2;
   public static final int RESULT_FLAGS_TAG = 3;
   public static final int ITEM_COUNT_TAG = 4;
   public static final int ITEM_DATA_TAG = 5;
   public static final int FIRST_SEQUENCE_NUMBER_TAG = 6;
   public static final int READ_RANGE_ACK_MAX_APP_HEADER_SIZE = 23;
   private BBacnetObjectIdentifier objectId = null;
   private int propertyId = -1;
   private int propertyArrayIndex = -1;
   private BBacnetBitString resultFlags = null;
   private long itemCount = -1L;
   private long firstSequenceNumber = -1L;
   private byte[] itemData = null;
   private boolean isError = false;
   private int errorClass = -1;
   private int errorCode = -1;

   public ReadRangeAck() {
      super(26);
   }

   public ReadRangeAck(
      BBacnetObjectIdentifier objectId, int propertyId, BBacnetBitString resultFlags, long itemCount, long firstSequenceNumber, byte[] itemData
   ) {
      this(objectId, propertyId, -1, resultFlags, itemCount, firstSequenceNumber, itemData);
   }

   public ReadRangeAck(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, BBacnetBitString resultFlags, long itemCount, byte[] itemData) {
      this(objectId, propertyId, propertyArrayIndex, resultFlags, itemCount, -1L, itemData);
   }

   public ReadRangeAck(
      BBacnetObjectIdentifier objectId,
      int propertyId,
      int propertyArrayIndex,
      BBacnetBitString resultFlags,
      long itemCount,
      long firstSequenceNumber,
      byte[] itemData
   ) {
      super(26);
      this.objectId = objectId;
      this.propertyId = propertyId;
      this.propertyArrayIndex = propertyArrayIndex;
      this.resultFlags = resultFlags;
      this.itemCount = itemCount;
      this.firstSequenceNumber = firstSequenceNumber;
      this.itemData = itemData;
   }

   public ReadRangeAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.readEncoded(inputStream);
   }

   public ReadRangeAck(int errorClass, int errorCode) {
      super(26);
      this.isError = true;
      this.errorClass = errorClass;
      this.errorCode = errorCode;
   }

   public ReadRangeAck(BBacnetErrorClass errorClass, BBacnetErrorCode errorCode) {
      super(26);
      this.isError = true;
      this.errorClass = errorClass.getOrdinal();
      this.errorCode = errorCode.getOrdinal();
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

   @Override
   public BBacnetBitString getResultFlags() {
      return this.resultFlags;
   }

   @Override
   public boolean includesFirstItem() {
      return this.resultFlags.getBit(0);
   }

   @Override
   public boolean includesLastItem() {
      return this.resultFlags.getBit(1);
   }

   @Override
   public boolean isMoreItems() {
      return this.resultFlags.getBit(2);
   }

   public boolean isEmpty() {
      return this.itemCount <= 0L;
   }

   @Override
   public long getItemCount() {
      return this.itemCount;
   }

   public boolean isFirstSequenceNumberUsed() {
      return this.firstSequenceNumber != -1L;
   }

   @Override
   public long getFirstSequenceNumber() {
      return this.firstSequenceNumber;
   }

   @Override
   public byte[] getItemData() {
      return this.itemData;
   }

   @Override
   public ErrorType getError() {
      return new NErrorType(this.errorClass, this.errorCode);
   }

   @Override
   public int getErrorClass() {
      return this.errorClass;
   }

   @Override
   public int getErrorCode() {
      return this.errorCode;
   }

   @Override
   public boolean isError() {
      return this.isError;
   }

   @Override
   public boolean equals(Object o) {
      if (o == null) {
         return false;
      } else if (!(o instanceof ReadRangeAck)) {
         return false;
      } else {
         ReadRangeAck rra = (ReadRangeAck)o;
         if (this.isError) {
            return !rra.isError ? false : this.errorClass == rra.errorClass && this.errorCode == rra.errorCode;
         } else if (rra.isError) {
            return false;
         } else if (!this.objectId.equals(rra.objectId)) {
            return false;
         } else if (this.propertyId != rra.propertyId) {
            return false;
         } else if (this.propertyArrayIndex != rra.propertyArrayIndex) {
            return false;
         } else if (this.itemCount != rra.itemCount) {
            return false;
         } else if (this.firstSequenceNumber != rra.firstSequenceNumber) {
            return false;
         } else {
            if (this.resultFlags != null) {
               if (rra.resultFlags == null) {
                  return false;
               }

               if (!this.resultFlags.equals(rra.resultFlags)) {
                  return false;
               }
            } else if (rra.resultFlags != null) {
               return false;
            }

            return ByteArrayUtil.equals(this.itemData, rra.itemData);
         }
      }
   }

   @Override
   public int hashCode() {
      return 1;
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
      if (this.isError) {
         out.writeEnumerated(this.errorClass);
         out.writeEnumerated(this.errorCode);
      } else {
         out.writeObjectIdentifier(0, this.objectId);
         out.writeEnumerated(1, this.propertyId);
         if (this.propertyArrayIndex != -1) {
            out.writeUnsignedInteger(2, this.propertyArrayIndex);
         }

         out.writeBitString(3, this.resultFlags);
         out.writeUnsignedInteger(4, this.itemCount);
         out.writeEncodedValue(5, this.itemData);
         if (this.isFirstSequenceNumberUsed()) {
            out.writeUnsignedInteger(6, this.firstSequenceNumber);
         }
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      this.objectId = in.readObjectIdentifier(0);
      this.propertyId = in.readEnumerated(1);
      in.peekTag();
      if (in.isValueTag(2)) {
         this.propertyArrayIndex = in.readUnsignedInt(2);
      } else {
         this.propertyArrayIndex = -1;
      }

      this.resultFlags = in.readBitString(3);
      this.itemCount = in.readUnsignedInteger(4);
      this.itemData = in.readEncodedValue(5);
      int tag = in.peekTag();
      if (tag != -1) {
         this.firstSequenceNumber = in.readUnsignedInteger(6);
      } else {
         this.firstSequenceNumber = -1L;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("ReadRangeACK: ");
      sb.append("\n  objId  " + this.objectId);
      sb.append("\n  propId " + this.propertyId);
      if (this.propertyArrayIndex != -1) {
         sb.append("[" + this.propertyArrayIndex + "]");
      }

      sb.append("\n  rFlags " + this.resultFlags.toString(BacnetBitStringUtil.BacnetResultFlags));
      sb.append("\n  itmCnt " + this.itemCount);
      sb.append("\n  frstSN " + this.firstSequenceNumber);
      sb.append("\n  itmDta " + ByteArrayUtil.toHexString(this.itemData));
      return sb.toString();
   }
}
