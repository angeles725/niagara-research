package javax.baja.driver.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Stack;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "fastRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(1000)",
      facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1), BFacets.SHOW_MILLISECONDS, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "normalRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(5000)",
      facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1), BFacets.SHOW_MILLISECONDS, BBoolean.TRUE)")}
   ), @NiagaraProperty(
      name = "slowRate",
      type = "BRelTime",
      defaultValue = "BRelTime.make(30000)",
      facets = {@Facet("BFacets.make(BFacets.MIN, BRelTime.make(1), BFacets.SHOW_MILLISECONDS, BBoolean.TRUE)")}
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
   )})
@NiagaraAction(
   name = "resetStatistics",
   flags = 128
)
public abstract class BPollScheduler extends BAbstractPollService implements Runnable {
   public static final Property fastRate = newProperty(0, BRelTime.make(1000L), BFacets.make("min", BRelTime.make(1L), "showMilliseconds", BBoolean.TRUE));
   public static final Property normalRate = newProperty(0, BRelTime.make(5000L), BFacets.make("min", BRelTime.make(1L), "showMilliseconds", BBoolean.TRUE));
   public static final Property slowRate = newProperty(0, BRelTime.make(30000L), BFacets.make("min", BRelTime.make(1L), "showMilliseconds", BBoolean.TRUE));
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
   public static final Action resetStatistics = newAction(128, null);
   public static final Type TYPE = Sys.loadType(BPollScheduler.class);
   static final DecimalFormat timeFormat = new DecimalFormat("0.0#ms");
   boolean isAlive;
   Thread thread;
   long start;
   long totalPollTime;
   int totalPollCount;
   double average;
   Stack<BIPollable> dibs = new Stack<>();
   int dibsPollTotal;
   int dibsSizeTotal;
   Object lock = new Object();
   int statsCount;
   BPollScheduler.Bucket fast = new BPollScheduler.Bucket(fastRate);
   BPollScheduler.Bucket norm = new BPollScheduler.Bucket(normalRate);
   BPollScheduler.Bucket slow = new BPollScheduler.Bucket(slowRate);

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

   public void resetStatistics() {
      this.invoke(resetStatistics, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public final void poll(BIPollable p) {
      long t1 = Clock.ticks();

      try {
         this.doPoll(p);
      } catch (Throwable var6) {
         var6.printStackTrace();
      }

      long t2 = Clock.ticks();
      this.totalPollCount++;
      this.totalPollTime += t2 - t1;
      this.average = (double)this.totalPollTime / this.totalPollCount;
   }

   public abstract void doPoll(BIPollable var1) throws Exception;

   public void started() {
      if (Sys.isStationStarted()) {
         this.startThread();
      }
   }

   public void stationStarted() throws Exception {
      super.stationStarted();
      this.startThread();
   }

   public void stopped() {
      this.stopThread();
   }

   public void startThread() {
      this.doResetStatistics();
      this.stopThread();
      this.isAlive = true;
      this.thread = new Thread(this, "Poll:" + this.getParent().getName());
      this.thread.start();
   }

   public void stopThread() {
      this.isAlive = false;
      if (this.thread != null) {
         this.thread.interrupt();
      }
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
            var6.printStackTrace();
         }
      }
   }

   private void pollDibs() {
      while (true) {
         BIPollable next = null;
         synchronized (this.lock) {
            if (this.dibs.empty()) {
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

   private void pollQueue(BPollScheduler.Bucket bucket) {
      if (bucket.nextTicks <= Clock.ticks() + 5L) {
         BIPollable p = null;
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

         long lastPollTime = 0L;
         if (p != null) {
            long t1 = Clock.ticks();
            this.poll(p);
            lastPollTime = Clock.ticks() - t1;
            bucket.pollTotal++;
         }

         long rate = ((BRelTime)this.get(bucket.rateProp)).getMillis();
         long sleep;
         if (size > 0) {
            double dRate = rate;
            double dSize = size;
            sleep = (long)((dRate - dSize * lastPollTime) / dSize);
         } else {
            sleep = 1000L;
         }

         bucket.nextTicks = Clock.ticks() + sleep;
      }
   }

   private long computeSleep() {
      long now = Clock.ticks();
      long sleep = 1000L;
      sleep = Math.min(this.fast.nextTicks - now, sleep);
      sleep = Math.min(this.norm.nextTicks - now, sleep);
      return Math.min(this.slow.nextTicks - now, sleep);
   }

   private void checkBucketConfig() {
      synchronized (this.lock) {
         ArrayList<BIPollable> newFast = new ArrayList<>();
         ArrayList<BIPollable> newNorm = new ArrayList<>();
         ArrayList<BIPollable> newSlow = new ArrayList<>();
         reSort(this.fast.q, newFast, newNorm, newSlow);
         reSort(this.norm.q, newFast, newNorm, newSlow);
         reSort(this.slow.q, newFast, newNorm, newSlow);
         this.fast.q = newFast;
         this.norm.q = newNorm;
         this.slow.q = newSlow;
      }
   }

   private static void reSort(ArrayList<BIPollable> orig, ArrayList<BIPollable> newFast, ArrayList<BIPollable> newNorm, ArrayList<BIPollable> newSlow) {
      int size = orig.size();

      for (int i = 0; i < size; i++) {
         BIPollable p = orig.get(i);
         switch (p.getPollFrequency().getOrdinal()) {
            case 0:
               newFast.add(p);
               break;
            case 1:
               newNorm.add(p);
               break;
            case 2:
               newSlow.add(p);
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
      this.setStatisticsStart(Clock.time());
      this.updateStats();
   }

   private void updateStats() {
      long now = Clock.ticks();
      long uptime = now - this.start;
      this.statsCount++;
      this.setAveragePoll(timeFormat.format(this.average));
      if (this.totalPollTime > 0L) {
         this.setBusyTime(
            "" + (int)(100.0 * ((double)this.totalPollTime / uptime)) + "% (" + this.duration(this.totalPollTime) + "/" + this.duration(uptime) + ")"
         );
      }

      this.setTotalPolls("" + this.count(this.totalPollCount) + " over " + this.duration(this.totalPollTime));
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

   private String toPollTotal(int bucketTotal) {
      int total = this.totalPollCount;
      StringBuilder s = new StringBuilder();
      if (total == 0) {
         s.append('-');
      } else {
         s.append((int)(100.0 * bucketTotal / total));
      }

      s.append("% (").append(this.count(bucketTotal)).append('/').append(this.count(total)).append(')');
      return s.toString();
   }

   private String toCount(int current, int total) {
      return "current=" + current + " average=" + total / this.statsCount;
   }

   private String toCycle(BPollScheduler.Bucket bucket, long uptime) {
      int cycles = bucket.cycleTotal;
      return cycles == 0 ? "-" : "average = " + uptime / cycles + "ms";
   }

   private String count(int count) {
      return count < 10000 ? String.valueOf(count) : count / 1000 + "k";
   }

   private String duration(long duration) {
      return duration < 10000L ? duration + "ms" : duration / 1000L + "sec";
   }

   @Override
   public void subscribe(BIPollable p) {
      synchronized (this.lock) {
         this.dibs.push(p);
         switch (p.getPollFrequency().getOrdinal()) {
            case 0:
               this.fast.q.add(p);
               break;
            case 1:
               this.norm.q.add(p);
               break;
            case 2:
               this.slow.q.add(p);
               break;
            default:
               throw new IllegalStateException();
         }
      }
   }

   @Override
   public boolean unsubscribe(BIPollable p) {
      synchronized (this.lock) {
         for (int i = 0; i < this.dibs.size(); i++) {
            if (this.dibs.get(i) == p) {
               this.dibs.remove(i);
               break;
            }
         }

         if (this.fast.remove(p)) {
            return true;
         } else {
            return this.norm.remove(p) ? true : this.slow.remove(p);
         }
      }
   }

   static class Bucket {
      Property rateProp;
      ArrayList<BIPollable> q = new ArrayList<>();
      int index;
      long nextTicks;
      int pollTotal;
      int sizeTotal;
      int cycleTotal;

      Bucket(Property rateProp) {
         this.rateProp = rateProp;
      }

      boolean remove(BIPollable p) {
         int size = this.q.size();

         for (int i = 0; i < size; i++) {
            if (this.q.get(i) == p) {
               this.q.remove(i);
               return true;
            }
         }

         return false;
      }

      void reset() {
         this.index = 0;
         this.nextTicks = 0L;
         this.pollTotal = 0;
         this.sizeTotal = 0;
         this.cycleTotal = 0;
      }
   }
}
