package com.tridium.bacnet.stack;

import com.tridium.bacnet.stack.network.BNetworkPort;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.util.BIBacnetPollable;
import javax.baja.bacnet.util.PollList;
import javax.baja.bacnet.util.PollListEntry;
import javax.baja.driver.util.BAbstractPollService;
import javax.baja.driver.util.BIPollable;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "fastRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(1000)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "normalRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(5000)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "slowRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(30000)",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BRelTime.make(1)"
      )}
   ), @NiagaraProperty(
      name = "statisticsStart",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "averagePoll",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "busyTime",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "totalPolls",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "dibsPolls",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "fastPolls",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "normalPolls",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "slowPolls",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "dibsCount",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "fastCount",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "normalCount",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "slowCount",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "fastCycleTime",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "normalCycleTime",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "slowCycleTime",
      type = "String",
      defaultValue = "-",
      flags = 3
   ), @NiagaraProperty(
      name = "deviceCount",
      type = "int",
      defaultValue = "0",
      flags = 7
   ), @NiagaraProperty(
      name = "pointCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "objectCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   ), @NiagaraProperty(
      name = "virtualCount",
      type = "int",
      defaultValue = "0",
      flags = 3
   )})
@NiagaraActions({@NiagaraAction(
      name = "resetStatistics",
      flags = 128
   ), @NiagaraAction(
      name = "rebuildPollLists",
      flags = 128
   )})
public class BBacnetPoll extends BAbstractPollService implements Runnable {
   public static final Property fastRate = newProperty(0, BRelTime.make(1000L), BFacets.make("min", BRelTime.make(1L)));
   public static final Property normalRate = newProperty(0, BRelTime.make(5000L), BFacets.make("min", BRelTime.make(1L)));
   public static final Property slowRate = newProperty(0, BRelTime.make(30000L), BFacets.make("min", BRelTime.make(1L)));
   public static final Property statisticsStart = newProperty(3, BAbsTime.NULL, null);
   public static final Property averagePoll = newProperty(3, "-", null);
   public static final Property busyTime = newProperty(3, "-", null);
   public static final Property totalPolls = newProperty(3, "-", null);
   public static final Property dibsPolls = newProperty(3, "-", null);
   public static final Property fastPolls = newProperty(3, "-", null);
   public static final Property normalPolls = newProperty(3, "-", null);
   public static final Property slowPolls = newProperty(3, "-", null);
   public static final Property dibsCount = newProperty(3, "-", null);
   public static final Property fastCount = newProperty(3, "-", null);
   public static final Property normalCount = newProperty(3, "-", null);
   public static final Property slowCount = newProperty(3, "-", null);
   public static final Property fastCycleTime = newProperty(3, "-", null);
   public static final Property normalCycleTime = newProperty(3, "-", null);
   public static final Property slowCycleTime = newProperty(3, "-", null);
   public static final Property deviceCount = newProperty(7, 0, null);
   public static final Property pointCount = newProperty(3, 0, null);
   public static final Property objectCount = newProperty(3, 0, null);
   public static final Property virtualCount = newProperty(3, 0, null);
   public static final Action resetStatistics = newAction(128, null);
   public static final Action rebuildPollLists = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BBacnetPoll.class);
   protected static final DecimalFormat timeFormat = new DecimalFormat("0.0#ms");
   boolean isAlive;
   Thread thread;
   protected long start;
   protected long lastPollTime;
   protected long totalPollTime;
   protected int totalPollCount;
   protected double average;
   protected Stack<PollList> dibs = new Stack<>();
   protected int dibsPollTotal;
   protected int dibsSizeTotal;
   protected final Object lock = new Object();
   protected int statsCount;
   protected BBacnetPoll.BacnetBucket fast;
   protected BBacnetPoll.BacnetBucket norm;
   protected BBacnetPoll.BacnetBucket slow;
   private BBacnetPollOrder pollOrder = null;
   protected long subTime = 0L;
   protected int subs = 0;
   protected long unsubTime = 0L;
   protected int unsubs = 0;
   protected long getPLEsSubTime = 0L;
   protected long getPLEsUnsubTime = 0L;
   protected long dibsSubTime = 0L;
   protected long pleAddTime = 0L;
   protected int pleAdds = 0;
   protected int pleRems = 0;
   protected long dibsUnsubTime = 0L;
   protected long pleRemTime = 0L;
   private int splitCount = 0;
   public static final String SPLIT_COUNT = "splitOnFailure";
   private long lastRepackTime = -1L;
   private long repackInterval = -1L;
   public static final String REPACK_INTERVAL = "repackInterval";
   protected Logger logger = Logger.getLogger("bacnet.point");
   private static final int DOUBLE_PLE_SIZE = -1;

   public BRelTime getFastRate() {
      return (BRelTime)this.get(fastRate);
   }

   public void setFastRate(BRelTime v) {
      this.set(fastRate, v, null);
   }

   public BRelTime getNormalRate() {
      return (BRelTime)this.get(normalRate);
   }

   public void setNormalRate(BRelTime v) {
      this.set(normalRate, v, null);
   }

   public BRelTime getSlowRate() {
      return (BRelTime)this.get(slowRate);
   }

   public void setSlowRate(BRelTime v) {
      this.set(slowRate, v, null);
   }

   public BAbsTime getStatisticsStart() {
      return (BAbsTime)this.get(statisticsStart);
   }

   public void setStatisticsStart(BAbsTime v) {
      this.set(statisticsStart, v, null);
   }

   public String getAveragePoll() {
      return this.getString(averagePoll);
   }

   public void setAveragePoll(String v) {
      this.setString(averagePoll, v, null);
   }

   public String getBusyTime() {
      return this.getString(busyTime);
   }

   public void setBusyTime(String v) {
      this.setString(busyTime, v, null);
   }

   public String getTotalPolls() {
      return this.getString(totalPolls);
   }

   public void setTotalPolls(String v) {
      this.setString(totalPolls, v, null);
   }

   public String getDibsPolls() {
      return this.getString(dibsPolls);
   }

   public void setDibsPolls(String v) {
      this.setString(dibsPolls, v, null);
   }

   public String getFastPolls() {
      return this.getString(fastPolls);
   }

   public void setFastPolls(String v) {
      this.setString(fastPolls, v, null);
   }

   public String getNormalPolls() {
      return this.getString(normalPolls);
   }

   public void setNormalPolls(String v) {
      this.setString(normalPolls, v, null);
   }

   public String getSlowPolls() {
      return this.getString(slowPolls);
   }

   public void setSlowPolls(String v) {
      this.setString(slowPolls, v, null);
   }

   public String getDibsCount() {
      return this.getString(dibsCount);
   }

   public void setDibsCount(String v) {
      this.setString(dibsCount, v, null);
   }

   public String getFastCount() {
      return this.getString(fastCount);
   }

   public void setFastCount(String v) {
      this.setString(fastCount, v, null);
   }

   public String getNormalCount() {
      return this.getString(normalCount);
   }

   public void setNormalCount(String v) {
      this.setString(normalCount, v, null);
   }

   public String getSlowCount() {
      return this.getString(slowCount);
   }

   public void setSlowCount(String v) {
      this.setString(slowCount, v, null);
   }

   public String getFastCycleTime() {
      return this.getString(fastCycleTime);
   }

   public void setFastCycleTime(String v) {
      this.setString(fastCycleTime, v, null);
   }

   public String getNormalCycleTime() {
      return this.getString(normalCycleTime);
   }

   public void setNormalCycleTime(String v) {
      this.setString(normalCycleTime, v, null);
   }

   public String getSlowCycleTime() {
      return this.getString(slowCycleTime);
   }

   public void setSlowCycleTime(String v) {
      this.setString(slowCycleTime, v, null);
   }

   public int getDeviceCount() {
      return this.getInt(deviceCount);
   }

   public void setDeviceCount(int v) {
      this.setInt(deviceCount, v, null);
   }

   public int getPointCount() {
      return this.getInt(pointCount);
   }

   public void setPointCount(int v) {
      this.setInt(pointCount, v, null);
   }

   public int getObjectCount() {
      return this.getInt(objectCount);
   }

   public void setObjectCount(int v) {
      this.setInt(objectCount, v, null);
   }

   public int getVirtualCount() {
      return this.getInt(virtualCount);
   }

   public void setVirtualCount(int v) {
      this.setInt(virtualCount, v, null);
   }

   public void resetStatistics() {
      this.invoke(resetStatistics, null, null);
   }

   public void rebuildPollLists() {
      this.invoke(rebuildPollLists, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context cx) {
      return this.isRunning() ? "BacnetPoll in " + this.getParent().getName() : "BacnetPoll";
   }

   public void started() throws Exception {
      super.started();
      this.setSplitCount(this.getProperty("splitOnFailure"));
      this.setRepackInterval(this.getProperty("repackInterval"));
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (this.isAlive) {
            if (p.equals(fastRate)) {
               this.fast.resetTicks();
            } else if (p.equals(normalRate)) {
               this.norm.resetTicks();
            } else if (p.equals(slowRate)) {
               this.slow.resetTicks();
            } else if ("splitOnFailure".equals(p.getName())) {
               this.setSplitCount(p);
            } else if ("repackInterval".equals(p.getName())) {
               this.setRepackInterval(p);
            }
         }
      }
   }

   public final boolean isParentLegal(BComponent parent) {
      return parent instanceof BNetworkPort;
   }

   public final void poll(PollList pl) {
      long t1 = Clock.ticks();

      try {
         if (!this.getPollEnabled()) {
            if (this.logger.isLoggable(Level.FINE)) {
               this.logger.fine("BBacnetPoll#poll: skipping disabled poll list: " + (pl != null ? pl.debug() : "null"));
            }

            return;
         }

         BBacnetDevice device = pl.getDevice();
         if (device.isRunning()) {
            boolean pollOk = device.poll(pl);
            if (pollOk) {
               pl.resetFailedCount();
            } else {
               if (this.logger.isLoggable(Level.FINE)) {
                  this.logger.fine("BBacnetPoll#poll: poll failed; split count = " + this.splitCount + "; poll list = " + pl.debug());
               }

               synchronized (this.lock) {
                  if (this.splitCount >= 0) {
                     pl.incrementFailedCount();
                     if (pl.size() > 1 && pl.getFailedCount() > this.splitCount) {
                        if (this.logger.isLoggable(Level.FINE)) {
                           this.logger.fine("Redistributing items in poll list:\n " + pl);
                        }

                        this.redistribute(pl);
                     } else if (this.logger.isLoggable(Level.FINE)) {
                        this.logger.fine("Cannot break down the poll list further:\n " + pl);
                     }
                  }
               }
            }
         } else {
            if (this.logger.isLoggable(Level.FINE)) {
               this.logger.fine("BBacnetPoll#poll: poll list's device is not running; poll list = " + pl.debug());
            }

            try {
               this.getQueue(pl.getPollFrequency()).remove(pl);
            } catch (Exception var8) {
               this.logger
                  .log(Level.SEVERE, "BBacnetPoll#poll: Exception removing poll list from queue for stopped device; poll list = " + pl.debug(), (Throwable)var8);
            }
         }
      } catch (Throwable var10) {
         this.logger.log(Level.SEVERE, "BBacnetPoll#poll: Poll exception on " + pl.debug(), var10);
      }

      this.lastPollTime = Clock.ticks() - t1;
      this.totalPollCount++;
      this.totalPollTime = this.totalPollTime + this.lastPollTime;
      this.average = (double)this.totalPollTime / this.totalPollCount;
   }

   protected void initializeBuckets() {
      synchronized (this.lock) {
         if (this.fast == null) {
            this.fast = new BBacnetPoll.BacnetBucket(fastRate);
         }

         if (this.norm == null) {
            this.norm = new BBacnetPoll.BacnetBucket(normalRate);
         }

         if (this.slow == null) {
            this.slow = new BBacnetPoll.BacnetBucket(slowRate);
         }

         this.rebuildPollBucket(this.getQueue(0));
         this.rebuildPollBucket(this.getQueue(2));
         this.rebuildPollBucket(this.getQueue(1));
      }
   }

   public void pollStart() {
      this.initializeBuckets();
      this.doResetStatistics();
      this.pollStop();
      this.isAlive = true;
      this.thread = new Thread(this, "BacnetPoll" + ((BNetworkPort)this.getParent()).getPortId());
      this.thread.start();
   }

   public void pollStop() {
      this.isAlive = false;
      if (this.thread != null) {
         this.thread.interrupt();
      }
   }

   public int getNumberOfThreads() {
      return 1;
   }

   @Override
   public void run() {
      long stats = Clock.ticks() + 10000L;

      while (this.isAlive) {
         try {
            if (!this.getPollEnabled()) {
               Thread.sleep(1000L);
            } else {
               this.pollDibs();
               this.pollQueues();
               long sleep = this.computeSleep();
               if (sleep > 0L) {
                  Thread.sleep(sleep);
               }

               if (Clock.ticks() > stats) {
                  this.checkBucketConfig();
                  this.updateStats();
                  stats = Clock.ticks() + 10000L;
               }
            }
         } catch (InterruptedException var5) {
         } catch (Throwable var6) {
            this.logger.log(Level.SEVERE, "Poll runnable exception occurred: ", var6);
         }
      }
   }

   protected void pollDibs() {
      while (true) {
         PollList next = null;
         synchronized (this.lock) {
            if (this.dibs.isEmpty()) {
               return;
            }

            next = this.dibs.pop();
            this.dibsPollTotal++;
         }

         this.poll(next);
      }
   }

   private void pollQueues() {
      this.pollQueue(this.fast);
      this.pollQueue(this.norm);
      this.pollQueue(this.slow);
   }

   private void pollQueue(BBacnetPoll.BacnetBucket bucket) {
      if (bucket.nextTicks <= Clock.ticks() + 5L) {
         PollList p = null;
         int size = 0;
         synchronized (this.lock) {
            int index = bucket.index;
            size = bucket.q.size();
            if (size > 0) {
               if (index >= size) {
                  index = 0;
                  bucket.cycleTotal++;
               }

               p = bucket.q.get(index);
               bucket.index = index + 1;
            } else {
               bucket.cycleTotal++;
            }
         }

         if (p != null) {
            this.poll(p);
            bucket.pollTotal++;
         }

         long rate = ((BRelTime)this.get(bucket.rateProp)).getMillis();
         long sleep = rate;
         if (size > 0) {
            double dRate = rate;
            double dSize = size;
            sleep = (long)((dRate - dSize * this.lastPollTime) / dSize);
         }

         bucket.nextTicks = Clock.ticks() + sleep;
      }
   }

   protected long computeSleep() {
      long now = Clock.ticks();
      long sleep = 1000L;
      sleep = Math.min(this.fast.nextTicks - now, sleep);
      sleep = Math.min(this.norm.nextTicks - now, sleep);
      return Math.min(this.slow.nextTicks - now, sleep);
   }

   protected void checkBucketConfig() {
      synchronized (this.lock) {
         if (this.pollFrequencyChanged()) {
            List<PollList> newFast = new ArrayList<>();
            List<PollList> newNorm = new ArrayList<>();
            List<PollList> newSlow = new ArrayList<>();
            reSort(this.fast.q, newFast, newNorm, newSlow);
            reSort(this.norm.q, newFast, newNorm, newSlow);
            reSort(this.slow.q, newFast, newNorm, newSlow);
            this.fast.q = newFast;
            this.norm.q = newNorm;
            this.slow.q = newSlow;
         }
      }
   }

   private boolean pollFrequencyChanged() {
      return pollFrequencyChanged(this.fast, 0) || pollFrequencyChanged(this.norm, 1) || pollFrequencyChanged(this.slow, 2);
   }

   private static boolean pollFrequencyChanged(BBacnetPoll.BacnetBucket bucket, int pollFrequency) {
      List<PollList> pollLists = bucket.q;

      for (int i = 0; i < pollLists.size(); i++) {
         PollList pl = pollLists.get(i);
         if (pollFrequency != pl.getPollFrequency()) {
            return true;
         }
      }

      return false;
   }

   protected static void reSort(List<PollList> orig, List<PollList> newFast, List<PollList> newNorm, List<PollList> newSlow) {
      int size = orig.size();

      for (int i = 0; i < size; i++) {
         PollList pl = orig.get(i);
         switch (pl.getPollFrequency()) {
            case 0:
               newFast.add(pl);
               break;
            case 1:
               newNorm.add(pl);
               break;
            case 2:
               newSlow.add(pl);
               break;
            default:
               throw new IllegalStateException();
         }
      }
   }

   public void doResetStatistics() {
      this.totalPollTime = 0L;
      this.totalPollCount = 0;
      this.average = 0.0;
      this.statsCount = 0;
      this.dibsPollTotal = 0;
      this.dibsSizeTotal = 0;
      this.fast.reset();
      this.norm.reset();
      this.slow.reset();
      this.start = Clock.ticks();
      this.setStatisticsStart(BAbsTime.make());
      this.updateStats();
   }

   public void doRebuildPollLists() {
      synchronized (this.lock) {
         this.rebuildQueue(this.fast.q);
         this.rebuildQueue(this.norm.q);
         this.rebuildQueue(this.slow.q);
      }
   }

   protected void updateStats() {
      long now = Clock.ticks();
      long uptime = now - this.start;
      this.statsCount++;
      this.setAveragePoll(timeFormat.format(this.average));
      if (this.totalPollTime > 0L) {
         double totalPollTimeByThd = (double)this.totalPollTime / this.getNumberOfThreads();
         this.setBusyTime(
            ""
               + (int)(100.0 * totalPollTimeByThd / uptime)
               + "% ("
               + duration(this.totalPollTime)
               + "/"
               + duration(uptime)
               + " over "
               + this.getNumberOfThreads()
               + " thds"
         );
      }

      this.setTotalPolls("" + count(this.totalPollCount) + " over " + duration(this.totalPollTime));
      this.setDibsPolls(this.toPollTotal(this.dibsPollTotal));
      this.setFastPolls(this.toPollTotal(this.fast.pollTotal));
      this.setNormalPolls(this.toPollTotal(this.norm.pollTotal));
      this.setSlowPolls(this.toPollTotal(this.slow.pollTotal));
      this.setDibsCount(this.toCount(this.dibs.size(), this.dibsSizeTotal));
      this.dibsSizeTotal = this.dibsSizeTotal + this.dibs.size();
      this.setFastCount(this.toCount(this.fast.q.size(), this.fast.sizeTotal));
      this.fast.sizeTotal = this.fast.sizeTotal + this.fast.q.size();
      this.setNormalCount(this.toCount(this.norm.q.size(), this.norm.sizeTotal));
      this.norm.sizeTotal = this.norm.sizeTotal + this.norm.q.size();
      this.setSlowCount(this.toCount(this.slow.q.size(), this.slow.sizeTotal));
      this.slow.sizeTotal = this.slow.sizeTotal + this.slow.q.size();
      this.setFastCycleTime(this.toCycle(this.fast, uptime));
      this.setNormalCycleTime(this.toCycle(this.norm, uptime));
      this.setSlowCycleTime(this.toCycle(this.slow, uptime));
   }

   protected String toPollTotal(int bucketTotal) {
      int total = this.totalPollCount;
      StringBuilder s = new StringBuilder();
      if (total == 0) {
         s.append('-');
      } else {
         s.append((int)(100.0 * bucketTotal / total));
      }

      s.append("% (").append(count(bucketTotal)).append('/').append(count(total)).append(')');
      return s.toString();
   }

   protected String toCount(int current, int total) {
      return "current=" + current + " average=" + total / this.statsCount;
   }

   protected String toCycle(BBacnetPoll.BacnetBucket bucket, long uptime) {
      int cycles = bucket.cycleTotal;
      return cycles == 0 ? "-" : "average = " + uptime / cycles + "ms";
   }

   protected static String count(int count) {
      return count < 10000 ? String.valueOf(count) : count / 1000 + "k";
   }

   protected static String duration(long duration) {
      return duration < 10000L ? duration + "ms" : duration / 1000L + "sec";
   }

   public void pollNow(BIBacnetPollable p) {
      if (p.getPollableType() != 0) {
         PollListEntry[] ples = p.getPollListEntries();
         if (ples != null) {
            synchronized (this.lock) {
               for (int i = 0; i < ples.length; i++) {
                  if (!ples[i].getDevice().getStatus().isDown()) {
                     this.add(this.dibs, ples[i], true);
                  }
               }
            }
         }
      }
   }

   public void subscribe(BIPollable p) {
      long t0 = Clock.ticks();
      if (!(p instanceof BIBacnetPollable)) {
         throw new IllegalArgumentException(Lexicon.make("bacnet").getText("IllegalArgumentException.notBacnetPollable"));
      } else if (this.isAlive) {
         BIBacnetPollable bp = (BIBacnetPollable)p;
         long t1 = Clock.ticks();
         this.pollNow(bp);
         long t2 = Clock.ticks();
         long t6 = Clock.ticks();
         PollListEntry[] ples = bp.getPollListEntries();
         long t7 = Clock.ticks();
         long tmp;
         synchronized (this.lock) {
            int pollType = bp.getPollableType();
            switch (pollType) {
               case -1:
               case 4:
               default:
                  break;
               case 0:
                  this.logger.warning("BBacnetPoll.subscribe(device) no longer used!");
                  break;
               case 1:
                  this.setPointCount(this.getPointCount() + 1);
                  break;
               case 2:
                  this.setObjectCount(this.getObjectCount() + 1);
                  break;
               case 3:
                  this.setVirtualCount(this.getVirtualCount() + 1);
                  break;
               case 5:
                  this.setPointCount(this.getPointCount() + 1);
            }

            if (ples == null) {
               return;
            }

            tmp = 0L;

            for (int i = 0; i < ples.length; i++) {
               long t3 = Clock.ticks();
               List<PollList> bucket = this.getQueue(bp.getPollFrequency().getOrdinal());
               this.add(bucket, ples[i], false);
               long t4 = Clock.ticks();
               this.pleAdds++;
               tmp += t4 - t3;
            }
         }

         long t5 = Clock.ticks();
         this.subs++;
         this.subTime += t5 - t0;
         this.dibsSubTime += t2 - t1;
         this.pleAddTime += tmp;
         this.getPLEsSubTime += t7 - t6;
      }
   }

   public boolean unsubscribe(BIPollable p) {
      long t0 = Clock.ticks();
      if (!(p instanceof BIBacnetPollable)) {
         throw new IllegalArgumentException(Lexicon.make("bacnet").getText("IllegalArgumentException.notBacnetPollable"));
      } else if (!this.isAlive) {
         return false;
      } else {
         BIBacnetPollable bp = (BIBacnetPollable)p;
         long t5 = Clock.ticks();
         PollListEntry[] ples = bp.getPollListEntries();
         long t6 = Clock.ticks();
         boolean removed = false;
         long tmp1;
         long tmp2;
         synchronized (this.lock) {
            int pollType = bp.getPollableType();
            if (ples == null) {
               return false;
            }

            tmp1 = 0L;
            tmp2 = 0L;

            for (int i = 0; i < ples.length; i++) {
               boolean pleRemoved = true;
               long t1 = Clock.ticks();
               pleRemoved = this.remove(this.dibs, ples[i]);
               long t2 = Clock.ticks();
               pleRemoved = this.remove(this.fast.q, ples[i]);
               if (!pleRemoved) {
                  pleRemoved = this.remove(this.norm.q, ples[i]);
               }

               if (!pleRemoved) {
                  pleRemoved = this.remove(this.slow.q, ples[i]);
               }

               long t3 = Clock.ticks();
               this.pleRems++;
               tmp1 += t2 - t1;
               tmp2 += t3 - t2;
               removed |= pleRemoved;
            }

            if (removed) {
               switch (pollType) {
                  case -1:
                  case 4:
                  default:
                     break;
                  case 0:
                     this.logger.warning("BBacnetPoll.unsubscribe(device) no longer used!");
                     break;
                  case 1:
                     this.setPointCount(this.getPointCount() - 1);
                     break;
                  case 2:
                     this.setObjectCount(this.getObjectCount() - 1);
                     break;
                  case 3:
                     this.setVirtualCount(this.getVirtualCount() - 1);
                     break;
                  case 5:
                     this.setPointCount(this.getPointCount() - 1);
               }
            }
         }

         long t4 = Clock.ticks();
         this.unsubs++;
         this.dibsUnsubTime += tmp1;
         this.pleRemTime += tmp2;
         this.unsubTime += t4 - t0;
         this.getPLEsUnsubTime += t6 - t5;
         return removed;
      }
   }

   @Deprecated
   public void removePLE(BIBacnetPollable p, PollListEntry ple) {
      synchronized (this.lock) {
         this.remove(this.getQueue(p.getPollFrequency().getOrdinal()), ple);
      }
   }

   private boolean contains(List<PollList> list, PollListEntry ple) {
      synchronized (this.lock) {
         for (PollList pl : list) {
            if (pl.contains(ple)) {
               return true;
            }
         }

         return false;
      }
   }

   private void redistribute(PollList pl) {
      synchronized (this.lock) {
         this.setSize(pl, -1);
         List<PollList> queue = this.getQueue(pl.getPollFrequency());
         queue.remove(pl);
         PollListEntry[] entries = pl.getPollEntries();

         for (int i = 0; i < entries.length; i++) {
            this.addEntryToNewPollList(queue, entries[i]);
         }
      }
   }

   private void rebuildQueue(List<PollList> queue) {
      List<PollListEntry> entryList = new ArrayList<>();

      for (int i = 0; i < queue.size(); i++) {
         PollList pl = queue.get(i);
         PollListEntry[] ples = pl.getPollEntries();

         for (int j = 0; j < ples.length; j++) {
            if (ples[j] != null) {
               entryList.add(ples[j]);
            }
         }
      }

      queue.clear();

      for (int i = 0; i < entryList.size(); i++) {
         PollListEntry entry = entryList.get(i);
         this.add(queue, entry, false);
      }

      this.sortPollList(queue);
   }

   private void rebuildPollBucket(List<PollList> queue) {
      for (int i = 0; i < queue.size(); i++) {
         PollList pl = queue.get(i);
         this.setSize(pl, 11);
      }

      this.rebuildQueue(queue);
   }

   private void setSize(PollList pl, int size) {
      PollListEntry[] entries = pl.getPollEntries();

      for (int i = 0; i < entries.length; i++) {
         PollListEntry entry = entries[i];
         if (entry != null) {
            if (size == -1) {
               entry.doubleDataSize(pl.getDevice().getMaxAPDULengthAccepted());
            } else {
               int newSize = Math.min(pl.getDevice().getMaxAPDULengthAccepted(), size);
               entry.setDataSize(newSize);
            }
         }
      }
   }

   private void addEntryToNewPollList(List<PollList> list, PollListEntry ple) {
      this.add(list, ple, false, true);
   }

   private void add(List<PollList> list, PollListEntry ple, boolean push) {
      this.add(list, ple, push, false);
   }

   private void add(List<PollList> list, PollListEntry ple, boolean push, boolean newList) {
      if (!this.pleExists(list, ple)) {
         synchronized (this.lock) {
            BBacnetDevice device = ple.getDevice();
            Iterator<PollList> it = list.iterator();
            boolean added = false;

            while (!added && it.hasNext()) {
               PollList pl = it.next();
               if (device == pl.getDevice()
                  && (!newList || pl.pollCount.get() == 0)
                  && !pl.isPolling()
                  && pl.getDataSize() + ple.getDataSize() < this.getMaxDataSize(pl)) {
                  pl.add(ple);
                  added = true;
                  break;
               }
            }

            if (!added) {
               PollList pl = new PollList(ple);
               if (push) {
                  ((Stack)list).push(pl);
               } else {
                  list.add(pl);
               }
            }
         }
      }
   }

   private int getMaxDataSize(PollList pl) {
      int maxDataSize = pl.getDevice().getMaxAPDULengthAccepted();
      int myMax = BBacnetNetwork.localDevice().getMaxAPDULengthAccepted();
      if (maxDataSize > myMax) {
         maxDataSize = myMax;
      }

      return maxDataSize - 5;
   }

   private boolean remove(List<PollList> list, PollListEntry ple) {
      synchronized (this.lock) {
         BBacnetDevice device = ple.getDevice();
         boolean removed = false;
         PollList toRemove = null;
         PollList newList = null;

         for (int i = 0; i < list.size(); i++) {
            PollList pl = list.get(i);
            if (device == pl.getDevice() && pl.contains(ple)) {
               PollListEntry[] entries = pl.getPollEntries();
               if (pl.isPolling() || !pl.isDone()) {
                  toRemove = pl;

                  for (int j = 0; j < entries.length; j++) {
                     if (!entries[j].equals(ple)) {
                        if (newList == null) {
                           newList = new PollList(entries[j]);
                        } else {
                           newList.add(entries[j]);
                        }
                     }
                  }
                  break;
               }

               removed = pl.remove(ple);
               if (pl.size() == 0) {
                  toRemove = pl;
               }
               break;
            }
         }

         if (toRemove != null) {
            list.remove(toRemove);
            if (newList != null) {
               newList.setDone(true);
               list.add(newList);
            }

            removed = true;
         }

         return removed;
      }
   }

   private void sortPollList(List<PollList> queue) {
      if (this.pollOrder != null) {
         this.pollOrder.sort(queue);
      }
   }

   private List<PollList> getQueue(int pf) {
      synchronized (this.lock) {
         switch (pf) {
            case 0:
               return this.fast.q;
            case 1:
               return this.norm.q;
            case 2:
               return this.slow.q;
            default:
               return this.norm.q;
         }
      }
   }

   protected void setPollOrder(BBacnetPollOrder newPollOrder) {
      if (newPollOrder == null) {
         this.pollOrder = null;
      } else if (this.pollOrder != newPollOrder) {
         if (this.pollOrder == null && newPollOrder.getEnabled()) {
            this.pollOrder = newPollOrder;
         } else {
            newPollOrder.setEnabled(false);
            newPollOrder.setFaultCause(Lexicon.make("bacnet").getText("BacnetPoll.onlyOnePollOrderingPerPollService"));
         }
      }
   }

   protected int getSplitCount() {
      return this.splitCount;
   }

   private void setSplitCount(Property p) {
      if (p != null) {
         BValue splitCountValue = this.get(p);
         if (splitCountValue != null && splitCountValue instanceof BInteger) {
            this.splitCount = ((BInteger)splitCountValue).getInt();
         }
      }
   }

   protected boolean getTimeToRepack() {
      return this.lastRepackTime != -1L && this.repackInterval != -1L ? Clock.ticks() > this.lastRepackTime + this.repackInterval : false;
   }

   private void setRepackInterval(Property p) {
      synchronized (this.lock) {
         if (p != null) {
            BValue repackValue = this.get(p);
            if (repackValue != null && repackValue instanceof BRelTime) {
               this.repackInterval = ((BRelTime)repackValue).getMillis();
               this.lastRepackTime = Clock.ticks();
            }
         }
      }
   }

   protected void setLastRepackTime(long lastRepackTime) {
      this.lastRepackTime = lastRepackTime;
   }

   private boolean pleExists(List<PollList> list, PollListEntry ple) {
      for (int i = 0; i < list.size(); i++) {
         PollList pl = list.get(i);
         if (pl.contains(ple)) {
            return true;
         }
      }

      return false;
   }

   public void spy(SpyWriter out) throws Exception {
      out.startProps();
      out.prop("subs", BInteger.make(this.subs));
      out.prop("subTime", BLong.make(this.subTime));
      out.prop("subAvg", BDouble.make((double)this.subTime / this.subs));
      out.prop("unsubs", BInteger.make(this.unsubs));
      out.prop("unsubTime", BLong.make(this.unsubTime));
      out.prop("unsubAvg", BDouble.make((double)this.unsubTime / this.unsubs));
      out.prop("getPLEsSubTime", BLong.make(this.getPLEsSubTime));
      out.prop("getPLEsSubAvg", BDouble.make((double)this.getPLEsSubTime / this.subs));
      out.prop("pleAdds", BInteger.make(this.pleAdds));
      out.prop("pleAddTime", BLong.make(this.pleAddTime));
      out.prop("pleAddAvg", BDouble.make((double)this.pleAddTime / this.pleAdds));
      out.prop("dibsSubTime", BLong.make(this.dibsSubTime));
      out.prop("dibsSubAvg", BDouble.make((double)this.dibsSubTime / this.subs));
      out.prop("getPLEsUnsubTime", BLong.make(this.getPLEsUnsubTime));
      out.prop("getPLEsUnsubAvg", BDouble.make((double)this.getPLEsUnsubTime / this.subs));
      out.prop("pleRems", BInteger.make(this.pleRems));
      out.prop("pleRemTime", BLong.make(this.pleRemTime));
      out.prop("pleRemAvg", BDouble.make((double)this.pleRemTime / this.pleRems));
      out.prop("dibsUnsubTime", BLong.make(this.dibsUnsubTime));
      out.prop("dibsUnsubAvg", BDouble.make((double)this.dibsUnsubTime / this.unsubs));
      out.endProps();
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetPoll", 2);
      out.prop("isAlive", this.isAlive);
      out.prop("ticks", BLong.make(Clock.ticks()));
      out.prop("dibs size", this.dibs.size());
      out.prop("statsCount", this.statsCount);
      out.prop("splitCount", BInteger.make(this.splitCount));
      out.prop("lastRepackTime", BLong.make(this.lastRepackTime));
      out.prop("repackInterval", BRelTime.make(this.repackInterval));
      if (this.fast != null) {
         out.prop("fast", this.fast.q.size());
         out.prop("fast.nextTicks", BLong.make(this.fast.nextTicks));
         out.prop("fast.index", this.fast.index);
         out.prop("fast.pollCount", this.fast.pollCount);
         Iterator<PollList> it = this.fast.q.iterator();
         int i = 0;

         while (it.hasNext()) {
            out.prop("fast[" + i++ + "]", it.next().debug());
         }

         out.prop("norm", this.norm.q.size());
         out.prop("norm.nextTicks", BLong.make(this.norm.nextTicks));
         out.prop("norm.index", this.norm.index);
         out.prop("norm.pollCount", this.norm.pollCount);
         it = this.norm.q.iterator();
         i = 0;

         while (it.hasNext()) {
            out.prop("norm[" + i++ + "]", it.next().debug());
         }

         out.prop("slow", this.slow.q.size());
         out.prop("slow.nextTicks", BLong.make(this.slow.nextTicks));
         out.prop("slow.index", this.slow.index);
         out.prop("slow.pollCount", this.slow.pollCount);
         it = this.slow.q.iterator();
         i = 0;

         while (it.hasNext()) {
            out.prop("slow[" + i++ + "]", it.next().debug());
         }
      }

      out.endProps();
   }

   protected static class BacnetBucket {
      public Property rateProp;
      public List<PollList> q = new ArrayList<>();
      public int index;
      public int pollCount;
      public long nextTicks;
      public int pollTotal;
      public int sizeTotal;
      public int cycleTotal;
      public BBacnetPoll.BacnetBucket nextBucket;

      public BacnetBucket(Property rateProp) {
         this.rateProp = rateProp;
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append(this.rateProp.getName()).append('[').append(this.index).append('/').append(this.q.size()).append(']');
         return sb.toString();
      }

      public boolean removeAll(BIBacnetPollable p) {
         int count = 0;
         boolean keepLooking = this.q.size() >= 1;

         boolean found;
         for (found = false; keepLooking; keepLooking = count < this.q.size()) {
            if (this.q.get(count) == p) {
               this.q.remove(count);
               found = true;
            } else {
               count++;
            }
         }

         return found;
      }

      public boolean remove(BIBacnetPollable p) {
         int size = this.q.size();

         for (int i = 0; i < size; i++) {
            if (this.q.get(i) == p) {
               this.q.remove(i);
               return true;
            }
         }

         return false;
      }

      public void reset() {
         this.resetDone();
         this.index = 0;
         this.nextTicks = 0L;
         this.pollTotal = 0;
         this.sizeTotal = 0;
         this.cycleTotal = 0;
         this.pollCount = 0;
      }

      public boolean donePolling() {
         for (int i = 0; i < this.q.size(); i++) {
            PollList pl = this.q.get(i);
            if (pl.isPolling() || !pl.isDone()) {
               return false;
            }
         }

         return true;
      }

      public void resetDone() {
         for (int i = 0; i < this.q.size(); i++) {
            this.q.get(i).setDone(false);
         }
      }

      public void resetTicks() {
         this.nextTicks = 0L;
      }

      public int count(BIBacnetPollable p) {
         int count = 0;
         int size = this.q.size();

         for (int i = 0; i < size; i++) {
            if (this.q.get(i) == p) {
               count++;
            }
         }

         return count;
      }
   }
}
