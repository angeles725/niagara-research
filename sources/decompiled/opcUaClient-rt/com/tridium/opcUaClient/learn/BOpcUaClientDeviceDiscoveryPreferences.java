package com.tridium.opcUaClient.learn;

import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BOpcUaClientDeviceDiscoveryPreferences extends BNDiscoveryPreferences {
   public static final Type TYPE = Sys.loadType(BOpcUaClientDeviceDiscoveryPreferences.class);

   public Type getType() {
      return TYPE;
   }

   public Type getDiscoveryLeafType() {
      return BOpcUaClientLearnDeviceEntry.TYPE;
   }
}
