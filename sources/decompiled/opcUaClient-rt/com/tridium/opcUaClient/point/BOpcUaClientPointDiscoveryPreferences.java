package com.tridium.opcUaClient.point;

import com.tridium.ndriver.discover.BNDiscoveryPreferences;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "showAddableOnly",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "excludeServer",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "excludeTypesFolder",
      type = "boolean",
      defaultValue = "true"
   )})
public class BOpcUaClientPointDiscoveryPreferences extends BNDiscoveryPreferences {
   public static final Property showAddableOnly = newProperty(0, true, null);
   public static final Property excludeServer = newProperty(0, true, null);
   public static final Property excludeTypesFolder = newProperty(0, true, null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientPointDiscoveryPreferences.class);

   public boolean getShowAddableOnly() {
      return this.getBoolean(showAddableOnly);
   }

   public void setShowAddableOnly(boolean v) {
      this.setBoolean(showAddableOnly, v, null);
   }

   public boolean getExcludeServer() {
      return this.getBoolean(excludeServer);
   }

   public void setExcludeServer(boolean v) {
      this.setBoolean(excludeServer, v, null);
   }

   public boolean getExcludeTypesFolder() {
      return this.getBoolean(excludeTypesFolder);
   }

   public void setExcludeTypesFolder(boolean v) {
      this.setBoolean(excludeTypesFolder, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getDiscoveryLeafType() {
      return BOpcUaLearnBase.TYPE;
   }
}
