package com.tridium.bacnet.stack.network;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.timers.Timers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "dnet",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "routerAddress",
      type = "BBacnetAddress",
      defaultValue = "BBacnetAddress.DEFAULT"
   ), @NiagaraProperty(
      name = "portId",
      type = "int",
      defaultValue = "-1",
      flags = 1
   ), @NiagaraProperty(
      name = "portInfo",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "routerStatus",
      type = "BRouterStatus",
      defaultValue = "BRouterStatus.unknown",
      flags = 1
   )})
public class BBacnetRouterEntry extends BStruct {
   public static final Property dnet = newProperty(0, -1, null);
   public static final Property routerAddress = newProperty(0, BBacnetAddress.DEFAULT, null);
   public static final Property portId = newProperty(1, -1, null);
   public static final Property portInfo = newProperty(1, "", null);
   public static final Property routerStatus = newProperty(1, BRouterStatus.unknown, null);
   public static final Type TYPE = Sys.loadType(BBacnetRouterEntry.class);
   public static final int BUSY_TIMEOUT = 30000;
   private int busyTimerId;
   long lastUpdateTicks = 0L;

   public int getDnet() {
      return this.getInt(dnet);
   }

   public void setDnet(int v) {
      this.setInt(dnet, v, null);
   }

   public BBacnetAddress getRouterAddress() {
      return (BBacnetAddress)this.get(routerAddress);
   }

   public void setRouterAddress(BBacnetAddress v) {
      this.set(routerAddress, v, null);
   }

   public int getPortId() {
      return this.getInt(portId);
   }

   public void setPortId(int v) {
      this.setInt(portId, v, null);
   }

   public String getPortInfo() {
      return this.getString(portInfo);
   }

   public void setPortInfo(String v) {
      this.setString(portInfo, v, null);
   }

   public BRouterStatus getRouterStatus() {
      return (BRouterStatus)this.get(routerStatus);
   }

   public void setRouterStatus(BRouterStatus v) {
      this.set(routerStatus, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetRouterEntry() {
      this.lastUpdateTicks = Clock.ticks();
   }

   public BBacnetRouterEntry(int dnet, BBacnetAddress routerAddress, int portId, String portInfo, BRouterStatus routerStatus) {
      this.lastUpdateTicks = Clock.ticks();
      this.setDnet(dnet);
      this.setRouterAddress(routerAddress);
      this.setPortId(portId);
      this.setPortInfo(portInfo == null ? "" : portInfo);
      this.setRouterStatus(routerStatus);
   }

   public String toString(Context cx) {
      return "" + this.getPortId() + ":" + this.getRouterAddress() + "->" + this.getDnet() + "-" + this.getRouterStatus();
   }

   public boolean isDisconnected() {
      return this.getRouterStatus() == BRouterStatus.routerNotConnected;
   }

   public boolean isUnavailable() {
      return this.getRouterStatus() == BRouterStatus.routerUnavailable;
   }

   public boolean isBusy() {
      return this.getRouterStatus() == BRouterStatus.routerBusy;
   }

   public void readNetworkBytes(ByteArrayInputStream is) throws IOException {
      int dnet = is.read() << 8;
      dnet |= is.read();
      this.setDnet(dnet);
      this.setPortId(is.read());
      int portInfoLength = is.read();
      byte[] portInfo = new byte[portInfoLength];
      is.read(portInfo);
      this.setPortInfo(new String(portInfo));
   }

   public void writeNetworkBytes(ByteArrayOutputStream os) {
      int dnet = this.getDnet();
      os.write(dnet >> 8);
      os.write(dnet & 0xFF);
      os.write(this.getPortId());
      byte[] portInfo = this.getPortInfo().getBytes();
      if (portInfo == null) {
         os.write(0);
      } else {
         os.write(portInfo.length);
         os.write(portInfo, 0, portInfo.length);
      }
   }

   public void startBusyTimer() {
      Timers.cancel(this.busyTimerId);
      this.busyTimerId = Timers.add(((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getNetwork(), 30000L, this);
   }

   public void stopBusyTimer() {
      Timers.cancel(this.busyTimerId);
   }
}
