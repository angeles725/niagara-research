package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.ObjectTypeList;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NBacnetPropertyValue;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.history.BBacnetActivePeriod;
import com.tridium.bacnet.history.BBacnetNumericTrendLogExt;
import com.tridium.bacnet.history.BBacnetNumericTrendLogRemoteExt;
import com.tridium.bacnet.history.BBacnetTrendLogAlarmSourceExt;
import com.tridium.bacnet.history.BBacnetTrendLogRemoteExt;
import com.tridium.bacnet.history.BIBacnetTrendLogExt;
import com.tridium.bacnet.history.BacnetTrendLogUtil;
import com.tridium.bacnet.services.BacnetConfirmedRequest;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.services.error.NChangeListError;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetClientCov;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetLoggingType;
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
import javax.baja.bacnet.point.BBacnetTuningPolicy;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.history.BCapacity;
import javax.baja.history.BCollectionInterval;
import javax.baja.history.BFullPolicy;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BHistoryId;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.db.BHistoryDatabase;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.history.ext.BCovHistoryExt;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.history.ext.BIntervalHistoryExt;
import javax.baja.history.ext.BNumericCovHistoryExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusNumeric;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;

@NiagaraType(
   agent = {@AgentOn(
      types = {"bacnet:IBacnetTrendLogExt"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "logOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 65,
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"baja:Component\""
      )}
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.TREND_LOG)",
      flags = 64
   ), @NiagaraProperty(
      name = "historyOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 69
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "logDeviceObjectPropertyReference",
      type = "BBacnetDeviceObjectPropertyReference",
      defaultValue = "new BBacnetDeviceObjectPropertyReference()",
      flags = 5
   ), @NiagaraProperty(
      name = "covResubscriptionInterval",
      type = "int",
      defaultValue = "5"
   ), @NiagaraProperty(
      name = "reliability",
      type = "BBacnetReliability",
      defaultValue = "BBacnetReliability.configurationError",
      flags = 5
   ), @NiagaraProperty(
      name = "clientCovIncrement",
      type = "BBacnetClientCov",
      defaultValue = "new BBacnetClientCov()"
   )})
@NiagaraAction(
   name = "historyUpdated",
   flags = 20
)
public class BBacnetTrendLogDescriptor extends BBacnetEventSource implements BacnetPropertyListProvider {
   public static final Property logOrd = newProperty(65, BOrd.DEFAULT, BFacets.make("targetType", "baja:Component"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(20), null);
   public static final Property historyOrd = newProperty(69, BOrd.DEFAULT, null);
   public static final Property objectName = newProperty(0, "", null);
   public static final Property description = newProperty(0, "", null);
   public static final Property logDeviceObjectPropertyReference = newProperty(5, new BBacnetDeviceObjectPropertyReference(), null);
   public static final Property covResubscriptionInterval = newProperty(0, 5, null);
   public static final Property reliability = newProperty(5, BBacnetReliability.configurationError, null);
   public static final Property clientCovIncrement = newProperty(0, new BBacnetClientCov(), null);
   public static final Action historyUpdated = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetTrendLogDescriptor.class);
   private static final int[] ARRAY_PROPS = new int[]{130, 351, 352, 371};
   private static final BIcon icon = BIcon.make(BIcon.std("history.png"), BIcon.std("badges/export.png"));
   private int[] optionalProps;
   private BBacnetTrendLogDescriptor.BacnetTrendLogSubscriber logSubscriber;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private static final BBacnetDeviceObjectPropertyReference NULL_DOPR = new BBacnetDeviceObjectPropertyReference();
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 133, 144, 126, 131, 141, 145, 36, 197, 111};
   private BIBacnetTrendLogExt tlog;
   private BComponent targetPoint;
   private final AtomicBoolean isBusy = new AtomicBoolean();
   private final AtomicBoolean isWaiting = new AtomicBoolean();
   private static final Logger logger = Logger.getLogger("bacnet.export.object.trendlog");

   public BOrd getLogOrd() {
      return (BOrd)this.get(logOrd);
   }

   public void setLogOrd(BOrd v) {
      this.set(logOrd, v, null);
   }

   @Override
   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   @Override
   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public BOrd getHistoryOrd() {
      return (BOrd)this.get(historyOrd);
   }

   public void setHistoryOrd(BOrd v) {
      this.set(historyOrd, v, null);
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

   public BBacnetDeviceObjectPropertyReference getLogDeviceObjectPropertyReference() {
      return (BBacnetDeviceObjectPropertyReference)this.get(logDeviceObjectPropertyReference);
   }

   public void setLogDeviceObjectPropertyReference(BBacnetDeviceObjectPropertyReference v) {
      this.set(logDeviceObjectPropertyReference, v, null);
   }

   public int getCovResubscriptionInterval() {
      return this.getInt(covResubscriptionInterval);
   }

   public void setCovResubscriptionInterval(int v) {
      this.setInt(covResubscriptionInterval, v, null);
   }

   public BBacnetReliability getReliability() {
      return (BBacnetReliability)this.get(reliability);
   }

   public void setReliability(BBacnetReliability v) {
      this.set(reliability, v, null);
   }

   public BBacnetClientCov getClientCovIncrement() {
      return (BBacnetClientCov)this.get(clientCovIncrement);
   }

   public void setClientCovIncrement(BBacnetClientCov v) {
      this.set(clientCovIncrement, v, null);
   }

   public void historyUpdated() {
      this.invoke(historyUpdated, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public final void started() throws Exception {
      super.started();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.logSubscriber = new BBacnetTrendLogDescriptor.BacnetTrendLogSubscriber(this, this.getLog());
      this.checkConfiguration();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      this.logSubscriber.unsubscribeAll();
      this.optionalProps = null;
      this.logSubscriber = null;
      this.tlog = null;
      this.oldId = null;
      this.oldName = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   @Override
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
         } else if (p.equals(logOrd)) {
            this.tlog = null;
            this.checkConfiguration();
            if (this.getStatus().isOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         }
      }
   }

   public final BFacets getSlotFacets(Slot s) {
      return s.equals(objectId) ? BBacnetObjectType.getObjectIdFacets(20) : super.getSlotFacets(s);
   }

   @Override
   public final BObject getObject() {
      return (BObject)this.getLog();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getLogOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(logOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         this.logSubscriber.unsubscribeAll();
         boolean configOk = true;
         if (this.getLog() == null && !this.isDynamicallyCreated()) {
            this.setFaultCause("Cannot find exported history");
            configOk = false;
         } else {
            this.logSubscriber.config = this.tlog.getHistoryConfig();
            this.logSubscriber.subscribe(this.tlog.getHistoryConfig());
            if (!this.isDynamicallyCreated()) {
               this.logSubscriber.subscribe((BComponent)((BComplex)this.tlog).getParent());
               BBacnetDeviceObjectPropertyReference logDOPRef = this.getLogDOPRef();
               if (logDOPRef != NULL_DOPR) {
                  this.setLogDeviceObjectPropertyReference(logDOPRef);
               } else {
                  logger.severe("Cannot write log device object property in static trend log extension on unexported control object");
               }
            }

            if (objectName.isEquivalentToDefaultValue(this.get(objectName))) {
               this.setObjectName(this.tlog.getHistoryConfig().getId().getHistoryName());
            }

            this.setHistoryOrd(BOrd.make("history:" + this.tlog.getHistoryConfig().getId().toString()));
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            configOk = false;
         }

         if (configOk) {
            String err = BBacnetNetwork.localDevice().export(this);
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
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BBacnetTrendLogAlarmSourceExt;
   }

   @Deprecated
   @Override
   protected void updateAlarmInhibit() {
   }

   @Override
   public final boolean isEventInitiationEnabled() {
      return this.getNotificationClass() != null;
   }

   @Override
   public final BEnum getEventState() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BBacnetEventState.make(almExt.getAlarmState());
   }

   @Override
   public final BBacnetBitString getAckedTransitions() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAckedTransitions());
   }

   @Override
   public final BBacnetTimeStamp[] getEventTimeStamps() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt == null) {
         return null;
      } else {
         BBacnetTimeStamp[] ets = new BBacnetTimeStamp[3];
         BAbsTime t = almExt.getToOffnormalTimes().getAlarmTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[0] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[0] = new BBacnetTimeStamp(t);
         }

         t = almExt.getToFaultTimes().getAlarmTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[1] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[1] = new BBacnetTimeStamp(t);
         }

         t = almExt.getToNormalTimes().getAlarmTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[2] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[2] = new BBacnetTimeStamp(t);
         }

         return ets;
      }
   }

   @Override
   public final BBacnetNotifyType getNotifyType() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : almExt.getNotifyType();
   }

   @Override
   public final BBacnetBitString getEventEnable() {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable());
   }

   @Override
   public final int[] getEventPriorities() {
      BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
      return nc == null ? null : nc.getEventPriorities();
   }

   @Override
   public BBacnetNotificationClassDescriptor getNotificationClass() {
      return BacnetTrendLogUtil.getNotificationClass(this.tlog);
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.bufferReady;
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      this.getLog();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getLog();
      ArrayList<PropertyValue> results = new ArrayList<>(refs.length);

      for (int i = 0; i < refs.length; i++) {
         switch (refs[i].getPropertyId()) {
            case 8:
               int[] props = REQUIRED_PROPS;

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }

               props = this.getOptionalProps();

               for (int j = 0; j < props.length; j++) {
                  results.add(this.readProperty(props[j], -1));
               }
               break;
            case 80:
               int[] props = this.getOptionalProps();

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
      this.getLog();
      if (this.tlog == null) {
         return new ReadRangeAck(1, 1000);
      } else {
         int propertyId = rangeReference.getPropertyId();
         if (!this.hasProperty(propertyId)) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 131) {
            return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else if (rangeReference.getPropertyArrayIndex() != -1) {
            return new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray);
         } else {
            Integer pointAsnType = null;
            BAbstractProxyExt pxExt = this.getPoint().getProxyExt();
            if (pxExt instanceof BBacnetProxyExt) {
               BBacnetProxyExt bacPxExt = (BBacnetProxyExt)pxExt;
               pointAsnType = AsnUtil.getAsnType(bacPxExt.getDataType());
            } else {
               BBacnetDeviceObjectPropertyReference dopr = this.getLogDOPRef();
               if (dopr != null && dopr != NULL_DOPR) {
                  BBacnetObjectIdentifier oid = null;
                  if ((oid = dopr.getObjectId()) != null) {
                     int objectType = oid.getObjectType();
                     int propId = dopr.getPropertyId();
                     PropertyInfo info = ObjectTypeList.getInstance().getPropertyInfo(objectType, propId);
                     pointAsnType = info.getAsnType();
                  }
               }
            }

            int maxDataSize = -1;
            if (rangeReference instanceof BacnetConfirmedRequest) {
               maxDataSize = ((BacnetConfirmedRequest)rangeReference).getMaxDataLength();
               maxDataSize -= 17;
            }

            switch (rangeReference.getRangeType()) {
               case -1:
                  try {
                     RangeData rlr = BacnetTrendLogUtil.readRangeAll(this.tlog, maxDataSize, pointAsnType);
                     return new ReadRangeAck(
                        this.getObjectId(),
                        propertyId,
                        -1,
                        rlr.getResultFlags(),
                        rlr.getItemCount(),
                        rlr.getItemCount() > 0L ? rlr.getFirstSequenceNumber() : -1L,
                        rlr.getItemData()
                     );
                  } catch (Exception var16) {
                     return new ReadRangeAck(2, 0);
                  }
               case 0:
               case 1:
               case 2:
               case 4:
               case 5:
               default:
                  logger.warning("Unsupported ReadRange Range Type: " + rangeReference.getRangeType());
                  return new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.parameterOutOfRange);
               case 3:
                  long referenceNum = rangeReference.getReferenceIndex();
                  int count = rangeReference.getCount();

                  try {
                     RangeData rlr = BacnetTrendLogUtil.readRangeByPosition(this.tlog, referenceNum, count, maxDataSize, pointAsnType);
                     return new ReadRangeAck(this.getObjectId(), propertyId, -1, rlr.getResultFlags(), rlr.getItemCount(), -1L, rlr.getItemData());
                  } catch (Exception var15) {
                     return new ReadRangeAck(2, 0);
                  }
               case 6:
                  maxDataSize -= 5;
                  long startSeqNum = rangeReference.getReferenceIndex();
                  int count = rangeReference.getCount();

                  try {
                     RangeData rlr = BacnetTrendLogUtil.readRangeBySequence(this.tlog, startSeqNum, count, maxDataSize, pointAsnType);
                     return new ReadRangeAck(
                        this.getObjectId(),
                        propertyId,
                        -1,
                        rlr.getResultFlags(),
                        rlr.getItemCount(),
                        rlr.getItemCount() > 0L ? rlr.getFirstSequenceNumber() : -1L,
                        rlr.getItemData()
                     );
                  } catch (Exception var14) {
                     return new ReadRangeAck(2, 0);
                  }
               case 7:
                  maxDataSize -= 5;
                  BBacnetDateTime refTime = rangeReference.getReferenceTime();
                  int count = rangeReference.getCount();

                  try {
                     RangeData rlr = BacnetTrendLogUtil.readRangeByTime(this.tlog, refTime, count, maxDataSize, pointAsnType);
                     return new ReadRangeAck(
                        this.getObjectId(),
                        propertyId,
                        -1,
                        rlr.getResultFlags(),
                        rlr.getItemCount(),
                        rlr.getItemCount() > 0L ? rlr.getFirstSequenceNumber() : -1L,
                        rlr.getItemData()
                     );
                  } catch (Exception var13) {
                     return new ReadRangeAck(2, 0);
                  }
            }
         }
      }
   }

   private boolean hasProperty(int propertyId) {
      for (int id : REQUIRED_PROPS) {
         if (id == propertyId) {
            return true;
         }
      }

      for (int idx : this.getOptionalProps()) {
         if (idx == propertyId) {
            return true;
         }
      }

      return propertyId == 371;
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      this.getLog();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      this.getLog();
      if (this.tlog == null && !this.isDynamicallyCreated()) {
         return new NChangeListError(8, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!this.hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 131) {
            return BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
         }
      }
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      this.getLog();
      if (this.tlog == null) {
         return new NChangeListError(9, new NErrorType(1, 1000), 0L);
      } else {
         int propertyId = propertyValue.getPropertyId();
         if (!this.hasProperty(propertyId)) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty);
         } else if (propertyId != 131) {
            return BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
         } else {
            return propertyValue.getPropertyArrayIndex() != -1
               ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.propertyIsNotAnArray)
               : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.writeAccessDenied);
         }
      }
   }

   boolean isArray(int propertyId) {
      for (int arrayPropId : ARRAY_PROPS) {
         if (propertyId == arrayPropId) {
            return true;
         }
      }

      return false;
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      this.findLog();
      if (this.tlog == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else {
         if (ndx >= 0) {
            if (!this.isArray(pId)) {
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
            }
         } else if (ndx < -1) {
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 42));
         }

         switch (pId) {
            case 28:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
            case 36:
               return this.readEventState();
            case 75:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
            case 77:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
            case 79:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
            case 111:
               return new NReadPropertyResult(pId, ndx, AsnUtil.statusToAsnStatusFlags(BStatus.ok));
            case 126:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(BacnetTrendLogUtil.getMaxRecords(this.tlog)));
            case 131:
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 27));
            case 133:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.tlog.getEnabled()));
            case 141:
               long recCount = 0L;
               HistoryDatabaseConnection conn = getHistoryDbConnection(BLocalBacnetDevice.getBacnetContext());
               Throwable var6 = null;

               try {
                  BIHistory hist = this.getHistory(conn);
                  if (this.getHistory(conn) != null) {
                     recCount = conn.getRecordCount(hist);
                  }
               } catch (Throwable var15) {
                  var6 = var15;
                  throw var15;
               } finally {
                  if (conn != null) {
                     if (var6 != null) {
                        try {
                           conn.close();
                        } catch (Throwable var14) {
                           var6.addSuppressed(var14);
                        }
                     } else {
                        conn.close();
                     }
                  }
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(recCount));
            case 144:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.tlog.getHistoryConfig().getFullPolicy().equals(BFullPolicy.stop)));
            case 145:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.tlog.getTotalRecordCount()));
            case 197:
               return this.readLoggingType();
            case 371:
               return this.readPropertyList(ndx);
            default:
               return this.readOptionalProperty(pId, ndx);
         }
      }
   }

   private PropertyValue readEventState() {
      if (!this.getEventDetectionEnable()) {
         return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0));
      } else {
         BBacnetTrendLogAlarmSourceExt alarmExt = this.getAlarmExt();
         if (alarmExt == null) {
            return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0));
         } else {
            if (this.tlog instanceof BCovHistoryExt) {
               BControlPoint point = this.getPoint();
               BAbstractProxyExt pxExt = point.getProxyExt();
               if (pxExt instanceof BBacnetProxyExt) {
                  BBacnetProxyExt bac = (BBacnetProxyExt)pxExt;
                  if (bac.useCov() && !bac.isCOV()) {
                     return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(1));
                  }

                  return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0));
               }
            }

            return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(BBacnetEventState.fromBAlarmState(alarmExt.getAlarmState())));
         }
      }
   }

   private PropertyValue readLoggingType() {
      if (BacnetDescriptorUtil.isGenericTrendLogExtension(this.tlog)) {
         return ((BIntervalHistoryExt)this.getLog()).getInterval().getMillis() == 0L
            ? makeLoggingTypeResult(BBacnetLoggingType.cov)
            : makeLoggingTypeResult(BBacnetLoggingType.polled);
      } else if (this.tlog instanceof BCovHistoryExt) {
         return makeLoggingTypeResult(BBacnetLoggingType.cov);
      } else if (this.tlog instanceof BIntervalHistoryExt) {
         return makeLoggingTypeResult(BBacnetLoggingType.polled);
      } else {
         logger.warning(this + ": trend log ext type is not supported: " + this.tlog.getClass());
         return new NReadPropertyResult(197, new NErrorType(2, 0));
      }
   }

   private static PropertyValue makeLoggingTypeResult(BBacnetLoggingType type) {
      return new NReadPropertyResult(197, AsnUtil.toAsnEnumerated(type));
   }

   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         switch (pId) {
            case 0:
               return this.readAckedTransitions(almExt.getAckedTransitions());
            case 17:
               BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
               if (nc == null) {
                  return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(nc.getNotificationClass()));
            case 35:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable())));
            case 72:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(almExt.getNotifyType()));
            case 130:
               return this.readEventTimeStamps(
                  almExt.getToOffnormalTimes().getAlarmTime(), almExt.getToFaultTimes().getAlarmTime(), almExt.getToNormalTimes().getAlarmTime(), ndx
               );
            case 137:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getNotificationThreshold()));
            case 140:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getRecordsSinceNotification()));
            case 173:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getLastNotifyRecord()));
            case 351:
               return this.readEventMessageTexts(ndx);
            case 352:
               return this.readEventMessageTextsConfig(
                  almExt.getToOffnormalText().getFormat(), almExt.getToFaultText().getFormat(), almExt.getToNormalText().getFormat(), ndx
               );
            case 353:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getEventDetectionEnable()));
         }
      }

      BControlPoint point = this.getPoint();
      switch (pId) {
         case 127:
            if (this.tlog instanceof BCovHistoryExt) {
               SlotCursor<Property> c = point.getProperties();
               if (c.next(BBacnetProxyExt.class)) {
                  BBacnetProxyExt ext = (BBacnetProxyExt)c.get();
                  if (ext.isCOV()) {
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
                  }
               }

               if (this.tlog instanceof BNumericCovHistoryExt) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BNumericCovHistoryExt)this.tlog).getChangeTolerance()));
               }

               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
            }

            if (this.getClientCovIncrement().getIncrement().getStatus() != BStatus.nullStatus) {
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(this.getClientCovIncrement().getIncrement().getNumeric()));
            }

            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
         case 128:
            if (this.getLog() == null || !(this.tlog instanceof BCovHistoryExt)) {
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getCovResubscriptionInterval()));
            }

            SlotCursor<Property> cx = point.getProperties();
            if (cx.next(BBacnetProxyExt.class)) {
               BBacnetProxyExt ext = (BBacnetProxyExt)cx.get();
               BBacnetDevice device = ext.device();
               if (device != null && ext.isCOV()) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(ext.getCovSubscriptionLifetime() * 30L));
               }
            }
            break;
         case 132:
            BBacnetDeviceObjectPropertyReference dopRef = this.getLogDeviceObjectPropertyReference();
            if (dopRef == NULL_DOPR) {
               return new NReadPropertyResult(pId, ndx, new NErrorType(2, 72));
            }

            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsn(dopRef));
         case 134:
            long interval = 0L;
            if (this.tlog instanceof BIntervalHistoryExt) {
               interval = ((BIntervalHistoryExt)this.getLog()).getInterval().getMillis() / 10L;
            }

            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(interval));
         case 142:
            if (this.tlog.getActivePeriod() instanceof BBacnetActivePeriod) {
               BBacnetDateTime startTime = ((BBacnetActivePeriod)this.tlog.getActivePeriod()).getStartTime();
               if (startTime != null) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsn(startTime));
               }
            }
            break;
         case 143:
            if (this.tlog.getActivePeriod() instanceof BBacnetActivePeriod) {
               BBacnetDateTime stopTime = ((BBacnetActivePeriod)this.tlog.getActivePeriod()).getStopTime();
               if (stopTime != null) {
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsn(stopTime));
               }
            }
            break;
         case 205:
            return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.tlog.getTrigger()));
      }

      return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
   }

   private NReadPropertyResult readAckedTransitions(BAlarmTransitionBits ackedTrans) {
      if (this.getEventDetectionEnable()) {
         BAlarmTransitionBits eventTrans = this.readEventTransition(ackedTrans);
         return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(eventTrans)));
      } else {
         return new NReadPropertyResult(0, -1, AsnUtil.toAsnBitString(ACKED_TRANS_DEFAULT));
      }
   }

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      BIBacnetTrendLogExt trendLogExt = this.getLog();
      if (trendLogExt == null) {
         return new NErrorType(1, 1000);
      } else {
         if (ndx >= 0) {
            if (!this.isArray(pId)) {
               return new NErrorType(2, 50);
            }
         } else if (ndx < -1) {
            return new NErrorType(2, 42);
         }

         try {
            switch (pId) {
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 36:
               case 75:
               case 79:
               case 111:
               case 131:
               case 145:
               case 197:
               case 371:
                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               case 126:
                  return this.writeBufferSize(val);
               case 132:
                  return this.writeLogDeviceObjectProperty(pId, val);
               case 133:
                  return this.writeEnable(pId, val);
               case 141:
                  return this.writeRecordCount(val);
               case 144:
                  BFullPolicy newPolicy = AsnUtil.fromAsnBoolean(val) ? BFullPolicy.stop : BFullPolicy.roll;
                  BHistoryConfig extConfig = this.tlog.getHistoryConfig();
                  if (extConfig.getFullPolicy().equals(newPolicy)) {
                     return null;
                  } else if (this.isBusy(pId)) {
                     return new NErrorType(1, 82);
                  } else {
                     try {
                        extConfig.setFullPolicy(newPolicy);
                        this.waitForHistoryOperation(pId);
                        if (this.tlog.getEnabled()) {
                           BIHistory history = this.tlog.getHistory();
                           if (history == null) {
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine(this + ": history config not found after write to StopWhenFull");
                              }

                              return new NErrorType(2, 0);
                           }

                           BFullPolicy actualPolicy = history.getConfig().getFullPolicy();
                           if (!actualPolicy.equals(newPolicy)) {
                              if (logger.isLoggable(Level.FINE)) {
                                 logger.fine(this + ": StopWhenFull write failed; expected FullPolicy: " + newPolicy + ", actual: " + actualPolicy);
                              }

                              return new NErrorType(2, 0);
                           }
                        }

                        return null;
                     } finally {
                        this.isBusy.set(false);
                     }
                  }
               default:
                  return this.writeOptionalProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var16) {
            logger.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var16);
            return new NErrorType(2, 9);
         } catch (PermissionException var17) {
            logger.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var17);
            return new NErrorType(2, 40);
         }
      }
   }

   private boolean isBusy(int propertyId) {
      if (this.isBusy.compareAndSet(false, true)) {
         return false;
      } else {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this + ": history operation is already in-progress when attempting to write " + BBacnetPropertyIdentifier.tag(propertyId));
         }

         return true;
      }
   }

   private void waitForHistoryOperation(int propertyId) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this + ": waiting for history operation to finish while writing " + BBacnetPropertyIdentifier.tag(propertyId));
      }

      this.isWaiting.set(true);
      this.historyUpdated();

      while (this.isWaiting.get()) {
         try {
            Thread.sleep(100L);
         } catch (InterruptedException var3) {
            logger.warning(this + ": wait for historyUpdated interrupted when writing " + BBacnetPropertyIdentifier.tag(propertyId));
            this.isWaiting.set(false);
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   private ErrorType writeLogDeviceObjectProperty(int pId, byte[] val) throws AsnException, RejectException {
      BBacnetDeviceObjectPropertyReference newRef = (BBacnetDeviceObjectPropertyReference)AsnUtil.fromAsn(-1, val, new BBacnetDeviceObjectPropertyReference());
      BBacnetDeviceObjectPropertyReference oldRef = this.getLogDeviceObjectPropertyReference();
      if (BacnetDescriptorUtil.isEqual(newRef, oldRef)) {
         return null;
      } else {
         BComponent point = this.getTargetPoint(new NBacnetPropertyValue(pId, newRef.getPropertyArrayIndex(), val));
         if (point == null) {
            return new NErrorType(2, 45);
         } else {
            BIBacnetTrendLogExt trendLogExt = this.getLog();
            boolean enabled = ((BHistoryExt)trendLogExt).getEnabled();
            if (this.isBusy(pId)) {
               return new NErrorType(1, 82);
            } else {
               try {
                  ((BHistoryExt)trendLogExt).setEnabled(false);
                  if (this.getLogOrd().isNull()
                     && point instanceof BControlPoint
                     && BacnetDescriptorUtil.areTrendLogAndPointCompatible((BControlPoint)point, trendLogExt, newRef)
                     && BacnetDescriptorUtil.isLocalDevice(newRef.getDeviceId().getInstanceNumber())) {
                     String trendLogName = "TrendLog_" + this.getObjectId().getInstanceNumber();
                     point.add(trendLogName, (BIntervalHistoryExt)trendLogExt);
                     this.setLogOrd(((BIntervalHistoryExt)trendLogExt).getHandleOrd());
                     BacnetDescriptorUtil.removeHistory(this, false);
                  } else {
                     PropertyValue[] pvs = BacnetDescriptorUtil.getValuesWrittenToTrendExtension(this);
                     BacnetDescriptorUtil.removeHistory(this, true);
                     this.tlog = null;

                     try {
                        this.tlog = BacnetDescriptorUtil.copy(this, newRef, pvs);
                     } catch (BacnetException var14) {
                        return new NErrorType(2, 45);
                     }
                  }

                  this.setLogDeviceObjectPropertyReference(newRef);
                  if (!(this.tlog instanceof BNumericCovHistoryExt)
                     && !(this.tlog instanceof BBacnetNumericTrendLogRemoteExt)
                     && !(this.tlog instanceof BBacnetNumericTrendLogExt)) {
                     this.setClientCovIncrement(new BBacnetClientCov());
                  }

                  BBacnetNetwork.localDevice().exportByOrd(this);
                  if (enabled) {
                     ((BHistoryExt)this.getLog()).setEnabled(enabled);
                     this.waitForHistoryOperation(pId);
                     if (this.tlog.getHistory() == null) {
                        if (logger.isLoggable(Level.FINE)) {
                           logger.fine(this + ": history not found after write to LogDeviceObjectProperty");
                        }

                        return new NErrorType(2, 0);
                     }
                  }

                  return null;
               } finally {
                  this.isBusy.set(false);
               }
            }
         }
      }
   }

   private ErrorType writeEnable(int pId, byte[] val) throws AsnException {
      if (Flags.isReadonly((BComplex)this.tlog, BHistoryExt.enabled)) {
         return new NErrorType(1, 1000);
      } else {
         long recCount = 0L;
         HistoryDatabaseConnection conn = getHistoryDbConnection(BLocalBacnetDevice.getBacnetContext());
         Throwable var6 = null;

         try {
            BIHistory hist = this.getHistory(conn);
            if (this.getHistory(conn) != null) {
               recCount = conn.getRecordCount(hist);
            }
         } catch (Throwable var23) {
            var6 = var23;
            throw var23;
         } finally {
            if (conn != null) {
               if (var6 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var21) {
                     var6.addSuppressed(var21);
                  }
               } else {
                  conn.close();
               }
            }
         }

         long bufSize = BacnetTrendLogUtil.getMaxRecords(this.tlog);
         if (this.tlog.getHistoryConfig().getFullPolicy().equals(BFullPolicy.stop) && recCount >= bufSize) {
            return new NErrorType(1, 75);
         } else {
            boolean newEnable = AsnUtil.fromOnlyAsnBoolean(val);
            if (this.tlog.getEnabled() == newEnable) {
               return null;
            } else if (this.isBusy(pId)) {
               return new NErrorType(1, 82);
            } else {
               NErrorType var8;
               try {
                  ((BHistoryExt)this.tlog).setBoolean(BHistoryExt.enabled, newEnable, BLocalBacnetDevice.getBacnetContext());
                  if (this.getLogOrd() == null || this.getLogOrd().isNull()) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(this + ": log ord is null after write to Enable, configuration error");
                     }

                     this.setReliability(BBacnetReliability.configurationError);
                  }

                  this.waitForHistoryOperation(pId);
                  if (!newEnable || this.tlog.getHistory() != null) {
                     return null;
                  }

                  if (logger.isLoggable(Level.FINE)) {
                     logger.fine(this + ": history not found after write to Enable");
                  }

                  var8 = new NErrorType(2, 0);
               } finally {
                  this.isBusy.set(false);
               }

               return var8;
            }
         }
      }
   }

   private ErrorType writeRecordCount(byte[] val) throws AsnException {
      long recordCount = AsnUtil.fromAsnUnsignedInteger(val);
      if (recordCount == 0L) {
         try {
            HistoryDatabaseConnection conn = getHistoryDbConnection(BLocalBacnetDevice.getBacnetContext());
            Throwable var5 = null;

            Object var7;
            try {
               BHistoryId historyId = this.tlog.getHistoryConfig().getId();
               if (conn.getHistory(historyId) != null) {
                  conn.clearAllRecords(historyId);
                  BacnetTrendLogUtil.writeEvent(
                     this.tlog,
                     BAbsTime.now(),
                     BStatus.DEFAULT,
                     BacnetTrendLogUtil.incrementSequenceNumber(this.tlog.getTotalRecordCount()),
                     this.tlog.getEnabled() ? BTrendEvent.LOG_STATUS_ENABLED_BUFFER_PURGED : BTrendEvent.LOG_STATUS_DISABLED_BUFFER_PURGED
                  );
                  return null;
               }

               var7 = null;
            } catch (Throwable var19) {
               var5 = var19;
               throw var19;
            } finally {
               if (conn != null) {
                  if (var5 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var18) {
                        var5.addSuppressed(var18);
                     }
                  } else {
                     conn.close();
                  }
               }
            }

            return (ErrorType)var7;
         } catch (PermissionException var21) {
            logger.warning("PermissionException clearing history when writing record count" + this + ": " + var21);
            return new NErrorType(2, 40);
         } catch (Exception var22) {
            logger.log(Level.WARNING, "Error clearing history when writing record count" + this, (Throwable)var22);
            return new NErrorType(0, 25);
         }
      } else {
         return new NErrorType(2, 37);
      }
   }

   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      try {
         BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt != null) {
            switch (pId) {
               case 0:
               case 130:
               case 140:
               case 173:
               case 351:
                  return new NErrorType(2, 40);
               case 17:
                  long ncinst = AsnUtil.fromAsnUnsignedInteger(val);
                  if (ncinst > 4194302L) {
                     return new NErrorType(2, 37);
                  }

                  BBacnetObjectIdentifier ncid = BBacnetObjectIdentifier.make(15, (int)ncinst);
                  BBacnetNotificationClassDescriptor nc = (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(ncid);
                  if (nc == null) {
                     return new NErrorType(2, 37);
                  }

                  BAlarmClass ac = nc.getAlarmClass();
                  almExt.setString(BBacnetTrendLogAlarmSourceExt.alarmClass, ac.getName(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 35:
                  almExt.set(
                     BBacnetTrendLogAlarmSourceExt.alarmEnable,
                     BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                     BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               case 72:
                  almExt.set(
                     BBacnetTrendLogAlarmSourceExt.notifyType, BBacnetNotifyType.make(AsnUtil.fromAsnEnumerated(val)), BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               case 137:
                  almExt.setNotificationThreshold(AsnUtil.fromAsnUnsignedInteger(val));
                  return null;
               case 352:
                  return writeMessageTextsConfig(ndx, val, almExt);
               case 353:
                  this.setBoolean(eventDetectionEnable, AsnUtil.fromAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
            }
         }

         switch (pId) {
            case 127:
               if (this.getPoint() == null) {
                  return new NErrorType(2, 8);
               }

               BBacnetClientCov bacnetClientCov = (BBacnetClientCov)AsnUtil.fromAsn(-1, val, new BBacnetClientCov());
               BStatusNumeric covIncrement = bacnetClientCov.getIncrement();
               double covRealIncrement = 0.0;
               if (!covIncrement.getStatus().equals(BStatus.nullStatus)) {
                  covRealIncrement = covIncrement.getNumeric();
               }

               if (this.tlog instanceof BNumericCovHistoryExt) {
                  ((BNumericCovHistoryExt)this.tlog).setChangeTolerance(covRealIncrement);
               } else if (this.tlog instanceof BBacnetNumericTrendLogRemoteExt) {
                  ((BBacnetNumericTrendLogRemoteExt)this.tlog).setChangeTolerance(covRealIncrement);
               } else if (this.tlog instanceof BBacnetNumericTrendLogExt) {
                  ((BBacnetNumericTrendLogExt)this.tlog).setChangeTolerance(covRealIncrement);
               } else if (!covIncrement.getStatus().equals(BStatus.nullStatus)) {
                  return new NErrorType(2, 8);
               }

               this.setClientCovIncrement(bacnetClientCov);
               return null;
            case 128:
               int lifeTime = AsnUtil.fromAsnUnsignedInt(val);
               if (BacnetDescriptorUtil.isGenericTrendLogExtension(this.getLog())) {
                  return this.writeCovResubscriptionIntervalToGenericTrengLog(this.tlog, lifeTime);
               }

               if (this.tlog instanceof BCovHistoryExt) {
                  BControlPoint point = this.getPoint();
                  SlotCursor<Property> c = point.getProperties();
                  if (c.next(BBacnetProxyExt.class)) {
                     BBacnetProxyExt ext = (BBacnetProxyExt)c.get();
                     if (ext != null && ext.isCOV()) {
                        BBacnetTuningPolicy bacnetTuningPolicy = (BBacnetTuningPolicy)ext.getTuningPolicy();
                        bacnetTuningPolicy.setCovSubscriptionLifetime(lifeTime / 30);
                        return null;
                     }
                  }
               }
               break;
            case 134:
               return this.writeLogInterval(pId, val);
            case 142:
               if (this.getLog().getActivePeriod() instanceof BBacnetActivePeriod) {
                  BBacnetActivePeriod activePeriodSta = (BBacnetActivePeriod)this.getLog().getActivePeriod();
                  BBacnetDateTime startTime = (BBacnetDateTime)activePeriodSta.getStartTime().newCopy();
                  AsnUtil.fromAsn(val, startTime);
                  checkForSpecialValues(startTime);
                  activePeriodSta.set(BBacnetActivePeriod.startTime, startTime, BLocalBacnetDevice.getBacnetContext());
                  return null;
               }
               break;
            case 143:
               if (this.getLog().getActivePeriod() instanceof BBacnetActivePeriod) {
                  BBacnetActivePeriod activePeriodSto = (BBacnetActivePeriod)this.tlog.getActivePeriod();
                  BBacnetDateTime stopTime = (BBacnetDateTime)activePeriodSto.getStopTime().newCopy();
                  AsnUtil.fromAsn(val, stopTime);
                  checkForSpecialValues(stopTime);
                  activePeriodSto.set(BBacnetActivePeriod.stopTime, stopTime, BLocalBacnetDevice.getBacnetContext());
                  return null;
               }
               break;
            case 205:
               return new NErrorType(2, 40);
         }
      } catch (OutOfRangeException var15) {
         logger.warning("OutOfRangeException writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
         return new NErrorType(2, 37);
      } catch (AsnException var16) {
         logger.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var16);
         return new NErrorType(2, 9);
      } catch (PermissionException var17) {
         logger.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var17);
         return new NErrorType(2, 40);
      }

      return new NErrorType(2, 32);
   }

   private static ErrorType writeMessageTextsConfig(int ndx, byte[] val, BBacnetTrendLogAlarmSourceExt alarmExt) throws AsnException {
      if (ndx >= -1 && ndx <= 3) {
         switch (ndx) {
            case -1:
               BBacnetArray textsConfig = new BBacnetArray(BString.TYPE, 3);
               AsnUtil.fromAsn(-4, val, textsConfig);
               String toOffnormalText = textsConfig.getElement(1).toString(null);
               String toFaultText = textsConfig.getElement(2).toString(null);
               String toNormalText = textsConfig.getElement(3).toString(null);
               if (!toOffnormalText.isEmpty() || !toFaultText.isEmpty()) {
                  return new NErrorType(2, 40);
               }

               alarmExt.set(BBacnetTrendLogAlarmSourceExt.toNormalText, BFormat.make(toNormalText), BLocalBacnetDevice.getBacnetContext());
               break;
            case 0:
               return new NErrorType(2, 40);
            case 1:
            case 2:
               if (!AsnUtil.fromAsnCharacterString(val).isEmpty()) {
                  return new NErrorType(2, 40);
               }
               break;
            case 3:
               alarmExt.set(
                  BBacnetTrendLogAlarmSourceExt.toNormalText, BFormat.make(AsnUtil.fromAsnCharacterString(val)), BLocalBacnetDevice.getBacnetContext()
               );
         }

         return null;
      } else {
         return new NErrorType(2, 42);
      }
   }

   private ErrorType writeLogInterval(int pId, byte[] val) throws AsnException {
      BObject o = (BObject)this.getLog();
      if (!(o instanceof BIntervalHistoryExt)) {
         if (logger.isLoggable(Level.FINE)) {
            logger.fine(this + ": attempting to write LOG_INTERVAL property to history ext type " + o.getType());
         }

         return new NErrorType(2, 40);
      } else {
         BIntervalHistoryExt historyExt = (BIntervalHistoryExt)o;
         BRelTime newInterval = BRelTime.make(AsnUtil.fromAsnUnsignedInteger(val) * 10L);
         if (historyExt.getInterval().equals(newInterval)) {
            return null;
         } else if (this.isBusy(pId)) {
            return new NErrorType(1, 82);
         } else {
            try {
               historyExt.setInterval(newInterval);
               this.waitForHistoryOperation(pId);
               if (this.tlog.getEnabled()) {
                  BIHistory history = this.tlog.getHistory();
                  if (history == null) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(this + ": history config not found after write to LogInterval");
                     }

                     return new NErrorType(2, 0);
                  }

                  BCollectionInterval actualInterval = history.getConfig().getInterval();
                  if (!actualInterval.getInterval().equals(newInterval)) {
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine(this + ": LogInterval write failed; expected interval: " + newInterval + ", actual: " + actualInterval.getInterval());
                     }

                     return new NErrorType(2, 0);
                  }
               }

               return null;
            } finally {
               this.isBusy.set(false);
            }
         }
      }
   }

   public void doHistoryUpdated() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine(this + ": asynchronous history action completed; clearing isWaiting flag");
      }

      this.isWaiting.set(false);
   }

   private ErrorType writeBufferSize(byte[] value) throws AsnException {
      long maxRecords = AsnUtil.fromAsnUnsignedInteger(value);
      if (maxRecords == 4294967295L) {
         this.tlog.getHistoryConfig().set(BHistoryConfig.capacity, BCapacity.UNLIMITED, BLocalBacnetDevice.getBacnetContext());
      } else {
         if (maxRecords > 2147483647L) {
            return new NErrorType(BBacnetErrorClass.property, BBacnetErrorCode.valueOutOfRange);
         }

         this.tlog.getHistoryConfig().set(BHistoryConfig.capacity, BCapacity.makeByRecordCount((int)maxRecords), BLocalBacnetDevice.getBacnetContext());
      }

      return null;
   }

   private int[] getOptionalProps() {
      ArrayList<BBacnetPropertyIdentifier> v = new ArrayList<>();
      v.add(BBacnetPropertyIdentifier.description);
      v.add(BBacnetPropertyIdentifier.logInterval);
      v.add(BBacnetPropertyIdentifier.trigger);
      if (BacnetDescriptorUtil.isGenericTrendLogExtension(this.tlog)) {
         v.add(BBacnetPropertyIdentifier.covResubscriptionInterval);
         v.add(BBacnetPropertyIdentifier.clientCovIncrement);
         v.add(BBacnetPropertyIdentifier.logDeviceObjectProperty);
         v.add(BBacnetPropertyIdentifier.startTime);
         v.add(BBacnetPropertyIdentifier.stopTime);
         v.add(BBacnetPropertyIdentifier.notificationThreshold);
         v.add(BBacnetPropertyIdentifier.recordsSinceNotification);
         v.add(BBacnetPropertyIdentifier.lastNotifyRecord);
         v.add(BBacnetPropertyIdentifier.notificationClass);
         v.add(BBacnetPropertyIdentifier.eventEnable);
         v.add(BBacnetPropertyIdentifier.ackedTransitions);
         v.add(BBacnetPropertyIdentifier.notifyType);
         v.add(BBacnetPropertyIdentifier.eventTimeStamps);
         v.add(BBacnetPropertyIdentifier.eventMessageTexts);
         v.add(BBacnetPropertyIdentifier.eventMessageTextsConfig);
         v.add(BBacnetPropertyIdentifier.eventDetectionEnable);
      } else {
         BControlPoint point = this.getPoint();
         if (point != null) {
            BOrd pointOrd = point.getHandleOrd();
            BAbstractProxyExt pxExt = point.getProxyExt();
            if (this.tlog instanceof BCovHistoryExt) {
               if (pxExt instanceof BBacnetProxyExt && ((BBacnetProxyExt)pxExt).isCOV()) {
                  v.add(BBacnetPropertyIdentifier.covResubscriptionInterval);
               }

               v.add(BBacnetPropertyIdentifier.clientCovIncrement);
            }

            BBacnetObjectIdentifier logObjId = BBacnetNetwork.localDevice().lookupBacnetObjectId(pointOrd);
            if (logObjId != null) {
               BIBacnetExportObject logObject = BBacnetNetwork.localDevice().lookupBacnetObject(logObjId);
               if (logObject != null) {
                  v.add(BBacnetPropertyIdentifier.logDeviceObjectProperty);
               }
            }

            if (this.tlog != null && this.tlog.getActivePeriod() instanceof BBacnetActivePeriod) {
               v.add(BBacnetPropertyIdentifier.startTime);
               v.add(BBacnetPropertyIdentifier.stopTime);
            }

            BBacnetTrendLogAlarmSourceExt almExt = this.getAlarmExt();
            if (almExt != null) {
               v.add(BBacnetPropertyIdentifier.notificationThreshold);
               v.add(BBacnetPropertyIdentifier.recordsSinceNotification);
               v.add(BBacnetPropertyIdentifier.lastNotifyRecord);
               v.add(BBacnetPropertyIdentifier.notificationClass);
               v.add(BBacnetPropertyIdentifier.eventEnable);
               v.add(BBacnetPropertyIdentifier.ackedTransitions);
               v.add(BBacnetPropertyIdentifier.notifyType);
               v.add(BBacnetPropertyIdentifier.eventTimeStamps);
               v.add(BBacnetPropertyIdentifier.eventMessageTexts);
               v.add(BBacnetPropertyIdentifier.eventMessageTextsConfig);
               v.add(BBacnetPropertyIdentifier.eventDetectionEnable);
            }
         }
      }

      this.optionalProps = new int[v.size()];

      for (int i = 0; i < this.optionalProps.length; i++) {
         this.optionalProps[i] = ((BEnum)v.get(i)).getOrdinal();
      }

      return this.optionalProps;
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   final BIBacnetTrendLogExt getLog(boolean forceful) {
      if (forceful) {
         this.tlog = null;
      }

      return this.getLog();
   }

   final BIBacnetTrendLogExt getLog() {
      if (this.tlog == null) {
         return this.findLog();
      } else {
         if (this.tlog == null && this.isDynamicallyCreated()) {
            this.tlog = new BBacnetNumericTrendLogExt();
         }

         return this.tlog;
      }
   }

   public BBacnetTrendLogAlarmSourceExt getAlarmExt() {
      return BacnetTrendLogUtil.getAlarmExt(this.tlog);
   }

   @Override
   public BControlPoint getPoint() {
      return this.tlog != null ? (BControlPoint)((BHistoryExt)this.tlog).getParent() : null;
   }

   private static void checkForSpecialValues(BBacnetDateTime dateTime) throws OutOfRangeException {
      BBacnetDate date = dateTime.getDate();
      BBacnetTime time = dateTime.getTime();
      if (!allUnspecified(date) || !allUnspecified(time)) {
         if (date.getYear() == -1 || monthHasSpecialValue(date.getMonth()) || dayHasSpecialValue(date.getDayOfMonth()) || date.getDayOfWeek() == -1) {
            throw new OutOfRangeException("Date contains Special Values.");
         } else if (time.isHourUnspecified() || time.isMinuteUnspecified() || time.isSecondUnspecified() || time.isHundredthUnspecified()) {
            throw new OutOfRangeException("Time contains Special Values.");
         }
      }
   }

   private static boolean allUnspecified(BBacnetTime time) {
      return time.isHourUnspecified() && time.isMinuteUnspecified() && time.isSecondUnspecified() && time.isHundredthUnspecified();
   }

   private static boolean allUnspecified(BBacnetDate date) {
      return date.isYearUnspecified() && date.isMonthUnspecified() && date.isDayOfMonthUnspecified() && date.isDayOfWeekUnspecified();
   }

   private static boolean monthHasSpecialValue(int month) {
      return month == -1 || month == 13 || month == 14;
   }

   private static boolean dayHasSpecialValue(int day) {
      return day == -1 || day == 32 || day == 33 || day == 34 || day == 35;
   }

   private BIBacnetTrendLogExt findLog() {
      try {
         if (!logOrd.isEquivalentToDefaultValue(this.getLogOrd()) || this.isDynamicallyCreated() && this.getLogOrd() != null && !this.getLogOrd().isNull()) {
            BObject o = this.getLogOrd().get(this);
            if (o instanceof BIBacnetTrendLogExt) {
               this.tlog = (BIBacnetTrendLogExt)o;
            } else {
               this.tlog = null;
            }
         }
      } catch (Exception var2) {
         logger.warning("Unable to resolve log ord for " + this + " " + this.getLogOrd() + ": " + var2);
         this.tlog = null;
      }

      if (this.tlog == null && this.isRunning()) {
         this.setFaultCause("Cannot find exported history");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      if (this.isDynamicallyCreated() && this.tlog == null) {
         this.tlog = new BBacnetNumericTrendLogExt();
         BBacnetTrendLogAlarmSourceExt ext = new BBacnetTrendLogAlarmSourceExt();
         ext.setAlarmEnable(BAlarmTransitionBits.EMPTY);
         ((BComponent)this.tlog).add("BBacnetTrendLogAlarmSourceExt", ext);
      }

      return this.tlog;
   }

   private BIHistory getHistory(HistoryDatabaseConnection conn) {
      BIBacnetTrendLogExt tlog = this.getLog();
      BIHistory history = tlog.getHistory();
      if (history == null) {
         BHistoryConfig config = tlog.getHistoryConfig();
         BHistoryId id = config.getId();
         if (conn.exists(id)) {
            history = conn.getHistory(id);
         }
      }

      return history;
   }

   private static HistoryDatabaseConnection getHistoryDbConnection(Context cx) {
      BHistoryService service = (BHistoryService)Sys.getService(BHistoryService.TYPE);
      BHistoryDatabase db = service.getDatabase();
      return db.getDbConnection(null);
   }

   private BBacnetDeviceObjectPropertyReference getLogDOPRef() {
      BBacnetDeviceObjectPropertyReference dopRef = NULL_DOPR;
      if (this.tlog == null) {
         this.findLog();
      }

      BControlPoint controlPoint = this.getPoint();
      if (controlPoint == null) {
         return dopRef;
      } else {
         BOrd pointOrd = controlPoint.getHandleOrd();
         BBacnetObjectIdentifier logObjId = BBacnetNetwork.localDevice().lookupBacnetObjectId(pointOrd);
         if (logObjId != null) {
            dopRef = new BBacnetDeviceObjectPropertyReference(logObjId);
         } else {
            BAbstractProxyExt pxExt = controlPoint.getProxyExt();
            if (pxExt instanceof BBacnetProxyExt) {
               BBacnetProxyExt bacPxExt = (BBacnetProxyExt)pxExt;
               dopRef = new BBacnetDeviceObjectPropertyReference(
                  bacPxExt.getObjectId(), bacPxExt.getPropertyId().getOrdinal(), bacPxExt.getPropertyArrayIndex(), bacPxExt.device().getObjectId()
               );
            }
         }

         return dopRef;
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetTrendLogDescriptor", 2);
      out.prop("tlog", this.tlog);
      out.prop("logSubscriber", this.logSubscriber);
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
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, this.getOptionalProps());
   }

   @Override
   public boolean isDynamicallyCreated() {
      return this.getDynamicallyCreated();
   }

   private BComponent getTargetPoint(PropertyValue pv) {
      try {
         if (pv == null) {
            return this.targetPoint;
         }

         BBacnetDeviceObjectPropertyReference ref = (BBacnetDeviceObjectPropertyReference)AsnUtil.fromAsn(
            -1, pv.getPropertyValue(), new BBacnetDeviceObjectPropertyReference()
         );
         this.targetPoint = BacnetDescriptorUtil.isValid(ref) ? BacnetDescriptorUtil.findOrAddPoint(ref) : null;
      } catch (Exception var3) {
         logger.severe("Could not find the target point: " + var3);
      }

      return this.targetPoint;
   }

   private ErrorType writeCovResubscriptionIntervalToGenericTrengLog(BIBacnetTrendLogExt tlog, int lifeTime) {
      if (lifeTime > 28800) {
         return new NErrorType(5, 37);
      } else {
         if (tlog != null) {
            if (tlog instanceof BBacnetTrendLogRemoteExt) {
               ((BBacnetTrendLogRemoteExt)tlog).setCovResubscriptionInterval(lifeTime);
            }

            this.setCovResubscriptionInterval(lifeTime);
         }

         return null;
      }
   }

   private int readCovResubscriptionIntervalToGenericTrengLog(BIBacnetTrendLogExt tlog, int lifeTime) {
      if (tlog != null && tlog instanceof BBacnetTrendLogRemoteExt) {
         this.setCovResubscriptionInterval(((BBacnetTrendLogRemoteExt)tlog).getCovResubscriptionInterval());
      }

      return this.getCovResubscriptionInterval();
   }

   class BacnetTrendLogSubscriber extends Subscriber {
      private final BBacnetTrendLogDescriptor obj;
      BHistoryConfig config;

      public BacnetTrendLogSubscriber(BBacnetTrendLogDescriptor obj, BIBacnetTrendLogExt log) {
         this.obj = obj;
         if (log != null) {
            this.config = log.getHistoryConfig();
         }
      }

      public void event(BComponentEvent event) {
         try {
            switch (event.getId()) {
               case 0:
                  if (BHistoryConfig.historyName.equals(event.getSlot().asProperty())) {
                     this.obj.checkConfiguration();
                  }
                  break;
               case 2:
                  BObject object = this.obj.getObject();
                  if (object instanceof BComplex && !BBacnetTrendLogDescriptor.this.isDynamicallyCreated() && ((BComplex)object).getPropertyInParent() == null) {
                     ((BComponent)this.obj.getParent()).remove(this.obj.getPropertyInParent());
                  }
            }
         } catch (Exception var3) {
            BBacnetTrendLogDescriptor.logger.warning("Exception occurred handling event " + this.obj.getObjectId() + ": " + var3);
         }
      }
   }
}
