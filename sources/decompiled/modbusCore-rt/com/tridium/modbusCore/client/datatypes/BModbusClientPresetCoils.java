package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.MessageListener;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "addPresetCoilValue",
   parameterType = "BModbusClientPresetCoil",
   defaultValue = "new BModbusClientPresetCoil()"
)
public class BModbusClientPresetCoils extends BModbusClientPresetComponent implements MessageListener {
   public static final Action addPresetCoilValue = newAction(0, new BModbusClientPresetCoil(), null);
   public static final Type TYPE = Sys.loadType(BModbusClientPresetCoils.class);

   public void addPresetCoilValue(BModbusClientPresetCoil parameter) {
      this.invoke(addPresetCoilValue, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void writePresetValues() {
      int address = this.getDevice().getDeviceAddress();
      int baseAddress = this.getAbsoluteStartingAddress().getDataAddress();
      boolean isForceMultiple = this.getDevice().isForceMultiple();
      BModbusClientPresetCoil[] kids = (BModbusClientPresetCoil[])this.getChildren(BModbusClientPresetCoil.class);
      if (kids != null && kids.length > 0) {
         int count = kids.length;
         byte[] dataOut = null;
         if (isForceMultiple) {
            dataOut = new byte[(count + 7) / 8];
         }

         for (int i = 0; i < count; i++) {
            BModbusClientPresetCoil kid = kids[i];
            boolean bValue = kid.getValue();
            if (!isForceMultiple) {
               int pointAddress = baseAddress + i;
               dataOut = new byte[]{0, 0};
               int code = 5;
               if (bValue) {
                  dataOut[0] = -1;
               }

               ModbusWriteRequest req = new ModbusWriteRequest(this.getNetwork().getModbusMode(), this.getDevice(), address, code, pointAddress, 1, dataOut);
               kid.setLastChild(i == count - 1);
               this.getNetwork().postWrite(new BModbusClientPresetComponent.ModbusClientWriteRequest(req, kid));
            } else if (bValue) {
               int setBit = 1;
               setBit <<= i % 8;
               dataOut[i / 8] = (byte)(dataOut[i / 8] | setBit);
            }
         }

         if (isForceMultiple) {
            int code = 15;
            ModbusWriteRequest req = new ModbusWriteRequest(this.getNetwork().getModbusMode(), this.getDevice(), address, code, baseAddress, count, dataOut);
            this.getNetwork().postWrite(new BModbusClientPresetComponent.ModbusClientWriteRequest(req, this));
         }
      }
   }

   @Override
   protected Property getBaseAddressProperty() {
      return BModbusClientDevice.coilStatusBaseAddress;
   }

   @Override
   protected boolean isValidAddress(BFlexAddress address) {
      if (!address.isModbusFormat()) {
         return address.isValid();
      } else {
         return address.isModbusDigitalAddress() ? address.isModbusCoilAddress() : false;
      }
   }

   public void doAddPresetCoilValue(BModbusClientPresetCoil param) {
      this.add(null, param);
      BComponentSpace space = this.getComponentSpace();
      if (space != null) {
         space.update(this, 0);
      }
   }

   public void processMessage(Message response) {
      int exceptionCode = 0;
      if (response == null) {
         exceptionCode = 9;
      } else {
         exceptionCode = ((ModbusResponse)response).exceptionCode;
      }

      this.setStatusFault(exceptionCode != 0 && exceptionCode != -2 && exceptionCode != 5);
      BCommStatus tmp = new BCommStatus(exceptionCode);
      BAbsTime timestamp = Clock.time();
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BModbusClientPresetCoil.class)) {
         BModbusClientPresetCoil kid = (BModbusClientPresetCoil)c.get();
         kid.getWriteStatus().setErrorCode(tmp.getErrorCode());
         kid.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
         if (exceptionCode != 0 && exceptionCode != 5) {
            kid.setLastFailedWrite(timestamp);
         } else {
            kid.setLastSuccessfulWrite(timestamp);
         }
      }

      if (exceptionCode == 0 || exceptionCode == 5) {
         this.fireWriteSuccessful(null);
         if (this.getNetwork().getModbusLog().isTraceOn()) {
            this.getNetwork().getModbusLog().trace(this.getName() + " write successful.");
         }
      } else if (this.getNetwork().getModbusLog().isTraceOn()) {
         this.getNetwork().getModbusLog().trace(this.getName() + " write unsuccessful.");
      }
   }

   public void computeStatus(boolean fireSuccessful) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BModbusClientPresetCoil.class)) {
         BModbusClientPresetCoil kid = (BModbusClientPresetCoil)c.get();
         int errorCode = kid.getWriteStatus().getErrorCode();
         if (errorCode != 0 && errorCode != -2 && errorCode != 5) {
            this.setStatusFault(true);
            if (fireSuccessful && this.getNetwork().getModbusLog().isTraceOn()) {
               this.getNetwork().getModbusLog().trace(this.getName() + " write unsuccessful.");
            }

            return;
         }
      }

      this.setStatusFault(false);
      if (fireSuccessful) {
         this.fireWriteSuccessful(null);
         if (this.getNetwork().getModbusLog().isTraceOn()) {
            this.getNetwork().getModbusLog().trace(this.getName() + " write successful.");
         }
      }
   }
}
