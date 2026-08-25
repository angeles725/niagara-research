package javax.baja.nre.security;

import java.util.Map;

public interface HsmManager {
   String DEFAULT_HSM_TYPE = "none";

   boolean hasHsmEngine();

   String getHsmEngineClassName();

   Map<String, String> getProperties();

   default String getHsmType() {
      return "none";
   }

   default boolean hasHsm() {
      return !"none".equals(this.getHsmType());
   }
}
