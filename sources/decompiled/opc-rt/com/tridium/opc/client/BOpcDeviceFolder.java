package com.tridium.opc.client;

import javax.baja.driver.BDeviceFolder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcDeviceFolder extends BDeviceFolder {
   public static final Type TYPE = Sys.loadType(BOpcDeviceFolder.class);
   private BOpcNetwork network;

   public Type getType() {
      return TYPE;
   }

   public final BOpcNetwork getOpcNetwork() {
      if (this.network == null) {
         for (BComplex cur = this.getParent(); cur != null; cur = cur.getParent()) {
            if (cur instanceof BOpcNetwork) {
               this.network = (BOpcNetwork)cur;
               break;
            }
         }
      }

      return this.network;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BOpcNetwork || parent instanceof BOpcDeviceFolder;
   }

   public final void started() throws Exception {
      this.network = null;
   }
}
