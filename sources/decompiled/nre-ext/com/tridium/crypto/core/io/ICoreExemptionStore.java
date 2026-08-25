package com.tridium.crypto.core.io;

import com.tridium.crypto.core.cert.NHostExemption;
import java.util.Enumeration;

public interface ICoreExemptionStore extends ICoreStore {
   Enumeration<NHostExemption> exemptions() throws Exception;

   void setExemption(NHostExemption var1) throws Exception;

   void deleteExemption(String var1) throws Exception;

   NHostExemption getExemption(String var1) throws Exception;

   void load() throws Exception;

   void save() throws Exception;

   default int size() throws Exception {
      int exemptionSize = 0;
      Enumeration<NHostExemption> exemptions = this.exemptions();

      while (exemptions.hasMoreElements()) {
         exemptionSize++;
         exemptions.nextElement();
      }

      return exemptionSize;
   }
}
