package com.tridium.bacnet.history;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyReference;
import com.tridium.bacnet.datatypes.BTrendEvent;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.control.BControlPoint;
import javax.baja.history.BTrendFlags;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "BBacnetBitString",
   defaultValue = "BBacnetBitString.DEFAULT",
   flags = 8
)
public class BBacnetBitStringTrendRecord extends BBacnetTrendRecord {
   public static final Property value = newProperty(8, BBacnetBitString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetBitStringTrendRecord.class);
   private static final int MAX_BITS_SUPPORTED = 50;
   private int propertyId;
   private static Logger logger = Logger.getLogger("bacnet.server");

   public BBacnetBitString getValue() {
      return (BBacnetBitString)this.get(value);
   }

   public void setValue(BBacnetBitString v) {
      this.set(value, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetBitStringTrendRecord() {
      this(111);
   }

   public BBacnetBitStringTrendRecord(int propertyId) {
      this.propertyId = propertyId;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatus status, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setValue(BBacnetBitString.DEFAULT);
      this.setStatus(status);
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      return this;
   }

   @Override
   public BBacnetTrendRecord set(BAbsTime timestamp, BStatusValue out, long seqNum, BTrendEvent event, BTrendFlags trendFlags) {
      this.setTimestamp(timestamp);
      this.setStatus(out.getStatus());
      this.setSequenceNumber(seqNum);
      this.setLogEvent(event);
      this.setTrendFlags(trendFlags);
      this.setBitStringValue(out);
      return this;
   }

   private void setBitStringValue(BStatusValue statusValue) {
      BControlPoint targetPoint = (BControlPoint)statusValue.getParent();
      if (targetPoint != null) {
         BOrd pointOrd = targetPoint.getHandleOrd();
         BBacnetObjectIdentifier logObjId = BBacnetNetwork.localDevice().lookupBacnetObjectId(pointOrd);
         if (logObjId != null) {
            BBacnetBitString bitString = this.readBitStringProperty(logObjId);
            this.setValue(bitString);
         }
      } else {
         BStatusNumeric statusNumeric = (BStatusNumeric)statusValue;
         this.setValue(this.getBacnetBitString((long)statusNumeric.getNumeric()));
      }
   }

   private BBacnetBitString readBitStringProperty(BBacnetObjectIdentifier logObjId) {
      BBacnetBitString bitString = BBacnetBitString.DEFAULT;

      try {
         PropertyValue val = BBacnetNetwork.localDevice().lookupBacnetObject(logObjId).readProperty(new NBacnetPropertyReference(this.propertyId, -1));
         byte[] bytes = val.getPropertyValue();
         bitString = bytes != null ? AsnUtil.fromAsnBitString(bytes) : BBacnetBitString.DEFAULT;
      } catch (Exception var5) {
         logger.severe("Error reading bit string value for " + logObjId.toString(null) + " " + var5.getMessage());
      }

      return bitString;
   }

   public Property getValueProperty() {
      return value;
   }

   public boolean isFixedSize() {
      return Boolean.TRUE;
   }

   @Override
   public int getLogDatumType() {
      int eventCheck = super.getLogDatumType();
      if (eventCheck != -1) {
         return eventCheck;
      } else {
         return this.getStatus().isNull() ? 7 : 6;
      }
   }

   @Override
   protected void doReadTrend(DataInput in) throws IOException {
      super.doReadTrend(in);
      this.setValue(this.getBacnetBitString(in.readLong()));
   }

   @Override
   protected void doWriteTrend(DataOutput out) throws IOException {
      super.doWriteTrend(out);
      BBacnetBitString value = this.getValue();
      out.writeLong(getBits(value));
   }

   public static long getBits(BBacnetBitString value) {
      boolean[] bits = value.getBits();
      long ret = 0L;
      if (bits.length > 0) {
         for (int index = bits.length - 1; index >= 0; index--) {
            if (bits[index]) {
               ret |= 1 << bits.length - index - 1;
            }
         }
      }

      long len = value.length();
      return ret | len << 50;
   }

   private BBacnetBitString getBacnetBitString(long bits) {
      int len = (int)(bits >> 50);
      if (len >= 0 && len <= 50) {
         boolean[] bitArray = new boolean[len];
         int counter = 0;

         for (int bIndex = 0; bIndex < len; bIndex++) {
            long ref = 1L << bIndex;
            bitArray[len - bIndex - 1] = (bits & ref) == ref;
         }

         return BBacnetBitString.make(bitArray);
      } else {
         return BBacnetBitString.DEFAULT;
      }
   }
}
