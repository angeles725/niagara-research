package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.firewall.BServerPort;
import javax.net.ssl.SSLException;
import org.bouncycastle.tls.TlsException;

public abstract class FoxServer {
   private static final String FOX = "fox";
   private static final String FOXS = "foxs";
   static Logger log = Logger.getLogger("fox");
   protected BServerPort foxPort;
   protected BServerPort foxsPort;
   private boolean alive = true;
   private Thread foxThread;
   private ServerSocket foxServerSocket;
   private boolean foxRunning = false;
   private Thread foxsThread;
   private ServerSocket foxsServerSocket;
   private boolean foxsRunning = false;
   private MulticastServer multicastServer;

   public FoxServer(BServerPort foxPort, BServerPort foxsPort) {
      this.foxPort = foxPort;
      this.foxsPort = foxsPort;
   }

   public MulticastServer getMulticastServer() {
      return this.multicastServer;
   }

   public void run() throws Exception {
      if (this.foxThread == null && this.foxsThread == null) {
         this.alive = true;
         if (this.foxPort != null) {
            this.foxThread = new Thread(new FoxServer.MainLoop("fox"), "Fox:Server");
            this.foxThread.start();
         }

         if (this.foxsPort != null) {
            this.foxsThread = new Thread(new FoxServer.MainLoop("foxs"), "Foxs:Server");
            this.foxsThread.start();
         }

         try {
            if (!Fox.ipv6Enabled && !Fox.ipv4Enabled) {
               System.err.println("WARNING: Disabling Fox multicast, no local addresses detected");
               Fox.multicastEnabled = false;
            }

            if (Fox.multicastEnabled) {
               this.multicastServer = new MulticastServer(this);
               this.multicastServer.start();
            } else {
               log.info("Multicast disabled (no station discovery supported)");
            }
         } catch (Exception var2) {
            var2.printStackTrace();
            System.out.println("ERROR: Cannot open Fox multicast socket: " + var2);
         }
      } else {
         throw new IllegalStateException("Server already running");
      }
   }

   private void runFox() {
      this.foxRunning = true;
      this.run("fox");
      this.foxRunning = false;
   }

   private void runFoxs() {
      this.foxsRunning = true;
      this.run("foxs");
      this.foxsRunning = false;
   }

   public boolean isServing() {
      return this.foxServerSocket != null || this.foxsServerSocket != null;
   }

   public boolean isRunning() {
      return this.foxRunning || this.foxsRunning;
   }

   public boolean isFoxRunning() {
      return this.foxRunning;
   }

   public boolean isFoxsRunning() {
      return this.foxsRunning;
   }

   public ServerSocket getFoxServerSocket() throws IOException {
      return this.foxPort.getBindToLoopback()
         ? new ServerSocket(this.foxPort.getBindingPort(), 10, InetAddress.getByName(null))
         : new ServerSocket(this.foxPort.getBindingPort(), 10);
   }

   public ServerSocket getFoxsServerSocket() throws IOException {
      throw new UnsupportedOperationException("FOXS not supported.");
   }

   private void run(String scheme) {
      ServerSocket server = null;
      BServerPort port = null;

      while (this.alive) {
         try {
            if (scheme.equalsIgnoreCase("fox")) {
               port = this.foxPort;
               server = this.foxServerSocket = this.getFoxServerSocket();
            } else {
               port = this.foxsPort;
               server = this.foxsServerSocket = this.getFoxsServerSocket();
            }
         } catch (BindException var8) {
            log.severe(scheme.toUpperCase() + " server failed to bind to port [" + port.getPublicServerPort() + "] " + var8);
            server = null;

            try {
               Thread.sleep(5000L);
            } catch (InterruptedException var7) {
            }
         } catch (IllegalArgumentException var9) {
            log.severe(scheme.toUpperCase() + " server failed to start on port [" + port.getPublicServerPort() + "]\n " + var9);
            return;
         } catch (IOException var10) {
            log.severe(scheme.toUpperCase() + " server failed to start on port. [" + port.getPublicServerPort() + "]\n " + var10);
            return;
         }

         if (server != null) {
            try {
               log.info(scheme.toUpperCase() + " server started on port [" + port.getPublicServerPort() + "]");
               Socket s = null;

               while (this.alive) {
                  try {
                     s = server.accept();
                     if (!this.alive) {
                        break;
                     }

                     Tuner.openServer(this, s, scheme);
                  } catch (IOException var11) {
                     if (this.alive && (var11 instanceof SSLException || var11 instanceof TlsException)) {
                        if (log.isLoggable(Level.FINE)) {
                           log.log(Level.WARNING, "Server accept SSLHandshakeException: " + var11.getLocalizedMessage(), s);
                        } else {
                           log.warning("Server accept SSLHandshakeException: " + var11.getLocalizedMessage());
                        }
                     } else if (this.alive) {
                        log.info("Server accept acception: " + var11.getLocalizedMessage());
                     }
                  } catch (Throwable var12) {
                     if (log.isLoggable(Level.FINE)) {
                        log.log(Level.WARNING, "Server accept exception:" + var12.getLocalizedMessage(), var12);
                     }
                  }
               }
            } catch (Throwable var13) {
               if (this.alive) {
                  log.log(Level.SEVERE, scheme + ": Error in main loop.", var13);
               }
            }
         }
      }

      log.info(scheme.toUpperCase() + " server stopped on port [" + port.getPublicServerPort() + "]");
      if (server != null) {
         try {
            server.close();
         } catch (Exception var6) {
         }
      }
   }

   public void stop() {
      if (this.foxThread != null) {
         this.alive = false;
         this.foxThread.interrupt();
         this.foxThread = null;
         if (this.foxServerSocket != null) {
            try {
               this.foxServerSocket.close();
            } catch (Exception var3) {
            }

            this.foxServerSocket = null;
         }
      }

      if (this.foxsThread != null) {
         this.alive = false;
         this.foxsThread.interrupt();
         this.foxsThread = null;
         if (this.foxsServerSocket != null) {
            try {
               this.foxsServerSocket.close();
            } catch (Exception var2) {
            }

            this.foxsServerSocket = null;
         }
      }

      if (this.multicastServer != null) {
         this.multicastServer.kill();
         this.multicastServer = null;
      }
   }

   public abstract FoxConnection makeConnection(FoxSession var1, FoxMessage var2) throws Exception;

   public abstract FoxMessage getAnnouncement(FoxMessage var1);

   public abstract boolean authenticateBasic(FoxSession var1, String var2, String var3) throws Exception;

   public abstract boolean authenticateDigest(FoxSession var1, String var2, byte[] var3, byte[] var4) throws Exception;

   public void connectionAuthenticated(FoxConnection conn, FoxSession session, FoxMessage remoteHello) throws Exception {
   }

   private class MainLoop implements Runnable {
      private final String scheme;

      public MainLoop(String scheme) {
         this.scheme = scheme;
      }

      @Override
      public void run() {
         if (this.scheme.equals("fox")) {
            FoxServer.this.runFox();
         } else {
            FoxServer.this.runFoxs();
         }
      }
   }
}
