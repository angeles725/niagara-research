package com.tridium.opcUaClient.history;

import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import com.tridium.opcUaClient.point.BOpcUaNodeLearnEntry;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaClientHistoryDiscoveryPreferences extends BNDiscoveryPreferences {
   public static final Type TYPE = Sys.loadType(BOpcUaClientHistoryDiscoveryPreferences.class);

   public Type getType() {
      return TYPE;
   }

   public Type getDiscoveryLeafType() {
      return BOpcUaNodeLearnEntry.TYPE;
   }
}
