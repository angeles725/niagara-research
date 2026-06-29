package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.MessageListener;
import com.tridium.basicdriver.message.Message;
import com.tridium.modbusCore.messages.ModbusResponse;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "value",
      type = "boolean",
      defaultValue = "false"
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
public class BModbusClientPresetCoil extends BComponent implements MessageListener {
   public static final Property value = newProperty(0, false, null);
   public static final Property lastSuccessfulWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property lastFailedWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property writeStatus = newProperty(67, new BCommStatus(-2), null);
   public static final Type TYPE = Sys.loadType(BModbusClientPresetCoil.class);
   private boolean lastChild = false;

   public boolean getValue() {
      return this.getBoolean(value);
   }

   public void setValue(boolean v) {
      this.setBoolean(value, v, null);
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
      return parent instanceof BModbusClientPresetCoils;
   }

   public void changed(Property property, Context context) {
      super.changed(property, context);
      if (this.isRunning()) {
         if (property.equals(value)) {
            BModbusClientPresetCoils parent = (BModbusClientPresetCoils)this.getParent();
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

      ((BModbusClientPresetCoils)this.getParent()).computeStatus(this.lastChild);
   }

   public void setLastChild(boolean lastChild) {
      this.lastChild = lastChild;
   }

   public String toString(Context context) {
      return this.propertyValueToString(value, context) + " {" + this.propertyValueToString(writeStatus, context) + "}";
   }
}
