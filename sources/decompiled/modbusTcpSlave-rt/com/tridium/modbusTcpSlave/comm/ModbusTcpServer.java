package com.tridium.modbusTcpSlave.comm;

import com.tridium.modbusTcpSlave.BModbusTcpSlaveNetwork;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.baja.firewall.BServerPort;

public class ModbusTcpServer implements Runnable {
   private BServerPort port;
   private boolean isAlive = false;
   private Thread thread;
   private ServerSocket serverSocket;
   private BModbusTcpSlaveNetwork host;
   private ModbusTcpSlaveSession[] sessions;

   public ModbusTcpServer(BModbusTcpSlaveNetwork host) {
      this.host = host;
      this.sessions = new ModbusTcpSlaveSession[host.getMaximumConnections()];
   }

   public void start() {
      System.out.println("ModbusTcpServer.start()");
      this.host.setCurrentConnections(0);
      if (this.thread != null) {
         throw new IllegalStateException("Server already running");
      } else {
         this.port = this.host.getPort();
         this.isAlive = true;
         this.thread = new Thread(this, "ModTcpSlave:Server");
         this.thread.start();
      }
   }

   public void stop() {
      this.host.setCurrentConnections(0);
      if (this.thread != null) {
         this.isAlive = false;
         this.thread.interrupt();
         this.stopSessions();

         try {
            this.serverSocket.close();
         } catch (Exception var2) {
         }

         this.thread = null;
         this.serverSocket = null;
      }
   }

   @Override
   public void run() {
      this.openPort();
      this.acceptSessions();
   }

   private void openPort() {
      while (this.isAlive) {
         try {
            if (this.port.getBindToLoopback()) {
               this.serverSocket = new ServerSocket(this.port.getBindingPort(), 50, InetAddress.getByName(null));
            } else {
               this.serverSocket = new ServerSocket(this.port.getBindingPort(), 50);
            }

            this.host
               .getModbusLog()
               .message("ModbusTcpSlave server started on port [" + this.port.getPublicServerPort() + "] (" + this.port.getBindingPort() + ")");
            return;
         } catch (Throwable var3) {
            this.host.getModbusLog().error("Cannot open ModbusTcpSlave server port [" + this.port.getPublicServerPort() + "] ", var3);

            try {
               Thread.sleep(5000L);
            } catch (InterruptedException var2) {
            }
         }
      }
   }

   private void acceptSessions() {
      while (this.isAlive) {
         try {
            if (this.getNumberSessions() < this.sessions.length) {
               Socket socket = this.serverSocket.accept();
               this.host.getModbusLog().trace("ModbusTcpServer acceptSession with: " + socket);
               ModbusTcpSlaveSession session = new ModbusTcpSlaveSession(socket, this.host);
               int sessionId = this.addSession(session, socket);
               if (sessionId < 0) {
                  this.host.getModbusLog().error("*** ModbusTcpServer.addSession() returned -1 for some reason.  Should never get here!!!!");
               } else {
                  session.setSessionId(sessionId);
                  session.start();
                  this.host.setCurrentConnections(this.getNumberSessions());
               }
            } else {
               Thread.sleep(1000L);
            }
         } catch (Throwable var4) {
            if (this.isAlive) {
               var4.printStackTrace();
            }
         }
      }
   }

   public void closeConnection(int sessionId) {
      this.host.getModbusLog().trace("closeConnection with sessionId: " + sessionId);
      this.removeSession(sessionId);
      this.host.setCurrentConnections(this.getNumberSessions());
   }

   private int addSession(ModbusTcpSlaveSession session, Socket socket) {
      this.host.getModbusLog().trace("ModbusTcpServer.addSession with: " + socket);

      for (int i = 0; i < this.sessions.length; i++) {
         if (this.sessions[i] == null) {
            this.host.getModbusLog().trace("    found slot at: " + i);
            this.sessions[i] = session;
            return i;
         }
      }

      return -1;
   }

   private void removeSession(int id) {
      this.sessions[id] = null;
   }

   private int getNumberSessions() {
      int count = 0;

      for (int i = 0; i < this.sessions.length; i++) {
         if (this.sessions[i] != null) {
            count++;
         }
      }

      return count;
   }

   private void stopSessions() {
      for (int i = 0; i < this.sessions.length; i++) {
         if (this.sessions[i] != null) {
            this.sessions[i].stop();
            this.sessions[i] = null;
         }
      }
   }

   public void tcpServerDump() {
      System.out.println(" tcpSessions:");

      for (int i = 0; i < this.sessions.length; i++) {
         if (this.sessions[i] != null) {
            System.out.println("   " + this.sessions[i].socket);
         }
      }

      System.out.println("***** Node.dump()end *****");
   }

   public void removeOldSessions(int maximumConnections) {
      ModbusTcpSlaveSession[] temp = new ModbusTcpSlaveSession[maximumConnections];

      for (int i = 0; i < this.sessions.length; i++) {
         if (this.sessions[i] != null) {
            if (i < temp.length) {
               temp[i] = this.sessions[i];
            } else {
               this.sessions[i].stop();
               this.sessions[i] = null;
            }
         }
      }

      this.sessions = temp;
   }

   public boolean isStarted() {
      return this.isAlive;
   }
}
