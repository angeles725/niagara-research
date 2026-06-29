package com.tridium.bacnet.stack.link.sc.connection;

import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.net.BInternetAddress;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "connectionName",
      type = "String",
      defaultValue = "BString.DEFAULT"
   ), @NiagaraProperty(
      name = "uriAddress",
      type = "BInternetAddress",
      defaultValue = "new BInternetAddress(\"example.com\")"
   ), @NiagaraProperty(
      name = "uriPath",
      type = "String",
      defaultValue = "BNodeSwitch.DEFAULT_SERVLET_NAME"
   ), @NiagaraProperty(
      name = "vmac",
      type = "BBacnetOctetString",
      defaultValue = "BBacnetOctetString.DEFAULT"
   )})
public final class BAddDirectInitiatingConnectionParams extends BStruct {
   public static final Property connectionName = newProperty(0, BString.DEFAULT, null);
   public static final Property uriAddress = newProperty(0, new BInternetAddress("example.com"), null);
   public static final Property uriPath = newProperty(0, "nodeSwitch", null);
   public static final Property vmac = newProperty(0, BBacnetOctetString.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BAddDirectInitiatingConnectionParams.class);
   public static final BAddDirectInitiatingConnectionParams DEFAULT = make("", new BInternetAddress("example.com"), "nodeSwitch", BBacnetOctetString.DEFAULT);

   public String getConnectionName() {
      return this.getString(connectionName);
   }

   public void setConnectionName(String v) {
      this.setString(connectionName, v, null);
   }

   public BInternetAddress getUriAddress() {
      return (BInternetAddress)this.get(uriAddress);
   }

   public void setUriAddress(BInternetAddress v) {
      this.set(uriAddress, v, null);
   }

   public String getUriPath() {
      return this.getString(uriPath);
   }

   public void setUriPath(String v) {
      this.setString(uriPath, v, null);
   }

   public BBacnetOctetString getVmac() {
      return (BBacnetOctetString)this.get(vmac);
   }

   public void setVmac(BBacnetOctetString v) {
      this.set(vmac, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BAddDirectInitiatingConnectionParams make(String connectionName, BInternetAddress uriAddress, String uriPath, BBacnetOctetString vmac) {
      BAddDirectInitiatingConnectionParams params = new BAddDirectInitiatingConnectionParams();
      params.setConnectionName(connectionName);
      params.setUriAddress(uriAddress);
      params.setUriPath(uriPath);
      params.setVmac(vmac);
      return params;
   }
}
