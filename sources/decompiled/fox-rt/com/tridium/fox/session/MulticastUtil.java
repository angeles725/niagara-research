package com.tridium.fox.session;

import com.tridium.fox.message.FoxBoolean;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.message.MessageReader;
import com.tridium.fox.message.MessageWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

public class MulticastUtil {
   private static final int MULTICAST_PACKET_SIZE = 1024;

   public static FoxFrame makeFrame(String command, FoxMessage msg) {
      return new FoxFrame(109, -1, -1, "fox", command, msg);
   }

   public static void send(String command, FoxMessage msg, int sendCount) throws Exception {
      send(makeFrame(command, msg), sendCount);
   }

   public static void send(FoxFrame frame, int sendCount) throws Exception {
      if ((Fox.ipv4Enabled || Fox.ipv6Enabled) && Fox.multicastEnabled) {
         ByteArrayOutputStream bout = new ByteArrayOutputStream(256);
         MessageWriter out = new MessageWriter(bout);
         frame.write(out);
         byte[] buf = bout.toByteArray();
         DatagramPacket v4packet = Fox.ipv4Enabled ? new DatagramPacket(buf, buf.length, InetAddress.getByName(Fox.MULTICAST_ADDRESS), 1911) : null;
         DatagramPacket v6packet = Fox.ipv6Enabled ? new DatagramPacket(buf, buf.length, InetAddress.getByName(Fox.IPV6_MULTICAST_ADDRESS), 1911) : null;
         MulticastSocket socket = new MulticastSocket();
         socket.setTimeToLive(Fox.multicastTimeToLive);
         IOException ipv4Exception = null;
         IOException ipv6Exception = null;
         boolean sentIPv4 = false;
         boolean sentIPv6 = false;

         try {
            if (v4packet != null) {
               for (int i = 0; i < sendCount; i++) {
                  Thread.sleep(100L);

                  try {
                     AccessController.doPrivileged(new MulticastUtil.SocketSendPrivilegedAction(socket, v4packet));
                  } catch (PrivilegedActionException var16) {
                     throw var16.getException();
                  }
               }

               sentIPv4 = true;
            }
         } catch (IOException var18) {
            System.err.println("WARNING: Could not send IPv4 multicast packet: " + var18);
            sentIPv4 = false;
            ipv4Exception = var18;
         }

         try {
            if (v6packet != null) {
               for (int i = 0; i < sendCount; i++) {
                  Thread.sleep(100L);

                  try {
                     AccessController.doPrivileged(new MulticastUtil.SocketSendPrivilegedAction(socket, v6packet));
                  } catch (PrivilegedActionException var15) {
                     throw var15.getException();
                  }
               }

               sentIPv6 = true;
            }
         } catch (IOException var17) {
            System.err.println("WARNING: Could not send IPv6 multicast packet: " + var17);
            sentIPv6 = false;
            ipv6Exception = var17;
         }

         if (!sentIPv6 && !sentIPv4) {
            String ipv4FailureCause = Fox.ipv4Enabled && ipv4Exception != null ? ipv4Exception.getMessage() : null;
            String ipv6FailureCause = Fox.ipv6Enabled && ipv6Exception != null ? ipv6Exception.getMessage() : null;
            String failureCause = "OK";
            if (ipv4FailureCause != null) {
               failureCause = ipv4FailureCause;
            }

            if (ipv6FailureCause != null) {
               failureCause = ipv4FailureCause == null ? ipv6FailureCause : failureCause + ", " + ipv6FailureCause;
            }

            throw new IOException("ERROR: Could not send Fox multicast packet(s): (" + failureCause + ")");
         }
      }
   }

   public static FoxFrame receive(DatagramSocket socket) throws Exception {
      byte[] buf = new byte[1024];
      DatagramPacket packet = new DatagramPacket(buf, buf.length);

      try {
         AccessController.doPrivileged((PrivilegedExceptionAction<Void>)(() -> {
            socket.receive(packet);
            return null;
         }));
      } catch (PrivilegedActionException var6) {
         throw var6.getException();
      }

      ByteArrayInputStream bin = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
      MessageReader in = new MessageReader(bin);
      in.setReadLimit(1024L);
      FoxFrame receivedFrame = FoxFrame.read(in);
      if (packet.getAddress() instanceof Inet6Address) {
         receivedFrame.message.add("IPv6FoxFrameMulticastReceived", true);
      }

      return receivedFrame;
   }

   public static boolean isIpv6Message(FoxMessage msg) {
      FoxTuple[] list = msg.list("IPv6FoxFrameMulticastReceived");
      boolean ipV6 = false;
      if (list != null && list.length != 0) {
         FoxBoolean targetTuple = (FoxBoolean)list[list.length - 1];
         ipV6 = targetTuple.value;
      }

      return ipV6;
   }

   private static class SocketSendPrivilegedAction implements PrivilegedExceptionAction<Object> {
      private final MulticastSocket socket;
      private final DatagramPacket packet;

      private SocketSendPrivilegedAction(MulticastSocket socket, DatagramPacket packet) {
         this.socket = socket;
         this.packet = packet;
      }

      @Override
      public Object run() throws Exception {
         this.socket.send(this.packet);
         return null;
      }
   }
}
