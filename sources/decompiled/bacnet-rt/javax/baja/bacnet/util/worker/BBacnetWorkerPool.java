package javax.baja.bacnet.util.worker;

import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Queue;
import javax.baja.util.ThreadPoolWorker;

@NiagaraType
@NiagaraProperty(
   name = "maxThreads",
   type = "int",
   defaultValue = "DEFAULT_WORKERS",
   facets = {@Facet(
      name = "BFacets.MIN",
      value = "1"
   )}
)
public class BBacnetWorkerPool extends BComponent implements IWorkerPool {
   private static final int DEFAULT_WORKERS = 2;
   public static final Property maxThreads = newProperty(0, 2, BFacets.make("min", 1));
   public static final Type TYPE = Sys.loadType(BBacnetWorkerPool.class);
   private ThreadPoolWorker worker = null;
   private Queue queue = null;
   private static final BIcon icon = BIcon.std("gears.png");

   public int getMaxThreads() {
      return this.getInt(maxThreads);
   }

   public void setMaxThreads(int v) {
      this.setInt(maxThreads, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return isLegalParent(parent);
   }

   public static boolean isLegalParent(BComponent parent) {
      return parent instanceof IWorkerPoolAware && !alreadyHasWorkerPool(parent);
   }

   private static boolean alreadyHasWorkerPool(BComponent parent) {
      BComponent[] children = parent.getChildComponents();

      for (int i = 0; i < children.length; i++) {
         if (children[i] instanceof IWorkerPool) {
            return true;
         }
      }

      return false;
   }

   public void started() throws Exception {
      super.started();
      IWorkerPoolAware parent = (IWorkerPoolAware)this.getParent();
      this.queue = parent.getQueue();
      this.worker = new ThreadPoolWorker(this.queue);
      this.worker.setMaxThreads(this.getMaxThreads());
      this.worker.start(parent.getWorkerThreadName());
      parent.stopWorker();
   }

   public void stopped() throws Exception {
      super.stopped();
      this.worker.stop();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(maxThreads)) {
            this.updateMaxThreads();
         }
      }
   }

   private void updateMaxThreads() {
      this.worker.setMaxThreads(this.getMaxThreads());
   }

   @Override
   public IFuture post(Runnable r) {
      if (!this.isRunning()) {
         throw new NotRunningException();
      } else {
         this.queue.enqueue(r);
         return null;
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
