package com.tridium.bacnet.stack;

import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;

public class DeviceEntry {
   BBacnetObjectIdentifier deviceId;
   BBacnetAddress address;
   int maxApduLengthSupported;
   BBacnetSegmentation segmentationSupported;
   int maxSegmentsAccepted;
   int protocolRevision;

   public DeviceEntry(BBacnetObjectIdentifier deviceId, BBacnetAddress address, int maxApduLengthSupported, BBacnetSegmentation segmentationSupported) {
      this(deviceId, address, maxApduLengthSupported, segmentationSupported, 0);
   }

   public DeviceEntry(
      BBacnetObjectIdentifier deviceId, BBacnetAddress address, int maxApduLengthSupported, BBacnetSegmentation segmentationSupported, int protocolRevision
   ) {
      this(deviceId, address, maxApduLengthSupported, segmentationSupported, 0, protocolRevision);
   }

   public DeviceEntry(
      BBacnetObjectIdentifier deviceId,
      BBacnetAddress address,
      int maxApduLengthSupported,
      BBacnetSegmentation segmentationSupported,
      int maxSegmentsAccepted,
      int protocolRevision
   ) {
      this.deviceId = deviceId;
      this.address = address;
      this.maxApduLengthSupported = maxApduLengthSupported;
      this.segmentationSupported = segmentationSupported;
      this.maxSegmentsAccepted = maxSegmentsAccepted;
      this.protocolRevision = protocolRevision;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.deviceId.toString())
         .append(" [")
         .append(this.address.toString())
         .append(']')
         .append(" APDU=")
         .append(this.maxApduLengthSupported)
         .append(" SEG=")
         .append(this.segmentationSupported)
         .append(" MaxSeg=")
         .append(this.maxSegmentsAccepted)
         .append(" PRev=")
         .append(this.protocolRevision);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return this.deviceId != null ? this.deviceId.hashCode() : 0;
   }

   @Override
   public boolean equals(Object o) {
      if (!(o instanceof DeviceEntry)) {
         return false;
      } else {
         DeviceEntry other = (DeviceEntry)o;
         return this.deviceId.equals(other.deviceId);
      }
   }
}
