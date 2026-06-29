package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.MessageListener;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.datatypes.BFlexAddress;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import com.tridium.modbusCore.enums.BDataTypeEnum;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteRequest;
import com.tridium.modbusCore.util.ByteConverterUtil;
import com.tridium.modbusCore.util.DataTypeUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.ICoalesceable;

@NiagaraType
@NiagaraProperty(
   name = "dataType",
   type = "BDataTypeEnum",
   defaultValue = "BDataTypeEnum.integerType"
)
@NiagaraAction(
   name = "addPresetRegisterValue",
   parameterType = "BModbusClientPresetRegister",
   defaultValue = "new BModbusClientPresetRegister()"
)
public class BModbusClientPresetRegisters extends BModbusClientPresetComponent implements MessageListener {
   public static final Property dataType = newProperty(0, BDataTypeEnum.integerType, null);
   public static final Action addPresetRegisterValue = newAction(0, new BModbusClientPresetRegister(), null);
   public static final Type TYPE = Sys.loadType(BModbusClientPresetRegisters.class);

   public BDataTypeEnum getDataType() {
      return (BDataTypeEnum)this.get(dataType);
   }

   public void setDataType(BDataTypeEnum v) {
      this.set(dataType, v, null);
   }

   public void addPresetRegisterValue(BModbusClientPresetRegister parameter) {
      this.invoke(addPresetRegisterValue, parameter, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void writePresetValues() {
      ByteArrayOutputStream oStream = new ByteArrayOutputStream();
      int address = this.getDevice().getDeviceAddress();
      int baseAddress = this.getAbsoluteStartingAddress().getDataAddress();
      boolean isPresetMultiple = this.getDevice().isPresetMultiple();
      BModbusClientPresetRegister[] kids = (BModbusClientPresetRegister[])this.getChildren(BModbusClientPresetRegister.class);
      if (kids != null && kids.length > 0) {
         for (int i = 0; i < kids.length; i++) {
            BModbusClientPresetRegister kid = kids[i];
            double value = kid.getValue().getDouble();
            byte[] dataOut;
            if (this.isDataTypeInteger()) {
               dataOut = this.setIntegerByteArray((float)value);
            } else if (this.isDataTypeLong()) {
               dataOut = this.setLongByteArray((float)value);
            } else if (this.isDataType64BitLong()) {
               dataOut = ByteConverterUtil.to8ByteLongArray(value, this.getDevice().getLong64BitDataByteOrder(), this.isDataTypeSigned());
            } else if (this.isDataTypeDouble()) {
               dataOut = ByteConverterUtil.to8ByteDoubleArray(value, this.getDevice().getDouble64BitDataByteOrder());
            } else {
               dataOut = this.setFloatByteArray((float)value);
            }

            if (!isPresetMultiple) {
               int count = DataTypeUtil.getRegisterCount(this.getDataType());
               int pointAddress = baseAddress + i * count;
               int code = 6;
               ModbusWriteRequest req = new ModbusWriteRequest(this.getNetwork().getModbusMode(), this.getDevice(), address, code, pointAddress, 1, dataOut);
               if (count > 1) {
                  this.getNetwork().postWrite(new BModbusClientPresetRegisters.ModbusWriteFloatRequest(kid, req, dataOut, i == kids.length - 1));
               } else {
                  kid.setLastChild(i == kids.length - 1);
                  this.getNetwork().postWrite(new BModbusClientPresetComponent.ModbusClientWriteRequest(req, kid));
               }
            } else {
               try {
                  oStream.write(dataOut);
               } catch (IOException var15) {
               }
            }
         }

         if (isPresetMultiple) {
            int count = kids.length;
            if (this.isDataTypeLong() || this.isDataTypeFloat()) {
               count *= 2;
            }

            int code = 16;
            byte[] dataOutx = oStream.toByteArray();
            ModbusWriteRequest req = new ModbusWriteRequest(this.getNetwork().getModbusMode(), this.getDevice(), address, code, baseAddress, count, dataOutx);
            this.getNetwork().postWrite(new BModbusClientPresetComponent.ModbusClientWriteRequest(req, this));
         }
      }
   }

   @Override
   protected Property getBaseAddressProperty() {
      return BModbusClientDevice.holdingRegisterBaseAddress;
   }

   @Override
   protected boolean isValidAddress(BFlexAddress address) {
      if (!address.isModbusFormat()) {
         return address.isValid();
      } else {
         return address.isModbusAnalogAddress() ? address.isModbusHoldingAddress() : false;
      }
   }

   public void doAddPresetRegisterValue(BModbusClientPresetRegister param) {
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

      while (c.next(BModbusClientPresetRegister.class)) {
         BModbusClientPresetRegister kid = (BModbusClientPresetRegister)c.get();
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

      while (c.next(BModbusClientPresetRegister.class)) {
         BModbusClientPresetRegister kid = (BModbusClientPresetRegister)c.get();
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

   private boolean isDataTypeInteger() {
      return DataTypeUtil.is16BitInteger(this.getDataType());
   }

   private boolean isDataTypeLong() {
      return DataTypeUtil.is32BitLong(this.getDataType());
   }

   private boolean isDataType64BitLong() {
      return DataTypeUtil.is64BitLong(this.getDataType());
   }

   private boolean isDataTypeDouble() {
      return DataTypeUtil.isDouble(this.getDataType());
   }

   private boolean isDataTypeFloat() {
      return DataTypeUtil.isFloat(this.getDataType());
   }

   private boolean isDataTypeSigned() {
      return DataTypeUtil.isSigned(this.getDataType());
   }

   private byte[] setIntegerByteArray(float fValue) {
      return ByteConverterUtil.to2ByteIntArray(fValue, this.isDataTypeSigned());
   }

   private byte[] setLongByteArray(float fValue) {
      BDataByteOrderEnum byteOrder = this.getDevice().getLongDataByteOrder();
      return ByteConverterUtil.to4ByteLongArray(fValue, byteOrder, true);
   }

   private byte[] setFloatByteArray(float fValue) {
      BDataByteOrderEnum byteOrder = this.getDevice().getFloatDataByteOrder();
      return ByteConverterUtil.to4ByteFloatArray(fValue, byteOrder);
   }

   private class ModbusWriteFloatRequest implements Runnable, ICoalesceable {
      private int hashCode;
      private BModbusClientPresetRegister source;
      private ModbusWriteRequest req;
      private byte[] dataOut;
      private boolean lastChild;

      public ModbusWriteFloatRequest(BModbusClientPresetRegister source, ModbusWriteRequest req, byte[] dataOut, boolean lastChild) {
         this.hashCode = source.hashCode();
         this.source = source;
         this.req = req;
         this.dataOut = dataOut;
         this.lastChild = lastChild;
      }

      @Override
      public void run() {
         this.source.setLastChild(false);
         this.source.processMessage(BModbusClientPresetRegisters.this.getDevice().sendModbusMessage(this.req));
         if (this.source.getWriteStatus().getErrorCode() == 0 || this.source.getWriteStatus().getErrorCode() == 5) {
            this.dataOut[0] = this.dataOut[2];
            this.dataOut[1] = this.dataOut[3];
            this.req.startAddress++;
            this.req.data = this.dataOut;
            this.source.setLastChild(this.lastChild);
            this.source.processMessage(BModbusClientPresetRegisters.this.getDevice().sendModbusMessage(this.req));
         }
      }

      @Override
      public int hashCode() {
         return this.hashCode;
      }

      @Override
      public boolean equals(Object object) {
         if (object instanceof BModbusClientPresetRegisters.ModbusWriteFloatRequest) {
            BModbusClientPresetRegisters.ModbusWriteFloatRequest o = (BModbusClientPresetRegisters.ModbusWriteFloatRequest)object;
            return this.source == o.source;
         } else {
            return false;
         }
      }

      public Object getCoalesceKey() {
         return this;
      }

      public ICoalesceable coalesce(ICoalesceable c) {
         return c;
      }
   }
}
