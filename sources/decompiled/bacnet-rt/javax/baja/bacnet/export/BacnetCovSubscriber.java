package javax.baja.bacnet.export;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.Subscriber;

class BacnetCovSubscriber extends Subscriber {
   private final HashMap<BComponent, BIBacnetCovSource> sublist = new HashMap<>();
   private static final Logger logger = Logger.getLogger("bacnet.server");

   public void subscribe(BIBacnetCovSource export, BComponent src) {
      this.sublist.put(src, export);
      this.subscribe(src);
   }

   public void unsubscribe(BIBacnetCovSource export, BComponent src) {
      this.sublist.remove(src);
      this.unsubscribe(src);
   }

   public void event(BComponentEvent event) {
      BIBacnetCovSource export = null;
      BComponent src = event.getSourceComponent();

      try {
         if (event.getId() == 0) {
            export = this.sublist.get(src);
            if (export != null) {
               if (event.getSlot() == export.getOutProperty()) {
                  export.checkCov();
               } else if (event.getSlotName().equals("loopEnable")) {
                  export.checkCov();
               }
            } else if (logger.isLoggable(Level.FINE)) {
               logger.fine("BacnetCovSubscriber received event for unknown component: " + src);
            }
         }
      } catch (Exception var5) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Error in BacnetCovSubscriber: src=" + src + "; export=" + export, (Throwable)var5);
         }
      }
   }
}
