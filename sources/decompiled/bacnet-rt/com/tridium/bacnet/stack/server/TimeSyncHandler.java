package com.tridium.bacnet.stack.server;

import com.tridium.bacnet.services.BacnetServicePrimitive;
import com.tridium.bacnet.services.unconfirmed.TimeSynchronizationRequest;
import com.tridium.bacnet.services.unconfirmed.UtcTimeSynchronizationRequest;
import com.tridium.history.audit.BAuditHistoryService;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.security.AuditEvent;
import javax.baja.security.Auditor;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.ServiceNotFoundException;
import javax.baja.sys.Sys;

public class TimeSyncHandler implements ServiceHandler, BacnetUnconfirmedServiceChoice {
   private BBacnetServerLayer server = null;
   private static final Logger logger = Logger.getLogger("bacnet.server");
   private static final BFacets TIME_FACETS = BFacets.make("showSeconds", BBoolean.TRUE, "showMilliseconds", BBoolean.TRUE);

   TimeSyncHandler(BBacnetServerLayer server) {
      this.server = server;
   }

   @Override
   public BacnetServicePrimitive receiveRequest(int serviceChoice, BacnetServicePrimitive request, BBacnetAddress sourceAddress) {
      switch (serviceChoice) {
         case 6:
            this.processTimeSynchRequest((TimeSynchronizationRequest)request, sourceAddress);
            return null;
         case 9:
            this.processUTCTimeSynchRequest((UtcTimeSynchronizationRequest)request, sourceAddress);
            return null;
         default:
            logger.info("TimeSyncHandler.receiveRequest:Unknown request! " + request);
            return null;
      }
   }

   private void processTimeSynchRequest(TimeSynchronizationRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("TimeSyncHandler: TimeSynchronizationRequest received: " + request + "\n from address " + sourceAddress);
      }

      if (this.server.getTimeSynchAllowed()) {
         BAbsTime oldTime = Clock.time(10);
         BAbsTime newTime = request.getBAbsTime();
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            Clock.setTime(newTime);
            return null;
         }));

         try {
            Auditor auditor = Sys.getAuditor();
            AuditEvent event = new AuditEvent(
               "Changed", "Station", "Time Synch", oldTime.toString(TIME_FACETS), newTime.toString(TIME_FACETS), sourceAddress.toString()
            );
            auditor.audit(event);
         } catch (ServiceNotFoundException var7) {
            logger.log(Level.SEVERE, "ServiceNotFoundException occurred in processTimeSynchRequest", (Throwable)var7);
         }
      } else {
         logger.info("Time Synchronization not allowed for this station!");
      }
   }

   private void processUTCTimeSynchRequest(UtcTimeSynchronizationRequest request, BBacnetAddress sourceAddress) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("TimeSyncHandler: UtcTimeSynchronizationRequest received: " + request + "\n from address " + sourceAddress);
      }

      if (this.server.getTimeSynchAllowed()) {
         BAbsTime oldTime = Clock.time(10);
         BAbsTime bt = request.getBAbsTime();
         int off = BAbsTime.make().getTimeZoneOffset();
         BRelTime rt = BRelTime.make(off);
         BAbsTime newTime = bt.add(rt);
         AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
            Clock.setTime(newTime);
            return null;
         }));

         try {
            var ahs = (BAuditHistoryService & BAuditHistoryService)Sys.getService(BAuditHistoryService.TYPE);
            AuditEvent event = new AuditEvent(
               "Changed", "Station", "UTC Time Synch", oldTime.toString(TIME_FACETS), newTime.toString(TIME_FACETS), sourceAddress.toString()
            );
            ahs.audit(event);
         } catch (ServiceNotFoundException var10) {
            logger.log(Level.SEVERE, "ServiceNotFoundException occurred in processUTCTimeSynchRequest", (Throwable)var10);
         }
      } else {
         logger.info("UTC Time Synchronization not allowed for this station!");
      }
   }
}
