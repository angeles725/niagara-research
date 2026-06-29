package com.tridium.opcUaServer;

import com.tridium.opcUaCore.BOpcTcpSecurityModes;
import com.tridium.opcUaCore.BOpcTcpSecurityPolicies;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "port",
      type = "int",
      defaultValue = "52520"
   ), @NiagaraProperty(
      name = "securityMode",
      type = "BOpcTcpSecurityModes",
      defaultValue = "BOpcTcpSecurityModes.make(true)"
   ), @NiagaraProperty(
      name = "securityPolicies",
      type = "BOpcTcpSecurityPolicies",
      defaultValue = "BOpcTcpSecurityPolicies.DEFAULT"
   )})
public class BOpcTcpEndpoint extends BStruct {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property port = newProperty(0, 52520, null);
   public static final Property securityMode = newProperty(0, BOpcTcpSecurityModes.make(true), null);
   public static final Property securityPolicies = newProperty(0, BOpcTcpSecurityPolicies.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BOpcTcpEndpoint.class);

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public int getPort() {
      return this.getInt(port);
   }

   public void setPort(int v) {
      this.setInt(port, v, null);
   }

   public BOpcTcpSecurityModes getSecurityMode() {
      return (BOpcTcpSecurityModes)this.get(securityMode);
   }

   public void setSecurityMode(BOpcTcpSecurityModes v) {
      this.set(securityMode, v, null);
   }

   public BOpcTcpSecurityPolicies getSecurityPolicies() {
      return (BOpcTcpSecurityPolicies)this.get(securityPolicies);
   }

   public void setSecurityPolicies(BOpcTcpSecurityPolicies v) {
      this.set(securityPolicies, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcTcpEndpoint() {
   }

   public BOpcTcpEndpoint(BOpcTcpEndpoint src) {
      this.copyFrom(src);
   }
}
