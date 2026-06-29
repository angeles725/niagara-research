package com.tridium.fox.sys;

import com.tridium.fox.session.FoxSession;
import com.tridium.sys.license.Brand;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.license.LicenseException;
import javax.baja.sys.Sys;

public class Acceptor {
   public static final Logger log = Logger.getLogger("fox");

   public static void accept(FoxSession session) {
      boolean incoming = session.isServer();
      boolean incomingIsStation = session.getRemoteHello().getString("station.name", null) != null
         || session.getRemoteHello().getString("app.name", "").equals("Station");
      boolean thisIsStation = Sys.getStation() != null;
      if (incoming) {
         if (incomingIsStation) {
            acceptStationIn(session);
         } else {
            acceptWbIn(session);
         }
      } else if (thisIsStation) {
         acceptStationOut(session);
      } else {
         acceptWbOut(session);
      }
   }

   private static void acceptStationIn(FoxSession session) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Accept.station.in: " + session);
      }

      String brandId = session.getRemoteHello().getString("brandId", null);
      Brand.checkStationIn(brandId);
      BFoxConnection conn = (BFoxConnection)session.conn();
      Optional<NiagaraStation> station = conn.getConnectionTarget(NiagaraStation.class);
      if (station.isPresent() && station.get().isFatalFault()) {
         throw new LicenseException("Station in fatal fault: " + station.get().getFaultCause());
      }
   }

   private static void acceptStationOut(FoxSession session) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Accept.station.out: " + session);
      }

      BFoxConnection conn = (BFoxConnection)session.conn();
      Optional<NiagaraStation> station = conn.getConnectionTarget(NiagaraStation.class);
      if (station.isPresent()) {
         String expected = station.get().getStationName();
         String actual = session.getRemoteHello().getString("station.name", null);
         if (expected != null && actual != null && !expected.equals(actual)) {
            throw new MismatchedStationNamesException(expected, actual);
         }
      }

      String brandId = session.getRemoteHello().getString("brandId", null);
      Brand.checkStationOut(brandId);
   }

   private static void acceptWbIn(FoxSession session) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Accept.wb.in: " + session);
      }

      String brandId = session.getRemoteHello().getString("brandId", null);
      Brand.checkWbIn(brandId);
   }

   private static void acceptWbOut(FoxSession session) {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Accept.wb.out: " + session);
      }

      String brandId = session.getRemoteHello().getString("brandId", null);
      Brand.checkWbOut(brandId);
   }
}
