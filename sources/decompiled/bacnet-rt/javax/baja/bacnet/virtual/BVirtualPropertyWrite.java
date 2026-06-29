package javax.baja.bacnet.virtual;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetPriorityValue;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.AuditEvent;
import javax.baja.security.Auditor;
import javax.baja.sys.BAction;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BVirtualPropertyWrite extends BAction implements BacnetConst {
   public static final Type TYPE = Sys.loadType(BVirtualPropertyWrite.class);
   private static final Logger logger = Logger.getLogger("bacnet.virtual");

   public Type getType() {
      return TYPE;
   }

   public BValue getParameterDefault() {
      BBacnetVirtualProperty bvp = (BBacnetVirtualProperty)this.getParent();
      BValue v = bvp.getValue();
      if (bvp.object().getPrioritizedPoint() && bvp.getPropertyId() == 85) {
         BBacnetPriorityValue pv = new BBacnetPriorityValue();
         pv.setPriorityValue(v);
         return pv;
      } else {
         return v.newCopy();
      }
   }

   public Type getParameterType() {
      BBacnetVirtualProperty bvp = (BBacnetVirtualProperty)this.getParent();
      return bvp.getValue().getType();
   }

   public Type getReturnType() {
      return null;
   }

   public BValue invoke(BComponent target, BValue arg) throws Exception {
      BBacnetVirtualProperty bvp = (BBacnetVirtualProperty)target;
      BValue currentValue = bvp.getValue();
      boolean audit = bvp.auditWrites();
      if (bvp.object().getPrioritizedPoint() && bvp.getPropertyId() == 85) {
         byte[] encodedValue = AsnUtil.toAsn(arg);
         bvp.write(-1, encodedValue, bvp.object().getWritePriority());
      } else if (!currentValue.equals(arg)) {
         if (bvp.getValue() instanceof BBacnetArray) {
            if (arg instanceof BBacnetArray) {
               BBacnetArray cur = (BBacnetArray)currentValue;
               BBacnetArray nue = (BBacnetArray)arg;
               int len = nue.getSize();

               for (int i = 1; i <= len; i++) {
                  if (!cur.getElement(i).equivalent(nue.getElement(i))) {
                     bvp.write(i, AsnUtil.toAsn(nue.getElement(i)), -1);
                  }
               }

               bvp.setValue(nue, noWrite);
            } else {
               int index = bvp.getArrayIndex();
               bvp.write(index, AsnUtil.toAsn(arg), -1);
            }
         } else {
            bvp.setValue(arg);
            audit = false;
         }
      } else {
         byte[] encodedValue = AsnUtil.toAsn(arg);
         bvp.write(-1, encodedValue, -1);
      }

      if (audit) {
         AuditEvent event = new AuditEvent("Changed", bvp.getAuditName(), "value", currentValue.toString(), arg.toString(), "");

         try {
            Auditor a = Sys.getAuditor();
            a.audit(event);
         } catch (ServiceNotFoundException var10) {
            logger.info("Could not find AuditHistoryService to log audit event:" + event);
         }
      }

      return null;
   }
}
