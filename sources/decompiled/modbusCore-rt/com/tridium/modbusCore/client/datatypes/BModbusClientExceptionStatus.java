package com.tridium.modbusCore.client.datatypes;

import com.tridium.basicdriver.BBasicNetwork;
import com.tridium.basicdriver.util.BIBasicPollable;
import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.client.BModbusClientNetwork;
import com.tridium.modbusCore.messages.ModbusReadExceptionStatusRequest;
import com.tridium.modbusCore.messages.ModbusReadExceptionStatusResponse;
import com.tridium.modbusCore.messages.ModbusResponse;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BIStatus;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.make(0)",
      flags = 11
   ), @NiagaraProperty(
      name = "enabled",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal"
   ), @NiagaraProperty(
      name = "bytesReturned",
      type = "int",
      defaultValue = "1",
      facets = {@Facet("BFacets.makeInt(1, 2)")}
   ), @NiagaraProperty(
      name = "lastSuccessfulRead",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "lastFailedRead",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 67
   ), @NiagaraProperty(
      name = "readStatus",
      type = "BCommStatus",
      defaultValue = "new BCommStatus(ModbusMessageConst.OK_NOT_ACTIVE)",
      flags = 67
   ), @NiagaraProperty(
      name = "out",
      type = "BStatusNumeric",
      defaultValue = "new BStatusNumeric()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit0",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit1",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit2",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit3",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit4",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit5",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit6",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit7",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit8",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit9",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit10",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit11",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit12",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit13",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit14",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   ), @NiagaraProperty(
      name = "bit15",
      type = "BStatusBoolean",
      defaultValue = "new BStatusBoolean()",
      flags = 3
   )})
@NiagaraAction(
   name = "poll",
   flags = 16
)
public class BModbusClientExceptionStatus extends BComponent implements BIStatus, BIBasicPollable {
   public static final Property status = newProperty(11, BStatus.make(0), null);
   public static final Property enabled = newProperty(0, true, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property pollFrequency = newProperty(0, BPollFrequency.normal, null);
   public static final Property bytesReturned = newProperty(0, 1, BFacets.makeInt(1, 2));
   public static final Property lastSuccessfulRead = newProperty(67, BAbsTime.NULL, null);
   public static final Property lastFailedRead = newProperty(67, BAbsTime.NULL, null);
   public static final Property readStatus = newProperty(67, new BCommStatus(-2), null);
   public static final Property out = newProperty(3, new BStatusNumeric(), null);
   public static final Property bit0 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit1 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit2 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit3 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit4 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit5 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit6 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit7 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit8 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit9 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit10 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit11 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit12 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit13 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit14 = newProperty(3, new BStatusBoolean(), null);
   public static final Property bit15 = newProperty(3, new BStatusBoolean(), null);
   public static final Action poll = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BModbusClientExceptionStatus.class);

   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public boolean getEnabled() {
      return this.getBoolean(enabled);
   }

   public void setEnabled(boolean v) {
      this.setBoolean(enabled, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public int getBytesReturned() {
      return this.getInt(bytesReturned);
   }

   public void setBytesReturned(int v) {
      this.setInt(bytesReturned, v, null);
   }

   public BAbsTime getLastSuccessfulRead() {
      return (BAbsTime)this.get(lastSuccessfulRead);
   }

   public void setLastSuccessfulRead(BAbsTime v) {
      this.set(lastSuccessfulRead, v, null);
   }

   public BAbsTime getLastFailedRead() {
      return (BAbsTime)this.get(lastFailedRead);
   }

   public void setLastFailedRead(BAbsTime v) {
      this.set(lastFailedRead, v, null);
   }

   public BCommStatus getReadStatus() {
      return (BCommStatus)this.get(readStatus);
   }

   public void setReadStatus(BCommStatus v) {
      this.set(readStatus, v, null);
   }

   public BStatusNumeric getOut() {
      return (BStatusNumeric)this.get(out);
   }

   public void setOut(BStatusNumeric v) {
      this.set(out, v, null);
   }

   public BStatusBoolean getBit0() {
      return (BStatusBoolean)this.get(bit0);
   }

   public void setBit0(BStatusBoolean v) {
      this.set(bit0, v, null);
   }

   public BStatusBoolean getBit1() {
      return (BStatusBoolean)this.get(bit1);
   }

   public void setBit1(BStatusBoolean v) {
      this.set(bit1, v, null);
   }

   public BStatusBoolean getBit2() {
      return (BStatusBoolean)this.get(bit2);
   }

   public void setBit2(BStatusBoolean v) {
      this.set(bit2, v, null);
   }

   public BStatusBoolean getBit3() {
      return (BStatusBoolean)this.get(bit3);
   }

   public void setBit3(BStatusBoolean v) {
      this.set(bit3, v, null);
   }

   public BStatusBoolean getBit4() {
      return (BStatusBoolean)this.get(bit4);
   }

   public void setBit4(BStatusBoolean v) {
      this.set(bit4, v, null);
   }

   public BStatusBoolean getBit5() {
      return (BStatusBoolean)this.get(bit5);
   }

   public void setBit5(BStatusBoolean v) {
      this.set(bit5, v, null);
   }

   public BStatusBoolean getBit6() {
      return (BStatusBoolean)this.get(bit6);
   }

   public void setBit6(BStatusBoolean v) {
      this.set(bit6, v, null);
   }

   public BStatusBoolean getBit7() {
      return (BStatusBoolean)this.get(bit7);
   }

   public void setBit7(BStatusBoolean v) {
      this.set(bit7, v, null);
   }

   public BStatusBoolean getBit8() {
      return (BStatusBoolean)this.get(bit8);
   }

   public void setBit8(BStatusBoolean v) {
      this.set(bit8, v, null);
   }

   public BStatusBoolean getBit9() {
      return (BStatusBoolean)this.get(bit9);
   }

   public void setBit9(BStatusBoolean v) {
      this.set(bit9, v, null);
   }

   public BStatusBoolean getBit10() {
      return (BStatusBoolean)this.get(bit10);
   }

   public void setBit10(BStatusBoolean v) {
      this.set(bit10, v, null);
   }

   public BStatusBoolean getBit11() {
      return (BStatusBoolean)this.get(bit11);
   }

   public void setBit11(BStatusBoolean v) {
      this.set(bit11, v, null);
   }

   public BStatusBoolean getBit12() {
      return (BStatusBoolean)this.get(bit12);
   }

   public void setBit12(BStatusBoolean v) {
      this.set(bit12, v, null);
   }

   public BStatusBoolean getBit13() {
      return (BStatusBoolean)this.get(bit13);
   }

   public void setBit13(BStatusBoolean v) {
      this.set(bit13, v, null);
   }

   public BStatusBoolean getBit14() {
      return (BStatusBoolean)this.get(bit14);
   }

   public void setBit14(BStatusBoolean v) {
      this.set(bit14, v, null);
   }

   public BStatusBoolean getBit15() {
      return (BStatusBoolean)this.get(bit15);
   }

   public void setBit15(BStatusBoolean v) {
      this.set(bit15, v, null);
   }

   public void poll() {
      this.invoke(poll, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void stopped() throws Exception {
      try {
         this.unsubscribed();
      } catch (Exception var2) {
      }
   }

   public BModbusClientNetwork getNetwork() {
      try {
         return (BModbusClientNetwork)this.getDevice().getNetwork();
      } catch (Exception var2) {
         return null;
      }
   }

   public BModbusClientDevice getDevice() {
      try {
         return (BModbusClientDevice)this.getParent();
      } catch (Exception var2) {
         return null;
      }
   }

   public void changed(Property p, Context cx) {
      if (p.equals(enabled)) {
         this.setStatus(BStatus.makeDisabled(this.getStatus(), !this.getEnabled()));
      }

      if (p.equals(status)) {
         BStatusValue[] sValues = (BStatusValue[])this.getChildren(BStatusValue.class);

         for (int i = 0; i < sValues.length; i++) {
            sValues[i].setStatusDown(this.getStatus().isDown());
            sValues[i].setStatusFault(this.getStatus().isFault());
            sValues[i].setStatusDisabled(this.getStatus().isDisabled());
         }
      }
   }

   public void subscribed() {
      if (this.isRunning()) {
         ((BBasicNetwork)this.getDevice().getNetwork()).getPollScheduler().subscribe(this);
      }
   }

   public void unsubscribed() {
      try {
         ((BBasicNetwork)this.getDevice().getNetwork()).getPollScheduler().unsubscribe(this);
      } catch (Exception var2) {
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      return action.equals(poll) ? this.getNetwork().postAsync(new Invocation(this, action, arg, cx)) : super.post(action, arg, cx);
   }

   public void doPoll() {
      BModbusDevice device = this.getDevice();
      if (device == null || device.isFault()) {
         this.setStatus(BStatus.makeFault(this.getStatus(), true));
         BCommStatus tmp = new BCommStatus(-7);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (device.isDown()) {
         this.setStatus(BStatus.makeDown(this.getStatus(), true));
         BCommStatus tmp = new BCommStatus(-6);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (!device.isDisabled() && this.getEnabled()) {
         this.setStatus(BStatus.ok);
         if (device instanceof BModbusClientDevice) {
            this.pollForData((BModbusClientDevice)device);
         }
      } else {
         this.setStatus(BStatus.makeDisabled(this.getStatus(), true));
         BCommStatus tmp = new BCommStatus(-8);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      }
   }

   private void pollForData(BModbusClientDevice device) {
      BModbusNetwork network = this.getNetwork();
      ModbusReadExceptionStatusRequest req = new ModbusReadExceptionStatusRequest(
         network.getModbusMode(), device, device.getDeviceAddress(), this.getBytesReturned()
      );
      ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(req);
      if (resp == null) {
         resp = new ModbusResponse(network.getModbusMode(), device);
         resp.exceptionCode = 9;
      }

      if (resp.isError()) {
         BCommStatus tmp = new BCommStatus(resp.exceptionCode);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
         this.setLastFailedRead(Clock.time());
      } else {
         ModbusReadExceptionStatusResponse response = (ModbusReadExceptionStatusResponse)resp;
         this.setOut(new BStatusNumeric(response.getDouble()));
         this.setBit0(new BStatusBoolean(response.getStatusBit(0)));
         this.setBit1(new BStatusBoolean(response.getStatusBit(1)));
         this.setBit2(new BStatusBoolean(response.getStatusBit(2)));
         this.setBit3(new BStatusBoolean(response.getStatusBit(3)));
         this.setBit4(new BStatusBoolean(response.getStatusBit(4)));
         this.setBit5(new BStatusBoolean(response.getStatusBit(5)));
         this.setBit6(new BStatusBoolean(response.getStatusBit(6)));
         this.setBit7(new BStatusBoolean(response.getStatusBit(7)));
         this.setBit8(new BStatusBoolean(response.getStatusBit(8)));
         this.setBit9(new BStatusBoolean(response.getStatusBit(9)));
         this.setBit10(new BStatusBoolean(response.getStatusBit(10)));
         this.setBit11(new BStatusBoolean(response.getStatusBit(11)));
         this.setBit12(new BStatusBoolean(response.getStatusBit(12)));
         this.setBit13(new BStatusBoolean(response.getStatusBit(13)));
         this.setBit14(new BStatusBoolean(response.getStatusBit(14)));
         this.setBit15(new BStatusBoolean(response.getStatusBit(15)));
         BCommStatus tmp = new BCommStatus(0);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
         this.setLastSuccessfulRead(Clock.time());
      }
   }
}
