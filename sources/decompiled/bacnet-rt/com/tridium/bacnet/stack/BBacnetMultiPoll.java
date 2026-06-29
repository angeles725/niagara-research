package com.tridium.bacnet.stack;

import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import java.text.DecimalFormat;
import java.util.logging.Level;
import javax.baja.bacnet.util.PollList;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BLong;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "maxDibsPolls",
      type = "int",
      defaultValue = "5",
      flags = 4
   ), @NiagaraProperty(
      name = "numberOfThreads",
      type = "int",
      defaultValue = "2",
      flags = 1
   )})
public class BBacnetMultiPoll extends BBacnetPoll {
   public static final Property maxDibsPolls = newProperty(4, 5, null);
   public static final Property numberOfThreads = newProperty(1, 2, null);
   public static final Type TYPE = Sys.loadType(BBacnetMultiPoll.class);
   static final DecimalFormat avgCntFormat = new DecimalFormat("0.0#");
   private static final boolean RECORD_POLL_STATISTICS = Boolean.valueOf(System.getProperty("niagara.bacnet.poll.statistics", "true"));
   long stats;
   MultiPollThread[] pollThreads;
   private int dibsPolls = 0;
   private long sleep;
   private BBacnetPoll.BacnetBucket nextBucket;

   public int getMaxDibsPolls() {
      return this.getInt(maxDibsPolls);
   }

   public void setMaxDibsPolls(int v) {
      this.setInt(maxDibsPolls, v, null);
   }

   @Override
   public int getNumberOfThreads() {
      return this.getInt(numberOfThreads);
   }

   public void setNumberOfThreads(int v) {
      this.setInt(numberOfThreads, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context cx) {
      return this.isRunning() ? "BacnetMultiPoll in " + this.getParent().getName() : "BacnetMultiPoll";
   }

   @Override
   public void pollStart() {
      synchronized (this.lock) {
         this.initializeBuckets();
         this.fast.nextBucket = this.norm;
         this.norm.nextBucket = this.slow;
         this.slow.nextBucket = this.fast;
         this.nextBucket = this.fast;
         int numThds = this.getNumberOfThreads();
         this.pollStop();
         this.isAlive = true;
         this.pollThreads = new MultiPollThread[numThds];
         this.dibsPolls = 0;
         this.doResetStatistics();
         this.stats = Clock.ticks() + 10000L;

         for (int i = 0; i < numThds; i++) {
            this.pollThreads[i] = new MultiPollThread(this, "BnMP" + ((BNetworkPort)this.getParent()).getPortId() + ":" + this.getParent().getName() + ":", i);
            this.pollThreads[i].start();
         }
      }
   }

   @Override
   public void pollStop() {
      this.isAlive = false;
      if (this.pollThreads != null) {
         for (int i = 0; i < this.pollThreads.length; i++) {
            if (this.pollThreads[i] != null) {
               this.pollThreads[i].stop();
               this.pollThreads[i] = null;
            }
         }

         this.pollThreads = null;
      }
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(numberOfThreads) && this.isAlive) {
            this.pollStop();
            ((BBacnetNetworkLayer)this.getParent().getParent()).initializeAsnInputPool();
            synchronized (this.lock) {
               this.fast.resetDone();
               this.norm.resetDone();
               this.slow.resetDone();
            }

            this.pollStart();
         }
      }
   }

   public PollList next() {
      PollList next = null;
      synchronized (this.lock) {
         if (!this.dibs.isEmpty() && this.dibsPolls < this.getMaxDibsPolls()) {
            next = this.dibs.pop();
            this.dibsPollTotal++;
            this.dibsPolls++;
            next.setSleep(this.computeSleep());
            return next;
         } else {
            this.dibsPolls = 0;
            next = this.checkBuckets();
            if (updateStats(this.stats)) {
               this.checkBucketConfig();
               this.updateStats();
               this.stats = Clock.ticks() + 10000L;
            }

            if (next != null) {
               next.setSleep(this.computeSleep());
            }

            return next;
         }
      }
   }

   private static boolean updateStats(long stats) {
      return RECORD_POLL_STATISTICS && Clock.ticks() > stats;
   }

   private PollList checkBuckets() {
      synchronized (this.lock) {
         for (int i = 0; i < 3; i++) {
            PollList next = this.checkBucket(this.nextBucket, false);
            if (next != null) {
               return next;
            }
         }

         for (int ix = 0; ix < 3; ix++) {
            PollList next = this.checkBucket(this.nextBucket, true);
            if (next != null) {
               return next;
            }
         }

         return null;
      }
   }

   private PollList checkBucket(BBacnetPoll.BacnetBucket bucket, boolean isBusyOk) {
      synchronized (this.lock) {
         this.nextBucket = bucket.nextBucket;
         if (bucket.nextTicks > Clock.ticks() + 5L) {
            return null;
         } else {
            PollList pl = null;
            int index = bucket.index;
            int size = bucket.q.size();
            if (size > 0) {
               if (index >= size) {
                  index = 0;
               }

               for (int nodesChecked = 0; nodesChecked++ < size; index = (index + 1) % size) {
                  PollList p1 = bucket.q.get(index);
                  if (!p1.isPolling() && (isBusyOk || !p1.getDevice().isPolling()) && !p1.isDone()) {
                     pl = p1;
                     bucket.index = index + 1;
                     break;
                  }
               }

               if (bucket.donePolling()) {
                  if (this.logger.isLoggable(Level.FINE)) {
                     this.logger.fine("Reset bucket " + bucket + " on thd " + Thread.currentThread().getName());
                  }

                  bucket.cycleTotal++;
                  bucket.pollCount = 0;
                  bucket.resetDone();
                  if (this.getTimeToRepack()) {
                     if (this.logger.isLoggable(Level.FINE)) {
                        this.logger.fine("Repacking bucket: " + bucket);
                     }

                     this.setLastRepackTime(Clock.ticks());
                     this.initializeBuckets();
                  }
               }
            } else {
               bucket.cycleTotal++;
            }

            if (pl != null) {
               bucket.pollCount++;
               bucket.pollTotal++;
               pl.setIsPolling(true);
               pl.setDone(true);
            }

            long rate = ((BRelTime)this.get(bucket.rateProp)).getMillis();
            long slep;
            if (size > 0) {
               double dRate = rate;
               double dSize = size;
               slep = (long)((dRate - dSize * this.lastPollTime) / dSize);
            } else {
               slep = 1000L;
            }

            bucket.nextTicks = Clock.ticks() + slep;
            return pl;
         }
      }
   }

   @Override
   protected long computeSleep() {
      synchronized (this.lock) {
         if (!this.dibs.isEmpty()) {
            return 0L;
         } else {
            long now = Clock.ticks();
            long fnt = this.fast.nextTicks - now;
            long nnt = this.norm.nextTicks - now;
            long snt = this.slow.nextTicks - now;
            this.sleep = 10000L;
            if (fnt < this.sleep) {
               this.sleep = fnt;
               this.nextBucket = this.fast;
            }

            if (nnt < this.sleep) {
               this.sleep = nnt;
               this.nextBucket = this.norm;
            }

            if (snt < this.sleep) {
               this.sleep = snt;
               this.nextBucket = this.slow;
            }

            return this.sleep;
         }
      }
   }

   @Override
   public void doResetStatistics() {
      this.totalPollTime = 0L;
      this.totalPollCount = 0;
      this.average = 0.0;
      this.statsCount = 0;
      this.dibsPollTotal = 0;
      this.dibsSizeTotal = 0;
      synchronized (this.lock) {
         this.fast.reset();
         this.norm.reset();
         this.slow.reset();
         this.start = Clock.ticks();
         this.setStatisticsStart(BAbsTime.make());
         this.updateStats();
      }
   }

   @Override
   protected String toCount(int current, int total) {
      return "current=" + current + " average=" + avgCntFormat.format((double)total / this.statsCount);
   }

   @Override
   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetMultiPoll", 2);
      out.prop("dibsPolls", this.dibsPolls);
      out.prop("stats", BLong.make(this.stats));
      out.prop("sleep", BLong.make(this.sleep));
      out.prop("nextBucket", this.nextBucket);
      if (this.isAlive) {
         out.prop("pollThreads", this.pollThreads.length);

         for (int i = 0; i < this.pollThreads.length; i++) {
            out.prop("  " + i, this.pollThreads[i].getName());
            this.pollThreads[i].spyImpl(out);
         }
      }

      out.endProps();
   }
}
