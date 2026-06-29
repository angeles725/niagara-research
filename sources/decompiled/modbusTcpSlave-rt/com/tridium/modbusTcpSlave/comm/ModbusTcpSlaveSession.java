package com.tridium.modbusTcpSlave.comm;

import com.tridium.modbusCore.messages.ModbusMessage;
import com.tridium.modbusTcpSlave.BModbusTcpSlaveNetwork;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.baja.nre.util.ByteArrayUtil;

public class ModbusTcpSlaveSession {
   private BModbusTcpSlaveNetwork modbusNetwork;
   private boolean done = true;
   public Socket socket;
   private InputStream inStream;
   private OutputStream outStream;
   private ModbusTcpSlaveSession.ReceiveDriver rcvDriver;
   private Thread rcvThread;
   private int sessionId = -1;

   public ModbusTcpSlaveSession(Socket socket, BModbusTcpSlaveNetwork modbusNetwork) {
      this.socket = socket;
      this.modbusNetwork = modbusNetwork;
   }

   public boolean start() {
      try {
         this.socket.setSoTimeout(this.modbusNetwork.getSocketTimeoutInMillis());
         this.inStream = this.socket.getInputStream();
         this.outStream = this.socket.getOutputStream();
         this.done = false;
      } catch (Exception var3) {
         this.done = true;
         String errMsg = "ModbusTcpSlave Unable to get Socket input or output stream to client ";
         this.modbusNetwork.getModbusLog().error(errMsg, var3);
         throw new RuntimeException(errMsg);
      }

      this.rcvDriver = new ModbusTcpSlaveSession.ReceiveDriver(this);
      this.rcvThread = new Thread(this.rcvDriver, "ModTcpSlave:RcvSess");
      this.rcvThread.start();
      this.rcvThread.setPriority(5);
      return true;
   }

   public synchronized void stop() {
      try {
         this.done = true;
         if (this.rcvDriver != null) {
            this.rcvThread.interrupt();
         }

         this.rcvDriver = null;
      } catch (Exception var3) {
         this.modbusNetwork.getModbusLog().error("ModbusTcpSlaveSession.stop() rcvThread.interrupt() caught exception", var3);
      }

      if (this.socket != null) {
         try {
            this.socket.close();
         } catch (Exception var2) {
         }
      }

      this.inStream = null;
      this.outStream = null;
   }

   public void setSessionId(int id) {
      this.sessionId = id;
   }

   public synchronized void sendMessage(ModbusMessage msg) {
      try {
         if (this.modbusNetwork.getLog().isTraceOn()) {
            this.modbusNetwork.getLog().trace("**** ModbusTcpSlave Sending Message: " + msg.toDebugString());
         }

         this.output(msg);
      } catch (Exception var3) {
         this.modbusNetwork.getLog().error("ModbusTcpSlaveSession.sendMessage() caught exception ", var3);
      }
   }

   private void output(ModbusMessage msg) {
      synchronized (this.outStream) {
         try {
            msg.write(this.outStream);
            this.modbusNetwork.incrementSent();
         } catch (Exception var5) {
            this.modbusNetwork.getLog().error("Exception in ModbusTcpSlaveSession", var5);
         }
      }
   }

   class ReceiveDriver implements Runnable {
      ModbusTcpSlaveSession tcpSession = null;

      ReceiveDriver(ModbusTcpSlaveSession tcpSession) {
         this.tcpSession = tcpSession;
      }

      public void initModbusnMode() {
      }

      @Override
      public void run() {
         this.initModbusnMode();

         while (!ModbusTcpSlaveSession.this.done) {
            byte[] ibuf = new byte[261];

            int rxSize;
            try {
               rxSize = ModbusTcpSlaveSession.this.inStream.read(ibuf, 0, 261);
            } catch (Exception var7) {
               if (ModbusTcpSlaveSession.this.modbusNetwork.getLog().isTraceOn()) {
                  ModbusTcpSlaveSession.this.modbusNetwork.getLog().trace("ioException in ModbusTcpSlaveSession", var7);
               }

               ModbusTcpSlaveSession.this.done = true;

               try {
                  ModbusTcpSlaveSession.this.socket.close();
               } catch (Exception var5) {
                  var5.printStackTrace();
               }

               ModbusTcpSlaveSession.this.modbusNetwork.tcpServer().closeConnection(ModbusTcpSlaveSession.this.sessionId);
               break;
            }

            if (rxSize > 10) {
               byte[] rxData = new byte[rxSize - 6];
               System.arraycopy(ibuf, 6, rxData, 0, rxData.length);
               int transactionIdentifier = ibuf[0] << 8 & 0xFF00 | ibuf[1] & 255;
               this.messageComplete(rxData, transactionIdentifier);
            } else if (rxSize > 0) {
               ModbusTcpSlaveSession.this.modbusNetwork.getLog().message("Received message too small!!! rxSize = " + rxSize);
            } else {
               ModbusTcpSlaveSession.this.modbusNetwork.getLog().trace("*** closing socket *** thread = " + Thread.currentThread().getName());

               try {
                  ModbusTcpSlaveSession.this.socket.close();
                  ModbusTcpSlaveSession.this.done = true;
                  ModbusTcpSlaveSession.this.modbusNetwork.tcpServer().closeConnection(ModbusTcpSlaveSession.this.sessionId);
               } catch (Exception var6) {
                  ModbusTcpSlaveSession.this.modbusNetwork.getLog().error(" ****** Exception: ", var6);
                  ModbusTcpSlaveSession.this.done = true;
               }
            }
         }
      }

      private void messageComplete(byte[] a, int transactionIdentifier) {
         ModbusTcpSlaveSession.this.modbusNetwork.incrementReceived();
         if (ModbusTcpSlaveSession.this.modbusNetwork.getLog().isTraceOn()) {
            System.out.println("**** ModbusTcpSlave Received Bytes: ****");
            ByteArrayUtil.hexDump(a);
            System.out.println("****************************************");
         }

         UnsolicitedMessageElement message = new UnsolicitedMessageElement(a, this.tcpSession, transactionIdentifier);
         ModbusTcpSlaveSession.this.modbusNetwork.unsolicitedReceive().receiveMessage(message);
      }
   }
}
