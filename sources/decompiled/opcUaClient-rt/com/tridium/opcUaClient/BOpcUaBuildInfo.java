package com.tridium.opcUaClient;

import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.enums.BServerState;
import java.util.logging.Level;
import java.util.logging.Logger;
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
   public static final Logger logger = Logger.getLogger("opcUaClient.client");
   private static final NodeId PRODUCT_NAME = NodeId.parseNodeId("i=2261");
   private static final NodeId PRODUCT_URI = NodeId.parseNodeId("i=2262");
   private static final NodeId MANUFACTURER = NodeId.parseNodeId("i=2263");
   private static final NodeId SOFTWARE_VERSION = NodeId.parseNodeId("i=2264");
   private static final NodeId BUILD_NUMBER = NodeId.parseNodeId("i=2265");
   private static final NodeId BUILD_DATE = NodeId.parseNodeId("i=2266");
   BOpcUaDevice device = null;
   BOpcUaNetwork network = null;

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
      if (complex instanceof BOpcUaDevice) {
         this.device = (BOpcUaDevice)complex;
         this.network = this.device.getOpcUaClientNetwork();
      }
   }

   public IFuture post(Action action, BValue argument, Context cx) {
      BComplex complex = this.getParent();
      if (complex instanceof BOpcUaDevice) {
         this.device = (BOpcUaDevice)complex;
         this.network = this.device.getOpcUaClientNetwork();
         return this.network.postAsync(new Invocation(this, action, argument, cx));
      } else {
         return super.post(action, argument, cx);
      }
   }

   public void subscribed() {
      this.update();
   }

   public void doUpdate() {
      if (this.device != null) {
         UaClient uaClient = this.device.uaClient;
         BServerState state = this.device.getServerState();
         if (state.equals(BServerState.Running)) {
            try {
               this.setProductName(OpcUaClientUtil.readValue(uaClient, PRODUCT_NAME, "-"));
               this.setProductUri(OpcUaClientUtil.readValue(uaClient, PRODUCT_URI, "-"));
               this.setManufacturer(OpcUaClientUtil.readValue(uaClient, MANUFACTURER, "-"));
               this.setSoftwareVersion(OpcUaClientUtil.readValue(uaClient, SOFTWARE_VERSION, "-"));
               this.setBuildNumber(OpcUaClientUtil.readValue(uaClient, BUILD_NUMBER, "-"));
               this.setBuildDate(OpcUaClientUtil.readValue(uaClient, BUILD_DATE, "-"));
            } catch (Exception var4) {
               logger.log(Level.SEVERE, "Unable to set Build Info: " + var4);
            }
         }
      }
   }
}
