package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.alarm.BBacnetChangeOfDiscreteValueAlgorithm;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.history.BBacnetTrendLogAlarmSourceExt;
import com.tridium.bacnet.history.BBacnetTrendLogRemoteExt;
import com.tridium.bacnet.history.BIBacnetTrendLogExt;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BAlarmTimestamps;
import javax.baja.alarm.ext.BFaultAlgorithm;
import javax.baja.alarm.ext.BLimitEnable;
import javax.baja.alarm.ext.BNotifyType;
import javax.baja.alarm.ext.BOffnormalAlgorithm;
import javax.baja.alarm.ext.fault.BEnumFaultAlgorithm;
import javax.baja.alarm.ext.fault.BOutOfRangeFaultAlgorithm;
import javax.baja.alarm.ext.offnormal.BBooleanChangeOfStateAlgorithm;
import javax.baja.alarm.ext.offnormal.BBooleanCommandFailureAlgorithm;
import javax.baja.alarm.ext.offnormal.BEnumChangeOfStateAlgorithm;
import javax.baja.alarm.ext.offnormal.BEnumCommandFailureAlgorithm;
import javax.baja.alarm.ext.offnormal.BFloatingLimitAlgorithm;
import javax.baja.alarm.ext.offnormal.BNumericChangeOfStateAlgorithm;
import javax.baja.alarm.ext.offnormal.BOutOfRangeAlgorithm;
import javax.baja.alarm.ext.offnormal.BStringChangeOfStateAlgorithm;
import javax.baja.alarm.ext.offnormal.BStringChangeOfStateFaultAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.alarm.BBacnetStatusAlgorithm;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetEventParameter;
import javax.baja.bacnet.datatypes.BBacnetListOf;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetPropertyStates;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.datatypes.BBacnetUnsigned;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ChangeListError;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.io.RangeData;
import javax.baja.bacnet.io.RangeReference;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BPointExtension;
import javax.baja.control.BStringPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLink;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.util.BFormat;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "eventEnrollmentOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.EVENT_ENROLLMENT)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "typeOfEvent",
      type = "BBacnetEventType",
      defaultValue = "BBacnetEventType.none",
      flags = 65
   ), @NiagaraProperty(
      name = "notifyTypeId",
      type = "BNotifyType",
      defaultValue = "BNotifyType.alarm",
      flags = 69
   ), @NiagaraProperty(
      name = "objectPropertyReference",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()",
      flags = 69
   ), @NiagaraProperty(
      name = "notificationClassId",
      type = "int",
      defaultValue = "0",
      flags = 64
   ), @NiagaraProperty(
      name = "reliability",
      type = "BBacnetReliability",
      defaultValue = "BBacnetReliability.configurationError",
      flags = 65
   ), @NiagaraProperty(
      name = "eventParameter",
      type = "BBacnetEventParameter",
      defaultValue = "new BBacnetEventParameter()",
      flags = 5
   )})
public class BBacnetEventEnrollmentDescriptor extends BBacnetEventSource implements BacnetPropertyListProvider {
   public static final Property eventEnrollmentOrd = newProperty(64, BOrd.DEFAULT, null);
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(9), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property description = newProperty(64, "", null);
   public static final Property typeOfEvent = newProperty(65, BBacnetEventType.none, null);
   public static final Property notifyTypeId = newProperty(69, BNotifyType.alarm, null);
   public static final Property objectPropertyReference = newProperty(69, new BBacnetDeviceObjectPropertyReference(), null);
   public static final Property notificationClassId = newProperty(64, 0, null);
   public static final Property reliability = newProperty(65, BBacnetReliability.configurationError, null);
   public static final Property eventParameter = newProperty(5, new BBacnetEventParameter(), null);
   public static final Type TYPE = Sys.loadType(BBacnetEventEnrollmentDescriptor.class);
   private static final int[] ARRAY_PROPS = new int[]{130, 351, 352, 371};
   private static final Logger logger = Logger.getLogger("bacnet.export.object.eventEnrollment");
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 37, 72, 83, 78, 36, 35, 0, 17, 130, 353, 111, 103};
   private static final int[] OPTIONAL_PROPS = new int[]{28, 351, 352, 355, 354};
   private static final PropertyValue[] EMPTY_PROP_VALUE_ARRAY = new PropertyValue[0];
   private static final BBacnetBitString STATUS_FLAGS_DEFAULT = BBacnetBitString.make(new boolean[]{false, true, false, false});
   private static final BLink[] EMPTY_LINKS_ARRAY = new BLink[0];
   private BPointExtension pointExt;
   private BAlarmTransitionBits eventEnable = BAlarmTransitionBits.DEFAULT;
   private String toOffnormalText = "";
   private String toFaultText = "";
   private String toNormalText = "";
   private boolean eventAlgorithmInhibit;
   private static final BBacnetObjectPropertyReference OBJECT_PROP_REF_DEFAULT = new BBacnetObjectPropertyReference(BBacnetObjectIdentifier.make(0, 4194303));
   private BBacnetObjectPropertyReference eventAlgorithmInhibitRef = OBJECT_PROP_REF_DEFAULT;
   private BBacnetObjectIdentifier oldId;
   private String oldName;
   private boolean duplicate;
   private boolean configOk;

   public BOrd getEventEnrollmentOrd() {
      return (BOrd)this.get(eventEnrollmentOrd);
   }

   public void setEventEnrollmentOrd(BOrd v) {
      this.set(eventEnrollmentOrd, v, null);
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

   public BBacnetEventType getTypeOfEvent() {
      return (BBacnetEventType)this.get(typeOfEvent);
   }

   public void setTypeOfEvent(BBacnetEventType v) {
      this.set(typeOfEvent, v, null);
   }

   public BNotifyType getNotifyTypeId() {
      return (BNotifyType)this.get(notifyTypeId);
   }

   public void setNotifyTypeId(BNotifyType v) {
      this.set(notifyTypeId, v, null);
   }

   public BBacnetDeviceObjectPropertyReference getObjectPropertyReference() {
      return (BBacnetDeviceObjectPropertyReference)this.get(objectPropertyReference);
   }

   public void setObjectPropertyReference(BBacnetDeviceObjectPropertyReference v) {
      this.set(objectPropertyReference, v, null);
   }

   public int getNotificationClassId() {
      return this.getInt(notificationClassId);
   }

   public void setNotificationClassId(int v) {
      this.setInt(notificationClassId, v, null);
   }

   public BBacnetReliability getReliability() {
      return (BBacnetReliability)this.get(reliability);
   }

   public void setReliability(BBacnetReliability v) {
      this.set(reliability, v, null);
   }

   public BBacnetEventParameter getEventParameter() {
      return (BBacnetEventParameter)this.get(eventParameter);
   }

   public void setEventParameter(BBacnetEventParameter v) {
      this.set(eventParameter, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      if (Sys.isStationStarted()) {
         this.initialize();
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public void stationStarted() throws Exception {
      super.stationStarted();
      this.initialize();
   }

   private void initialize() {
      BBacnetNetwork.localDevice().export(this);
      BPointExtension pointExt = (BPointExtension)this.getObject();
      this.updateEventParameters(pointExt);
      this.getEventEnable(pointExt);
      this.getNotificationClass(pointExt);
      this.updateEventMessageTextsConfig(pointExt);
      this.updateEventAlgorithmInhibitInfo(pointExt);
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId)) {
            BLocalBacnetDevice local = BBacnetNetwork.localDevice();
            local.unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var5) {
            }

            if (this.configOk) {
               local.incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BLocalBacnetDevice local = BBacnetNetwork.localDevice();
            local.unexport(this.oldId, this.oldName, this);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.configOk) {
               local.incrementDatabaseRevision();
            }
         } else if (p.equals(eventEnrollmentOrd)) {
            this.pointExt = null;
            BLocalBacnetDevice local = BBacnetNetwork.localDevice();
            local.exportByOrd(this);
            if (this.configOk) {
               local.incrementDatabaseRevision();
            }
         }
      }
   }

   public void stopped() throws Exception {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      this.oldId = null;
      this.oldName = null;
      this.pointExt = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }

      super.stopped();
   }

   public String toString(Context context) {
      return this.getObjectName() + " [" + this.getObjectId() + ']';
   }

   @Override
   public BObject getObject() {
      BPointExtension pointExt = this.pointExt;
      if (pointExt == null) {
         pointExt = this.resolvePointExt();
         if (pointExt == null) {
            return null;
         }
      }

      BComponent target = (BComponent)pointExt.getParent();
      if (target == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": associated PointExt (" + pointExt.getSlotPath() + ") has no parent");
         }

         this.resetDescriptor();
         return null;
      } else {
         this.pointExt = pointExt;
         return pointExt;
      }
   }

   private BPointExtension resolvePointExt() {
      BOrd objectOrd = this.getEventEnrollmentOrd();
      if (objectOrd.isNull()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": eventEnrollmentOrd is null");
         }

         this.resetDescriptor();
         return null;
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": resolving eventEnrollmentOrd: " + objectOrd);
         }

         BObject resolved;
         try {
            resolved = objectOrd.get(this);
         } catch (Exception var4) {
            logException(
               Level.WARNING, new StringBuilder(this.getObjectId().toString()).append(": could not resolve eventEnrollmentOrd: ").append(objectOrd), var4
            );
            this.resetDescriptor();
            return null;
         }

         if (!(resolved instanceof BAlarmSourceExt) && !(resolved instanceof BBacnetTrendLogAlarmSourceExt)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": eventEnrollmentOrd resolved to type "
                     + resolved.getType()
                     + " and not instanceof alarm:AlarmSourceExt or bacnet:BacnetTrendLogAlarmSourceExt"
               );
            }

            this.resetDescriptor();
            return null;
         } else {
            return (BPointExtension)resolved;
         }
      }
   }

   @Override
   public BOrd getObjectOrd() {
      return this.getEventEnrollmentOrd();
   }

   @Override
   public void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(eventEnrollmentOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         this.configOk = false;
      } else if (!this.getObjectId().isValid()) {
         this.setStatusFaulted("Invalid Object ID");
      } else {
         String err = BBacnetNetwork.localDevice().export(this);
         if (err != null) {
            this.duplicate = true;
            this.setStatusFaulted(err);
         } else {
            this.duplicate = false;
            this.configOk = true;
            this.setStatus(BStatus.ok);
            this.setFaultCause("");
         }
      }
   }

   private void setStatusFaulted(String faultCause) {
      this.setStatus(BStatus.makeFault(this.getStatus(), true));
      this.setFaultCause(faultCause);
      this.configOk = false;
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, OPTIONAL_PROPS);
   }

   public int[] getOptionalProps() {
      return OPTIONAL_PROPS;
   }

   public int[] getRequiredProps() {
      return REQUIRED_PROPS;
   }

   @Override
   public PropertyValue readProperty(PropertyReference propertyReference) throws RejectException {
      return this.readProperty(propertyReference.getPropertyId(), propertyReference.getPropertyArrayIndex());
   }

   @Override
   public PropertyValue[] readPropertyMultiple(PropertyReference[] propertyReferences) throws RejectException {
      ArrayList<PropertyValue> results = new ArrayList<>(propertyReferences.length);

      for (PropertyReference ref : propertyReferences) {
         switch (ref.getPropertyId()) {
            case 8:
               for (int prop : REQUIRED_PROPS) {
                  results.add(this.readProperty(prop, -1));
               }

               for (int prop : OPTIONAL_PROPS) {
                  results.add(this.readProperty(prop, -1));
               }
               break;
            case 80:
               for (int prop : OPTIONAL_PROPS) {
                  results.add(this.readProperty(prop, -1));
               }
               break;
            case 105:
               for (int prop : REQUIRED_PROPS) {
                  results.add(this.readProperty(prop, -1));
               }
               break;
            default:
               results.add(this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex()));
         }
      }

      return results.toArray(EMPTY_PROP_VALUE_ARRAY);
   }

   @Override
   public RangeData readRange(RangeReference rangeReference) throws RejectException {
      int propertyId = rangeReference.getPropertyId();
      return !hasProperty(propertyId)
         ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
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
   public ErrorType writeProperty(PropertyValue propertyValue) throws BacnetException {
      return this.writeProperty(
         propertyValue.getPropertyId(), propertyValue.getPropertyArrayIndex(), propertyValue.getPropertyValue(), propertyValue.getPriority()
      );
   }

   @Override
   public ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   public BBacnetEventState getEventState() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      if (pointExt instanceof BAlarmSourceExt) {
         return BBacnetEventState.make(((BAlarmSourceExt)pointExt).getAlarmState());
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         return BBacnetEventState.make(((BBacnetTrendLogAlarmSourceExt)pointExt).getAlarmState());
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt; returning null for event state"
            );
         }

         return null;
      }
   }

   private BBacnetEventState getEventState(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         return BBacnetEventState.make(((BAlarmSourceExt)pointExt).getAlarmState());
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         return BBacnetEventState.make(((BBacnetTrendLogAlarmSourceExt)pointExt).getAlarmState());
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt; returning BacnetEventState.normal"
            );
         }

         return BBacnetEventState.normal;
      }
   }

   @Override
   public BControlPoint getPoint() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      if (pointExt instanceof BAlarmSourceExt) {
         return pointExt.getParentPoint();
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt; returning null for getPoint"
            );
         }

         return null;
      }
   }

   @Override
   public BBacnetBitString getAckedTransitions() {
      BAlarmTransitionBits ackedTransitions = getAckedTransitions((BPointExtension)this.getObject());
      if (ackedTransitions == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (this.pointExt != null ? this.pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt; returning default ackedTransitions value"
            );
         }

         return null;
      } else {
         return BacnetBitStringUtil.getBacnetEventTransitionBits(ackedTransitions);
      }
   }

   private static BAlarmTransitionBits getAckedTransitions(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         return ((BAlarmSourceExt)pointExt).getAckedTransitions();
      } else {
         return pointExt instanceof BBacnetTrendLogAlarmSourceExt ? ((BBacnetTrendLogAlarmSourceExt)pointExt).getAckedTransitions() : null;
      }
   }

   @Override
   public BBacnetTimeStamp[] getEventTimeStamps() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      if (pointExt instanceof BAlarmSourceExt) {
         BAlarmSourceExt alarmExt = (BAlarmSourceExt)pointExt;
         return new BBacnetTimeStamp[]{
            new BBacnetTimeStamp(alarmExt.getLastOffnormalTime()),
            new BBacnetTimeStamp(alarmExt.getLastFaultTime()),
            new BBacnetTimeStamp(alarmExt.getLastToNormalTime())
         };
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         BBacnetTrendLogAlarmSourceExt trendAlarmExt = (BBacnetTrendLogAlarmSourceExt)pointExt;
         BAlarmTimestamps toOffnormalTimes = trendAlarmExt.getToOffnormalTimes();
         BAlarmTimestamps toFaultTimes = trendAlarmExt.getToFaultTimes();
         BAbsTime normalTime = toOffnormalTimes.getNormalTime();
         if (normalTime.isBefore(toFaultTimes.getNormalTime())) {
            normalTime = toFaultTimes.getNormalTime();
         }

         return new BBacnetTimeStamp[]{
            new BBacnetTimeStamp(toOffnormalTimes.getAlarmTime()), new BBacnetTimeStamp(toFaultTimes.getAlarmTime()), new BBacnetTimeStamp(normalTime)
         };
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt; returning default eventTimeStamps"
            );
         }

         return null;
      }
   }

   @Override
   public BBacnetNotifyType getNotifyType() {
      return BBacnetNotifyType.make(this.getNotifyTypeId());
   }

   @Override
   public BBacnetBitString getEventEnable() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      BAlarmTransitionBits alarmEnable = this.getEventEnable(pointExt);
      return alarmEnable != null ? BacnetBitStringUtil.getBacnetEventTransitionBits(alarmEnable) : null;
   }

   private BAlarmTransitionBits getEventEnable(BPointExtension pointExt) {
      BAlarmTransitionBits alarmEnable = null;
      if (pointExt instanceof BAlarmSourceExt) {
         alarmEnable = ((BAlarmSourceExt)pointExt).getAlarmEnable();
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         alarmEnable = ((BBacnetTrendLogAlarmSourceExt)pointExt).getAlarmEnable();
      }

      if (alarmEnable != null) {
         this.eventEnable = alarmEnable;
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine(
            this.getObjectId()
               + ": associated PointExt ("
               + (pointExt != null ? pointExt.getSlotPath() : null)
               + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt"
         );
      }

      return alarmEnable;
   }

   @Override
   public BEnum getEventType() {
      this.updateEventParameters((BPointExtension)this.getObject());
      return this.getTypeOfEvent();
   }

   @Override
   public void statusChanged() {
      this.setBacnetStatusFlags(this.getStatusFlags());
   }

   public BBacnetBitString getStatusFlags() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      if (pointExt == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt; returning default status flags"
            );
         }

         return STATUS_FLAGS_DEFAULT;
      } else {
         return BBacnetBitString.make(
            new boolean[]{
               !BBacnetEventState.isNormal(this.getEventState(pointExt)), !this.readReliability().equals(BBacnetReliability.noFaultDetected), false, false
            }
         );
      }
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      return pointExt instanceof BAlarmSourceExt || pointExt instanceof BBacnetTrendLogAlarmSourceExt;
   }

   @Override
   public boolean isEventInitiationEnabled() {
      return true;
   }

   @Override
   public int[] getEventPriorities() {
      BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
      return nc != null ? nc.getEventPriorities() : null;
   }

   @Override
   public BBacnetNotificationClassDescriptor getNotificationClass() {
      return this.getNotificationClass((BPointExtension)this.getObject());
   }

   private BBacnetNotificationClassDescriptor getNotificationClass(BPointExtension pointExt) {
      String alarmClassName = getAlarmClassName(pointExt);
      if (alarmClassName == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": cannot retrieve the notification class descriptor- associated PointExt ("
                  + (pointExt != null ? pointExt.getSlotPath() : null)
                  + ") is not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt"
            );
         }

         return null;
      } else if (alarmClassName.isEmpty()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": pointExt's alarmClassName is an empty string; setting notification class ID to unconfigured instance number");
         }

         this.setNotificationClassId(4194303);
         this.setReliability(BBacnetReliability.configurationError);
         return null;
      } else {
         BBacnetNotificationClassDescriptor descriptor = this.findNotificationClass(alarmClassName);
         if (descriptor == null) {
            this.setReliability(BBacnetReliability.configurationError);
            return null;
         } else {
            this.setNotificationClassId(descriptor.getObjectId().getInstanceNumber());
            return descriptor;
         }
      }
   }

   private BBacnetNotificationClassDescriptor findNotificationClass(String alarmClassName) {
      BAlarmService alarmService;
      try {
         alarmService = (BAlarmService)Sys.getService(BAlarmService.TYPE);
      } catch (ServiceNotFoundException var5) {
         logException(Level.WARNING, new StringBuilder(this.getObjectId().toString()).append(": getNotificationClass: could not find the alarm service"), var5);
         return null;
      }

      BAlarmClass alarmClass = alarmService.lookupAlarmClass(alarmClassName);
      if (alarmClass == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": getNotificationClass: could not find alarm class " + alarmClassName + " in the alarm service");
         }

         return null;
      } else {
         BIBacnetExportObject descriptor = this.findDescriptor(alarmClass.getHandleOrd());
         if (descriptor instanceof BBacnetNotificationClassDescriptor) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": getNotificationClass: found new notification class descriptor for alarm class " + alarmClassName);
            }

            return (BBacnetNotificationClassDescriptor)descriptor;
         } else {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": getNotificationClass: could not find a notification class descriptor for alarmClass \""
                     + alarmClass.getSlotPath()
                     + '"'
               );
            }

            return null;
         }
      }
   }

   @Deprecated
   @Override
   protected void updateAlarmInhibit() {
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (ndx >= 0 && !isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else if (ndx < -1) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
      } else {
         switch (pId) {
            case 0:
               return this.readAckedTransitions();
            case 17:
               this.getNotificationClass();
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getNotificationClassId()));
            case 35:
               return this.readEventEnable();
            case 36:
               return this.readEventState();
            case 37:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getEventType()));
            case 72:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(0));
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 78:
               return this.readObjectPropertyReference();
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(9));
            case 83:
               return this.readEventParameters();
            case 103:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.readReliability()));
            case 111:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBitString(this.getStatusFlags()));
            case 130:
               return this.readEventTimeStamps(ndx);
            case 353:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getEventDetectionEnable()));
            case 371:
               return this.readPropertyList(ndx);
            default:
               return this.readOptionalProperty(pId, ndx);
         }
      }
   }

   private static String getAlarmClassName(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         return ((BAlarmSourceExt)pointExt).getAlarmClass();
      } else {
         return pointExt instanceof BBacnetTrendLogAlarmSourceExt ? ((BBacnetTrendLogAlarmSourceExt)pointExt).getAlarmClass() : null;
      }
   }

   private PropertyValue readEventParameters() {
      try {
         BBacnetEventParameter params = this.readEventParameters((BPointExtension)this.getObject());
         return new NReadPropertyResult(83, AsnUtil.toAsn(-4, params));
      } catch (BBacnetEventEnrollmentDescriptor.EventEnrollmentException var2) {
         return new NReadPropertyResult(83, var2.errorType);
      }
   }

   private PropertyValue readObjectPropertyReference() {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      if (pointExt == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": target alarm ext not configured; falling back to cached objectPropertyReference value");
         }

         return makeObjPropRefPropValue(this.getObjectPropertyReference());
      } else {
         BComponent target = (BComponent)pointExt.getParent();
         if (target == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": target alarm ext has no parent");
            }

            return makeObjPropRefError();
         } else {
            return this.getTargetObjPropRef(target);
         }
      }
   }

   private PropertyValue getTargetObjPropRef(BComponent target) {
      if (target instanceof BControlPoint) {
         return this.getPointPropRef((BControlPoint)target);
      } else if (target instanceof BIBacnetTrendLogExt) {
         return this.getTrendPropRef((BIBacnetTrendLogExt)target);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": target alarm ext has no parent");
         }

         return makeObjPropRefError();
      }
   }

   private PropertyValue getPointPropRef(BControlPoint point) {
      BAbstractProxyExt proxyExt = point.getProxyExt();
      if (proxyExt instanceof BBacnetProxyExt) {
         return makeObjPropRefPropValue(makeRemoteDeviceObjPropRef((BBacnetProxyExt)proxyExt));
      } else {
         BIBacnetExportObject descriptor = this.findDescriptor(point.getHandleOrd());
         if (descriptor == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": alarm ext's target's descriptor not found in local device; target slot path: " + point.getSlotPath());
            }

            return makeObjPropRefError();
         } else {
            return makeObjPropRefPropValue(makeLocalDeviceObjPropRef(descriptor.getObjectId()));
         }
      }
   }

   private BIBacnetExportObject findDescriptor(BOrd ord) {
      BBacnetObjectIdentifier objectId = BBacnetNetwork.localDevice().lookupBacnetObjectId(ord);
      if (objectId == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": could not find object ID for ord \"" + ord + '"');
         }

         return null;
      } else {
         BIBacnetExportObject descriptor = BBacnetNetwork.localDevice().lookupBacnetObject(objectId);
         if (descriptor == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": found object ID \"" + objectId + "\" for ord \"" + ord + "\" but could not find descriptor");
            }

            return null;
         } else {
            return descriptor;
         }
      }
   }

   private PropertyValue getTrendPropRef(BIBacnetTrendLogExt trendLogExt) {
      if (trendLogExt instanceof BBacnetTrendLogRemoteExt) {
         BBacnetTrendLogRemoteExt trendLogRemoteExt = (BBacnetTrendLogRemoteExt)trendLogExt;
         BBacnetDeviceObjectPropertyReference deviceObjPropRef = new BBacnetDeviceObjectPropertyReference(
            trendLogRemoteExt.getObjectId(), trendLogRemoteExt.getPropertyId(), trendLogRemoteExt.getArrayIndex(), trendLogRemoteExt.getDevice().getObjectId()
         );
         return makeObjPropRefPropValue(deviceObjPropRef);
      } else {
         BIBacnetExportObject descriptor = this.findDescriptor(((BComponent)trendLogExt).getHandleOrd());
         if (descriptor == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": alarm ext's target's descriptor not found in local device; target slot path: "
                     + ((BComponent)trendLogExt).getSlotPath()
               );
            }

            return makeObjPropRefError();
         } else {
            return makeObjPropRefPropValue(makeLocalDeviceObjPropRef(descriptor.getObjectId()));
         }
      }
   }

   private static PropertyValue makeObjPropRefPropValue(BBacnetDeviceObjectPropertyReference objPropRef) {
      return new NReadPropertyResult(78, -1, AsnUtil.toAsn(-4, objPropRef));
   }

   private static PropertyValue makeObjPropRefError() {
      return new NReadPropertyResult(78, new NErrorType(2, 0));
   }

   private PropertyValue readEventState() {
      BBacnetEventState eventState = null;
      if (this.getEventDetectionEnable()) {
         eventState = this.getEventState();
      }

      if (eventState == null) {
         eventState = BBacnetEventState.normal;
      }

      return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(eventState));
   }

   private PropertyValue readEventEnable() {
      BBacnetBitString eventEnable = this.getEventEnable();
      if (eventEnable == null) {
         eventEnable = BacnetBitStringUtil.getBacnetEventTransitionBits(this.eventEnable);
      }

      return new NReadPropertyResult(35, -1, AsnUtil.toAsnBitString(eventEnable));
   }

   private PropertyValue readAckedTransitions() {
      if (!this.getEventDetectionEnable()) {
         return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(ACKED_TRANS_DEFAULT));
      } else {
         BAlarmTransitionBits ackedTrans = getAckedTransitions((BPointExtension)this.getObject());
         if (ackedTrans == null) {
            return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(ACKED_TRANS_DEFAULT));
         } else {
            BAlarmTransitionBits eventTrans = this.readEventTransition(ackedTrans);
            return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(eventTrans)));
         }
      }
   }

   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      switch (pId) {
         case 28:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
         case 351:
            return this.readEventMessageTexts(ndx);
         case 352:
            return this.readEventMessageTextsConfig(ndx);
         case 354:
            return this.readEventAlgorithmInhibit();
         case 355:
            return this.readEventAlgorithmInhibitRef();
         default:
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
      }
   }

   private PropertyValue readEventMessageTextsConfig(int ndx) {
      this.updateEventMessageTextsConfig((BPointExtension)this.getObject());
      return this.readEventMessageTextsConfig(this.toOffnormalText, this.toFaultText, this.toNormalText, ndx);
   }

   private void updateEventMessageTextsConfig(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         BAlarmSourceExt alarmExt = (BAlarmSourceExt)pointExt;
         this.toOffnormalText = alarmExt.getToOffnormalText().getFormat();
         this.toFaultText = alarmExt.getToFaultText().getFormat();
         this.toNormalText = alarmExt.getToNormalText().getFormat();
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         BBacnetTrendLogAlarmSourceExt trendAlarmExt = (BBacnetTrendLogAlarmSourceExt)pointExt;
         this.toOffnormalText = trendAlarmExt.getToOffnormalText().getFormat();
         this.toFaultText = trendAlarmExt.getToFaultText().getFormat();
         this.toNormalText = trendAlarmExt.getToNormalText().getFormat();
      }
   }

   private void updateEventParameters(BPointExtension pointExt) {
      try {
         this.readEventParameters(pointExt);
      } catch (BBacnetEventEnrollmentDescriptor.EventEnrollmentException var3) {
         logger.log(Level.FINE, this.getObjectId() + ": exception while updating event parameters", (Throwable)var3);
      }
   }

   private BBacnetEventParameter readEventParameters(BPointExtension pointExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (pointExt == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": target alarm ext not configured; falling back to cached eventParameters value");
         }

         return this.getEventParameter();
      } else {
         BBacnetEventParameter eventParam;
         if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
            BBacnetTrendLogAlarmSourceExt trendAlarmExt = (BBacnetTrendLogAlarmSourceExt)pointExt;
            eventParam = BBacnetEventParameter.makeBufferReady(trendAlarmExt.getNotificationThreshold(), trendAlarmExt.getLastNotifyRecord());
         } else {
            if (!(pointExt instanceof BAlarmSourceExt)) {
               logger.warning(this.getObjectId() + ": could not construct EventParameters for BPointExtension of type " + pointExt.getType());
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  "pointExt type " + pointExt.getType() + " not supported", new NErrorType(2, 0)
               );
            }

            BAlarmSourceExt alarmExt = (BAlarmSourceExt)pointExt;
            BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
            if (offnormalAlgorithm instanceof BBooleanChangeOfStateAlgorithm) {
               eventParam = BBacnetEventParameter.makeChangeOfState(
                  alarmExt.getTimeDelay(), this.getListOfValues((BBooleanChangeOfStateAlgorithm)offnormalAlgorithm)
               );
            } else if (offnormalAlgorithm instanceof BEnumChangeOfStateAlgorithm) {
               eventParam = BBacnetEventParameter.makeChangeOfState(
                  alarmExt.getTimeDelay(), this.getListOfValues((BEnumChangeOfStateAlgorithm)offnormalAlgorithm)
               );
            } else if (offnormalAlgorithm instanceof BNumericChangeOfStateAlgorithm) {
               eventParam = BBacnetEventParameter.makeChangeOfState(
                  alarmExt.getTimeDelay(), this.getListOfValues((BNumericChangeOfStateAlgorithm)offnormalAlgorithm)
               );
            } else if (offnormalAlgorithm instanceof BStringChangeOfStateAlgorithm) {
               eventParam = BBacnetEventParameter.makeChangeOfCharacterString(
                  alarmExt.getTimeDelay(), getListOfValues((BStringChangeOfStateAlgorithm)offnormalAlgorithm)
               );
            } else if (offnormalAlgorithm instanceof BBooleanCommandFailureAlgorithm) {
               eventParam = BBacnetEventParameter.makeCommandFailure(
                  alarmExt.getTimeDelay(),
                  this.getLinkedPropertyReference(offnormalAlgorithm, BBooleanCommandFailureAlgorithm.feedbackValue, BBacnetBinaryPointDescriptor.TYPE)
               );
            } else if (offnormalAlgorithm instanceof BEnumCommandFailureAlgorithm) {
               eventParam = BBacnetEventParameter.makeCommandFailure(
                  alarmExt.getTimeDelay(),
                  this.getLinkedPropertyReference(offnormalAlgorithm, BEnumCommandFailureAlgorithm.feedbackValue, BBacnetMultiStatePointDescriptor.TYPE)
               );
            } else if (offnormalAlgorithm instanceof BFloatingLimitAlgorithm) {
               BFloatingLimitAlgorithm floatingLimitAlgorithm = (BFloatingLimitAlgorithm)offnormalAlgorithm;
               eventParam = BBacnetEventParameter.makeFloatingLimit(
                  alarmExt.getTimeDelay(),
                  this.getLinkedPropertyReference(offnormalAlgorithm, BFloatingLimitAlgorithm.setpoint, BBacnetAnalogPointDescriptor.TYPE),
                  (float)floatingLimitAlgorithm.getLowDiffLimit(),
                  (float)floatingLimitAlgorithm.getHighDiffLimit(),
                  (float)floatingLimitAlgorithm.getDeadband()
               );
            } else if (offnormalAlgorithm instanceof BOutOfRangeAlgorithm) {
               BOutOfRangeAlgorithm outOfRangeAlgorithm = (BOutOfRangeAlgorithm)offnormalAlgorithm;
               eventParam = BBacnetEventParameter.makeOutOfRange(
                  alarmExt.getTimeDelay(),
                  (float)outOfRangeAlgorithm.getLowLimit(),
                  (float)outOfRangeAlgorithm.getHighLimit(),
                  (float)outOfRangeAlgorithm.getDeadband()
               );
            } else {
               if (!(offnormalAlgorithm instanceof BBacnetChangeOfDiscreteValueAlgorithm)) {
                  logger.warning(
                     this.getObjectId() + ": could not construct EventParameters for BAlarmExt offnormalAlgorithm of type " + offnormalAlgorithm.getType()
                  );
                  throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                     "alarmExt offnormal algorithm type " + offnormalAlgorithm.getType() + " not supported", new NErrorType(2, 0)
                  );
               }

               eventParam = BBacnetEventParameter.makeChangeOfDiscreteValue(alarmExt.getTimeDelayToNormal());
            }
         }

         this.setEventParameter(eventParam);
         this.setTypeOfEvent(BBacnetEventType.make(eventParam.getChoice()));
         return eventParam;
      }
   }

   private BBacnetListOf getListOfValues(BBooleanChangeOfStateAlgorithm offnormalAlgorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      PropertyInfo propInfo = findPropertyInfo(this.getObjectPropertyReference());
      BBacnetPropertyStates element;
      if (propInfo.getAsnType() == 1) {
         element = BBacnetPropertyStates.makeBoolean(offnormalAlgorithm.getAlarmValue());
      } else {
         if (!Sys.getType(propInfo.getType()).equals(BBacnetBinaryPv.TYPE)) {
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               "Boolean change-of-state alarm extension is not supported for the property type: " + propInfo, new NErrorType(2, 0)
            );
         }

         element = BBacnetPropertyStates.makeBinaryPv(offnormalAlgorithm.getAlarmValue());
      }

      BBacnetListOf listOfValues = new BBacnetListOf(BBacnetPropertyStates.TYPE);
      listOfValues.addListElement(element, null);
      return listOfValues;
   }

   private BBacnetListOf getListOfValues(BEnumChangeOfStateAlgorithm offnormalAlgorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BBacnetDeviceObjectPropertyReference objectPropRef = this.getObjectPropertyReference();
      PropertyInfo propInfo = findPropertyInfo(objectPropRef);
      BBacnetListOf listOfValues = new BBacnetListOf(BBacnetPropertyStates.TYPE);
      if (propInfo.isEnum()) {
         BTypeSpec propTypeSpec = BTypeSpec.make(propInfo.getType());
         BObject range = offnormalAlgorithm.getSlotFacets(BEnumChangeOfStateAlgorithm.alarmValues).getFacet("range");
         if (range instanceof BEnumRange) {
            Type frozenType = ((BEnumRange)range).getFrozenType();
            if (frozenType != null && !frozenType.getTypeSpec().equals(propTypeSpec)) {
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  this.getObjectId()
                     + ": enum change-of-state alarm value enum type \""
                     + frozenType
                     + '"'
                     + " does not match property type \""
                     + propInfo
                     + '"'
                     + " for object property reference: "
                     + objectPropRef,
                  new NErrorType(2, 0)
               );
            }
         }

         BEnumRange alarmValues = offnormalAlgorithm.getAlarmValues();

         for (int ordinal : alarmValues.getOrdinals()) {
            listOfValues.addListElement(BBacnetPropertyStates.makeEnum(propTypeSpec, ordinal), null);
         }
      } else if (propInfo.getAsnType() == 2) {
         for (int ordinal : offnormalAlgorithm.getAlarmValues().getOrdinals()) {
            if (ordinal < 0) {
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  this.getObjectId()
                     + ": enum change-of-state alarm value \""
                     + ordinal
                     + '"'
                     + " for property type \""
                     + propInfo
                     + '"'
                     + " must be greater than or equal to zero",
                  new NErrorType(2, 0)
               );
            }

            listOfValues.addListElement(BBacnetPropertyStates.makeUnsigned(ordinal), null);
         }
      } else {
         if (propInfo.getAsnType() != 3) {
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               this.getObjectId()
                  + ": enum change-of-state alarm extension is not supported for property type \""
                  + propInfo
                  + '"'
                  + " for object property reference: "
                  + objectPropRef,
               new NErrorType(2, 0)
            );
         }

         for (int ordinal : offnormalAlgorithm.getAlarmValues().getOrdinals()) {
            listOfValues.addListElement(BBacnetPropertyStates.makeInteger(ordinal), null);
         }
      }

      return listOfValues;
   }

   private BBacnetListOf getListOfValues(BNumericChangeOfStateAlgorithm offnormalAlgorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BBacnetDeviceObjectPropertyReference objectPropRef = this.getObjectPropertyReference();
      PropertyInfo propInfo = findPropertyInfo(objectPropRef);
      BBacnetListOf listOfValues = new BBacnetListOf(BBacnetPropertyStates.TYPE);
      if ((!propInfo.isArray() || objectPropRef.getPropertyArrayIndex() != 0) && propInfo.getAsnType() != 2) {
         if (propInfo.getAsnType() != 3) {
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               "Numeric change-of-state alarm extension is not supported for the property type: " + propInfo, new NErrorType(2, 0)
            );
         }

         for (int ordinal : offnormalAlgorithm.getAlarmValues().getOrdinals()) {
            listOfValues.addListElement(BBacnetPropertyStates.makeInteger(ordinal), null);
         }
      } else {
         for (int ordinal : offnormalAlgorithm.getAlarmValues().getOrdinals()) {
            listOfValues.addListElement(BBacnetPropertyStates.makeUnsigned(ordinal), null);
         }
      }

      return listOfValues;
   }

   private static BBacnetListOf getListOfValues(BStringChangeOfStateAlgorithm offnormalAlgorithm) {
      BBacnetListOf listOfValues = new BBacnetListOf(BString.TYPE);
      listOfValues.addListElement(offnormalAlgorithm.get(BStringChangeOfStateAlgorithm.expression), null);
      return listOfValues;
   }

   private static PropertyInfo findPropertyInfo(BBacnetDeviceObjectPropertyReference objectPropRef) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      int objectType = objectPropRef.getObjectId().getObjectType();
      int propId = objectPropRef.getPropertyId();
      PropertyInfo propInfo = getPropertyInfo(objectType, propId);
      if (propInfo == null) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "Property info not found; object type: " + BBacnetObjectType.tag(objectType) + ", property ID: " + BBacnetPropertyIdentifier.tag(propId),
            new NErrorType(2, 0)
         );
      } else {
         return propInfo;
      }
   }

   private PropertyValue readEventTimeStamps(int ndx) {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      BAbsTime lastOffnormalTime = BAbsTime.DEFAULT;
      BAbsTime lastFaultTime = BAbsTime.DEFAULT;
      BAbsTime lastToNormalTime = BAbsTime.DEFAULT;
      if (pointExt instanceof BAlarmSourceExt) {
         BAlarmSourceExt alarmExt = (BAlarmSourceExt)pointExt;
         lastOffnormalTime = alarmExt.getLastOffnormalTime();
         lastFaultTime = alarmExt.getLastFaultTime();
         lastToNormalTime = alarmExt.getLastToNormalTime();
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         BBacnetTrendLogAlarmSourceExt trendAlarmExt = (BBacnetTrendLogAlarmSourceExt)pointExt;
         BAlarmTimestamps toOffnormalTimes = trendAlarmExt.getToOffnormalTimes();
         BAlarmTimestamps toFaultTimes = trendAlarmExt.getToFaultTimes();
         lastOffnormalTime = toOffnormalTimes.getAlarmTime();
         lastFaultTime = toFaultTimes.getAlarmTime();
         lastToNormalTime = toOffnormalTimes.getNormalTime();
         if (lastToNormalTime.isBefore(toFaultTimes.getNormalTime())) {
            lastToNormalTime = toFaultTimes.getNormalTime();
         }
      }

      return this.readEventTimeStamps(lastOffnormalTime, lastFaultTime, lastToNormalTime, ndx);
   }

   private BBacnetReliability readReliability() {
      BBacnetReliability eventEnrollmentReliability = this.getReliability();
      if (!eventEnrollmentReliability.equals(BBacnetReliability.noFaultDetected)) {
         return eventEnrollmentReliability;
      } else {
         BPointExtension target = (BPointExtension)this.getObject();
         BComplex parent = target != null ? target.getParent() : null;
         if (parent == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": reliability set to configurationError because target could not be resolved");
            }

            this.setReliability(BBacnetReliability.configurationError);
            return BBacnetReliability.configurationError;
         } else if (!(parent instanceof BIStatus)) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": resolved target is type "
                     + parent.getType()
                     + " and not instanceof BIStatus; returning noFaultDetected as reliability value"
               );
            }

            return BBacnetReliability.noFaultDetected;
         } else {
            BStatus parentStatus = ((BIStatus)parent).getStatus();
            if (parentStatus.isNull()) {
               return BBacnetReliability.unreliableOther;
            } else if (parentStatus.isDown() || parentStatus.isStale()) {
               return BBacnetReliability.communicationFailure;
            } else {
               return parentStatus.isFault() ? BBacnetReliability.monitoredObjectFault : BBacnetReliability.noFaultDetected;
            }
         }
      }
   }

   private PropertyValue readEventAlgorithmInhibit() {
      this.updateEventAlgorithmInhibitInfo((BPointExtension)this.getObject());
      return new NReadPropertyResult(354, -1, AsnUtil.toAsnBoolean(this.eventAlgorithmInhibit));
   }

   private PropertyValue readEventAlgorithmInhibitRef() {
      this.updateEventAlgorithmInhibitInfo((BPointExtension)this.getObject());
      return new NReadPropertyResult(355, -1, AsnUtil.toAsn(-4, this.eventAlgorithmInhibitRef));
   }

   private void updateEventAlgorithmInhibitInfo(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         this.eventAlgorithmInhibit = ((BAlarmSourceExt)pointExt).getAlarmInhibit().getBoolean();
         this.updateAlarmInhibitRef(pointExt.getLinks(BAlarmSourceExt.alarmInhibit));
      } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
         this.eventAlgorithmInhibit = ((BBacnetTrendLogAlarmSourceExt)pointExt).getAlarmInhibit().getBoolean();
         this.updateAlarmInhibitRef(pointExt.getLinks(BBacnetTrendLogAlarmSourceExt.alarmInhibit));
      }
   }

   private void updateAlarmInhibitRef(BLink[] links) {
      for (BLink link : links) {
         if (link.isActive() && link.isEnabled()) {
            BComponent source = link.getSourceComponent();
            if (source instanceof BBooleanPoint) {
               BIBacnetExportObject descriptor = this.findDescriptor(source.getHandleOrd());
               if (descriptor != null) {
                  BBacnetObjectPropertyReference newValue = new BBacnetObjectPropertyReference(descriptor.getObjectId());
                  if (logger.isLoggable(Level.FINE) && this.eventAlgorithmInhibitRef.getObjectId().getInstanceNumber() == 4194303) {
                     logger.fine(
                        this.getObjectId()
                           + ": updating unconfigured eventAlgorithmInhibitRef because there is a valid link to alarmInhibitRef; new value: "
                           + newValue
                     );
                  }

                  this.eventAlgorithmInhibitRef = newValue;
                  return;
               }
            }
         }
      }

      if (logger.isLoggable(Level.FINE) && this.eventAlgorithmInhibitRef.getObjectId().getInstanceNumber() != 4194303) {
         logger.fine(
            this.getObjectId()
               + ": setting eventAlgorithmInhibitRef as unconfigured because there are no valid links to alarmInhibitRef; old value: "
               + this.eventAlgorithmInhibitRef
         );
      }

      this.eventAlgorithmInhibitRef = OBJECT_PROP_REF_DEFAULT;
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) {
      if (ndx >= 0) {
         if (!isArray(pId)) {
            return new NErrorType(2, 50);
         }
      } else if (ndx < -1) {
         return new NErrorType(2, 42);
      }

      try {
         switch (pId) {
            case 0:
            case 36:
            case 37:
            case 75:
            case 79:
            case 103:
            case 111:
            case 130:
            case 351:
            case 371:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": attempted to write read-only property " + BBacnetPropertyIdentifier.tag(pId));
               }

               return new NErrorType(2, 40);
            case 17:
               return this.writeNotificationClass(val);
            case 28:
               this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
               return null;
            case 35:
               return this.writeEventEnable(val);
            case 72:
               return this.writeNotifyType(val);
            case 77:
               return BacUtil.setObjectName(this, objectName, val);
            case 78:
               return this.writeObjectPropertyReference(val);
            case 83:
               return this.writeEventParameters(val);
            case 352:
               return this.writeMessageTextsConfig(ndx, val);
            case 353:
               this.setBoolean(BBacnetEventSource.eventDetectionEnable, AsnUtil.fromAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
               return null;
            case 354:
               return this.writeEventAlgorithmInhibit(val);
            case 355:
               return this.writeEventAlgorithmInhibitRef(val);
            default:
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": unknown property: " + BBacnetPropertyIdentifier.tag(pId));
               }

               return new NErrorType(2, 32);
         }
      } catch (OutOfRangeException var6) {
         logException(
            Level.INFO,
            new StringBuilder(this.getObjectId().toString()).append(": OutOfRangeException writing property ").append(BBacnetPropertyIdentifier.tag(pId)),
            var6
         );
         return new NErrorType(2, 37);
      } catch (AsnException var7) {
         logException(
            Level.INFO,
            new StringBuilder(this.getObjectId().toString()).append(": AsnException writing property ").append(BBacnetPropertyIdentifier.tag(pId)),
            var7
         );
         return new NErrorType(2, 9);
      } catch (PermissionException var8) {
         logException(
            Level.INFO,
            new StringBuilder(this.getObjectId().toString()).append(": PermissionException writing property ").append(BBacnetPropertyIdentifier.tag(pId)),
            var8
         );
         return new NErrorType(2, 40);
      }
   }

   private ErrorType writeNotifyType(byte[] val) throws AsnException {
      BBacnetNotifyType notifyType = BBacnetNotifyType.make(AsnUtil.fromAsnEnumerated(val));
      if (notifyType.getOrdinal() == 0) {
         return null;
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": the notify type must be 'Alarm': attempted value: " + notifyType);
         }

         return new NErrorType(2, 37);
      }
   }

   private ErrorType writeEventParameters(byte[] val) throws AsnException {
      BBacnetEventParameter eventParam = new BBacnetEventParameter();
      AsnUtil.fromAsn(-4, val, eventParam);
      Context bacnetContext = BLocalBacnetDevice.getBacnetContext();
      bacnetContext.getUser().checkWrite(this, eventParameter);
      bacnetContext.getUser().checkWrite(this, typeOfEvent);
      this.checkEventType(eventParam.getChoice());
      ErrorType error = this.writeEventParameters(eventParam);
      if (error != null) {
         return error;
      } else {
         this.set(eventParameter, eventParam, bacnetContext);
         this.set(typeOfEvent, BBacnetEventType.make(eventParam.getChoice()), bacnetContext);
         return null;
      }
   }

   private void checkEventType(int eventType) throws OutOfRangeException {
      switch (eventType) {
         case 0:
         case 2:
         case 6:
         case 7:
         case 8:
         case 9:
         case 11:
         case 12:
         case 13:
         case 18:
         case 19:
         default:
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": event type " + BBacnetEventType.tag(eventType) + " is not supported");
            }

            throw new OutOfRangeException("event type " + BBacnetEventType.tag(eventType) + " is not supported");
         case 1:
         case 3:
         case 4:
         case 5:
         case 10:
         case 14:
         case 15:
         case 16:
         case 17:
         case 20:
         case 21:
      }
   }

   private ErrorType writeEventParameters(BBacnetEventParameter eventParam) {
      BPointExtension pointExt = (BPointExtension)this.getObject();
      BBacnetDeviceObjectPropertyReference objPropRef = this.getObjectPropertyReference();
      BComponent target;
      if (pointExt != null) {
         target = (BComponent)pointExt.getParent();
         this.getEventEnable(pointExt);
         this.getNotificationClass(pointExt);
         this.updateEventMessageTextsConfig(pointExt);
         this.updateEventAlgorithmInhibitInfo(pointExt);
      } else {
         target = this.resolveTarget(objPropRef);
         if (target == null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": eventParameters BACnet property ("
                     + eventParam
                     + ") was written but a target could not be resolved based on the objectPropertyReference: "
                     + objPropRef
               );
            }

            return null;
         }
      }

      ErrorType error = this.configureExt(eventParam, objPropRef, pointExt, target);
      if (error != null && logger.isLoggable(Level.FINE)) {
         logger.fine(this.getObjectId() + ": error configuring alarm ext while writing EventParameters property: " + eventParam);
      }

      return error;
   }

   private ErrorType writeObjectPropertyReference(byte[] val) throws AsnException {
      BBacnetDeviceObjectPropertyReference objPropRef = new BBacnetDeviceObjectPropertyReference();
      AsnUtil.fromAsn(-4, val, objPropRef);
      BComponent newTarget = this.resolveTarget(objPropRef);
      if (newTarget == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": could not find a target when writing BACnet objectPropertyReference property: " + objPropRef);
         }

         return new NErrorType(2, 37);
      } else {
         BPointExtension pointExt = (BPointExtension)this.getObject();
         if (pointExt != null) {
            BComponent oldTarget = (BComponent)pointExt.getParent();
            if (oldTarget == newTarget) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": BACnet write of Object_Property_Reference points to existing extension's parent: " + objPropRef);
               }

               return null;
            }

            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": BACnet changing Object_Property_Reference to " + objPropRef);
            }

            this.updateEventParameters(pointExt);
            this.getEventEnable(pointExt);
            this.getNotificationClass(pointExt);
            this.updateEventMessageTextsConfig(pointExt);
            this.updateEventAlgorithmInhibitInfo(pointExt);
         } else if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": BACnet writing Object_Property_Reference when extension is not yet configured: " + objPropRef);
         }

         this.resetDescriptor();
         ErrorType error = this.configureExt(this.getEventParameter(), objPropRef, null, newTarget);
         if (error != null) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": error configuring alarm ext when writing BACnet objectPropertyReference property: " + objPropRef);
            }
         } else {
            this.setObjectPropertyReference(objPropRef);
         }

         return error;
      }
   }

   private ErrorType writeNotificationClass(byte[] val) throws AsnException {
      int instanceNum = AsnUtil.fromAsnUnsignedInt(val);
      Context context = BLocalBacnetDevice.getBacnetContext();
      ErrorType error = this.configureAlarmClass((BPointExtension)this.getObject(), instanceNum, context);
      if (error == null) {
         this.setInt(notificationClassId, instanceNum, context);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.getObjectId() + ": error in writeNotificationClass");
      }

      return error;
   }

   private static BBacnetNotificationClassDescriptor lookupNotificationClass(int instanceNum) {
      BBacnetObjectIdentifier id = BBacnetObjectIdentifier.make(15, instanceNum);
      return (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(id);
   }

   private ErrorType writeEventEnable(byte[] val) throws AsnException {
      BBacnetBitString eventEnableBits = AsnUtil.fromAsnBitString(val);
      BAlarmTransitionBits alarmEnable = BacnetBitStringUtil.getBAlarmTransitionBits(eventEnableBits);
      if (alarmEnable == null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": could not write the eventEnable property because the alarm transition bits could not be retrieved for value "
                  + eventEnableBits
            );
         }

         return new NErrorType(2, 37);
      } else {
         this.eventEnable = alarmEnable;
         BPointExtension pointExt = (BPointExtension)this.getObject();
         if (pointExt instanceof BAlarmSourceExt) {
            pointExt.set(BAlarmSourceExt.alarmEnable, alarmEnable, BLocalBacnetDevice.getBacnetContext());
         } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
            pointExt.set(BBacnetTrendLogAlarmSourceExt.alarmEnable, alarmEnable, BLocalBacnetDevice.getBacnetContext());
         } else if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": could not write the eventEnable property because the associated point ext is not set or not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt"
            );
         }

         return null;
      }
   }

   private ErrorType writeMessageTextsConfig(int ndx, byte[] val) throws AsnException {
      if (ndx < -1 || ndx > 3) {
         return new NErrorType(2, 42);
      } else if (ndx == 0) {
         return new NErrorType(2, 40);
      } else {
         switch (ndx) {
            case -1:
               BBacnetArray textsConfig = new BBacnetArray(BString.TYPE, 3);
               AsnUtil.fromAsn(-4, val, textsConfig);
               this.toOffnormalText = textsConfig.getElement(1).toString(null);
               this.toFaultText = textsConfig.getElement(2).toString(null);
               this.toNormalText = textsConfig.getElement(3).toString(null);
            case 0:
            default:
               break;
            case 1:
               this.toOffnormalText = AsnUtil.fromAsnCharacterString(val);
               break;
            case 2:
               this.toFaultText = AsnUtil.fromAsnCharacterString(val);
               break;
            case 3:
               this.toNormalText = AsnUtil.fromAsnCharacterString(val);
         }

         BPointExtension pointExt = (BPointExtension)this.getObject();
         if (pointExt instanceof BAlarmSourceExt) {
            BAlarmSourceExt alarmExt = (BAlarmSourceExt)pointExt;
            Context context = BLocalBacnetDevice.getBacnetContext();
            switch (ndx) {
               case -1:
                  alarmExt.set(BAlarmSourceExt.toOffnormalText, BFormat.make(this.toOffnormalText), context);
                  alarmExt.set(BAlarmSourceExt.toFaultText, BFormat.make(this.toFaultText), context);
                  alarmExt.set(BAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), context);
                  resetOutOfRangeTexts(alarmExt);
               case 0:
               default:
                  break;
               case 1:
                  alarmExt.set(BAlarmSourceExt.toOffnormalText, BFormat.make(this.toOffnormalText), context);
                  resetOutOfRangeTexts(alarmExt);
                  break;
               case 2:
                  alarmExt.set(BAlarmSourceExt.toFaultText, BFormat.make(this.toFaultText), context);
                  break;
               case 3:
                  alarmExt.set(BAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), context);
            }
         } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
            BBacnetTrendLogAlarmSourceExt trendAlarmExt = (BBacnetTrendLogAlarmSourceExt)pointExt;
            switch (ndx) {
               case -1:
                  if (!this.toOffnormalText.isEmpty() || !this.toFaultText.isEmpty()) {
                     return new NErrorType(2, 40);
                  }

                  trendAlarmExt.set(BBacnetTrendLogAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), BLocalBacnetDevice.getBacnetContext());
               case 0:
               default:
                  break;
               case 1:
                  if (!this.toOffnormalText.isEmpty()) {
                     return new NErrorType(2, 40);
                  }
                  break;
               case 2:
                  if (!this.toFaultText.isEmpty()) {
                     return new NErrorType(2, 40);
                  }
                  break;
               case 3:
                  trendAlarmExt.set(BBacnetTrendLogAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), BLocalBacnetDevice.getBacnetContext());
            }
         } else if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": could not write the eventMessageTextsConfig property because the associated point ext is not set or not an AlarmSourceExt or BacnetTrendLogAlarmSourceExt"
            );
         }

         return null;
      }
   }

   private ErrorType writeEventAlgorithmInhibit(byte[] val) throws AsnException {
      boolean newValue = AsnUtil.fromAsnBoolean(val);
      if (!this.getEventDetectionEnable()) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": could not write the alarmInhibit property because event detection is disabled");
         }

         return new NErrorType(2, 40);
      } else {
         BPointExtension pointExt = (BPointExtension)this.getObject();
         if (pointExt != null) {
            BLink[] alarmInhibitLinks = getAlarmInhibitLinks(pointExt);
            if (alarmInhibitLinks.length > 0) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": could not write the eventAlgorithmInhibit property because alarmInhibit is linked");
               }

               return new NErrorType(2, 40);
            }

            if (pointExt instanceof BAlarmSourceExt) {
               pointExt.set(BAlarmSourceExt.alarmInhibit, new BStatusBoolean(newValue), BLocalBacnetDevice.getBacnetContext());
            } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
               pointExt.set(BBacnetTrendLogAlarmSourceExt.alarmInhibit, new BStatusBoolean(newValue), BLocalBacnetDevice.getBacnetContext());
            }
         } else if (this.eventAlgorithmInhibitRef.getObjectId().getInstanceNumber() != 4194303) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  this.getObjectId()
                     + ": could not write the eventAlgorithmInhibit property because eventAlgorithmInhibitRef ("
                     + this.eventAlgorithmInhibitRef
                     + ") is configured"
               );
            }

            return new NErrorType(2, 40);
         }

         this.eventAlgorithmInhibit = newValue;
         return null;
      }
   }

   private static BLink[] getAlarmInhibitLinks(BPointExtension pointExt) {
      if (pointExt instanceof BAlarmSourceExt) {
         return pointExt.getLinks(BAlarmSourceExt.alarmInhibit);
      } else {
         return pointExt instanceof BBacnetTrendLogAlarmSourceExt ? pointExt.getLinks(BBacnetTrendLogAlarmSourceExt.alarmInhibit) : EMPTY_LINKS_ARRAY;
      }
   }

   private ErrorType writeEventAlgorithmInhibitRef(byte[] val) throws AsnException {
      BBacnetObjectPropertyReference newObjPropRef = new BBacnetObjectPropertyReference();
      AsnUtil.fromAsn(val, newObjPropRef);
      Context context = BLocalBacnetDevice.getBacnetContext();
      if (newObjPropRef.getObjectId().getInstanceNumber() == 4194303) {
         this.eventAlgorithmInhibitRef = newObjPropRef;
         this.removeAlarmInhibitLinks((BPointExtension)this.getObject(), context);
         return null;
      } else {
         BBooleanPoint sourcePoint;
         try {
            sourcePoint = findEventAlgorithmInhibitSourcePoint(newObjPropRef);
         } catch (BBacnetEventEnrollmentDescriptor.EventEnrollmentException var6) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": error adding eventAlgorithmInhibitRef link; message: " + var6.getMessage());
            }

            return var6.errorType;
         }

         checkLinkPermissions(sourcePoint, "out", context);
         BPointExtension pointExt = (BPointExtension)this.getObject();
         if (pointExt instanceof BAlarmSourceExt) {
            replaceLinks(pointExt, BAlarmSourceExt.alarmInhibit, sourcePoint, context);
         } else if (pointExt instanceof BBacnetTrendLogAlarmSourceExt) {
            replaceLinks(pointExt, BBacnetTrendLogAlarmSourceExt.alarmInhibit, sourcePoint, context);
         }

         this.eventAlgorithmInhibitRef = newObjPropRef;
         return null;
      }
   }

   private void removeAlarmInhibitLinks(BPointExtension pointExt, Context context) {
      if (pointExt != null) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId() + ": removing links to alarmInhibit because eventAlgorithmInhibitRef is set to the unconfigured instance number " + 4194303
            );
         }

         for (BLink link : getAlarmInhibitLinks(pointExt)) {
            pointExt.remove(link.getPropertyInParent(), context);
         }
      }
   }

   private static BBooleanPoint findEventAlgorithmInhibitSourcePoint(BBacnetObjectPropertyReference objPropRef) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      int newObjectType = objPropRef.getObjectId().getObjectType();
      if (newObjectType != 3 && newObjectType != 4 && newObjectType != 5) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "EventAlgorithmInhibitRef is to non-binary object type " + BBacnetObjectType.tag(newObjectType), new NErrorType(2, 37)
         );
      } else {
         BControlPoint sourcePoint;
         try {
            sourcePoint = BacnetDescriptorUtil.findOrAddLocalPoint(objPropRef);
         } catch (Exception var4) {
            logException(
               Level.SEVERE,
               new StringBuilder(objPropRef.getObjectId().toString()).append(": could not resolve the point for objectPropertyReference ").append(objPropRef),
               var4
            );
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               "Could not resolve point for eventAlgorithmInhibitRef " + objPropRef, new NErrorType(2, 37)
            );
         }

         if (!(sourcePoint instanceof BBooleanPoint)) {
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               "eventAlgorithmRef (" + objPropRef + ") point is type " + sourcePoint.getType() + " but should be instanceof BooleanPoint",
               new NErrorType(2, 37)
            );
         } else {
            return (BBooleanPoint)sourcePoint;
         }
      }
   }

   private ErrorType configureExt(
      BBacnetEventParameter eventParam, BBacnetDeviceObjectPropertyReference objPropRef, BPointExtension pointExt, BComponent target
   ) {
      try {
         int eventType = eventParam.getChoice();
         switch (eventType) {
            case 0:
            case 2:
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 12:
            case 13:
            case 18:
            case 19:
            default:
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  "event type " + BBacnetEventType.tag(eventType) + " is not supported", new NErrorType(2, 45)
               );
            case 1:
               this.configureChangeOfStateExt(eventParam, objPropRef, pointExt, target);
               break;
            case 3:
               this.configureCommandFailureExt(eventParam, pointExt, target);
               break;
            case 4:
               this.configureFloatingLimitExt(eventParam, pointExt, target);
               break;
            case 5:
            case 14:
            case 15:
            case 16:
               this.configureOutOfRangeExt(eventParam, pointExt, target);
               break;
            case 10:
               this.configureTrendAlarmExt(eventParam, pointExt, target);
               break;
            case 17:
               this.configureStringChangeOfStateExt(eventParam, pointExt, target);
               break;
            case 20:
               this.configureNoneExt();
               break;
            case 21:
               this.configureChangeOfDiscreteValueExt(eventParam, objPropRef, pointExt, target);
         }

         return null;
      } catch (BBacnetEventEnrollmentDescriptor.EventEnrollmentException var6) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": error configuring alarm ext; message: " + var6.getMessage());
         }

         this.resetDescriptor();
         return var6.errorType;
      } catch (PermissionException var7) {
         logException(Level.INFO, new StringBuilder(this.getObjectId().toString()).append(": permission exception configuring alarm ext"), var7);
         this.resetDescriptor();
         return new NErrorType(2, 40);
      } catch (Exception var8) {
         logException(Level.INFO, new StringBuilder(this.getObjectId().toString()).append(": unexpected error configuring alarm ext"), var8);
         this.resetDescriptor();
         return new NErrorType(2, 0);
      }
   }

   private void configureAlarmExt(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) {
      Context context = BLocalBacnetDevice.getBacnetContext();
      configureTimeDelays(eventParam, alarmExt, context);
      alarmExt.set(BAlarmSourceExt.alarmEnable, this.eventEnable, context);
      this.configureAlarmClass(alarmExt, this.getNotificationClassId(), context);
      alarmExt.set(BAlarmSourceExt.toOffnormalText, BFormat.make(this.toOffnormalText), context);
      alarmExt.set(BAlarmSourceExt.toFaultText, BFormat.make(this.toFaultText), context);
      alarmExt.set(BAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), context);
      this.configureAlarmInhibit(alarmExt, context);
   }

   private static void configureTimeDelays(BBacnetEventParameter eventParam, BAlarmSourceExt ext, Context context) {
      BRelTime timeDelay = BRelTime.makeSeconds(((BBacnetUnsigned)eventParam.get("timeDelay")).getInt());
      ext.set(BAlarmSourceExt.timeDelay, timeDelay, context);
      ext.set(BAlarmSourceExt.timeDelayToNormal, timeDelay, context);
   }

   private ErrorType configureAlarmClass(BPointExtension ext, int instanceNum, Context context) {
      if (instanceNum >= 0 && instanceNum <= 4194303) {
         String alarmClassName;
         if (instanceNum == 4194303) {
            alarmClassName = "";
         } else {
            BBacnetNotificationClassDescriptor descriptor = lookupNotificationClass(instanceNum);
            if (descriptor == null) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(this.getObjectId() + ": configureAlarmClass: cannot find descriptor for notification class instance number " + instanceNum);
               }

               return new NErrorType(2, 37);
            }

            BAlarmClass alarmClass = descriptor.getAlarmClass();
            if (alarmClass == null) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(
                     this.getObjectId()
                        + ": configureAlarmClass: descriptor for notification class instance number "
                        + instanceNum
                        + " could not resolve its alarm class"
                  );
               }

               return new NErrorType(2, 0);
            }

            alarmClassName = alarmClass.getName();
         }

         if (ext instanceof BAlarmSourceExt) {
            ext.setString(BAlarmSourceExt.alarmClass, alarmClassName, context);
         } else if (ext instanceof BBacnetTrendLogAlarmSourceExt) {
            ext.setString(BBacnetTrendLogAlarmSourceExt.alarmClass, alarmClassName, context);
         } else if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": configureAlarmClass: no associated point ext on which to update the alarm class based on notification-class instance number "
                  + instanceNum
            );
         }

         return null;
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": configureAlarmClass: notification class instance number "
                  + instanceNum
                  + " exceeds the maximum allowable instance number value"
            );
         }

         return new NErrorType(2, 37);
      }
   }

   private void configureAlarmInhibit(BPointExtension ext, Context context) {
      boolean isLinked = this.addAlarmInhibitLink(ext, context);
      if (!isLinked) {
         if (ext instanceof BAlarmSourceExt) {
            ((BAlarmSourceExt)ext).setAlarmInhibit(new BStatusBoolean(this.eventAlgorithmInhibit));
         } else if (ext instanceof BBacnetTrendLogAlarmSourceExt) {
            ((BBacnetTrendLogAlarmSourceExt)ext).setAlarmInhibit(new BStatusBoolean(this.eventAlgorithmInhibit));
         }
      }
   }

   private boolean addAlarmInhibitLink(BPointExtension ext, Context context) {
      if (this.eventAlgorithmInhibitRef.getObjectId().getInstanceNumber() == 4194303) {
         this.removeAlarmInhibitLinks(ext, context);
         return false;
      } else {
         try {
            BBooleanPoint sourcePoint = findEventAlgorithmInhibitSourcePoint(this.eventAlgorithmInhibitRef);
            checkLinkPermissions(sourcePoint, "out", context);
            if (ext instanceof BAlarmSourceExt) {
               replaceLinks(ext, BAlarmSourceExt.alarmInhibit, sourcePoint, context);
               return true;
            } else if (ext instanceof BBacnetTrendLogAlarmSourceExt) {
               replaceLinks(ext, BBacnetTrendLogAlarmSourceExt.alarmInhibit, sourcePoint, context);
               return true;
            } else {
               if (logger.isLoggable(Level.WARNING)) {
                  logger.log(Level.WARNING, this.getObjectId() + ": when adding alarm inhibit link, type not supported: " + ext.getType());
               }

               return false;
            }
         } catch (Exception var5) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.FINE, this.getObjectId() + ": error adding eventAlgorithmInhibitRef link", (Throwable)var5);
            }

            return false;
         }
      }
   }

   private void configureGeneralFaultAlgorithm(BBacnetEventParameter eventParam, BAlarmSourceExt ext) {
      BFaultAlgorithm faultAlgorithm = ext.getFaultAlgorithm();
      if (!(ext.getFaultAlgorithm() instanceof BFaultAlgorithm)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing fault algorithm of type "
                  + faultAlgorithm.getType()
                  + " with FaultAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         ext.set(BAlarmSourceExt.faultAlgorithm, new BFaultAlgorithm(), BLocalBacnetDevice.getBacnetContext());
      }
   }

   private BAlarmSourceExt updateToAlarmExt(BPointExtension pointExt) {
      if (!(pointExt instanceof BAlarmSourceExt)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this.getObjectId() + ": replacing point extension of type " + (pointExt != null ? pointExt.getType() : null) + " with BAlarmExt");
         }

         return new BAlarmSourceExt();
      } else {
         return (BAlarmSourceExt)pointExt;
      }
   }

   private BBacnetTrendLogAlarmSourceExt updateToTrendAlarmExt(BPointExtension pointExt) {
      if (!(pointExt instanceof BBacnetTrendLogAlarmSourceExt)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing point extension of type "
                  + (pointExt != null ? pointExt.getType() : null)
                  + " with BBacnetTrendLogAlarmSourceExt"
            );
         }

         return new BBacnetTrendLogAlarmSourceExt();
      } else {
         return (BBacnetTrendLogAlarmSourceExt)pointExt;
      }
   }

   private static void addExtIfMissing(BPointExtension pointExt, BComponent target) {
      if (pointExt.getParent() == null) {
         target.add("EventEnrollmentAlarmExt?", pointExt, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureNoneExt() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this.getObjectId() + ": resetting the descriptor because event type is none");
      }

      this.resetDescriptor();
   }

   private void configureChangeOfStateExt(
      BBacnetEventParameter eventParam, BBacnetDeviceObjectPropertyReference objPropRef, BPointExtension pointExt, BComponent target
   ) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      checkChangeOfStateTarget(objPropRef, target);
      BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
      this.configureAlarmExt(eventParam, alarmExt);
      if (target instanceof BBooleanPoint) {
         this.configureBooleanChangeOfStateOffnormal(eventParam, alarmExt);
      } else if (target instanceof BNumericPoint) {
         this.configureNumericChangeOfStateOffnormal(eventParam, alarmExt, (BNumericPoint)target);
      } else {
         this.configureEnumChangeOfStateOffnormal(eventParam, alarmExt, (BEnumPoint)target);
      }

      this.configureGeneralFaultAlgorithm(eventParam, alarmExt);
      addExtIfMissing(alarmExt, target);
      this.updateDescriptor(alarmExt);
   }

   private static void checkChangeOfStateTarget(BBacnetDeviceObjectPropertyReference objPropRef, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BBooleanPoint) && !(target instanceof BEnumPoint) && !(target instanceof BNumericPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "referenced object is of type "
               + target.getType()
               + " and not instanceof BooleanPoint or EnumPoint or NumericPoint, which is required for change-of-state extensions",
            new NErrorType(2, 37)
         );
      } else {
         PropertyInfo propInfo = getPropertyInfo(objPropRef.getObjectId().getObjectType(), objPropRef.getPropertyId());
         if (propInfo == null) {
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
               "BACnet property information not found for object ID: "
                  + objPropRef.getObjectId()
                  + ", property ID: "
                  + BBacnetPropertyIdentifier.tag(objPropRef.getPropertyId()),
               new NErrorType(2, 37)
            );
         } else if (!propInfo.isArray() || objPropRef.getPropertyArrayIndex() != 0) {
            switch (propInfo.getAsnType()) {
               case 1:
               case 2:
               case 3:
               case 9:
                  return;
               case 4:
               case 5:
               case 6:
               case 7:
               case 8:
               default:
                  throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                     "change-of-state alarm extension is not supported for the property data type: "
                        + AsnUtil.getAsnTypeName(propInfo.getAsnType())
                        + "; object type: "
                        + BBacnetObjectType.tag(objPropRef.getObjectId().getObjectType())
                        + ", property ID: "
                        + BBacnetPropertyIdentifier.tag(objPropRef.getPropertyId()),
                     new NErrorType(2, 37)
                  );
            }
         }
      }
   }

   private void configureBooleanChangeOfStateOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BBooleanChangeOfStateAlgorithm) {
         configureBooleanChangeOfStateOffnormal(eventParam, (BBooleanChangeOfStateAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with BooleanChangeOfStateAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BBooleanChangeOfStateAlgorithm changeOfStateAlgorithm = new BBooleanChangeOfStateAlgorithm();
         configureBooleanChangeOfStateOffnormal(eventParam, changeOfStateAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, changeOfStateAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private static void configureBooleanChangeOfStateOffnormal(BBacnetEventParameter eventParam, BBooleanChangeOfStateAlgorithm algorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BBacnetListOf listOfValues = (BBacnetListOf)eventParam.get("listOfValues");
      BBacnetPropertyStates[] propStates = (BBacnetPropertyStates[])listOfValues.getChildren(BBacnetPropertyStates.class);
      if (propStates.length < 1) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "boolean change-of-state alarm extensions require at least 1 alarm value; event type: " + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         boolean alarmValue;
         switch (propStates[0].getChoice()) {
            case 0:
               alarmValue = ((BBoolean)propStates[0].getValue()).getBoolean();
               break;
            case 1:
               alarmValue = ((BBacnetBinaryPv)propStates[0].getValue()).isActive();
               break;
            default:
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  "boolean change-of-state alarm extensions require a BOOLEAN (0) or BACnetBinaryPv (1) value; found: "
                     + propStates[0].getChoice()
                     + ", event type: "
                     + BBacnetEventType.tag(eventParam.getChoice()),
                  new NErrorType(2, 37)
               );
         }

         algorithm.setBoolean(BBooleanChangeOfStateAlgorithm.alarmValue, alarmValue, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureNumericChangeOfStateOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt, BNumericPoint point) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BNumericChangeOfStateAlgorithm) {
         BEnumRange alarmValues = getChangeOfStateAlarmValues(eventParam, point);
         offnormalAlgorithm.set(BNumericChangeOfStateAlgorithm.alarmValues, alarmValues, BLocalBacnetDevice.getBacnetContext());
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with NumericChangeOfStateAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BNumericChangeOfStateAlgorithm changeOfStateAlgorithm = new BNumericChangeOfStateAlgorithm();
         changeOfStateAlgorithm.setAlarmValues(getChangeOfStateAlarmValues(eventParam, point));
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, changeOfStateAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureEnumChangeOfStateOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt, BEnumPoint point) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BEnumChangeOfStateAlgorithm) {
         BEnumRange alarmValues = getChangeOfStateAlarmValues(eventParam, point);
         offnormalAlgorithm.set(BEnumChangeOfStateAlgorithm.alarmValues, alarmValues, BLocalBacnetDevice.getBacnetContext());
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with EnumChangeOfStateAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BEnumChangeOfStateAlgorithm changeOfStateAlgorithm = new BEnumChangeOfStateAlgorithm();
         changeOfStateAlgorithm.setAlarmValues(getChangeOfStateAlarmValues(eventParam, point));
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, changeOfStateAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private static BEnumRange getChangeOfStateAlarmValues(BBacnetEventParameter eventParam, BControlPoint point) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BBacnetListOf listOfValues = (BBacnetListOf)eventParam.get("listOfValues");
      BBacnetPropertyStates[] propStates = (BBacnetPropertyStates[])listOfValues.getChildren(BBacnetPropertyStates.class);
      BEnumRange range = (BEnumRange)point.getFacets().get("range");
      int[] ordinals = new int[propStates.length];
      String[] tags = new String[propStates.length];

      for (int i = 0; i < propStates.length; i++) {
         BValue value = propStates[i].getValue();
         if (value instanceof BEnum) {
            ordinals[i] = ((BEnum)value).getOrdinal();
         } else if (value instanceof BBacnetUnsigned) {
            ordinals[i] = ((BBacnetUnsigned)value).getInt();
         } else {
            if (!(value instanceof BInteger)) {
               throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
                  "change-of-state alarm extension is not supported for the property state: "
                     + propStates[i]
                     + ", event type: "
                     + BBacnetEventType.tag(eventParam.getChoice()),
                  new NErrorType(2, 37)
               );
            }

            ordinals[i] = ((BInteger)value).getInt();
         }

         tags[i] = range != null ? SlotPath.escape(range.getTag(ordinals[i])) : SlotPath.escape(String.valueOf(ordinals[i]));
      }

      return BEnumRange.make(ordinals, tags);
   }

   private void configureCommandFailureExt(BBacnetEventParameter eventParam, BPointExtension pointExt, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BBooleanPoint) && !(target instanceof BEnumPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "referenced object is of type "
               + target.getType()
               + " and not instanceof BooleanPoint or EnumPoint, which is required for command failure extensions; event type: "
               + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
         this.configureAlarmExt(eventParam, alarmExt);
         if (target instanceof BBooleanPoint) {
            this.configureBooleanCommandFailureOffnormal(eventParam, alarmExt);
            this.configureGeneralFaultAlgorithm(eventParam, alarmExt);
         } else {
            this.configureEnumCommandFailureOffnormal(eventParam, alarmExt);
            this.configureEnumCommandFailureFault(eventParam, alarmExt);
         }

         addExtIfMissing(alarmExt, target);
         this.updateDescriptor(alarmExt);
      }
   }

   private void configureBooleanCommandFailureOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BBooleanCommandFailureAlgorithm) {
         this.configureBooleanCommandFailureOffnormal(eventParam, (BBooleanCommandFailureAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with BooleanCommandFailureAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BBooleanCommandFailureAlgorithm commandFailureAlgorithm = new BBooleanCommandFailureAlgorithm();
         this.configureBooleanCommandFailureOffnormal(eventParam, commandFailureAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, commandFailureAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureBooleanCommandFailureOffnormal(BBacnetEventParameter eventParam, BBooleanCommandFailureAlgorithm algorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BControlPoint feedbackPoint = this.getCommandFailureFeedbackPoint(eventParam);
      if (!(feedbackPoint instanceof BBooleanPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "feedback point for boolean command failure is type " + feedbackPoint.getType() + " but should be instanceof BooleanPoint", new NErrorType(2, 37)
         );
      } else {
         Context context = BLocalBacnetDevice.getBacnetContext();
         checkLinkPermissions(feedbackPoint, "out", context);
         replaceLinks(algorithm, BBooleanCommandFailureAlgorithm.feedbackValue, feedbackPoint, context);
      }
   }

   private static void replaceLinks(BComponent target, Property targetSlot, BComponent source, Context context) {
      BLink existingLink = null;

      for (BLink link : target.getLinks(targetSlot)) {
         if (link.getSourceOrd().equals(source.getHandleOrd()) && link.getSourceSlotName().equals("out") && existingLink == null) {
            existingLink = link;
         } else {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(
                  "Before adding a link to feedback point, cleared link to source ord " + link.getSourceOrd() + " and source slot " + link.getSourceSlotName()
               );
            }

            target.remove(link.getPropertyInParent(), context);
         }
      }

      if (existingLink == null) {
         target.add(null, new BLink(source.getHandleOrd(), "out", targetSlot.getName(), true), context);
      }
   }

   private BBacnetDeviceObjectPropertyReference getLinkedPropertyReference(BComponent component, Property targetSlot, Type targetDescType) {
      BLink[] targetSlotLinks = component.getLinks(targetSlot);

      for (BLink link : targetSlotLinks) {
         if (link.getSourceSlotName().equals("out") && link.isActive() && link.isEnabled()) {
            BComponent source = link.getSourceComponent();
            if (source instanceof BControlPoint) {
               BAbstractProxyExt proxyExt = ((BControlPoint)source).getProxyExt();
               if (proxyExt instanceof BBacnetProxyExt) {
                  return makeRemoteDeviceObjPropRef((BBacnetProxyExt)proxyExt);
               }
            }

            BIBacnetExportObject descriptor = this.findDescriptor(source.getHandleOrd());
            if (descriptor != null && descriptor.getType().is(targetDescType)) {
               return makeLocalDeviceObjPropRef(descriptor.getObjectId());
            }
         }
      }

      return makeUnconfiguredDeviceObjPropRef();
   }

   private static BBacnetDeviceObjectPropertyReference makeUnconfiguredDeviceObjPropRef() {
      return new BBacnetDeviceObjectPropertyReference(BBacnetObjectIdentifier.make(0, 4194303), 85, -1, BBacnetObjectIdentifier.make(8, 4194303));
   }

   private static BBacnetDeviceObjectPropertyReference makeLocalDeviceObjPropRef(BBacnetObjectIdentifier objectId) {
      return new BBacnetDeviceObjectPropertyReference(objectId, 85, -1, BBacnetNetwork.localDevice().getObjectId());
   }

   private static BBacnetDeviceObjectPropertyReference makeRemoteDeviceObjPropRef(BBacnetProxyExt proxyExt) {
      return new BBacnetDeviceObjectPropertyReference(
         proxyExt.getObjectId(), proxyExt.getPropertyId().getOrdinal(), proxyExt.getPropertyArrayIndex(), proxyExt.device().getObjectId()
      );
   }

   private static void checkLinkPermissions(BComponent source, String sourceSlotName, Context context) {
      BUser user = context != null ? context.getUser() : null;
      if (user != null) {
         user.checkWrite(source, source.getSlot(sourceSlotName));
      }
   }

   private void configureEnumCommandFailureOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BEnumCommandFailureAlgorithm) {
         this.configureEnumCommandFailureOffnormal(eventParam, (BEnumCommandFailureAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with EnumCommandFailureAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BEnumCommandFailureAlgorithm commandFailureAlgorithm = new BEnumCommandFailureAlgorithm();
         this.configureEnumCommandFailureOffnormal(eventParam, commandFailureAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, commandFailureAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureEnumCommandFailureOffnormal(BBacnetEventParameter eventParam, BEnumCommandFailureAlgorithm algorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BControlPoint feedbackPoint = this.getCommandFailureFeedbackPoint(eventParam);
      if (!(feedbackPoint instanceof BEnumPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "feedback point for enum command failure is type " + feedbackPoint.getType() + " but should be instanceof EnumPoint", new NErrorType(2, 37)
         );
      } else {
         Context context = BLocalBacnetDevice.getBacnetContext();
         checkLinkPermissions(feedbackPoint, "out", context);
         replaceLinks(algorithm, BEnumCommandFailureAlgorithm.feedbackValue, feedbackPoint, context);
      }
   }

   private void configureEnumCommandFailureFault(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) {
      BFaultAlgorithm faultAlgorithm = alarmExt.getFaultAlgorithm();
      if (!(alarmExt.getFaultAlgorithm() instanceof BEnumFaultAlgorithm)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing fault algorithm of type "
                  + faultAlgorithm.getType()
                  + " with EnumFaultAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         alarmExt.set(BAlarmSourceExt.faultAlgorithm, new BEnumFaultAlgorithm(), BLocalBacnetDevice.getBacnetContext());
      }
   }

   private BControlPoint getCommandFailureFeedbackPoint(BBacnetEventParameter eventParam) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BValue feedbackRef = eventParam.get("feedbackPropertyReference");
      if (!(feedbackRef instanceof BBacnetDeviceObjectPropertyReference)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "feedback reference for command failure is type "
               + (feedbackRef != null ? feedbackRef.getType() : null)
               + " but should be instanceof BacnetDeviceObjectPropertyReference",
            new NErrorType(2, 37)
         );
      } else {
         try {
            return BacnetDescriptorUtil.findOrAddPoint((BBacnetDeviceObjectPropertyReference)feedbackRef);
         } catch (Exception var4) {
            logException(Level.FINE, new StringBuilder(this.getObjectId().toString()).append(": error finding point for command failure feedback ref"), var4);
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException("error finding point for command failure feedback ref", new NErrorType(2, 37));
         }
      }
   }

   private void configureFloatingLimitExt(BBacnetEventParameter eventParam, BPointExtension pointExt, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BNumericPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "referenced object is of type "
               + target.getType()
               + " and not instanceof NumericPoint, which is required for Floating Limit Algorithm extensions; event type: "
               + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
         this.configureAlarmExt(eventParam, alarmExt);
         this.configureFloatingLimitOffnormal(eventParam, alarmExt);
         this.configureGeneralFaultAlgorithm(eventParam, alarmExt);
         addExtIfMissing(alarmExt, target);
         this.updateDescriptor(alarmExt);
      }
   }

   private void configureFloatingLimitOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BFloatingLimitAlgorithm) {
         this.configureFloatingLimitOffnormal(eventParam, (BFloatingLimitAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with BFloatingLimitAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BFloatingLimitAlgorithm floatingLimitAlgorithm = new BFloatingLimitAlgorithm();
         this.configureFloatingLimitOffnormal(eventParam, floatingLimitAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, floatingLimitAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureFloatingLimitOffnormal(BBacnetEventParameter eventParam, BFloatingLimitAlgorithm algorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BControlPoint setpoint = this.getFloatingLimitSetpoint(eventParam);
      if (!(setpoint instanceof BNumericPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "feedback point for floating limit is type " + setpoint.getType() + " but should be instanceof BNumericPoint", new NErrorType(2, 37)
         );
      } else {
         Context context = BLocalBacnetDevice.getBacnetContext();
         checkLinkPermissions(setpoint, "out", context);
         replaceLinks(algorithm, BFloatingLimitAlgorithm.setpoint, setpoint, context);
         algorithm.setDouble(BFloatingLimitAlgorithm.lowDiffLimit, ((BNumber)eventParam.get("lowDiffLimit")).getDouble(), context);
         algorithm.setDouble(BFloatingLimitAlgorithm.highDiffLimit, ((BNumber)eventParam.get("highDiffLimit")).getDouble(), context);
         algorithm.setDouble(BFloatingLimitAlgorithm.deadband, ((BNumber)eventParam.get("deadband")).getDouble(), context);
         BLimitEnable limitEnable = algorithm.getLimitEnable();
         limitEnable.setBoolean(BLimitEnable.highLimitEnable, true, context);
         limitEnable.setBoolean(BLimitEnable.lowLimitEnable, true, context);
      }
   }

   private BControlPoint getFloatingLimitSetpoint(BBacnetEventParameter eventParam) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BValue setpointRef = eventParam.get("setpointReference");
      if (!(setpointRef instanceof BBacnetDeviceObjectPropertyReference)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "setpoint reference for Floating Limit is type "
               + (setpointRef != null ? setpointRef.getType() : null)
               + " but should be instanceof BacnetDeviceObjectPropertyReference",
            new NErrorType(2, 37)
         );
      } else {
         try {
            return BacnetDescriptorUtil.findOrAddPoint((BBacnetDeviceObjectPropertyReference)setpointRef);
         } catch (Exception var4) {
            logException(Level.WARNING, new StringBuilder(this.getObjectId().toString()).append(": error finding point for floating limit setpoint ref"), var4);
            throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException("error finding point for floating limit setpoint ref", new NErrorType(2, 37));
         }
      }
   }

   private void configureOutOfRangeExt(BBacnetEventParameter eventParam, BPointExtension pointExt, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BNumericPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "referenced object is of type "
               + target.getType()
               + " and not instanceof NumericPoint, which is required for out-of-range extensions; event type: "
               + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
         this.configureAlarmExt(eventParam, alarmExt);
         this.configureOutOfRangeOffnormal(eventParam, alarmExt);
         this.configureOutOfRangeFault(eventParam, alarmExt);
         addExtIfMissing(alarmExt, target);
         this.updateDescriptor(alarmExt);
      }
   }

   private void configureOutOfRangeOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt ext) {
      BOffnormalAlgorithm offnormalAlgorithm = ext.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BOutOfRangeAlgorithm) {
         configureOutOfRangeOffnormal(eventParam, (BOutOfRangeAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with OutOfRangeAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BOutOfRangeAlgorithm outOfRangeAlgorithm = new BOutOfRangeAlgorithm();
         configureOutOfRangeOffnormal(eventParam, outOfRangeAlgorithm);
         ext.set(BAlarmSourceExt.offnormalAlgorithm, outOfRangeAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private static void configureOutOfRangeOffnormal(BBacnetEventParameter eventParam, BOutOfRangeAlgorithm algorithm) {
      Context context = BLocalBacnetDevice.getBacnetContext();
      algorithm.setDouble(BOutOfRangeAlgorithm.lowLimit, ((BNumber)eventParam.get("lowLimit")).getDouble(), context);
      algorithm.setDouble(BOutOfRangeAlgorithm.highLimit, ((BNumber)eventParam.get("highLimit")).getDouble(), context);
      algorithm.setDouble(BOutOfRangeAlgorithm.deadband, ((BNumber)eventParam.get("deadband")).getDouble(), context);
      BLimitEnable limitEnable = algorithm.getLimitEnable();
      limitEnable.setBoolean(BLimitEnable.highLimitEnable, true, context);
      limitEnable.setBoolean(BLimitEnable.lowLimitEnable, true, context);
   }

   private void configureOutOfRangeFault(BBacnetEventParameter eventParam, BAlarmSourceExt ext) {
      BFaultAlgorithm faultAlgorithm = ext.getFaultAlgorithm();
      if (!(ext.getFaultAlgorithm() instanceof BOutOfRangeFaultAlgorithm)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing fault algorithm of type "
                  + faultAlgorithm.getType()
                  + " with OutOfRangeFaultAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         ext.set(BAlarmSourceExt.faultAlgorithm, new BOutOfRangeFaultAlgorithm(), BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureTrendAlarmExt(BBacnetEventParameter eventParam, BPointExtension pointExt, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BIBacnetTrendLogExt)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "target is of type "
               + target.getType()
               + " and not instanceof BIBacnetTrendLogExt, which is required for trend log alarm source extensions; event type: "
               + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         BBacnetTrendLogAlarmSourceExt trendAlarmExt = this.updateToTrendAlarmExt(pointExt);
         addExtIfMissing(trendAlarmExt, target);
         Context context = BLocalBacnetDevice.getBacnetContext();
         trendAlarmExt.set(BAlarmSourceExt.alarmEnable, this.eventEnable, context);
         this.configureAlarmClass(trendAlarmExt, this.getNotificationClassId(), context);
         trendAlarmExt.set(BBacnetTrendLogAlarmSourceExt.toNormalText, BFormat.make(this.toNormalText), context);
         trendAlarmExt.updateParameters(
            getLongParameter(eventParam, "notificationThreshold"), getLongParameter(eventParam, "previousNotificationCount"), context
         );
         this.configureAlarmInhibit(trendAlarmExt, context);
         this.updateDescriptor(trendAlarmExt);
      }
   }

   private static long getLongParameter(BBacnetEventParameter eventParam, String slotName) {
      return ((BBacnetUnsigned)eventParam.get(slotName)).getLong();
   }

   private void configureStringChangeOfStateExt(BBacnetEventParameter eventParam, BPointExtension pointExt, BComponent target) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      if (!(target instanceof BStringPoint)) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "referenced object is of type "
               + target.getType()
               + " and not instanceof StringPoint, which is required for String change-of-state extensions; event type: "
               + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
         this.configureAlarmExt(eventParam, alarmExt);
         this.configureStringChangeOfStateOffnormal(eventParam, alarmExt);
         this.configureStringChangeOfStateFault(eventParam, alarmExt);
         addExtIfMissing(alarmExt, target);
         this.updateDescriptor(alarmExt);
      }
   }

   private void configureStringChangeOfStateOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BStringChangeOfStateAlgorithm) {
         configureStringChangeOfStateOffnormal(eventParam, (BStringChangeOfStateAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with StringChangeOfStateAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BStringChangeOfStateAlgorithm stringChangeOfStateAlgorithm = new BStringChangeOfStateAlgorithm();
         configureStringChangeOfStateOffnormal(eventParam, stringChangeOfStateAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, stringChangeOfStateAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private static void configureStringChangeOfStateOffnormal(BBacnetEventParameter eventParam, BStringChangeOfStateAlgorithm algorithm) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      BBacnetListOf listOfValues = (BBacnetListOf)eventParam.get("listOfAlarmValues");
      BString[] alarmValues = (BString[])listOfValues.getChildren(BString.class);
      if (alarmValues.length < 1) {
         throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
            "String change-of-state alarm extensions require at least 1 alarm value; event type: " + BBacnetEventType.tag(eventParam.getChoice()),
            new NErrorType(2, 37)
         );
      } else {
         String alarmValue = alarmValues[0].getString();
         Context context = BLocalBacnetDevice.getBacnetContext();
         algorithm.setString(BStringChangeOfStateAlgorithm.expression, alarmValue, context);
         algorithm.setBoolean(BStringChangeOfStateAlgorithm.normalOnMatch, false, context);
      }
   }

   private void configureStringChangeOfStateFault(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) {
      BFaultAlgorithm faultAlgorithm = alarmExt.getFaultAlgorithm();
      if (!(alarmExt.getFaultAlgorithm() instanceof BStringChangeOfStateFaultAlgorithm)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing fault algorithm of type "
                  + faultAlgorithm.getType()
                  + " with StringChangeOfStateFaultAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         alarmExt.set(BAlarmSourceExt.faultAlgorithm, new BStringChangeOfStateFaultAlgorithm(), BLocalBacnetDevice.getBacnetContext());
      }
   }

   private void configureChangeOfStatusFlagsExt(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt, BComponent target) {
      alarmExt = alarmExt == null ? new BAlarmSourceExt() : alarmExt;
      this.configureAlarmExt(eventParam, alarmExt);
      this.configureChangeOfStatusFlagsOffnormal(eventParam, alarmExt);
      this.configureGeneralFaultAlgorithm(eventParam, alarmExt);
      addExtIfMissing(alarmExt, target);
      this.updateDescriptor(alarmExt);
   }

   private void configureChangeOfStatusFlagsOffnormal(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (offnormalAlgorithm instanceof BBacnetStatusAlgorithm) {
         configureChangeOfStatusFlagsOffnormal(eventParam, (BBacnetStatusAlgorithm)offnormalAlgorithm);
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with BBacnetStatusAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BBacnetStatusAlgorithm changeOfStatusFlagsAlgorithm = new BBacnetStatusAlgorithm();
         configureChangeOfStatusFlagsOffnormal(eventParam, changeOfStatusFlagsAlgorithm);
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, changeOfStatusFlagsAlgorithm, BLocalBacnetDevice.getBacnetContext());
      }
   }

   private static void configureChangeOfStatusFlagsOffnormal(BBacnetEventParameter eventParam, BBacnetStatusAlgorithm algorithm) {
      algorithm.set(BBacnetStatusAlgorithm.alarmValues, eventParam.get("statusFlags"), BLocalBacnetDevice.getBacnetContext());
   }

   private void configureChangeOfDiscreteValueExt(
      BBacnetEventParameter eventParam, BBacnetDeviceObjectPropertyReference objPropRef, BPointExtension pointExt, BComponent target
   ) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      checkChangeOfDiscreteValueTarget(objPropRef);
      BAlarmSourceExt alarmExt = this.updateToAlarmExt(pointExt);
      this.configureAlarmExt(eventParam, alarmExt);
      this.configureChangeOfDiscreteValue(eventParam, alarmExt);
      addExtIfMissing(alarmExt, target);
      this.updateDescriptor(alarmExt);
   }

   private static void checkChangeOfDiscreteValueTarget(BBacnetDeviceObjectPropertyReference objPropRef) throws BBacnetEventEnrollmentDescriptor.EventEnrollmentException {
      PropertyInfo propInfo = getPropertyInfo(objPropRef.getObjectId().getObjectType(), objPropRef.getPropertyId());
      if (propInfo != null) {
         switch (propInfo.getAsnType()) {
            case -1:
               if (propInfo.getType().equals(BBacnetDateTime.TYPE.getTypeSpec().toString())) {
                  return;
               }
            case 0:
            case 4:
            case 5:
            case 8:
            default:
               break;
            case 1:
            case 2:
            case 3:
            case 6:
            case 7:
            case 9:
            case 10:
            case 11:
            case 12:
               return;
         }
      }

      throw new BBacnetEventEnrollmentDescriptor.EventEnrollmentException(
         "Change_of_Discrete_Value extension only supports the following data types: Boolean, Unsigned, Integer, Enumerated, CharacterString, OctetString, Date, Time, BACnetObjectIdentifier, BACnetDateTime;; objectPropertyReference: "
            + objPropRef
            + ", property info: "
            + propInfo,
         new NErrorType(2, 37)
      );
   }

   private void configureChangeOfDiscreteValue(BBacnetEventParameter eventParam, BAlarmSourceExt alarmExt) {
      BOffnormalAlgorithm offnormalAlgorithm = alarmExt.getOffnormalAlgorithm();
      if (!(offnormalAlgorithm instanceof BBacnetChangeOfDiscreteValueAlgorithm)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(
               this.getObjectId()
                  + ": replacing offnormal algorithm of type "
                  + offnormalAlgorithm.getType()
                  + " with ChangeOfDiscreteValueAlgorithm for event type "
                  + BBacnetEventType.tag(eventParam.getChoice())
            );
         }

         BOffnormalAlgorithm var4 = new BBacnetChangeOfDiscreteValueAlgorithm();
         alarmExt.set(BAlarmSourceExt.offnormalAlgorithm, var4, BLocalBacnetDevice.getBacnetContext());
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetEventEnrollmentDescriptor", 2);
      out.prop("pointExt", this.pointExt);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("configOk", this.configOk);
      out.prop("duplicate", this.duplicate);
      out.prop("typeOfEvent", this.getTypeOfEvent());
      out.prop("notificationClass", this.getNotificationClass());
      out.endProps();
   }

   private static boolean isArray(int propId) {
      for (int arrayPropId : ARRAY_PROPS) {
         if (propId == arrayPropId) {
            return true;
         }
      }

      return false;
   }

   private BComponent resolveTarget(BBacnetDeviceObjectPropertyReference objPropRef) {
      if (!BacnetDescriptorUtil.isValid(objPropRef)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this + ": objectPropertyReference (" + objPropRef + ") is not valid");
         }

         return null;
      } else {
         try {
            BComponent target;
            if (objPropRef.getObjectId().getObjectType() == 20) {
               if (!BacnetDescriptorUtil.isLocalDevice(objPropRef.getDeviceId().getInstanceNumber())) {
                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this + ": references to Trend Log objects not supported on remote devices; objectPropertyReference: " + objPropRef);
                  }

                  return null;
               }

               target = BacnetDescriptorUtil.findLocalObject(objPropRef.getObjectId());
            } else {
               target = BacnetDescriptorUtil.findOrAddPoint(objPropRef);
            }

            if (target == null && logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": did not resolve objectPropertyReference " + objPropRef);
            }

            return target;
         } catch (Exception var3) {
            logException(
               Level.SEVERE,
               new StringBuilder(this.getObjectId().toString()).append(": could not resolve target for objectPropertyReference ").append(objPropRef),
               var3
            );
            return null;
         }
      }
   }

   private void updateDescriptor(BPointExtension pointExt) {
      this.pointExt = pointExt;
      Context context = BLocalBacnetDevice.getBacnetContext();
      this.set(eventEnrollmentOrd, pointExt.getHandleOrd(), context);
      if (this.getNotificationClass(pointExt) == null) {
         this.set(reliability, BBacnetReliability.configurationError, context);
      } else {
         this.set(reliability, BBacnetReliability.noFaultDetected, context);
      }
   }

   private void resetDescriptor() {
      BPointExtension pointExt = this.pointExt;
      if (pointExt != null) {
         BComplex parent = pointExt.getParent();
         if (parent instanceof BComponent) {
            ((BComponent)parent).remove(pointExt);
         }
      }

      this.pointExt = null;
      Context context = BLocalBacnetDevice.getBacnetContext();
      this.set(eventEnrollmentOrd, BOrd.NULL, context);
      this.set(reliability, BBacnetReliability.configurationError, context);
   }

   private static void logException(Level level, StringBuilder message, Exception e) {
      if (logger.isLoggable(Level.FINE)) {
         logger.log(Level.FINE, message.append("; exception: ").append(e.getLocalizedMessage()).toString(), (Throwable)e);
      } else if (logger.isLoggable(level)) {
         logger.log(level, message.append("; exception: ").append(e.getLocalizedMessage()).toString());
      }
   }

   private static PropertyInfo getPropertyInfo(int objectType, int propertyId) {
      return ObjectTypeList.getInstance().getPropertyInfo(objectType, propertyId);
   }

   private static class EventEnrollmentException extends Exception {
      final ErrorType errorType;

      public EventEnrollmentException(String message, ErrorType errorType) {
         super(message);
         this.errorType = errorType;
      }
   }
}
