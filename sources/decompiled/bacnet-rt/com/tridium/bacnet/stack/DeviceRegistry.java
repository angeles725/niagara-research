package com.tridium.bacnet.stack;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.LongHashMap;
import javax.baja.nre.util.LongHashMap.Iterator;

public class DeviceRegistry {
   private LongHashMap byAddress = new LongHashMap();
   private IntHashMap byId = new IntHashMap();
   private static DeviceRegistry registry = new DeviceRegistry();
   private static final int BYTE = 8;
   private static final int SIX_BYTES = 48;
   private static final int TWO_BYTE_MASK = 65535;
   private static final long BYTE_MASK = 255L;
   private static final long NET_MASK = -281474976710656L;
   private static final Logger log = Logger.getLogger("bacnet.stack");

   public static Iterator addressIterator() {
      return registry.byAddress.iterator();
   }

   public static javax.baja.nre.util.IntHashMap.Iterator idIterator() {
      return registry.byId.iterator();
   }

   public static void dump() {
      System.out.println("By Address Table");
      Iterator addrIt = registry.byAddress.iterator();
      int i = 0;

      while (addrIt.hasNext()) {
         DeviceEntry n = (DeviceEntry)addrIt.next();
         long k = addrIt.key();
         System.out.println(++i + "  k=" + k + " -> " + n);
      }

      System.out.println("\nBy ID Table");
      javax.baja.nre.util.IntHashMap.Iterator idIt = registry.byId.iterator();
      i = 0;

      while (idIt.hasNext()) {
         DeviceEntry n = (DeviceEntry)idIt.next();
         int k = idIt.key();
         System.out.println(++i + "  k=" + k + " -> " + n);
      }

      System.out.println();
   }

   public static void update(BBacnetDevice device) {
      registry.updateEntry(
         device.getObjectId(),
         device.getAddress(),
         device.getMaxAPDULengthAccepted(),
         device.getSegmentationSupported(),
         device.getMaxSegmentsAccepted(),
         device.getProtocolRevision()
      );
   }

   public static void update(BBacnetObjectIdentifier deviceId, BBacnetAddress address, int maxApduLengthSupported, BBacnetSegmentation segmentationSupported) {
      registry.updateEntry(deviceId, address, maxApduLengthSupported, segmentationSupported);
   }

   public static void remove(BBacnetObjectIdentifier deviceId) {
      registry.removeEntry(deviceId);
   }

   public static int getMaxApduLengthSupported(BBacnetAddress address) {
      return registry.getMaxLength(address);
   }

   public static BBacnetObjectIdentifier getDeviceId(BBacnetAddress address) {
      return registry.getDeviceObjectId(address);
   }

   public static BBacnetAddress getDeviceAddress(BBacnetObjectIdentifier objectId) {
      return registry.getAddress(objectId);
   }

   public static int getProtocolRevision(BBacnetAddress address) {
      return registry.getProtRev(address);
   }

   public static BBacnetSegmentation getSegmentationSupported(BBacnetAddress address) {
      return registry.getSegSupp(address);
   }

   public static int getMaxSegmentsAccepted(BBacnetAddress address) {
      return registry.getMaxSeg(address);
   }

   public void updateEntry(BBacnetObjectIdentifier deviceId, BBacnetAddress address, int maxApduLengthSupported, BBacnetSegmentation segmentationSupported) {
      this.updateEntry(deviceId, address, maxApduLengthSupported, segmentationSupported, 0);
   }

   public void updateEntry(
      BBacnetObjectIdentifier deviceId, BBacnetAddress address, int maxApduLengthSupported, BBacnetSegmentation segmentationSupported, int protocolRevision
   ) {
      this.updateEntry(deviceId, address, maxApduLengthSupported, segmentationSupported, 0, protocolRevision);
   }

   public void updateEntry(
      BBacnetObjectIdentifier deviceId,
      BBacnetAddress address,
      int maxApduLengthSupported,
      BBacnetSegmentation segmentationSupported,
      int maxSegmentsAccepted,
      int protocolRevision
   ) {
      if (!deviceId.equals(BBacnetObjectIdentifier.DEFAULT_DEVICE)) {
         DeviceEntry e = (DeviceEntry)this.byId.get(deviceId.hashCode());
         if (e != null) {
            e.deviceId = deviceId;
            e.maxApduLengthSupported = maxApduLengthSupported;
            e.segmentationSupported = segmentationSupported;
            e.maxSegmentsAccepted = maxSegmentsAccepted;
            e.protocolRevision = protocolRevision;
            this.byAddress.remove(getKey(e.address));
            e.address = address;
         } else {
            e = new DeviceEntry(deviceId, address, maxApduLengthSupported, segmentationSupported, maxSegmentsAccepted, protocolRevision);
            this.byId.put(deviceId.hashCode(), e);
         }

         this.byAddress.put(getKey(e.address), e);
         if (log.isLoggable(Level.FINE)) {
            log.fine(" [" + e.address + "] address new Device-Entry: " + e);
         }
      }
   }

   private void removeEntry(BBacnetObjectIdentifier deviceId) {
      DeviceEntry de = (DeviceEntry)this.byId.remove(deviceId.hashCode());
      if (de != null) {
         this.byAddress.remove(getKey(de.address));
      }
   }

   private int getMaxLength(BBacnetAddress address) {
      DeviceEntry entry = this.getByAddress(address);
      return entry == null ? 50 : entry.maxApduLengthSupported;
   }

   public BBacnetObjectIdentifier getDeviceObjectId(BBacnetAddress address) {
      DeviceEntry e = this.getByAddress(address);
      return e != null ? e.deviceId : null;
   }

   private BBacnetAddress getAddress(BBacnetObjectIdentifier objectId) {
      DeviceEntry e = (DeviceEntry)this.byId.get(objectId.hashCode());
      return e != null ? e.address : null;
   }

   private int getProtRev(BBacnetAddress address) {
      DeviceEntry entry = this.getByAddress(address);
      return entry == null ? 0 : entry.protocolRevision;
   }

   private BBacnetSegmentation getSegSupp(BBacnetAddress address) {
      DeviceEntry entry = this.getByAddress(address);
      return entry == null ? BBacnetSegmentation.noSegmentation : entry.segmentationSupported;
   }

   private int getMaxSeg(BBacnetAddress address) {
      DeviceEntry entry = this.getByAddress(address);
      return entry == null ? 0 : entry.maxSegmentsAccepted;
   }

   private DeviceEntry getByAddress(BBacnetAddress a) {
      return (DeviceEntry)this.byAddress.get(getKey(a));
   }

   public int getOidCount() {
      return this.byId.size();
   }

   public int getAddrCount() {
      return this.byAddress.size();
   }

   public static long getKey(BBacnetAddress address) {
      long networkNumber = address.getNetworkNumber();
      long key = -281474976710656L & (65535L & networkNumber) << 48;
      byte[] addr = address.getMacAddress().getAddr();
      if (addr != null && addr.length > 0) {
         long shift = addr.length * 8;

         for (int i = 0; i < addr.length; i++) {
            long value = addr[i] & 255L;
            key |= value << (int)(shift -= 8L);
         }
      }

      return key;
   }
}
