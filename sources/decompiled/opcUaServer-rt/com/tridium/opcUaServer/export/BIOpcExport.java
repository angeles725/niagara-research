package com.tridium.opcUaServer.export;

import com.tridium.opcUaServer.point.BOpcUaServerProxyExt;
import java.util.logging.Logger;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BIObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIOpcExport extends BIObject {
   Type TYPE = Sys.loadType(BIOpcExport.class);
   Logger logger = Logger.getLogger("opcUaServer.export");

   BAbstractProxyExt getProxyExt();

   void execute();

   default void updateValue(BStatusValue inValue, Context cx) {
      BAbstractProxyExt ext = this.getProxyExt();
      if (ext instanceof BOpcUaServerProxyExt) {
         BOpcUaServerProxyExt proxyExt = (BOpcUaServerProxyExt)ext;
         proxyExt.setWriteValue(inValue);

         try {
            proxyExt.write(cx);
         } catch (Exception var6) {
            logger.severe("Exception while Writing an updated value: " + var6);
            return;
         }

         this.execute();
      }
   }
}
