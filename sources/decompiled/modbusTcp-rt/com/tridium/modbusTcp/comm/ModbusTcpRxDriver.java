package com.tridium.modbusTcp.comm;

import com.tridium.basicdriver.comm.CommReceiver;
import com.tridium.basicdriver.message.Message;
import com.tridium.basicdriver.message.ReceivedMessage;
import com.tridium.modbusCore.BModbusNetwork;
import com.tridium.modbusCore.ModbusException;
import com.tridium.modbusCore.messages.ModbusMessage;
import com.tridium.modbusCore.messages.ModbusReceivedMessage;
import com.tridium.modbusTcp.BModbusTcpDevice;
import com.tridium.modbusTcp.BModbusTcpGateway;
import com.tridium.modbusTcp.BModbusTcpNetwork;
import com.tridium.modbusTcp.BSocketStatusEnum;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;

public class ModbusTcpRxDriver extends CommReceiver {
   private Log rxLog;
   private BModbusTcpDevice modbusTcpDevice;
   private BModbusTcpGateway modbusTcpGateway;
   private boolean rxProcessMode = false;
   private Socket commSocket;
   private OutputStream out = null;
   protected int numOutstandingRequests = 0;
   protected int numConnectionFailures = 0;
   protected int state = 0;
   protected boolean exit = false;
   private Object idleMonitor = new Object();
   private Object connectMonitor = new Object();
   protected BModbusNetwork network = null;
   protected ModbusReceivedMessage msg = null;
   protected ModbusMessage requestMessage = null;
   static Log socketLog = Log.getLog("modbusTcp_Socket");
   protected static final int STATE_IDLE = 0;
   protected static final int STATE_NO_SOCKET = 1;
   protected static final int STATE_GOT_SOCKET = 2;
   private static final boolean RX_MODE_PACKET = true;
   private static final boolean RX_MODE_BYTE = false;

   public ModbusTcpRxDriver(BModbusNetwork network) {
      this.network = network;
      this.switchState(0);
   }

   public void startSocketManager() {
      this.switchState(1);
      this.exit = false;
      this.modbusTcpDevice = (BModbusTcpDevice)((ModbusTcpComm)this.getComm()).getDevice();
      String logName = this.network.getName();
      if (this.modbusTcpDevice == null) {
         logName = logName + "_rx";
      } else {
         logName = logName + "_" + this.modbusTcpDevice.getName() + "_rx";
      }

      if (!SlotPath.isValidName(logName)) {
         logName = SlotPath.escape(logName);
      }

      this.rxLog = Log.getLog(logName);
   }

   public void writeOutputStream(Message message) throws Exception {
      if (this.isConnected() && this.out != null) {
         try {
            this.requestMessage = (ModbusMessage)message;
         } catch (Exception var3) {
            this.requestMessage = null;
         }

         this.numOutstandingRequests++;
         message.write(this.out);
         this.out.flush();
      } else {
         throw new ModbusException(104);
      }
   }

   protected ReceivedMessage receive() throws Exception {
      boolean gotMessage = false;

      while (!gotMessage && this.isAlive()) {
         gotMessage = this.finiteStateMachine();
      }

      return this.msg;
   }

   private boolean isConnected() {
      return this.state == 2;
   }

   protected synchronized void switchState(int newState) {
      if (this.state != newState) {
         this.state = newState;
         if (newState == 1) {
            this.numConnectionFailures = 0;
         }

         synchronized (this.idleMonitor) {
            this.idleMonitor.notifyAll();
         }
      }
   }

   public void stopSocketManager() {
      this.detailsLn("entered method stopSocketManager");
      this.exit = true;
   }

   private boolean finiteStateMachine() {
      if (this.exit) {
         return false;
      } else if (this.state == 2) {
         return this.readMessage();
      } else {
         synchronized (this.idleMonitor) {
            try {
               this.idleMonitor.wait(5000L);
            } catch (InterruptedException var4) {
            }

            return false;
         }
      }
   }

   private boolean readMessage() {
      this.setSocketTimeout();
      return this.readMessageFromStream();
   }

   protected boolean readMessageFromStream() {
      byte[] ibuf = new byte[261];
      int rxSize = 0;
      this.rxProcessMode = this.modbusTcpDevice != null ? this.modbusTcpDevice.getRxProcessMode() : ((BModbusTcpGateway)this.network).getRxProcessMode();

      try {
         if (this.rxProcessMode) {
            rxSize = this.getInputStream().read(ibuf, 0, 261);
         } else {
            InputStream in = this.getInputStream();
            int dataLen = 256;

            do {
               int x = in.read();
               this.rxLog.trace("rc= " + x + " rs= " + rxSize + " dl= " + dataLen);
               if (x < 0) {
                  throw new SocketException("End of stream.");
               }

               ibuf[rxSize++] = (byte)x;
               if (rxSize == 6) {
                  dataLen = x;
               }
            } while (rxSize < dataLen + 6);
         }

         if (this.rxLog.isTraceOn() && rxSize > 0) {
            this.rxLog.trace("received bytes= ", ibuf, 0, rxSize);
         }

         if (rxSize >= 9) {
            if (0 != (ibuf[7] & 128) && this.network.getLog().isTraceOn()) {
               this.network.getLog().trace("MODBUS exception response - type " + ibuf[8]);
            }

            byte[] rxData = new byte[rxSize - 6];
            System.arraycopy(ibuf, 6, rxData, 0, rxData.length);
            int transactionId = (ibuf[0] & 255) << 8;
            transactionId |= ibuf[1] & 255;
            this.numOutstandingRequests = 0;
            if (this.msg == null) {
               this.msg = new ModbusReceivedMessage(rxData, rxData.length);
            } else {
               this.msg.setBytes(rxData);
               this.msg.setLength(rxData.length);
            }

            this.msg.setTransactionId(transactionId);
            if ((this.modbusTcpDevice == null || !this.modbusTcpDevice.getDisableTransactionIdCheck())
               && this.requestMessage != null
               && this.requestMessage.transactionIdentifier != transactionId) {
               if (this.network.getLog().isTraceOn()) {
                  this.network.getLog().trace("Transaction ID missmatch: req/resp = " + this.requestMessage.transactionIdentifier + "/" + transactionId);
               }

               return false;
            }

            return true;
         }

         if (rxSize > 0) {
            this.network.getLog().message("response was too short - " + rxSize + " chars");
         } else if (rxSize == -1) {
            this.switchState(1);
            this.nullStreams();
            this.closeSocket();
            if (this.modbusTcpDevice != null) {
               socketLog.trace(this.network.getDisplayName(null) + "." + this.modbusTcpDevice.getDisplayName(null) + ": socket read Timeout, socket closed");
            }
         }
      } catch (Exception var6) {
         if (var6 instanceof SocketException || var6 instanceof SocketTimeoutException || this.commSocket == null || !this.commSocket.isConnected()) {
            this.switchState(1);
            this.nullStreams();
            this.closeSocket();
            if (this.modbusTcpDevice != null) {
               socketLog.trace(
                  this.network.getDisplayName(null) + "." + this.modbusTcpDevice.getDisplayName(null) + ": socket closed Socket Timeout or Exception"
               );
            }
         }
      }

      return false;
   }

   protected void setSocketTimeout() {
      try {
         int soTimeout = (int)((BModbusTcpNetwork)this.network).getSocketOptionTimeout().getMillis();
         this.commSocket.setSoTimeout(soTimeout);
      } catch (Exception var2) {
         this.detailsLn("exception caught setting so timeout, e=" + var2);
      }
   }

   public void closeSocket() {
      if (this.commSocket != null) {
         try {
            this.commSocket.close();
         } catch (Exception var2) {
            this.detailsLn("exception caught from closeSocket() while closing commSocket" + var2);
         }

         this.commSocket = null;
         if (this.modbusTcpDevice != null) {
            this.modbusTcpDevice.setSocketStatus(BSocketStatusEnum.closed);
         } else if (this.modbusTcpGateway != null) {
            this.modbusTcpGateway.setSocketStatus(BSocketStatusEnum.closed);
         }
      }
   }

   private void failureSocketConnectionInit(Exception e) {
      if (socketLog.isTraceOn() && this.modbusTcpDevice != null) {
         socketLog.trace(this.modbusTcpDevice.getDisplayName(null) + ": open socket failed. Exception: " + e);
      } else {
         this.detailsLn(" ModbusTcp - Cannot open socket connection to " + ((ModbusTcpComm)this.getComm()).devName() + ".");
      }

      this.numConnectionFailures++;
      this.nullStreams();
      this.closeSocket();
   }

   private void notifyConnectMonitor() {
      synchronized (this.connectMonitor) {
         this.connectMonitor.notifyAll();
      }
   }

   private void successSocketConnectionInit() {
      this.detailsLn(" ModbusTcp - socket connection established to " + ((ModbusTcpComm)this.getComm()).devName() + ".");
      this.switchState(2);
      this.numOutstandingRequests = 0;
      this.notifyConnectMonitor();
   }

   private void connectSocket() throws Exception {
      this.modbusTcpDevice = (BModbusTcpDevice)((ModbusTcpComm)this.getComm()).getDevice();
      if (this.network instanceof BModbusTcpGateway) {
         this.modbusTcpGateway = (BModbusTcpGateway)this.network;
      }

      InetAddress ip = this.modbusTcpDevice != null ? this.modbusTcpDevice.getInetAddr() : ((BModbusTcpGateway)this.network).getInetAddr();
      if (ip == null) {
         throw new ModbusException(104);
      } else {
         if (this.modbusTcpDevice != null) {
            this.modbusTcpDevice.setSocketStatus(BSocketStatusEnum.openPending);
            this.commSocket = new Socket(ip, this.modbusTcpDevice.getPort());
            if (socketLog.isTraceOn()) {
               socketLog.trace(this.network.getDisplayName(null) + "." + this.modbusTcpDevice.getDisplayName(null) + ": socket opened");
            } else {
               this.detailsLn("Socket connection made to device " + ((ModbusTcpComm)this.getComm()).devName() + ".");
            }
         } else if (this.modbusTcpGateway != null) {
            this.modbusTcpGateway.setSocketStatus(BSocketStatusEnum.openPending);
            this.commSocket = new Socket(ip, ((BModbusTcpGateway)this.network).getPort());
            this.detailsLn("Socket connection made to gateway " + ((ModbusTcpComm)this.getComm()).devName() + ".");
         }

         if (this.commSocket != null) {
            if (this.modbusTcpDevice != null) {
               this.modbusTcpDevice.setSocketStatus(BSocketStatusEnum.opened);
            } else if (this.modbusTcpGateway != null) {
               this.modbusTcpGateway.setSocketStatus(BSocketStatusEnum.opened);
            }

            try {
               this.setSocketTimeout();
            } catch (Exception var3) {
               this.detailsLn("exception caught setting so timeout, e=" + var3);
            }
         } else if (this.modbusTcpDevice != null) {
            this.modbusTcpDevice.setSocketStatus(BSocketStatusEnum.openFailed);
         } else if (this.modbusTcpGateway != null) {
            this.modbusTcpGateway.setSocketStatus(BSocketStatusEnum.openFailed);
         }
      }
   }

   public void nullStreams() {
      this.setInputStream(null);
      this.out = null;
   }

   private void createStreams() throws IOException {
      this.setInputStream(this.commSocket.getInputStream());
      this.out = this.commSocket.getOutputStream();
   }

   public synchronized void initSocketConnection() {
      try {
         this.nullStreams();
         this.closeSocket();
         this.connectSocket();
         this.createStreams();
         this.successSocketConnectionInit();
      } catch (Exception var2) {
         if (this.modbusTcpDevice != null) {
            this.modbusTcpDevice.setSocketStatus(BSocketStatusEnum.openFailed);
         } else if (this.modbusTcpGateway != null) {
            this.modbusTcpGateway.setSocketStatus(BSocketStatusEnum.openFailed);
         }

         this.failureSocketConnectionInit(var2);
      }
   }

   protected void detailsLn(String details) {
      try {
         if (this.network.getLog().isTraceOn()) {
            this.network.getLog().trace(((ModbusTcpComm)this.getComm()).devName() + "(MbsSoMan driver)-" + details);
         }
      } catch (Exception var3) {
      }
   }
}
