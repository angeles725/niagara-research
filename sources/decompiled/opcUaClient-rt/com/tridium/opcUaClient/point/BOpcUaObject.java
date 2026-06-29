package com.tridium.opcUaClient.point;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "uaNodeId",
   type = "String",
   defaultValue = ""
)
public class BOpcUaObject extends BOpcUaClientPointFolder implements BIOpcUaObject {
   public static final Property uaNodeId = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BOpcUaObject.class);

   @Override
   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   @Override
   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
