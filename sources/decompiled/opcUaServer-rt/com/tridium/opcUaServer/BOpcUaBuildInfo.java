package com.tridium.opcUaServer;

import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "productName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "productUri",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "manufacturer",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "softwareVersion",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "buildNumber",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "buildDate",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
@NiagaraAction(
   name = "update",
   flags = 16
)
public final class BOpcUaBuildInfo extends BComponent {
   public static final Property productName = newProperty(1, "", null);
   public static final Property productUri = newProperty(1, "", null);
   public static final Property manufacturer = newProperty(1, "", null);
   public static final Property softwareVersion = newProperty(1, "", null);
   public static final Property buildNumber = newProperty(1, "", null);
   public static final Property buildDate = newProperty(1, "", null);
   public static final Action update = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcUaBuildInfo.class);
   BOpcUaServer network = null;

   public String getProductName() {
      return this.getString(productName);
   }

   public void setProductName(String v) {
      this.setString(productName, v, null);
   }

   public String getProductUri() {
      return this.getString(productUri);
   }

   public void setProductUri(String v) {
      this.setString(productUri, v, null);
   }

   public String getManufacturer() {
      return this.getString(manufacturer);
   }

   public void setManufacturer(String v) {
      this.setString(manufacturer, v, null);
   }

   public String getSoftwareVersion() {
      return this.getString(softwareVersion);
   }

   public void setSoftwareVersion(String v) {
      this.setString(softwareVersion, v, null);
   }

   public String getBuildNumber() {
      return this.getString(buildNumber);
   }

   public void setBuildNumber(String v) {
      this.setString(buildNumber, v, null);
   }

   public String getBuildDate() {
      return this.getString(buildDate);
   }

   public void setBuildDate(String v) {
      this.setString(buildDate, v, null);
   }

   public void update() {
      this.invoke(update, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      BComplex complex = this.getParent();
      if (complex instanceof BOpcUaServer) {
         this.network = (BOpcUaServer)complex;
      }
   }

   public IFuture post(Action action, BValue argument, Context cx) {
      return this.network != null ? this.network.postAsync(new Invocation(this, action, argument, cx)) : super.post(action, argument, cx);
   }

   public void subscribed() {
      if (this.network != null && this.network.getStatus().isValid()) {
         this.update();
      }
   }

   public void doUpdate() {
      if (this.network != null && this.network.getStatus().isValid() && this.network.server != null) {
         UaServer uaServer = this.network.server;
         BuildInfoTypeNode buildInfo = uaServer.getNodeManagerRoot().getServerData().getServerStatusNode().getBuildInfoNode();
         this.setProductName(buildInfo.getProductName());
         this.setProductUri(buildInfo.getProductUri());
         this.setManufacturer(buildInfo.getManufacturerName());
         this.setSoftwareVersion(buildInfo.getSoftwareVersion());
         this.setBuildNumber(buildInfo.getBuildNumber());
         this.setBuildDate(buildInfo.getBuildDate().toString());
      }
   }
}
