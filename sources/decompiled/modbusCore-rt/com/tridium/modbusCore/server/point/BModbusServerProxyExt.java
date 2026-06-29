package com.tridium.modbusCore.server.point;

import com.tridium.basicdriver.util.BIBasicPollable;
import com.tridium.modbusCore.ModbusErrorCodes;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BRegisterTypesEnum;
import com.tridium.modbusCore.point.BModbusProxyExt;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BModbusServerProxyExt extends BModbusProxyExt implements ModbusErrorCodes, BIBasicPollable {
   public static final Type TYPE = Sys.loadType(BModbusServerProxyExt.class);
   protected boolean configFault = false;
   BStatusValue lastReadValue = null;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BReadWriteMode getMode() {
      return this.getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
   }

   public Type getDeviceExtType() {
      return BModbusServerPointDeviceExt.TYPE;
   }

   public boolean write(Context cx) throws Exception {
      return this.configFault ? false : super.write(cx);
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop.equals(dataAddress)) {
            this.setStale(true, null);
            this.checkConfiguration();
            if (this.getParentPoint().isWritablePoint()) {
               this.getTuning().writeDesired();
            }
         }
      }
   }

   protected void convertDeviceToProxy(BStatusValue deviceValue, BStatusValue proxyValue) {
      this.lastReadValue = deviceValue;
      super.convertDeviceToProxy(deviceValue, proxyValue);
   }

   protected final void checkConfiguration() {
      if (!this.isValidAddress(this.getDataAddress())) {
         this.configFault = true;
         String errMsg = "Illegal Modbus address " + this.getDataAddress() + " for this point's configuration.";
         this.readFail(errMsg);
         if (Sys.atSteadyState() && this.modbusNet() != null) {
            this.modbusNet()
               .getModbusLog()
               .error("Illegal Modbus address for point " + this.getParent().getName() + ": Modbus Address does not match Object type.");
         }
      } else {
         this.configFault = false;
      }
   }

   protected boolean isValidAddress(BFlexAddress addr) {
      return addr.isValid();
   }

   public abstract BRegisterTypesEnum determineRegisterType();
}
