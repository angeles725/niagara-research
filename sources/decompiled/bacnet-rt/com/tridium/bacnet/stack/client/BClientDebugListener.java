package com.tridium.bacnet.stack.client;

import com.tridium.bacnet.asn.NReadAccessResult;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NWriteAccessSpec;
import com.tridium.bacnet.services.BacnetComplexAck;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import com.tridium.bacnet.services.confirmed.AcknowledgeAlarmRequest;
import com.tridium.bacnet.services.confirmed.AtomicReadFileRequest;
import com.tridium.bacnet.services.confirmed.AtomicWriteFileRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedCovNotificationRequest;
import com.tridium.bacnet.services.confirmed.ConfirmedEventNotificationRequest;
import com.tridium.bacnet.services.confirmed.GetEventInformationRequest;
import com.tridium.bacnet.services.confirmed.ListElementRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.ReadPropertyRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.confirmed.ReadRangeRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyMultipleRequest;
import com.tridium.bacnet.services.confirmed.WritePropertyRequest;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedCovNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.UnconfirmedEventNotificationRequest;
import com.tridium.bacnet.services.unconfirmed.WhoHasRequest;
import com.tridium.bacnet.stack.AppDebugListener;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "deviceNameEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "deviceName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "addressEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "deviceAddress",
      type = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT"
   ), @NiagaraProperty(
      name = "serviceChoiceEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "serviceChoices",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetServicesSupported\"))",
      facets = {@Facet("BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS")}
   ), @NiagaraProperty(
      name = "objectIdEnabled",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   )})
public class BClientDebugListener extends BComponent implements AppDebugListener {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property deviceNameEnabled = newProperty(0, false, null);
   public static final Property deviceName = newProperty(0, "", null);
   public static final Property addressEnabled = newProperty(0, false, null);
   public static final Property deviceAddress = newProperty(0, BBacnetAddress.DEFAULT, null);
   public static final Property serviceChoiceEnabled = newProperty(0, false, null);
   public static final Property serviceChoices = newProperty(
      0,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetServicesSupported")),
      BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_FACETS
   );
   public static final Property objectIdEnabled = newProperty(0, false, null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BClientDebugListener.class);
   private static final int[] confirmedServiceChoiceMap = new int[]{
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 35, 37, 38, 39
   };
   private static final int[] unconfirmedServiceChoiceMap = new int[]{26, 27, 28, 29, 30, 31, 32, 33, 34, 36};
   private static final Logger logger = Logger.getLogger("bacnet.client");

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public boolean getDeviceNameEnabled() {
      return this.getBoolean(deviceNameEnabled);
   }

   public void setDeviceNameEnabled(boolean v) {
      this.setBoolean(deviceNameEnabled, v, null);
   }

   public String getDeviceName() {
      return this.getString(deviceName);
   }

   public void setDeviceName(String v) {
      this.setString(deviceName, v, null);
   }

   public boolean getAddressEnabled() {
      return this.getBoolean(addressEnabled);
   }

   public void setAddressEnabled(boolean v) {
      this.setBoolean(addressEnabled, v, null);
   }

   public BBacnetAddress getDeviceAddress() {
      return (BBacnetAddress)this.get(deviceAddress);
   }

   public void setDeviceAddress(BBacnetAddress v) {
      this.set(deviceAddress, v, null);
   }

   public boolean getServiceChoiceEnabled() {
      return this.getBoolean(serviceChoiceEnabled);
   }

   public void setServiceChoiceEnabled(boolean v) {
      this.setBoolean(serviceChoiceEnabled, v, null);
   }

   public BBacnetBitString getServiceChoices() {
      return (BBacnetBitString)this.get(serviceChoices);
   }

   public void setServiceChoices(BBacnetBitString v) {
      this.set(serviceChoices, v, null);
   }

   public boolean getObjectIdEnabled() {
      return this.getBoolean(objectIdEnabled);
   }

   public void setObjectIdEnabled(boolean v) {
      this.setBoolean(objectIdEnabled, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      BBacnetClientLayer clientLayer = this.getClient();
      if (clientLayer != null) {
         clientLayer.addDebugListener(this);
      }
   }

   public void stopped() throws Exception {
      super.stopped();
      BBacnetClientLayer clientLayer = this.getClient();
      if (clientLayer != null) {
         clientLayer.removeDebugListener(this);
      }
   }

   @Override
   public void receive(BBacnetAddress address, BacnetServicePrimitive message) {
      if (this.passFilter(address, message, true)) {
         this.display(address, message, true);
      }
   }

   @Override
   public void send(BBacnetAddress address, BacnetServicePrimitive message) {
      if (this.passFilter(address, message, false)) {
         this.display(address, message, false);
      }
   }

   protected boolean passFilter(BBacnetAddress address, BacnetServicePrimitive message, boolean rcv) {
      if (message == null) {
         return false;
      } else {
         try {
            if (!this.getEnabled()) {
               return false;
            } else {
               boolean pass = true;
               if (this.getAddressEnabled()) {
                  pass = this.checkAddress(address);
               }

               if (!pass) {
                  return false;
               } else {
                  if (this.getDeviceNameEnabled()) {
                     pass = this.checkDeviceName(address);
                  }

                  if (!pass) {
                     return false;
                  } else {
                     if (this.getServiceChoiceEnabled()) {
                        pass = this.checkServiceChoice(message);
                     }

                     if (!pass) {
                        return false;
                     } else {
                        if (this.getObjectIdEnabled()) {
                           pass = this.checkObjectId(message);
                        }

                        return !pass ? false : pass;
                     }
                  }
               }
            }
         } catch (Exception var5) {
            logger.log(Level.SEVERE, "passFilter exception occurred: ", (Throwable)var5);
            return false;
         }
      }
   }

   private boolean checkAddress(BBacnetAddress address) {
      return equals(address, this.getDeviceAddress());
   }

   private boolean checkDeviceName(BBacnetAddress address) {
      BBacnetNetwork net = BBacnetNetwork.bacnet();
      BBacnetDevice dev = net.lookupDeviceByAddress(address);
      return dev == null ? false : dev.getName().equals(this.getDeviceName());
   }

   private boolean checkServiceChoice(BacnetServicePrimitive message) {
      int serviceType = message.getServiceType();
      int serviceChoice = message.getServiceChoice();
      switch (serviceType) {
         case 0:
         case 2:
         case 3:
            return this.getServiceChoices().getBit(confirmedServiceChoiceMap[serviceChoice]);
         case 1:
            return this.getServiceChoices().getBit(unconfirmedServiceChoiceMap[serviceChoice]);
         case 4:
         case 5:
         case 6:
         case 7:
            return false;
         default:
            return false;
      }
   }

   private boolean checkObjectId(BacnetServicePrimitive message) {
      int serviceType = message.getServiceType();
      int serviceChoice = message.getServiceChoice();
      switch (serviceType) {
         case 0:
            return this.checkConfirmed(serviceChoice, (BacnetConfirmedRequest)message);
         case 1:
            return this.checkUnconfirmed(serviceChoice, (BacnetUnconfirmedRequest)message);
         case 2:
         case 4:
         case 5:
         case 6:
         case 7:
            return false;
         case 3:
            return this.checkComplexAck(serviceChoice, (BacnetComplexAck)message);
         default:
            return false;
      }
   }

   private boolean checkConfirmed(int serviceChoice, BacnetConfirmedRequest message) {
      BBacnetObjectIdentifier myId = this.getObjectId();
      switch (serviceChoice) {
         case 0:
            return myId.equals(((AcknowledgeAlarmRequest)message).getEventObjectId());
         case 1:
            return myId.equals(((ConfirmedCovNotificationRequest)message).getCovNotificationParameters().getMonitoredObjectId());
         case 2:
            return myId.equals(((ConfirmedEventNotificationRequest)message).getEventNotificationParameters().getEventObjectId());
         case 3:
         case 4:
         case 10:
         case 11:
         case 13:
         case 17:
         case 18:
         case 19:
         case 20:
         case 21:
         case 22:
         case 23:
         case 24:
         case 25:
         case 27:
         case 28:
         default:
            return false;
         case 5:
            return myId.equals(((SubscribeCovRequest)message).getMonitoredObjectId());
         case 6:
            return myId.equals(((AtomicReadFileRequest)message).getFileId());
         case 7:
            return myId.equals(((AtomicWriteFileRequest)message).getFileId());
         case 8:
            return myId.equals(((ListElementRequest)message).getObjectId());
         case 9:
            return myId.equals(((ListElementRequest)message).getObjectId());
         case 12:
            return myId.equals(((ReadPropertyRequest)message).getObjectId());
         case 14:
            ListIterator<NReadAccessSpec> rit = ((ReadPropertyMultipleRequest)message).getReadAccessSpecs();

            while (rit.hasNext()) {
               if (myId.equals(rit.next().getObjectId())) {
                  return true;
               }
            }

            return false;
         case 15:
            return myId.equals(((WritePropertyRequest)message).getObjectId());
         case 16:
            ListIterator<NWriteAccessSpec> wit = ((WritePropertyMultipleRequest)message).getWriteAccessSpecs();

            while (wit.hasNext()) {
               if (myId.equals(wit.next().getObjectId())) {
                  return true;
               }
            }

            return false;
         case 26:
            return myId.equals(((ReadRangeRequest)message).getObjectId());
         case 29:
            return myId.equals(((GetEventInformationRequest)message).getLastReceivedObjectId());
      }
   }

   private boolean checkUnconfirmed(int serviceChoice, BacnetUnconfirmedRequest message) {
      BBacnetObjectIdentifier myId = this.getObjectId();
      switch (serviceChoice) {
         case 0:
            return myId.equals(((IAmRequest)message).getObjectId());
         case 1:
            return myId.equals(((IHaveRequest)message).getObjectId());
         case 2:
            return myId.equals(((UnconfirmedCovNotificationRequest)message).getCovNotificationParameters().getMonitoredObjectId());
         case 3:
            return myId.equals(((UnconfirmedEventNotificationRequest)message).getEventNotificationParameters().getEventObjectId());
         case 4:
         case 5:
         case 6:
         default:
            return false;
         case 7:
            return myId.equals(((WhoHasRequest)message).getObjectId());
      }
   }

   private boolean checkComplexAck(int serviceChoice, BacnetComplexAck message) {
      BBacnetObjectIdentifier myId = this.getObjectId();
      switch (serviceChoice) {
         case 12:
            return this.getObjectId().equals(((ReadPropertyAck)message).getObjectId());
         case 14:
            ListIterator it = ((ReadPropertyMultipleAck)message).getReadAccessResults();

            while (it.hasNext()) {
               if (myId.equals(((NReadAccessResult)it.next()).getObjectId())) {
                  return true;
               }
            }

            return false;
         case 26:
            return myId.equals(((ReadRangeAck)message).getObjectId());
         default:
            return false;
      }
   }

   private BBacnetClientLayer getClient() {
      BComplex parent = this;

      while (parent != null) {
         parent = parent.getParent();
         if (parent instanceof BBacnetClientLayer) {
            return (BBacnetClientLayer)parent;
         }
      }

      return null;
   }

   private static boolean equals(BBacnetAddress a1, BBacnetAddress a2) {
      if (a1 == null) {
         return a2 == null;
      } else {
         return a2 == null ? false : a1.equals(a2.getNetworkNumber(), a2.getMacAddress().getBytes());
      }
   }

   private void display(BBacnetAddress address, BacnetServicePrimitive message, boolean recv) {
      if (recv) {
         System.out.println("BACnet Recv [" + address + "]:" + message);
      } else {
         System.out.println("BACnet Send [" + address + "]:" + message);
      }
   }
}
