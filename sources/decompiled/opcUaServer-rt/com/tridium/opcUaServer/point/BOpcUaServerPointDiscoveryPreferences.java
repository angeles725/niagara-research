package com.tridium.opcUaServer.point;

import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "doNotAskAgain",
      type = "boolean",
      defaultValue = "false",
      flags = 4,
      override = true
   ), @NiagaraProperty(
      name = "searchLocation",
      type = "BOrd",
      defaultValue = "BOrd.make(\"station:|slot:/|bql:select * from control:ControlPoint\")"
   )})
public class BOpcUaServerPointDiscoveryPreferences extends BNDiscoveryPreferences {
   public static final Property doNotAskAgain = newProperty(4, false, null);
   public static final Property searchLocation = newProperty(0, BOrd.make("station:|slot:/|bql:select * from control:ControlPoint"), null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerPointDiscoveryPreferences.class);

   public BOrd getSearchLocation() {
      return (BOrd)this.get(searchLocation);
   }

   public void setSearchLocation(BOrd v) {
      this.set(searchLocation, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDiscoveryLeafType() {
      return BOpcUaServerLearnPointEntry.TYPE;
   }
}
