package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.history.BBacnetTrendLogAlarmSourceExt;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.AsyncEventNotificationRequest;
import com.tridium.bacnet.util.BacnetAlarmRecipientUtil;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.AlarmDbConnection;
import javax.baja.alarm.BAckState;
import javax.baja.alarm.BAlarmClass;
import javax.baja.alarm.BAlarmRecipient;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BSourceState;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.export.BBacnetEventSource;
import javax.baja.bacnet.export.BIBacnetExportObject;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.bacnet.virtual.BBacnetVirtualProperty;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.category.BCategoryMask;
import javax.baja.naming.BOrd;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BTime;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BDaysOfWeekBits;
import javax.baja.util.BTimeRange;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "timeRange",
      type = "BTimeRange",
      defaultValue = "new BTimeRange(BTime.make(0, 0, 0, 0), BTime.make(23, 59, 59, 999))",
      override = true
   ), @NiagaraProperty(
      name = "transitions",
      type = "BAlarmTransitionBits",
      defaultValue = "BAlarmTransitionBits.make(BAlarmTransitionBits.TO_OFFNORMAL | BAlarmTransitionBits.TO_FAULT | BAlarmTransitionBits.TO_NORMAL)",
      override = true
   ), @NiagaraProperty(
      name = "routeAcks",
      type = "boolean",
      defaultValue = "true",
      flags = 1,
      override = true
   ), @NiagaraProperty(
      name = "recipient",
      type = "BBacnetRecipient",
      defaultValue = "new BBacnetRecipient()",
      flags = 8
   ), @NiagaraProperty(
      name = "processIdentifier",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.make(0)"
   ), @NiagaraProperty(
      name = "issueConfirmedNotifications",
      type = "boolean",
      defaultValue = "false"
   )})
public class BBacnetDestination extends BAlarmRecipient implements BIBacnetDataType, BacnetAlarmConst {
   public static final Property timeRange = newProperty(0, new BTimeRange(BTime.make(0, 0, 0, 0), BTime.make(23, 59, 59, 999)), null);
   public static final Property transitions = newProperty(0, BAlarmTransitionBits.make(7), null);
   public static final Property routeAcks = newProperty(1, true, null);
   public static final Property recipient = newProperty(8, new BBacnetRecipient(), null);
   public static final Property processIdentifier = newProperty(0, BBacnetUnsigned.make(0L), null);
   public static final Property issueConfirmedNotifications = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BBacnetDestination.class);
   private boolean client = false;
   private BEnumRange eventStateRange = BBacnetEventState.DEFAULT.getRange();
   private final Logger log = Logger.getLogger("bacnet.server");
   public static final long LOCAL_PROCESS_ID = 0L;
   public static final int MAX_ENCODED_SIZE = 35;

   public BBacnetRecipient getRecipient() {
      return (BBacnetRecipient)this.get(recipient);
   }

   public void setRecipient(BBacnetRecipient v) {
      this.set(recipient, v, null);
   }

   public BBacnetUnsigned getProcessIdentifier() {
      return (BBacnetUnsigned)this.get(processIdentifier);
   }

   public void setProcessIdentifier(BBacnetUnsigned v) {
      this.set(processIdentifier, v, null);
   }

   public boolean getIssueConfirmedNotifications() {
      return this.getBoolean(issueConfirmedNotifications);
   }

   public void setIssueConfirmedNotifications(boolean v) {
      this.setBoolean(issueConfirmedNotifications, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final void handleAlarm(BAlarmRecord alarmRecord) {
      try {
         boolean traceOn = this.log.isLoggable(Level.FINE);
         if (traceOn) {
            this.log
               .fine(
                  "handleAlarm on "
                     + SlotPath.unescape(this.getName())
                     + ":"
                     + alarmRecord
                     + "\n alarmData="
                     + alarmRecord.getAlarmData()
                     + "\n uuid="
                     + alarmRecord.getUuid()
               );
         }

         BObject alarmSource = alarmRecord.getSource().get(0).get(this);
         BComponent eventObject = alarmSource.asComplex().getParent().asComponent();
         BBacnetObjectIdentifier eventObjectId = BacnetAlarmRecipientUtil.getEventObjectId(alarmSource);
         if (eventObjectId == null) {
            this.log
               .warning(
                  "Alarm " + alarmRecord.getUuid() + " not sent to " + this.getRecipient() + ": object " + eventObject.getName() + " not exposed to BACnet!"
               );
            return;
         }

         BString toSt = (BString)alarmRecord.getAlarmFacet("toState");
         BEnum toState = toSt != null ? BBacnetEventState.make(toSt.getString()) : BBacnetEventState.make(alarmRecord.getSourceState());
         BString fromSt = (BString)alarmRecord.getAlarmFacet("fromState");
         BEnum fromState = fromSt != null ? BBacnetEventState.make(fromSt.getString()) : BBacnetEventState.make(alarmRecord.getSourceState());
         boolean ackAlarmAndNormal = false;
         BInteger stateAcked = (BInteger)alarmRecord.getAlarmFacet("stateAcked");
         BAlarmService alarmService = (BAlarmService)Sys.getService(BAlarmService.TYPE);
         BAlarmClass alarmClass = alarmService.lookupAlarmClass(alarmRecord.getAlarmClass());
         BSourceState newToState = alarmRecord.getSourceState();
         String destinationPath = this.getSlotPathOrd().toString(null).substring(alarmService.getSlotPathOrd().toString(null).length());
         BInteger oldToState = (BInteger)alarmRecord.getAlarmFacet(SlotPath.escape(destinationPath));
         boolean stateChanged = oldToState != null ? oldToState.getInt() != newToState.getOrdinal() : true;
         alarmRecord.addAlarmFacet(SlotPath.escape(destinationPath), BInteger.make(newToState.getOrdinal()));
         if (traceOn) {
            this.log.fine("stateChanged=" + stateChanged + " ar:" + this.ackDump(alarmRecord));
         }

         if (stateChanged) {
            if (traceOn) {
               this.log.fine("stateAcked=" + stateAcked + " ar:" + this.ackDump(alarmRecord));
            }

            if (stateAcked == null) {
               if (traceOn) {
                  this.log.fine(" new alarm - add acksReq to BAC_ACK_REQUIRED");
               }

               int acksReq = 0;
               if (alarmSource instanceof BAlarmSourceExt) {
                  BInteger acksReqFacet = (BInteger)alarmRecord.getAlarmFacet("bacnetAcksRequired");
                  if (acksReqFacet != null) {
                     acksReq = acksReqFacet.getInt();
                  }
               }

               BAlarmTransitionBits acAckReq = alarmClass.getAckRequired();
               if (BBacnetEventState.isOffnormal(toState)) {
                  if (acAckReq.isToOffnormal()) {
                     alarmRecord.removeAlarmFacet("offnormalAcked");
                     acksReq |= 4;
                  }
               } else if (BBacnetEventState.isFault(toState)) {
                  if (acAckReq.isToFault()) {
                     acksReq |= 2;
                  }
               } else if (BBacnetEventState.isNormal(toState)) {
                  boolean isNormalAlarmEnabledForTrendLogExt = alarmSource instanceof BBacnetTrendLogAlarmSourceExt
                     && !((BBacnetTrendLogAlarmSourceExt)alarmSource).getAlarmEnable().isToNormal();
                  boolean isNormalAlarmEnabledForAlarmSrcLogExt = alarmSource instanceof BAlarmSourceExt
                     && !((BAlarmSourceExt)alarmSource).getAlarmEnable().isToNormal();
                  if (isNormalAlarmEnabledForTrendLogExt || isNormalAlarmEnabledForAlarmSrcLogExt) {
                     return;
                  }

                  if (acAckReq.isToNormal()) {
                     acksReq |= 1;
                  }
               }

               alarmRecord.addAlarmFacet("bacnetAcksRequired", BInteger.make(acksReq));
            }

            this.addToNotifyList(alarmRecord);
            AlarmDbConnection conn = alarmService.getAlarmDb().getDbConnection(null);
            Throwable var60 = null;

            try {
               conn.update(alarmRecord);
            } catch (Throwable var48) {
               var60 = var48;
               throw var48;
            } finally {
               if (conn != null) {
                  if (var60 != null) {
                     try {
                        conn.close();
                     } catch (Throwable var46) {
                        var60.addSuppressed(var46);
                     }
                  } else {
                     conn.close();
                  }
               }
            }
         } else {
            if ((!alarmRecord.isAcknowledged() || !alarmClass.getAckRequired().includes(alarmRecord.getSourceState()))
               && alarmRecord.getAckState() != BAckState.ackPending) {
               return;
            }

            BBacnetObjectIdentifier deviceId = BBacnetNetwork.localDevice().getObjectId();
            if (BBacnetEventState.isNormal(toState)) {
               BBoolean offnormalAcked = (BBoolean)alarmRecord.getAlarmFacet("offnormalAcked");
               if (offnormalAcked != null) {
                  ackAlarmAndNormal = !offnormalAcked.getBoolean();
               } else {
                  ackAlarmAndNormal = toSt == null || !toSt.equals(fromSt);
               }
            } else if (BBacnetEventState.isOffnormal(toState)) {
               alarmRecord.addAlarmFacet("offnormalAcked", BBoolean.TRUE);
               AlarmDbConnection conn = alarmService.getAlarmDb().getDbConnection(null);
               Throwable request = null;

               try {
                  conn.update(alarmRecord);
               } catch (Throwable var47) {
                  request = var47;
                  throw var47;
               } finally {
                  if (conn != null) {
                     if (request != null) {
                        try {
                           conn.close();
                        } catch (Throwable var45) {
                           request.addSuppressed(var45);
                        }
                     } else {
                        conn.close();
                     }
                  }
               }
            }

            if (traceOn) {
               this.log.fine("acknowledged - stateAcked=" + stateAcked + " ar:" + this.ackDump(alarmRecord) + ", toState(alarm)=" + toState);
            }

            if (stateAcked != null) {
               toState = BBacnetEventState.make(stateAcked.getInt());
            }

            BAlarmRecord rec = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
               .getServer()
               .getEventHandler()
               .getRecordFromEventBuffer(toState.getOrdinal(), deviceId, eventObjectId, 0L, alarmRecord.getUuid(), false);
            if (traceOn) {
               this.log.fine("dest " + this.getName() + ": rec=" + (rec != null ? rec.getUuid().toString() : "null"));
            }

            if (rec == null) {
               if (traceOn) {
                  this.log
                     .fine(
                        "Skipping event notification: No matching record in event buffer for "
                           + deviceId
                           + " "
                           + eventObjectId
                           + " "
                           + 0L
                           + " with UUID "
                           + alarmRecord.getUuid()
                     );
               }

               return;
            }

            this.removeFromNotifyList(alarmRecord);
         }

         BBacnetEventSource evtSrc = null;

         try {
            evtSrc = (BBacnetEventSource)BBacnetNetwork.localDevice().lookupBacnetObject(eventObjectId);
         } catch (ClassCastException var49) {
            if (traceOn) {
               this.log.fine("BBacnetObjectIdentifier is not an BacnetEventSource");
            }
         }

         if (evtSrc != null && !evtSrc.getEventDetectionEnable()) {
            this.log.fine("Do not send event notification for an when event source, eventdetectionenable is false");
            return;
         }

         BBoolean stale = (BBoolean)alarmRecord.getAlarmFacet("staleAck");
         if (stale != null && stale.getBoolean()) {
            this.log.fine("Skipping event notification for stale ack (BAC_STALE_ACK)");
            return;
         }

         if (newToState.equals(BSourceState.normal)
            && alarmSource instanceof BAlarmSourceExt
            && !((BAlarmSourceExt)alarmSource).isLastNormalRecord(alarmRecord)) {
            this.log.fine("Skipping event notification for to-normal transition of record that is not the alarm source's last normal record");
            return;
         }

         if (this.checkForEventTypeBasedSpecialHandling(fromState, toState, eventObjectId.getObjectType())) {
            this.faultToOffNormalTransition(alarmRecord, eventObjectId, eventObject, ackAlarmAndNormal, stateChanged);
         } else {
            AsyncEventNotificationRequest request = new AsyncEventNotificationRequest(
               alarmRecord,
               eventObjectId,
               eventObject,
               this.getProcessIdentifier().getUnsigned(),
               this.getRecipient(),
               this.getIssueConfirmedNotifications(),
               ackAlarmAndNormal
            );
            request.setAlarm(stateChanged);
            this.sendNotification(request);
         }
      } catch (Exception var52) {
         this.log.log(Level.SEVERE, "Exception handling alarm in " + this.getName() + ":" + var52, (Throwable)var52);
      }
   }

   private boolean checkForEventTypeBasedSpecialHandling(BEnum fromState, BEnum toState, int objectType) {
      return BBacnetEventState.isFault(fromState) && BBacnetEventState.isOffnormal(toState) && (objectType == 19 || objectType == 13 || objectType == 14);
   }

   private void faultToOffNormalTransition(
      BAlarmRecord alarmRecord, BBacnetObjectIdentifier eventObjectId, BComponent eventObject, boolean ackAlarmAndNormal, boolean stateChanged
   ) {
      BBacnetObjectIdentifier oid = BBacnetNetwork.localDevice().lookupBacnetObjectId(eventObject.getHandleOrd());
      BIBacnetExportObject descriptor = BBacnetNetwork.localDevice().lookupBacnetObject(oid);
      int[] eventPriorities = ((BBacnetEventSource)descriptor).getEventPriorities();
      int eventPriority = eventPriorities[2];
      alarmRecord.setPriority(eventPriority);
      this.notification(alarmRecord, eventObjectId, eventObject, ackAlarmAndNormal, stateChanged, BBacnetEventState.fault, BBacnetEventState.normal);
      eventPriority = eventPriorities[0];
      alarmRecord.setPriority(eventPriority);
      this.notification(alarmRecord, eventObjectId, eventObject, ackAlarmAndNormal, stateChanged, BBacnetEventState.normal, BBacnetEventState.offnormal);
   }

   private void notification(
      BAlarmRecord alarmRecord,
      BBacnetObjectIdentifier eventObjectId,
      BComponent eventObject,
      boolean ackAlarmAndNormal,
      boolean stateChanged,
      BEnum from,
      BEnum to
   ) {
      BOrd source = alarmRecord.getSource().get(0);
      String alarmClass = alarmRecord.getAlarmClass();
      BFacets alarmData = (BFacets)alarmRecord.getAlarmData().newCopy();
      alarmData = BFacets.make(alarmData, "fromState", BString.make(from.getTag()));
      alarmData = BFacets.make(alarmData, "toState", BString.make(to.getTag()));
      BAlarmRecord alarmRecord_transition = new BAlarmRecord(source, alarmClass, alarmData);
      alarmRecord_transition.setPriority(alarmRecord.getPriority());
      AsyncEventNotificationRequest request = new AsyncEventNotificationRequest(
         alarmRecord_transition,
         eventObjectId,
         eventObject,
         this.getProcessIdentifier().getUnsigned(),
         this.getRecipient(),
         this.getIssueConfirmedNotifications(),
         ackAlarmAndNormal
      );
      request.setAlarm(stateChanged);
      this.sendNotification(request);
   }

   protected void sendNotification(AsyncEventNotificationRequest request) {
      BBacnetNetwork.bacnet().postAsync(request);
   }

   public final boolean recipientEquals(BBacnetRecipient recip) {
      return this.getRecipient().equivalent(recip);
   }

   public final boolean destinationEquals(BBacnetDestination dest) {
      return this.destinationEquals(dest, false);
   }

   public final boolean destinationEquals(BBacnetDestination dest, boolean compareMillis) {
      if (dest == null) {
         return false;
      } else {
         int mask = BAlarmTransitionBits.ALL.getBits() & -9;
         int mybits = this.getTransitions().getBits() & mask;
         int dbits = dest.getTransitions().getBits() & mask;
         return this.timeRangesEquivalent(this.getTimeRange(), dest.getTimeRange(), compareMillis)
            && this.getDaysOfWeek().equals(dest.getDaysOfWeek())
            && mybits == dbits
            && this.getTransitions().getBits() == dest.getTransitions().getBits()
            && this.getRecipient().equivalent(dest.getRecipient())
            && this.getProcessIdentifier().equals(dest.getProcessIdentifier())
            && this.getIssueConfirmedNotifications() == dest.getIssueConfirmedNotifications();
      }
   }

   public final boolean timeRangesEquivalent(BTimeRange tr1, BTimeRange tr2, boolean compareMillis) {
      return compareMillis
         ? tr1.equivalent(tr2)
         : tr1.getStartTime().getHour() == tr2.getStartTime().getHour()
            && tr1.getStartTime().getMinute() == tr2.getStartTime().getMinute()
            && tr1.getStartTime().getSecond() == tr2.getStartTime().getSecond()
            && tr1.getEndTime().getHour() == tr2.getEndTime().getHour()
            && tr1.getEndTime().getMinute() == tr2.getEndTime().getMinute()
            && tr1.getEndTime().getSecond() == tr2.getEndTime().getSecond();
   }

   public final void started() {
      if (this.getParent() instanceof BBacnetListOf) {
         this.client = true;
      }

      if (Sys.atSteadyState() && this.isRunning()) {
         this.resolveRecipient();
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(recipient)) {
            this.resolveRecipient();
         }

         BComplex parent = this.getParent();
         if (parent != null) {
            parent.asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public final void atSteadyState() throws Exception {
      super.atSteadyState();
      this.resolveRecipient();
   }

   public final void subscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public final void unsubscribed() {
      BBacnetVirtualProperty vp = BacnetVirtualUtil.getVirtualProperty(this);
      if (vp != null) {
         vp.childSubscribed(this);
      }
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BAlarmService || parent instanceof BBacnetListOf;
   }

   public final BCategoryMask getAppliedCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getAppliedCategoryMask() : super.getAppliedCategoryMask();
   }

   public final BCategoryMask getCategoryMask() {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getCategoryMask() : super.getCategoryMask();
   }

   public final BPermissions getPermissions(Context cx) {
      return BacnetVirtualUtil.isVirtual(this) ? this.getParent().asComponent().getPermissions(cx) : super.getPermissions(cx);
   }

   public boolean accept(BAlarmRecord rec) {
      BAbsTime ts = BAbsTime.now();
      if (!this.getDaysOfWeek().includes(ts.getWeekday())) {
         return false;
      } else {
         return !this.getTimeRange().includes(ts) ? false : this.getTransitions().includes(rec.getSourceState());
      }
   }

   private void resolveRecipient() {
      if (!this.client) {
         if (this.getRecipient().isDevice()) {
            BBacnetObjectIdentifier deviceId = this.getRecipient().getDevice();
            if (deviceId.isValid() && deviceId.getObjectType() == 8 && BBacnetNetwork.bacnet().doLookupDeviceById(deviceId) == null) {
               try {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm())
                     .getClient()
                     .whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, deviceId.getInstanceNumber(), deviceId.getInstanceNumber());
               } catch (BacnetException var4) {
                  this.log
                     .log(Level.WARNING, "Unable to determine address for Bacnet Destination " + this.getName() + ": " + this.getRecipient(), (Throwable)var4);
               }
            }
         } else {
            BBacnetAddress address = this.getRecipient().getAddress();
            if (BBacnetNetwork.bacnet().doLookupDeviceByAddress(address) == null) {
               try {
                  ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient().whoIs(address);
               } catch (BacnetException var3) {
                  this.log
                     .log(Level.SEVERE, "Unable to resolve address for Bacnet Destination " + this.getName() + ": " + this.getRecipient(), (Throwable)var3);
               }
            }
         }
      }
   }

   private void addToNotifyList(BAlarmRecord r) {
      BString notify = (BString)r.getAlarmFacet("bacNotify");
      if (notify == null) {
         notify = BString.make(this.getHandle().toString());
      } else {
         notify = BString.make(notify.getString() + ";" + this.getHandle().toString());
      }

      r.addAlarmFacet("bacNotify", notify);
   }

   private boolean removeFromNotifyList(BAlarmRecord r) {
      BString notify = (BString)r.getAlarmFacet("bacNotify");
      String h = this.getHandle().toString();
      if (notify != null) {
         StringTokenizer st = new StringTokenizer(notify.getString(), ";");
         StringBuilder sb = new StringBuilder();

         while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (!h.equals(tok)) {
               sb.append(tok).append(";");
            }
         }

         if (sb.length() == 0) {
            r.removeAlarmFacet("bacNotify");
            return false;
         } else {
            r.addAlarmFacet("bacNotify", BString.make(sb.toString()));
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public final void writeAsn(AsnOutput out) {
      out.writeBitString(BacnetBitStringUtil.getBacnetDaysOfWeek(this.getDaysOfWeek()).getBits());
      out.writeTime(this.getTimeRange().getStartTime());
      out.writeTime(this.getTimeRange().getEndTime());
      this.getRecipient().writeAsn(out);
      out.writeUnsigned(this.getProcessIdentifier());
      out.writeBoolean(this.getIssueConfirmedNotifications());
      out.writeBitString(BacnetBitStringUtil.getBacnetEventTransitionBits(this.getTransitions()).getBits());
   }

   @Override
   public final void readAsn(AsnInput in) throws AsnException {
      BDaysOfWeekBits daysOfWeek;
      try {
         daysOfWeek = BacnetBitStringUtil.getBDaysOfWeekBits(in.readBitString());
      } catch (IllegalArgumentException var12) {
         throw new AsnException(BBacnetErrorCode.invalidDataType.getTag());
      }

      BBacnetTime startTime = in.readTime();
      BBacnetTime endTime = in.readTime();
      if (startTime.getHour() == -1 || startTime.getMinute() == -1 || startTime.getSecond() == -1 || startTime.getHundredth() == -1) {
         throw new AsnException(BBacnetErrorCode.valueOutOfRange.getTag());
      } else if (endTime.getHour() != -1 && endTime.getMinute() != -1 && endTime.getSecond() != -1 && endTime.getHundredth() != -1) {
         BTime bajaStartTime = BBacnetTime.getBTime(startTime, true);
         BTime bajaEndTime = BBacnetTime.getBTime(endTime, false);
         BBacnetRecipient recipient = new BBacnetRecipient();
         recipient.readAsn(in);
         long processIdentifier = in.readUnsignedInteger();
         boolean issueConfirmedNotifications = in.readBoolean();
         BAlarmTransitionBits transitions = BacnetBitStringUtil.getBAlarmTransitionBits(in.readBitString());
         this.set(BAlarmRecipient.daysOfWeek, daysOfWeek, noWrite);
         this.getTimeRange().set(BTimeRange.startTime, bajaStartTime, noWrite);
         this.getTimeRange().set(BTimeRange.endTime, bajaEndTime, noWrite);
         this.set(BBacnetDestination.recipient, recipient, noWrite);
         this.set(BBacnetDestination.processIdentifier, BBacnetUnsigned.make(processIdentifier), noWrite);
         this.setBoolean(BBacnetDestination.issueConfirmedNotifications, issueConfirmedNotifications, noWrite);
         this.set(BBacnetDestination.transitions, transitions, noWrite);
      } else {
         throw new AsnException(BBacnetErrorCode.valueOutOfRange.getTag());
      }
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getRecipient().toString(cx))
         .append(" pId=")
         .append(this.getProcessIdentifier())
         .append(" conf=")
         .append(this.getIssueConfirmedNotifications())
         .append(" times=")
         .append(this.getTimeRange().toString(cx))
         .append(" days=")
         .append(this.getDaysOfWeek().toString(cx))
         .append(" trans=")
         .append(this.getTransitions().toString(cx));
      return sb.toString();
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetDestination", 2);
      out.prop("client", this.client);
      out.prop("eventStateRange", this.eventStateRange);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }

   private String ackDump(BAlarmRecord alarmRecord) {
      return alarmRecord.getSourceState() + "/" + alarmRecord.getAckState() + "/" + alarmRecord.getAckRequired();
   }
}
