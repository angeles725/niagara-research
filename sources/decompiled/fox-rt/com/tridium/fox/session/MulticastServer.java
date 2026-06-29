package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.MessageReader;
import com.tridium.fox.message.MessageWriter;
import com.tridium.nre.util.IPAddressUtil;
import com.tridium.nre.util.NamedThreadFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.sys.Clock;
import javax.baja.sys.LocalizableRuntimeException;

public class MulticastServer extends Thread {
   private FoxServer server;
   private boolean isAlive = true;
   private Object rollcallLock = new Object();
   private Vector<FoxMessage> announcements;
   private Collection<DatagramChannel> receiveChannels;
   private Selector receiveSelector;
   private static final int MAX_NUM_ROLLCALL_MESSAGES = 4;
   private static final int MIN_ROLLCALL_INTERVAL_MS = 500;
   private ScheduledThreadPoolExecutor scheduledExecutor;
   private static final int MAX_SEND_QUEUE_SIZE = 20;
   private long lastAnnouncementTimeMS = 0L;
   private static final int ANNOUNCEMENT_INTERVAL_FIFO_SIZE = 5;
   private int announcementIntervalMSPos = 0;
   private long[] announcementIntervalMS = new long[5];
   private long totalAnnouncementIntervalMS = 0L;
   private static final int MIN_ANNOUNCEMENT_INTERVAL_MS = 25;
   private static final int MAX_ANNOUNCEMENT_WAIT_MS = 30000;
   private static final int GUARD_MS = 50;
   public static final String RESPONSE_DELAY_KEY = "responseDelay";
   public static final String RESPONSE_INTERVAL_KEY = "responseInterval";
   private static final Logger logger = Logger.getLogger(MulticastServer.class.getName());

   public MulticastServer(FoxServer server) throws IOException {
      super(Fox.threadGroup, "Fox:MulticastServer");
      if (!Fox.multicastEnabled) {
         throw new LocalizableRuntimeException("fox", "error.multicastDisabled");
      } else {
         this.server = server;
         this.createReceiveChannels();
         this.scheduledExecutor = new ScheduledThreadPoolExecutor(0, new NamedThreadFactory("MulticastServerResponse"));
         this.scheduledExecutor.setMaximumPoolSize(1);
         this.scheduledExecutor.setKeepAliveTime(60000L, TimeUnit.MILLISECONDS);
      }
   }

   protected void createReceiveChannels() throws IOException {
      this.receiveSelector = Selector.open();
      this.receiveChannels = new ArrayList<>();

      for (StandardProtocolFamily protocolFamily : this.getEnabledFoxIPProtocols()) {
         DatagramChannel channel = this.createReceiveChannel(protocolFamily);
         this.receiveChannels.add(channel);
         channel.register(this.receiveSelector, 1);
      }
   }

   protected DatagramChannel createReceiveChannel(final StandardProtocolFamily protocolFamily) throws IOException {
      try {
         return AccessController.doPrivileged(
            new PrivilegedExceptionAction<DatagramChannel>() {
               public DatagramChannel run() throws IOException {
                  DatagramChannel channel = DatagramChannel.open(protocolFamily);

                  try {
                     channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                     channel.bind(new InetSocketAddress(1911));
                     channel.configureBlocking(false);

                     for (NetworkInterface networkInterface : MulticastServer.this.getMulticastInterfaces(
                        ifc -> MulticastServer.this.isProtocolSupported(ifc, protocolFamily)
                     )) {
                        channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
                        channel.join(MulticastServer.this.getFoxMulticastAddress(protocolFamily), networkInterface);
                        MulticastServer.logger
                           .fine(
                              () -> String.format("Joined multicast group on interface %s for protocol %s", networkInterface.getDisplayName(), protocolFamily)
                           );
                     }

                     return channel;
                  } catch (RuntimeException | IOException var4) {
                     channel.close();
                     throw var4;
                  }
               }
            }
         );
      } catch (PrivilegedActionException var3) {
         throw (IOException)var3.getException();
      }
   }

   public FoxMessage[] rollcall(FoxMessage message, long wait, MulticastServer.RollcallCallback cb) throws Exception {
      synchronized (this.rollcallLock) {
         Vector<FoxMessage> v = this.announcements = new Vector<>();
         long startTimeMS = Clock.ticks();
         int numRollcallMessages = Math.min((int)wait / 500, 4);

         for (int i = 0; i < numRollcallMessages; i++) {
            long sendTime = wait * i / numRollcallMessages;
            int sequenceNum = i;
            ScheduledFuture var14 = this.scheduledExecutor
               .schedule(() -> this.sendRollcallMessage(message, wait, startTimeMS, numRollcallMessages, sequenceNum), sendTime, TimeUnit.MILLISECONDS);
         }

         long waitPercents = wait / 100L;

         for (int i = 0; i < 100; i++) {
            Thread.sleep(waitPercents);
            if (cb != null) {
               cb.completed(i);
            }
         }

         this.announcements = null;
         logger.fine(() -> String.format("Received %d announcement messages during rollcall", v.size()));
         FoxMessage[] result = new FoxMessage[v.size()];
         v.copyInto(result);
         return result;
      }
   }

   protected boolean sendRollcallMessage(FoxMessage messagePrototype, long totalTimeMS, long startTimeMS, int totalNumMessages, int sequenceNum) {
      boolean success = false;

      try {
         FoxMessage message = new FoxMessage(messagePrototype);
         long currentTimeMS = Clock.ticks();
         this.setRollcallTimingParams(message, totalTimeMS, startTimeMS, currentTimeMS, totalNumMessages, sequenceNum);
         ByteBuffer buffer = this.createByteBuffer(MulticastUtil.makeFrame("rollcall", message));

         for (NetworkInterface networkInterface : this.getMulticastInterfaces(ifc -> !ifc.isLoopback())) {
            for (StandardProtocolFamily protocolFamily : this.getEnabledFoxIPProtocols()) {
               if (this.isProtocolSupported(networkInterface, protocolFamily) && this.sendMulticastMessage(buffer, networkInterface, protocolFamily)) {
                  success = true;
                  logger.fine(() -> String.format("Sent rollcall on interface %s using protocol %s", networkInterface, protocolFamily));
               }
            }
         }
      } catch (IOException var17) {
         logger.log(Level.SEVERE, "Failed to send Fox discovery rollcall message", (Throwable)var17);
      }

      return success;
   }

   protected ByteBuffer createByteBuffer(FoxFrame frame) throws IOException {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
      MessageWriter out = new MessageWriter(byteArrayOutputStream);
      frame.write(out);
      return ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
   }

   protected void setRollcallTimingParams(FoxMessage message, long totalTimeMS, long startTimeMS, long currentTimeMS, int totalNumMessages, int sequenceNum) {
      long elapsedTimeMS = currentTimeMS - startTimeMS;
      long responseDelay = Math.max(0L, 350L - elapsedTimeMS);
      long responseInterval = Math.max(0L, totalTimeMS / totalNumMessages - responseDelay - 50L);
      message.add("responseDelay", (int)responseDelay);
      message.add("responseInterval", (int)responseInterval);
      logger.finest(
         () -> String.format(
            "Calculated timing parameters for rollcall message %d at %dms elapsed time: responseDelay = %d, responseInterval = %d",
            sequenceNum,
            elapsedTimeMS,
            responseDelay,
            responseInterval
         )
      );
   }

   protected void updateAnnouncementAddress(InetAddress sourceAddress, FoxMessage announcement) {
      InetAddress localAddress = IPAddressUtil.getLocalHost(sourceAddress);
      if (!localAddress.isLoopbackAddress()) {
         if (Fox.ipv4Enabled && localAddress instanceof Inet4Address) {
            this.setMessageValue(announcement, "hostName", localAddress.getHostName());
            this.setMessageValue(announcement, "hostAddress", localAddress.getHostAddress());
         }

         if (Fox.ipv6Enabled && localAddress instanceof Inet6Address) {
            this.setMessageValue(announcement, "hostNameIPv6", localAddress.getHostName());
            this.setMessageValue(announcement, "hostAddressIPv6", localAddress.getHostAddress());
         }
      }
   }

   private void setMessageValue(FoxMessage message, String key, String value) {
      message.remove(key);
      message.add(key, value);
      logger.finest(() -> String.format("Setting %s to %s", key, value));
   }

   protected void sendAnnouncement(InetAddress sourceAddress, FoxMessage rollcallMessage) {
      if (this.server != null) {
         if (this.checkRateLimit()) {
            if (!sourceAddress.isSiteLocalAddress() && !sourceAddress.isLinkLocalAddress() && IPAddressUtil.findInterfaceNetworkSettings(sourceAddress) == null
               )
             {
               logger.fine(() -> String.format("Ignoring rollcall received from IP address %s", sourceAddress));
            } else {
               FoxMessage announcement = this.server.getAnnouncement(rollcallMessage);
               if (announcement != null) {
                  this.updateAnnouncementAddress(sourceAddress, announcement);
                  announcement.remove("niagaraPlatformType");
                  String platformType = FoxSession.getNiagaraPlatformType();
                  if (!platformType.isEmpty()) {
                     announcement.add("niagaraPlatformType", platformType);
                  }

                  announcement.add("unicast", true);
                  FoxFrame announcementFrame = MulticastUtil.makeFrame("announcement", announcement);
                  int waitTime = this.getResponseWaitTime(rollcallMessage);
                  int queueSize = this.scheduledExecutor.getQueue().size();
                  if (queueSize <= 20) {
                     logger.fine(
                        () -> String.format("Scheduling announcement send to %s in %dms, current send queue size = %d", sourceAddress, waitTime, queueSize)
                     );
                     this.scheduledExecutor
                        .schedule(() -> this.sendAnnouncementMessage(sourceAddress, announcementFrame), (long)waitTime, TimeUnit.MILLISECONDS);
                  } else {
                     logger.fine("Not sending announcement as send queue is full");
                  }
               }
            }
         }
      }
   }

   private boolean checkRateLimit() {
      long currentTimeMS = Clock.ticks();
      if (this.lastAnnouncementTimeMS == 0L) {
         this.lastAnnouncementTimeMS = currentTimeMS - 25L;
      }

      long intervalMS = currentTimeMS - this.lastAnnouncementTimeMS;
      long avgIntervalMS = (intervalMS + this.totalAnnouncementIntervalMS) / 6L;
      if (avgIntervalMS < 25L) {
         return false;
      } else {
         this.lastAnnouncementTimeMS = currentTimeMS;
         this.totalAnnouncementIntervalMS = this.totalAnnouncementIntervalMS - this.announcementIntervalMS[this.announcementIntervalMSPos];
         this.totalAnnouncementIntervalMS += intervalMS;
         this.announcementIntervalMS[this.announcementIntervalMSPos] = intervalMS;
         this.announcementIntervalMSPos = (this.announcementIntervalMSPos + 1) % 5;
         logger.fine(() -> String.format("Rate limit check ok: last interval = %dms, new average = %dms", intervalMS, this.totalAnnouncementIntervalMS / 5L));
         return true;
      }
   }

   protected int getResponseWaitTime(FoxMessage rollcallMessage) {
      int responseDelay = rollcallMessage.getInt("responseDelay", 650);
      int responseInterval = rollcallMessage.getInt("responseInterval", 3950);
      Random random = new Random();
      int waitTime = responseDelay + random.nextInt(responseInterval);
      waitTime = Math.max(0, waitTime);
      return Math.min(30000, waitTime);
   }

   protected void sendAnnouncementMessage(InetAddress destinationAddress, FoxFrame announcementFrame) {
      InetSocketAddress destination = new InetSocketAddress(destinationAddress, 1911);
      StandardProtocolFamily protocol = destinationAddress instanceof Inet4Address ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;
      if (this.getEnabledFoxIPProtocols().contains(protocol)) {
         try (DatagramChannel channel = DatagramChannel.open(protocol)) {
            channel.send(this.createByteBuffer(announcementFrame), destination);
            logger.fine(() -> String.format("Sent Fox announcement packet to %s", destinationAddress));
         } catch (IOException var18) {
            logger.log(Level.SEVERE, var18, () -> String.format("Unable to send Fox announcement to %s", destinationAddress));
         }
      } else {
         logger.info(
            () -> String.format(
               "Not sending Fox announcement packet to %s as ipv%d in not enabled in the local station",
               destinationAddress,
               protocol.equals(StandardProtocolFamily.INET) ? 4 : 6
            )
         );
      }
   }

   protected void receiveAnnouncement(FoxMessage message) {
      Vector<FoxMessage> v = this.announcements;
      if (v != null) {
         v.addElement(message);
      }
   }

   private void echoAnnouncement(InetAddress sourceAddress, FoxFrame frame) {
      try {
         if (frame.message.getBoolean("unicast", false)) {
            frame.message.remove("unicast");
            ByteBuffer buffer = this.createByteBuffer(frame);
            StandardProtocolFamily protocolFamily = sourceAddress instanceof Inet4Address ? StandardProtocolFamily.INET : StandardProtocolFamily.INET6;

            for (NetworkInterface networkInterface : this.getMulticastInterfaces(ifc -> ifc.isLoopback() && this.isProtocolSupported(ifc, protocolFamily))) {
               logger.fine(
                  () -> String.format("Sending announcement echo on interface %s using protocol %s", networkInterface.getDisplayName(), protocolFamily)
               );
               this.scheduledExecutor.schedule(() -> this.sendMulticastMessage(buffer, networkInterface, protocolFamily), 0L, TimeUnit.MILLISECONDS);
            }
         }
      } catch (IOException var7) {
      }
   }

   private boolean sendMulticastMessage(final ByteBuffer message, final NetworkInterface networkInterface, final StandardProtocolFamily protocolFamily) {
      try {
         final InetSocketAddress multicastAddress = new InetSocketAddress(this.getFoxMulticastAddress(protocolFamily), 1911);
         logger.finest(() -> String.format("Sending multicast packet to address %s on interface %s", multicastAddress, networkInterface));
         AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
            public Void run() throws IOException {
               Object var3;
               try (DatagramChannel channel = MulticastServer.this.createMulticastChannel(protocolFamily, networkInterface)) {
                  ((Buffer)message).position(0);
                  channel.send(message, multicastAddress);
                  var3 = null;
               }

               return (Void)var3;
            }
         });
         return true;
      } catch (PrivilegedActionException var5) {
         logger.log(Level.SEVERE, "Failed to send multicast message", (Throwable)var5.getException());
      } catch (UnknownHostException var6) {
         logger.log(Level.SEVERE, "Failed to send multicast message", (Throwable)var6);
      }

      return false;
   }

   public void kill() {
      this.isAlive = false;
      this.interrupt();

      for (DatagramChannel channel : this.receiveChannels) {
         try {
            channel.close();
         } catch (IOException var5) {
         }
      }

      this.receiveChannels = null;

      try {
         this.receiveSelector.close();
      } catch (IOException var4) {
      }

      this.receiveSelector = null;
      this.scheduledExecutor.shutdownNow();
   }

   protected void receiveData(final DatagramChannel channel) throws IOException {
      final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

      InetSocketAddress sourceAddress;
      try {
         sourceAddress = AccessController.doPrivileged(new PrivilegedExceptionAction<InetSocketAddress>() {
            public InetSocketAddress run() throws IOException {
               return (InetSocketAddress)channel.receive(byteBuffer);
            }
         });
      } catch (PrivilegedActionException var5) {
         throw (IOException)var5.getException();
      }

      ((Buffer)byteBuffer).flip();
      this.processReceivedFrame(sourceAddress.getAddress(), this.readFrameFromByteBuffer(byteBuffer));
   }

   protected FoxFrame readFrameFromByteBuffer(ByteBuffer byteBuffer) throws IOException {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.limit());
      MessageReader messageReader = new MessageReader(inputStream);
      messageReader.setReadLimit(byteBuffer.limit());
      return FoxFrame.read(messageReader);
   }

   protected void processReceivedFrame(InetAddress sourceAddress, FoxFrame receivedFrame) {
      if (sourceAddress instanceof Inet6Address) {
         receivedFrame.message.add("IPv6FoxFrameMulticastReceived", true);
      }

      if (Fox.traceMulticast) {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         receivedFrame.dump(byteArrayOutputStream);
         logger.info("-- Fox.multicast.received\n" + new String(byteArrayOutputStream.toByteArray()));
      }

      if (receivedFrame.command.equals("rollcall")) {
         logger.fine(() -> String.format("Received rollcall from %s", sourceAddress));
         this.sendAnnouncement(sourceAddress, receivedFrame.message);
      } else if (receivedFrame.command.equals("announcement")) {
         if (logger.isLoggable(Level.FINEST)) {
            logger.finest(
               () -> String.format(
                  "Received announcement from %s, station = %s, unicast = %s",
                  sourceAddress,
                  receivedFrame.message.getString("station", "NONE"),
                  receivedFrame.message.getBoolean("unicast", false)
               )
            );
         } else if (logger.isLoggable(Level.FINE) && (receivedFrame.message.getBoolean("unicast", false) || sourceAddress.isLoopbackAddress())) {
            logger.fine(
               () -> String.format(
                  "Received announcement from %s, station = %s, unicast = %s",
                  sourceAddress,
                  receivedFrame.message.getString("station", "NONE"),
                  receivedFrame.message.getBoolean("unicast", false)
               )
            );
         }

         this.receiveAnnouncement(receivedFrame.message);
         this.echoAnnouncement(sourceAddress, receivedFrame);
      } else {
         logger.fine(() -> String.format("Unrecognized command: %s", receivedFrame.command));
      }
   }

   protected DatagramChannel createMulticastChannel(StandardProtocolFamily protocolFamily, NetworkInterface networkInterface) throws IOException {
      DatagramChannel channel = DatagramChannel.open(protocolFamily);

      try {
         channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
         channel.setOption(StandardSocketOptions.IP_MULTICAST_TTL, Fox.multicastTimeToLive);
         logger.finest(() -> String.format("Created %s channel for interface %s", protocolFamily, networkInterface.getDisplayName()));
         return channel;
      } catch (RuntimeException | IOException var5) {
         channel.close();
         throw var5;
      }
   }

   protected InetAddress getFoxMulticastAddress(final StandardProtocolFamily protocolFamily) throws UnknownHostException {
      try {
         return AccessController.doPrivileged(new PrivilegedExceptionAction<InetAddress>() {
            public InetAddress run() throws UnknownHostException {
               switch (protocolFamily) {
                  case INET:
                     return InetAddress.getByName(Fox.MULTICAST_ADDRESS);
                  case INET6:
                     return InetAddress.getByName(Fox.IPV6_MULTICAST_ADDRESS);
                  default:
                     return null;
               }
            }
         });
      } catch (PrivilegedActionException var3) {
         throw (UnknownHostException)var3.getException();
      }
   }

   protected Set<StandardProtocolFamily> getEnabledFoxIPProtocols() {
      Set<StandardProtocolFamily> protocols = new HashSet<>();
      if (Fox.ipv4Enabled) {
         protocols.add(StandardProtocolFamily.INET);
      }

      if (Fox.ipv6Enabled) {
         protocols.add(StandardProtocolFamily.INET6);
      }

      return protocols;
   }

   private List<NetworkInterface> getMulticastInterfaces(MulticastServer.NetworkInterfaceFilter filter) {
      List<NetworkInterface> interfaces = new ArrayList<>();

      try {
         for (NetworkInterface networkInterface : this.getAllNetworkInterfaces()) {
            boolean supportsMulticast = networkInterface.supportsMulticast();
            boolean isUp = networkInterface.isUp();
            logger.finest(
               () -> String.format(
                  "Found network interface %s, supports multicast = %s, is up = %s", networkInterface.getDisplayName(), supportsMulticast, isUp
               )
            );
            if (supportsMulticast && isUp && (filter == null || filter.test(networkInterface))) {
               logger.finest(() -> String.format("Adding network interface %s to multicast interface list", networkInterface.getDisplayName()));
               interfaces.add(networkInterface);
            }
         }
      } catch (SocketException var7) {
         logger.log(Level.SEVERE, "Unexpected SocketException", (Throwable)var7);
      }

      return interfaces;
   }

   protected boolean isProtocolSupported(NetworkInterface networkInterface, StandardProtocolFamily protocolFamily) {
      for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
         if (interfaceAddress.getAddress() instanceof Inet4Address && protocolFamily.equals(StandardProtocolFamily.INET)
            || interfaceAddress.getAddress() instanceof Inet6Address && protocolFamily.equals(StandardProtocolFamily.INET6)) {
            logger.finest(
               () -> String.format(
                  "protocol %s is supported on interface %s by address %s", protocolFamily, networkInterface.getDisplayName(), interfaceAddress.getAddress()
               )
            );
            return true;
         }
      }

      return false;
   }

   protected List<NetworkInterface> getAllNetworkInterfaces() throws SocketException {
      return Collections.list(NetworkInterface.getNetworkInterfaces());
   }

   @Override
   public void run() {
      while (this.isAlive) {
         try {
            if (this.receiveSelector.select() > 0) {
               Iterator<SelectionKey> keyIterator = this.receiveSelector.selectedKeys().iterator();

               while (keyIterator.hasNext()) {
                  SelectionKey key = keyIterator.next();
                  keyIterator.remove();
                  if (key.isReadable()) {
                     this.receiveData((DatagramChannel)key.channel());
                  }
               }
            }
         } catch (Throwable var3) {
            if (this.isAlive) {
               logger.log(Level.SEVERE, "Unexpected exception in multicast server", var3);
            }
         }
      }
   }

   @FunctionalInterface
   private interface NetworkInterfaceFilter {
      boolean test(NetworkInterface var1) throws SocketException;
   }

   public interface RollcallCallback {
      void completed(int var1);
   }
}
