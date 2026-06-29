package com.tridium.modbusCore;

import com.tridium.basicdriver.BBasicDevice;
import com.tridium.modbusCore.datatypes.BModbusConfig;
import com.tridium.modbusCore.enums.BDataByteOrder64BitEnum;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import javax.baja.driver.point.BPointDeviceExt;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BFacets;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceAddress",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(null, 1, 247)")}
   ), @NiagaraProperty(
      name = "modbusConfig",
      type = "BModbusConfig",
      defaultValue = "new BModbusConfig()"
   )})
public abstract class BModbusDevice extends BBasicDevice {
   public static final Property deviceAddress = newProperty(0, 1, BFacets.makeInt(null, 1, 247));
   public static final Property modbusConfig = newProperty(0, new BModbusConfig(), null);
   public static final Type TYPE = Sys.loadType(BModbusDevice.class);
   private long totalMessages = 0L;
   private long totalCrcErrors = 0L;
   private long totalLrcErrors = 0L;
   private long totalTransactionIdErrors = 0L;
   private long timeouts = 0L;

   public int getDeviceAddress() {
      return this.getInt(deviceAddress);
   }

   public void setDeviceAddress(int v) {
      this.setInt(deviceAddress, v, null);
   }

   public BModbusConfig getModbusConfig() {
      return (BModbusConfig)this.get(modbusConfig);
   }

   public void setModbusConfig(BModbusConfig v) {
      this.set(modbusConfig, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusDevice() {
      this.setFlags(upload, 4);
      this.setFlags(download, 4);
   }

   public BDataByteOrderEnum getLongDataByteOrder() {
      return this.getModbusConfig().getOverrideNetwork() ? this.getModbusConfig().getLongByteOrder() : ((BModbusNetwork)this.getNetwork()).getLongByteOrder();
   }

   public BDataByteOrderEnum getFloatDataByteOrder() {
      return this.getModbusConfig().getOverrideNetwork() ? this.getModbusConfig().getFloatByteOrder() : ((BModbusNetwork)this.getNetwork()).getFloatByteOrder();
   }

   public BDataByteOrder64BitEnum getDouble64BitDataByteOrder() {
      return this.getModbusConfig().getOverrideNetwork()
         ? this.getModbusConfig().getDouble64BitByteOrder()
         : ((BModbusNetwork)this.getNetwork()).getDouble64BitByteOrder();
   }

   public BDataByteOrder64BitEnum getLong64BitDataByteOrder() {
      return this.getModbusConfig().getOverrideNetwork()
         ? this.getModbusConfig().getLong64BitByteOrder()
         : ((BModbusNetwork)this.getNetwork()).getLong64BitByteOrder();
   }

   public BModbusNetwork modbusNet() {
      return (BModbusNetwork)this.getNetwork();
   }

   public abstract BPointDeviceExt getPointDeviceExt();

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("ModbusDevice", 2);
      out.prop("Total Timeouts", this.timeouts);
      out.prop("Total CRC Errors", this.totalCrcErrors);
      out.prop("Total LRC Errors", this.totalLrcErrors);
      out.prop("Total Transaction ID Errors", this.totalTransactionIdErrors);
      out.endProps();
   }

   public final void incrementCrcErrors() {
      this.totalCrcErrors++;
      if (this.modbusNet() != null) {
         this.modbusNet().incrementCrcErrors();
      }
   }

   public final void incrementLrcErrors() {
      this.totalLrcErrors++;
      if (this.modbusNet() != null) {
         this.modbusNet().incrementLrcErrors();
      }
   }

   public final void incrementTransactionIdErrors() {
      this.totalTransactionIdErrors++;
      if (this.modbusNet() != null) {
         this.modbusNet().incrementTransactionIdErrors();
      }
   }

   public int getModbusMode() {
      return 0;
   }

   public void incrementRequest() {
      this.totalMessages++;
   }

   public void incrementTimeouts() {
      this.timeouts++;
   }

   public long getTotalMessages() {
      return this.totalMessages;
   }

   public long getTotalCrcErrors() {
      return this.totalCrcErrors;
   }

   public long getTotalLrcErrors() {
      return this.totalLrcErrors;
   }

   public long getTotalTransactionIdErrors() {
      return this.totalTransactionIdErrors;
   }

   public long getTotalTimeouts() {
      return this.timeouts;
   }
}
