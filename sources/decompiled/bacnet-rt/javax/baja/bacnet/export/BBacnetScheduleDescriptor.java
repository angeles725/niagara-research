package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.schedule.ScheduleSupport0;
import com.tridium.bacnet.schedule.ScheduleSupport16;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.confirmed.CreateObjectAck;
import com.tridium.bacnet.services.confirmed.CreateObjectRequest;
import com.tridium.bacnet.services.confirmed.DeleteObjectAck;
import com.tridium.bacnet.services.confirmed.DeleteObjectRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.CreateObjectError;
import com.tridium.bacnet.services.error.DeleteObjectError;
import com.tridium.bacnet.services.error.NChangeListError;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import com.tridium.bacnet.stack.server.object.BObjectHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnDataTypeNotSupportedException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.DataTypeNotSupportedException;
import javax.baja.bacnet.io.DuplicateEntryException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.bacnet.util.SpecialEventDetails;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.schedule.BCompositeSchedule;
import javax.baja.schedule.BDailySchedule;
import javax.baja.schedule.BDaySchedule;
import javax.baja.schedule.BScheduleReference;
import javax.baja.schedule.BTimeSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.BWeekday;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
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
      name = "scheduleOrd",
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.SCHEDULE)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "listOfObjectPropertyReferences",
      type = "BBacnetListOf",
      defaultValue = "new BBacnetListOf(BBacnetDeviceObjectPropertyReference.TYPE)"
   ), @NiagaraProperty(
      name = "priorityForWriting",
      type = "int",
      defaultValue = "16",
      facets = {@Facet("BFacets.makeInt(1, 16)")}
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "reliability",
      type = "BBacnetReliability",
      defaultValue = "BBacnetReliability.noFaultDetected",
      flags = 3
   )})
@NiagaraAction(
   name = "writePresentValue",
   flags = 20
)
public abstract class BBacnetScheduleDescriptor extends BComponent implements BIBacnetExportObject, BacnetPropertyListProvider {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property scheduleOrd = newProperty(64, BOrd.DEFAULT, BFacets.make("targetType", "baja:Component"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(17), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property listOfObjectPropertyReferences = newProperty(0, new BBacnetListOf(BBacnetDeviceObjectPropertyReference.TYPE), null);
   public static final Property priorityForWriting = newProperty(0, 16, BFacets.makeInt(1, 16));
   public static final Property description = newProperty(0, "", null);
   public static final Property reliability = newProperty(3, BBacnetReliability.noFaultDetected, null);
   public static final Action writePresentValue = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetScheduleDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("schedule.png"), BIcon.std("badges/export.png"));
   private boolean fatalFault = false;
   private BWeeklySchedule schedule;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private boolean configOk;
   private boolean isValid;
   static final AsnInputStream asnIn = new AsnInputStream();
   static final AsnOutputStream asnOut = new AsnOutputStream();
   private static ScheduleSupport0 supp = new ScheduleSupport16();
   static Logger log = Logger.getLogger("bacnet.server");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 85, 32, 174, 54, 88, 111, 103, 81};
   private static final int[] OPTIONAL_PROPS = new int[]{28, 123, 38};
   private static final Comparator<Object> specialEventBnIdxComparator = new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
         if (o1 == null || o2 == null) {
            throw new NullPointerException();
         } else if (o1 instanceof BDailySchedule && o2 instanceof BDailySchedule) {
            BDailySchedule ds1 = (BDailySchedule)o1;
            BDailySchedule ds2 = (BDailySchedule)o2;
            BInteger bnIdx1 = (BInteger)ds1.get("bacnetIdx");
            if (bnIdx1 == null) {
               bnIdx1 = BBacnetScheduleDescriptor.NO_BN_IDX;
            }

            BInteger bnIdx2 = (BInteger)ds2.get("bacnetIdx");
            if (bnIdx2 == null) {
               bnIdx2 = BBacnetScheduleDescriptor.NO_BN_IDX;
            }

            return bnIdx1.getInt() - bnIdx2.getInt();
         } else {
            throw new ClassCastException("Cannot compare " + o1.getClass() + " and " + o2.getClass());
         }
      }
   };
   private static final BInteger NO_BN_IDX = BInteger.make(-1);
   protected static final String LAST_EFFECTIVE_VALUE = "leValue";

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

   public BOrd getScheduleOrd() {
      return (BOrd)this.get(scheduleOrd);
   }

   public void setScheduleOrd(BOrd v) {
      this.set(scheduleOrd, v, null);
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

   public BBacnetListOf getListOfObjectPropertyReferences() {
      return (BBacnetListOf)this.get(listOfObjectPropertyReferences);
   }

   public void setListOfObjectPropertyReferences(BBacnetListOf v) {
      this.set(listOfObjectPropertyReferences, v, null);
   }

   public int getPriorityForWriting() {
      return this.getInt(priorityForWriting);
   }

   public void setPriorityForWriting(int v) {
      this.setInt(priorityForWriting, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   public BBacnetReliability getReliability() {
      return (BBacnetReliability)this.get(reliability);
   }

   public void setReliability(BBacnetReliability v) {
      this.set(reliability, v, null);
   }

   public void writePresentValue() {
      this.invoke(writePresentValue, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.checkFatalFault();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.checkConfiguration();
      this.validateReferences();
      if (Sys.isStationStarted() && this.configOk()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unsubscribe(this, this.schedule);
      local.unexport(this.oldId, this.oldName, this);
      this.schedule = null;
      this.oldId = null;
      this.oldName = null;
      if (this.configOk() && local.isRunning()) {
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

            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BBacnetNetwork.localDevice().unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(scheduleOrd)) {
            this.checkConfiguration();
            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(priorityForWriting)) {
            BBacnetNetwork.bacnet().postAsync(new Runnable() {
               @Override
               public void run() {
                  BBacnetScheduleDescriptor.this.writePresentValue();
               }
            });
         } else if (p.equals(listOfObjectPropertyReferences)) {
            BBacnetNetwork.bacnet().postAsync(new Runnable() {
               @Override
               public void run() {
                  BBacnetScheduleDescriptor.this.resolveTargetReferences();

                  try {
                     Thread.sleep(5000L);
                  } catch (InterruptedException var2) {
                  }

                  BBacnetScheduleDescriptor.this.writePresentValue();
               }
            });
         } else if (p.getName().equals("protocolRevision")) {
            setSupport(((BInteger)this.get("protocolRevision")).getInt());
         } else {
            super.changed(p, cx);
         }
      }
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      this.resolveTargetReferences();
   }

   private static void setSupport(int protocolRevision) {
      supp = ScheduleSupport0.makeForProtocolRevision(protocolRevision, supp);
      if (log.isLoggable(Level.FINE)) {
         log.fine("Server schedule support (new) is now " + supp.getClass());
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s == objectId ? BBacnetObjectType.getObjectIdFacets(17) : super.getSlotFacets(s);
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      if (action.equals(writePresentValue)) {
         this.resolveTargetReferences();
         return BBacnetNetwork.bacnet().postAsync(new Invocation(this, action, arg, cx));
      } else {
         return super.post(action, arg, cx);
      }
   }

   public abstract void doWritePresentValue();

   protected void findOrAddRemoteDeviceAndPoint(BBacnetDeviceObjectPropertyReference ref) {
      if (BacnetDescriptorUtil.isValid(ref)) {
         try {
            BacnetDescriptorUtil.findOrAddRemotePoint(ref);
         } catch (Exception var3) {
            log.log(
               Level.WARNING,
               this + ": Exception finding/adding remote device/point; reference: " + ref + "; exception: " + var3,
               (Throwable)(log.isLoggable(Level.FINE) ? var3 : null)
            );
         }
      } else if (log.isLoggable(Level.FINE)) {
         log.fine(this + ": object property reference not valid: " + ref);
      }
   }

   @Override
   public final BObject getObject() {
      return this.getSchedule();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getScheduleOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(scheduleOrd, objectOrd, cx);
   }

   @Override
   public synchronized void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         this.configOk = false;
      } else {
         local.unsubscribe(this, this.schedule);
         this.findSchedule();
         boolean cfgOk = true;
         if (this.schedule == null) {
            this.setFaultCause("Cannot find exported schedule");
            cfgOk = false;
         } else {
            local.subscribe(this, this.schedule);
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            cfgOk = false;
         }

         cfgOk &= this.checkScheduleConfiguration();
         if (cfgOk) {
            String err = local.export(this);
            if (err != null) {
               this.duplicate = true;
               this.setFaultCause(err);
               cfgOk = false;
            } else {
               this.duplicate = false;
            }
         }

         this.configOk = cfgOk;
         if (cfgOk) {
            this.setFaultCause("");
            this.validate();
         } else {
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
         }
      }
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      this.getSchedule();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getSchedule();
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
      this.getSchedule();
      if (this.schedule == null) {
         return new ReadRangeAck(1, 1000);
      } else {
         int propertyId = rangeReference.getPropertyId();
         if (!hasProperty(propertyId)) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 54) {
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
                  BBacnetDeviceObjectPropertyReference[] list = (BBacnetDeviceObjectPropertyReference[])this.getListOfObjectPropertyReferences()
                     .getChildren(BBacnetDeviceObjectPropertyReference.class);
                  int len = list.length;
                  if (rangeType == 3) {
                     int refNdx = (int)rangeReference.getReferenceIndex();
                     int count = rangeReference.getCount();
                     if (refNdx <= len && refNdx >= 1) {
                        Array<BBacnetDeviceObjectPropertyReference> a = new Array(BBacnetDeviceObjectPropertyReference.class);
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

                        Iterator<BBacnetDeviceObjectPropertyReference> it = a.iterator();
                        int itemCount = 0;
                        synchronized (asnOut) {
                           asnOut.reset();
                           if (maxDataLength > 0) {
                              while (it.hasNext()) {
                                 if (maxDataLength - asnOut.size() < 16) {
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
                              if (maxDataLength - asnOut.size() < 16) {
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
      this.getSchedule();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      if (this.getSchedule() == null) {
         return new NChangeListError(8, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 54) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.addScheduleTargets(propertyValue.getPropertyValue());
         }
      }
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      if (this.getSchedule() == null) {
         return new NChangeListError(9, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 54) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : this.removeScheduleTargets(propertyValue.getPropertyValue());
         }
      }
   }

   boolean isArray(int propertyId) {
      if (propertyId == 123) {
         return true;
      } else {
         return propertyId == 38 ? true : propertyId == 371;
      }
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (this.schedule == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         switch (pId) {
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 32:
               synchronized (asnOut) {
                  asnOut.reset();
                  supp.encodeDateRange(this.schedule.getEffective(), asnOut);
                  return new NReadPropertyResult(pId, ndx, asnOut.toByteArray());
               }
            case 38:
               return this.readExceptionSchedule(ndx);
            case 54:
               return this.readListOfObjectPropertyReferences(ndx);
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 81:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(((BStatusValue)this.schedule.get("out")).getStatus().isDisabled()));
            case 88:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getPriorityForWriting()));
            case 103:
               int rel = this.getReliability().getOrdinal();
               if (((BStatusValue)this.schedule.get("out")).getStatus().isFault()) {
                  rel = 10;
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(rel));
            case 111:
               return new NReadPropertyResult(pId, ndx, AsnUtil.statusToAsnStatusFlags(((BStatusValue)this.schedule.get("out")).getStatus()));
            case 123:
               return this.readWeeklySchedule(ndx);
            case 371:
               return this.readPropertyList(ndx);
            default:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (this.schedule == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            synchronized (asnIn) {
               asnIn.setBuffer(val);
               switch (pId) {
                  case 28:
                     this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  case 32:
                     this.schedule.set(BWeeklySchedule.effective, supp.decodeDateRange(asnIn), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  case 38:
                     return this.writeExceptionSchedule(ndx);
                  case 54:
                     return this.writeListOfObjectPropertyReferences(pId, ndx, val, pri);
                  case 75:
                  case 79:
                  case 371:
                     return new NErrorType(2, 40);
                  case 77:
                     return BacUtil.setObjectName(this, objectName, val);
                  case 81:
                     Property pIn = this.schedule.loadSlots().getProperty("in");
                     BLink[] inLinks = this.schedule.getLinks(pIn);
                     if (inLinks.length > 0) {
                        return new NErrorType(2, 40);
                     }

                     boolean outOfService = asnIn.readBoolean();
                     BStatusValue svi = (BStatusValue)this.schedule.get(pIn).newCopy();
                     Property pOut = this.schedule.loadSlots().getProperty("out");
                     BStatusValue svo = (BStatusValue)this.schedule.get(pOut).newCopy();
                     svi.setStatusNull(!outOfService);
                     svi.setStatusDisabled(outOfService);
                     svi.setValueValue(svo.getValueValue());
                     this.schedule.set(pIn, svi, BLocalBacnetDevice.getBacnetContext());
                     return null;
                  case 88:
                     int pfw = asnIn.readUnsignedInt();
                     if (pfw >= 1 && pfw <= 16) {
                        this.setInt(priorityForWriting, pfw, BLocalBacnetDevice.getBacnetContext());
                        return null;
                     }

                     return new NErrorType(2, 37);
                  case 103:
                     return new NErrorType(2, 40);
                  case 111:
                     return new NErrorType(2, 40);
                  case 123:
                     return this.writeWeeklySchedule(ndx);
                  case 174:
                     return this.writeScheduleDefaultValue(val);
                  default:
                     return new NErrorType(2, 32);
               }
            }
         } catch (OutOfRangeException var15) {
            log.warning("Value out of range exception writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
            return new NErrorType(2, 37);
         } catch (AsnDataTypeNotSupportedException var16) {
            log.warning(bacnetLexicon.getText("BacnetSchedule.typechange.warning", new Object[]{this.getAsnType(), var16.getAsnType()}));
            return new NErrorType(2, 7);
         } catch (DataTypeNotSupportedException var17) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var17);
            return new NErrorType(2, 47);
         } catch (AsnException var18) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var18);
            return new NErrorType(2, 9);
         } catch (PermissionException var19) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var19);
            return new NErrorType(2, 40);
         }
      }
   }

   protected boolean checkScheduleConfiguration() {
      return true;
   }

   void checkValid() {
      if (this.configOk()) {
         this.validate();
      }
   }

   protected void validate() {
      BWeeklySchedule ws = this.getSchedule();

      for (int i = 0; i < 7; i++) {
         BDaySchedule ds = ws.get(BWeekday.make(i));
         BTimeSchedule[] times = ds.getTimesInOrder();

         for (int j = 0; j < times.length; j++) {
            if (times[j].getEffectiveValue().getStatus().isNull()) {
               this.markConfigurationError("Exported schedule value cannot be null");
               return;
            }
         }
      }

      this.markNoFaultDetected();
   }

   protected ErrorType writeScheduleDefaultValue(byte[] val) {
      BWeeklySchedule sched = this.getSchedule();
      if (sched == null) {
         return new NErrorType(1, 1000);
      } else {
         synchronized (asnIn) {
            asnIn.setBuffer(val);

            ErrorType var10000;
            try {
               int applicationTag = asnIn.peekApplicationTag();
               if (applicationTag == 0) {
                  sched.getDefaultOutput()
                     .set(BStatusValue.status, BStatus.make(sched.getDefaultOutput().getStatus(), 64, true), BLocalBacnetDevice.getBacnetContext());
                  return null;
               }

               if (!this.isEqual(applicationTag, this.getAsnType())) {
                  return new NErrorType(2, 7);
               }

               var10000 = this.doWriteScheduleDefaultValue(asnIn, applicationTag);
            } catch (OutOfRangeException var6) {
               log.warning("Value out of range writing property 174 in object " + this.getObjectId() + ": " + var6);
               return new NErrorType(2, 37);
            } catch (DataTypeNotSupportedException var7) {
               log.warning("Datatype not supported writing property 174 in object " + this.getObjectId() + ": " + var7);
               return new NErrorType(2, 47);
            } catch (AsnException var8) {
               log.warning("AsnException writing property 174 in object " + this.getObjectId() + ": " + var8);
               return new NErrorType(2, 9);
            } catch (PermissionException var9) {
               log.warning("PermissionException writing property 174 in object " + this.getObjectId() + ": " + var9);
               return new NErrorType(2, 40);
            } catch (Exception var10) {
               log.warning("Exception writing property 174 in object " + this.getObjectId() + ": " + var10);
               return new NErrorType(2, 0);
            }

            return var10000;
         }
      }
   }

   protected abstract ErrorType doWriteScheduleDefaultValue(AsnInputStream var1, int var2) throws Exception;

   synchronized boolean configOk() {
      return this.configOk;
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   protected final BWeeklySchedule getSchedule() {
      return this.schedule == null ? this.findSchedule() : this.schedule;
   }

   public void markConfigurationError(String faultCause) {
      this.setReliability(BBacnetReliability.configurationError);
      this.setFaultCause(faultCause);
      this.setStatus(BStatus.makeFault(this.getStatus(), true));
   }

   public void markNoFaultDetected() {
      this.setReliability(BBacnetReliability.noFaultDetected);
      this.setFaultCause("");
      this.setStatus(BStatus.ok);
   }

   public BStatusValue getLastEffectiveValue() {
      return (BStatusValue)this.get("leValue");
   }

   public void setLastEffectiveValue(BStatusValue o) {
      if (this.get("leValue") == null) {
         this.add("leValue", o, 260);
      } else {
         this.set("leValue", o);
      }
   }

   private BWeeklySchedule findSchedule() {
      try {
         if (!scheduleOrd.isEquivalentToDefaultValue(this.getScheduleOrd())) {
            BObject o = this.getScheduleOrd().get(this);
            if (o instanceof BWeeklySchedule) {
               this.schedule = (BWeeklySchedule)o;
            } else {
               this.schedule = null;
            }
         }

         if (!this.isScheduleTypeLegal(this.schedule)) {
            this.schedule = null;
         }
      } catch (Exception var2) {
         log.warning("Unable to resolve schedule ord for " + this + ": " + this.getScheduleOrd() + ": " + var2);
         this.schedule = null;
      }

      if (this.schedule == null && this.isRunning()) {
         this.setFaultCause("Cannot find exported schedule");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      return this.schedule;
   }

   boolean isScheduleTypeLegal(BWeeklySchedule sched) {
      return true;
   }

   abstract int getAsnType();

   abstract Property getScheduleOutputProperty();

   abstract BStatusValue getEffectiveValueFrom(BStatusValue var1);

   private PropertyValue readWeeklySchedule(int ndx) {
      synchronized (asnOut) {
         asnOut.reset();
         switch (ndx) {
            case -1:
               for (int i = 1; i <= 7; i++) {
                  supp.encodeDailySchedule(this.schedule.get(BWeekday.make(i % 7)), this.schedule.getDefaultOutput(), asnOut, this.getAsnType());
               }

               return new NReadPropertyResult(123, ndx, asnOut.toByteArray());
            case 0:
               return new NReadPropertyResult(123, ndx, AsnUtil.toAsnUnsigned(7L));
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
               supp.encodeDailySchedule(this.schedule.get(BWeekday.make(ndx % 7)), this.schedule.getDefaultOutput(), asnOut, this.getAsnType());
               return new NReadPropertyResult(123, ndx, asnOut.toByteArray());
            default:
               return new NReadPropertyResult(123, ndx, new NErrorType(2, 42));
         }
      }
   }

   private PropertyValue readExceptionSchedule(int ndx) {
      try {
         synchronized (asnOut) {
            asnOut.reset();
            switch (ndx) {
               case -1:
                  supp.encodeExceptionScheduleWithIdx(
                     this.schedule.getSpecialEvents(), this.schedule.getDefaultOutput(), asnOut, this.getAsnType(), BBacnetNetwork.localDevice().getObjectId()
                  );
                  return new NReadPropertyResult(38, ndx, asnOut.toByteArray());
               case 0:
                  SlotCursor<Property> c = this.schedule.getSpecialEvents().getProperties();
                  int cnt = 0;

                  while (c.next(BDailySchedule.class)) {
                     cnt++;
                  }

                  return new NReadPropertyResult(38, ndx, AsnUtil.toAsnUnsigned(cnt));
               default:
                  if (ndx < 0) {
                     return new NReadPropertyResult(38, ndx, new NErrorType(2, 42));
                  } else {
                     supp.encodeSpecialEvent(
                        ndx,
                        this.schedule.getSpecialEvents(),
                        this.schedule.getDefaultOutput(),
                        asnOut,
                        this.getAsnType(),
                        BBacnetNetwork.localDevice().getObjectId()
                     );
                     return new NReadPropertyResult(38, ndx, asnOut.toByteArray());
                  }
            }
         }
      } catch (Exception var7) {
         return new NReadPropertyResult(38, ndx, new NErrorType(2, 42));
      }
   }

   private ErrorType writeWeeklySchedule(int ndx) throws BacnetException {
      try {
         synchronized (asnIn) {
            switch (ndx) {
               case -1:
                  BDaySchedule[] schedules = new BDaySchedule[7];

                  for (int i = 1; i <= 7; i++) {
                     schedules[i - 1] = supp.decodeDailySchedule(this.schedule.getDefaultOutput(), asnIn, this.getAsnType());
                  }

                  if (asnIn.peekTag() != -1) {
                     log.warning(this.getObjectId() + ": Did not read end-of-data after decoding 7 days of daily schedules");
                     return new NErrorType(2, 42);
                  } else {
                     Context context = BLocalBacnetDevice.getBacnetContext();

                     for (int i = 1; i <= 7; i++) {
                        this.getDailySchedule(i).set(BDailySchedule.day, schedules[i - 1], context);
                     }

                     return null;
                  }
               case 0:
                  return new NErrorType(2, 37);
               case 1:
               case 2:
               case 3:
               case 4:
               case 5:
               case 6:
               case 7:
                  BDaySchedule daySchedule = supp.decodeDailySchedule(this.schedule.getDefaultOutput(), asnIn, this.getAsnType());
                  this.getDailySchedule(ndx).set(BDailySchedule.day, daySchedule, BLocalBacnetDevice.getBacnetContext());
                  return null;
               default:
                  return new NErrorType(2, 42);
            }
         }
      } catch (PermissionException var8) {
         log.warning("PermissionException writing weeklySchedule in object " + this.getObjectId() + ": " + var8);
         return new NErrorType(2, 40);
      } catch (DuplicateEntryException var9) {
         log.warning("DuplicateEntryException writing weeklySchedule in object " + this.getObjectId() + ": " + var9);
         return new NErrorType(2, 137);
      }
   }

   private BDailySchedule getDailySchedule(int i) {
      String weekday = BWeekday.make(i % 7).getTag();
      return (BDailySchedule)this.schedule.getWeek().get(weekday);
   }

   private ErrorType writeExceptionSchedule(int ndx) throws BacnetException {
      try {
         synchronized (asnIn) {
            switch (ndx) {
               case -1:
                  BCompositeSchedule newSchedule = supp.decodeExceptionSchedule(
                     this.schedule.getDefaultOutput(), asnIn, BBacnetNetwork.localDevice().getObjectId(), this.getAsnType()
                  );
                  this.switchOrds((BDailySchedule[])newSchedule.getChildren(BDailySchedule.class));
                  Property specialEvents = this.schedule.getSchedule().loadSlots().getProperty("specialEvents");
                  this.schedule.getSchedule().set(specialEvents, newSchedule, BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 0:
                  writeExceptionSchSize(this.schedule.getSpecialEvents(), asnIn.readInteger());
                  return null;
               default:
                  if (ndx < 1) {
                     return new NErrorType(2, 42);
                  } else {
                     BDailySchedule[] se = (BDailySchedule[])this.schedule.getSpecialEvents().getChildren(BDailySchedule.class);
                     if (ndx > se.length) {
                        return new NErrorType(2, 42);
                     } else {
                        BDailySchedule specialEvent = supp.decodeSpecialEvent(
                           this.schedule.getDefaultOutput(), asnIn, BBacnetNetwork.localDevice().getObjectId(), this.getAsnType(), ndx
                        );
                        this.switchOrds(new BDailySchedule[]{specialEvent});
                        se[ndx - 1].copyFrom(specialEvent, BLocalBacnetDevice.getBacnetContext());
                        return null;
                     }
                  }
            }
         }
      } catch (PermissionException var9) {
         log.warning("PermissionException writing exceptionSchedule in object " + this.getObjectId() + ": " + var9);
         return new NErrorType(2, 40);
      } catch (DuplicateEntryException var10) {
         log.warning("DuplicateEntryException writing exceptionSchedule in object " + this.getObjectId() + ": " + var10);
         return new NErrorType(2, 137);
      }
   }

   private static void writeExceptionSchSize(BCompositeSchedule specialEvents, int newSize) {
      if (specialEvents != null) {
         BDailySchedule[] se = (BDailySchedule[])specialEvents.getChildren(BDailySchedule.class);
         if (se != null) {
            Arrays.sort(se, specialEventBnIdxComparator);
            int sizeChange = se.length - newSize;
            if (sizeChange > 0) {
               for (int i = 0; i < sizeChange; i++) {
                  specialEvents.remove(se[se.length - 1 - i]);
               }
            } else {
               for (int i = 0; i < -sizeChange; i++) {
                  BDailySchedule newSch = new BDailySchedule();
                  newSch.add("bacnetIdx", BInteger.make(se.length + i));
                  specialEvents.add(null, newSch);
               }
            }
         }
      }
   }

   private void switchOrds(BDailySchedule[] specialEvents) {
      if (specialEvents != null) {
         for (int i = 0; i < specialEvents.length; i++) {
            BDailySchedule specialEvent = specialEvents[i];
            if (specialEvent.getDays() instanceof BScheduleReference) {
               BOrd ref = ((BScheduleReference)specialEvent.getDays()).getRef();
               BComponent c = ref.get(this).asComponent();
               if (c instanceof BBacnetCalendarDescriptor) {
                  BCalendarSchedule cal = (BCalendarSchedule)((BBacnetCalendarDescriptor)c).getObject();
                  ((BScheduleReference)specialEvent.getDays()).setRef(cal.getSlotPathOrd());
               }
            }
         }
      }
   }

   private PropertyValue readListOfObjectPropertyReferences(int ndx) {
      synchronized (asnOut) {
         asnOut.reset();
         this.getListOfObjectPropertyReferences().writeAsn(asnOut);
         return new NReadPropertyResult(54, ndx, asnOut.toByteArray());
      }
   }

   private ErrorType writeListOfObjectPropertyReferences(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      List<BBacnetDeviceObjectPropertyReference> objectPropertyReferenceList = Collections.synchronizedList(new ArrayList<>());
      int firstAsnType = -1;

      try {
         synchronized (asnIn) {
            int tag = asnIn.peekTag();

            for (int currentScheduleAsnType = this.getAsnType(); tag != -1; tag = asnIn.peekTag()) {
               BBacnetDeviceObjectPropertyReference deviceObjectPropertyReference = new BBacnetDeviceObjectPropertyReference();
               deviceObjectPropertyReference.readAsn(asnIn);
               if (deviceObjectPropertyReference.isDeviceIdUsed()
                  && !deviceObjectPropertyReference.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId())
                  && deviceObjectPropertyReference.getDeviceId().getObjectType() != 8) {
                  return new NErrorType(2, 37);
               }

               PropertyInfo propertyInfo = getPropertyInfo(deviceObjectPropertyReference);
               if (firstAsnType == -1) {
                  firstAsnType = propertyInfo.getAsnType();
               }

               boolean hasError = Boolean.TRUE;
               if (propertyInfo != null) {
                  if (this.isEqual(propertyInfo.getAsnType(), currentScheduleAsnType)) {
                     objectPropertyReferenceList.add(deviceObjectPropertyReference);
                     hasError = Boolean.FALSE;
                  } else if (this.isEqual(propertyInfo.getAsnType(), firstAsnType)) {
                     hasError = Boolean.FALSE;
                     log.warning(this.getLexicon().getText("BacnetSchedule.typechange.warning", new Object[]{currentScheduleAsnType, firstAsnType}));
                  }
               }

               if (hasError) {
                  return new NErrorType(2, 47);
               }
            }
         }
      } catch (AsnException var15) {
         return new NErrorType(2, 9);
      }

      return (ErrorType)(this.isEqual(firstAsnType, this.getAsnType()) ? this.updateCurrentBacnetSchedule(objectPropertyReferenceList) : new NErrorType(2, 7));
   }

   private ErrorType updateCurrentBacnetSchedule(List<BBacnetDeviceObjectPropertyReference> objectPropertyReferences) {
      BBacnetListOf listOf = this.getListOfObjectPropertyReferences();
      listOf.removeAll();

      try {
         BLocalBacnetDevice local = BBacnetNetwork.localDevice();

         for (BBacnetDeviceObjectPropertyReference r : objectPropertyReferences) {
            if (!r.isDeviceIdUsed() || r.getDeviceId().equals(local.getObjectId())) {
               BIBacnetExportObject o = local.lookupBacnetObject(r.getObjectId());
               if (o == null) {
                  return new NErrorType(2, 37);
               }

               BObject obj = o.getObject();
               if (obj == null) {
                  return new NErrorType(2, 1000);
               }
            }

            if (BacnetDescriptorUtil.isValid(r)) {
               BacnetDescriptorUtil.findOrAddPoint(r);
            }

            listOf.addListElement(r, BLocalBacnetDevice.getBacnetContext());
         }

         return null;
      } catch (PermissionException var8) {
         return new NErrorType(2, 40);
      } catch (Exception var9) {
         return new NErrorType(2, 0);
      }
   }

   private ErrorType replaceCurrentBacnetSchedule(int pId, int ndx, byte[] val, int pri) {
      Array<PropertyValue> initialValues = new Array(PropertyValue.class);
      initialValues.add(new NBacnetPropertyValue(77, AsnUtil.toAsnCharacterString(this.getObjectName())));
      initialValues.add(new NBacnetPropertyValue(28, AsnUtil.toAsnCharacterString(this.getDescription())));
      initialValues.add(new NBacnetPropertyValue(88, AsnUtil.toAsnUnsigned(this.getPriorityForWriting())));
      initialValues.add(new NBacnetPropertyValue(pId, ndx, val, pri));
      Map<Integer, List<BTimeSchedule>> timeScheduleMap = null;
      Map<String, SpecialEventDetails> specialEventMap = null;
      if (this.getSchedule() != null) {
         asnOut.reset();
         supp.encodeDateRange(this.getSchedule().getEffective(), asnOut);
         initialValues.add(new NBacnetPropertyValue(32, asnOut.toByteArray()));
         byte[] outOfServiceFlag = AsnUtil.toAsnBoolean(((BStatusValue)this.schedule.get("out")).getStatus().isDisabled());

         try {
            BWeeklySchedule weeklySchedule = this.getSchedule();
            timeScheduleMap = getTimeScheduleMap(weeklySchedule);
            specialEventMap = getSpecialEventMap(weeklySchedule);
         } catch (Exception var10) {
            log.warning("Failed to copy schedule time lines: " + var10);
            return new NErrorType(2, 56);
         }

         initialValues.add(new NBacnetPropertyValue(81, outOfServiceFlag));
      }

      BBacnetScheduleDescriptor descriptor = this.replaceCurrentBacnetSchedule(initialValues);
      if (descriptor != null) {
         BWeeklySchedule schedule = descriptor.getSchedule();
         if (schedule != null) {
            updateDailySchedule(timeScheduleMap, descriptor);
            updateSpecialEvents(specialEventMap, descriptor);
         }

         return null;
      } else {
         return new NErrorType(2, 56);
      }
   }

   private static void updateSpecialEvents(Map<String, SpecialEventDetails> specialEventMap, BBacnetScheduleDescriptor descriptor) {
      BWeeklySchedule schedule = descriptor.getSchedule();
      BCompositeSchedule specialEvents = schedule.getSpecialEvents();

      for (SpecialEventDetails details : specialEventMap.values()) {
         List<BTimeSchedule> timeSchedules = details.getTimeSchedules();
         BDailySchedule dailySchedule = new BDailySchedule();

         for (BTimeSchedule timeSchedule : timeSchedules) {
            timeSchedule.setEffectiveValue(descriptor.getEffectiveValueFrom(timeSchedule.getEffectiveValue()));
            dailySchedule.getDay().add(timeSchedule);
         }

         dailySchedule.setDays(details.getDaysSchedule());
         specialEvents.add(dailySchedule.getName(), dailySchedule);
      }
   }

   private static void updateDailySchedule(Map<Integer, List<BTimeSchedule>> timeScheduleMap, BBacnetScheduleDescriptor descriptor) {
      BWeeklySchedule schedule = descriptor.getSchedule();

      for (int i = 1; i <= 7; i++) {
         BDaySchedule daySchedule = schedule.get(BWeekday.make((i - 1) % 7));
         List<BTimeSchedule> list = timeScheduleMap.get((i - 1) % 7);
         if (list != null) {
            for (BTimeSchedule timeSchedule : list) {
               timeSchedule.setEffectiveValue(descriptor.getEffectiveValueFrom(timeSchedule.getEffectiveValue()));
               daySchedule.add(timeSchedule);
            }
         }
      }
   }

   private static Map<String, SpecialEventDetails> getSpecialEventMap(BWeeklySchedule weeklySchedule) {
      Map<String, SpecialEventDetails> specialEventMap = new LinkedHashMap<>();
      BCompositeSchedule specialEvents = weeklySchedule.getSpecialEvents();
      SlotCursor<Property> slotCursor = specialEvents.getProperties();

      while (slotCursor.next(BDailySchedule.class)) {
         List<BTimeSchedule> specialEventList = new LinkedList<>();
         BDailySchedule dailySchedule = (BDailySchedule)slotCursor.get();
         BDaySchedule daySchedule = dailySchedule.getDay();
         BTimeSchedule[] timeSchedules = daySchedule.getTimesInOrder();

         for (BTimeSchedule timeSchedule : timeSchedules) {
            specialEventList.add((BTimeSchedule)timeSchedule.newCopy(true));
         }

         BAbstractSchedule abstractSchedule = dailySchedule.getDays();
         SpecialEventDetails specialEventDetails = new SpecialEventDetails((BAbstractSchedule)abstractSchedule.newCopy(true), specialEventList);
         specialEventMap.put(dailySchedule.getName(), specialEventDetails);
         abstractSchedule.newCopy(true);
      }

      return specialEventMap;
   }

   private static Map<Integer, List<BTimeSchedule>> getTimeScheduleMap(BWeeklySchedule weeklySchedule) {
      Map<Integer, List<BTimeSchedule>> timeScheduleMap = new LinkedHashMap<>();

      for (int i = 1; i <= 7; i++) {
         List<BTimeSchedule> timeScheduleList = new ArrayList<>();
         BDaySchedule daySchedule = weeklySchedule.get(BWeekday.make((i - 1) % 7));
         BTimeSchedule[] timeSchedules = daySchedule.getTimesInOrder();

         for (BTimeSchedule timeSchedule : timeSchedules) {
            timeScheduleList.add((BTimeSchedule)timeSchedule.newCopy(true));
         }

         timeScheduleMap.put(i - 1, timeScheduleList);
      }

      return timeScheduleMap;
   }

   protected BBacnetScheduleDescriptor replaceCurrentBacnetSchedule(Array<PropertyValue> initialValues) {
      BBacnetObjectIdentifier objectId = this.getObjectId();

      try {
         BObjectHandler objectHandler = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getObjectHandler();
         objectHandler.setDeleteEnabled(true);
         objectHandler.setCreateEnabled(true);
         BacnetServicePrimitive deleteResponse = objectHandler.receiveRequest(11, new DeleteObjectRequest(objectId), null);
         if (!(deleteResponse instanceof DeleteObjectAck)) {
            if (log.isLoggable(Level.FINE)) {
               ErrorType error = null;
               if (deleteResponse instanceof DeleteObjectError) {
                  error = ((DeleteObjectError)deleteResponse).getError();
               }

               log.fine("When replacing a BACnet schedule, failed to delete the existing schedule; object ID: " + objectId + ", error: " + error);
            }

            return null;
         } else {
            BacnetServicePrimitive createResponse = objectHandler.receiveRequest(10, new CreateObjectRequest(objectId, initialValues), null);
            if (!(createResponse instanceof CreateObjectAck)) {
               if (log.isLoggable(Level.FINE)) {
                  ErrorType error = null;
                  if (createResponse instanceof CreateObjectError) {
                     error = ((CreateObjectError)createResponse).getError();
                  }

                  log.fine(
                     "When replacing a BACnet schedule, failed to create a new schedule; object ID: "
                        + this.getObjectId()
                        + ", error: "
                        + error
                        + ", initial values: "
                        + initialValues
                  );
               }

               return null;
            } else {
               return (BBacnetScheduleDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(objectId);
            }
         }
      } catch (Exception var7) {
         String message = "Exception while replacing BACnet schedule with object ID " + objectId + "; exception: " + var7;
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.WARNING, message, (Throwable)var7);
         } else {
            log.warning(message);
         }

         return null;
      }
   }

   private void resolveTargetReferences() {
      SlotCursor<Property> c = this.getListOfObjectPropertyReferences().getProperties();

      while (c.next(BBacnetDeviceObjectPropertyReference.class)) {
         BBacnetDeviceObjectPropertyReference dopr = (BBacnetDeviceObjectPropertyReference)c.get();
         if (dopr.isDeviceIdUsed() && !dopr.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId())) {
            BBacnetObjectIdentifier deviceId = dopr.getDeviceId();
            if (deviceId.isValid() && deviceId.getObjectType() == 8 && BBacnetNetwork.bacnet().doLookupDeviceById(deviceId) == null) {
               try {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, deviceId.getInstanceNumber(), deviceId.getInstanceNumber());
               } catch (BacnetException var5) {
                  log.warning("Unable to determine address for Schedule target reference: " + this + " target=" + deviceId + ": " + var5);
               }
            }
         }
      }
   }

   private ChangeListError addScheduleTargets(byte[] scheduleTargets) throws RejectException {
      ArrayList<BBacnetDeviceObjectPropertyReference> v = new ArrayList<>();
      int ffen = 1;

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(scheduleTargets);

            for (int tag = asnIn.peekTag(); tag != -1; tag = asnIn.peekTag()) {
               BBacnetDeviceObjectPropertyReference r = new BBacnetDeviceObjectPropertyReference();
               r.readAsn(asnIn);
               if (r.isDeviceIdUsed() && !r.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId()) && r.getDeviceId().getObjectType() != 8) {
                  return new NChangeListError(8, new NErrorType(2, 37), ffen);
               }

               if (!this.isValidReference(r)) {
                  return new NChangeListError(8, new NErrorType(2, 47), ffen);
               }

               v.add(r);
               ffen++;
            }
         }
      } catch (AsnException var12) {
         return new NChangeListError(8, new NErrorType(2, 9), ffen);
      }

      ffen = 1;

      try {
         BLocalBacnetDevice local = BBacnetNetwork.localDevice();

         for (BBacnetDeviceObjectPropertyReference rx : v) {
            if (!rx.isDeviceIdUsed() || rx.getDeviceId().equals(local.getObjectId())) {
               BIBacnetExportObject o = local.lookupBacnetObject(rx.getObjectId());
               if (o == null) {
                  return new NChangeListError(8, new NErrorType(2, 37), ffen);
               }

               BObject obj = o.getObject();
               if (obj == null) {
                  return new NChangeListError(8, new NErrorType(2, 1000), ffen);
               }
            }

            this.getListOfObjectPropertyReferences().addListElement(rx, BLocalBacnetDevice.getBacnetContext());
            ffen++;
         }

         return null;
      } catch (PermissionException var9) {
         return new NChangeListError(8, new NErrorType(2, 40), ffen);
      } catch (Exception var10) {
         return new NChangeListError(8, new NErrorType(2, 0), ffen);
      }
   }

   private ChangeListError removeScheduleTargets(byte[] scheduleTargets) throws RejectException {
      int ffen = 1;
      ArrayList<BBacnetDeviceObjectPropertyReference> v = new ArrayList<>();

      try {
         synchronized (asnIn) {
            asnIn.setBuffer(scheduleTargets);

            for (int tag = asnIn.peekTag(); tag != -1; ffen++) {
               BBacnetDeviceObjectPropertyReference r = new BBacnetDeviceObjectPropertyReference();
               r.readAsn(asnIn);
               v.add(r);
               tag = asnIn.peekTag();
            }
         }
      } catch (AsnException var11) {
         if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "AsnException occurred in removeScheduleTargets", (Throwable)var11);
         }

         return new NChangeListError(9, new NErrorType(2, 9), ffen);
      }

      for (ffen = 1; ffen <= v.size(); ffen++) {
         BBacnetDeviceObjectPropertyReference r = v.get(ffen - 1);
         if (!this.getListOfObjectPropertyReferences().contains(r)) {
            return new NChangeListError(9, new NErrorType(5, 81), ffen);
         }
      }

      try {
         for (BBacnetDeviceObjectPropertyReference r : v) {
            this.getListOfObjectPropertyReferences().removeListElement(r, BLocalBacnetDevice.getBacnetContext());
            ffen++;
         }

         return null;
      } catch (PermissionException var8) {
         return new NChangeListError(9, new NErrorType(2, 40), ffen);
      } catch (Exception var9) {
         return new NChangeListError(9, new NErrorType(2, 0), ffen);
      }
   }

   protected boolean isEqual(int ansTypeOfRefObj, int asnTypeOfSchedule) {
      return ansTypeOfRefObj == asnTypeOfSchedule;
   }

   private boolean isValidReference(BBacnetDeviceObjectPropertyReference ref) {
      PropertyInfo pi = getPropertyInfo(ref);
      return pi != null ? this.isEqual(pi.getAsnType(), this.getAsnType()) : true;
   }

   private static PropertyInfo getPropertyInfo(BBacnetDeviceObjectPropertyReference ref) {
      PropertyInfo pi = BBacnetNetwork.localDevice().getPropertyInfo(ref.getObjectId().getObjectType(), ref.getPropertyId());
      if (ref.isDeviceIdUsed() && !ref.getDeviceId().equals(BBacnetNetwork.localDevice().getObjectId())) {
         BBacnetDevice device = BBacnetNetwork.bacnet().doLookupDeviceById(ref.getDeviceId());
         if (device != null) {
            pi = device.getPropertyInfo(ref.getObjectId().getObjectType(), ref.getPropertyId());
         }
      }

      return pi;
   }

   private void validateReferences() {
      BBacnetListOf list = this.getListOfObjectPropertyReferences();
      BBacnetDeviceObjectPropertyReference[] refs = (BBacnetDeviceObjectPropertyReference[])list.getChildren(BBacnetDeviceObjectPropertyReference.class);

      for (int i = 0; i < refs.length; i++) {
         BBacnetDeviceObjectPropertyReference dopr = refs[i];
         if (!this.isValidReference(dopr)) {
            list.remove(dopr);
         }
      }
   }

   public BIcon getIcon() {
      return icon;
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
      out.trTitle("BacnetScheduleDescriptor", 2);
      out.prop("fatalFault", this.fatalFault);
      out.prop("schedule", this.schedule);
      out.prop("supp", supp);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.prop("configOk", this.configOk());
      out.prop("isValid", this.isValid);
      out.endProps();
   }

   static BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }
}
