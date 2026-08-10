package com.tridium.workbench.nav;

import com.tridium.rpc.BNavMonitorRpc;
import com.tridium.sys.Nre;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.baja.nav.BINavNode;
import javax.baja.nav.BNavRoot;
import javax.baja.nav.NavListener;
import javax.baja.space.BISpaceNode;
import javax.baja.spy.Spy;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.ui.table.TableModel;
import javax.baja.ui.tree.TreeNode;
import javax.baja.workbench.nav.tree.BNavTree;
import javax.baja.workbench.nav.tree.NavTreeNode;

public final class NavMonitor extends Thread {
   public static final long SCAN_RATE = AccessController.doPrivileged((PrivilegedAction<Long>)(() -> Long.getLong("niagara.nav.touch.scanRate", 20000L)));
   static NavMonitor monitor;
   static BAbsTime lastScanStart = BAbsTime.NULL;
   static long lastScanTicks = 0L;
   static long minScanTicks = Long.MAX_VALUE;
   static long maxScanTicks = Long.MIN_VALUE;
   static long sumScanTicks = 0L;
   static long numScans = 0L;
   static Logger log = Logger.getLogger("NavMonitor");
   private static final Object lock = new Object();

   public static NavMonitor get() {
      if (monitor == null) {
         log.info("*** NavMonitor lazy init ***");
         init();
      }

      return monitor;
   }

   public static void init() {
      if (monitor == null) {
         monitor = new NavMonitor();
         monitor.start();
         if (log.isLoggable(Level.FINE)) {
            log.fine("NavMonitor thread started.");
         }
      }
   }

   public static void touch() {
      NavMonitor monitor = get();
      if (monitor != null) {
         synchronized (lock) {
            lock.notify();
         }
      }
   }

   private NavMonitor() {
      super(Nre.mainThreadGroup, "Ui:NavMonitor");
      this.setDaemon(true);
      Spy.ROOT.add("navMonitor", new NavMonitor.MySpy());
   }

   @Override
   public void run() {
      if (SCAN_RATE > 0L) {
         long waitTime = SCAN_RATE - 500L;
         if (waitTime <= 0L) {
            waitTime = SCAN_RATE;
         }

         if (log.isLoggable(Level.FINE)) {
            log.fine("NavMonitor enabled with SCAN_RATE = " + SCAN_RATE);
         }

         while (true) {
            try {
               synchronized (lock) {
                  lock.wait(waitTime);
               }

               Thread.sleep(500L);
               lastScanStart = BAbsTime.now();
               long scanStartTicks = Clock.ticks();
               if (log.isLoggable(Level.FINE)) {
                  log.fine("Performing full TOUCH Scan");
               }

               this.scan();
               synchronized (lock) {
                  lastScanTicks = Clock.ticks() - scanStartTicks;
                  minScanTicks = Math.min(lastScanTicks, minScanTicks);
                  maxScanTicks = Math.max(lastScanTicks, maxScanTicks);
                  sumScanTicks = sumScanTicks + lastScanTicks;
                  numScans++;
               }

               if (log.isLoggable(Level.FINE)) {
                  log.fine("Completed full TOUCH Scan in " + lastScanTicks + " ticks");
               }
            } catch (Throwable var9) {
               log.log(Level.SEVERE, "Exception in NavMonitor", var9);
            }
         }
      }

      if (log.isLoggable(Level.FINE)) {
         log.fine("NavMonitor disabled since SCAN_RATE = " + SCAN_RATE);
      }
   }

   void scan() {
      NavListener[] listeners = BNavRoot.INSTANCE.getNavListeners();
      if (listeners != null) {
         Stream<BISpaceNode> nodes = Stream.empty();

         for (NavListener listener : listeners) {
            if (listener instanceof BNavTree) {
               nodes = Stream.concat(nodes, visibleSpaceNodes((BNavTree)listener));
            } else if (listener instanceof TableModel) {
               nodes = Stream.concat(nodes, tableModelSpaceNodes((TableModel)listener));
            }
         }

         BNavMonitorRpc.touchNodes(nodes, null);
      }
   }

   private static Stream<BISpaceNode> visibleSpaceNodes(BNavTree tree) {
      TreeNode[] visibleNodes = tree.getVisibleNodes();
      return visibleNodes == null
         ? Stream.empty()
         : Arrays.stream(visibleNodes).map(n -> ((NavTreeNode)n).getNavNode()).filter(n -> n instanceof BISpaceNode).map(n -> (BISpaceNode)n);
   }

   private static Stream<BISpaceNode> tableModelSpaceNodes(TableModel model) {
      int rowCount = model.getRowCount();
      return IntStream.range(0, rowCount)
         .boxed()
         .<Object>map(model::getSubject)
         .filter(o -> o instanceof BINavNode && o instanceof BISpaceNode)
         .map(n -> (BISpaceNode)n);
   }

   public void spy(SpyWriter out) {
      out.startProps();
      out.trTitle("NavMonitor", 2);
      if (SCAN_RATE <= 0L) {
         out.prop("SCAN_RATE", SCAN_RATE + " (disabled)");
      } else {
         out.prop("SCAN_RATE", SCAN_RATE + " (" + BRelTime.make(SCAN_RATE) + ")");
      }

      out.prop("lastScanStart", lastScanStart);
      synchronized (lock) {
         out.prop("numScans", "" + numScans);
         out.prop("lastScanTicks", lastScanTicks + " (" + BRelTime.make(lastScanTicks) + ")");
         out.prop("minScanTicks", minScanTicks + " (" + BRelTime.make(minScanTicks) + ")");
         out.prop("maxScanTicks", maxScanTicks + " (" + BRelTime.make(maxScanTicks) + ")");
         if (numScans > 0L) {
            double avgScan = (double)sumScanTicks / numScans;
            out.prop("avgScanTicks", avgScan + " (" + BRelTime.make((long)avgScan) + ")");
         }
      }

      out.endProps();
   }

   class MySpy extends Spy {
      public void write(SpyWriter out) {
         NavMonitor.this.spy(out);
      }
   }
}
