package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.datatypes.BEventSaver;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmPriorities;
import javax.baja.alarm.BAlarmRecipient;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDestination;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"alarm:AlarmClass"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "alarmClassOrd",
      type = "BOrd",
      defaultValue = "BOrd.NULL",
      flags = 64
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.NOTIFICATION_CLASS)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "eventSaver",
      type = "BAlarmRecipient",
      defaultValue = "new BEventSaver()"
   )})
public class BBacnetNotificationClassDescriptor extends BComponent implements BIBacnetExportObject, BacnetAlarmConst, BacnetPropertyListProvider {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property alarmClassOrd = newProperty(64, BOrd.NULL, null);
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(15), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property eventSaver = newProperty(0, new BEventSaver(), null);
   public static final Type TYPE = Sys.loadType(BBacnetNotificationClassDescriptor.class);
   private boolean fatalFault = false;
   private static final BIcon icon = BIcon.make(BIcon.std("alarm.png"), BIcon.std("badges/export.png"));
   private BAlarmClass ac;
   private boolean recipientListChanged = true;
   private BBacnetDestination[] recipientList = new BBacnetDestination[0];
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private static final AsnInputStream asnIn = new AsnInputStream();
   private static final AsnOutputStream asnOut = new AsnOutputStream();
   private static final int MAX_PRIORITY = 255;
   static Logger log = Logger.getLogger("bacnet.server");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 17, 86, 1, 102};
   private static final int[] OPTIONAL_PROPS = new int[]{28};

   @Override
   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BOrd getAlarmClassOrd() {
      return (BOrd)this.get(alarmClassOrd);
   }

   public void setAlarmClassOrd(BOrd v) {
      this.set(alarmClassOrd, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public String getObjectName() {
      return this.getString(objectName);
   }

   @Override
   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BAlarmRecipient getEventSaver() {
      return (BAlarmRecipient)this.get(eventSaver);
   }

   public void setEventSaver(BAlarmRecipient v) {
      this.set(eventSaver, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void started() throws Exception {
      super.started();
      this.checkFatalFault();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.checkConfiguration();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      local.unsubscribe(this, this.ac);
      this.oldId = null;
      this.oldName = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var4) {
            }

            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(alarmClassOrd)) {
            this.checkConfiguration();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }

            super.changed(p, cx);
         }
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(15) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return this.getAlarmClass();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getAlarmClassOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(alarmClassOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         local.unsubscribe(this, this.ac);
         this.findAlarmClass();
         boolean configOk = true;
         if (this.ac == null) {
            this.setFaultCause("Cannot find exported alarm class");
            configOk = false;
         } else {
            local.subscribe(this, this.ac);
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
         }

         if (configOk) {
            BEventSaver eventSaver = (BEventSaver)this.getEventSaver();
            BLink[] links = this.getEventSaver().getLinks(BAlarmRecipient.routeAlarm);

            for (int i = 0; i < links.length; i++) {
               eventSaver.remove(links[i]);
            }

            BLink link = new BLink(this.ac.getHandleOrd(), "alarm", "routeAlarm", true);
            eventSaver.add("link", link, 4);
            String err = local.export(this);
            if (err != null) {
               this.duplicate = true;
               this.setFaultCause(err);
               configOk = false;
            } else {
               this.duplicate = false;
            }
         }

         if (configOk) {
            this.setFaultCause("");
         }

         this.setStatus(BStatus.makeFault(this.getStatus(), !configOk));
      }
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      this.getAlarmClass();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getAlarmClass();
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }

               props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 80:
               int[] props = OPTIONAL_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 105:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            default:
               results.add(this.readProperty(refs[i].getPropertyId(), refs[i].getPropertyArrayIndex()));
         }
      }

      return results.toArray(new PropertyValue[0]);
   }

   @Override
   public final RangeData readRange(RangeReference rangeReference) throws RejectException {
      this.getAlarmClass();
      if (this.ac == null) {
         return new ReadRangeAck(1, 1000);
      } else {
         int propertyId = rangeReference.getPropertyId();
         if (!hasProperty(propertyId)) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 102) {
            return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else if (rangeReference.getPropertyArrayIndex() != -1) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
         } else {
            int rangeType = rangeReference.getRangeType();
            switch (rangeType) {
               case -1:
               case 3:
                  int maxDataLength = -1;
                  if (rangeReference instanceof BacnetConfirmedRequest) {
                     maxDataLength = ((BacnetConfirmedRequest)rangeReference).getMaxDataLength() - 23 + 3 + 5;
                  }

                  boolean[] rflags = new boolean[]{false, false, false};
                  BBacnetDestination[] list = this.getRecipientList();
                  int len = list.length;
                  if (rangeType == 3) {
                     int refNdx = (int)rangeReference.getReferenceIndex();
                     int count = rangeReference.getCount();
                     if (refNdx <= len && refNdx >= 1) {
                        Array<BBacnetDestination> a = new Array(BBacnetDestination.class);
                        int itemsFound = 0;
                        if (count > 0) {
                           for (int i = refNdx - 1; i < len && itemsFound < count; i++) {
                              a.add(list[i]);
                              itemsFound++;
                           }

                           if (refNdx == 1) {
                              rflags[0] = true;
                           }

                           if (refNdx + count - 1 >= len) {
                              rflags[1] = true;
                           }
                        } else {
                           if (count >= 0) {
                              return new ReadRangeAck(5, 7);
                           }

                           count = -count;

                           for (int i = refNdx - 1; i >= 0 && itemsFound < count; i--) {
                              a.add(list[i]);
                              itemsFound++;
                           }

                           a = a.reverse();
                           if (refNdx - count <= 0) {
                              rflags[0] = true;
                           }

                           if (refNdx == len) {
                              rflags[1] = true;
                           }
                        }

                        Iterator<BBacnetDestination> it = a.iterator();
                        int itemCount = 0;
                        synchronized (asnOut) {
                           asnOut.reset();
                           if (maxDataLength > 0) {
                              while (it.hasNext()) {
                                 if (maxDataLength - asnOut.size() < 35) {
                                    rflags[1] = false;
                                    break;
                                 }

                                 it.next().writeAsn(asnOut);
                                 itemCount++;
                              }
                           } else {
                              itemCount = itemsFound;

                              while (it.hasNext()) {
                                 it.next().writeAsn(asnOut);
                              }
                           }

                           if (itemCount < itemsFound) {
                              rflags[2] = true;
                           }

                           return new ReadRangeAck(
                              this.getObjectId(), rangeReference.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray()
                           );
                        }
                     }

                     return new ReadRangeAck(this.getObjectId(), rangeReference.getPropertyId(), -1, BBacnetBitString.emptyBitString(3), 0L, new byte[0]);
                  } else {
                     rflags[0] = false;
                     int itemCount = 0;
                     synchronized (asnOut) {
                        asnOut.reset();
                        if (maxDataLength > 0) {
                           for (int i = 0; i < len; i++) {
                              list[i].writeAsn(asnOut);
                              itemCount++;
                              if (maxDataLength - asnOut.size() < 35) {
                                 break;
                              }
                           }

                           if (itemCount > 0) {
                              rflags[0] = true;
                           }

                           if (itemCount > 0 && itemCount == len) {
                              rflags[1] = true;
                           }
                        } else {
                           itemCount = len;

                           for (int ix = 0; ix < len; ix++) {
                              list[ix].writeAsn(asnOut);
                           }

                           if (len > 0) {
                              rflags[0] = true;
                           }

                           if (len > 0 && len == len) {
                              rflags[1] = true;
                           }
                        }

                        if (itemCount < len) {
                           rflags[2] = true;
                        }

                        return new ReadRangeAck(
                           this.getObjectId(), rangeReference.getPropertyId(), -1, BBacnetBitString.make(rflags), itemCount, asnOut.toByteArray()
                        );
                     }
                  }
               case 0:
               case 1:
               case 2:
               case 4:
               case 5:
               default:
                  return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.parameterOutOfRange);
               case 6:
                  return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotNumbered);
               case 7:
                  return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.listItemNotTimestamped);
            }
         }
      }
   }

   private static boolean hasProperty(int propertyId) {
      for (int id : REQUIRED_PROPS) {
         if (id == propertyId) {
            return true;
         }
      }

      for (int idx : OPTIONAL_PROPS) {
         if (idx == propertyId) {
            return true;
         }
      }

      return propertyId == 371;
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      this.getAlarmClass();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      BAlarmClass ac = this.getAlarmClass();
      if (ac == null) {
         return new NChangeListError(8, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 102) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return (ChangeListError)(propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.addRecipients(propertyValue));
         }
      }
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      BAlarmClass ac = this.getAlarmClass();
      if (ac == null) {
         return new NChangeListError(9, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 102) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return (ChangeListError)(propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.removeRecipients(propertyValue));
         }
      }
   }

   boolean isArray(int propertyId) {
      return propertyId == 86 ? true : propertyId == 371;
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      BAlarmClass ac = this.getAlarmClass();
      if (ac == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 1:
               BAlarmTransitionBits bits = ac.getAckRequired();
               synchronized (asnOut) {
                  asnOut.reset();
                  asnOut.writeBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(bits));
                  return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
               }
            case 17:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getObjectId().getInstanceNumber()));
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 86:
               BAlarmPriorities pri = ac.getPriority();
               switch (ndx) {
                  case -1:
                     synchronized (asnOut) {
                        asnOut.reset();
                        asnOut.writeUnsignedInteger(pri.getToOffnormal());
                        asnOut.writeUnsignedInteger(pri.getToFault());
                        asnOut.writeUnsignedInteger(pri.getToNormal());
                        return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
                     }
                  case 0:
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(3L));
                  case 1:
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(pri.getToOffnormal()));
                  case 2:
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(pri.getToFault()));
                  case 3:
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(pri.getToNormal()));
                  default:
                     return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
               }
            case 102:
               BBacnetDestination[] list = this.getRecipientList();
               synchronized (asnOut) {
                  asnOut.reset();

                  for (int i = 0; i < list.length; i++) {
                     list[i].writeAsn(asnOut);
                  }

                  return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
               }
            case 371:
               return this.readPropertyList(ndx);
            default:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BAlarmClass ac = this.getAlarmClass();
      if (ac == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 1:
                  synchronized (asnIn) {
                     asnIn.setBuffer(val);
                     ac.set(BAlarmClass.ackRequired, BacnetBitStringUtil.getBAlarmTransitionBits(asnIn.readBitString()), BLocalBacnetDevice.getBacnetContext());
                  }

                  return null;
               case 17:
                  return new NErrorType(2, 40);
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 75:
               case 79:
               case 371:
                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               case 86:
                  BAlarmPriorities apri = ac.getPriority();

                  try {
                     switch (ndx) {
                        case -1:
                           synchronized (asnIn) {
                              asnIn.setBuffer(val);

                              NErrorType var10000;
                              try {
                                 int toOffNormal = asnIn.readUnsignedInt();
                                 int toFault = asnIn.readUnsignedInt();
                                 int toNormal = asnIn.readUnsignedInt();
                                 if (toOffNormal < 0 || toFault < 0 || toNormal < 0 || toOffNormal > 255 || toFault > 255 || toNormal > 255) {
                                    return new NErrorType(2, 37);
                                 }

                                 var10000 = AsnUtil.peekTagAndPerform(
                                    asnIn, -1, 42, () -> ac.setPriority(BAlarmPriorities.make(toOffNormal, toFault, toNormal))
                                 );
                              } catch (Exception var14) {
                                 return new NErrorType(2, 42);
                              }

                              return var10000;
                           }
                        case 0:
                           return new NErrorType(2, 42);
                        case 1:
                           ac.set(
                              BAlarmClass.priority,
                              BAlarmPriorities.make(AsnUtil.fromAsnUnsignedInt(val), apri.getToFault(), apri.getToNormal()),
                              BLocalBacnetDevice.getBacnetContext()
                           );
                           return null;
                        case 2:
                           ac.set(
                              BAlarmClass.priority,
                              BAlarmPriorities.make(apri.getToOffnormal(), AsnUtil.fromAsnUnsignedInt(val), apri.getToNormal()),
                              BLocalBacnetDevice.getBacnetContext()
                           );
                           return null;
                        case 3:
                           ac.set(
                              BAlarmClass.priority,
                              BAlarmPriorities.make(apri.getToOffnormal(), apri.getToFault(), AsnUtil.fromAsnUnsignedInt(val)),
                              BLocalBacnetDevice.getBacnetContext()
                           );
                           return null;
                        default:
                           return new NErrorType(2, 42);
                     }
                  } catch (IllegalStateException var16) {
                     return new NErrorType(2, 37);
                  } catch (IllegalArgumentException var17) {
                     return new NErrorType(2, 37);
                  }
               case 102:
                  return this.writeRecipientList(val);
               default:
                  return new NErrorType(2, 32);
            }
         } catch (OutOfRangeException var18) {
            log.warning("Out Of Range Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var18);
            return new NErrorType(2, 37);
         } catch (AsnException var19) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var19);
            return new NErrorType(2, 9);
         } catch (PermissionException var20) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var20);
            return new NErrorType(2, 40);
         }
      }
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   public int getNotificationClass() {
      return this.getObjectId().getInstanceNumber();
   }

   public int[] getEventPriorities() {
      BAlarmPriorities pri = this.getAlarmClass().getPriority();
      return new int[]{pri.getToOffnormal(), pri.getToFault(), pri.getToNormal()};
   }

   public BBacnetDestination[] getRecipientList() {
      synchronized (this.recipientList) {
         if (this.recipientListChanged) {
            this.buildRecipientList();
         }

         return this.recipientList;
      }
   }

   public void recipientListChanged() {
      synchronized (this.recipientList) {
         this.recipientListChanged = true;
      }
   }

   public final BAlarmClass getAlarmClass() {
      return this.ac == null ? this.findAlarmClass() : this.ac;
   }

   private BAlarmClass findAlarmClass() {
      try {
         if (!alarmClassOrd.isEquivalentToDefaultValue(this.getAlarmClassOrd())) {
            BObject o = this.getAlarmClassOrd().get(this);
            if (o instanceof BAlarmClass) {
               this.ac = (BAlarmClass)o;
            } else {
               this.ac = null;
            }
         }

         return this.ac;
      } catch (Exception var2) {
         log.warning("Unable to resolve alarm class ord for " + this + ": " + this.getAlarmClassOrd() + ": " + var2);
         this.ac = null;
         if (this.ac == null && this.isRunning()) {
            this.setFaultCause("Cannot find exported alarm class");
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
         }

         return this.ac;
      }
   }

   private void buildRecipientList() {
      synchronized (this.recipientList) {
         Array<BBacnetDestination> a = new Array(BBacnetDestination.class);
         Knob[] srcLinks = this.getAlarmClass().getKnobs(BAlarmClass.alarm);

         for (int i = 0; i < srcLinks.length; i++) {
            if (srcLinks[i].getTargetComponent() instanceof BBacnetDestination) {
               a.add((BBacnetDestination)srcLinks[i].getTargetComponent());
            }
         }

         this.recipientList = (BBacnetDestination[])a.trim();
         this.recipientListChanged = false;
      }
   }

   private ErrorType writeRecipientList(byte[] encodedList) {
      ArrayList<BBacnetDestination> v = new ArrayList<>();

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(encodedList);

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               BBacnetDestination d = new BBacnetDestination();
               d.readAsn(asnIn);
               v.add(d);
            }
         }
      } catch (AsnException var16) {
         if (BBacnetErrorCode.valueOutOfRange.getTag().equals(var16.getMessage())) {
            return new NErrorType(2, 37);
         }

         return new NErrorType(2, 9);
      }

      try {
         BAlarmClass ac = this.getAlarmClass();
         Knob[] knobs = ac.getKnobs(BAlarmClass.alarm);
         int len = knobs.length;
         boolean[] toKeep = new boolean[len];

         for (int i = 0; i < v.size(); i++) {
            boolean knobFound = false;
            BBacnetDestination dest = v.get(i);

            for (int j = 0; j < len; j++) {
               if (knobs[j].getTargetComponent() instanceof BBacnetDestination && dest.destinationEquals((BBacnetDestination)knobs[j].getTargetComponent())) {
                  knobFound = true;
                  toKeep[j] = true;
                  break;
               }
            }

            if (!knobFound) {
               BBacnetDestination linkDest = null;
               BComponent alarmService = this.getAlarmClass().getParent().asComponent();
               BBacnetDestination[] dests = (BBacnetDestination[])alarmService.getChildren(BBacnetDestination.class);

               for (int k = 0; k < dests.length; k++) {
                  if (dest.destinationEquals(dests[k])) {
                     linkDest = dests[k];
                     break;
                  }
               }

               if (linkDest == null) {
                  alarmService.add(null, dest, BLocalBacnetDevice.getBacnetContext());
                  linkDest = dest;
               }

               BLink link = new BLink(this.getAlarmClass().getHandleOrd(), "alarm", "routeAlarm", true);
               linkDest.add(null, link, BLocalBacnetDevice.getBacnetContext());
            }
         }

         for (int i = 0; i < len; i++) {
            if (!toKeep[i]) {
               BComponent target = knobs[i].getTargetComponent();
               if (target instanceof BBacnetDestination) {
                  BLink[] tgtLinks = target.getLinks(BAlarmRecipient.routeAlarm);

                  for (int jx = 0; jx < tgtLinks.length; jx++) {
                     if (tgtLinks[jx].getSourceComponent() == this.getAlarmClass()) {
                        target.remove(tgtLinks[jx]);
                        break;
                     }
                  }
               }
            }
         }

         return null;
      } catch (PermissionException var14) {
         log.warning("PermissionException writing elements to recipientList in object " + this.getObjectId() + ": " + var14);
         return new NErrorType(2, 40);
      }
   }

   private NChangeListError addRecipients(PropertyValue propertyValue) {
      BAlarmClass ac = this.getAlarmClass();
      ArrayList<BBacnetDestination> v = new ArrayList<>();
      int ffen = 1;

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               BBacnetDestination d = new BBacnetDestination();
               d.readAsn(asnIn);
               v.add(d);
               ffen++;
            }
         }
      } catch (AsnException var16) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "AsnException occurred in addRecipients", (Throwable)var16);
         }

         return new NChangeListError(8, new NErrorType(2, 9), ffen);
      }

      try {
         Knob[] knobs = ac.getKnobs(BAlarmClass.alarm);
         int len = knobs.length;

         for (int i = 0; i < v.size(); i++) {
            boolean knobFound = false;
            BBacnetDestination dest = v.get(i);

            for (int j = 0; j < len; j++) {
               if (knobs[j].getTargetComponent() instanceof BBacnetDestination && dest.destinationEquals((BBacnetDestination)knobs[j].getTargetComponent())) {
                  knobFound = true;
                  break;
               }
            }

            if (!knobFound) {
               BBacnetDestination linkDest = null;
               BComponent alarmService = this.getAlarmClass().getParent().asComponent();
               BBacnetDestination[] dests = (BBacnetDestination[])alarmService.getChildren(BBacnetDestination.class);

               for (int k = 0; k < dests.length; k++) {
                  if (dest.destinationEquals(dests[k])) {
                     linkDest = dests[k];
                     break;
                  }
               }

               if (linkDest == null) {
                  alarmService.add(null, dest, BLocalBacnetDevice.getBacnetContext());
                  linkDest = dest;
               }

               BLink link = new BLink(this.getAlarmClass().getHandleOrd(), "alarm", "routeAlarm", true);
               linkDest.add(null, link, BLocalBacnetDevice.getBacnetContext());
            }
         }

         return null;
      } catch (PermissionException var14) {
         log.warning("PermissionException adding elements to recipientList in object " + this.getObjectId() + ": " + var14);
         return new NChangeListError(8, new NErrorType(2, 40), 0L);
      }
   }

   private NChangeListError removeRecipients(PropertyValue propertyValue) {
      BAlarmClass ac = this.getAlarmClass();
      ArrayList<BBacnetDestination> v = new ArrayList<>();
      int ffen = 1;

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               BBacnetDestination d = new BBacnetDestination();
               d.readAsn(asnIn);
               v.add(d);
               ffen++;
            }
         }
      } catch (AsnException var15) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "AsnException occurred in removeRecipients", (Throwable)var15);
         }

         return new NChangeListError(9, new NErrorType(2, 9), ffen);
      }

      Knob[] knobs = ac.getKnobs(BAlarmClass.alarm);
      int len = knobs.length;

      for (int var16 = 1; var16 <= v.size(); var16++) {
         BBacnetDestination dest = v.get(var16 - 1);
         boolean found = false;

         for (int j = 0; j < len; j++) {
            if (knobs[j].getTargetComponent() instanceof BBacnetDestination && dest.destinationEquals((BBacnetDestination)knobs[j].getTargetComponent())) {
               found = true;
               break;
            }
         }

         if (!found) {
            return new NChangeListError(9, new NErrorType(5, 81), var16);
         }
      }

      try {
         for (int i = 0; i < v.size(); i++) {
            BBacnetDestination dest = v.get(i);

            for (int jx = 0; jx < len; jx++) {
               if (knobs[jx].getTargetComponent() instanceof BBacnetDestination && dest.destinationEquals((BBacnetDestination)knobs[jx].getTargetComponent())) {
                  BComponent target = knobs[jx].getTargetComponent();
                  BLink[] tgtLinks = target.getLinks(BAlarmRecipient.routeAlarm);

                  for (int k = 0; k < tgtLinks.length; k++) {
                     if (tgtLinks[k].getSourceComponent() == this.getAlarmClass()) {
                        target.remove(tgtLinks[k]);
                        break;
                     }
                  }
                  break;
               }
            }
         }

         return null;
      } catch (PermissionException var13) {
         log.warning("PermissionException removing elements to recipientList in object " + this.getObjectId() + ": " + var13);
         return new NChangeListError(9, new NErrorType(2, 40), 0L);
      }
   }

   @Override
   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      BBacnetExportTable exports = null;
      BLocalBacnetDevice local = null;
      BBacnetNetwork network = null;
      if (!this.fatalFault) {
         for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BBacnetExportTable) {
               exports = (BBacnetExportTable)parent;
            } else if (parent instanceof BLocalBacnetDevice) {
               local = (BLocalBacnetDevice)parent;
               break;
            }
         }

         if (exports == null || local == null) {
            this.fatalFault = true;
            this.setFaultCause("Not under LocalBacnetDevice Export Table");
         } else if (local.isFatalFault()) {
            this.fatalFault = true;
            this.setFaultCause("LocalDevice fault: " + local.getFaultCause());
         } else {
            network = (BBacnetNetwork)local.getParent();
            if (network == null) {
               this.fatalFault = true;
               this.setFaultCause("Not under BacnetNetwork");
            } else if (network.isFatalFault()) {
               this.fatalFault = true;
               this.setFaultCause("Network fault: " + network.getFaultCause());
            } else if (!network.hasServerLicense()) {
               this.fatalFault = true;
               this.setFaultCause("Server capability not licensed");
            } else {
               this.setFaultCause("");
            }
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetNotificationClassDescriptor", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("ac", this.ac);
      out.prop("recipientListChanged", this.recipientListChanged);
      out.trTitle("Recipient List", 2);

      for (int i = 0; i < this.recipientList.length; i++) {
         out.prop("  " + i, this.recipientList[i]);
      }

      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }
}
