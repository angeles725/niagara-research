package com.tridium.modbusAsync.comm;

import com.tridium.basicdriver.comm.CommReceiver;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusAsync.BModbusAsyncNetwork;
import com.tridium.modbusCore.messages.ModbusReceivedMessage;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.sys.Clock;

public class ModbusAsyncRxDriver extends CommReceiver {
   private Log rxLog;
   private BModbusAsyncNetwork network = null;
   private int responseSize;
   private int snifferResponseSize = 0;
   private int rcvCount;
   private int frameStart;
   private int comType;
   private int state = 2;
   private ByteBuffer rcvBuffer = new ByteBuffer();
   private ModbusReceivedMessage msg = null;
   long rxCharTicks = 0L;
   protected static final int IDLE = 0;
   protected static final int READ_DATA = 1;
   protected static final int WAIT_FOR_FRAME_START = 2;
   protected static String RTU_PARTIAL_MESSAGE = "RTU (client) partial message received ";
   protected static String ASCII_PARTIAL_MESSAGE = "ASCII (client) partial message received ";

   public ModbusAsyncRxDriver(BModbusAsyncNetwork network) {
      this.network = network;
      if (network == null) {
         this.rxLog = Log.getLog("modbusAsync.rx");
      }

      String serialLogName = network.getName() + "_" + network.getSerialPortConfig().getPortName() + "_rx";
      if (!SlotPath.isValidName(serialLogName)) {
         serialLogName = SlotPath.escape(serialLogName);
      }

      this.rxLog = Log.getLog(serialLogName);
   }

   protected ReceivedMessage receive() throws Exception {
      this.rcvCount = 0;
      this.rcvBuffer.reset();
      boolean msgComplete = false;

      while (!msgComplete) {
         int charIn = this.getInputStream().read();
         if (charIn != -1) {
            this.rxCharTicks = Clock.ticks();
            if (this.state != 0) {
               this.logTrace("Received char: 0x" + Integer.toHexString(charIn));
               if (this.comType == 0) {
                  msgComplete = this.processASCIICharacter(charIn);
               } else {
                  msgComplete = this.processRTUCharacter(charIn);
               }
            }
         } else {
            long now = Clock.ticks();
            long delta = now - this.rxCharTicks;
            long maxDelta = this.network.getMaxRxInterCharacterDelay().getMillis();
            if (maxDelta == 0L) {
               this.logTrace("Not checking inter-char timing.");
               maxDelta = -1L;
            }

            String partialMessage = RTU_PARTIAL_MESSAGE;
            if (this.isAsciiProtocol() && maxDelta > 0L) {
               maxDelta = 1000L;
               partialMessage = ASCII_PARTIAL_MESSAGE;
            }

            if (this.rcvCount > 0 && maxDelta > 0L && delta > maxDelta) {
               this.network.incrementPartialRxMsgs();
               this.rxLog.message(partialMessage + ByteArrayUtil.toHexString(this.rcvBuffer.toByteArray()));
               if (this.rxCharTicks > 0L) {
                  this.rxLog.message(" character deltaT = " + delta + ", expected: " + this.responseSize + ", received: " + this.rcvCount);
               }

               this.rxCharTicks = now;
               this.rcvCount = 0;
               this.state = 0;
            }
         }
      }

      if (this.msg == null) {
         this.msg = new ModbusReceivedMessage(this.rcvBuffer.getBytes(), this.rcvBuffer.getLength());
      } else {
         this.msg.setBytes(this.rcvBuffer.getBytes());
         this.msg.setLength(this.rcvBuffer.getLength());
      }

      return this.msg;
   }

   private boolean processRTUCharacter(int character) {
      byte b = (byte)(character & 0xFF);
      if (this.network.getSnifferMode()) {
         if (this.snifferResponseSize == 0) {
            this.snifferResponseSize = this.network.getRtuSnifferModeBufferSize();
         }

         this.rcvBuffer.write(b);
         this.snifferResponseSize--;
         if (this.snifferResponseSize == 0) {
            return true;
         }
      } else {
         switch (this.state) {
            case 2:
               if (character != this.frameStart) {
                  this.logTrace("ModbusAsyncRxDriver received unexpected character: " + Integer.toString(character, 16));
                  break;
               } else {
                  this.state = 1;
               }
            case 1:
               this.rcvBuffer.write(b);
               if (this.rcvCount == 1 & character > 127) {
                  this.responseSize = 4;
               }

               this.rcvCount++;
               this.responseSize--;
               this.rxLog.trace("rc= " + this.rcvCount + " rs= " + this.responseSize);
               if (this.responseSize == 0) {
                  this.logTrace("messageReceived: " + ByteArrayUtil.toHexString(this.rcvBuffer.toByteArray()));
                  this.state = 0;
                  return true;
               }
         }
      }

      return false;
   }

   private boolean processASCIICharacter(int character) {
      byte b = (byte)(character & 0xFF);
      switch (this.state) {
         case 1:
            if (character == 58) {
               this.rxLog.message(ASCII_PARTIAL_MESSAGE + new String(this.rcvBuffer.toByteArray()));
               this.rcvCount = 0;
               this.rcvBuffer.reset();
            }

            this.rcvBuffer.write(b);
            this.rcvCount++;
            if (character == 10) {
               this.logTrace("Message received: " + new String(this.rcvBuffer.toByteArray()));
               this.state = 0;
               return true;
            }
            break;
         case 2:
            if (character == 58) {
               this.state = 1;
               this.rcvBuffer.write(b);
               this.rcvCount = 1;
            }
      }

      return false;
   }

   public void setFrameStart(int fs) {
      this.state = 0;

      while (this.isRxInProcess()) {
         try {
            Thread.sleep(10L);
         } catch (InterruptedException var3) {
         }
      }

      this.frameStart = fs;
      this.rcvCount = 0;
      this.rcvBuffer.reset();
      boolean msgComplete = false;
      this.state = 2;
   }

   public boolean isRxInProcess() {
      long millis = this.network.getMinRxFrameEnd().getMillis();
      return Clock.ticks() - this.rxCharTicks < millis;
   }

   public void setComType(int ct) {
      this.comType = ct;
   }

   public void setResponseSize(int rs) {
      this.responseSize = rs;
   }

   protected boolean isAsciiProtocol() {
      return this.comType == 0;
   }

   private void logTrace(String message) {
      if (this.network.getModbusLog().isTraceOn()) {
         this.network.getModbusLog().trace(message);
      } else if (this.rxLog.isTraceOn()) {
         this.rxLog.trace(message);
      }
   }
}
