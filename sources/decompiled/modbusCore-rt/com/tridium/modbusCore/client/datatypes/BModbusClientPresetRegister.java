package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.MessageListener;
import com.tridium.basicdriver.message.Message;
import com.tridium.fox.sys.BSysChannel;
import com.tridium.modbusCore.messages.ModbusResponse;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.BComponentSpace;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFloat;
import javax.baja.sys.BNumber;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.IPropertyValidator;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Validatable;
import javax.baja.util.Version;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "value",
      type = "BNumber",
      defaultValue = "BDouble.make(0.0)"
   ), @NiagaraProperty(
      name = "lastSuccessfulWrite",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "lastFailedWrite",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "writeStatus",
      type = "BCommStatus",
      defaultValue = "new BCommStatus(ModbusMessageConst.OK_NOT_ACTIVE)",
      flags = 67
   )})
public class BModbusClientPresetRegister extends BComponent implements MessageListener, IPropertyValidator {
   public static final Property value = newProperty(0, BDouble.make(0.0), null);
   public static final Property lastSuccessfulWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property lastFailedWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property writeStatus = newProperty(67, new BCommStatus(-2), null);
   public static final Type TYPE = Sys.loadType(BModbusClientPresetRegister.class);
   private boolean lastChild = false;

   public BNumber getValue() {
      return (BNumber)this.get(value);
   }

   public void setValue(BNumber v) {
      this.set(value, v, null);
   }

   public BAbsTime getLastSuccessfulWrite() {
      return (BAbsTime)this.get(lastSuccessfulWrite);
   }

   public void setLastSuccessfulWrite(BAbsTime v) {
      this.set(lastSuccessfulWrite, v, null);
   }

   public BAbsTime getLastFailedWrite() {
      return (BAbsTime)this.get(lastFailedWrite);
   }

   public void setLastFailedWrite(BAbsTime v) {
      this.set(lastFailedWrite, v, null);
   }

   public BCommStatus getWriteStatus() {
      return (BCommStatus)this.get(writeStatus);
   }

   public void setWriteStatus(BCommStatus v) {
      this.set(writeStatus, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BModbusClientPresetRegisters;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(value)) {
            BModbusClientPresetRegisters parent = (BModbusClientPresetRegisters)this.getParent();
            if (parent.getWriteOnInputChange()) {
               parent.write();
            }
         }
      }
   }

   public void processMessage(Message response) {
      int exceptionCode = 0;
      if (response == null) {
         exceptionCode = 9;
      } else {
         exceptionCode = ((ModbusResponse)response).exceptionCode;
      }

      BCommStatus tmp = new BCommStatus(exceptionCode);
      this.getWriteStatus().setErrorCode(tmp.getErrorCode());
      this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
      if (exceptionCode != 0 && exceptionCode != 5) {
         this.setLastFailedWrite(Clock.time());
      } else {
         this.setLastSuccessfulWrite(Clock.time());
      }

      ((BModbusClientPresetRegisters)this.getParent()).computeStatus(this.lastChild);
   }

   public void setLastChild(boolean lastChild) {
      this.lastChild = lastChild;
   }

   public String toString(Context context) {
      return this.propertyValueToString(value, context) + " {" + this.propertyValueToString(writeStatus, context) + "}";
   }

   public void validateSet(Validatable validatable, Context context) {
   }

   public BValue adjustPendingSetValue(BComplex instance, Property property, BValue value, Context context) {
      if (property.equals(BModbusClientPresetRegister.value) && value instanceof BNumber && this.isFloatTypeRequired()) {
         BNumber numberValue = (BNumber)value;
         return BFloat.make(numberValue.getFloat());
      } else {
         return value;
      }
   }

   public BValue[] adjustPendingSetValues(BComplex instance, Property[] properties, BValue[] values, Context context) {
      for (int i = 0; i < properties.length; i++) {
         values[i] = this.adjustPendingSetValue(instance, properties[i], values[i], context);
      }

      return values;
   }

   public IPropertyValidator getPropertyValidator(Property property, Context context) {
      return this;
   }

   public IPropertyValidator getPropertyValidator(Property[] properties, Context context) {
      return this;
   }

   public boolean isTransactionValidationAllowed() {
      return true;
   }

   private boolean isFloatTypeRequired() {
      BComponentSpace space = this.getComponentSpace();
      if (space != null && space.isProxyComponentSpace()) {
         Version remoteVersion = (Version)this.fw(404, "modbusCore", null, null, null);
         return remoteVersion.compareTo(BSysChannel.VER_4_14) < 0;
      } else {
         return false;
      }
   }
}
