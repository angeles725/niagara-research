package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.schedule.ScheduleSupport0;
import com.tridium.bacnet.schedule.ScheduleSupport16;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetBitString;
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
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType(
   agent = {@AgentOn(
      types = {"schedule:CalendarSchedule"}
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
      name = "calendarOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 64,
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"baja:Component\""
      )}
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.CALENDAR)",
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
   )})
public class BBacnetCalendarDescriptor extends BComponent implements BIBacnetExportObject, BacnetPropertyListProvider {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property calendarOrd = newProperty(64, BOrd.DEFAULT, BFacets.make("targetType", "baja:Component"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(6), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetCalendarDescriptor.class);
   private boolean fatalFault = false;
   private static final BIcon icon = BIcon.make(BIcon.std("calendar.png"), BIcon.std("badges/export.png"));
   private BCalendarSchedule calendar;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private static final AsnInputStream asnIn = new AsnInputStream();
   private static final AsnOutputStream asnOut = new AsnOutputStream();
   private static ScheduleSupport0 supp = new ScheduleSupport16();
   private static final Logger log = Logger.getLogger("bacnet.server");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 85, 23};
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

   public BOrd getCalendarOrd() {
      return (BOrd)this.get(calendarOrd);
   }

   public void setCalendarOrd(BOrd v) {
      this.set(calendarOrd, v, null);
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
      local.unsubscribe(this, this.calendar);
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
         } else if (p.equals(calendarOrd)) {
            this.checkConfiguration();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.getName() == "protocolRevision") {
            setSupport(((BInteger)this.get("protocolRevision")).getInt());
         }
      }
   }

   private static void setSupport(int protocolRevision) {
      supp = ScheduleSupport0.makeForProtocolRevision(protocolRevision, supp);
      log.info("Server calendar support (new) is now " + supp.getClass());
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(6) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return this.getCalendar();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getCalendarOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(calendarOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         local.unsubscribe(this, this.calendar);
         this.findCalendar();
         boolean configOk = true;
         if (this.calendar == null) {
            this.setFaultCause("Cannot find exported calendar");
            configOk = false;
         } else {
            local.subscribe(this, this.calendar);
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
         }

         if (configOk) {
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
      this.getCalendar();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getCalendar();
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
      this.getCalendar();
      if (this.calendar == null) {
         return new ReadRangeAck(1, 1000);
      } else {
         int propertyId = rangeReference.getPropertyId();
         if (!hasProperty(propertyId)) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 23) {
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
                  BAbstractSchedule[] dateList = (BAbstractSchedule[])this.calendar.getChildren(BAbstractSchedule.class);
                  int len = dateList.length;
                  if (rangeType == 3) {
                     int refNdx = (int)rangeReference.getReferenceIndex();
                     int count = rangeReference.getCount();
                     if (refNdx <= len && refNdx >= 1) {
                        Array<BAbstractSchedule> a = new Array(BAbstractSchedule.class);
                        int itemsFound = 0;
                        if (count > 0) {
                           for (int i = refNdx - 1; i < len && itemsFound < count; i++) {
                              a.add(dateList[i]);
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
                              a.add(dateList[i]);
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

                        Iterator<BAbstractSchedule> it = a.iterator();
                        int itemCount = 0;
                        synchronized (asnOut) {
                           asnOut.reset();
                           if (maxDataLength > 0) {
                              while (it.hasNext()) {
                                 if (maxDataLength - asnOut.size() < 12) {
                                    rflags[1] = false;
                                    break;
                                 }

                                 supp.encodeCalendarEntry(it.next(), asnOut);
                                 itemCount++;
                              }
                           } else {
                              itemCount = itemsFound;

                              while (it.hasNext()) {
                                 supp.encodeCalendarEntry(it.next(), asnOut);
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
                              supp.encodeCalendarEntry(dateList[i], asnOut);
                              itemCount++;
                              if (maxDataLength - asnOut.size() < 12) {
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
                              supp.encodeCalendarEntry(dateList[ix], asnOut);
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
      this.getCalendar();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      BCalendarSchedule c = this.getCalendar();
      if (c == null) {
         return new NChangeListError(8, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 23) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.addDates(propertyValue);
         }
      }
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      BCalendarSchedule c = this.getCalendar();
      if (c == null) {
         return new NChangeListError(9, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 23) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.removeDates(propertyValue);
         }
      }
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (this.calendar == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && pId != 371) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 23:
               return this.readDateList(ndx);
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 85:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.calendar.getOut().getValue()));
            case 371:
               return this.readPropertyList(ndx);
            default:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (this.calendar == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && pId != 371) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 23:
                  return this.writeDateList(val);
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 75:
               case 79:
               case 85:
               case 371:
                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               default:
                  return new NErrorType(2, 32);
            }
         } catch (AsnException var6) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var6);
            return new NErrorType(2, 9);
         } catch (PermissionException var7) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 40);
         }
      }
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   protected final BCalendarSchedule getCalendar() {
      return this.calendar == null ? this.findCalendar() : this.calendar;
   }

   private BCalendarSchedule findCalendar() {
      try {
         if (!calendarOrd.isEquivalentToDefaultValue(this.getCalendarOrd())) {
            BObject o = this.getCalendarOrd().get(this);
            if (o instanceof BCalendarSchedule) {
               this.calendar = (BCalendarSchedule)o;
            } else {
               this.calendar = null;
            }
         }
      } catch (Exception var2) {
         log.warning("Unable to resolve calendar ord for " + this + ":" + this.getCalendarOrd() + ": " + var2);
         this.calendar = null;
      }

      if (this.calendar == null && this.isRunning()) {
         this.setFaultCause("Cannot find exported calendar");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      return this.calendar;
   }

   private NReadPropertyResult readDateList(int ndx) {
      synchronized (asnOut) {
         asnOut.reset();
         supp.encodeDateList(this.calendar, asnOut);
         return new NReadPropertyResult(23, ndx, asnOut.toByteArray());
      }
   }

   private ErrorType writeDateList(byte[] encodedValue) {
      try {
         synchronized (asnIn) {
            asnIn.setBuffer(encodedValue);
            BCalendarSchedule newCalendar = supp.decodeDateList(asnIn);
            boolean cleanup = this.calendar.getCleanupExpiredEvents();
            newCalendar.setCleanupExpiredEvents(cleanup);
            this.calendar.copyFrom(newCalendar, BLocalBacnetDevice.getBacnetContext());
         }

         return null;
      } catch (OutOfRangeException var7) {
         log.warning("Value out of range writing datelist in object " + this.getObjectId() + ": " + var7);
         return new NErrorType(2, 37);
      } catch (AsnException var8) {
         log.warning("AsnException writing datelist in object " + this.getObjectId() + ": " + var8);
         return new NErrorType(2, 9);
      } catch (PermissionException var9) {
         log.warning("PermissionException writing datelist in object " + this.getObjectId() + ": " + var9);
         return new NErrorType(2, 40);
      } catch (Exception var10) {
         log.warning("Exception writing datelist in object " + this.getObjectId() + ": " + var10);
         return new NErrorType(2, 0);
      }
   }

   private ChangeListError addDates(PropertyValue propertyValue) {
      int ffen = 1;
      ArrayList<BAbstractSchedule> v = new ArrayList<>();

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (; asnIn.peekTag() != -1; ffen++) {
               BAbstractSchedule ce = supp.decodeCalendarEntry(asnIn);
               if (ce != null) {
                  v.add(ce);
               }
            }
         }
      } catch (AsnException var11) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "AsnException occurred in addDates in object " + this.getObjectId(), (Throwable)var11);
         }

         return new NChangeListError(8, new NErrorType(2, 9), ffen);
      }

      try {
         for (BAbstractSchedule ce : v) {
            SlotCursor<Property> sc = this.calendar.getProperties();
            boolean alreadyHere = false;

            while (sc.next(BAbstractSchedule.class)) {
               if (ce.equivalent(sc.get())) {
                  alreadyHere = true;
                  break;
               }
            }

            if (!alreadyHere) {
               this.calendar.add(null, ce, BLocalBacnetDevice.getBacnetContext());
            }
         }

         return null;
      } catch (PermissionException var8) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("PermissionException adding elements to datelist in object " + this.getObjectId() + ": " + var8);
         }

         return new NChangeListError(8, new NErrorType(2, 40), 0L);
      } catch (Exception var9) {
         log.warning("Exception adding elements to datelist in object " + this.getObjectId() + ": " + var9);
         return new NChangeListError(8, new NErrorType(2, 0), ffen);
      }
   }

   private ChangeListError removeDates(PropertyValue propertyValue) {
      int ffen = 1;
      ArrayList<BAbstractSchedule> v = new ArrayList<>();

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(propertyValue.getPropertyValue());

            for (; asnIn.peekTag() != -1; ffen++) {
               BAbstractSchedule ce = supp.decodeCalendarEntry(asnIn);
               if (ce != null) {
                  v.add(ce);
               }
            }
         }
      } catch (AsnException var11) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "AsnException occurred in removeDates in object " + this.getObjectId(), (Throwable)var11);
         }

         return new NChangeListError(9, new NErrorType(2, 9), ffen);
      }

      try {
         BAbstractSchedule[] a = this.calendar.getSchedules();

         for (int var12 = 1; var12 <= v.size(); var12++) {
            BAbstractSchedule ce = v.get(var12 - 1);
            boolean found = false;

            for (int i = 0; i < a.length; i++) {
               if (ce.equivalent(a[i])) {
                  found = true;
                  break;
               }
            }

            if (!found) {
               return new NChangeListError(9, new NErrorType(5, 81), var12);
            }
         }

         for (int var13 = 0; var13 < v.size(); var13++) {
            BAbstractSchedule ce = v.get(var13);

            for (int ix = 0; ix < a.length; ix++) {
               if (ce.equivalent(a[ix])) {
                  this.calendar.remove(a[ix]);
                  break;
               }
            }
         }

         return null;
      } catch (PermissionException var8) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("PermissionException removing elements from datelist in object " + this.getObjectId() + ": " + var8);
         }

         return new NChangeListError(9, new NErrorType(2, 40), 0L);
      } catch (Exception var9) {
         log.warning("Exception removing elements from datelist in object " + this.getObjectId() + ": " + var9);
         return new NChangeListError(9, new NErrorType(2, 0), 0L);
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
      out.trTitle("BacnetCalendarDescriptor", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("calendar", this.calendar);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.prop("supp", supp);
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }
}
