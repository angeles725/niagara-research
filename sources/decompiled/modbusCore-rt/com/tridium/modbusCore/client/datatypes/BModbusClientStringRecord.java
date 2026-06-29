package com.tridium.modbusCore.client.datatypes;

import com.tridium.modbusCore.BModbusDevice;
import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.client.BModbusClientDevice;
import com.tridium.modbusCore.datatypes.BModbusStringRecord;
import com.tridium.modbusCore.messages.ModbusReadFileRequest;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.messages.ModbusWriteFileRequest;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BValue;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
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
   )})
@NiagaraAction(
   name = "read",
   flags = 24
)
@NiagaraTopic(
   name = "readSuccessful",
   flags = 8
)
public class BModbusClientStringRecord extends BModbusStringRecord {
   public static final Property lastSuccessfulWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property lastFailedWrite = newProperty(67, BAbsTime.NULL, null);
   public static final Property writeStatus = newProperty(67, new BCommStatus(-2), null);
   public static final Property lastSuccessfulRead = newProperty(67, BAbsTime.NULL, null);
   public static final Property lastFailedRead = newProperty(67, BAbsTime.NULL, null);
   public static final Property readStatus = newProperty(67, new BCommStatus(-2), null);
   public static final Action read = newAction(24, null);
   public static final Topic readSuccessful = newTopic(8, null);
   public static final Type TYPE = Sys.loadType(BModbusClientStringRecord.class);

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

   public void read() {
      this.invoke(read, null, null);
   }

   public void fireReadSuccessful(BValue event) {
      this.fire(readSuccessful, event, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BModbusClientStringRecord() {
      this.setFlags(data, 259);
   }

   @Override
   public IFuture post(Action action, BValue arg, Context cx) {
      return action.equals(read) ? this.getNetwork().postAsync(new Invocation(this, action, arg, cx)) : super.post(action, arg, cx);
   }

   @Override
   public void doWrite() {
      BModbusDevice device = this.getDevice();
      if (device == null || device.isFault()) {
         BCommStatus tmp = new BCommStatus(-7);
         this.getWriteStatus().setErrorCode(tmp.getErrorCode());
         this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (device.isDown()) {
         BCommStatus tmp = new BCommStatus(-6);
         this.getWriteStatus().setErrorCode(tmp.getErrorCode());
         this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (device.isDisabled()) {
         BCommStatus tmp = new BCommStatus(-8);
         this.getWriteStatus().setErrorCode(tmp.getErrorCode());
         this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
      } else {
         if (device instanceof BModbusClientDevice) {
            this.writeRecords((BModbusClientDevice)device);
         }
      }
   }

   private void writeRecords(BModbusClientDevice device) {
      BModbusNetwork network = this.getNetwork();
      byte[] inputBytes = this.getInputBytes(this.getRecordLength(), this.getPadding() ? null : " ");
      int startIdx = 0;
      int lastIdx = inputBytes.length - 1;
      int endIdx = Math.min(lastIdx, 237);
      int startRecNum = this.getStartingRecordNumber();

      byte[] response;
      for (response = null; endIdx <= lastIdx; endIdx = Math.min(lastIdx, endIdx + 237)) {
         ModbusWriteFileRequest req = new ModbusWriteFileRequest(
            network.getModbusMode(),
            device,
            device.getDeviceAddress(),
            this.getFileNumber(),
            startRecNum,
            (endIdx - startIdx + 1) / 2,
            inputBytes,
            startIdx,
            endIdx
         );
         ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(req);
         if (resp == null) {
            resp = new ModbusResponse(network.getModbusMode(), device);
            resp.exceptionCode = 9;
         }

         if (resp.isError() && resp.exceptionCode != 5) {
            BCommStatus tmp = new BCommStatus(resp.exceptionCode);
            this.getWriteStatus().setErrorCode(tmp.getErrorCode());
            this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
            this.setLastFailedWrite(Clock.time());
            return;
         }

         response = appendBytes(response, resp.data);
         if (endIdx >= lastIdx) {
            break;
         }

         startRecNum += (endIdx - startIdx + 1) / 2;
         startIdx = endIdx;
      }

      BCommStatus tmp = new BCommStatus(0);
      this.getWriteStatus().setErrorCode(tmp.getErrorCode());
      this.getWriteStatus().setErrorDescription(tmp.getErrorDescription());
      this.setLastSuccessfulWrite(Clock.time());
      this.setOutputBytes(response);
      this.fireWriteSuccessful(null);
   }

   public void doRead() {
      BModbusDevice device = this.getDevice();
      if (device == null || device.isFault()) {
         BCommStatus tmp = new BCommStatus(-7);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (device.isDown()) {
         BCommStatus tmp = new BCommStatus(-6);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      } else if (device.isDisabled()) {
         BCommStatus tmp = new BCommStatus(-8);
         this.getReadStatus().setErrorCode(tmp.getErrorCode());
         this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      } else {
         if (device instanceof BModbusClientDevice) {
            this.readRecords((BModbusClientDevice)device);
         }
      }
   }

   private void readRecords(BModbusClientDevice device) {
      BModbusNetwork network = this.getNetwork();
      int startIdx = this.getStartingRecordNumber();
      int remainingLength = this.getRecordLength();
      int recLength = Math.min(remainingLength, 119);

      byte[] response;
      for (response = null; recLength > 0; recLength = Math.min(remainingLength, 119)) {
         ModbusReadFileRequest req = new ModbusReadFileRequest(
            network.getModbusMode(), device, device.getDeviceAddress(), this.getFileNumber(), startIdx, recLength
         );
         ModbusResponse resp = (ModbusResponse)device.sendModbusMessage(req);
         if (resp == null) {
            resp = new ModbusResponse(network.getModbusMode(), device);
            resp.exceptionCode = 9;
         }

         if (resp.isError() && resp.exceptionCode != 5) {
            BCommStatus tmp = new BCommStatus(resp.exceptionCode);
            this.getReadStatus().setErrorCode(tmp.getErrorCode());
            this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
            this.setLastFailedRead(Clock.time());
            return;
         }

         response = appendBytes(response, resp.data);
         startIdx += recLength;
         remainingLength -= recLength;
      }

      BCommStatus tmp = new BCommStatus(0);
      this.getReadStatus().setErrorCode(tmp.getErrorCode());
      this.getReadStatus().setErrorDescription(tmp.getErrorDescription());
      this.setLastSuccessfulRead(Clock.time());
      this.setOutputBytes(response);
      this.fireReadSuccessful(null);
   }

   private static byte[] appendBytes(byte[] data1, byte[] data2) {
      if (data1 == null && data2 == null) {
         return null;
      } else {
         int data1Length = 0;
         if (data1 != null) {
            data1Length = data1.length;
         }

         int data2Length = 0;
         if (data2 != null) {
            data2Length = data2.length;
         }

         byte[] result = new byte[data1Length + data2Length];
         if (data1Length > 0) {
            System.arraycopy(data1, 0, result, 0, data1Length);
         }

         if (data2Length > 0) {
            System.arraycopy(data2, 0, result, data1Length, data2Length);
         }

         return result;
      }
   }
}
