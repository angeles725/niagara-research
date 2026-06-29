package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.timers.TimerListener;
import com.tridium.bacnet.timers.Timers;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.util.QueueFullException;

public abstract class Transaction {
   public static final int IDLE = 0;
   public static final int SEGMENTED_REQUEST = 1;
   public static final int AWAIT_CONFIRMATION = 2;
   public static final int AWAIT_RESPONSE = 2;
   public static final int SEGMENTED_CONF = 3;
   public static final int SEGMENTED_RESPONSE = 3;
   private int timerId;
   private int segmentTimerId;
   private int segmentSize;
   private int retryCount;
   private int segmentRetryCount;
   private AtomicInteger duplicateCount = new AtomicInteger();
   private boolean sentAllSegments;
   private int lastSequenceNumber;
   private int initialSequenceNumber;
   private int actualWindowSize;
   private int proposedWindowSize;
   private int numSegments;
   private int segmentCounter;
   private int state;
   public boolean segmentTimerExpired = false;
   public boolean requestTimerExpired = false;
   private static Transaction.SegmentTimer segmentTimer = new Transaction.SegmentTimer();
   private static Transaction.RequestTimer requestTimer = new Transaction.RequestTimer();
   public static Logger logger = Logger.getLogger("bacnet.transport");

   protected Transaction() {
   }

   protected abstract ApplicationPdu getApdu();

   public abstract BBacnetAddress getClientAddress();

   public abstract BBacnetAddress getServerAddress();

   public abstract BBacnetAddress getAddress();

   public abstract int getInvokeId();

   public abstract boolean isServerTransaction();

   public void incrementDuplicateCount() {
      this.duplicateCount.getAndIncrement();
   }

   @Override
   public boolean equals(Object o) {
      boolean equals = false;
      if (o instanceof Transaction) {
         Transaction other = (Transaction)o;
         equals = this.getInvokeId() == other.getInvokeId() && this.getAddress() == other.getAddress();
      }

      return equals;
   }

   @Override
   public String toString() {
      return "state: "
         + this.getTag(this.getState())
         + ", invokeId: "
         + this.getInvokeId()
         + ", clientAddress: "
         + this.getClientAddress()
         + ", serverAddress: "
         + this.getServerAddress()
         + ", timerId: "
         + this.getTimerId();
   }

   public int getRetryCount() {
      return this.retryCount;
   }

   public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
   }

   public int getSegmentRetryCount() {
      return this.segmentRetryCount;
   }

   public void setSegmentRetryCount(int segmentRetryCount) {
      this.segmentRetryCount = segmentRetryCount;
   }

   public void setDuplicateCount(int duplicateCount) {
      this.duplicateCount.set(duplicateCount);
   }

   public int getDuplicateCount() {
      return this.duplicateCount.get();
   }

   public boolean getSentAllSegments() {
      return this.sentAllSegments;
   }

   public void setSentAllSegments(boolean sentAllSegments) {
      this.sentAllSegments = sentAllSegments;
   }

   public int getLastSequenceNumber() {
      return this.lastSequenceNumber;
   }

   public void setLastSequenceNumber(int lastSequenceNumber) {
      this.lastSequenceNumber = lastSequenceNumber;
   }

   public int getInitialSequenceNumber() {
      return this.initialSequenceNumber;
   }

   public void setInitialSequenceNumber(int initialSequenceNumber) {
      this.initialSequenceNumber = initialSequenceNumber;
   }

   public int getActualWindowSize() {
      return this.actualWindowSize;
   }

   public void setActualWindowSize(int actualWindowSize) {
      this.actualWindowSize = actualWindowSize;
   }

   public int getProposedWindowSize() {
      return this.proposedWindowSize;
   }

   public void setProposedWindowSize(int proposedWindowSize) {
      this.proposedWindowSize = proposedWindowSize;
   }

   public int getSegmentSize() {
      return this.segmentSize;
   }

   public void setSegmentSize(int segmentSize) {
      this.segmentSize = segmentSize;
   }

   public int getNumSegments() {
      return this.numSegments;
   }

   public void setNumSegments(int numSegments) {
      this.numSegments = numSegments;
   }

   public int getSegmentCounter() {
      return this.segmentCounter;
   }

   public void setSegmentCounter(int segmentCounter) {
      this.segmentCounter = segmentCounter;
   }

   public int getTimerId() {
      return this.timerId;
   }

   public int getState() {
      return this.state;
   }

   public void setState(int newState) {
      if (newState != 0) {
         if (this.state == 0) {
            this.activeTransactions().add(this);
         }
      } else if (newState == 0) {
         this.activeTransactions().remove(this);
      }

      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Setting state from " + this + " --> newState: " + this.getTag(newState));
      }

      this.state = newState;
   }

   protected abstract TransactionList activeTransactions();

   protected abstract String getTag(int var1);

   public abstract long getHashCode();

   public static final long hash(BBacnetAddress addr, int invokeId) {
      return (long)invokeId << 32 | addr.hash() & 4294967295L;
   }

   private static BBacnetTransportLayer transport() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getTransport();
   }

   public void startRequestTimer(int apduTimeout) {
      this.timerId = requestTimer.start(this, apduTimeout);
   }

   public void restartRequestTimer(int apduTimeout) {
      requestTimer.stop(this.timerId);
      this.timerId = requestTimer.start(this, apduTimeout);
   }

   public void stopRequestTimer() {
      requestTimer.stop(this.timerId);
   }

   public void startSegmentTimer(int apduSegmentTimeout) {
      this.segmentTimerId = segmentTimer.start(this, apduSegmentTimeout);
   }

   public void restartSegmentTimer(int apduSegmentTimeout) {
      segmentTimer.stop(this.segmentTimerId);
      this.segmentTimerId = segmentTimer.start(this, apduSegmentTimeout);
   }

   public void stopSegmentTimer() {
      segmentTimer.stop(this.segmentTimerId);
   }

   public void startSegmentWaitTimer(int apduSegmentTimeout) {
      this.segmentTimerId = segmentTimer.start(this, apduSegmentTimeout * 4);
   }

   public void restartSegmentWaitTimer(int apduSegmentTimeout) {
      segmentTimer.stop(this.segmentTimerId);
      this.segmentTimerId = segmentTimer.start(this, apduSegmentTimeout * 4);
   }

   public static class RequestTimer implements TimerListener {
      public int start(Transaction transaction, int apduTimeout) {
         int timerId = Timers.add(this, apduTimeout, transaction);
         if (Transaction.logger.isLoggable(Level.FINE)) {
            Transaction.logger
               .fine(
                  (transaction.isServerTransaction() ? "Server" : "Client")
                     + " Request Timer started; timerId: "
                     + timerId
                     + ", transaction: "
                     + transaction
                     + ", apdu: "
                     + transaction.getApdu()
               );
         }

         return timerId;
      }

      public void stop(int timerId) {
         Timers.cancel(timerId);
         if (Transaction.logger.isLoggable(Level.FINE)) {
            Transaction.logger.fine("Request Timer stopped; timerId: " + timerId);
         }
      }

      @Override
      public long timerExpired(int timerId, Object userObject) {
         Transaction transaction = (Transaction)userObject;
         if (Transaction.logger.isLoggable(Level.FINE)) {
            Transaction.logger
               .fine(
                  (transaction.isServerTransaction() ? "Server" : "Client")
                     + " Request Timer EXPIRED; timerId: "
                     + timerId
                     + ", transaction: "
                     + transaction
                     + ", apdu: "
                     + transaction.getApdu()
               );
         }

         transaction.requestTimerExpired = true;

         try {
            if (transaction.isServerTransaction()) {
               Transaction.transport().serverRequestTimeout(transaction.getApdu());
            } else {
               Transaction.transport().clientRequestTimeout(transaction.getApdu());
            }

            return 0L;
         } catch (QueueFullException var5) {
            Transaction.logger
               .warning(
                  "Transaction.RequestTimer.timerExpired: QueueFullException: " + var5 + ", transaction: " + transaction + ", apdu: " + transaction.getApdu()
               );
            return -1L;
         }
      }
   }

   public static class SegmentTimer implements TimerListener {
      public int start(Transaction transaction, int apduSegmentTimeout) {
         return Timers.add(this, apduSegmentTimeout, transaction);
      }

      public void stop(int timerId) {
         Timers.cancel(timerId);
      }

      @Override
      public long timerExpired(int timerId, Object userObject) {
         Transaction transaction = (Transaction)userObject;
         transaction.segmentTimerExpired = true;

         try {
            if (transaction.isServerTransaction()) {
               Transaction.transport().serverSegmentTimeout(transaction.getApdu());
            } else {
               Transaction.transport().clientSegmentTimeout(transaction.getApdu());
            }

            return 0L;
         } catch (QueueFullException var5) {
            Transaction.logger
               .warning(
                  "Transaction.SegmentTimer.timerExpired: QueueFullException: " + var5 + ", transaction: " + transaction + ", apdu: " + transaction.getApdu()
               );
            return -1L;
         }
      }
   }
}
