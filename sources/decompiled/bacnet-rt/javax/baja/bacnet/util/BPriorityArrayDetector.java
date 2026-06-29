package javax.baja.bacnet.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.point.BBacnetPointDeviceExt;
import javax.baja.bacnet.point.BBacnetProxyExt;
import javax.baja.control.BControlPoint;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFolder;

@NiagaraType
@NiagaraAction(
   name = "detect"
)
public class BPriorityArrayDetector extends BComponent {
   public static final Action detect = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BPriorityArrayDetector.class);
   private static final BIcon icon = BIcon.make("module://bacnet/com/tridium/bacnet/ui/icons/override.png");
   static Logger logger = Logger.getLogger("bacnet");

   public void detect() {
      this.invoke(detect, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetNetwork
         || parent instanceof BFolder
         || parent instanceof BBacnetDevice
         || parent instanceof BBacnetPointDeviceExt
         || parent instanceof BControlPoint
         || parent instanceof BBacnetProxyExt;
   }

   public void doDetect() {
      this.detectPriorityArray(this.getParent().asComponent());
   }

   public void detectPriorityArray(BComponent component) {
      if (component instanceof BBacnetProxyExt) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Detecting priority array on: " + component);
         }

         BBacnetProxyExt bacnetProxy = (BBacnetProxyExt)component;
         bacnetProxy.discoverPrioritizedPresentValue(true);
      }

      for (BComponent child : component.getChildComponents()) {
         if (child != this && this.isParentLegal(child)) {
            this.detectPriorityArray(child);
         }
      }
   }

   public BIcon getIcon() {
      return icon;
   }
}
