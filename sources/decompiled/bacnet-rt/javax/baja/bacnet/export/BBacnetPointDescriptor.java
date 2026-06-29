package javax.baja.bacnet.export;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.services.confirmed.ReadRangeAck;
import com.tridium.bacnet.stack.server.BBacnetExportFolder;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import com.tridium.bacnet.stack.server.BOverrideMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BFaultAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetCovSubscription;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
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
import javax.baja.control.BPointExtension;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.control.ext.BNullProxyExt;
import javax.baja.driver.point.BProxyExt;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.PermissionException;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.DuplicateSlotException;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.Lexicon;

@NiagaraType
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
      defaultValue = "BBacnetObjectIdentifier.DEFAULT",
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
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "description",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "notifyType",
      type = "BBacnetNotifyType",
      defaultValue = "BBacnetNotifyType.alarm",
      facets = {@Facet("BacUtil.makeBacnetNotifyTypeFacets()")}
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
      name = "makeWritable",
      parameterType = "BValue",
      defaultValue = "BString.DEFAULT",
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
public abstract class BBacnetPointDescriptor extends BBacnetEventSource implements BIBacnetCovSource, BacnetPropertyListProvider {
   public static final Property pointOrd = newProperty(64, BOrd.DEFAULT, BFacets.make("targetType", "baja:Component"));
   public static final Property objectId = newProperty(64, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property objectName = newProperty(64, "", null);
   public static final Property reliability = newProperty(3, BBacnetReliability.noFaultDetected, null);
   public static final Property outOfService = newProperty(5, false, null);
   public static final Property description = newProperty(0, "", null);
   public static final Property notifyType = newProperty(0, BBacnetNotifyType.alarm, BacUtil.makeBacnetNotifyTypeFacets());
   public static final Action addCovSubscription = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action removeCovSubscription = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action makeWritable = newAction(4, BString.DEFAULT, null);
   public static final Action sendCovNotification = newAction(4, new BBacnetCovSubscription(), null);
   public static final Action checkCov = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBacnetPointDescriptor.class);
   private static final int[] ARRAY_PROPS = new int[]{130, 351, 352, 371, 87, 110};
   private BControlPoint point;
   private int[] requiredProps;
   private int[] optionalProps;
   private int lastStatusBits = -1;
   private int oldNotifyType;
   private BBacnetObjectIdentifier oldId = null;
   private String oldName = null;
   private boolean duplicate = false;
   private boolean configOk;
   private BBacnetPointDescriptor.OrdStatus ordStatus = BBacnetPointDescriptor.OrdStatus.OK;
   static final AsnInputStream asnIn = new AsnInputStream();
   static final AsnOutputStream asnOut = new AsnOutputStream();
   static Logger log = Logger.getLogger("bacnet.server");
   static Lexicon lex = Lexicon.make("bacnet");
   static String lexNotWritable = lex.getText("server.notWritable");

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

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
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

   @Override
   public void addCovSubscription(BBacnetCovSubscription parameter) {
      this.invoke(addCovSubscription, parameter, null);
   }

   @Override
   public void removeCovSubscription(BBacnetCovSubscription parameter) {
      this.invoke(removeCovSubscription, parameter, null);
   }

   public void makeWritable(BValue parameter) {
      this.invoke(makeWritable, parameter, null);
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
   public void started() throws Exception {
      super.started();
      this.oldId = this.getObjectId();
      this.oldName = this.getObjectName();
      this.checkConfiguration();
      this.reliabilityChanged();
      if (Sys.isStationStarted()) {
         BBacnetNetwork.localDevice().incrementDatabaseRevision();
      }
   }

   public final void stopped() throws Exception {
      super.stopped();
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      local.unexport(this.oldId, this.oldName, this);
      this.clearTrendReferences(local);
      if (this.getOosExt() != null) {
         this.removeOutOfServiceExt();
      }

      local.unsubscribe(this, this.point);
      this.requiredProps = null;
      this.optionalProps = null;
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

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId)) {
            BLocalBacnetDevice local = BBacnetNetwork.localDevice();
            local.unexport(this.oldId, this.oldName, this);
            this.clearTrendReferences(local);
            this.checkConfiguration();
            this.oldId = this.getObjectId();

            try {
               ((BComponent)this.getParent()).rename(this.getPropertyInParent(), this.getObjectId().toString(nameContext));
            } catch (DuplicateSlotException var5) {
            }

            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(objectName)) {
            BLocalBacnetDevice local = BBacnetNetwork.localDevice();
            local.unexport(this.oldId, this.oldName, this);
            this.clearTrendReferences(local);
            this.checkConfiguration();
            this.oldName = this.getObjectName();
            if (this.configOk()) {
               BBacnetNetwork.localDevice().incrementDatabaseRevision();
            }
         } else if (p.equals(pointOrd)) {
            this.ordStatus = BBacnetPointDescriptor.OrdStatus.CHANGED;
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
         } else if (p.equals(reliability)) {
            this.reliabilityChanged();
         }
      }
   }

   @Override
   public void statusChanged() {
      BStatus status = this.getStatusFlags();
      if (this.lastStatusBits != status.getBits()) {
         this.lastStatusBits = status.getBits();
         this.setBacnetStatusFlags(BBacnetBitString.make(new boolean[]{status.isAlarm(), status.isFault(), status.isOverridden(), status.isDisabled()}));
      }
   }

   protected void reliabilityChanged() {
      BControlPoint point = this.getPoint(false);
      if (null != point) {
         BReliabilityAlarmSourceExt[] c = (BReliabilityAlarmSourceExt[])point.getChildren(BReliabilityAlarmSourceExt.class);

         for (int i = 0; i < c.length; i++) {
            BReliabilityAlarmSourceExt fault = c[i];
            fault.reliabilityChanged((BBacnetReliability)this.getReliability());
         }
      }
   }

   public void clockChanged(BRelTime shift) throws Exception {
      SlotCursor<Property> sc = this.getProperties();

      while (sc.next(BBacnetCovSubscription.class)) {
         BBacnetCovSubscription covSub = (BBacnetCovSubscription)sc.get();
         covSub.setSubscriptionEndTime(covSub.getSubscriptionEndTime().add(shift));
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

   public void doMakeWritable(BValue v) {
   }

   public void doSendCovNotification(BBacnetCovSubscription covSub) {
      BControlPoint pt = this.getPoint();
      if (covSub.getTimeRemaining() < 0) {
         this.removeCovSubscription(covSub);
      } else {
         if (log.isLoggable(Level.FINE)) {
            log.fine("Sending Cov Notification on " + this + ": pt=" + pt + "\n  covSub=" + covSub);
         }

         Cov cov = new Cov(covSub, this, pt);
         BBacnetNetwork.bacnet().getCovWorker().sendCov(cov);
      }
   }

   public final void doCheckCov() {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetCovSubscription.class)) {
         BBacnetCovSubscription covSub = (BBacnetCovSubscription)c.get();
         boolean send = false;
         if (covSub.isCovProperty()) {
            send = this.checkCovProperty(covSub);
         } else {
            BStatusValue currentValue = this.getCurrentStatusValue();
            if (this.checkCov(currentValue, covSub.getLastValue())) {
               covSub.setLastValue(currentValue);
               send = true;
            }
         }

         if (send) {
            this.sendCovNotification(covSub);
         }
      }
   }

   public final boolean checkCovProperty(BBacnetCovSubscription covSub) {
      boolean send = false;

      try {
         PropertyValue pv = this.readProperty(
            covSub.getMonitoredPropertyReference().getPropertyId(), covSub.getMonitoredPropertyReference().getPropertyArrayIndex()
         );
         PropertyInfo pi = BBacnetNetwork.localDevice()
            .getPropertyInfo(this.getObjectId().getObjectType(), covSub.getMonitoredPropertyReference().getPropertyId());
         byte[] newAsnPropValue = pv.getPropertyValue();
         BStatus sf = this.getStatusFlags();
         int cs = sf.getBits() & 43;
         int lsb = covSub.getLastStatusBits();
         if (cs != lsb) {
            if (log.isLoggable(Level.FINE)) {
               log.fine("Status flags changed from: " + lsb + " to: " + cs);
            }

            send = true;
         }

         if (pi.getAsnType() != 4 && pi.getAsnType() != 5) {
            PropertyValue lv = covSub.getLastPropertyValue();
            if (lv == null) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("No previous notifications");
               }

               send = true;
            } else if (!Arrays.equals(lv.getPropertyValue(), newAsnPropValue)) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("asn.1 encoded byte[] changed, send it!");
               }

               send = true;
            }
         } else {
            BValue cv = AsnUtil.asnToValue(pi, newAsnPropValue);
            BINumeric newValue = (BINumeric)cv;
            BINumeric lastNumeric = (BINumeric)covSub.getLastPropValue();
            double covIncrement = this.getCovIncrement(covSub);
            double diff = 0.0;
            if (lastNumeric == null || (diff = Math.abs(newValue.getNumeric() - lastNumeric.getNumeric())) >= covIncrement) {
               if (log.isLoggable(Level.FINE)) {
                  log.fine("NumericCOV changed by more than diff: " + diff + " to: " + newValue.getNumeric());
               }

               covSub.setLastPropValue(cv);
               send = true;
            }
         }

         if (send) {
            covSub.setLastPropertyValue(pv);
            covSub.setLastStatusFlags(sf);
         }
      } catch (AsnException var16) {
         log.log(Level.SEVERE, "AsnException occurred in checkCovProperty", (Throwable)var16);
      }

      return send;
   }

   private double getCovIncrement(BBacnetCovSubscription covSub) {
      double covIncrement = covSub.getCovIncrement();
      if (Double.isNaN(covIncrement)) {
         if (covSub.getMonitoredPropertyReference().getPropertyId() == 85) {
            BNumber d = (BNumber)this.get("covIncrement");
            covIncrement = d != null ? d.getDouble() : 0.0;
         } else {
            covIncrement = 0.0;
         }
      }

      return covIncrement;
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
   public synchronized void checkConfiguration() {
      BLocalBacnetDevice local = BBacnetNetwork.localDevice();
      if (this.isFatalFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         this.configOk = false;
      } else {
         local.unsubscribe(this, this.point);
         this.findPoint();
         boolean cfgOk = true;
         if (this.point == null) {
            this.setFaultCause("Cannot find exported point");
            cfgOk = false;
         } else {
            synchronized (this.point) {
               if (!this.getObjectId().isValid()) {
                  this.setFaultCause("Invalid Object ID");
                  cfgOk = false;
               }

               cfgOk &= this.checkPointConfiguration();
               if (cfgOk) {
                  String err = local.export(this);
                  if (err != null) {
                     this.duplicate = true;
                     this.setFaultCause(err);
                     cfgOk = false;
                  } else {
                     this.duplicate = false;
                     this.updateTrendReferences(local, this.point);
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

            local.subscribe(this, this.point);
         }
      }
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
   public abstract BEnum getEventType();

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
      BControlPoint point = this.getPoint();
      return point != null ? point.getOutProperty() : null;
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
         log.log(Level.SEVERE, "AsnException occurred in getCurrentCovValue", (Throwable)var4);
         return null;
      }
   }

   abstract BStatusValue getCurrentStatusValue();

   boolean checkCov(BStatusValue currentValue, BStatusValue covValue) {
      return !currentValue.equals(covValue);
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
               int[] props = this.getRequiredProps();

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
               int[] props = this.getRequiredProps();

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
   public RangeData readRange(RangeReference rangeReference) throws RejectException {
      this.getPoint();
      int propertyId = rangeReference.getPropertyId();
      return !this.hasProperty(propertyId)
         ? new ReadRangeAck(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : new ReadRangeAck(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public final ErrorType writeProperty(PropertyValue val) throws BacnetException {
      this.getPoint();
      return this.writeProperty(val.getPropertyId(), val.getPropertyArrayIndex(), val.getPropertyValue(), val.getPriority());
   }

   @Override
   public ChangeListError addListElements(PropertyValue propertyValue) throws BacnetException {
      this.getPoint();
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeAddListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   @Override
   public ChangeListError removeListElements(PropertyValue propertyValue) throws BacnetException {
      this.getPoint();
      int propertyId = propertyValue.getPropertyId();
      return !this.hasProperty(propertyId)
         ? BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.property, BBacnetErrorCode.unknownProperty)
         : BacnetDescriptorUtil.makeRemoveListElementError(BBacnetErrorClass.services, BBacnetErrorCode.propertyIsNotA_List);
   }

   boolean isArray(int propertyId) {
      for (int i = 0; i < ARRAY_PROPS.length; i++) {
         if (propertyId == ARRAY_PROPS[i]) {
            return true;
         }
      }

      return false;
   }

   protected PropertyValue readProperty(int pId, int ndx) {
      if (this.point == null) {
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
            case 81:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnBoolean(this.getOosExt().getOutOfService()));
            case 103:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getReliability()));
            case 111:
               return new NReadPropertyResult(pId, ndx, AsnUtil.statusToAsnStatusFlags(this.getStatusFlags()));
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
         BAlarmSourceExt alarmExt = this.getAlarmExt();
         return alarmExt == null
            ? new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(0))
            : new NReadPropertyResult(36, -1, AsnUtil.toAsnEnumerated(BBacnetEventState.fromBAlarmState(alarmExt.getAlarmState())));
      }
   }

   protected PropertyValue readOptionalProperty(int pId, int ndx) {
      BAlarmSourceExt almExt = this.getAlarmExt();
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
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnEnumerated(this.getNotifyType()));
            case 113:
               return new NReadPropertyResult(pId, ndx, AsnUtil.toAsnUnsigned(almExt.getTimeDelay().getSeconds()));
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

   protected final PropertyValue readInterfaceValue() {
      BAbstractProxyExt abstractProxyExt = this.point.getProxyExt();
      if (abstractProxyExt instanceof BNullProxyExt) {
         return makeInterfaceValueResult(AsnUtil.toAsnNull());
      } else {
         BProxyExt proxyExt = (BProxyExt)abstractProxyExt;
         BStatusValue deviceValue = proxyExt.getReadValue();
         BStatus status = BStatus.make(proxyExt.getStatus().getBits() | deviceValue.getStatus().getBits());
         if (!status.isValid()) {
            if (logger.isLoggable(Level.FINE)) {
               logger.fine(this.getObjectId() + ": Returning null for interface-value property because proxy/device value status is " + status);
            }

            return makeInterfaceValueResult(AsnUtil.toAsnNull());
         } else {
            try {
               BStatusValue proxyValue = (BStatusValue)deviceValue.newCopy(true);
               proxyExt.getConversion().convertDeviceToProxy(proxyExt, deviceValue, proxyValue);
               byte[] interfaceValue = this.makeInterfaceValue(proxyValue);
               return makeInterfaceValueResult(interfaceValue);
            } catch (Exception var7) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.log(
                     Level.FINE, this.getObjectId() + ": Failed to convert device value to proxy value when reading interface-value property", (Throwable)var7
                  );
               }

               return makeInterfaceValueResult(AsnUtil.toAsnNull());
            }
         }
      }
   }

   private static PropertyValue makeInterfaceValueResult(byte[] value) {
      return new NReadPropertyResult(387, -1, value);
   }

   protected abstract byte[] makeInterfaceValue(BStatusValue var1);

   protected ErrorType writeProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      if (this.point == null) {
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
               case 77:
               case 79:
               case 103:
               case 111:
               case 371:
                  return new NErrorType(2, 40);
               case 81:
                  this.getOosExt().setBoolean(BOutOfServiceExt.outOfService, AsnUtil.fromOnlyAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
               default:
                  return this.writeOptionalProperty(pId, ndx, val, pri);
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

   protected ErrorType writeOptionalProperty(int pId, int ndx, byte[] val, int pri) throws BacnetException {
      try {
         BAlarmSourceExt almExt = this.getAlarmExt();
         if (almExt != null) {
            switch (pId) {
               case 0:
                  return new NErrorType(2, 40);
               case 17:
                  int ncinst = AsnUtil.fromAsnUnsignedInt(val);
                  if (ncinst > 4194302) {
                     return new NErrorType(2, 37);
                  }

                  BBacnetObjectIdentifier ncid = BBacnetObjectIdentifier.make(15, ncinst);
                  BBacnetNotificationClassDescriptor nc = (BBacnetNotificationClassDescriptor)BBacnetNetwork.localDevice().lookupBacnetObject(ncid);
                  if (nc == null) {
                     return new NErrorType(2, 37);
                  }

                  BAlarmClass ac = nc.getAlarmClass();
                  almExt.setString(BAlarmSourceExt.alarmClass, ac.getName(), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 35:
                  return new NErrorType(2, 40);
               case 72:
                  this.set(notifyType, BBacnetNotifyType.make(AsnUtil.fromAsnEnumerated(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 113:
                  almExt.set(BAlarmSourceExt.timeDelay, BRelTime.makeSeconds((int)AsnUtil.fromAsnUnsignedInteger(val)), BLocalBacnetDevice.getBacnetContext());
                  return null;
               case 130:
               case 351:
                  return new NErrorType(2, 40);
               case 352:
                  return writeEventMessageTextsConfig(ndx, val, almExt);
               case 353:
                  this.setBoolean(eventDetectionEnable, AsnUtil.fromAsnBoolean(val), BLocalBacnetDevice.getBacnetContext());
                  return null;
            }
         }
      } catch (IllegalArgumentException | OutOfRangeException var10) {
         log.warning("OutOfRangeException | IllegalArgumentException writing property " + pId + " in object " + this.getObjectId() + ": " + var10);
         return new NErrorType(2, 37);
      } catch (AsnException var11) {
         log.warning("AsnException writing property " + pId + " in object " + this.getObjectId() + ": " + var11);
         return new NErrorType(2, 9);
      } catch (PermissionException var12) {
         log.warning("PermissionException writing property " + pId + " in object " + this.getObjectId() + ": " + var12);
         return new NErrorType(2, 40);
      }

      return new NErrorType(2, 32);
   }

   public int[] getRequiredProps() {
      if (this.requiredProps == null) {
         Vector<BBacnetPropertyIdentifier> v = new Vector<>();
         v.add(BBacnetPropertyIdentifier.objectIdentifier);
         v.add(BBacnetPropertyIdentifier.objectName);
         v.add(BBacnetPropertyIdentifier.objectType);
         this.addRequiredProps(v);
         this.requiredProps = new int[v.size()];

         for (int i = 0; i < this.requiredProps.length; i++) {
            this.requiredProps[i] = ((BEnum)v.elementAt(i)).getOrdinal();
         }
      }

      return this.requiredProps;
   }

   public int[] getOptionalProps() {
      Vector<BBacnetPropertyIdentifier> v = new Vector<>();
      v.add(BBacnetPropertyIdentifier.reliability);
      v.add(BBacnetPropertyIdentifier.description);
      BAlarmSourceExt almExt = this.getAlarmExt();
      if (almExt != null) {
         v.add(BBacnetPropertyIdentifier.timeDelay);
         v.add(BBacnetPropertyIdentifier.notificationClass);
         v.add(BBacnetPropertyIdentifier.eventEnable);
         v.add(BBacnetPropertyIdentifier.ackedTransitions);
         v.add(BBacnetPropertyIdentifier.notifyType);
         v.add(BBacnetPropertyIdentifier.eventTimeStamps);
         v.add(BBacnetPropertyIdentifier.eventMessageTexts);
         v.add(BBacnetPropertyIdentifier.eventMessageTextsConfig);
         v.add(BBacnetPropertyIdentifier.eventDetectionEnable);
      }

      this.addOptionalProps(v);
      this.optionalProps = new int[v.size()];

      for (int i = 0; i < this.optionalProps.length; i++) {
         this.optionalProps[i] = ((BEnum)v.elementAt(i)).getOrdinal();
      }

      return this.optionalProps;
   }

   @Override
   public int[] getPropertyList() {
      return BacnetPropertyList.makePropertyList(this.getRequiredProps(), this.getOptionalProps());
   }

   protected boolean hasProperty(int propertyId) {
      for (int id : this.getRequiredProps()) {
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

   protected void addRequiredProps(Vector v) {
   }

   protected void addOptionalProps(Vector v) {
   }

   protected boolean isPointTypeLegal(BControlPoint pt) {
      return pt instanceof BControlPoint;
   }

   BStatus getStatusFlags() {
      this.getPoint();
      BAlarmSourceExt alarmExt = this.getAlarmExt();
      int status = 0;
      if (alarmExt != null) {
         BStatus pointStatus = alarmExt.getStatus();
         status = this.point == null ? 2 : pointStatus.getBits();
         if (pointStatus.isDown()) {
            status |= 2;
         }
      }

      if (this.getOosExt().getOutOfService()) {
         status |= 1;
      } else {
         status &= -2;
      }

      if (this.point.getStatus().isOverridden()) {
         BBacnetServerLayer serverLayer = BBacnetServerLayer.getServerLayer();
         BOverrideMode overrideMode = serverLayer != null ? serverLayer.getOverrideMode() : BOverrideMode.legacy;
         if (overrideMode.getOrdinal() == 1) {
            int activeLevel = this.point.getStatus().geti("activeLevel", 17);
            if (activeLevel == 1) {
               status |= 32;
            }
         } else {
            status |= 32;
         }
      }

      return BStatus.make(status);
   }

   protected boolean checkPointConfiguration() {
      return true;
   }

   @Override
   void checkValid() {
      if (this.configOk()) {
         this.validate();
      }
   }

   protected void validate() {
      this.setStatus(BStatus.makeFault(this.getStatus(), false));
   }

   synchronized boolean configOk() {
      return this.configOk;
   }

   public String toString(Context c) {
      return this.getObjectName() + " [" + this.getObjectId() + "]";
   }

   @Override
   public final BControlPoint getPoint() {
      return this.point == null ? this.findPoint() : this.point;
   }

   public final BControlPoint getPoint(boolean force) {
      return this.point == null ? this.findPoint(force) : this.point;
   }

   protected BAlarmSourceExt getAlarmExt() {
      BControlPoint point = this.getPoint();
      if (point == null) {
         return null;
      } else {
         SlotCursor<Property> c = point.getProperties();

         while (c.next(BAlarmSourceExt.class)) {
            BAlarmSourceExt ext = (BAlarmSourceExt)c.get();
            if (this.isValidAlarmExt(ext)) {
               BBacnetObjectIdentifier eventEnrollmentId = BBacnetNetwork.localDevice().lookupBacnetObjectId(ext.getHandleOrd());
               if (eventEnrollmentId == null) {
                  return ext;
               }
            }
         }

         return null;
      }
   }

   protected BFaultAlgorithm getFaultAlgorithm() {
      BAlarmSourceExt alarmExt = this.getAlarmExt();
      return alarmExt != null ? alarmExt.getFaultAlgorithm() : null;
   }

   public BOutOfServiceExt getOosExt() {
      BControlPoint point = this.getPoint(false);
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
         outOfServiceExt.setCommandable(this.isCommandable());
         return outOfServiceExt;
      }
   }

   protected boolean isCommandable() {
      return false;
   }

   public String getBacnetWritable() {
      return lexNotWritable;
   }

   private BControlPoint findPoint() {
      return this.findPoint(true);
   }

   private BControlPoint findPoint(boolean force) {
      try {
         if (force || this.ordStatus == BBacnetPointDescriptor.OrdStatus.CHANGED) {
            if (!pointOrd.isEquivalentToDefaultValue(this.getPointOrd())) {
               BObject o = this.getPointOrd().get(this);
               if (o instanceof BControlPoint) {
                  this.point = (BControlPoint)o;
                  this.ordStatus = BBacnetPointDescriptor.OrdStatus.OK;
               } else {
                  this.point = null;
               }
            }

            if (!this.isPointTypeLegal(this.point)) {
               this.point = null;
            }
         }
      } catch (Exception var3) {
         log.warning("Unable to resolve point ord for " + this + ": " + this.getPointOrd() + ": " + var3);
         this.point = null;
         this.ordStatus = BBacnetPointDescriptor.OrdStatus.INVALID;
      }

      if (this.point == null && this.isRunning()) {
         this.setReliability(BBacnetReliability.unreliableOther);
         this.setFaultCause("Cannot find exported point");
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
      }

      return this.point;
   }

   @Deprecated
   @Override
   protected void updateAlarmInhibit() {
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

   private void updateTrendReferences(BLocalBacnetDevice local, BControlPoint point) {
      for (BPointExtension pointExt : point.getExtensions()) {
         if (pointExt instanceof BHistoryExt) {
            BIBacnetExportObject trendDescriptor = findDescriptor(local, pointExt);
            if (trendDescriptor instanceof BBacnetTrendLogDescriptor) {
               BBacnetDeviceObjectPropertyReference reference = new BBacnetDeviceObjectPropertyReference(this.getObjectId(), 85);
               ((BBacnetTrendLogDescriptor)trendDescriptor).setLogDeviceObjectPropertyReference(reference);
            }
         }
      }
   }

   private void clearTrendReferences(BLocalBacnetDevice local) {
      BControlPoint point = this.getPoint(false);
      if (point != null) {
         for (BPointExtension pointExt : point.getExtensions()) {
            if (pointExt instanceof BHistoryExt) {
               BIBacnetExportObject trendDescriptor = findDescriptor(local, pointExt);
               if (trendDescriptor instanceof BBacnetTrendLogDescriptor) {
                  ((BBacnetTrendLogDescriptor)trendDescriptor).setLogDeviceObjectPropertyReference(new BBacnetDeviceObjectPropertyReference());
               }
            }
         }
      }
   }

   private static BIBacnetExportObject findDescriptor(BLocalBacnetDevice local, BPointExtension pointExt) {
      BBacnetObjectIdentifier trendId = local.lookupBacnetObjectId(pointExt.getHandleOrd());
      return trendId == null ? null : local.lookupBacnetObject(trendId);
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetPointDescriptor", 2);
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

   private static enum OrdStatus {
      OK,
      INVALID,
      CHANGED;
   }
}
