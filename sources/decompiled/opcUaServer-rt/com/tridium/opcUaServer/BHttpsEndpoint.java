package com.tridium.opcUaServer;

import com.tridium.opcUaCore.BOpcHttpsSecurityPolicies;
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
      defaultValue = "52443"
   ), @NiagaraProperty(
      name = "opcHttpsSecurityPolicies",
      type = "BOpcHttpsSecurityPolicies",
      defaultValue = "BOpcHttpsSecurityPolicies.DEFAULT"
   )})
public class BHttpsEndpoint extends BStruct {
   public static final Property enabled = newProperty(0, true, null);
   public static final Property port = newProperty(0, 52443, null);
   public static final Property opcHttpsSecurityPolicies = newProperty(0, BOpcHttpsSecurityPolicies.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BHttpsEndpoint.class);

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

   public BOpcHttpsSecurityPolicies getOpcHttpsSecurityPolicies() {
      return (BOpcHttpsSecurityPolicies)this.get(opcHttpsSecurityPolicies);
   }

   public void setOpcHttpsSecurityPolicies(BOpcHttpsSecurityPolicies v) {
      this.set(opcHttpsSecurityPolicies, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
