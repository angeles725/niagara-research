package com.tridium.bacnet.history;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.datatypes.BTrendEvent;
import com.tridium.bacnet.services.BacnetComplexAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyAck;
import com.tridium.bacnet.services.confirmed.ReadPropertyRequest;
import com.tridium.bacnet.services.confirmed.SubscribeCovRequest;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.network.BNetworkPriority;
import com.tridium.bacnet.stack.network.InvalidNetworkMsgException;
import com.tridium.bacnet.stack.network.UnsupportedNetworkMsgException;
import com.tridium.bacnet.stack.network.UnsupportedProtocolVersionException;
import com.tridium.bacnet.stack.transport.BBacnetTransportLayer;
import com.tridium.bacnet.stack.transport.InvalidApduLengthException;
import com.tridium.bacnet.stack.transport.InvalidApduTypeException;
import com.tridium.bacnet.stack.transport.TransactionException;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.AbortException;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.DataTypeNotSupportedException;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.OutOfRangeException;
import javax.baja.bacnet.io.RejectException;
import javax.baja.bacnet.listeners.DynamicIAmListener;
import javax.baja.history.BCollectionInterval;
import javax.baja.history.BRolloverValue;
import javax.baja.history.ext.BHistoryExt;
import javax.baja.history.ext.BIntervalHistoryExt;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "activePeriod",
      type = "BActivePeriod",
      defaultValue = "new BBacnetActivePeriod()",
      facets = {@Facet("BFacets.make(\"alwaysExpand\", true)")},
      override = true
   ), @NiagaraProperty(
      name = "interval",
      type = "BRelTime",
      defaultValue = "BRelTime.makeMinutes(15)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(0L)"
      )},
      override = true
   ), @NiagaraProperty(
      name = "changeTolerance",
      type = "double",
      defaultValue = "0d"
   ), @NiagaraProperty(
      name = "totalRecordCount",
      type = "long",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "precision",
      type = "int",
      defaultValue = "32"
   ), @NiagaraProperty(
      name = "minRolloverValue",
      type = "BRolloverValue",
      defaultValue = "new BRolloverValue()"
   ), @NiagaraProperty(
      name = "maxRolloverValue",
      type = "BRolloverValue",
      defaultValue = "new BRolloverValue()"
   ), @NiagaraProperty(
      name = "covResubscriptionInterval",
      type = "int",
      defaultValue = "5"
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(-1)",
      flags = 5
   ), @NiagaraProperty(
      name = "propertyId",
      type = "int",
      defaultValue = "-1",
      flags = 5
   ), @NiagaraProperty(
      name = "arrayIndex",
      type = "int",
      defaultValue = "-1",
      flags = 5
   )})
@NiagaraActions({@NiagaraAction(
      name = "subscribeOnCoVSubscriptionInterval",
      flags = 4
   ), @NiagaraAction(
      name = "startLogging"
   ), @NiagaraAction(
      name = "stopLogging"
   )})
public abstract class BBacnetTrendLogRemoteExt extends BIntervalHistoryExt implements BIBacnetTrendLogExt {
   public static final Property activePeriod = newProperty(0, new BBacnetActivePeriod(), BFacets.make("alwaysExpand", true));
   public static final Property interval = newProperty(0, BRelTime.makeMinutes(15), BFacets.make("min", BRelTime.make(0L)));
   public static final Property changeTolerance = newProperty(0, 0.0, null);
   public static final Property totalRecordCount = newProperty(0, 0, null);
   public static final Property precision = newProperty(0, 32, null);
   public static final Property minRolloverValue = newProperty(0, new BRolloverValue(), null);
   public static final Property maxRolloverValue = newProperty(0, new BRolloverValue(), null);
   public static final Property covResubscriptionInterval = newProperty(0, 5, null);
   public static final Property objectId = newProperty(5, BBacnetObjectIdentifier.make(-1), null);
   public static final Property propertyId = newProperty(5, -1, null);
   public static final Property arrayIndex = newProperty(5, -1, null);
   public static final Action subscribeOnCoVSubscriptionInterval = newAction(4, null);
   public static final Action startLogging = newAction(0, null);
   public static final Action stopLogging = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BBacnetTrendLogRemoteExt.class);
   private BStatusValue lastValue;
   private boolean trigger = false;
   private Lock lock = new ReentrantLock();
   private BBacnetTransportLayer transport;
   private BBacnetDevice device;
   private static Logger logger = Logger.getLogger("bacnet.server");
   private boolean subscribed = false;
   private boolean intervalGreaterThan20Sec = false;
   private Ticket ticketCovSubscription;
   private boolean isIAmListening;
   private boolean unknownDeviceErrorLogged;

   public double getChangeTolerance() {
      return this.getDouble(changeTolerance);
   }

   public void setChangeTolerance(double v) {
      this.setDouble(changeTolerance, v, null);
   }

   @Override
   public long getTotalRecordCount() {
      return this.getLong(totalRecordCount);
   }

   @Override
   public void setTotalRecordCount(long v) {
      this.setLong(totalRecordCount, v, null);
   }

   public int getPrecision() {
      return this.getInt(precision);
   }

   public void setPrecision(int v) {
      this.setInt(precision, v, null);
   }

   public BRolloverValue getMinRolloverValue() {
      return (BRolloverValue)this.get(minRolloverValue);
   }

   public void setMinRolloverValue(BRolloverValue v) {
      this.set(minRolloverValue, v, null);
   }

   public BRolloverValue getMaxRolloverValue() {
      return (BRolloverValue)this.get(maxRolloverValue);
   }

   public void setMaxRolloverValue(BRolloverValue v) {
      this.set(maxRolloverValue, v, null);
   }

   public int getCovResubscriptionInterval() {
      return this.getInt(covResubscriptionInterval);
   }

   public void setCovResubscriptionInterval(int v) {
      this.setInt(covResubscriptionInterval, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public int getPropertyId() {
      return this.getInt(propertyId);
   }

   public void setPropertyId(int v) {
      this.setInt(propertyId, v, null);
   }

   public int getArrayIndex() {
      return this.getInt(arrayIndex);
   }

   public void setArrayIndex(int v) {
      this.setInt(arrayIndex, v, null);
   }

   public void subscribeOnCoVSubscriptionInterval() {
      this.invoke(subscribeOnCoVSubscriptionInterval, null, null);
   }

   public void startLogging() {
      this.invoke(startLogging, null, null);
   }

   public void stopLogging() {
      this.invoke(stopLogging, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetTrendLogRemoteExt() {
   }

   public BBacnetTrendLogRemoteExt(BBacnetDevice device, BBacnetObjectIdentifier objectId, int propertyId, int arrayIndex) {
      this.setArrayIndex(arrayIndex);
      this.setPropertyId(propertyId);
      this.setObjectId(objectId);
      this.setDevice(device);
   }

   public void doSubscribeOnCoVSubscriptionInterval() {
      this.subscribeCov(false);
   }

   public void doStartLogging() {
      this.setEnabled(true);
   }

   public void updateStatus() {
      BStatus prevStatus = this.getStatus();
      String prevFaultCause = this.getFaultCause();
      super.updateStatus();
      BStatus statusAfterUpdate = this.getStatus();
      String faultCauseAfterUpdate = this.getFaultCause();
      if (this.getInterval().getMillis() == 0L && this.getFaultCause().startsWith("Invalid interval")) {
         if (prevStatus.isFault() && !prevFaultCause.equals(faultCauseAfterUpdate)) {
            this.setStatus(prevStatus);
            this.setFaultCause(prevFaultCause);
         } else if (!prevStatus.isFault() && !prevFaultCause.equals(faultCauseAfterUpdate)) {
            this.setStatus(BStatus.makeFault(statusAfterUpdate, false));
            this.setFaultCause(prevFaultCause);
         } else {
            this.setStatus(BStatus.makeFault(statusAfterUpdate, false));
            this.setFaultCause("");
         }

         this.getHistoryConfig().setInterval(BCollectionInterval.make(BRelTime.make(0L)));
         this.deactivated(BAbsTime.now(), this.getParentPoint().getOutStatusValue());
      }
   }

   private boolean isValidInterval(BRelTime interval) {
      return interval.getMillis() >= 0L;
   }

   public void doStopLogging() {
      this.doLogging(false);
      this.setEnabled(false);
   }

   public void doLogging(boolean enabled) {
      try {
         this.lock.lock();
         this.trigger = true;
         BAbsTime timestamp = BAbsTime.make();
         if (this.getRecord() == null) {
            BacnetTrendLogUtil.writeEvent(
               this,
               timestamp,
               BStatus.DEFAULT,
               BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()),
               enabled ? BTrendEvent.LOG_STATUS_ENABLED : BTrendEvent.LOG_STATUS_DISABLED
            );
         }
      } catch (Exception var6) {
         logger.log(Level.SEVERE, "Error storing event", (Throwable)var6);
      } finally {
         this.lock.unlock();
      }
   }

   public void doIntervalElapsed() {
      if (BBacnetNetwork.bacnet() != null && BBacnetNetwork.bacnet().isRunning() && BBacnetNetwork.localDevice().isRunning() && this.getEnabled()) {
         if (this.getInterval().getMillis() == 0L) {
            this.deactivated(BAbsTime.now(), this.getParentPoint().getStatusValue());
            if (!this.subscribed) {
               this.subscribeCov(true);
            }
         } else if (this.intervalGreaterThan20Sec) {
            try {
               this.writeRecord(BAbsTime.now(), this.getParentPoint().getStatusValue());
            } catch (IOException var2) {
               logger.log(Level.SEVERE, "Could not write record.", (Throwable)var2);
            }
         } else {
            try {
               if (this.getDevice().isDown()) {
                  int deviceInstNumber = this.getDevice().getObjectId().getInstanceNumber();
                  this.logUnknownDeviceError();
                  this.whoIs(deviceInstNumber);
                  return;
               }

               BStatusValue value = this.readProperty(this.getObjectId(), this.getPropertyId(), this.getArrayIndex());
               this.writeRecord(BAbsTime.now(), value);
            } catch (IOException var3) {
               logger.log(Level.SEVERE, "Could not write record.", (Throwable)var3);
            } catch (ErrorException var4) {
               this.logErrorException(var4);
            } catch (BacnetException var5) {
               this.logUnknownDeviceError();
            }
         }
      }
   }

   private void readAndWritePropertyRecord() {
      try {
         BStatusValue value = this.readProperty(this.getObjectId(), this.getPropertyId(), this.getArrayIndex());
         this.writeRecord(BAbsTime.now(), value);
      } catch (IOException var4) {
         logger.log(Level.SEVERE, "Could not write record.", (Throwable)var4);
      } catch (ErrorException var5) {
         this.logErrorException(var5);
      } catch (BacnetException var6) {
         int errorClass = 5;
         int errorCode = this.getErrorCode(var6);
         this.logException(errorClass, errorCode);
      }
   }

   private int getErrorCode(BacnetException e) {
      int errorCode = 0;
      if (e instanceof AsnException) {
         if (e instanceof OutOfRangeException) {
            errorCode = 80;
         } else if (e instanceof DataTypeNotSupportedException) {
            errorCode = 47;
         } else {
            errorCode = -4;
         }
      }

      if (e instanceof RejectException) {
         errorCode = 69;
      }

      if (e instanceof AbortException) {
         errorCode = 56;
      }

      if (e instanceof BacnetStackException) {
         if (e instanceof InvalidApduTypeException) {
            errorCode = 52;
         }

         if (e instanceof InvalidApduLengthException || e instanceof InvalidNetworkMsgException) {
            errorCode = 123;
         }

         if (e instanceof UnsupportedNetworkMsgException) {
            errorCode = 112;
         }

         if (e instanceof UnsupportedProtocolVersionException) {
            errorCode = 129;
         }

         if (e instanceof TransactionException) {
            errorCode = 25;
         }
      }

      return errorCode;
   }

   private BStatusValue readProperty(BBacnetObjectIdentifier oid, int propertyId, int arrayIndex) throws BacnetException {
      BBacnetAddress deviceAddress = this.getDevice().getAddress();
      ReadPropertyRequest req = new ReadPropertyRequest(oid, propertyId, arrayIndex);
      BacnetComplexAck complex = this.getTransport().sendConfirmedRequestComplex(req, deviceAddress, BNetworkPriority.normal);
      ReadPropertyAck readPropertyAck = (ReadPropertyAck)complex;
      BValue v = AsnUtil.fromAsn(readPropertyAck.getEncodedValue())[0];
      return this.getStatusValue(v);
   }

   private void logUnknownDeviceError() {
      if (!this.unknownDeviceErrorLogged) {
         this.unknownDeviceErrorLogged = true;
         NErrorType error = new NErrorType(7, 70);
         this.writeErrorEvent(BTrendEvent.makeFailure(error), 16);
      }
   }

   private void logErrorException(ErrorException errorException) {
      int errorClass = errorException.getErrorType().getErrorClass();
      int errorCode = errorException.getErrorType().getErrorCode();
      this.writeErrorEvent(BTrendEvent.makeFailure(new NErrorType(errorClass, errorCode)), this.getStatus().getBits());
      if (errorCode == 31) {
         this.setEnabled(false);
      }
   }

   private void logException(int errorClass, int errorCode) {
      this.writeErrorEvent(BTrendEvent.makeFailure(new NErrorType(errorClass, errorCode)), this.getStatus().getBits());
      this.writeErrorEvent(BTrendEvent.make("NULL"), this.getStatus().getBits());
      if (errorCode == 31) {
         this.setEnabled(false);
      }
   }

   public void pointChanged(BAbsTime timestamp, BStatusValue out) {
      try {
         if (this.getInterval().getMillis() == 0L) {
            this.writeRecord(timestamp, out);
         }
      } catch (IOException var4) {
         logger.log(Level.SEVERE, "Could not write record on the point change.");
      }
   }

   private void writeErrorEvent(BTrendEvent event, int statusBits) {
      BStatus newStatus = BStatus.make(statusBits, this.getStatus().getFacets());
      if (!newStatus.equals(this.getStatus())) {
         this.setStatus(newStatus);
      }

      try {
         this.lock.lock();
         BacnetTrendLogUtil.writeEvent(this, BAbsTime.now(), this.getStatus(), BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()), event);
      } catch (IOException var8) {
         logger.log(Level.INFO, "Could not log the subscribe message in the records.");
      } finally {
         this.lock.unlock();
      }
   }

   public void activated(BAbsTime startTime, BAbsTime currentTime, BStatusValue value) throws IOException {
      super.activated(startTime, currentTime, value);
      if (this.getInterval().getMillis() == 0L) {
         this.writeRecord(currentTime, value);
      }
   }

   public abstract Type getRecordType();

   @Override
   public abstract BBacnetTrendRecord getRecord();

   protected abstract BStatusValue getStatusValue(BValue var1);

   @Override
   public ErrorType setLogInterval(long logInterval, Context cx) {
      if (logInterval >= 0L) {
         this.set(interval, BRelTime.make(logInterval), cx);
      }

      return null;
   }

   @Override
   public boolean getTrigger() {
      return this.trigger;
   }

   protected void writeRecord(BAbsTime timestamp, BStatusValue out) throws IOException {
      try {
         this.lock.lock();
         if (!this.areEqual(this.lastValue, out) || !this.subscribed) {
            if (out == null) {
               this.lastValue = null;
            } else {
               this.lastValue = (BStatusValue)out.newCopy(true);
            }

            if (this.lastValue != null) {
               BacnetTrendLogUtil.writeRecord(this, timestamp, out);
            } else {
               BacnetTrendLogUtil.writeEvent(
                  this,
                  timestamp,
                  this.getStatus(),
                  BacnetTrendLogUtil.incrementSequenceNumber(this.getTotalRecordCount()),
                  BTrendEvent.makeFailure(new NErrorType(2, 355))
               );
            }

            return;
         }
      } finally {
         this.lock.unlock();
      }
   }

   public BBacnetTransportLayer getTransport() {
      if (this.transport == null) {
         BBacnetNetwork network = BBacnetNetwork.bacnet();
         this.transport = ((BBacnetStack)network.getBacnetComm()).getTransport();
      }

      return this.transport;
   }

   public BBacnetDevice getDevice() {
      if (this.device == null) {
         this.device = (BBacnetDevice)this.getParent().getParent().getParent();
      }

      return this.device;
   }

   public void clockChanged(BRelTime shift) throws Exception {
      if (this.isValidInterval(shift)) {
         super.clockChanged(shift);
      }
   }

   private long fetchProcessId() {
      long processId = 0L;
      if (this.device != null) {
         processId = this.device.getAlarms().getNiagaraProcessId();
      }

      return processId;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p.equals(interval)) {
         this.setTotalRecordCount(0L);
         if (this.getInterval().getMillis() == 0L && !this.subscribed) {
            this.deactivated(BAbsTime.now(), this.lastValue);
            this.subscribeCov(true);
         } else if (this.getInterval().getMillis() > 0L && this.subscribed) {
            this.intervalGreaterThan20Sec = this.getInterval().getMillis() / 1000L > 20L;
            this.unsubscribeCov();
            this.doIntervalElapsed();
         }
      }

      if (p.equals(enabled)) {
         if (this.getEnabled()) {
            if (this.getInterval().getMillis() == 0L) {
               this.subscribeCov(true);
            } else {
               this.doIntervalElapsed();
            }
         } else {
            this.deactivated(BAbsTime.now(), this.lastValue);
            this.unsubscribeCov();
            this.doStopLogging();
         }
      }
   }

   public void atSteadyState() throws Exception {
      super.atSteadyState();
      if (this.getInterval().getMillis() == 0L && !this.getDevice().isDown()) {
         this.subscribeCov(true);
      }
   }

   public void stopped() throws Exception {
      if (this.subscribed) {
         this.unsubscribeCov();
      }

      super.stopped();
   }

   private void subscribeCov(boolean activateSchedule) {
      if (this.getEnabled()) {
         if (activateSchedule) {
            this.covSubscriptionBasedOnResubscritionInterval(true);
         }

         ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getConfirmedWorker().post(() -> {
            try {
               if (this.checkAndSendWhoIs()) {
                  return;
               }

               this.subscribed = false;
               long processId = this.fetchProcessId();
               SubscribeCovRequest request = new SubscribeCovRequest(processId, this.getObjectId(), true, this.getCovResubscriptionInterval() * 2);
               this.getTransport().sendConfirmedRequestSimple(request, this.getDevice().getAddress(), BNetworkPriority.normal);
               this.subscribed = true;
            } catch (BacnetStackException var4) {
               logger.log(Level.INFO, "Could not subscribe from CoV of remote point as the device is stale.");
            } catch (ErrorException var5) {
               this.logErrorException(var5);
               if (this.getEnabled()) {
                  this.setInterval(BRelTime.makeMinutes(15));
               }
            } catch (RejectException var6) {
               this.setInterval(BRelTime.makeMinutes(15));
               NErrorType errorType = new NErrorType(5, 43);
               this.writeErrorEvent(BTrendEvent.makeFailure(errorType), this.getDevice().getStatus().getBits());
            } catch (BacnetException var7) {
               logger.log(Level.INFO, "Could not unsubscribe from CoV of remote point.");
            }
         });
      }
   }

   private boolean checkAndSendWhoIs() throws BacnetException {
      if (this.getDevice().isDown()) {
         int instanceNum = this.getDevice().getObjectId().getInstanceNumber();
         this.logUnknownDeviceError();
         this.whoIs(instanceNum);
         if (!this.isIAmListening) {
            DynamicIAmListener.getDynamicIAmListenerInstance().subscribeHandler(this.iAmHandler(), this.getDevice().getObjectId());
            this.isIAmListening = false;
         }

         return true;
      } else {
         return false;
      }
   }

   private DynamicIAmListener.IAmHandler iAmHandler() {
      return new DynamicIAmListener.IAmHandler() {
         @Override
         public void handle(IAmRequest request, BBacnetAddress sourceAddress) {
            BBacnetTrendLogRemoteExt.this.isIAmListening = true;
            BBacnetTrendLogRemoteExt.this.unknownDeviceErrorLogged = false;
            if (BBacnetTrendLogRemoteExt.this.getInterval().getMillis() == 0L) {
               BBacnetTrendLogRemoteExt.this.unsubscribeCov();
               BBacnetTrendLogRemoteExt.this.subscribeCov(true);
            } else {
               BBacnetTrendLogRemoteExt.this.readAndWritePropertyRecord();
               BBacnetTrendLogRemoteExt.this.deactivated(BAbsTime.now(), BBacnetTrendLogRemoteExt.this.lastValue);

               try {
                  BBacnetTrendLogRemoteExt.this.activated(BAbsTime.now(), BAbsTime.now(), BBacnetTrendLogRemoteExt.this.lastValue);
               } catch (IOException var4) {
                  BHistoryExt.log.log(Level.SEVERE, "Could not start the schedule to log interval after deactivating.", (Throwable)var4);
               }
            }
         }
      };
   }

   private void whoIs(int deviceInstNumber) throws BacnetException {
      BBacnetClientLayer client = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
      client.whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, deviceInstNumber, deviceInstNumber);
   }

   private void unsubscribeCov() {
      this.covSubscriptionBasedOnResubscritionInterval(false);
      if (!this.getDevice().isDown() && this.subscribed) {
         long processId = this.fetchProcessId();
         SubscribeCovRequest request = new SubscribeCovRequest(processId, this.getObjectId());

         try {
            this.transport.sendConfirmedRequestSimple(request, this.device.getAddress(), BNetworkPriority.normal);
            this.subscribed = false;
         } catch (Exception var5) {
            logger.log(Level.SEVERE, "Could not send the Cov Request to Unsubscribe.");
         }
      }
   }

   private void covSubscriptionBasedOnResubscritionInterval(boolean flag) {
      if (this.ticketCovSubscription != null) {
         this.ticketCovSubscription.cancel();
      }

      if (flag) {
         this.ticketCovSubscription = Clock.schedulePeriodically(
            this, BRelTime.makeSeconds(this.getCovResubscriptionInterval()), subscribeOnCoVSubscriptionInterval, null
         );
      }
   }

   public void setDevice(BBacnetDevice v) {
      this.device = v;
   }

   protected abstract boolean areEqual(BStatusValue var1, BStatusValue var2);
}
