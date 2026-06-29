package javax.baja.bacnet.config;

import com.tridium.bacnet.stack.DeviceRegistry;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetAddressBinding;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetDeviceStatus;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetSegmentation;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.driver.loadable.BUploadParameters;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.DefaultFileCopy;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.DEVICE)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.DEVICE, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "systemStatus",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetDeviceStatus.OPERATIONAL, BEnumRange.make(BBacnetDeviceStatus.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SYSTEM_STATUS, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "vendorName",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.VENDOR_NAME, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "vendorIdentifier",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(-1)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.VENDOR_IDENTIFIER, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "modelName",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MODEL_NAME, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "firmwareRevision",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.FIRMWARE_REVISION, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "applicationSoftwareVersion",
      type = "String",
      defaultValue = "",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.APPLICATION_SOFTWARE_VERSION, ASN_CHARACTER_STRING)")}
   ), @NiagaraProperty(
      name = "protocolVersion",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(1)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROTOCOL_VERSION, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "protocolRevision",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(0)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROTOCOL_REVISION, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "protocolServicesSupported",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetServicesSupported\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROTOCOL_SERVICES_SUPPORTED, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_MAP)")}
   ), @NiagaraProperty(
      name = "protocolObjectTypesSupported",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetObjectTypesSupported\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROTOCOL_OBJECT_TYPES_SUPPORTED, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_OBJECT_TYPES_SUPPORTED_MAP)")}
   ), @NiagaraProperty(
      name = "objectList",
      type = "BBacnetArray",
      defaultValue = "new BBacnetArray(BBacnetObjectIdentifier.TYPE)",
      flags = 5,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_LIST, ASN_BACNET_ARRAY)")}
   ), @NiagaraProperty(
      name = "maxAPDULengthAccepted",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(ConfirmedRequestPdu.MAX_APDU_LENGTH_UP_TO_MIN_MSG_SIZE)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.MAX_APDU_LENGTH_ACCEPTED, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "segmentationSupported",
      type = "BBacnetSegmentation",
      defaultValue = "BBacnetSegmentation.noSegmentation",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.SEGMENTATION_SUPPORTED, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "apduTimeout",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(3000)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.APDU_TIMEOUT, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "numberOfAPDURetries",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(3)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.NUMBER_OF_APDU_RETRIES, ASN_UNSIGNED)")}
   ), @NiagaraProperty(
      name = "deviceAddressBinding",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetAddressBinding.TYPE)",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DEVICE_ADDRESS_BINDING, ASN_BACNET_LIST)")}
   ), @NiagaraProperty(
      name = "databaseRevision",
      type = "BBacnetUnsigned",
      defaultValue = "new BBacnetUnsigned(-1)",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.DATABASE_REVISION, ASN_UNSIGNED)")}
   )})
public class BBacnetDeviceObject extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(8), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(8, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property systemStatus = newProperty(1, BDynamicEnum.make(0, BEnumRange.make(BBacnetDeviceStatus.TYPE)), makeFacets(112, 9));
   public static final Property vendorName = newProperty(1, "", makeFacets(121, 7));
   public static final Property vendorIdentifier = newProperty(1, new BBacnetUnsigned(-1L), makeFacets(120, 2));
   public static final Property modelName = newProperty(1, "", makeFacets(70, 7));
   public static final Property firmwareRevision = newProperty(1, "", makeFacets(44, 7));
   public static final Property applicationSoftwareVersion = newProperty(1, "", makeFacets(12, 7));
   public static final Property protocolVersion = newProperty(1, new BBacnetUnsigned(1L), makeFacets(98, 2));
   public static final Property protocolRevision = newProperty(1, new BBacnetUnsigned(0L), makeFacets(139, 2));
   public static final Property protocolServicesSupported = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetServicesSupported")),
      makeFacets(97, 8, BacnetBitStringUtil.BACNET_SERVICES_SUPPORTED_MAP)
   );
   public static final Property protocolObjectTypesSupported = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetObjectTypesSupported")),
      makeFacets(96, 8, BacnetBitStringUtil.BACNET_OBJECT_TYPES_SUPPORTED_MAP)
   );
   public static final Property objectList = newProperty(5, new BBacnetArray(BBacnetObjectIdentifier.TYPE), makeFacets(76, -2));
   public static final Property maxAPDULengthAccepted = newProperty(1, new BBacnetUnsigned(50L), makeFacets(62, 2));
   public static final Property segmentationSupported = newProperty(1, BBacnetSegmentation.noSegmentation, makeFacets(107, 9));
   public static final Property apduTimeout = newProperty(0, new BBacnetUnsigned(3000L), makeFacets(11, 2));
   public static final Property numberOfAPDURetries = newProperty(0, new BBacnetUnsigned(3L), makeFacets(73, 2));
   public static final Property deviceAddressBinding = newProperty(0, new BBacnetListOf(BBacnetAddressBinding.TYPE), makeFacets(30, -3));
   public static final Property databaseRevision = newProperty(1, new BBacnetUnsigned(-1L), makeFacets(155, 2));
   public static final Type TYPE = Sys.loadType(BBacnetDeviceObject.class);
   private BBacnetObjectIdentifier oldId;

   public BEnum getSystemStatus() {
      return (BEnum)this.get(systemStatus);
   }

   public void setSystemStatus(BEnum v) {
      this.set(systemStatus, v, null);
   }

   public String getVendorName() {
      return this.getString(vendorName);
   }

   public void setVendorName(String v) {
      this.setString(vendorName, v, null);
   }

   public BBacnetUnsigned getVendorIdentifier() {
      return (BBacnetUnsigned)this.get(vendorIdentifier);
   }

   public void setVendorIdentifier(BBacnetUnsigned v) {
      this.set(vendorIdentifier, v, null);
   }

   public String getModelName() {
      return this.getString(modelName);
   }

   public void setModelName(String v) {
      this.setString(modelName, v, null);
   }

   public String getFirmwareRevision() {
      return this.getString(firmwareRevision);
   }

   public void setFirmwareRevision(String v) {
      this.setString(firmwareRevision, v, null);
   }

   public String getApplicationSoftwareVersion() {
      return this.getString(applicationSoftwareVersion);
   }

   public void setApplicationSoftwareVersion(String v) {
      this.setString(applicationSoftwareVersion, v, null);
   }

   public BBacnetUnsigned getProtocolVersion() {
      return (BBacnetUnsigned)this.get(protocolVersion);
   }

   public void setProtocolVersion(BBacnetUnsigned v) {
      this.set(protocolVersion, v, null);
   }

   public BBacnetUnsigned getProtocolRevision() {
      return (BBacnetUnsigned)this.get(protocolRevision);
   }

   public void setProtocolRevision(BBacnetUnsigned v) {
      this.set(protocolRevision, v, null);
   }

   public BBacnetBitString getProtocolServicesSupported() {
      return (BBacnetBitString)this.get(protocolServicesSupported);
   }

   public void setProtocolServicesSupported(BBacnetBitString v) {
      this.set(protocolServicesSupported, v, null);
   }

   public BBacnetBitString getProtocolObjectTypesSupported() {
      return (BBacnetBitString)this.get(protocolObjectTypesSupported);
   }

   public void setProtocolObjectTypesSupported(BBacnetBitString v) {
      this.set(protocolObjectTypesSupported, v, null);
   }

   public BBacnetArray getObjectList() {
      return (BBacnetArray)this.get(objectList);
   }

   public void setObjectList(BBacnetArray v) {
      this.set(objectList, v, null);
   }

   public BBacnetUnsigned getMaxAPDULengthAccepted() {
      return (BBacnetUnsigned)this.get(maxAPDULengthAccepted);
   }

   public void setMaxAPDULengthAccepted(BBacnetUnsigned v) {
      this.set(maxAPDULengthAccepted, v, null);
   }

   public BBacnetSegmentation getSegmentationSupported() {
      return (BBacnetSegmentation)this.get(segmentationSupported);
   }

   public void setSegmentationSupported(BBacnetSegmentation v) {
      this.set(segmentationSupported, v, null);
   }

   public BBacnetUnsigned getApduTimeout() {
      return (BBacnetUnsigned)this.get(apduTimeout);
   }

   public void setApduTimeout(BBacnetUnsigned v) {
      this.set(apduTimeout, v, null);
   }

   public BBacnetUnsigned getNumberOfAPDURetries() {
      return (BBacnetUnsigned)this.get(numberOfAPDURetries);
   }

   public void setNumberOfAPDURetries(BBacnetUnsigned v) {
      this.set(numberOfAPDURetries, v, null);
   }

   public BBacnetListOf getDeviceAddressBinding() {
      return (BBacnetListOf)this.get(deviceAddressBinding);
   }

   public void setDeviceAddressBinding(BBacnetListOf v) {
      this.set(deviceAddressBinding, v, null);
   }

   public BBacnetUnsigned getDatabaseRevision() {
      return (BBacnetUnsigned)this.get(databaseRevision);
   }

   public void setDatabaseRevision(BBacnetUnsigned v) {
      this.set(databaseRevision, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.oldId = this.getObjectId();
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if (BBacnetPropertyIdentifier.maxSegmentsAccepted.getTag().equals(p.getName())) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(this.device().getName() + " added slot maxSegmentsAccepted " + this.device().getMaxSegmentsAccepted());
            }

            DeviceRegistry.update(this.device());
            if (log.isLoggable(Level.FINEST)) {
               log.finest(this.device().getName() + " deviceObject added callback execution finish for property " + p);
            }
         }
      }
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && cx != fallback) {
         if (p.equals(objectId)) {
            BBacnetObjectIdentifier newId = this.getObjectId();
            BBacnetNetwork network = BBacnetNetwork.bacnet();
            if (newId.equals(this.oldId)) {
               return;
            }

            BBacnetDevice d = network.doLookupDeviceById(newId);
            if (d != null && d != this.device()) {
               log.severe("Duplicate Object ID:" + newId + ", used by " + d.getName() + "!\n  Resetting to old id:" + this.oldId);
               this.set(objectId, this.oldId, fallback);
            } else {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("Object ID changed from " + this.oldId + " to " + newId);
               }

               this.device().objectIdChanged();
               network.getLocalDevice().updateAddressBinding(this.oldId, newId);
               network.updateDevice(this.device());
               if (this.oldId != null) {
                  DeviceRegistry.remove(this.oldId);
               }

               DeviceRegistry.update(this.device());
               this.oldId = newId;
            }
         } else if (p.equals(protocolRevision)) {
            DeviceRegistry.update(this.device());
         } else if (p.equals(segmentationSupported)) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(this.device().getName() + " segmentationSupported changed to " + this.device().getSegmentationSupported());
            }

            DeviceRegistry.update(this.device());
         } else if (p.equals(maxAPDULengthAccepted)) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(this.device().getName() + " maxAPDULengthAccepted changed to " + this.device().getMaxAPDULengthAccepted());
            }

            DeviceRegistry.update(this.device());
         } else if (BBacnetPropertyIdentifier.maxSegmentsAccepted.getTag().equals(p.getName())) {
            if (log.isLoggable(Level.FINE)) {
               log.fine(this.device().getName() + " maxSegmentsAccepted changed to " + this.device().getMaxSegmentsAccepted());
            }

            DeviceRegistry.update(this.device());
         }

         if (p.equals(protocolServicesSupported)) {
            this.device().updateServicesSupported();
         }

         if (log.isLoggable(Level.FINEST)) {
            log.finest(this.device().getName() + " deviceObject changed callback execution finish for property " + p);
         }
      }
   }

   @Override
   public String toString(Context context) {
      return this.getObjectName() + " [" + this.getObjectId().toString(context) + "]";
   }

   @Override
   protected boolean shouldPoll(int propertyId) {
      switch (propertyId) {
         case 24:
         case 56:
         case 57:
         case 112:
            return true;
         default:
            return false;
      }
   }

   @Override
   public void doUpload(BUploadParameters p, Context cx) {
      if (this.device().getEnabled() && !this.getStatus().isDown()) {
         if (this.getObjectId().isValid()) {
            try {
               this.readProperty(maxAPDULengthAccepted);
               this.readProperty(segmentationSupported);
               this.readProperty(vendorIdentifier);
               this.readProperty(modelName);
               this.readProperty(firmwareRevision);
               this.readProperty(applicationSoftwareVersion);
               if (this.getVendorIdentifier().getInt() == 36 && this.getFirmwareRevision().startsWith("3")) {
                  try {
                     BOrd vendorObjectTypesFile = (BOrd)this.device().get("vendorObjectTypesFile");
                     if (vendorObjectTypesFile != null && !vendorObjectTypesFile.equals(BOrd.NULL)) {
                        DefaultFileCopy.copyFile("niagaraAxBacnetObjectTypes.xml");
                        this.device().set("vendorObjectTypesFile", BOrd.make("file!defaults/niagaraAxBacnetObjectTypes.xml"));
                     }
                  } catch (Exception var4) {
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Exception setting vendorObjectTypesFile for BACnet deviceObject " + this + ":" + var4, (Throwable)var4);
                     }
                  }
               }
            } catch (Exception var5) {
               if (log.isLoggable(Level.FINE)) {
                  log.log(Level.FINE, "Exception uploading BACnet deviceObject " + this + ":" + var5, (Throwable)var5);
               }
            }

            super.doUpload(p, cx);
            if (log.isLoggable(Level.FINEST)) {
               log.finest(this.device().getName() + "deviceObject upload execution finish.");
            }
         }
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine(this.device().getName() + " is either disabled or status is down, deviceObject upload is unsuccessful.");
         }
      }
   }

   public int getMaxSegmentsAccepted() {
      BBacnetUnsigned msa = (BBacnetUnsigned)this.get(BBacnetPropertyIdentifier.maxSegmentsAccepted.getTag());
      return msa != null ? msa.getInt() : 0;
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetDeviceObject", 2);
      out.prop("oldId", this.oldId);
      out.endProps();
   }
}
