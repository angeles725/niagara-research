package javax.baja.bacnet.alarm;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.server.BEventHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmService;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetAlarmConst;
import javax.baja.driver.alarm.BAlarmDeviceExt;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BLong;
import javax.baja.sys.BString;
import javax.baja.sys.Property;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "alarmClass",
      type = "String",
      defaultValue = "defaultAlarmClass",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"alarm:AlarmClassFE\""
      ), @Facet(
         name = "BFacets.UX_FIELD_EDITOR",
         value = "\"alarm:AlarmClassEditor\""
      )},
      override = true
   ), @NiagaraProperty(
      name = "niagaraProcessId",
      type = "long",
      defaultValue = "0",
      facets = {@Facet(
         name = "BFacets.MIN",
         value = "BLong.make(0)"
      ), @Facet(
         name = "BFacets.MAX",
         value = "BLong.make(0x7FFFFFFF)"
      )}
   )})
public class BBacnetAlarmDeviceExt extends BAlarmDeviceExt implements BacnetAlarmConst {
   public static final Property alarmClass = newProperty(
      0, "defaultAlarmClass", BFacets.make(BFacets.make("fieldEditor", "alarm:AlarmClassFE"), BFacets.make("uxFieldEditor", "alarm:AlarmClassEditor"))
   );
   public static final Property niagaraProcessId = newProperty(
      0, 0, BFacets.make(BFacets.make("min", BLong.make(0L)), BFacets.make("max", BLong.make(2147483647L)))
   );
   public static final Type TYPE = Sys.loadType(BBacnetAlarmDeviceExt.class);
   private static final Logger logger = Logger.getLogger("bacnet");

   public long getNiagaraProcessId() {
      return this.getLong(niagaraProcessId);
   }

   public void setNiagaraProcessId(long v) {
      this.setLong(niagaraProcessId, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBoolean doAckAlarm(BAlarmRecord ackRequest) {
      return this.getEventHandler().doAckAlarm(ackRequest);
   }

   public void doRouteAlarm(BAlarmRecord record) throws Exception {
      try {
         BString s = (BString)record.getAlarmFacet("processId");
         if (s != null) {
            long processId = Long.parseLong(s.getString());
            boolean noEventProcessor = true;
            if (processId == this.getNiagaraProcessId()) {
               noEventProcessor = false;
               BAlarmService as = (BAlarmService)Sys.getService(BAlarmService.TYPE);
               as.routeAlarm(record);
            }

            SlotCursor<Property> sc = this.getProperties();

            while (sc.next(BBacnetEventProcessor.class)) {
               BBacnetEventProcessor proc = (BBacnetEventProcessor)sc.get();
               if (proc.getProcessId() == processId) {
                  noEventProcessor = false;
                  proc.routeAlarm(record);
               }
            }

            if (noEventProcessor) {
               logger.info("AlarmDeviceExt(procId " + this.getNiagaraProcessId() + "): no event processor for alarm record:\n" + record);
            }
         }
      } catch (ServiceNotFoundException var8) {
         logger.log(Level.SEVERE, "AlarmDeviceExt.processEvent:Unable to find Alarm Service!", (Throwable)var8);
      }
   }

   private BEventHandler getEventHandler() {
      return ((BBacnetStack)((BBacnetNetwork)this.getNetwork()).getBacnetComm()).getServer().getEventHandler();
   }
}
