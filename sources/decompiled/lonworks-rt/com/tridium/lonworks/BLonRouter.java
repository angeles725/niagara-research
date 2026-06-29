package com.tridium.lonworks;

import com.tridium.lonworks.datatypes.BLonRouteTable;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import com.tridium.lonworks.util.NmUtil;
import com.tridium.lonworks.util.RouterUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.sys.Action;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BIcon;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 75
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 67
   ), @NiagaraProperty(
      name = "routerType",
      type = "BLonRouterType",
      defaultValue = "BLonRouterType.unknown"
   ), @NiagaraProperty(
      name = "routerMode",
      type = "BLonRouterMode",
      defaultValue = "BLonRouterMode.unknown"
   ), @NiagaraProperty(
      name = "nearDeviceData",
      type = "BDeviceData",
      defaultValue = "new BDeviceData()"
   ), @NiagaraProperty(
      name = "farDeviceData",
      type = "BDeviceData",
      defaultValue = "new BDeviceData()"
   ), @NiagaraProperty(
      name = "nearSubnetTable",
      type = "BLonRouteTable",
      defaultValue = "BLonRouteTable.DEFAULT"
   ), @NiagaraProperty(
      name = "farSubnetTable",
      type = "BLonRouteTable",
      defaultValue = "BLonRouteTable.DEFAULT"
   ), @NiagaraProperty(
      name = "nearGroupTable",
      type = "BLonRouteTable",
      defaultValue = "BLonRouteTable.DEFAULT"
   ), @NiagaraProperty(
      name = "farGroupTable",
      type = "BLonRouteTable",
      defaultValue = "BLonRouteTable.DEFAULT"
   )})
@NiagaraActions({@NiagaraAction(
      name = "setType",
      parameterType = "BLonRouterType",
      defaultValue = "BLonRouterType.unknown"
   ), @NiagaraAction(
      name = "reset"
   )})
public class BLonRouter extends BComponent implements BIStatus {
   public static final Property status = newProperty(75, BStatus.ok, null);
   public static final Property faultCause = newProperty(67, "", null);
   public static final Property routerType = newProperty(0, BLonRouterType.unknown, null);
   public static final Property routerMode = newProperty(0, BLonRouterMode.unknown, null);
   public static final Property nearDeviceData = newProperty(0, new BDeviceData(), null);
   public static final Property farDeviceData = newProperty(0, new BDeviceData(), null);
   public static final Property nearSubnetTable = newProperty(0, BLonRouteTable.DEFAULT, null);
   public static final Property farSubnetTable = newProperty(0, BLonRouteTable.DEFAULT, null);
   public static final Property nearGroupTable = newProperty(0, BLonRouteTable.DEFAULT, null);
   public static final Property farGroupTable = newProperty(0, BLonRouteTable.DEFAULT, null);
   public static final Action setType = newAction(0, BLonRouterType.unknown, null);
   public static final Action reset = newAction(0, null);
   public static final Type TYPE = Sys.loadType(BLonRouter.class);
   private LonComm loncomm = null;
   protected Logger log = null;
   private BLonNetwork lonNetwork = null;
   private static final BIcon icon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/router.png");

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BLonRouterType getRouterType() {
      return (BLonRouterType)this.get(routerType);
   }

   public void setRouterType(BLonRouterType v) {
      this.set(routerType, v, null);
   }

   public BLonRouterMode getRouterMode() {
      return (BLonRouterMode)this.get(routerMode);
   }

   public void setRouterMode(BLonRouterMode v) {
      this.set(routerMode, v, null);
   }

   public BDeviceData getNearDeviceData() {
      return (BDeviceData)this.get(nearDeviceData);
   }

   public void setNearDeviceData(BDeviceData v) {
      this.set(nearDeviceData, v, null);
   }

   public BDeviceData getFarDeviceData() {
      return (BDeviceData)this.get(farDeviceData);
   }

   public void setFarDeviceData(BDeviceData v) {
      this.set(farDeviceData, v, null);
   }

   public BLonRouteTable getNearSubnetTable() {
      return (BLonRouteTable)this.get(nearSubnetTable);
   }

   public void setNearSubnetTable(BLonRouteTable v) {
      this.set(nearSubnetTable, v, null);
   }

   public BLonRouteTable getFarSubnetTable() {
      return (BLonRouteTable)this.get(farSubnetTable);
   }

   public void setFarSubnetTable(BLonRouteTable v) {
      this.set(farSubnetTable, v, null);
   }

   public BLonRouteTable getNearGroupTable() {
      return (BLonRouteTable)this.get(nearGroupTable);
   }

   public void setNearGroupTable(BLonRouteTable v) {
      this.set(nearGroupTable, v, null);
   }

   public BLonRouteTable getFarGroupTable() {
      return (BLonRouteTable)this.get(farGroupTable);
   }

   public void setFarGroupTable(BLonRouteTable v) {
      this.set(farGroupTable, v, null);
   }

   public void setType(BLonRouterType parameter) {
      this.invoke(setType, parameter, null);
   }

   public void reset() {
      this.invoke(reset, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isConfigOnline() {
      return this.getNearDeviceData().getNodeState() == BLonNodeState.configOnline;
   }

   public boolean isConfigOffline() {
      return this.getNearDeviceData().getNodeState() == BLonNodeState.configOffline;
   }

   public boolean isConfigured() {
      return this.isConfigOnline() || this.isConfigOffline();
   }

   public boolean authenticate() {
      return this.getNearDeviceData().getAuthenticate();
   }

   public void started() throws Exception {
      super.started();
      BComplex p = this.getParent();

      while (p != null && !(p instanceof BLonNetwork)) {
         p = p.getParent();
      }

      if (p != null) {
         this.lonNetwork = (BLonNetwork)p;
         this.loncomm = this.lonNetwork.lonComm();
         ((NAddressManager)this.lonNetwork.addressManager()).registerLonRouter(this);
      }
   }

   public void stopped() throws Exception {
      super.started();
      ((NAddressManager)this.lonNetwork.addressManager()).unregisterLonRouter(this);
   }

   public void doSetType(BLonRouterType param) {
      this.setRouterType(param);
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (!BLonNetwork.lonNoWrite.equals(context)) {
            if (prop == routerType) {
               this.lonNetwork().postAsync(new BLonRouter.asyncRouterUpdate(this, this.getRouterType()));
            } else if (prop == routerMode) {
               this.lonNetwork().postAsync(new BLonRouter.asyncRouterUpdate(this, this.getRouterMode()));
            }
         }
      }
   }

   public void doReset() {
      try {
         NmUtil.resetNode(this);
      } catch (Throwable var2) {
         this.log().log(Level.SEVERE, "Exception in LonRouter.reset()", var2);
      }
   }

   public BNeuronId getNeuronIdAddress() {
      return this.getNearDeviceData().getNeuronId();
   }

   public LonComm lonComm() {
      return this.loncomm;
   }

   public Logger log() {
      if (this.log == null) {
         this.log = this.lonNetwork().log();
      }

      return this.log;
   }

   public BLonNetwork lonNetwork() {
      return this.lonNetwork;
   }

   public BIcon getIcon() {
      return icon;
   }

   private static class asyncRouterUpdate implements Runnable {
      BLonRouter rtr;
      BValue obj;

      asyncRouterUpdate(BLonRouter rtr, BValue obj) {
         this.rtr = rtr;
         this.obj = obj;
      }

      @Override
      public void run() {
         try {
            if (this.obj.getType() == BLonRouterType.TYPE) {
               RouterUtil.setRouterType(this.rtr, (BLonRouterType)this.obj);
            } else if (this.obj.getType() == BLonRouterMode.TYPE) {
               RouterUtil.setRouterMode(this.rtr, (BLonRouterMode)this.obj);
            }
         } catch (LonException var2) {
            this.rtr.log().log(Level.SEVERE, "Unable to set router type or mode " + this.rtr.getDisplayName(null), (Throwable)var2);
         }
      }
   }
}
