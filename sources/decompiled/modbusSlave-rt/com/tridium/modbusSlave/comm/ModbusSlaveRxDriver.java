package com.tridium.modbusSlave.comm;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.basicdriver.comm.CommReceiver;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.messages.ModbusInputStream;
import com.tridium.modbusCore.messages.ModbusMessage;
import com.tridium.modbusSlave.BModbusSlaveNetwork;
import java.io.ByteArrayOutputStream;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.Clock;

public class ModbusSlaveRxDriver extends CommReceiver {
   private Log rxLog;
   private BModbusSlaveNetwork network = null;
   private int responseSize;
   private int rcvCount;
   private int functionCode = 0;
   private boolean done = false;
   private ByteArrayOutputStream rcv;
   long rxCharTicks = 0L;
   private boolean waitForExTimeout = false;
   protected static String RTU_PARTIAL_MESSAGE = "RTU (server) partial message received ";
   protected static String ASCII_PARTIAL_MESSAGE = "ASCII (server) partial message received ";

   public ModbusSlaveRxDriver(BModbusSlaveNetwork network) {
      this.rcv = new ByteArrayOutputStream();
      this.network = network;
      if (network == null) {
         this.rxLog = Log.getLog("modbusSlave.rx");
      }

      String serialLogName = network.getName() + "_" + network.getSerialPortConfig().getPortName() + "_rx";
      if (!SlotPath.isValidName(serialLogName)) {
         serialLogName = SlotPath.escape(serialLogName);
      }

      this.rxLog = Log.getLog(serialLogName);
   }

   public void setAlive(boolean alive) {
      this.done = !alive;
   }

   public boolean isAlive() {
      return !this.done;
   }

   public void run() {
      while (!this.done) {
         boolean newChar = false;

         try {
            int charIn = this.getInputStream().read();
            if (charIn == -1) {
               long now = Clock.ticks();
               long delta = now - this.rxCharTicks;
               long maxDelta = 50L;
               if (this.waitForExTimeout) {
                  this.rxLog.trace("rxCharTimeout: clearing flag");
               }

               this.waitForExTimeout = false;
               String partialMessage = RTU_PARTIAL_MESSAGE;
               if (this.isAsciiProtocol()) {
                  maxDelta = 1000L;
                  partialMessage = ASCII_PARTIAL_MESSAGE;
               }

               if (this.rcvCount > 0 && delta > maxDelta) {
                  this.network.incrementPartialRxMsgs();
                  this.rxLog.message(partialMessage + ByteArrayUtil.toHexString(this.rcv.toByteArray()));
                  if (this.rxCharTicks > 0L) {
                     this.rxLog.message(" character deltaT = " + delta + ", expected: " + this.responseSize + ", received: " + this.rcvCount);
                  }

                  this.rxCharTicks = now;
                  newChar = false;
                  this.rcvCount = 0;
                  this.responseSize = 10;
               }
            } else {
               long nowx = Clock.ticks();
               long charDelta = nowx - this.rxCharTicks;
               if (this.waitForExTimeout & charDelta > 500L) {
                  this.waitForExTimeout = false;
                  this.rxLog.trace("charDelta > 500: clearing flag");
                  this.rcvCount = 0;
                  this.responseSize = 10;
               }

               this.rxCharTicks = nowx;
               newChar = true;
               byte var1 = (byte)(charIn & 0xFF);
            }

            if (newChar) {
               this.rxLog.trace("Received char: 0x" + Integer.toHexString(charIn));
               if (this.network.getModbusMode() == 0) {
                  this.processASCIICharacter(charIn);
               } else {
                  this.processRTUCharacter(charIn);
               }
            }
         } catch (Exception var11) {
            if (this.isAlive()) {
               this.network.getModbusLog().error("Exception in ModbusSlaveRxDriver", var11);
            }
         }
      }

      this.getComm().receiveFinal();
   }

   private void processRTUCharacter(int character) {
      if (this.waitForExTimeout) {
         this.logTrace("    ignoring character: 0x" + Integer.toHexString(character));
         this.rcvCount = 0;
      } else {
         byte b = (byte)(character & 0xFF);
         if (this.rcvCount == 0) {
            this.rcv.reset();
         }

         this.rcv.write(b);
         if (this.rcvCount == 0) {
            this.logTrace("finding device: " + (character & 0xFF));
            if (this.network.findModbusDevice(character & 0xFF) == null) {
               this.logTrace("address not for me, waitForRxTimeout: " + (character & 0xFF));
               this.waitForExTimeout = true;
               this.responseSize = 10;
               return;
            }
         }

         if (this.rcvCount == 1) {
            if (character > 127) {
               this.responseSize = 4;
            } else {
               this.functionCode = character;
               switch (character) {
                  case 1:
                  case 2:
                  case 3:
                  case 4:
                  case 5:
                  case 6:
                     this.responseSize = 7;
               }
            }
         } else if ((this.functionCode == 15 || this.functionCode == 16) && this.rcvCount == 6) {
            this.responseSize = character + 3;
         } else if ((this.functionCode == 20 || this.functionCode == 21) && this.rcvCount == 2) {
            this.responseSize = character + 3;
         }

         this.rcvCount++;
         this.responseSize--;
         this.rxLog.trace("rc= " + this.rcvCount + " rs= " + this.responseSize);
         if (this.responseSize == 0) {
            this.messageComplete();
         }
      }
   }

   private void processASCIICharacter(int character) {
      byte b = (byte)(character & 0xFF);
      if (character == 58) {
         this.rcv.reset();
      }

      this.rcv.write(b);
      this.rcvCount++;
      if (character == 10) {
         this.messageComplete();
      }
   }

   protected void messageComplete() {
      byte[] a = this.rcv.toByteArray();
      boolean messageOK = true;
      if (this.network.getModbusMode() == 0) {
         byte[] checkA = ModbusInputStream.convertAscii2Rtu(a);
         if (!ModbusMessage.verifyLRC(checkA)) {
            this.network.incrementLrcErrors();
            this.network.getModbusLog().message("modbusSlave: received message had LRC error: " + new String(a));
            messageOK = false;
         }
      } else {
         boolean crcOk = false;

         try {
            crcOk = ModbusMessage.verifyCRC(a);
         } catch (Exception var5) {
            var5.printStackTrace();
         }

         if (!crcOk) {
            this.network.incrementCrcErrors();
            if (this.network.getModbusLog().isLoggable(1)) {
               this.network.getModbusLog().message("modbusSlave: received message had CRC error: ");
               ByteArrayUtil.hexDump(a);
            }

            messageOK = false;
         }
      }

      this.network.incrementReceived();
      if (messageOK) {
         if (this.network.getModbusMode() == 0) {
            this.logTrace(" Received ascii [" + new String(a, 0, a.length - 2) + "]");
         } else {
            this.logTrace(" Received rtu [" + ByteArrayUtil.toHexString(a) + "]");
         }

         if (!this.network.getSnifferMode()) {
            UnsolicitedMessageElement message = new UnsolicitedMessageElement(a);
            this.network.unsolicitedReceive().receiveMessage(message);
            Comm comm = this.network.getComm();
            if (comm instanceof ModbusSlaveSerialComm) {
               ((ModbusSlaveSerialComm)comm).setReceivedMessageTicks(Clock.ticks());
            }
         }
      }

      this.rcv.reset();
      this.rcvCount = 0;
      this.responseSize = 10;
   }

   protected ReceivedMessage receive() throws Exception {
      return null;
   }

   private void logTrace(String message) {
      if (this.network.getModbusLog().isTraceOn()) {
         this.network.getModbusLog().trace(message);
      } else if (this.rxLog.isTraceOn()) {
         this.rxLog.trace(message);
      }
   }

   protected boolean isAsciiProtocol() {
      return this.network.getModbusMode() == 0;
   }
}
