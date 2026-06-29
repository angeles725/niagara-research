package com.tridium.fox.sys;

import java.util.Optional;
import java.util.regex.Pattern;
import javax.baja.sys.BajaRuntimeException;

public class UnreachableStationException extends BajaRuntimeException {
   private static final String UNOPERATIONAL_REACHABLE_STATION_FORMAT = "Station '%s' in the NiagaraNetwork of station '%s' is currently unoperational";
   private static final Pattern UNOPERATIONAL_REACHABLE_EXCEPTION_PATTERN = Pattern.compile(
      ".*Station '.*' in the NiagaraNetwork of station '.*' is currently unoperational.*"
   );

   public UnreachableStationException(String msg) {
      super(msg);
   }

   public UnreachableStationException(Throwable cause) {
      super(cause.getMessage(), cause);
   }

   public String toString() {
      return this.toString(null);
   }

   public static UnreachableStationException makeUnoperationalStationException(String unoperationalStationName, String parentStationName) {
      return new UnreachableStationException(
         String.format("Station '%s' in the NiagaraNetwork of station '%s' is currently unoperational", unoperationalStationName, parentStationName)
      );
   }

   public static Optional<UnreachableStationException> extractUnoperationalStationException(Throwable throwable) {
      for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
         if (cause.getMessage() != null && UNOPERATIONAL_REACHABLE_EXCEPTION_PATTERN.matcher(cause.getMessage()).matches()) {
            return Optional.of(new UnreachableStationException(cause));
         }
      }

      return Optional.empty();
   }
}
