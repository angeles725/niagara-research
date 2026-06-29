package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.datatypes.BDeviceDiscoveryConfig;
import com.tridium.bacnet.datatypes.BDiscoveryNetworks;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.IAmListener;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.link.BBacnetLinkLayer;
import com.tridium.bacnet.stack.link.ethernet.BBacnetEthernetLinkLayer;
import com.tridium.bacnet.stack.link.ip.BBacnetIpLinkLayer;
import com.tridium.bacnet.stack.link.mstp.BBacnetMstpLinkLayer;
import com.tridium.bacnet.stack.link.sc.BScLinkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import java.text.MessageFormat;
import java.util.ArrayList;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.enums.BCharacterSetEncoding;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetDiscoverDevicesJob extends BDeviceManagerJob implements IAmListener {
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverDevicesJob.class);
   private BDeviceDiscoveryConfig params;
   private ArrayList<BBacnetDiscoverDevicesJob.IAmDevice> iAmDevices = new ArrayList<>();
   private int count;
   private static final Log logger = Log.getLog("bacnet.client");
   private static final int DUPLICATE_MAC = -2;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverDevicesJob() {
   }

   public BBacnetDiscoverDevicesJob(BBacnetNetwork bacnet, BDeviceDiscoveryConfig params) {
      super(bacnet);
      this.params = params;
   }

   public void run(Context cx) throws Exception {
      if (this.bacnet == null) {
         throw new IllegalStateException("Must submit thru BacnetNetwork.submitDeviceManagerJob()");
      } else if (this.params != null) {
         this.log().start(lex.getText("deviceManager.begin"));
         String s = null;
         this.server().registerIAmListener(this);
         BDiscoveryNetworks networks = this.params.getNetworks();
         if (networks.isAllNetworks()) {
            BBacnetAddress addr = BBacnetAddress.GLOBAL_BROADCAST_ADDRESS;
            s = lex.getText("deviceManager.params.global");

            try {
               if (this.params.isDefaultRange()) {
                  this.log().message(s + lex.getText("deviceManager.params.all"));
                  this.client().whoIs(addr);
               } else {
                  s = s + MessageFormat.format(lex.getText("deviceManager.params.range"), this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
                  this.log().message(s);
                  this.client().whoIs(addr, this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
               }
            } catch (Exception var25) {
               this.log().failed(lex.getText("deviceManager.failed") + "\n  " + var25);
               logger.error("Exception sending " + s + ": " + var25, var25);
               return;
            }
         } else {
            s = lex.getText("deviceManager.params.local");

            try {
               int[] nets = networks.getNetworks();

               for (int i = 0; i < nets.length; i++) {
                  BBacnetAddress addr = new BBacnetAddress(nets[i], (BBacnetOctetString)null);
                  if (this.params.isDefaultRange()) {
                     this.log().message(s + lex.getText("deviceManager.params.all"));
                     this.client().whoIs(addr);
                  } else {
                     String rangeMessage = s
                        + MessageFormat.format(lex.getText("deviceManager.params.range"), this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
                     this.log().message(rangeMessage);
                     this.client().whoIs(addr, this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
                  }
               }
            } catch (Exception var26) {
               this.log().failed(lex.getText("deviceManager.failed") + "\n  " + var26);
               logger.error("Exception sending " + s + ": " + var26, var26);
               return;
            }
         }

         BAbsTime start = BAbsTime.make();
         long startMillis = start.getMillis();
         BRelTime wait = BRelTime.makeSeconds(this.params.getWaitResponseTime());
         BAbsTime end = start.add(wait);
         double waitMillis = end.getMillis() - startMillis;
         BAbsTime now = BAbsTime.make();
         this.count = 0;
         int iAmSize = 0;
         BBacnetDiscoverDevicesJob.IAmDevice iAmDev = null;
         ArrayList<BBacnetObjectIdentifier> dups = new ArrayList<>();
         synchronized (this.iAmDevices) {
            iAmSize = this.iAmDevices.size();
         }

         while ((now.isBefore(end) || this.count < iAmSize) && this.isAlive()) {
            if (this.count == iAmSize) {
               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var23) {
               }
            } else {
               synchronized (this.iAmDevices) {
                  iAmDev = this.iAmDevices.get(this.count);
               }

               int deviceId = iAmDev.iAm.getObjectId().getInstanceNumber();
               if (this.params.isDefaultRange() || deviceId >= this.params.getDeviceLowLimit() && deviceId <= this.params.getDeviceHighLimit()) {
                  this.log().message("Reading parameters for " + iAmDev.iAm.getObjectId() + ", " + (this.count + 1) + " of " + iAmSize);
                  BDiscoveryDevice dd = this.discoverDevice(iAmDev);
                  if (iAmDev.dup) {
                     dd.setDuplicate(true);
                     dups.add(iAmDev.iAm.getObjectId());
                  }

                  this.add(null, dd);
               }

               this.count++;
            }

            now = BAbsTime.make();
            int timeProgress = (int)((now.getMillis() - startMillis) / waitMillis * 100.0);
            int deviceProgress = (int)(this.count * 100.0 / iAmSize);
            int oldProgress = this.getProgress();
            int progres = oldProgress;
            if (timeProgress < deviceProgress) {
               if (timeProgress > oldProgress) {
                  progres = timeProgress;
               }
            } else {
               if (deviceProgress > oldProgress) {
                  progres = deviceProgress;
               }

               if (deviceProgress == 0) {
                  progres = timeProgress;
               }
            }

            if (progres > 100) {
               progres = 100;
               this.log().message(lex.getText("deviceManager.finishing"));
            }

            this.progress(progres);
            synchronized (this.iAmDevices) {
               iAmSize = this.iAmDevices.size();
            }
         }

         for (BBacnetObjectIdentifier id : dups) {
            BDiscoveryDevice[] dupdevs = this.getDuplicateDevices(id);
            if (dupdevs.length > 1) {
               for (int ix = 0; ix < dupdevs.length; ix++) {
                  dupdevs[ix].setDuplicate(true);
               }
            }
         }

         this.log().success(lex.getText("deviceManager.end"));
         this.server().unregisterIAmListener(this);
      }
   }

   private BDiscoveryDevice discoverDevice(BBacnetDiscoverDevicesJob.IAmDevice iAmDev) {
      IAmRequest request = iAmDev.iAm;
      BBacnetAddress sourceAddress = iAmDev.addr;
      BBacnetObjectIdentifier deviceId = request.getObjectId();
      String name = deviceId.toString(BacnetConst.nameContext);
      BCharacterSetEncoding encoding = BCharacterSetEncoding.unknown;
      int listSize = -1;
      BBacnetBitString servicesSupported = BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetServicesSupported"));
      String vendorName = lex.getText("deviceManager.unknown");
      String modelName = lex.getText("deviceManager.unknown");
      int protocolRevision = -1;
      String firmwareRevision = lex.getText("deviceManager.unknown");
      String applicationSoftwareVersion = lex.getText("deviceManager.unknown");

      try {
         byte[] b = this.client().readProperty(sourceAddress, deviceId, 77);
         if (b != null) {
            encoding = AsnUtil.getCharacterSetEncoding(b);
            name = AsnUtil.fromAsnCharacterString(b);
            if (!this.isValidName(name)) {
               name = "";
            }
         }

         b = this.client().readProperty(sourceAddress, deviceId, 76, 0);
         if (b != null) {
            listSize = AsnUtil.fromAsnUnsignedInt(b);
         }

         b = this.client().readProperty(sourceAddress, deviceId, 97);
         if (b != null) {
            servicesSupported = AsnUtil.fromAsnBitString(b);
         }

         b = this.client().readProperty(sourceAddress, deviceId, 121);
         if (b != null) {
            vendorName = AsnUtil.fromAsnCharacterString(b);
         }

         b = this.client().readProperty(sourceAddress, deviceId, 70);
         if (b != null) {
            modelName = AsnUtil.fromAsnCharacterString(b);
         }

         b = this.readProperty(this.client(), sourceAddress, deviceId, 139);
         if (b != null) {
            protocolRevision = AsnUtil.fromAsnUnsigned(b).getInt();
         }

         b = this.client().readProperty(sourceAddress, deviceId, 44);
         if (b != null) {
            firmwareRevision = AsnUtil.fromAsnCharacterString(b);
         }

         b = this.client().readProperty(sourceAddress, deviceId, 12);
         if (b != null) {
            applicationSoftwareVersion = AsnUtil.fromAsnCharacterString(b);
         }
      } catch (AsnException var15) {
         this.log().failed("Unable to convert device name/listSize for " + name + ":" + var15);
      } catch (BacnetException var16) {
         this.log().failed("BacnetException " + var16 + " trying to read device parameters for " + deviceId);
         logger.error("BacnetException trying to read device parameters: " + var16, var16);
      } catch (Exception var17) {
         this.log().failed("Unable to read device parameters for " + name + " [" + deviceId + "]:" + var17);
      }

      return new BDiscoveryDevice(
         name,
         request,
         sourceAddress,
         listSize,
         encoding,
         servicesSupported,
         vendorName,
         modelName,
         protocolRevision,
         firmwareRevision,
         applicationSoftwareVersion
      );
   }

   private byte[] readProperty(BBacnetClientLayer client, BBacnetAddress sourceAddress, BBacnetObjectIdentifier deviceId, int propertyId) throws BacnetException {
      try {
         return client.readProperty(sourceAddress, deviceId, propertyId);
      } catch (ErrorException var7) {
         if (propertyId == 139) {
            ErrorType error = var7.getErrorType();
            if (error.getErrorClass() == 2 && error.getErrorCode() == 32) {
               return AsnUtil.toAsnUnsigned(0L);
            }
         }

         throw var7;
      }
   }

   BDiscoveryDevice[] getDuplicateDevices(BBacnetObjectIdentifier objectId) {
      Array<BDiscoveryDevice> a = new Array(BDiscoveryDevice.class);
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BDiscoveryDevice.class)) {
         BDiscoveryDevice d = (BDiscoveryDevice)sc.get();
         if (d.getObjectId().equals(objectId)) {
            a.add(d);
         }
      }

      return (BDiscoveryDevice[])a.trim();
   }

   @Override
   public void receiveIAm(IAmRequest request, BBacnetAddress sourceAddress) {
      int inst = request.getObjectId().getInstanceNumber();
      if (this.identifierInRange(this.params, inst) && this.networkInRange(this.params, sourceAddress.getNetworkNumber())) {
         this.setLinkLayer(sourceAddress);
         this.addDevice(request, sourceAddress);
      }
   }

   private void setLinkLayer(BBacnetAddress sourceAddress) {
      BNetworkPort port = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getNetwork().getPortByNetwork(sourceAddress.getNetworkNumber());
      if (port != null && port.getNetworkNumber() == sourceAddress.getNetworkNumber()) {
         BBacnetLinkLayer link = port.getLink();
         if (link instanceof BBacnetIpLinkLayer) {
            sourceAddress.setInt(BBacnetAddress.addressType, 2, BacnetConst.noWrite);
         } else if (link instanceof BBacnetEthernetLinkLayer) {
            sourceAddress.setInt(BBacnetAddress.addressType, 1, BacnetConst.noWrite);
         } else if (link instanceof BBacnetMstpLinkLayer) {
            sourceAddress.setInt(BBacnetAddress.addressType, 3, BacnetConst.noWrite);
         } else if (link instanceof BScLinkLayer) {
            sourceAddress.setInt(BBacnetAddress.addressType, 4, BacnetConst.noWrite);
         }
      }
   }

   private void addDevice(IAmRequest request, BBacnetAddress sourceAddress) {
      BBacnetDiscoverDevicesJob.IAmDevice dev = new BBacnetDiscoverDevicesJob.IAmDevice(request, sourceAddress);
      String message = null;
      synchronized (this.iAmDevices) {
         int index = this.index(this.iAmDevices, request.getObjectId().getInstanceNumber(), sourceAddress);
         if (index < 0) {
            if (index != -2) {
               this.iAmDevices.add(dev);
            }

            message = MessageFormat.format(lex.getText("deviceManager.found"), request.getObjectId(), sourceAddress);
         } else {
            dev.dup = true;
            this.iAmDevices.add(dev);
         }
      }

      if (message != null) {
         this.log().message(message);
      }
   }

   private boolean identifierInRange(BDeviceDiscoveryConfig params, int instanceId) {
      return params.isDefaultRange() ? true : instanceId >= params.getDeviceLowLimit() && instanceId <= params.getDeviceHighLimit();
   }

   private boolean networkInRange(BDeviceDiscoveryConfig params, int networkNumber) {
      BDiscoveryNetworks networks = params.getNetworks();
      return networks.isAllNetworks() ? true : networks.contains(networkNumber);
   }

   private int index(ArrayList<BBacnetDiscoverDevicesJob.IAmDevice> v, int inst, BBacnetAddress sourceAddress) {
      for (int i = 0; i < v.size(); i++) {
         BBacnetDiscoverDevicesJob.IAmDevice iad = v.get(i);
         if (iad.iAm.getObjectId().getInstanceNumber() == inst) {
            if (iad.addr.getMacAddress().equals(sourceAddress.getMacAddress())) {
               return -2;
            }

            return i;
         }
      }

      return -1;
   }

   private boolean isValidName(String name) {
      if (name == null) {
         return false;
      } else if (name.length() == 0) {
         return false;
      } else {
         return name.trim().length() == 0 ? false : name.length() != 1 || SlotPath.isValidName(name);
      }
   }

   static class IAmDevice {
      IAmRequest iAm;
      BBacnetAddress addr;
      boolean dup;

      IAmDevice(IAmRequest iAm, BBacnetAddress addr) {
         this.iAm = iAm;
         this.addr = addr;
      }
   }
}
