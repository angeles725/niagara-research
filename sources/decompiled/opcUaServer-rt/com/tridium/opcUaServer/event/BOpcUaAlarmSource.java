package com.tridium.opcUaServer.event;

import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import javax.baja.control.BControlPoint;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nodeId",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true",
      flags = 1
   ), @NiagaraProperty(
      name = "ord",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      flags = 1
   )})
public class BOpcUaAlarmSource extends BComponent {
   public static final Property nodeId = newProperty(1, "", null);
   public static final Property enabled = newProperty(1, true, null);
   public static final Property ord = newProperty(1, BOrd.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BOpcUaAlarmSource.class);

   public String getNodeId() {
      return this.getString(nodeId);
   }

   public void setNodeId(String v) {
      this.setString(nodeId, v, null);
   }

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public BOrd getOrd() {
      return (BOrd)this.get(ord);
   }

   public void setOrd(BOrd v) {
      this.set(ord, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BOpcUaAlarmSource make(AlarmConditionTypeNode variable, BControlPoint point) {
      BOpcUaAlarmSource almSrc = new BOpcUaAlarmSource();
      almSrc.setNodeId(variable.getNodeId().toString());
      almSrc.setOrd(point.getSlotPathOrd());
      almSrc.setEnabled(variable.isEnabled());
      return almSrc;
   }

   public void updateFrom(AlarmConditionTypeNode variable, BControlPoint point) {
      this.setNodeId(variable.getNodeId().toString());
      this.setOrd(point.getSlotPathOrd());
      this.setEnabled(variable.isEnabled());
   }

   public void updateFrom(ConditionTypeNode variable) {
      this.setNodeId(variable.getNodeId().toString());
      this.setEnabled(variable.isEnabled());
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getEnabled()).append(" ").append(this.getOrd());
      return sb.toString();
   }
}
