package javax.baja.bacnet.point;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.history.BIBacnetTrendLogExt;
import com.tridium.bacnet.job.BacnetDiscoveryUtil;
import com.tridium.bacnet.stack.BBacnetPoll;
import com.tridium.bacnet.stack.BBacnetStack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BIRemoteAlarmSource;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetDeviceObjectPropertyReference;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetEventState;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.enums.BBacnetReliability;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.bacnet.util.PropertyInfo;
import javax.baja.control.BControlPoint;
import javax.baja.control.BIWritablePoint;
import javax.baja.driver.point.BProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceFacets",
      type = "BFacets",
      defaultValue = "BFacets.NULL",
      flags = 1,
      override = true
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   ), @NiagaraProperty(
      name = "propertyId",
      type = "BDynamicEnum",
      defaultValue = "BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue)"
   ), @NiagaraProperty(
      name = "propertyArrayIndex",
      type = "int",
      defaultValue = "NOT_USED"
   ), @NiagaraProperty(
      name = "dataType",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "readStatus",
      type = "String",
      defaultValue = "BBacnetProxyExt.UNSUBSCRIBED",
      flags = 3
   ), @NiagaraProperty(
      name = "writeStatus",
      type = "String",
      defaultValue = "BBacnetProxyExt.READONLY",
      flags = 3
   )})
@NiagaraActions({@NiagaraAction(
      name = "forceRead"
   ), @NiagaraAction(
      name = "forceWrite"
   ), @NiagaraAction(
      name = "subscribeCov",
      flags = 4
   ), @NiagaraAction(
      name = "subscribeCovProperty",
      flags = 4
   ), @NiagaraAction(
      name = "ackAlarm",
      parameterType = "BAlarmRecord",
      defaultValue = "new BAlarmRecord()",
      returnType = "BBoolean",
      flags = 4
   )})
public abstract class BBacnetProxyExt extends BProxyExt implements BacnetConst, BIBacnetPollable, BIRemoteAlarmSource {
   static Lexicon lex = Lexicon.make("bacnet");
   static String COV = lex.getText("point.cov");
   static String COVP = lex.getText("point.covp");
   static String POLLED = lex.getText("point.polled");
   static String COV_PENDING = lex.getText("point.covPending");
   static String COVP_PENDING = lex.getText("point.covpPending");
   static String COV_FAILED = lex.getText("point.covFailed");
   static String UNSUBSCRIBED = lex.getText("point.unsubscribed");
   static String WRITABLE = lex.getText("point.writable");
   static String READONLY = lex.getText("point.readonly");
   static String OK = lex.getText("point.ok");
   public static final Property deviceFacets = newProperty(1, BFacets.NULL, null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Property propertyId = newProperty(0, BDynamicEnum.make(BBacnetPropertyIdentifier.presentValue), null);
   public static final Property propertyArrayIndex = newProperty(0, -1, null);
   public static final Property dataType = newProperty(1, "", null);
   public static final Property readStatus = newProperty(3, UNSUBSCRIBED, null);
   public static final Property writeStatus = newProperty(3, READONLY, null);
   public static final Action forceRead = newAction(0, null);
   public static final Action forceWrite = newAction(0, null);
   public static final Action subscribeCov = newAction(4, null);
   public static final Action subscribeCovProperty = newAction(4, null);
   public static final Action ackAlarm = newAction(4, new BAlarmRecord(), null);
   public static final Type TYPE = Sys.loadType(BBacnetProxyExt.class);
   private BBacnetDevice bacnetDevice = null;
   private boolean debug = false;
   public static final int SUB_STATE_UNSUB = 0;
   public static final int SUB_STATE_POLLED = 1;
   public static final int SUB_STATE_COV = 2;
   public static final int SUB_STATE_FIRST_COV_PENDING = 3;
   public static final int SUB_STATE_POLLED_PENDING = 4;
   public static final int SUB_STATE_COV_PENDING = 5;
   public static final int SUB_STATE_FIRST_COVP_PENDING = 6;
   public static final int SUB_STATE_COVP = 7;
   public static final int SUB_STATE_COVP_PENDING = 8;
   public static final int SUB_STATE_COVP_FAILED = 9;
   public static final byte[] NO_VALUE = new byte[0];
   public static final String READ_STATUS_FLAGS = "statusFlags";
   public static final String READ_EVENT_STATE = "eventState";
   public static final String READ_PRIORITY_ARRAY = "priorityArray";
   public static final String READ_RELIABILITY = "reliability";
   public static final String STATUS_FLAGS_STATUS_FACET = "statusFlags";
   public static final String EVENT_STATE_STATUS_FACET = "state";
   public static final String PRIORITY_ARRAY_STATUS_FACET = "bac";
   public static final String RELIABILITY_STATUS_FACET = "reliability";
   public static final String ALARM_SOURCE_NAME = "alarmSourceName";
   public static final String PRIORITIZED_PRESENT_VALUE = "priPV";
   static final ErrorType ERROR_DEVICE_OTHER = new NErrorType(0, 0);
   public static final Context covContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:covContext";
      }
   };
   private Ticket ticket = null;
   private ErrorType lastReadError;
   protected int dataSize = 0;
   protected int asnType = 0;
   private BBoolean priPV = BBoolean.FALSE;
   private BBoolean useStatusFlags = null;
   private int lastWriteLevel = 0;
   private BBacnetPoll pollService;
   private int subState = 0;
   private PollListEntry[] ples = null;
   static Logger log = Logger.getLogger("bacnet.point");
   private static final int DAY_IN_MINUTES = 1440;
   private static final long RESUBSCRIPTION_FACTOR = 30000L;
   private static final long MINIMUM_SUBSCRIPTION_LIFETIME = 5L;
   private static final int INTERVAL_ON_POST_FAILURE = 10;
   private static final BRelTime RELTIME_ON_POST_FAILURE = BRelTime.makeSeconds(10);
   private static final BRelTime ONCE_A_DAY_RELTIME = BRelTime.make(43200000L);
   private static final BRelTime MINIMUM_RELTIME = BRelTime.make(150000L);

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public BDynamicEnum getPropertyId() {
      return (BDynamicEnum)this.get(propertyId);
   }

   public void setPropertyId(BDynamicEnum v) {
      this.set(propertyId, v, null);
   }

   public int getPropertyArrayIndex() {
      return this.getInt(propertyArrayIndex);
   }

   public void setPropertyArrayIndex(int v) {
      this.setInt(propertyArrayIndex, v, null);
   }

   public String getDataType() {
      return this.getString(dataType);
   }

   public void setDataType(String v) {
      this.setString(dataType, v, null);
   }

   public String getReadStatus() {
      return this.getString(readStatus);
   }

   public void setReadStatus(String v) {
      this.setString(readStatus, v, null);
   }

   public String getWriteStatus() {
      return this.getString(writeStatus);
   }

   public void setWriteStatus(String v) {
      this.setString(writeStatus, v, null);
   }

   public void forceRead() {
      this.invoke(forceRead, null, null);
   }

   public void forceWrite() {
      this.invoke(forceWrite, null, null);
   }

   public void subscribeCov() {
      this.invoke(subscribeCov, null, null);
   }

   public void subscribeCovProperty() {
      this.invoke(subscribeCovProperty, null, null);
   }

   public BBoolean ackAlarm(BAlarmRecord parameter) {
      return (BBoolean)this.invoke(ackAlarm, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void doForceRead() {
      try {
         this.network().postAsync(new PointCmd(Integer.MIN_VALUE, this));
         if (this.isCOVProperty()) {
            this.network().postAsync(new PointCmd(-268435456, this));
         } else if (this.isCOV()) {
            this.network().postAsync(new PointCmd(1073741824, this));
         }
      } catch (Exception var2) {
         if (log.isLoggable(Level.FINE)) {
            log.fine("Failed to read " + this + ":" + var2);
         }
      }
   }

   public void doForceWrite() {
      this.write(null);
   }

   public void doSubscribeCov() {
      if (this.isSubscribedDesired()) {
         switch (this.subState) {
            case 0:
            case 1:
               this.setSubState(3);
               break;
            case 2:
               this.setSubState(5);
               break;
            case 3:
               return;
            case 4:
            case 5:
         }

         this.submitSubscribeCmd(1073741824);
      }
   }

   public void doSubscribeCovProperty() {
      if (this.isSubscribedDesired()) {
         switch (this.subState) {
            case 0:
            case 1:
               this.setSubState(6);
            case 2:
            case 3:
            case 4:
            case 5:
            case 8:
            default:
               break;
            case 6:
               return;
            case 7:
               this.setSubState(8);
         }

         this.submitSubscribeCmd(-268435456);
      }
   }

   private void submitSubscribeCmd(int pointCmd) {
      boolean postFailed = false;

      try {
         this.network().postAsync(new PointCmd(pointCmd, this));
      } catch (Exception var4) {
         postFailed = true;
         this.setSubState(4);
         this.pollService = (BBacnetPoll)this.network().getPollService(this);
         this.pollService.subscribe(this);
      }

      if (pointCmd == -268435456) {
         this.scheduleResubscribeProperty(postFailed);
      } else {
         this.scheduleResubscribe(postFailed);
      }
   }

   public BBoolean doAckAlarm(BAlarmRecord alarm) {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler().doAckAlarm(alarm);
   }

   public void started() throws Exception {
      super.started();
      this.setDebug();
      this.setAsnType();
      this.setDataSize();
      this.discoverPrioritizedPresentValue();
      if (this.getParentPoint().isWritablePoint()) {
         this.setWriteStatus(WRITABLE);
      }

      this.points().registerPoint(this);
   }

   public void stopped() throws Exception {
      super.stopped();
      this.points().unregisterPoint(this);
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if (this.get(p) instanceof BBacnetDeviceObjectPropertyReference) {
            this.readUnsubscribed(null);
            this.readSubscribed(null);
         } else if (p.getName().equals("debug")) {
            this.setDebug();
         }
      }
   }

   public void removed(Property p, BValue v, Context cx) {
      super.removed(p, v, cx);
      if (v instanceof BBacnetDeviceObjectPropertyReference) {
         this.readUnsubscribed(null);
         this.readSubscribed(null);
      } else if (p.getName().equals("debug")) {
         this.debug = false;
      }
   }

   public final void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(objectId) || p.equals(propertyId)) {
            PropertyInfo info = this.device().getPropertyInfo(this.getObjectId().getObjectType(), this.getPropertyId().getOrdinal());
            if (info != null) {
               this.setDataType(AsnUtil.getAsnTypeName(info.getAsnType()));
            }

            if (this.isSubscribed()) {
               this.readUnsubscribed(null);
               this.readSubscribed(null);
            }

            this.setAsnType();
            this.setDataSize();
            BFacets f = this.getDeviceFacets();
            f = BFacets.makeRemove(f, "priPV");
            this.setDeviceFacets(f);
            this.discoverPrioritizedPresentValue();
            this.points().reregisterPoint(this);
         } else if (p.equals(propertyArrayIndex)) {
            if (this.isSubscribed()) {
               this.readUnsubscribed(null);
               this.readSubscribed(null);
            }

            this.points().reregisterPoint(this);
         } else if (p.equals(dataType)) {
            this.setAsnType();
            this.setDataSize();
         } else if (p.equals(tuningPolicyName)) {
            if (this.isSubscribed()) {
               this.readUnsubscribed(null);
               this.readSubscribed(null);
            }
         } else if (p.equals(deviceFacets)) {
            this.priPV = (BBoolean)this.getDeviceFacets().get("priPV");
         } else if (p.getName().equals("debug")) {
            this.setDebug();
         } else {
            BValue v = this.get(p);
            if (v instanceof BBacnetDeviceObjectPropertyReference && this.isSubscribed()) {
               this.readUnsubscribed(null);
               this.readSubscribed(null);
            }
         }
      }
   }

   public Type getDeviceExtType() {
      return BBacnetPointDeviceExt.TYPE;
   }

   public BReadWriteMode getMode() {
      return this.getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
   }

   @Override
   public void readFail(String cause) {
      this.setReadStatus(cause);
      super.readFail(cause);
   }

   public void writeFail(String cause) {
      this.setWriteStatus(cause);
      super.writeFail(cause);
   }

   public void pointFacetsChanged() {
      super.pointFacetsChanged();
      if (this.getParentPoint().isSubscribed()) {
         this.readUnsubscribed(null);
         this.readSubscribed(null);
      }

      this.useStatusFlags = null;
   }

   protected boolean isSiblingLegal(BComponent sibling) {
      if (sibling instanceof BIBacnetTrendLogExt) {
         this.ples = null;
      }

      return super.isSiblingLegal(sibling);
   }

   public final void readSubscribed(Context cx) {
      if (this.isRunning()) {
         BBacnetDevice device = this.device();
         if (this.useCov()
            && device.canAddCov()
            && BBacnetObjectType.canSupportCov(this.getObjectId().getObjectType(), device)
            && this.getPropertyId().getOrdinal() == 85) {
            this.subscribeCov();
            this.forceRead();
         } else if (this.useCovProperty() && device.canAddCovProperty()) {
            this.subscribeCovProperty();
            this.forceRead();
         } else {
            this.pollService = (BBacnetPoll)this.network().getPollService(this);
            this.pollService.subscribe(this);
            this.setSubState(1);
         }
      }
   }

   public final void readUnsubscribed(Context cx) {
      if (this.isCOVProperty()) {
         if (this.network().isRunning()) {
            this.device().unsubscribeCovProperty(this);
         }
      } else if (this.isCOV()) {
         if (this.network().isRunning()) {
            this.device().unsubscribeCov(this);
         }
      } else {
         this.pollService = (BBacnetPoll)this.network().getPollService(this);
         this.pollService.unsubscribe(this);
      }

      this.ples = null;
      if (this.ticket != null) {
         this.ticket.cancel();
      }

      this.ticket = null;
      this.setSubState(0);
   }

   public boolean write(Context cx) {
      try {
         int writeLevel = 0;
         BStatusValue writeValue;
         if (this.getActiveLevel() != 17 || !this.getWriteValue().getStatus().isNull()) {
            writeValue = (BStatusValue)this.getWriteValue().newCopy();
            if (this.isPrioritizedPresentValue()) {
               writeLevel = this.getActiveLevel();
            }
         } else if (this.isPriorityArrayPoint()) {
            writeValue = null;
         } else {
            if (!this.isPrioritizedPresentValue() || !this.network().setAndGetWriteOnFacetChange().getBoolean()) {
               return false;
            }

            writeValue = null;
         }

         this.network().postWrite(new PointCmd(536870912, this, writeValue, this.lastWriteLevel, writeLevel));
         if (this.isPrioritizedPresentValue()) {
            this.lastWriteLevel = writeLevel;
         }
      } catch (Throwable var4) {
         String postWriteFailed = lex.getText("point.writeFail");
         this.writeFail(postWriteFailed + ":" + var4);
         log.log(Level.INFO, "Error writing BACnet point " + this + " in " + this.device() + ": " + var4, var4);
      }

      return false;
   }

   @Override
   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   @Override
   public final int getPollableType() {
      return 1;
   }

   @Deprecated
   @Override
   public boolean poll() {
      if (!this.device().getEnabled() || this.device().getStatus().isDown()) {
         return false;
      } else {
         return !this.getObjectId().isValid() ? false : this.device().poll(this);
      }
   }

   public BPollFrequency getPollFrequency() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getPollFrequency();
   }

   @Override
   public final PollListEntry[] getPollListEntries() {
      if (this.ples == null) {
         Array<PollListEntry> a = new Array(PollListEntry.class);
         a.add(new PollListEntry(this));
         boolean sfAdded = false;
         BFacets facets = this.getParentPoint().getFacets();
         if (this.useStatusFlags()) {
            a.add(new PollListEntry(this, 111));
            sfAdded = true;
         }

         BBoolean f = (BBoolean)facets.getFacet("eventState");
         if (f != null && f.getBoolean()) {
            a.add(new PollListEntry(this, 36));
         }

         f = (BBoolean)facets.getFacet("priorityArray");
         if (f != null && f.getBoolean()) {
            a.add(new PollListEntry(this, 87));
         }

         f = (BBoolean)facets.getFacet("reliability");
         if (f != null && f.getBoolean()) {
            a.add(new PollListEntry(this, 103));
         }

         SlotCursor<Property> sc = this.getProperties();

         while (sc.next(BBacnetDeviceObjectPropertyReference.class)) {
            BBacnetDeviceObjectPropertyReference dopr = (BBacnetDeviceObjectPropertyReference)sc.get();
            BBacnetObjectIdentifier deviceId = dopr.getDeviceId();
            if (dopr.isDeviceIdUsed() && deviceId.hashCode() != this.device().getObjectId().hashCode()) {
               BBacnetDevice dev = this.network().doLookupDeviceById(deviceId);
               if (dev == null) {
                  throw new IllegalStateException("Cannot find BACnet device for point metadata:" + this + " ref=" + dopr);
               }

               a.add(new PollListEntry(dopr.getObjectId(), dopr.getPropertyId(), dopr.getPropertyArrayIndex(), dev, this));
            } else {
               a.add(new PollListEntry(dopr.getObjectId(), dopr.getPropertyId(), dopr.getPropertyArrayIndex(), this.device(), this));
            }
         }

         BIBacnetTrendLogExt[] logs = (BIBacnetTrendLogExt[])this.getParentPoint().getChildren(BIBacnetTrendLogExt.class);
         if (logs.length > 0 && !sfAdded) {
            a.add(new PollListEntry(this, 111));
         }

         this.ples = (PollListEntry[])a.trim();
      }

      return this.ples;
   }

   public final BBacnetPointDeviceExt points() {
      return (BBacnetPointDeviceExt)this.getDeviceExt();
   }

   public boolean useCov() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getUseCov();
   }

   public boolean useConfirmedCov() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getUseConfirmedCov();
   }

   public int getCovSubscriptionLifetime() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getCovSubscriptionLifetime();
   }

   public boolean useCovProperty() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getUseCovProperty();
   }

   public boolean useConfirmedCovProperty() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getUseConfirmedCovProperty();
   }

   public int getCovPropertySubscriptionLifetime() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getCovPropertySubscriptionLifetime();
   }

   public BDouble getCovPropertyIncrement() {
      return BDouble.make(((BBacnetTuningPolicy)this.getTuningPolicy()).getCovPropertyIncrement());
   }

   public boolean getAcceptUnsolicitedCov() {
      return ((BBacnetTuningPolicy)this.getTuningPolicy()).getAcceptUnsolicitedCov();
   }

   public void tuningChanged(BBacnetTuningPolicy policy, Context cx) {
      if ((policy == null || policy == this.getTuningPolicy()) && this.isSubscribed()) {
         this.readUnsubscribed(cx);
         this.readSubscribed(cx);
      }
   }

   public String toString(Context cx) {
      char sep = (char)(cx == nameContext ? 95 : 58);
      return this.getObjectId().toString(cx) + sep + this.getPropertyId() + sep + this.getPropertyArrayIndex() + sep + this.getDataType();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder(super.toDebugString());
      sb.append(" dev=")
         .append(this.device().getObjectId().toString())
         .append(" oid=")
         .append(this.getObjectId().toString())
         .append(" pid=")
         .append(this.getPropertyId())
         .append(" ndx=")
         .append(this.getPropertyArrayIndex())
         .append(" asn=")
         .append(this.getDataType())
         .append(" run=")
         .append(this.isRunning())
         .append(" sub=")
         .append(this.isSubscribed());
      return sb.toString();
   }

   public BFacets getSlotFacets(Slot slot) {
      if (!this.isMounted()) {
         return super.getSlotFacets(slot);
      } else {
         BBacnetDevice device = this.bacnetDevice();
         if (device != null) {
            if (slot.equals(objectId)) {
               return device.getEnumerationList().getObjectTypeFacets();
            }

            if (slot.equals(propertyId)) {
               return device.getEnumerationList().getPropertyIdFacets();
            }
         }

         return super.getSlotFacets(slot);
      }
   }

   private BBacnetDevice bacnetDevice() {
      if (this.bacnetDevice == null) {
         for (BComplex dev = this.getParent(); dev != null; dev = dev.getParent()) {
            if (dev instanceof BBacnetDevice) {
               this.bacnetDevice = (BBacnetDevice)dev;
               break;
            }
         }
      }

      return this.bacnetDevice;
   }

   public final boolean isCOV() {
      return this.subState == 2 || this.subState == 5;
   }

   public final boolean isCOVPending() {
      return this.subState == 3 || this.subState == 4 || this.subState == 5;
   }

   public final boolean isCOVProperty() {
      return this.subState == 7 || this.subState == 8;
   }

   public final boolean isCOVPropertyPending() {
      return this.subState == 6 || this.subState == 4 || this.subState == 8;
   }

   public final boolean isCOVPropertyFailed() {
      return this.subState == 4;
   }

   public final boolean isPolled() {
      return this.subState == 1 || this.subState == 4;
   }

   @Deprecated
   public void setCOV(boolean cov) {
      if (cov) {
         this.subState = 2;
      }
   }

   public final void setSubState(int subState) {
      this.subState = subState;
      switch (subState) {
         case 0:
            this.setReadStatus(UNSUBSCRIBED);
            break;
         case 1:
            this.setReadStatus(POLLED);
            break;
         case 2:
            this.setReadStatus(COV);
            break;
         case 3:
            if (!this.isCOV()) {
               this.setReadStatus(COV_PENDING);
            }
            break;
         case 4:
            this.setReadStatus(POLLED);
         case 5:
         case 8:
         default:
            break;
         case 6:
            if (!this.isCOVProperty()) {
               this.setReadStatus(COVP_PENDING);
            }
            break;
         case 7:
            this.setReadStatus(COVP);
      }
   }

   public ErrorType getLastReadError() {
      return this.lastReadError;
   }

   public void setLastReadError(ErrorType e) {
      this.lastReadError = e;
   }

   public boolean useStatusFlags() {
      return this.getPropertyId().getOrdinal() == 85
         && BBacnetObjectType.hasStatusFlags(this.getObjectId().getObjectType(), this.device())
         && this.statusFlagsEnabled();
   }

   private boolean statusFlagsEnabled() {
      if (this.useStatusFlags != null) {
         return this.useStatusFlags.getBoolean();
      } else {
         BControlPoint parent = this.getParentPoint();
         if (parent != null) {
            BFacets facets = parent.getFacets();
            if (facets != null) {
               BObject facet = facets.getFacet("statusFlags");
               if (facet instanceof BBoolean) {
                  this.useStatusFlags = (BBoolean)facet;
                  return this.useStatusFlags.getBoolean();
               }
            }
         }

         return false;
      }
   }

   public boolean isPriorityArrayPoint() {
      return this.getPropertyId().getOrdinal() == 87;
   }

   public boolean isPrioritizedPresentValue() {
      if (this.priPV != null) {
         return this.priPV.getBoolean();
      } else {
         log.severe("\n***\n***\n***\n\npriPV is NULL in " + this + "!  How did we get here?? thd: " + Thread.currentThread().getName());
         Thread.dumpStack();
         return true;
      }
   }

   public int getDataSize() {
      return this.dataSize;
   }

   public void setPrioritizedPresentValue(boolean prioritizedPV) {
      this.priPV = BBoolean.make(prioritizedPV);
      if (!prioritizedPV) {
         this.lastWriteLevel = 0;
      }

      BFacets f = this.getDeviceFacets();
      f = BFacets.make(f, "priPV", prioritizedPV ? BBoolean.TRUE : BBoolean.FALSE);
      this.setDeviceFacets(f);
   }

   @Override
   public abstract void fromEncodedValue(byte[] var1, BStatus var2, Context var3);

   public abstract byte[] toEncodedValue(BStatusValue var1);

   protected void updateReadStatus(Context cx) {
      if (cx == covContext) {
         if (this.isCOVProperty()) {
            this.setReadStatus(COVP);
         } else if (this.isCOV()) {
            this.setReadStatus(COV);
         }
      } else if (this.isPolled() && (cx == PollListEntry.pointCx || cx.getBase() == PollListEntry.pointCx)) {
         this.setReadStatus(POLLED);
      }
   }

   protected void readMetaData(byte[] encodedValue, Context cx, BStatusValue dv) throws AsnException {
      BStatus s = dv.getStatus();
      PollListEntry ple = (PollListEntry)cx;
      BBacnetObjectIdentifier objectId = ple.getObjectId();
      if (objectId.hashCode() == this.getObjectId().hashCode()) {
         int propId = ple.getPropertyId();
         label61:
         switch (propId) {
            case 36:
               int esord = AsnUtil.fromAsnEnumerated(encodedValue);
               String estag = BBacnetEventState.tag(esord);
               s = BStatus.make(s, "state", estag);
               break;
            case 87:
               s = BStatus.make(s, "bac", BString.make("def"));
               AsnInputStream asnIn = AsnInputStream.make(encodedValue);

               try {
                  int tag = 0;
                  int activeLevel = 0;

                  for (int var28 = 1; var28 <= 16; var28++) {
                     tag = asnIn.skipTag();
                     if (tag != 0) {
                        s = BStatus.make(s, "bac", BInteger.make(var28));
                        break label61;
                     }
                  }
                  break;
               } finally {
                  asnIn.release();
               }
            case 103:
               int rord = AsnUtil.fromAsnEnumerated(encodedValue);
               String rtag = BBacnetReliability.tag(rord);
               s = BStatus.make(s, "reliability", rtag);
               break;
            case 111:
               s = AsnUtil.asnStatusFlagsToBStatus(encodedValue);
               break;
            default:
               BValue val = AsnUtil.asnToValue(this.device().getPropertyInfo(ple.getObjectId().getObjectType(), propId), encodedValue);
               String key = BBacnetPropertyIdentifier.tag(propId);
               s = BStatus.make(s, key, val.toString());
         }
      } else if (objectId.getObjectType() == 9) {
         int propId = ple.getPropertyId();
         switch (propId) {
            case 36:
               int esord = AsnUtil.fromAsnEnumerated(encodedValue);
               String estag = BBacnetEventState.tag(esord);
               s = BStatus.make(s, "EE" + objectId.getInstanceNumber(), estag);
               break;
            default:
               BValue val = AsnUtil.asnToValue(this.device().getPropertyInfo(9, propId), encodedValue);
               String key = BBacnetPropertyIdentifier.tag(propId);
               s = BStatus.make(s, "EE" + objectId.getInstanceNumber() + "_" + key, val.toString());
         }
      } else {
         String key = ple.getObjectId().toString(BacnetConst.facetsContext) + "_" + BBacnetPropertyIdentifier.tag(ple.getPropertyId());
         BValue val = AsnUtil.asnToValue(this.device().getPropertyInfo(ple.getObjectId().getObjectType(), ple.getPropertyId()), encodedValue);
         s = BStatus.make(s, key, val.toString());
      }

      dv.setStatus(s);
   }

   protected final BBacnetNetwork network() {
      return (BBacnetNetwork)this.getNetwork();
   }

   protected BBacnetAddress getDeviceAddress() {
      return ((BBacnetDevice)this.getDevice()).getAddress();
   }

   private int getActiveLevel() {
      return this.getWriteValue().getStatus().geti("activeLevel", 17);
   }

   private void scheduleResubscribe(boolean postFailed) {
      if (this.ticket != null) {
         this.ticket.cancel();
      }

      BRelTime resubscribeTime = calculateResubscribeTime(postFailed, this.getCovSubscriptionLifetime());
      this.ticket = Clock.schedule(this, resubscribeTime, subscribeCov, null);
   }

   private void scheduleResubscribeProperty(boolean postFailed) {
      if (this.ticket != null) {
         this.ticket.cancel();
      }

      BRelTime resubscribeTime = calculateResubscribeTime(postFailed, this.getCovPropertySubscriptionLifetime());
      this.ticket = Clock.schedule(this, resubscribeTime, subscribeCovProperty, null);
   }

   public static BRelTime calculateResubscribeTime(boolean postFailed, int subscriptionLifeTime) {
      if (postFailed) {
         return RELTIME_ON_POST_FAILURE;
      } else if (subscriptionLifeTime <= 0) {
         return ONCE_A_DAY_RELTIME;
      } else {
         return subscriptionLifeTime <= 5 ? MINIMUM_RELTIME : BRelTime.make(subscriptionLifeTime * 30000L);
      }
   }

   private void setDataSize() {
      switch (this.asnType) {
         case 0:
            this.dataSize = 1;
            break;
         case 1:
            this.dataSize = 2;
            break;
         case 2:
         case 3:
         case 4:
         case 6:
         case 8:
         default:
            this.dataSize = 5;
            break;
         case 5:
            this.dataSize = 9;
            break;
         case 7:
            this.dataSize = 20;
            break;
         case 9:
            this.dataSize = 3;
      }
   }

   private void setAsnType() {
      this.asnType = AsnUtil.getAsnType(this.getDataType());
   }

   public void discoverPrioritizedPresentValue() {
      this.discoverPrioritizedPresentValue(false);
   }

   public void discoverPrioritizedPresentValue(boolean force) {
      int otyp = this.getObjectId().getObjectType();
      switch (otyp) {
         case 1:
         case 4:
         case 14:
            this.priPV = BBoolean.make(this.getPropertyId().getOrdinal() == 85);
            break;
         case 2:
         case 5:
         case 19:
         case 40:
         case 45:
         case 46:
         case 48:
            if (this.getPropertyId().getOrdinal() == 85) {
               BFacets f = this.getDeviceFacets();
               BBoolean ppv = (BBoolean)f.getFacet("priPV");
               if (!(this.getParentPoint() instanceof BIWritablePoint) || ppv != null && !force) {
                  this.priPV = ppv;
               } else {
                  this.network().postWrite(new Runnable() {
                     @Override
                     public void run() {
                        BBacnetProxyExt.this.checkForPriorityArray();
                     }
                  });
               }
            } else {
               this.priPV = BBoolean.FALSE;
            }
            break;
         default:
            this.priPV = BBoolean.FALSE;
      }
   }

   private void checkForPriorityArray() {
      BFacets f = this.getDeviceFacets();
      BBoolean priArr = BacnetDiscoveryUtil.checkForPriorityArray(this.getObjectId(), this.device());
      this.setDeviceFacets(BFacets.make(f, "priPV", priArr));
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetProxyExt", 2);
      out.prop("asnType", this.asnType);
      out.prop("dataSize", this.dataSize);
      out.prop("lastReadError", this.lastReadError);
      out.prop("ticket", this.ticket);
      out.prop("prioritizedPV", this.priPV);
      out.prop("lastWriteLevel", this.lastWriteLevel);
      out.prop("subState", this.subState);
      out.prop("pollService", this.pollService);
      if (this.ples != null) {
         out.prop("ples", this.ples.length);

         for (int i = 0; i < this.ples.length; i++) {
            out.prop("ples[" + i + "]:", this.ples[i].debugString());
         }
      }

      out.endProps();
   }

   void dbg(String s) {
      if (this.debug) {
         System.out.println("BACPxExtDBG{" + this + "}:" + s);
      }
   }

   private void setDebug() {
      BBoolean dbg = (BBoolean)this.get("debug");
      if (dbg != null) {
         this.debug = dbg.getBoolean();
      }
   }
}
