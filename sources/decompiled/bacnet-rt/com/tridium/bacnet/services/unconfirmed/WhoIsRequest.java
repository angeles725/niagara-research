package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.io.AsnException;

public class WhoIsRequest extends BacnetUnconfirmedRequest {
   private static final int LIMITS_NOT_USED = -1;
   private static final int DEVICE_INSTANCE_LOW_RANGE_TAG = 0;
   private static final int DEVICE_INSTANCE_HIGH_RANGE_TAG = 1;
   private int deviceInstanceRangeLowLimit = -1;
   private int deviceInstanceRangeHighLimit = -1;

   public WhoIsRequest() {
      this(-1, -1);
   }

   public WhoIsRequest(int deviceInstanceRangeLowLimit, int deviceInstanceRangeHighLimit) {
      super(8);
      this.deviceInstanceRangeLowLimit = deviceInstanceRangeLowLimit;
      this.deviceInstanceRangeHighLimit = deviceInstanceRangeHighLimit;
   }

   public int getDeviceInstanceRangeLowLimit() {
      return this.deviceInstanceRangeLowLimit;
   }

   public int getDeviceInstanceRangeHighLimit() {
      return this.deviceInstanceRangeHighLimit;
   }

   public boolean useLimits() {
      return this.deviceInstanceRangeLowLimit != -1;
   }

   @Override
   public void writeEncoded(AsnOutputStream outputStream) {
      if (this.deviceInstanceRangeLowLimit != -1) {
         outputStream.writeUnsignedInteger(0, this.deviceInstanceRangeLowLimit);
         outputStream.writeUnsignedInteger(1, this.deviceInstanceRangeHighLimit);
      }
   }

   @Override
   public void readEncoded(AsnInputStream inputStream) throws AsnException {
      if (inputStream.peekTag() == -1) {
         this.deviceInstanceRangeLowLimit = -1;
         this.deviceInstanceRangeHighLimit = -1;
      } else {
         this.deviceInstanceRangeLowLimit = inputStream.readUnsignedInt(0);
         this.deviceInstanceRangeHighLimit = inputStream.readUnsignedInt(1);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(BacnetUnconfirmedServiceChoice.TAGS[8]);
      if (this.deviceInstanceRangeLowLimit != -1) {
         sb.append("\n  deviceInstanceRangeLowLimit " + this.deviceInstanceRangeLowLimit);
         sb.append("\n  deviceInstanceRangeHighLimit " + this.deviceInstanceRangeHighLimit);
      } else {
         sb.append("\n  ALL DEVICES");
      }

      return sb.toString();
   }
}
