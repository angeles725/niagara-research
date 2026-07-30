package com.tridium.modbusSlave.comm;

import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.messages.ModbusInputStream;
import com.tridium.modbusCore.messages.ModbusMessageConst;
import com.tridium.modbusCore.messages.ModbusResponse;
import com.tridium.modbusCore.server.messages.ModbusServerReadFileRequest;
import com.tridium.modbusCore.server.messages.ModbusServerReadFileResponse;
import com.tridium.modbusCore.server.messages.ModbusServerReadRequest;
import com.tridium.modbusCore.server.messages.ModbusServerWriteFileRequest;
import com.tridium.modbusCore.server.messages.ModbusServerWriteFileResponse;
import com.tridium.modbusCore.server.messages.ModbusServerWriteRequest;
import com.tridium.modbusCore.server.util.TLinkedListManager;
import com.tridium.modbusSlave.BModbusSlaveDevice;
import com.tridium.modbusSlave.BModbusSlaveNetwork;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;

public class ModbusUnsolicitedReceive implements Runnable, ModbusMessageConst {
   private TLinkedListManager unsolicitedMessageManager = null;
   private BModbusSlaveNetwork host;
   private boolean timeToDie = true;
   private Thread myThread;
   private long delayedStartTicks = 0L;
   private static final int UNSOLICITED_THREAD_STOP_TIMEOUT = 10000;

   public ModbusUnsolicitedReceive(BModbusSlaveNetwork host) {
      this.host = host;
   }

   public final void init() {
   }

   public final void cleanup() {
   }

   public final void start() {
      this.timeToDie = false;
      this.myThread = new Thread(this, "ModSlave:UnsolRcv");
      this.myThread.start();
   }

   public final void stop() {
      this.timeToDie = true;
      this.myThread.interrupt();

      try {
         this.myThread.join(10000L);
      } catch (InterruptedException var2) {
      }

      if (this.myThread.isAlive()) {
         this.host.getModbusLog().warning(this.myThread.getName() + " did not terminate in a timely manner, abandoning thread");
      }

      this.myThread = null;
   }

   public boolean isDying() {
      return this.timeToDie;
   }

   public void atSteadyState() {
      this.delayedStartTicks = Clock.ticks() + 1000L;
   }

   @Override
   public void run() {
      if (Sys.atSteadyState()) {
         this.delayedStartTicks = Clock.ticks() + 10000L;
      } else {
         this.delayedStartTicks = Clock.ticks() + 120000L;
      }

      this.unsolicitedMessageManager = new TLinkedListManager("ModbusUnsolicitedReceive Manager");

      while (!this.timeToDie) {
         UnsolicitedMessageElement unsolicitedMsg;
         try {
            unsolicitedMsg = (UnsolicitedMessageElement)this.unsolicitedMessageManager.removeFromHead(-1L);
            if (this.delayedStartTicks > 0L) {
               long delayTicks = this.delayedStartTicks - Clock.ticks();
               if (delayTicks > 0L) {
                  this.host.getModbusLog().message("delayedStartsTicks: " + delayTicks);
                  unsolicitedMsg = null;
               } else if (unsolicitedMsg != null) {
                  this.delayedStartTicks = 0L;
               }
            }
         } catch (InterruptedException var11) {
            if (this.timeToDie) {
               break;
            }

            this.host.getModbusLog().error("ModbusUnsolicitedReceive.run Problem with " + this.unsolicitedMessageManager.getName() + ": ", var11);
            unsolicitedMsg = null;
         }

         try {
            if (unsolicitedMsg != null) {
               byte[] newMessage = unsolicitedMsg.getMessage();
               if (this.host.getModbusMode() == 0) {
                  newMessage = ModbusInputStream.convertAscii2Rtu(newMessage);
               }

               if (newMessage.length != 0) {
                  int deviceAddr = newMessage[0] & 255;
                  BModbusSlaveDevice device = this.findModbusDevice(deviceAddr);
                  if (device != null) {
                     device.incrementRequest();
                     if (!device.getStatus().isDisabled()) {
                        switch (newMessage[1]) {
                           case 1:
                           case 2:
                           case 3:
                           case 4:
                              ModbusServerReadRequest readRequest = new ModbusServerReadRequest(this.host.getModbusMode(), device, newMessage);
                              this.processReadRequest(device, readRequest);
                              break;
                           case 5:
                           case 6:
                           case 15:
                           case 16:
                              ModbusServerWriteRequest writeRequest = new ModbusServerWriteRequest(this.host.getModbusMode(), device, newMessage);
                              this.processWriteRequest(device, writeRequest);
                              break;
                           case 7:
                           case 8:
                           case 9:
                           case 10:
                           case 11:
                           case 12:
                           case 13:
                           case 14:
                           case 17:
                           case 18:
                           case 19:
                           default:
                              ModbusResponse resp = new ModbusResponse(this.host.getModbusMode(), device);
                              resp.setResponseExpected(false);
                              resp.deviceAddress = (byte)deviceAddr;
                              resp.functionCode = (byte)(newMessage[1] | 128);
                              resp.byteCount = 1;
                              resp.data = new byte[0];
                              this.host.sendSync(resp);
                              break;
                           case 20:
                              ModbusServerReadFileRequest readFileRequest = new ModbusServerReadFileRequest(newMessage);
                              this.processReadFileRequest(device, readFileRequest);
                              break;
                           case 21:
                              ModbusServerWriteFileRequest writeFileRequest = new ModbusServerWriteFileRequest(newMessage);
                              this.processWriteFileRequest(device, writeFileRequest);
                        }
                     } else if (this.host.getModbusLog().isTraceOn()) {
                        this.host.getModbusLog().trace("ModbusUnsolicitedReceive found device is disabled: " + deviceAddr);
                     }
                  } else if (this.host.getModbusLog().isTraceOn()) {
                     this.host.getModbusLog().trace("ModbusUnsolicitedReceive did not find device: " + deviceAddr);
                  }
               }
            }
         } catch (Exception var10) {
            if (this.timeToDie) {
               break;
            }

            this.host.getModbusLog().error(" ModbusSlaveUnsolicitedReceive thread caught Exception: ", var10);
         }
      }
   }

   private void processReadRequest(BModbusSlaveDevice device, ModbusServerReadRequest readRequest) {
      ModbusResponse resp = new ModbusResponse(this.host.getModbusMode(), device);
      resp.setResponseExpected(false);
      resp.deviceAddress = (byte)readRequest.deviceAddress;
      resp.functionCode = (byte)readRequest.functionCode;
      int address = readRequest.startAddress;
      int code = readRequest.functionCode;
      int numberPoints = readRequest.numberPoints;
      boolean error = false;

      try {
         switch (code) {
            case 1:
               resp.data = device.getCoilStatusValues(address, numberPoints);
               break;
            case 2:
               resp.data = device.getInputStatusValues(address, numberPoints);
               break;
            case 3:
               resp.data = device.getHoldingRegisterValues(address, numberPoints);
               break;
            case 4:
               resp.data = device.getInputRegisterValues(address, numberPoints);
               break;
            default:
               error = true;
         }
      } catch (ModbusException var9) {
         error = true;
      }

      if (error) {
         resp.functionCode = (byte)(resp.functionCode | 128);
         resp.byteCount = 2;
         resp.data = new byte[0];
         this.host.sendSync(resp);
      } else {
         resp.byteCount = (byte)resp.data.length;
         this.host.sendSync(resp);
      }
   }

   private void processWriteRequest(BModbusSlaveDevice device, ModbusServerWriteRequest writeRequest) {
      ModbusResponse resp = new ModbusResponse(this.host.getModbusMode(), device, writeRequest);
      resp.setResponseExpected(false);
      int address = writeRequest.startAddress;
      int code = writeRequest.functionCode;
      boolean error = false;
      switch (code) {
         case 5:
            if (device.isCoilAddressValid(address, writeRequest.numberPoints)) {
               device.setCoilStatusValue(address, (writeRequest.data[0] & 255) == 255);
            } else {
               error = true;
            }
            break;
         case 6:
         case 16:
            if (device.isHoldingRegisterAddressValid(address, writeRequest.numberPoints)) {
               device.setHoldingRegisterValues(address, writeRequest.data);
            } else {
               error = true;
            }
            break;
         case 15:
            if (device.isCoilAddressValid(address, writeRequest.numberPoints)) {
               device.setCoilStatusValue(address, writeRequest.numberPoints, writeRequest.data);
            } else {
               error = true;
            }
            break;
         default:
            error = true;
      }

      if (error) {
         resp.functionCode = (byte)(resp.functionCode | 128);
         resp.byteCount = 2;
         this.host.sendSync(resp);
      } else {
         this.host.sendSync(resp);
      }
   }

   private void processReadFileRequest(BModbusSlaveDevice device, ModbusServerReadFileRequest readRequest) {
      ModbusServerReadFileResponse resp = new ModbusServerReadFileResponse(this.host.getModbusMode(), device, readRequest);
      int subRequests = readRequest.getNumSubRequests();
      boolean error = false;

      try {
         for (int i = 0; i < subRequests; i++) {
            resp.addSubRequestData(
               readRequest.getFileNumber(i),
               readRequest.getStartingRecordNumber(i),
               readRequest.getRecordLength(i),
               device.getFileRecordData(readRequest.getFileNumber(i), readRequest.getStartingRecordNumber(i), readRequest.getRecordLength(i))
            );
         }
      } catch (Exception var7) {
         error = true;
      }

      if (error) {
         resp.functionCode = (byte)(resp.functionCode | 128);
         resp.byteCount = 2;
         resp.data = new byte[0];
         this.host.sendSync(resp);
      } else {
         resp.byteCount = (byte)resp.data.length;
         this.host.sendSync(resp);
      }
   }

   private void processWriteFileRequest(BModbusSlaveDevice device, ModbusServerWriteFileRequest writeRequest) {
      ModbusServerWriteFileResponse resp = new ModbusServerWriteFileResponse(this.host.getModbusMode(), device, writeRequest);
      int subRequests = writeRequest.getNumSubRequests();
      boolean error = false;

      try {
         for (int i = 0; i < subRequests; i++) {
            resp.addSubRequestData(
               writeRequest.getFileNumber(i),
               writeRequest.getStartingRecordNumber(i),
               writeRequest.getRecordLength(i),
               device.setFileRecordData(
                  writeRequest.getFileNumber(i), writeRequest.getStartingRecordNumber(i), writeRequest.getRecordLength(i), writeRequest.getRecordData(i)
               )
            );
         }
      } catch (Exception var7) {
         error = true;
      }

      if (error) {
         resp.functionCode = (byte)(resp.functionCode | 128);
         resp.byteCount = 2;
         resp.data = new byte[0];
         this.host.sendSync(resp);
      } else {
         resp.byteCount = (byte)resp.data.length;
         this.host.sendSync(resp);
      }
   }

   public void receiveMessage(UnsolicitedMessageElement message) {
      if (this.unsolicitedMessageManager != null) {
         this.unsolicitedMessageManager.addToTail(message);
      }
   }

   public BModbusSlaveDevice findModbusDevice(int address) {
      return this.host.findModbusDevice(address);
   }
}
