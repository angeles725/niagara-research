package com.tridium.modbusTcp;

import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.client.BModbusClientDevice;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BModbusTcpGatewayDevice extends BModbusClientDevice {
   public static final Type TYPE = Sys.loadType(BModbusTcpGatewayDevice.class);

   public Type getType() {
      return TYPE;
   }

   public Type getNetworkType() {
      return BModbusTcpGateway.TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusTcpGateway || parent instanceof BModbusTcpGatewayDeviceFolder;
   }

   public int getModbusMode() {
      return 2;
   }

   public Message sendModbusMessage(Message msg) {
      BModbusTcpGateway gateway = (BModbusTcpGateway)this.modbusNet();
      if (gateway != null) {
         this.incrementRequest();
         Message rspMsg = gateway.sendSync(msg);
         if (rspMsg == null) {
            this.incrementTimeouts();
         }

         return rspMsg;
      } else {
         return null;
      }
   }
}
