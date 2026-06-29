package com.tridium.modbusAsync;

import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.enums.BDeviceDataModeEnum;
import com.tridium.sys.station.Station;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "modbusDataMode",
   type = "BDeviceDataModeEnum",
   defaultValue = "BDeviceDataModeEnum.useNetworkDataMode"
)
public class BModbusAsyncDevice extends BModbusClientDevice {
   public static final Property modbusDataMode = newProperty(0, BDeviceDataModeEnum.useNetworkDataMode, null);
   public static final Type TYPE = Sys.loadType(BModbusAsyncDevice.class);

   public BDeviceDataModeEnum getModbusDataMode() {
      return (BDeviceDataModeEnum)this.get(modbusDataMode);
   }

   public void setModbusDataMode(BDeviceDataModeEnum v) {
      this.set(modbusDataMode, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Type getNetworkType() {
      return BModbusAsyncNetwork.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusAsyncNetwork || parent instanceof BModbusAsyncDeviceFolder;
   }

   public Message sendModbusMessage(Message msg) {
      BModbusAsyncNetwork net = (BModbusAsyncNetwork)this.modbusNet();
      if (net == null) {
         return null;
      } else {
         this.incrementRequest();
         BValue saveDebug = this.get("saveDebug");
         if (saveDebug != null && saveDebug instanceof BBoolean && ((BBoolean)saveDebug).getBoolean() && Station.inSave) {
            long ticks = Clock.ticks();
            System.out.println("sendModbusMessage:: Wait for station save to complete");

            while (Station.inSave && Clock.ticks() - ticks < 10000L) {
               try {
                  Thread.sleep(100L);
               } catch (InterruptedException var7) {
                  var7.printStackTrace();
               }
            }

            if (Station.inSave && Clock.ticks() - ticks > 10000L) {
               System.out.println("sendModbusMessage:: Gave up on station stave to complete");
            }
         }

         Message rspMsg = net.sendSync(msg);
         if (rspMsg == null) {
            this.incrementTimeouts();
         }

         return rspMsg;
      }
   }

   public void doPing() {
      if (!((BModbusAsyncNetwork)this.modbusNet()).getSnifferMode()) {
         super.doPing();
      }
   }

   public int getModbusMode() {
      int retValue = 0;
      switch (this.getModbusDataMode().getOrdinal()) {
         case 0:
            retValue = ((BModbusAsyncNetwork)this.getNetwork()).getModbusMode();
            break;
         case 2:
            retValue = 1;
      }

      return retValue;
   }
}
