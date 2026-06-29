package javax.baja.bacnet.util;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.nre.util.Array;

public class PollList {
   private final AtomicInteger failedCount = new AtomicInteger(0);
   public final AtomicInteger pollCount = new AtomicInteger(0);
   int addressHash = 0;
   int dataSize = 0;
   BBacnetDevice device = null;
   Array<PollListEntry> entries = new Array(PollListEntry.class);
   private volatile boolean polling = false;
   private volatile boolean done = false;
   private long sleep = 0L;
   private static final int OBJECT_ID_SIZE = 5;

   private PollList() {
   }

   public PollList(PollListEntry ple) {
      this.addressHash = ple.getAddressHash();
      this.dataSize = ple.getDataSize();
      this.device = ple.getDevice();
      this.add(ple);
   }

   public synchronized void add(PollListEntry ple) {
      if (ple != null) {
         if (ple.getDevice() != this.device) {
            throw new IllegalArgumentException("PollList.add:ple device (" + ple.getDevice() + ") != pl device (" + this.device + ")");
         } else {
            ple.setPollList(this);
            this.dataSize = this.dataSize + ple.getDataSize();
            Iterator<PollListEntry> it = this.entries.iterator();
            boolean added = false;
            int hc = ple.getObjectId().hashCode();

            for (int ndx = 0; it.hasNext(); ndx++) {
               if (hc == it.next().getObjectId().hashCode()) {
                  this.entries.add(ndx, ple);
                  added = true;
                  break;
               }
            }

            if (!added) {
               this.entries.add(ple);
               this.dataSize += 5;
            }
         }
      }
   }

   public synchronized boolean remove(PollListEntry ple) {
      if (ple == null) {
         return false;
      } else if (ple.getDevice() != this.device) {
         throw new IllegalArgumentException("PollList.remove:ple device (" + ple.getDevice() + ") != pl device (" + this.device + ")");
      } else {
         int hc = ple.getObjectId().hashCode();
         int plen = this.entries.size();
         int ndx = this.entries.indexOf(ple);
         if (ndx >= 0) {
            this.entries.remove(ndx);
            this.dataSize = this.dataSize - ple.getDataSize();
            boolean foundAnother = false;
            if (ndx > 0 && ((PollListEntry)this.entries.get(ndx - 1)).getObjectId().hashCode() == hc) {
               foundAnother = true;
            }

            if (!foundAnother && ndx < plen - 1 && ((PollListEntry)this.entries.get(ndx)).getObjectId().hashCode() == hc) {
               foundAnother = true;
            }

            if (!foundAnother) {
               this.dataSize -= 5;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public synchronized int size() {
      return this.entries.size();
   }

   public final int getAddressHash() {
      return this.addressHash;
   }

   public final int getDataSize() {
      return this.dataSize;
   }

   public final BBacnetDevice getDevice() {
      return this.device;
   }

   public synchronized PollListEntry[] getPollEntries() {
      return (PollListEntry[])this.entries.trim();
   }

   public final synchronized boolean contains(PollListEntry ple) {
      return this.entries.contains(ple);
   }

   @Override
   public String toString() {
      return this.string(false);
   }

   public String debug() {
      return this.string(true);
   }

   private String string(boolean debug) {
      StringBuilder sb = new StringBuilder(this.pollCount.get() + " PollList [");
      if (this.device != null) {
         sb.append(this.device.getName()).append(' ').append(this.device.getObjectId()).append(' ').append(this.device.getAddress());
      }

      sb.append("] ").append(Integer.toHexString(this.addressHash)).append(" {").append(this.dataSize).append("} ");
      sb.append(this.isPolling() ? "P" : "-");
      sb.append(this.isDone() ? "D " : "- ");
      sb.append(" [").append(this.getFailedCount()).append("] ");
      int sz = this.entries.size();
      sb.append(sz).append(sz == 1 ? " PLE" : " PLEs");
      if (debug) {
         sb.append("\n");
         Iterator<PollListEntry> it = this.entries.iterator();

         while (it.hasNext()) {
            sb.append("  " + it.next().debugString() + "\n");
         }
      }

      return sb.toString();
   }

   public synchronized int getPollFrequency() {
      return this.entries.size() > 0 ? ((PollListEntry)this.entries.get(0)).getPollable().getPollFrequency().getOrdinal() : -1;
   }

   public void setIsPolling(boolean polling) {
      this.polling = polling;
   }

   public boolean isPolling() {
      return this.polling;
   }

   public void setDone(boolean done) {
      this.done = done;
   }

   public boolean isDone() {
      return this.done;
   }

   public long getSleep() {
      return this.sleep;
   }

   public void setSleep(long sleep) {
      this.sleep = sleep;
   }

   public int getFailedCount() {
      return this.failedCount.get();
   }

   public void incrementFailedCount() {
      this.failedCount.getAndIncrement();
   }

   public void resetFailedCount() {
      this.failedCount.set(0);
   }
}
