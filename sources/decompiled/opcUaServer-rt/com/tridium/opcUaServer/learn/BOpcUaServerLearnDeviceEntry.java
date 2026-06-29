package com.tridium.opcUaServer.learn;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "address",
   type = "String",
   defaultValue = ""
)
public class BOpcUaServerLearnDeviceEntry extends BComponent {
   public static final Property address = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerLearnDeviceEntry.class);

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaServerLearnDeviceEntry() {
   }

   public BOpcUaServerLearnDeviceEntry(String address) {
      this.setAddress(address);
   }
}
