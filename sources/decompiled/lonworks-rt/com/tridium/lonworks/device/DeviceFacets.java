package com.tridium.lonworks.device;

import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.TimedCoalesceQueue;
import com.tridium.lonworks.xml.XDeviceFacets;
import javax.baja.data.BIDataValue;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.util.Invocation;

public class DeviceFacets {
   private int delayToReset = 0;
   private int delayToHardOffline = 0;
   private int minNvUpdateInterMsgDelay = 0;
   private boolean disableSetOfflineInBind = false;
   private boolean disableToggleMode = false;
   private int nodeObjectIndex;
   private long lastNvWriteTime = 0L;
   private int delayCnt = 0;

   public static int getNodeObjectIndex(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.nodeObjectIndex : 0;
   }

   public static int getDelayToReset(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.delayToReset : 0;
   }

   public static void delayToReset(BLonDevice dev) {
      int delay = getDelayToReset(dev);
      if (delay > 0) {
         NmUtil.wait(delay);
      }
   }

   public static int getDelayToHardOffline(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.delayToHardOffline : 0;
   }

   public static void delayToHardOffline(BLonDevice dev) {
      int delay = getDelayToHardOffline(dev);
      if (delay > 0) {
         NmUtil.wait(delay);
      }
   }

   public static int getMinNvUpdateInterMsgDelay(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.minNvUpdateInterMsgDelay : 0;
   }

   public static boolean delayNvUpdate(BLonDevice dev, BNetworkVariable nv) {
      DeviceFacets p = getPickle(dev);
      int minUpdateTime = p != null ? p.minNvUpdateInterMsgDelay : 0;
      int writeDelay = nv.getWriteDelay();
      if (minUpdateTime == 0 && writeDelay == 0) {
         return false;
      } else {
         Object sync = p != null ? p : nv;
         synchronized (sync) {
            long currentTime = Clock.ticks();
            long nextWriteTime = currentTime;
            if (p != null && minUpdateTime > 0) {
               nextWriteTime = p.lastNvWriteTime + minUpdateTime * (p.delayCnt + 1);
            }

            if (writeDelay > 0) {
               long delayedTime = currentTime + writeDelay;
               if (delayedTime > nextWriteTime) {
                  nextWriteTime = delayedTime;
               }
            }

            label58:
            if (currentTime < nextWriteTime) {
               TimedCoalesceQueue q = (TimedCoalesceQueue)dev.lonNetwork().getTimedQueue().getTodo();
               DeviceFacets.TimedInvocation invoc = new DeviceFacets.TimedInvocation(nv, BNetworkVariable.forceWrite, p, nextWriteTime);

               boolean var10000;
               try {
                  if (q.enqueue(invoc) && p != null) {
                     p.delayCnt++;
                  }

                  var10000 = true;
               } catch (Throwable var15) {
                  System.out.println("in delayNvUpdate could not queue TimedInvocation " + var15);
                  break label58;
               }

               return var10000;
            }

            if (p != null) {
               p.lastNvWriteTime = Clock.ticks();
            }

            return false;
         }
      }
   }

   public static boolean disableToggleMode(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.disableToggleMode : false;
   }

   static void delayedWrite(BNetworkVariable nv, DeviceFacets p) {
      Object sync = p != null ? p : nv;
      synchronized (sync) {
         nv.doForceWrite();
         if (p != null) {
            p.delayCnt--;
            p.lastNvWriteTime = Clock.ticks();
         }
      }
   }

   public static boolean getDisableSetOfflineInBind(BLonDevice dev) {
      DeviceFacets p = getPickle(dev);
      return p != null ? p.disableSetOfflineInBind : false;
   }

   private static DeviceFacets getPickle(BLonDevice dev) {
      BDeviceData dd = dev.getDeviceData();
      BFacets f = dd.getFacets();
      if (BFacets.NULL == f) {
         return null;
      } else {
         Object o = dd.getPickle();
         if (o != null) {
            return (DeviceFacets)o;
         } else {
            DeviceFacets df = new DeviceFacets();
            df.delayToReset = f.geti("delayToReset", 0);
            df.delayToHardOffline = f.geti("delayToHardOffline", 0);
            df.minNvUpdateInterMsgDelay = f.geti("minNvUpdateInterMsgDelay", 0);
            df.disableSetOfflineInBind = f.getb("disableSetOfflineInBind", false);
            df.disableToggleMode = f.getb("disableToggleMode", false);
            df.nodeObjectIndex = f.geti("nodeObjectIndex", 0);
            dd.setPickle(df);
            return df;
         }
      }
   }

   public static void moveDeviceFacets(BLonDevice dev) {
      if (dev.getDeviceData().getFacets() == BFacets.NULL) {
         Property prop = dev.getPropertyInParent();
         if (prop != null) {
            BFacets f = prop.getFacets();
            if (f != BFacets.NULL) {
               dev.getDeviceData().setFacets(f);
               ((BComponent)dev.getParent()).setFacets(dev.getPropertyInParent(), BFacets.NULL, null);
            }
         }
      }
   }

   public static BFacets makeDeviceFacets(XDeviceFacets xdevFacets) {
      Array<String> keyA = new Array(String.class);
      Array<BIDataValue> valueA = new Array(BIDataValue.class);
      if (xdevFacets.delayToReset > 0) {
         keyA.add("delayToReset");
         valueA.add(BInteger.make(xdevFacets.delayToReset));
      }

      if (xdevFacets.delayToHardOffline > 0) {
         keyA.add("delayToHardOffline");
         valueA.add(BInteger.make(xdevFacets.delayToHardOffline));
      }

      if (xdevFacets.minNvUpdateInterMsgDelay > 0) {
         keyA.add("minNvUpdateInterMsgDelay");
         valueA.add(BInteger.make(xdevFacets.minNvUpdateInterMsgDelay));
      }

      if (xdevFacets.disableSetOfflineInBind) {
         keyA.add("disableSetOfflineInBind");
         valueA.add(BBoolean.make(xdevFacets.disableSetOfflineInBind));
      }

      if (xdevFacets.disableToggleMode) {
         keyA.add("disableToggleMode");
         valueA.add(BBoolean.make(xdevFacets.disableToggleMode));
      }

      if (xdevFacets.nodeObjectIndex > 0) {
         keyA.add("nodeObjectIndex");
         valueA.add(BInteger.make(xdevFacets.nodeObjectIndex));
      }

      if (keyA.size() == 0) {
         return null;
      } else {
         String[] keys = (String[])keyA.trim();
         BIDataValue[] values = (BIDataValue[])valueA.trim();
         return BFacets.make(keys, values);
      }
   }

   public static void spy(BLonDevice dev, SpyWriter out) throws Exception {
      DeviceFacets df = getPickle(dev);
      if (df != null) {
         out.startProps("DeviceFacets");
         out.prop("DelayToReset", df.delayToReset);
         out.prop("DelayToHardOffline", df.delayToHardOffline);
         out.prop("MinNvUpdateInterMsgDelay", df.minNvUpdateInterMsgDelay);
         out.prop("DisableSetOfflineInBind", df.disableSetOfflineInBind);
         out.prop("disableToggleMode", df.disableToggleMode);
         out.prop("nodeObjectIndex", df.nodeObjectIndex);
         out.prop("ticks since last NvWrite", Long.toString(Clock.ticks() - df.lastNvWriteTime));
         out.prop("DelayCnt", df.delayCnt);
         out.endProps();
      }
   }

   public static class TimedInvocation extends Invocation implements TimedCoalesceQueue.ITimed {
      DeviceFacets devFacets;
      long invocationTime;

      public TimedInvocation(BComponent instance, Action action, DeviceFacets df, long time) {
         super(instance, action, null, null);
         this.devFacets = df;
         this.invocationTime = time;
      }

      public void run() {
         DeviceFacets.delayedWrite((BNetworkVariable)this.instance, this.devFacets);
      }

      @Override
      public boolean equals(Object object) {
         return super.equals(object);
      }

      @Override
      public long getTime() {
         return this.invocationTime;
      }

      public String getName() {
         return this.instance.getParent().getDisplayName(null) + ":" + this.instance.getDisplayName(null);
      }
   }
}
