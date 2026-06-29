package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.stack.server.BBacnetExportFolder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.alarm.ext.BOffnormalAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetOptionalReal;
import javax.baja.bacnet.datatypes.BBacnetSetpointReference;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetEngineeringUnits;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetEventType;
import javax.baja.bacnet.enums.BBacnetNotifyType;
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
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.BControlPoint;
import javax.baja.control.BNumericPoint;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BIcon;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Knob;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.units.BUnit;
import javax.baja.util.Lexicon;

@NiagaraType(
   agent = {@AgentOn(
      types = {"kitControl:LoopPoint"}
   )}
)
@NiagaraProperties({@NiagaraProperty(
      name = "pointOrd",
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
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.LOOP)",
      flags = 64
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = "",
      flags = 64
   ), @NiagaraProperty(
      name = "reliability",
      type = "BEnum",
      defaultValue = "BBacnetReliability.noFaultDetected",
      flags = 3
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.alarm",
      facets = {@Facet("BacUtil.makeBacnetNotifyTypeFacets()")}
   ), @NiagaraProperty(
      name = "covIncrement",
      type = "float",
      defaultValue = "0.0F"
   ), @NiagaraProperty(
      name = "updateInterval",
      type = "BRelTime",
      defaultValue = "BRelTime.make(UPDATE_INTERVAL)",
      flags = 1
   )})
@NiagaraActions({@NiagaraAction(
      name = "addCovSubscription",
      parameterType = "BBacnetCovSubscription",
      defaultValue = "new BBacnetCovSubscription()",
      flags = 4
   ), @NiagaraAction(
      name = "removeCovSubscription",
      parameterType = "BBacnetCovSubscription",
      defaultValue = "new BBacnetCovSubscription()",
      flags = 4
   ), @NiagaraAction(
      name = "sendCovNotification",
      parameterType = "BBacnetCovSubscription",
      defaultValue = "new BBacnetCovSubscription()",
      flags = 4
   ), @NiagaraAction(
      name = "checkCov",
      flags = 20
   )})
public class BBacnetLoopDescriptor extends BBacnetEventSource implements BIBacnetCovSource, BacnetPropertyListProvider {
   public static final long UPDATE_INTERVAL = 10000L;
   public static final Property pointOrd = newProperty(64, BOrd.DEFAULT, BFacets.make("targetType", "baja:Component"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.make(12), null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property reliability = newProperty(3, BBacnetReliability.noFaultDetected, null);
   public static final Property description = newProperty(0, "", null);
   public static final Property notifyType = newProperty(0, BBacnetNotifyType.alarm, BacUtil.makeBacnetNotifyTypeFacets());
   public static final Property covIncrement = newProperty(0, 0.0F, null);
   public static final Property updateInterval = newProperty(1, BRelTime.make(10000L), null);
   public static final Action addCovSubscription = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action removeCovSubscription = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action sendCovNotification = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action checkCov = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetLoopDescriptor.class);
   private static final BIcon icon = BIcon.make(BIcon.std("control/numericPoint.png"), BIcon.std("badges/export.png"));
   private BNumericPoint point;
   private int[] optionalProps;
   private int lastStatusBits = -1;
   private int oldNotifyType;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private boolean configOk;
   static final AsnInputStream asnIn = new AsnInputStream();
   static final AsnOutputStream asnOut = new AsnOutputStream();
   static Logger log = Logger.getLogger("bacnet.server");
   static Lexicon lex = Lexicon.make("bacnet");
   private static final BBacnetObjectPropertyReference NULL_OPR = new BBacnetObjectPropertyReference();
   private static final int[] REQUIRED_PROPS = new int[]{75, 77, 79, 85, 111, 36, 81, 82, 60, 19, 21, 20, 109, 108, 2, 88};

   public BOrd getPointOrd() {
      return (BOrd)this.get(pointOrd);
   }

   public void setPointOrd(BOrd v) {
      this.set(pointOrd, v, null);
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

   public BEnum getReliability() {
      return (BEnum)this.get(reliability);
   }

   public void setReliability(BEnum v) {
      this.set(reliability, v, null);
   }

   public String getDescription() {
      return this.getString(description);
   }

   public void setDescription(String v) {
      this.setString(description, v, null);
   }

   @Override
   public BBacnetNotifyType getNotifyType() {
      return (BBacnetNotifyType)this.get(notifyType);
   }

   public void setNotifyType(BBacnetNotifyType v) {
      this.set(notifyType, v, null);
   }

   public float getCovIncrement() {
      return this.getFloat(covIncrement);
   }

   public void setCovIncrement(float v) {
      this.setFloat(covIncrement, v, null);
   }

   public BRelTime getUpdateInterval() {
      return (BRelTime)this.get(updateInterval);
   }

   public void setUpdateInterval(BRelTime v) {
      this.set(updateInterval, v, null);
   }

   @Override
   public void addCovSubscription(BBacnetCovSubscription parameter) {
      this.invoke(addCovSubscription, parameter, null);
   }

   @Override
   public void removeCovSubscription(BBacnetCovSubscription parameter) {
      this.invoke(removeCovSubscription, parameter, null);
   }

   public void sendCovNotification(BBacnetCovSubscription parameter) {
      this.invoke(sendCovNotification, parameter, null);
   }

   @Override
   public void checkCov() {
      this.invoke(checkCov, null, null);
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
      this.checkConfiguration();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      if (this.getOosExt() != null) {
         this.removeOutOfServiceExt();
      }

      local.unsubscribe(this, this.point);
      this.point = null;
      this.oldId = null;
      this.oldName = null;
      if (local.isRunning()) {
         local.incrementDatabaseRevision();
      }
   }

   public final void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         ;
      }
   }

   public final void removed(Property p, BValue oldValue, Context cx) {
      super.removed(p, oldValue, cx);
   }

   @Override
   public void changed(Property p, Context cx) {
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
         } else if (p.equals(pointOrd)) {
            this.checkConfiguration();
            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(notifyType)) {
            if (this.getNotifyType() == BBacnetNotifyType.ackNotification) {
               log.warning("Invalid Notify Type for " + this);
               this.setNotifyType(BBacnetNotifyType.make(this.oldNotifyType));
            } else {
               this.oldNotifyType = this.getNotifyType().getOrdinal();
            }
         } else if (p.equals(description)) {
            BBacnetExportFolder f = this.getSvo();
            if (f != null) {
               f.fireSubordinateAnnotationChanged(null);
            }
         }
      }
   }

   public final void doAddCovSubscription(BBacnetCovSubscription sub) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Adding Cov subscription: " + sub + " on " + this);
      }

      Property p = this.add("covSubscription?", sub, 3);
      BBacnetNetwork.localDevice().subscribeCov(this, this.getPoint(), p);
   }

   public final void doRemoveCovSubscription(BBacnetCovSubscription sub) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Removing Cov subscription: " + sub + " on " + this);
      }

      Ticket ticket = sub.getTicket();
      if (ticket != null) {
         ticket.cancel();
      }

      sub.setTicket(null);
      Property p = this.getProperty(sub.getName());
      if (p != null) {
         this.remove(p);
      }

      BBacnetNetwork.localDevice().unsubscribeCov(this, this.getPoint(), p);
   }

   public void doSendCovNotification(BBacnetCovSubscription covSub) {
      BNumericPoint pt = this.getPoint();
      if (covSub.getTimeRemaining() < 0) {
         this.removeCovSubscription(covSub);
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine("Sending Cov Notification: pt=" + pt + ", covSub=" + covSub);
         }

         Cov cov = new Cov(covSub, this, pt);
         BBacnetNetwork.bacnet().postAsync(cov);
         if (covSub.isCovProperty()) {
            covSub.setLastPropValue(this.getCurrentCovValue(covSub));
            covSub.setLastStatusBits(this.getStatusFlags().getBits() & 43);
         } else {
            covSub.setLastValue(this.getCurrentStatusValue());
         }
      }
   }

   public final void doCheckCov() {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetCovSubscription.class)) {
         BBacnetCovSubscription covSub = (BBacnetCovSubscription)c.get();
         if (covSub.isCovProperty()) {
            try {
               PropertyValue pv = this.readProperty(
                  covSub.getMonitoredPropertyReference().getPropertyId(), covSub.getMonitoredPropertyReference().getPropertyArrayIndex()
               );
               PropertyInfo pi = BBacnetNetwork.localDevice()
                  .getPropertyInfo(this.getObjectId().getObjectType(), covSub.getMonitoredPropertyReference().getPropertyId());
               BValue cv = AsnUtil.asnToValue(pi, pv.getPropertyValue());
               int cs = this.getStatusFlags().getBits() & 43;
               if (cs != covSub.getLastStatusBits()) {
                  this.sendCovNotification(covSub);
                  return;
               }

               if (pi.getAsnType() == 4) {
                  double diff = Math.abs(((BINumeric)cv).getNumeric() - ((BINumeric)covSub.getLastPropValue()).getNumeric());
                  double covIncrement = covSub.getCovIncrement();
                  if (Double.isNaN(covIncrement)) {
                     if (covSub.getMonitoredPropertyReference().getPropertyId() == 85) {
                        BDouble d = (BDouble)this.get("covIncrement");
                        covIncrement = d != null ? d.getDouble() : 0.0;
                     } else {
                        covIncrement = 0.0;
                     }
                  }

                  if (diff >= covIncrement) {
                     this.sendCovNotification(covSub);
                  }
               } else if (!cv.equals(covSub.getLastPropValue())) {
                  this.sendCovNotification(covSub);
               }
            } catch (AsnException var12) {
               logger.warning("AsnException occurred in doCheckCov: " + var12);
            }
         } else if (this.checkCov(this.getCurrentStatusValue(), covSub.getLastValue())) {
            this.sendCovNotification(covSub);
         }
      }
   }

   @Override
   public final BObject getObject() {
      return this.getPoint();
   }

   @Override
   public final BOrd getObjectOrd() {
      return this.getPointOrd();
   }

   @Override
   public final void setObjectOrd(BOrd objectOrd, Context cx) {
      this.set(pointOrd, objectOrd, cx);
   }

   @Override
   public void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else {
         local.unsubscribe(this, this.point);
         this.findPoint();
         boolean cfgOk = true;
         if (this.point == null) {
            this.setFaultCause("Cannot find exported loop");
            cfgOk = false;
         } else {
            local.subscribe(this, this.point);
         }

         if (!this.getObjectId().isValid()) {
            this.setFaultCause("Invalid Object ID");
            cfgOk = false;
         }

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
            this.setReliability(BBacnetReliability.noFaultDetected);
            this.setFaultCause("");
            this.validate();
         } else {
            this.setReliability(BBacnetReliability.unreliableOther);
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
         }

         if (this.configOk()) {
            this.getOosExt();
         }
      }
   }

   @Override
   public boolean isValidAlarmExt(BIAlarmSource ext) {
      return ext instanceof BAlarmSourceExt
         ? ((BAlarmSourceExt)ext).getOffnormalAlgorithm().getType().toString().equals("kitControl:LoopAlarmAlgorithm")
         : false;
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
      BAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BBacnetEventState.make(almExt.getAlarmState());
   }

   @Override
   public final BBacnetBitString getAckedTransitions() {
      BAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAckedTransitions());
   }

   @Override
   public final BBacnetTimeStamp[] getEventTimeStamps() {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt == null) {
         return null;
      } else {
         BBacnetTimeStamp[] ets = new BBacnetTimeStamp[3];
         BAbsTime t = almExt.getLastOffnormalTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[0] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[0] = new BBacnetTimeStamp(t);
         }

         t = almExt.getLastFaultTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[1] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[1] = new BBacnetTimeStamp(t);
         }

         t = almExt.getLastToNormalTime();
         if (BAbsTime.DEFAULT.equals(t)) {
            ets[2] = new BBacnetTimeStamp(new BBacnetDateTime());
         } else {
            ets[2] = new BBacnetTimeStamp(t);
         }

         return ets;
      }
   }

   @Override
   public final BBacnetBitString getEventEnable() {
      BAlarmSourceExt almExt = this.getAlarmExt();
      return almExt == null ? null : BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable());
   }

   @Override
   public final int[] getEventPriorities() {
      BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
      return nc == null ? null : nc.getEventPriorities();
   }

   @Override
   public final BBacnetNotificationClassDescriptor getNotificationClass() {
      BBacnetNotificationClassDescriptor nc = null;
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt == null) {
         return null;
      } else {
         try {
            BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
            BAlarmClass ac = as.lookupAlarmClass(almExt.getAlarmClass());
            BBacnetObjectIdentifier ncId = BBacnetNetwork.localDevice().lookupBacnetObjectId(ac.getHandleOrd());
            if (ncId != null) {
               nc = (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(ncId);
               if (nc == null) {
                  log.warning("Can't find Notification Class Descriptor for ID " + ncId);
               }
            } else {
               log.warning("Alarm Class '" + ac + "' is used for BACnet-exposed object " + this + ", but is not exposed as a BACnet Notification Class");
            }
         } catch (ServiceNotFoundException var6) {
            log.log(Level.SEVERE, "getNotificationClass on " + this + ": Unable to find alarm service", (Throwable)var6);
         }

         return nc;
      }
   }

   @Override
   public BEnum getEventType() {
      return BBacnetEventType.floatingLimit;
   }

   @Override
   public BIBacnetExportObject getExport() {
      return this;
   }

   @Override
   public final BBacnetCovSubscription findCovSubscription(BBacnetAddress subscriberAddress, long processId, BBacnetObjectIdentifier objectId) {
      return this.findSubscription(false, subscriberAddress, processId, objectId, 85, -1);
   }

   @Override
   public final BBacnetCovSubscription findCovPropertySubscription(
      BBacnetAddress subscriberAddress, long processId, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex
   ) {
      return this.findSubscription(true, subscriberAddress, processId, objectId, propertyId, -1);
   }

   @Override
   public final void startCovTimer(BBacnetCovSubscription covSub, long lifetime) {
      Ticket ticket = covSub.getTicket();
      if (ticket != null) {
         ticket.cancel();
      }

      if (lifetime > 0L) {
         BRelTime subLife = BRelTime.make((int)lifetime * 1000L);
         covSub.setSubscriptionEndTime(BAbsTime.make().add(subLife));
         covSub.setTicket(Clock.schedule(this, subLife, removeCovSubscription, covSub));
      }

      this.sendCovNotification(covSub);
   }

   @Override
   public Property getOutProperty() {
      return this.getPoint().getOutProperty();
   }

   @Override
   public boolean supportsSubscribeCov() {
      return true;
   }

   @Override
   public BValue getCurrentCovValue(BBacnetCovSubscription sub) {
      PropertyValue pv = this.readProperty(sub.getMonitoredPropertyReference().getPropertyId(), sub.getMonitoredPropertyReference().getPropertyArrayIndex());

      try {
         return AsnUtil.asnToValue(
            BBacnetNetwork.localDevice().getPropertyInfo(this.getObjectId().getObjectType(), sub.getMonitoredPropertyReference().getPropertyId()),
            pv.getPropertyValue()
         );
      } catch (AsnException var4) {
         logger.warning("AsnException occurred in getCurrentCovValue in object " + this.getObjectId() + ": " + var4);
         return null;
      }
   }

   final BStatusValue getCurrentStatusValue() {
      BStatusValue sv = (BStatusValue)this.getPoint().getOutStatusValue().newCopy(true);
      sv.setStatus(this.getStatusFlags());
      return sv;
   }

   boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      return currentValue.getStatus().getBits() != covValue.getStatus().getBits()
         ? true
         : Math.abs(((BINumeric)currentValue).getNumeric() - ((BINumeric)covValue).getNumeric()) >= this.getCovIncrement();
   }

   @Override
   public final PropertyValue readProperty(PropertyReference ref) throws RejectException {
      this.getPoint();
      return this.readProperty(ref.getPropertyId(), ref.getPropertyArrayIndex());
   }

   @Override
   public final PropertyValue[] readPropertyMultiple(PropertyReference[] refs) throws RejectException {
      this.getPoint();
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
      int propertyId = rangeReference.getPropertyId();
      return !this.hasProperty(propertyId)
         ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
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
      this.getPoint();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public final ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public final ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   boolean isArray(int propertyId) {
      switch (propertyId) {
         case 130:
         case 351:
         case 352:
         case 371:
            return true;
         default:
            return false;
      }
   }

   BStatus getStatusFlags() {
      this.getPoint();
      int status = this.point == null ? 2 : this.point.getStatus().getBits();
      if (this.point != null && this.point.getStatus().isDown()) {
         status |= 2;
      }

      if (this.point != null && ((BStatusBoolean)this.point.get("loopEnable")).getValue() && !this.getOosExt().getOutOfService()) {
         status &= -2;
      } else {
         status |= 1;
      }

      if (this.getReliability() != BBacnetReliability.noFaultDetected) {
         status |= 2;
      }

      return BStatus.make(status);
   }

   @Override
   public void statusChanged() {
      BStatus status = this.getStatusFlags();
      if (this.lastStatusBits != status.getBits()) {
         this.lastStatusBits = status.getBits();
         this.setBacnetStatusFlags(BBacnetBitString.make(new boolean[]{status.isAlarm(), status.isFault(), status.isOverridden(), status.isDisabled()}));
      }
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(REQUIRED_PROPS, this.getOptionalProps());
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (this.point == null) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(1, 1000));
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NReadPropertyResult(pId, ndx, new NErrorType(2, 50));
      } else {
         try {
            switch (pId) {
               case 2:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(((BEnum)this.point.get("loopAction")).getOrdinal()));
               case 14:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("bias")).getDouble()));
               case 19:
                  return new NReadPropertyResult(pId, ndx, this.getControlledVariableReference());
               case 20: {
                  BUnit u = (BUnit)((BFacets)this.point.get("inputFacets")).getFacet("units");
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(BBacnetEngineeringUnits.make(u)));
               }
               case 21:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BStatusNumeric)this.point.get("controlledVariable")).getValue()));
               case 22:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(this.getCovIncrement()));
               case 26:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("derivativeConstant")).getDouble()));
               case 27:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(73));
               case 28:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getDescription()));
               case 36:
                  return this.readEventState();
               case 49:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("integralConstant")).getDouble()));
               case 50:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(100));
               case 60:
                  return new NReadPropertyResult(pId, ndx, this.getManipulatedVariableReference());
               case 61:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("maximumOutput")).getDouble()));
               case 68:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("minimumOutput")).getDouble()));
               case 75:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnObjectId(this.getObjectId()));
               case 77:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnCharacterString(this.getObjectName()));
               case 79:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getObjectId().getObjectType()));
               case 81:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getOosExt().getOutOfService()));
               case 82: {
                  BUnit u = (BUnit)this.point.getFacets().getFacet("units");
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(BBacnetEngineeringUnits.make(u)));
               }
               case 85:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(this.point.getOut().getValue()));
               case 88:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getPriorityForWriting()));
               case 93:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)this.point.get("proportionalConstant")).getDouble()));
               case 94:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(95));
               case 103:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getReliability()));
               case 108:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BStatusNumeric)this.point.get("setpoint")).getValue()));
               case 109:
                  return new NReadPropertyResult(pId, ndx, this.getSetpointReference());
               case 111:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.statusToAsnStatusFlags(this.getStatusFlags()));
               case 118:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(this.getUpdateInterval().getMillis() / 10L));
               case 371:
                  return this.readPropertyList(ndx);
               default:
                  return this.readOptionalProperty(pId, ndx);
            }
         } catch (NullPointerException var5) {
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
      }
   }

   private PropertyValue readEventState() {
      if (!this.getEventDetectionEnable()) {
         return makeEventStatePropertyValue(0);
      } else {
         BAlarmSourceExt alarmExt = this.getAlarmExt();
         if (alarmExt == null) {
            return makeEventStatePropertyValue(0);
         } else {
            BAlarmState alarmState = alarmExt.getAlarmState();
            if (alarmState.isOffnormal()) {
               double cv = ((BStatusNumeric)this.point.get("controlledVariable")).getValue();
               double set = ((BStatusNumeric)this.point.get("setpoint")).getValue();
               alarmState = cv > set ? BAlarmState.highLimit : BAlarmState.lowLimit;
            }

            return makeEventStatePropertyValue(BBacnetEventState.fromBAlarmState(alarmState));
         }
      }
   }

   private static PropertyValue makeEventStatePropertyValue(int eventState) {
      return new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(eventState));
   }

   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BOffnormalAlgorithm alg = almExt.getOffnormalAlgorithm();

         try {
            switch (pId) {
               case 0:
                  return this.readAckedTransitions(almExt.getAckedTransitions());
               case 17:
                  BBacnetNotificationClassDescriptor nc = this.getNotificationClass();
                  if (nc == null) {
                     return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
                  }

                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(nc.getNotificationClass()));
               case 25:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)alg.get("deadband")).getDouble()));
               case 34:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(((BDouble)alg.get("errorLimit")).getDouble()));
               case 35:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(almExt.getAlarmEnable())));
               case 72:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getNotifyType()));
               case 113:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getTimeDelay().getMillis() / 1000L));
               case 130:
                  return this.readEventTimeStamps(almExt.getLastOffnormalTime(), almExt.getLastFaultTime(), almExt.getLastToNormalTime(), ndx);
               case 351:
                  return this.readEventMessageTexts(ndx);
               case 352:
                  return this.readEventMessageTextsConfig(
                     almExt.getToOffnormalText().getFormat(), almExt.getToFaultText().getFormat(), almExt.getToNormalText().getFormat(), ndx
                  );
               case 353:
                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getEventDetectionEnable()));
               case 390:
                  boolean lowDiffLimitEnabled = ((BBoolean)alg.get("lowDiffLimitEnabled")).getBoolean();
                  if (lowDiffLimitEnabled) {
                     double value = ((BDouble)alg.get("lowDiffLimit")).getDouble();
                     return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnReal(value));
                  }

                  return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnNull());
            }
         } catch (NullPointerException var9) {
            return new NReadPropertyResult(pId, ndx, new NErrorType(2, 32));
         }
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
      if (this.point == null) {
         return new NErrorType(1, 1000);
      } else if (ndx >= 0 && !this.isArray(pId)) {
         return new NErrorType(2, 50);
      } else {
         try {
            switch (pId) {
               case 2:
                  return new NErrorType(2, 40);
               case 14:
                  this.point.set(this.point.getProperty("bias"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 19:
               case 20:
               case 21:
               case 60:
               case 82:
               case 109:
                  return new NErrorType(2, 40);
               case 22:
                  this.setFloat(covIncrement, AsnUtil.fromAsnReal(val), BLocalBacnetDevice.getBacnetContext());
                  this.checkCov();
                  return null;
               case 26:
                  this.point.set(this.point.getProperty("derivativeConstant"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 27:
                  return new NErrorType(2, 40);
               case 28:
                  this.setString(description, AsnUtil.fromAsnCharacterString(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 36:
               case 103:
               case 111:
               case 118:
                  return new NErrorType(2, 40);
               case 49:
                  this.point.set(this.point.getProperty("integralConstant"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 50:
                  return new NErrorType(2, 40);
               case 61:
                  this.point.set(this.point.getProperty("maximumOutput"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 68:
                  this.point.set(this.point.getProperty("minimumOutput"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 75:
               case 79:
               case 85:
                  if (this.getOosExt().getOutOfService()) {
                     this.getOosExt().set(BOutOfServiceExt.presentValue, BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                     return null;
                  }

                  return new NErrorType(2, 40);
               case 77:
                  return BacUtil.setObjectName(this, objectName, val);
               case 81:
                  this.getOosExt().setBoolean(BOutOfServiceExt.outOfService, AsnUtil.fromOnlyAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 88:
                  return new NErrorType(2, 40);
               case 93:
                  this.point.set(this.point.getProperty("proportionalConstant"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 94:
                  return new NErrorType(2, 40);
               case 108:
                  BLink[] links = this.point.getLinks(this.point.getProperty("setpoint"));
                  if (links.length > 0) {
                     return new NErrorType(2, 40);
                  }

                  this.point.set(this.point.getProperty("setpoint"), new BStatusNumeric(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 371:
                  return new NErrorType(2, 40);
               default:
                  return this.writeOptionalProperty(pId, ndx, val, pri);
            }
         } catch (AsnException var6) {
            log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var6);
            return new NErrorType(2, 9);
         } catch (PermissionException var7) {
            log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var7);
            return new NErrorType(2, 40);
         } catch (NullPointerException var8) {
            log.warning("Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var8);
            return new NErrorType(2, 32);
         }
      }
   }

   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      try {
         BAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt != null) {
            BOffnormalAlgorithm alg = almExt.getOffnormalAlgorithm();
            switch (pId) {
               case 0:
               case 72:
               case 130:
               case 351:
                  return new NErrorType(2, 40);
               case 17:
                  int ncinst = AsnUtil.fromAsnUnsignedInt(val);
                  if (ncinst > 4194302) {
                     return new NErrorType(2, 37);
                  }

                  BBacnetNotificationClassDescriptor ncd = (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice()
                     .lookupBacnetObject(BBacnetObjectIdentifier.make(15, ncinst));
                  if (ncd == null) {
                     return new NErrorType(2, 37);
                  }

                  BAlarmClass ac = ncd.getAlarmClass();
                  almExt.setAlarmClass(ac.getName());
                  return null;
               case 25:
                  alg.set(alg.getProperty("deadband"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 34:
                  alg.set(alg.getProperty("errorLimit"), BDouble.make(AsnUtil.fromAsnReal(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 35:
                  almExt.set(
                     BAlarmSourceExt.alarmEnable,
                     BacnetBitStringUtil.getBAlarmTransitionBits(AsnUtil.fromAsnBitString(val)),
                     BLocalBacnetDevice.getBacnetContext()
                  );
                  return null;
               case 113:
                  almExt.set(BAlarmSourceExt.timeDelay, BRelTime.make(AsnUtil.fromAsnUnsignedInteger(val) * 1000L), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 352:
                  return writeEventMessageTextsConfig(ndx, val, almExt);
               case 353:
                  this.setBoolean(eventDetectionEnable, AsnUtil.fromAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 390:
                  BBacnetOptionalReal lowDiffLimit = new BBacnetOptionalReal();
                  AsnUtil.fromAsn(-4, val, lowDiffLimit);
                  Context context = BLocalBacnetDevice.getBacnetContext();
                  if (lowDiffLimit.isNull()) {
                     alg.set(alg.getProperty("lowDiffLimitEnabled"), BBoolean.FALSE, context);
                  } else {
                     alg.set(alg.getProperty("lowDiffLimitEnabled"), BBoolean.TRUE, context);
                     alg.set(alg.getProperty("lowDiffLimit"), BDouble.make(lowDiffLimit.getValue()), context);
                  }

                  return null;
            }
         }
      } catch (OutOfRangeException var12) {
         log.warning("OutOfRangeException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
         return new NErrorType(2, 37);
      } catch (AsnException var13) {
         log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var13);
         return new NErrorType(2, 9);
      } catch (PermissionException var14) {
         log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var14);
         return new NErrorType(2, 40);
      } catch (NullPointerException var15) {
         log.warning("Exception writing property " + pId + " in object " + this.getObjectId() + ": " + var15);
         return new NErrorType(2, 32);
      }

      return new NErrorType(2, 32);
   }

   private int[] getOptionalProps() {
      ArrayList<BBacnetPropertyIdentifier> v = new ArrayList<>();
      v.add(BBacnetPropertyIdentifier.description);
      v.add(BBacnetPropertyIdentifier.reliability);
      v.add(BBacnetPropertyIdentifier.proportionalConstant);
      v.add(BBacnetPropertyIdentifier.proportionalConstantUnits);
      v.add(BBacnetPropertyIdentifier.integralConstant);
      v.add(BBacnetPropertyIdentifier.integralConstantUnits);
      v.add(BBacnetPropertyIdentifier.derivativeConstant);
      v.add(BBacnetPropertyIdentifier.derivativeConstantUnits);
      v.add(BBacnetPropertyIdentifier.bias);
      v.add(BBacnetPropertyIdentifier.maximumOutput);
      v.add(BBacnetPropertyIdentifier.minimumOutput);
      v.add(BBacnetPropertyIdentifier.covIncrement);
      v.add(BBacnetPropertyIdentifier.updateInterval);
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         BOffnormalAlgorithm alg = almExt.getOffnormalAlgorithm();
         if (alg.getType().getTypeName().equals("LoopAlarmAlgorithm")) {
            v.add(BBacnetPropertyIdentifier.timeDelay);
            v.add(BBacnetPropertyIdentifier.notificationClass);
            v.add(BBacnetPropertyIdentifier.eventEnable);
            v.add(BBacnetPropertyIdentifier.ackedTransitions);
            v.add(BBacnetPropertyIdentifier.notifyType);
            v.add(BBacnetPropertyIdentifier.eventTimeStamps);
            v.add(BBacnetPropertyIdentifier.eventMessageTexts);
            v.add(BBacnetPropertyIdentifier.eventMessageTextsConfig);
            v.add(BBacnetPropertyIdentifier.deadband);
            v.add(BBacnetPropertyIdentifier.errorLimit);
            v.add(BBacnetPropertyIdentifier.eventDetectionEnable);
            v.add(BBacnetPropertyIdentifier.lowDiffLimit);
         }
      }

      this.optionalProps = new int[v.size()];

      for (int i = 0; i < this.optionalProps.length; i++) {
         this.optionalProps[i] = ((BEnum)v.get(i)).getOrdinal();
      }

      return this.optionalProps;
   }

   @Override
   void checkValid() {
      if (this.configOk()) {
         this.validate();
      }
   }

   private synchronized boolean configOk() {
      return this.configOk;
   }

   private void validate() {
      BStatusNumeric sn = this.getPoint().getOut();
      BStatus s = sn.getStatus();
      if (s.isNull()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Invalid value for BACnet Object:" + sn);
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      } else if (!s.isFault() && !s.isDown()) {
         this.setReliability(BBacnetReliability.noFaultDetected);
         if (this.configOk()) {
            this.setStatus(BStatus.makeFault(this.getStatus(), false));
            this.setFaultCause("");
         } else {
            this.setStatus(BStatus.makeFault(this.getStatus(), true));
            this.setFaultCause(lex.getText("export.configurationFault"));
         }
      } else {
         this.setReliability(BBacnetReliability.unreliableOther);
      }
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   public final BNumericPoint getPoint() {
      return this.point == null ? this.findPoint() : this.point;
   }

   private BAlarmSourceExt getAlarmExt() {
      BControlPoint point = this.getPoint();
      if (point == null) {
         return null;
      } else {
         SlotCursor<Property> c = point.getProperties();

         while (c.next(BAlarmSourceExt.class)) {
            BAlarmSourceExt ext = (BAlarmSourceExt)c.get();
            if (this.isValidAlarmExt(ext)) {
               return ext;
            }
         }

         return null;
      }
   }

   private BOutOfServiceExt getOosExt() {
      BControlPoint point = this.getPoint();
      if (point == null) {
         return null;
      } else {
         BOutOfServiceExt outOfServiceExt = null;
         SlotCursor<Property> c = point.getProperties();
         if (c.next(BOutOfServiceExt.class)) {
            outOfServiceExt = (BOutOfServiceExt)c.get();
         }

         if (outOfServiceExt == null) {
            outOfServiceExt = new BOutOfServiceExt();
            point.add("outOfServiceExt?", outOfServiceExt);
         }

         outOfServiceExt.setExport(this);
         outOfServiceExt.setCommandable(false);
         return outOfServiceExt;
      }
   }

   private BNumericPoint findPoint() {
      try {
         if (!pointOrd.isEquivalentToDefaultValue(this.getPointOrd())) {
            BObject o = this.getPointOrd().get(this);
            if (o instanceof BNumericPoint && o.getType().toString().equals("kitControl:LoopPoint")) {
               this.point = (BNumericPoint)o;
            } else {
               this.point = null;
            }
         }
      } catch (Exception var2) {
         log.warning("Unable to resolve point ord for " + this + ": " + this.getPointOrd());
         this.point = null;
      }

      if (this.point == null && this.isRunning()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Cannot find exported loop");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      return this.point;
   }

   private byte[] getManipulatedVariableReference() {
      BBacnetObjectPropertyReference opr = NULL_OPR;
      Knob[] knobs = this.point.getKnobs(this.point.getProperty("out"));
      if (knobs.length > 0) {
         BOrd tgtOrd = knobs[0].getTargetOrd();
         BBacnetObjectIdentifier tgtId = BBacnetNetwork.localDevice().lookupBacnetObjectId(tgtOrd);
         if (tgtId != null) {
            opr = new BBacnetObjectPropertyReference(tgtId);
         }
      }

      return AsnUtil.toAsn(opr);
   }

   private byte[] getControlledVariableReference() {
      BBacnetObjectPropertyReference opr = NULL_OPR;
      BLink[] links = this.point.getLinks(this.point.getSlot("controlledVariable"));
      if (links.length > 0) {
         BOrd tgtOrd = links[0].getSourceComponent().getAbsoluteOrd();
         BBacnetObjectIdentifier tgtId = BBacnetNetwork.localDevice().lookupBacnetObjectId(tgtOrd);
         if (tgtId != null) {
            opr = new BBacnetObjectPropertyReference(tgtId);
         }
      }

      return AsnUtil.toAsn(opr);
   }

   private byte[] getSetpointReference() {
      BLink[] links = this.point.getLinks(this.point.getProperty("setpoint"));
      if (links.length > 0) {
         BOrd srcOrd = links[0].getSourceComponent().getAbsoluteOrd();
         BBacnetObjectIdentifier srcId = BBacnetNetwork.localDevice().lookupBacnetObjectId(srcOrd);
         if (srcId != null) {
            BBacnetSetpointReference spr = new BBacnetSetpointReference(new BBacnetObjectPropertyReference(srcId));
            return AsnUtil.toAsn(spr);
         }
      }

      return new byte[0];
   }

   private int getPriorityForWriting() {
      Knob[] knobs = this.point.getKnobs(this.point.getProperty("out"));
      if (knobs.length == 0) {
         return 16;
      } else {
         String s = knobs[0].getTargetSlotName();

         try {
            return Integer.parseInt(s.substring(2));
         } catch (Exception var4) {
            return 16;
         }
      }
   }

   private BBacnetCovSubscription findSubscription(
      boolean covProperty, BBacnetAddress subscriberAddress, long processId, BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex
   ) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetCovSubscription.class)) {
         BBacnetCovSubscription sub = (BBacnetCovSubscription)c.get();
         if (sub.isCovProperty() == covProperty
            && sub.getRecipient().getRecipient().getAddress().equals(subscriberAddress.getNetworkNumber(), subscriberAddress.getMacAddress().getBytes())
            && sub.getRecipient().getProcessIdentifier().getUnsigned() == processId
            && this.getObjectId().equals(objectId)
            && sub.getMonitoredPropertyReference().getPropertyId() == propertyId
            && sub.getMonitoredPropertyReference().getPropertyArrayIndex() == propertyArrayIndex) {
            return sub;
         }
      }

      return null;
   }

   private void removeOutOfServiceExt() {
      BControlPoint point = this.getPoint();
      if (point != null) {
         Object[] outOfServiceExts = point.getChildren(BOutOfServiceExt.class);
         if (outOfServiceExts != null && outOfServiceExts.length > 0 && outOfServiceExts[0] instanceof BOutOfServiceExt) {
            point.remove((BOutOfServiceExt)outOfServiceExts[0]);
         }
      }
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetLoopDescriptor", 2);
      out.prop("point", this.point);
      out.prop("oldId", this.oldId);
      out.prop("oldName", this.oldName);
      out.prop("duplicate", this.duplicate);
      out.prop("oldNotifyType", this.oldNotifyType);
      out.prop("almExt", this.getAlarmExt());
      out.prop("notificationClass", this.getNotificationClass());
      out.prop("configOk", this.configOk());
      out.prop("oosExt", this.getOosExt());
      out.endProps();
   }

   public BIcon getIcon() {
      return icon;
   }
}
