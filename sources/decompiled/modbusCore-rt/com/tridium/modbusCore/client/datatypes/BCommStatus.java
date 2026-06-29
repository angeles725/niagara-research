package com.tridium.modbusCore.client.datatypes;

import com.tridium.modbusCore.client.enums.BCommStatusEnum;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "errorCode",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "errorDescription",
      type = "BCommStatusEnum",
      defaultValue = "BCommStatusEnum.ok",
      flags = 1
   )})
public class BCommStatus extends BStruct implements ModbusMessageConst {
   public static final Property errorCode = newProperty(1, 0, null);
   public static final Property errorDescription = newProperty(1, BCommStatusEnum.ok, null);
   public static final Type TYPE = Sys.loadType(BCommStatus.class);

   public int getErrorCode() {
      return this.getInt(errorCode);
   }

   public void setErrorCode(int v) {
      this.setInt(errorCode, v, null);
   }

   public BCommStatusEnum getErrorDescription() {
      return (BCommStatusEnum)this.get(errorDescription);
   }

   public void setErrorDescription(BCommStatusEnum v) {
      this.set(errorDescription, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BCommStatus() {
   }

   public BCommStatus(int errorCode) {
      this.setErrorCode(errorCode);
      switch (errorCode) {
         case -8:
            this.setErrorDescription(BCommStatusEnum.disabled);
            break;
         case -7:
            this.setErrorDescription(BCommStatusEnum.fault);
            break;
         case -6:
            this.setErrorDescription(BCommStatusEnum.down);
            break;
         case -5:
            this.setErrorDescription(BCommStatusEnum.lrcError);
            break;
         case -4:
         default:
            this.setErrorDescription(BCommStatusEnum.otherError);
            break;
         case -3:
            this.setErrorDescription(BCommStatusEnum.unknown);
            break;
         case -2:
            this.setErrorDescription(BCommStatusEnum.okNotActive);
            break;
         case -1:
            this.setErrorDescription(BCommStatusEnum.crcError);
            break;
         case 0:
            this.setErrorDescription(BCommStatusEnum.ok);
            break;
         case 1:
            this.setErrorDescription(BCommStatusEnum.illegalFunction);
            break;
         case 2:
            this.setErrorDescription(BCommStatusEnum.illegalDataAddress);
            break;
         case 3:
            this.setErrorDescription(BCommStatusEnum.illegalDataValue);
            break;
         case 4:
            this.setErrorDescription(BCommStatusEnum.slaveDeviceFailure);
            break;
         case 5:
            this.setErrorDescription(BCommStatusEnum.acknowledge);
            break;
         case 6:
            this.setErrorDescription(BCommStatusEnum.slaveDeviceBusy);
            break;
         case 7:
            this.setErrorDescription(BCommStatusEnum.negativeAcknowledge);
            break;
         case 8:
            this.setErrorDescription(BCommStatusEnum.memoryParityError);
            break;
         case 9:
            this.setErrorDescription(BCommStatusEnum.deviceTimeout);
            break;
         case 10:
            this.setErrorDescription(BCommStatusEnum.gatewayPathUnavailable);
            break;
         case 11:
            this.setErrorDescription(BCommStatusEnum.gatewayTargetDeviceFailedToRespond);
      }
   }

   public BCommStatus(BCommStatus src) {
      this.copyFrom(src);
   }

   public String toDebugString() {
      return this.toString(null);
   }

   public String toString(Context context) {
      return this.getErrorCode() < 0 ? "" + this.getErrorDescription().getTag() : this.getErrorCode() + ": " + this.getErrorDescription().getTag();
   }
}
