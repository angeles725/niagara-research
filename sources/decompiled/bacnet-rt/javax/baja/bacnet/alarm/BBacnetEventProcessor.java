package javax.baja.bacnet.alarm;

import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLong;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "processId",
   type = "long",
   defaultValue = "1",
   facets = {@Facet(
      name = "BFacets.MIN",
      value = "BLong.make(0)"
   ), @Facet(
      name = "BFacets.MAX",
      value = "BLong.make(0x7FFFFFFF)"
   )}
)
public class BBacnetEventProcessor extends BStruct {
   public static final Property processId = newProperty(0, 1, BFacets.make(BFacets.make("min", BLong.make(0L)), BFacets.make("max", BLong.make(2147483647L))));
   public static final Type TYPE = Sys.loadType(BBacnetEventProcessor.class);
   private static final Logger logger = Logger.getLogger("bacnet.alarm");

   public long getProcessId() {
      return this.getLong(processId);
   }

   public void setProcessId(long v) {
      this.setLong(processId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void routeAlarm(BAlarmRecord record) {
      logger.info("BacnetEventProcessor(" + this.getProcessId() + "):routeAlarm::\n" + record);
   }
}
