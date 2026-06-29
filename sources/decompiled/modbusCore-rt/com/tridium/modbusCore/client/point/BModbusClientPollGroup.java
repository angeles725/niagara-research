package com.tridium.modbusCore.client.point;

import com.tridium.basicdriver.point.BBasicProxyExt;
import com.tridium.basicdriver.util.BBasicPollGroup;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.datatypes.BDevicePollConfigEntry;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusClientPollGroup extends BBasicPollGroup {
   public static final Type TYPE = Sys.loadType(BModbusClientPollGroup.class);

   public Type getType() {
      return TYPE;
   }

   public void poll() {
      Object obj = this.getCode();
      if (obj instanceof BDevicePollConfigEntry) {
         BDevicePollConfigEntry entry = (BDevicePollConfigEntry)obj;
         if (!entry.getEnabled()) {
            return;
         }

         BModbusClientDevice device = entry.getDevice();
         if (device == null) {
            BBasicProxyExt[] proxies = this.getSubscribedProxyExts();
            if (proxies != null) {
               for (int i = 0; i < proxies.length; i++) {
                  try {
                     this.readUnsubscribed(proxies[i]);
                  } catch (Exception var9) {
                     var9.printStackTrace();
                  }
               }
            }

            return;
         }

         int startAddress = entry.getStartAddress().getDataAddress();
         int numRegisters = entry.getConsecutivePointsToPoll();
         int minReadSize = entry.getReadGroupSize();

         try {
            if (entry.getDataType().equals(BRegisterTypesEnum.inputRegister)) {
               entry.setByteArray(device.readRegisters(4, startAddress, numRegisters, minReadSize, entry));
            } else if (entry.getDataType().equals(BRegisterTypesEnum.holdingRegister)) {
               entry.setByteArray(device.readRegisters(3, startAddress, numRegisters, minReadSize, entry));
            } else if (entry.getDataType().equals(BRegisterTypesEnum.discreteCoil)) {
               entry.setByteArray(device.readStatusRegisters(1, startAddress, numRegisters, entry));
            } else if (entry.getDataType().equals(BRegisterTypesEnum.discreteInput)) {
               entry.setByteArray(device.readStatusRegisters(2, startAddress, numRegisters, entry));
            }

            BBasicProxyExt[] proxies = this.getSubscribedProxyExts();
            if (proxies != null) {
               for (int i = 0; i < proxies.length; i++) {
                  ((BModbusClientProxyExt)proxies[i]).devicePoll(entry);
               }
            }
         } catch (NotRunningException var10) {
            throw var10;
         } catch (Exception var11) {
            device.getLogger().info("poll device timeout");
         }
      } else if (obj instanceof BModbusClientProxyExt) {
         ((BModbusClientProxyExt)obj).poll();
      }
   }

   public String toString(Context context) {
      Object obj = this.getCode();
      if (obj instanceof BModbusClientProxyExt) {
         return ((BModbusClientProxyExt)obj).getParentPoint().getDisplayName(context)
            + " ("
            + ((BModbusClientProxyExt)obj).getAbsoluteAddress().toString(context)
            + ")";
      } else {
         return obj instanceof BObject ? ((BObject)obj).toString(context) + " (MCPG)" : super.toString(context);
      }
   }
}
