package com.tridium.opc.client;

import com.tridium.opc.client.util.BOpcSecurityLoginState;
import javax.baja.log.Log;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPassword;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "privateSecurity",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "state",
      type = "BOpcSecurityLoginState",
      defaultValue = "BOpcSecurityLoginState.login"
   ), @NiagaraProperty(
      name = "loginName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "loginPassword",
      type = "BPassword",
      defaultValue = "BPassword.DEFAULT"
   )})
public class BOpcDASecurity extends BComponent {
   public static final Property privateSecurity = newProperty(0, false, null);
   public static final Property state = newProperty(0, BOpcSecurityLoginState.login, null);
   public static final Property loginName = newProperty(0, "", null);
   public static final Property loginPassword = newProperty(0, BPassword.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BOpcDASecurity.class);
   Log opcLog = Log.getLog("OpcDaLog");

   public boolean getPrivateSecurity() {
      return this.getBoolean(privateSecurity);
   }

   public void setPrivateSecurity(boolean v) {
      this.setBoolean(privateSecurity, v, null);
   }

   public BOpcSecurityLoginState getState() {
      return (BOpcSecurityLoginState)this.get(state);
   }

   public void setState(BOpcSecurityLoginState v) {
      this.set(state, v, null);
   }

   public String getLoginName() {
      return this.getString(loginName);
   }

   public void setLoginName(String v) {
      this.setString(loginName, v, null);
   }

   public BPassword getLoginPassword() {
      return (BPassword)this.get(loginPassword);
   }

   public void setLoginPassword(BPassword v) {
      this.set(loginPassword, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void MakeSlotsReadOnly(boolean bNTSec, boolean bPrivateSec) {
      if (!bNTSec) {
         this.setPrivateSecurity(true);
         this.setFlags(this.getSlot("privateSecurity"), 1);
         this.setState(BOpcSecurityLoginState.login);
      } else if (!bPrivateSec) {
         this.setPrivateSecurity(false);
         this.setFlags(this.getSlot("privateSecurity"), 1);
         this.setState(BOpcSecurityLoginState.login);
         this.setFlags(this.getSlot("state"), 1);
      }
   }
}
