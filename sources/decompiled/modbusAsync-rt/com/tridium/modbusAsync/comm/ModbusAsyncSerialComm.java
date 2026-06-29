package com.tridium.modbusAsync.comm;

import com.tridium.basicdriver.comm.Comm;
import com.tridium.basicdriver.comm.CommReceiver;
import com.tridium.basicdriver.message.Message;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.basicdriver.util.BasicException;
import com.tridium.modbusAsync.BModbusAsyncNetwork;
import java.io.InputStream;
import java.io.OutputStream;
import javax.baja.serial.BISerialPort;
import javax.baja.serial.BISerialService;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Sys;

public class ModbusAsyncSerialComm extends Comm {
   private static final long MIN_SLEEP_TIME = 1L;
   protected BISerialPort serialPort;
   protected InputStream in;
   protected OutputStream out;
   protected Thread rxThread;
   private long lastRecvMessageTicks = 0L;

   public ModbusAsyncSerialComm(BModbusAsyncNetwork serialNetwork) {
      super(serialNetwork, new ModbusAsyncRxDriver(serialNetwork), new ModbusAsyncTxDriver());
   }

   protected boolean started() throws Exception {
      try {
         BISerialService platSvc = (BISerialService)Sys.getService(BISerialService.TYPE);
         this.serialPort = ((BModbusAsyncNetwork)this.getNetwork()).getSerialPortConfig().open(this.getNetwork().getName());
         this.serialPort.enableReceiveTimeout(platSvc.getMinTimeout());
         this.in = this.serialPort.getInputStream();
         this.out = this.serialPort.getOutputStream();
      } catch (Exception var6) {
         String errMsg = "Error opening and configuring the serial port";
         this.getNetwork().getLog().error(errMsg, var6);
         if (this.in != null) {
            try {
               this.in.close();
            } catch (Exception var5) {
               this.getNetwork().getLog().error("Unable to close serial input stream.", var5);
            }
         }

         if (this.out != null) {
            try {
               this.out.close();
            } catch (Exception var4) {
               this.getNetwork().getLog().error("Unable to close serial output stream.", var4);
            }
         }

         if (this.serialPort != null) {
            this.serialPort.close();
         }

         throw var6;
      }

      this.getCommReceiver().setInputStream(this.in);
      this.getCommTransmitter().setOutputStream(this.out);
      this.rxThread = new Thread(this.getCommReceiver(), "SerialRcv:" + this.getNetwork().getName());
      this.getCommReceiver().setAlive(true);
      this.rxThread.start();
      if (((BModbusAsyncNetwork)this.getNetwork()).getRxPriority()) {
         this.rxThread.setPriority(7);
      } else {
         this.rxThread.setPriority(5);
      }

      return true;
   }

   protected void stopped() throws Exception {
      this.getCommReceiver().setAlive(false);
      if (this.getCommReceiver() != null && this.rxThread != null) {
         this.rxThread.interrupt();
      }

      if (this.in != null) {
         try {
            this.in.close();
         } catch (Exception var3) {
            this.getNetwork().getLog().error("Unable to close serial input stream.", var3);
         }
      }

      if (this.out != null) {
         try {
            this.out.close();
         } catch (Exception var2) {
            this.getNetwork().getLog().error("Unable to close serial output stream.", var2);
         }
      }

      if (this.serialPort != null) {
         this.serialPort.disableReceiveTimeout();
         this.serialPort.close();
      }

      this.in = null;
      this.out = null;
   }

   public Message transmit(Message msg, BRelTime responseTimeout, int retryCount) throws BasicException {
      if (msg == null) {
         return null;
      } else if (!msg.getResponseExpected()) {
         this.transmitNoResponse(msg);
         return null;
      } else {
         this.performInterMessageDelay();
         return super.transmit(msg, responseTimeout, retryCount);
      }
   }

   public void transmitNoResponse(Message msg) throws BasicException {
      if (msg != null) {
         if (!this.isCommStarted()) {
            throw new BasicException("Communication handler service not started.");
         } else {
            this.performInterMessageDelay();
            super.transmitNoResponse(msg);
         }
      }
   }

   public void receive(ReceivedMessage msg) {
      if (msg != null) {
         this.setReceivedMessageTicks(Clock.ticks());
         super.receive(msg);
      }
   }

   protected void performInterMessageDelay() {
      CommReceiver commReceiver = this.getCommReceiver();
      if (commReceiver instanceof ModbusAsyncRxDriver) {
         while (((ModbusAsyncRxDriver)commReceiver).isRxInProcess()) {
            try {
               Thread.sleep(10L);
            } catch (InterruptedException var10) {
            }
         }
      }

      long minDelay = ((BModbusAsyncNetwork)this.getNetwork()).getInterMessageDelay().getMillis();
      if (minDelay > 0L) {
         long difference = Clock.ticks() - this.lastRecvMessageTicks;
         if (difference < minDelay) {
            long sleepTime = Math.max(minDelay - difference, 1L);

            try {
               Thread.sleep(sleepTime);
            } catch (Exception var9) {
               var9.printStackTrace();
            }
         }
      }
   }

   protected void setReceivedMessageTicks(long ticks) {
      this.lastRecvMessageTicks = ticks;
   }

   public BISerialPort getSerialPort() {
      return this.serialPort;
   }

   public Thread getRxThread() {
      return this.rxThread;
   }
}
