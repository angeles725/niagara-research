package com.tridium.bacnet.util.tap;

import com.tridium.bacnet.util.point.BPooledWorkerPoint;
import com.tridium.bacnet.util.point.EventsPerSecond;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.makeNumeric(BUnit.getUnit(\"per second\"), 1)",
      override = true
   ), @NiagaraProperty(
      name = "logMessages",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "port",
      type = "String",
      defaultValue = "BForwardedMessageSink.PORT_DEFAULT",
      facets = {@Facet(
         name = "BFacets.MAX",
         value = "65535"
      ), @Facet(
         name = "BFacets.MIN",
         value = "49000"
      )}
   )})
public class BForwardedMessageSink extends BPooledWorkerPoint implements EventsPerSecond {
   public static final Property facets = newProperty(0, BFacets.makeNumeric(BUnit.getUnit("per second"), 1), null);
   public static final Property logMessages = newProperty(0, false, null);
   public static final Property port = newProperty(0, "49000", BFacets.make(BFacets.make("max", 65535), BFacets.make("min", 49000)));
   public static final Type TYPE = Sys.loadType(BForwardedMessageSink.class);
   private BForwardedMessageSink.MessageListener listener = null;
   private volatile boolean log = false;
   private volatile boolean done = false;
   private volatile Integer portNumber = null;
   private volatile DatagramSocket socket = null;
   private AtomicLong receivedMessages = new AtomicLong();
   private long lastUpdate = System.currentTimeMillis();
   private static final String PORT_DEFAULT = "49000";
   private static final Logger logger = Logger.getLogger("bacnetUtil.tap.forwarded.messages");

   public boolean getLogMessages() {
      return this.getBoolean(logMessages);
   }

   public void setLogMessages(boolean v) {
      this.setBoolean(logMessages, v, null);
   }

   public String getPort() {
      return this.getString(port);
   }

   public void setPort(String v) {
      this.setString(port, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
      this.restartSocket();
   }

   @Override
   public void stopped() throws Exception {
      super.stopped();
      this.stopSocket();
   }

   @Override
   public void changed(Property property, Context cx) {
      if (property.equals(port)) {
         this.portNumber = null;
         this.restartSocket();
      } else if (property.equals(enabled)) {
         this.done = !this.getEnabled();
         if (this.done) {
            this.stopSocket();
         }
      } else if (property.equals(logMessages)) {
         this.log = this.getLogMessages();
      }

      super.changed(property, cx);
   }

   private void stopSocket() {
      this.done = true;
      if (this.socket != null) {
         this.socket.close();
      }

      this.listener = null;
   }

   private void restartSocket() {
      this.stopSocket();
      if (this.getEnabled()) {
         try {
            this.done = false;
            this.listener = new BForwardedMessageSink.MessageListener();
            this.socket = new DatagramSocket(this.getPortNumber());
            this.listener.start();
         } catch (NumberFormatException var2) {
            logger.log(Level.SEVERE, "NumberFormatException occurred in restartSocket", (Throwable)var2);
         } catch (SocketException var3) {
            logger.log(Level.SEVERE, "SocketException occurred in restartSocket", (Throwable)var3);
         }
      }
   }

   public void onExecute(BStatusValue o, Context cx) {
      long t1 = this.lastUpdate;
      this.lastUpdate = System.currentTimeMillis();
      long messagesRecv = this.receivedMessages.getAndSet(0L);
      BStatusNumeric out = (BStatusNumeric)o;
      out.setValue(this.calculateEventsPerSecond(messagesRecv, t1, this.lastUpdate));
   }

   private int getPortNumber() {
      if (this.portNumber == null) {
         this.portNumber = Integer.parseInt(this.getPort());
      }

      return this.portNumber;
   }

   private class MessageListener extends Thread {
      public MessageListener() {
         super("forwardedMessageSink");
      }

      @Override
      public void run() {
         byte[] message = new byte[1500];

         while (!BForwardedMessageSink.this.done) {
            DatagramPacket packet = new DatagramPacket(message, message.length);

            try {
               BForwardedMessageSink.this.socket.receive(packet);
               BForwardedMessageSink.this.receivedMessages.incrementAndGet();
               byte[] data = new byte[packet.getLength()];
               System.arraycopy(message, 0, data, 0, packet.getLength());
               if (BForwardedMessageSink.this.log) {
                  BForwardedMessageSink.logger.fine("Received message from: " + packet.getAddress() + " on port " + BForwardedMessageSink.this.portNumber);
                  ByteArrayUtil.hexDump(data);
               }
            } catch (IOException var4) {
               BForwardedMessageSink.logger.log(Level.SEVERE, "Exception occurred in MessageListener", (Throwable)var4);
            }
         }
      }
   }
}
