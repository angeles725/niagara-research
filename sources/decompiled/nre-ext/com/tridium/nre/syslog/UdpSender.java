package com.tridium.nre.syslog;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class UdpSender extends SyslogSender {
   private final String serverHost;
   private final int serverPort;
   private volatile DatagramSocket socket;
   private boolean shutdown;

   protected UdpSender(SyslogManager syslogManager, BlockingQueue<Message> blockingQueue) {
      super(syslogManager, blockingQueue, "Syslog:UdpSender");
      this.serverHost = syslogManager.getServerHost();
      this.serverPort = syslogManager.getServerPort();
   }

   @Override
   public void run() {
      InetAddress address = null;

      while (!this.shutdown) {
         try {
            if (this.socket == null) {
               address = InetAddress.getByName(this.serverHost);
               this.socket = new DatagramSocket();
            }

            Message message = this.blockingQueue.poll(100L, TimeUnit.MILLISECONDS);
            if (message != null) {
               SyslogManager.LOG.fine("Processing syslog message");
               DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getLength(), address, this.serverPort);
               this.socket.send(packet);
               SyslogManager.LOG.fine("Syslog message sent");
            }
         } catch (InterruptedException ie) {
            this.releaseResources();
            return;
         } catch (Throwable e) {
            SyslogManager.LOG.log(Level.WARNING, "Unable to send message packets via UDP socket: " + e.getMessage(), e);
            this.releaseResources();

            try {
               Thread.sleep(5000L);
            } catch (InterruptedException ie) {
               return;
            }
         }
      }

      SyslogManager.LOG.fine("Log sending complete");
   }

   private void releaseResources() {
      try {
         if (this.socket != null) {
            this.socket.close();
         }
      } catch (Throwable var2) {
      }

      this.socket = null;
   }

   @Override
   public void shutdown() {
      SyslogManager.LOG.fine("Shutting down...");
      this.shutdown = true;
      this.interrupt();
   }
}
